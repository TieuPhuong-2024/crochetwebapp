package org.crochet.service;

import org.crochet.exception.BadRequestException;
import org.crochet.model.UploadAsset;
import org.crochet.payload.request.PresignedUploadRequest;
import org.crochet.properties.R2Properties;
import org.crochet.repository.UploadAssetRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.crochet.service.UploadTestSupport.*;

class UploadReservationServiceTest {
    private UploadAssetRepository repository;
    private R2Storage storage;
    private R2Properties properties;
    private UploadReservationService service;

    @BeforeEach
    void setUp() {
        repository = mock(UploadAssetRepository.class);
        storage = mock(R2Storage.class);
        properties = properties();
        service = new UploadReservationService(repository, storage, properties, CLOCK);
        authenticate("owner", "ROLE_USER");
        when(storage.presign(anyString(), anyString(), anyString(), anyLong(), any())).thenReturn("https://signed.example.test");
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void savesPendingBeforeSigningAndReturnsContract() {
        var result = service.reserve(request("blogs", "images", "photo.PNG", "image/png", 100L));
        var captor = ArgumentCaptor.forClass(UploadAsset.class);
        var order = inOrder(repository, storage);
        order.verify(repository).saveAndFlush(captor.capture());
        order.verify(storage).presign(eq("unit-test-bucket"), eq(result.objectKey()), eq("image/png"), eq(100L),
                eq(UploadReservationService.SIGNED_URL_VALIDITY));
        var asset = captor.getValue();
        assertThat(asset.getState()).isEqualTo(UploadAsset.State.PENDING);
        assertThat(asset.getOwnerId()).isEqualTo("owner");
        assertThat(asset.getExpiresAt()).isEqualTo(NOW.plusSeconds(86400));
        assertThat(result.objectKey()).matches("development/blogs/images/[0-9a-f-]{36}\\.png");
        assertThat(result.publicUrl()).isEqualTo(CDN + "/" + result.objectKey());
        assertThat(result.signedUrl()).isEqualTo("https://signed.example.test");
        assertThat(result.expiresAt()).isEqualTo(NOW.plusSeconds(300));
    }

    @ParameterizedTest
    @CsvSource({"../blogs,images,a.png,image/png,100", "blogs,../images,a.png,image/png,100",
            "blogs,images,a.svg,image/svg+xml,100", "blogs,images,a.exe,image/png,100",
            "blogs,images,a.png,video/mp4,100", "blogs,images,a.mov,video/quicktime,100",
            "blogs,images,a.html,text/html,100", "blogs,images,a.png,image/png,0",
            "blogs,images,a.png,image/png,-1", "blogs,images,a.png,image/png,104857601"})
    void rejectsInvalidMetadata(String module, String category, String name, String type, long size) {
        assertThatThrownBy(() -> service.reserve(request(module, category, name, type, size)))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(repository);
    }

    @ParameterizedTest
    @CsvSource({"a.mp4,video/mp4", "a.webm,video/webm", "a.pdf,application/pdf", "a.doc,application/msword",
            "a.docx,application/vnd.openxmlformats-officedocument.wordprocessingml.document"})
    void acceptsAllowedDocumentsAndVideos(String name, String type) {
        assertThat(service.reserve(request("blogs", "attachments", name, type, 104857600L)).objectKey()).isNotBlank();
    }

    @ParameterizedTest
    @CsvSource({"settings", "video-popups"})
    void adminOnlyModules(String module) {
        assertThatThrownBy(() -> service.reserve(request(module, "images", "a.png", "image/png", 100L)))
                .isInstanceOf(ResponseStatusException.class).satisfies(ex ->
                        assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(403));
        authenticate("admin", "ROLE_ADMIN");
        assertThat(service.reserve(request(module, "images", "a.png", "image/png", 100L))).isNotNull();
    }

    @Test
    void requiresAuthenticationAndConfiguration() {
        SecurityContextHolder.clearContext();
        assertThatThrownBy(() -> service.reserve(request("blogs", "images", "a.png", "image/png", 100L)))
                .isInstanceOf(ResponseStatusException.class);
        authenticate("owner", "ROLE_USER");
        properties.setSecretKey("");
        assertThatThrownBy(() -> service.reserve(request("blogs", "images", "a.png", "image/png", 100L)))
                .isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void signingFailureIsNotReturnedToCaller() {
        when(storage.presign(anyString(), anyString(), anyString(), anyLong(), any())).thenThrow(new IllegalStateException("offline"));
        assertThatThrownBy(() -> service.reserve(request("blogs", "images", "a.png", "image/png", 100L)))
                .isInstanceOf(IllegalStateException.class);
    }

    private PresignedUploadRequest request(String module, String category, String name, String type, Long size) {
        return new PresignedUploadRequest(module, category, name, type, size);
    }
}
