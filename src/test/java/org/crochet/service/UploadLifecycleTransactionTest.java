package org.crochet.service;

import jakarta.persistence.EntityManagerFactory;
import org.crochet.model.UploadAsset;
import org.crochet.payload.request.PresignedUploadRequest;
import org.crochet.repository.UploadAssetRepository;
import org.crochet.util.UploadReferenceParser;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.crochet.service.UploadTestSupport.*;

/** No application/profile loading: this fixture owns an isolated in-memory database and a mocked storage. */
class UploadLifecycleTransactionTest {
    private static LocalContainerEntityManagerFactoryBean factory;
    private static EntityManagerFactory emf;
    private static JpaTransactionManager manager;
    private static UploadAssetRepository repository;
    private static TransactionTemplate tx;
    private static JdbcTemplate jdbc;
    private R2Storage storage;
    private UploadLifecycleService lifecycle;
    private UploadReservationService reservations;
    private UploadCleanupTransactions cleanup;

    @BeforeAll
    static void openIsolatedDatabase() {
        var dataSource = new DriverManagerDataSource("jdbc:h2:mem:upload-lifecycle-" + UUID.randomUUID()
                + ";DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000", "sa", "");
        factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(dataSource);
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setManagedTypes(PersistenceManagedTypes.of(UploadAsset.class.getName()));
        factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "create-drop", "hibernate.show_sql", "false"));
        factory.afterPropertiesSet();
        emf = factory.getObject();
        manager = new JpaTransactionManager(emf);
        manager.setDataSource(dataSource);
        manager.afterPropertiesSet();
        tx = new TransactionTemplate(manager);
        repository = new JpaRepositoryFactory(SharedEntityManagerCreator.createSharedEntityManager(emf))
                .getRepository(UploadAssetRepository.class);
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("create table test_upload_domain (id varchar(50) primary key)");
    }

    @AfterAll
    static void closeDatabase() {
        factory.destroy();
    }

    @BeforeEach
    void setUp() {
        tx.executeWithoutResult(status -> {
            repository.deleteAll();
            jdbc.update("delete from test_upload_domain");
        });
        storage = mock(R2Storage.class);
        var properties = properties();
        lifecycle = transactional(new UploadLifecycleService(repository, storage, properties, new UploadReferenceParser(properties), CLOCK));
        reservations = transactional(new UploadReservationService(repository, storage, properties, CLOCK));
        cleanup = transactional(new UploadCleanupTransactions(repository, properties, CLOCK));
        when(storage.head(anyString(), anyString())).thenReturn(new R2Storage.Metadata(100, "image/png"));
        when(storage.presign(anyString(), anyString(), anyString(), anyLong(), any())).thenReturn("https://signed.example.test");
        authenticate("owner", "ROLE_USER");
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void mandatoryApisRejectCallsWithoutDomainTransaction() {
        assertThatThrownBy(() -> lifecycle.attach(new UploadLifecycleService.References().urls(CDN + "/" + KEY)))
                .isInstanceOf(IllegalTransactionStateException.class);
        assertThatThrownBy(() -> lifecycle.attach(new UploadLifecycleService.References()))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    void reservationCommitsBeforeReturnAndSigningFailureRollsBack() {
        var result = reservations.reserve(new PresignedUploadRequest("blogs", "images", "a.png", "image/png", 100L));
        try (var independent = emf.createEntityManager()) {
            assertThat(independent.createQuery("select a from UploadAsset a where a.objectKey = :key", UploadAsset.class)
                    .setParameter("key", result.objectKey()).getSingleResult().getState()).isEqualTo(UploadAsset.State.PENDING);
        }
        when(storage.presign(anyString(), anyString(), anyString(), anyLong(), any())).thenThrow(new IllegalStateException("offline"));
        assertThatThrownBy(() -> reservations.reserve(new PresignedUploadRequest("blogs", "images", "b.png", "image/png", 100L)))
                .isInstanceOf(IllegalStateException.class);
        assertThat(tx.<Long>execute(status -> repository.count())).isEqualTo(1L);
    }

    @Test
    void attachmentAndDomainWritesCommitOrRollbackTogether() {
        var saved = save(asset());
        assertThatThrownBy(() -> tx.executeWithoutResult(status -> {
            lifecycle.attach(new UploadLifecycleService.References().urls(CDN + "/" + KEY));
            jdbc.update("insert into test_upload_domain (id) values ('rollback')");
            throw new IllegalStateException("domain persistence failed");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(state(saved)).isEqualTo(UploadAsset.State.PENDING);
        assertThat(jdbc.queryForObject("select count(*) from test_upload_domain", Long.class)).isZero();
        tx.executeWithoutResult(status -> {
            lifecycle.attach(new UploadLifecycleService.References().urls(CDN + "/" + KEY));
            jdbc.update("insert into test_upload_domain (id) values ('commit')");
        });
        assertThat(state(saved)).isEqualTo(UploadAsset.State.ATTACHED);
        assertThat(jdbc.queryForObject("select count(*) from test_upload_domain", Long.class)).isEqualTo(1L);
    }

    @Test
    void committedCleanupClaimBlocksAttachmentAndTombstoneIsRetained() {
        var saved = save(asset(KEY, NOW));
        var claim = cleanup.claim(KEY).orElseThrow();
        assertThat(state(saved)).isEqualTo(UploadAsset.State.DELETING);
        assertThatThrownBy(() -> tx.executeWithoutResult(status -> lifecycle.attach(new UploadLifecycleService.References().urls(CDN + "/" + KEY))))
                .isInstanceOf(ResponseStatusException.class);
        cleanup.complete(claim);
        assertThat(state(saved)).isEqualTo(UploadAsset.State.DELETED);
        assertThat(tx.<Long>execute(status -> repository.count())).isEqualTo(1L);
        assertThatThrownBy(() -> tx.executeWithoutResult(status -> lifecycle.attach(new UploadLifecycleService.References().urls(CDN + "/" + KEY))))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void attachingRowLockWinsAgainstCleanupRecheck() throws Exception {
        var saved = save(asset());
        var enteredHead = new CountDownLatch(1);
        var releaseHead = new CountDownLatch(1);
        var claimStarted = new CountDownLatch(1);
        when(storage.head("unit-test-bucket", KEY)).thenAnswer(invocation -> {
            enteredHead.countDown();
            if (!releaseHead.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("test synchronization timed out");
            }
            return new R2Storage.Metadata(100, "image/png");
        });
        var laterCleanup = transactional(new UploadCleanupTransactions(repository, properties(),
                Clock.fixed(NOW.plusSeconds(86400), ZoneOffset.UTC)));
        var executor = Executors.newFixedThreadPool(2);
        try {
            var attached = executor.submit(() -> {
                authenticate("owner", "ROLE_USER");
                try {
                    tx.executeWithoutResult(status -> lifecycle.attach(new UploadLifecycleService.References().urls(CDN + "/" + KEY)));
                } finally {
                    SecurityContextHolder.clearContext();
                }
            });
            assertThat(enteredHead.await(5, TimeUnit.SECONDS)).isTrue();
            var claimed = executor.submit(() -> {
                claimStarted.countDown();
                return laterCleanup.claim(KEY);
            });
            assertThat(claimStarted.await(5, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> claimed.get(150, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
            releaseHead.countDown();
            attached.get(5, TimeUnit.SECONDS);
            assertThat(claimed.get(5, TimeUnit.SECONDS)).isEmpty();
            assertThat(state(saved)).isEqualTo(UploadAsset.State.ATTACHED);
        } finally {
            releaseHead.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void competingWorkersOnlyClaimOnceAndCandidateQueryScopesRows() throws Exception {
        save(asset(KEY, NOW));
        save(new UploadAsset(KEY.replace("development", "production"), "production", "unit-test-bucket", "owner",
                "image/png", 100, NOW.minusSeconds(86400), NOW));
        save(new UploadAsset(KEY.replace("0001.png", "0002.png"), "development", "another-bucket", "owner",
                "image/png", 100, NOW.minusSeconds(86400), NOW));
        var candidates = tx.execute(status -> repository.findCleanupCandidates("development", "unit-test-bucket",
                List.of(UploadAsset.State.PENDING, UploadAsset.State.DELETING), NOW, PageRequest.of(0, 100)));
        assertThat(candidates).containsExactly(KEY);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var start = new CountDownLatch(1);
            var first = executor.submit(() -> { start.await(); return cleanup.claim(KEY); });
            var second = executor.submit(() -> { start.await(); return cleanup.claim(KEY); });
            start.countDown();
            var claims = List.of(first.get(5, TimeUnit.SECONDS), second.get(5, TimeUnit.SECONDS));
            assertThat(claims.stream().filter(java.util.Optional::isPresent).count()).isEqualTo(1);
        }
    }

    private UploadAsset save(UploadAsset asset) {
        return tx.execute(status -> repository.save(asset));
    }

    private UploadAsset.State state(UploadAsset asset) {
        return tx.execute(status -> repository.findById(asset.getId()).orElseThrow().getState());
    }

    @SuppressWarnings("unchecked")
    private static <T> T transactional(T target) {
        var advice = new TransactionInterceptor();
        advice.setTransactionManager(manager);
        advice.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());
        advice.afterPropertiesSet();
        var proxy = new ProxyFactory(target);
        proxy.setProxyTargetClass(true);
        proxy.addAdvice(advice);
        return (T) proxy.getProxy();
    }
}
