package org.crochet.service;

import org.crochet.properties.R2Properties;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;

public class R2StorageClient implements R2Storage, AutoCloseable {
    private final R2Properties properties;
    private final S3Client client;
    private final S3Presigner presigner;

    public R2StorageClient(R2Properties properties, S3Client client, S3Presigner presigner) {
        this.properties = properties;
        this.client = client;
        this.presigner = presigner;
    }

    @Override
    public String presign(String bucket, String key, String contentType, long size, Duration validity) {
        validate(bucket, key);
        try {
            return presigner.presignPutObject(PutObjectPresignRequest.builder()
                    .signatureDuration(validity)
                    .putObjectRequest(PutObjectRequest.builder().bucket(bucket).key(key)
                            .contentType(contentType).contentLength(size).build()).build()).url().toString();
        } catch (RuntimeException ex) {
            throw unavailable();
        }
    }

    @Override
    public Metadata head(String bucket, String key) {
        validate(bucket, key);
        try {
            var response = client.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
            return new Metadata(response.contentLength(), response.contentType());
        } catch (S3Exception ex) {
            if (ex.statusCode() == 404) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Upload has not completed");
            }
            throw unavailable();
        } catch (RuntimeException ex) {
            throw unavailable();
        }
    }

    @Override
    public void delete(String bucket, String key) {
        validate(bucket, key);
        try {
            client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (S3Exception ex) {
            // Deletion is idempotent, including a retry after a successful delete and process crash.
            if (ex.statusCode() != 404) {
                throw unavailable();
            }
        } catch (RuntimeException ex) {
            throw unavailable();
        }
    }

    private void validate(String bucket, String key) {
        properties.requireConfigured();
        properties.validateScope(bucket, key);
        if (client == null || presigner == null) {
            throw unavailable();
        }
    }

    private ResponseStatusException unavailable() {
        // Do not propagate SDK messages/causes: they may contain signed URLs or credentials.
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "R2 storage operation failed");
    }

    @Override
    public void close() {
        if (presigner != null) {
            presigner.close();
        }
        if (client != null) {
            client.close();
        }
    }
}
