package org.crochet.service;

import org.crochet.config.R2Configuration;
import org.crochet.properties.R2Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.crochet.service.UploadTestSupport.*;

class R2StorageClientTest {
    private S3Client sdk;
    private S3Presigner presigner;
    private R2StorageClient storage;

    @BeforeEach
    void setUp() {
        sdk = mock(S3Client.class);
        presigner = mock(S3Presigner.class);
        storage = new R2StorageClient(properties(), sdk, presigner);
    }

    @Test
    void validatesScopeBeforeEverySdkOperation() {
        assertThatThrownBy(() -> storage.head("other-bucket", KEY)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> storage.delete("unit-test-bucket", KEY.replace("development", "production")))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> storage.presign("unit-test-bucket", "development/../production/a.png", "image/png", 100,
                UploadReservationService.SIGNED_URL_VALIDITY)).isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(sdk, presigner);
    }

    @Test
    void returnsHeadMetadataAndTreatsMissingAsConflict() {
        when(sdk.headObject(any(HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder().contentLength(100L).contentType("image/png").build())
                .thenThrow(S3Exception.builder().statusCode(404).message("unsafe URL").build());
        assertThat(storage.head("unit-test-bucket", KEY)).isEqualTo(new R2Storage.Metadata(100, "image/png"));
        assertThatThrownBy(() -> storage.head("unit-test-bucket", KEY)).isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(409))
                .hasCause(null).hasMessageNotContaining("unsafe URL");
    }

    @Test
    void deleteMissingIsSuccessAndOtherErrorsDoNotLeakSdkMessages() {
        when(sdk.deleteObject(any(DeleteObjectRequest.class)))
                .thenThrow(S3Exception.builder().statusCode(404).build())
                .thenThrow(S3Exception.builder().statusCode(500).message("unsafe signed URL").build());
        assertThatCode(() -> storage.delete("unit-test-bucket", KEY)).doesNotThrowAnyException();
        assertThatThrownBy(() -> storage.delete("unit-test-bucket", KEY))
                .isInstanceOf(ResponseStatusException.class).hasCause(null).hasMessageNotContaining("unsafe signed URL");
    }

    @Test
    void missingCredentialsAllowStartupButNotCallsAndEnvironmentIsStrict() {
        R2Properties properties = new R2Properties();
        try (var unconfigured = new R2Configuration().r2Storage(properties)) {
            assertThatThrownBy(() -> unconfigured.head("unit-test-bucket", KEY))
                    .isInstanceOf(ResponseStatusException.class).hasMessageContaining("not configured");
        }
        properties.setEnvironment("staging");
        assertThatThrownBy(properties::validateEnvironment).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void actualPresignerUsesConfiguredScopeAndFiveMinuteValidityWithoutNetwork() {
        try (var configured = new R2Configuration().r2Storage(properties())) {
            String url = configured.presign("unit-test-bucket", KEY, "image/png", 100, UploadReservationService.SIGNED_URL_VALIDITY);
            assertThat(url).startsWith("https://storage.example.test/unit-test-bucket/" + KEY)
                    .contains("X-Amz-Expires=300").contains("X-Amz-SignedHeaders=");
        }
    }
}
