package org.crochet.service.impl;

import jakarta.persistence.LockModeType;
import org.crochet.enums.ResultCode;
import org.crochet.exception.TokenException;
import org.crochet.model.RefreshToken;
import org.crochet.properties.AppProperties;
import org.crochet.repository.RefreshTokenRepo;
import org.crochet.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.repository.Lock;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    @Mock
    private RefreshTokenRepo refreshTokenRepo;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AppProperties appProps;

    @InjectMocks
    private RefreshTokenServiceImpl refreshTokenService;

    @Test
    void ensureActiveRefreshToken_returnsExisting_whenPresent() {
        var token = RefreshToken.builder().token("rt-1").build();
        when(refreshTokenRepo.findFirstByUser_IdAndRevokedFalseAndExpiresAtAfterOrderByExpiresAtDesc(
                eq("user-1"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(token));

        var result = refreshTokenService.ensureActiveRefreshToken("user-1");

        assertThat(result).isSameAs(token);
        verify(userRepository, never()).findById(any());
    }

    @Test
    void findActiveToken_returnsEmpty_whenNone() {
        when(refreshTokenRepo.findFirstByUser_IdAndRevokedFalseAndExpiresAtAfterOrderByExpiresAtDesc(
                eq("user-1"), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        assertThat(refreshTokenService.findActiveToken("user-1")).isEmpty();
    }

    @Test
    void createRefreshToken_rememberMeTrue_usesThirtyDays() {
        var auth = new AppProperties.Auth();
        auth.setRefreshTokenRememberMeExpirationMs(2_592_000_000L);
        when(appProps.getAuth()).thenReturn(auth);
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user()));
        when(refreshTokenRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var before = LocalDateTime.now();

        var result = refreshTokenService.createRefreshToken("user-1", true);

        assertThat(result.getExpiresAt()).isAfter(before.plusDays(30).minusSeconds(2));
        assertThat(result.getExpiresAt()).isBefore(before.plusDays(30).plusSeconds(2));
    }

    @Test
    void createRefreshToken_rememberMeFalse_usesOneDay() {
        var auth = new AppProperties.Auth();
        auth.setRefreshTokenExpirationMs(86_400_000L);
        when(appProps.getAuth()).thenReturn(auth);
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user()));
        when(refreshTokenRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var before = LocalDateTime.now();

        var result = refreshTokenService.createRefreshToken("user-1", false);

        assertThat(result.getExpiresAt()).isAfter(before.plusDays(1).minusSeconds(2));
        assertThat(result.getExpiresAt()).isBefore(before.plusDays(1).plusSeconds(2));
    }

    @Test
    void rotateRefreshToken_validToken_rotatesAndPreservesExpiry() {
        var expiresAt = LocalDateTime.now().plusDays(1);
        var current = RefreshToken.builder()
                .token("old-token")
                .expiresAt(expiresAt)
                .user(user())
                .build();
        when(refreshTokenRepo.findByTokenForUpdate("old-token")).thenReturn(Optional.of(current));
        when(refreshTokenRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var rotated = refreshTokenService.rotateRefreshToken("old-token");

        assertThat(rotated.getToken()).isNotEqualTo("old-token");
        assertThat(rotated.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(current.isRevoked()).isTrue();
        verify(refreshTokenRepo).save(current);
        verify(refreshTokenRepo).save(rotated);
    }

    @Test
    void rotateRefreshToken_revokedToken_revokesAllActiveTokens() {
        var current = RefreshToken.builder()
                .token("old-token")
                .expiresAt(LocalDateTime.now().plusDays(1))
                .revoked(true)
                .user(user())
                .build();
        var active = RefreshToken.builder()
                .token("active-token")
                .expiresAt(LocalDateTime.now().plusDays(1))
                .user(user())
                .build();
        when(refreshTokenRepo.findByTokenForUpdate("old-token")).thenReturn(Optional.of(current));
        when(refreshTokenRepo.findAllValidRefreshTokenByUserId("user-1")).thenReturn(List.of(active));

        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken("old-token"))
                .isInstanceOfSatisfying(TokenException.class, error ->
                        assertThat(error.getMessageCode()).isEqualTo(ResultCode.MSG_REFRESH_TOKEN_REUSED.code()));
        assertThat(active.isRevoked()).isTrue();
        verify(refreshTokenRepo).save(active);
        verify(refreshTokenRepo, never()).save(current);
    }

    @Test
    void rotateRefreshToken_expiredToken_deletesToken() {
        var current = RefreshToken.builder()
                .token("expired-token")
                .expiresAt(LocalDateTime.now().minusSeconds(1))
                .user(user())
                .build();
        when(refreshTokenRepo.findByTokenForUpdate("expired-token")).thenReturn(Optional.of(current));

        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken("expired-token"))
                .isInstanceOfSatisfying(TokenException.class, error ->
                        assertThat(error.getMessageCode()).isEqualTo(ResultCode.MSG_REFRESH_TOKEN_EXPIRED.code()));
        verify(refreshTokenRepo).delete(current);
        verify(refreshTokenRepo, never()).save(any());
    }

    @Test
    void revokeRefreshToken_missingToken_isIdempotent() {
        when(refreshTokenRepo.findByToken("missing")).thenReturn(Optional.empty());

        assertThatCode(() -> refreshTokenService.revokeRefreshToken("missing")).doesNotThrowAnyException();

        verify(refreshTokenRepo, never()).save(any());
    }

    @Test
    void revokeAllRefreshTokens_revokesEveryActiveToken() {
        var first = RefreshToken.builder().token("first").build();
        var second = RefreshToken.builder().token("second").build();
        when(refreshTokenRepo.findAllValidRefreshTokenByUserId("user-1")).thenReturn(List.of(first, second));

        refreshTokenService.revokeAllRefreshTokens("user-1");

        assertThat(first.isRevoked()).isTrue();
        assertThat(second.isRevoked()).isTrue();
        verify(refreshTokenRepo).save(first);
        verify(refreshTokenRepo).save(second);
    }

    @Test
    void findByTokenForUpdate_usesPessimisticWriteLock() throws NoSuchMethodException {
        var method = RefreshTokenRepo.class.getMethod("findByTokenForUpdate", String.class);
        var lock = method.getAnnotation(Lock.class);

        assertThat(lock).isNotNull();
        assertThat(lock.value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    }

    private User user() {
        return User.builder().id("user-1").build();
    }
}
