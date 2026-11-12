package org.crochet.service;

import org.crochet.exception.BadRequestException;
import org.crochet.model.UploadAsset;
import org.crochet.payload.request.PresignedUploadRequest;
import org.crochet.payload.response.PresignedUploadResponse;
import org.crochet.properties.R2Properties;
import org.crochet.repository.UploadAssetRepository;
import org.crochet.util.SecurityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class UploadReservationService {
    public static final Duration SIGNED_URL_VALIDITY = Duration.ofMinutes(5);
    public static final Duration PENDING_VALIDITY = Duration.ofHours(24);
    private static final long MAX_SIZE = 100L * 1024 * 1024;
    private static final Map<String, Set<String>> TYPES = Map.ofEntries(
            Map.entry("image/jpeg", Set.of("jpg", "jpeg")), Map.entry("image/png", Set.of("png")),
            Map.entry("image/gif", Set.of("gif")), Map.entry("image/webp", Set.of("webp")),
            Map.entry("image/avif", Set.of("avif")), Map.entry("image/bmp", Set.of("bmp")),
            Map.entry("video/mp4", Set.of("mp4")), Map.entry("video/webm", Set.of("webm")),
            Map.entry("application/pdf", Set.of("pdf")), Map.entry("application/msword", Set.of("doc")),
            Map.entry("application/vnd.openxmlformats-officedocument.wordprocessingml.document", Set.of("docx")));

    private final UploadAssetRepository repository;
    private final R2Storage storage;
    private final R2Properties properties;
    private final Clock clock;

    public UploadReservationService(UploadAssetRepository repository, R2Storage storage, R2Properties properties, Clock clock) {
        this.repository = repository;
        this.storage = storage;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public PresignedUploadResponse reserve(PresignedUploadRequest request) {
        var user = SecurityUtils.getCurrentUser();
        if (user == null || user.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }
        if (request == null || !safeSegment(request.module()) || !safeSegment(request.category())) {
            throw new BadRequestException("Invalid upload module or category");
        }
        if ((Set.of("settings", "video-popups").contains(request.module())
                || Set.of("settings", "video-popups").contains(request.category()))
                && !SecurityUtils.hasRole("ROLE_ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrator access is required");
        }
        String extension = validateFile(request);
        properties.requireConfigured();
        var now = clock.instant();
        String key = properties.getEnvironment() + "/" + request.module() + "/" + request.category()
                + "/" + UUID.randomUUID() + "." + extension;
        repository.saveAndFlush(new UploadAsset(key, properties.getEnvironment(), properties.getBucketName(),
                user.getId(), request.contentType(), request.size(), now, now.plus(PENDING_VALIDITY)));
        String signedUrl = storage.presign(properties.getBucketName(), key, request.contentType(), request.size(), SIGNED_URL_VALIDITY);
        // The transaction proxy commits the reservation before the controller receives this result.
        return new PresignedUploadResponse(key, properties.publicUrl(key), signedUrl, now.plus(SIGNED_URL_VALIDITY));
    }

    private boolean safeSegment(String value) {
        return value != null && value.matches("[a-z0-9][a-z0-9-]{0,39}");
    }

    private String validateFile(PresignedUploadRequest request) {
        String name = request.fileName();
        if (name == null || name.isBlank() || name.length() > 255 || name.contains("/") || name.contains("\\")
                || name.chars().anyMatch(Character::isISOControl) || name.lastIndexOf('.') <= 0
                || request.size() == null || request.size() <= 0 || request.size() > MAX_SIZE) {
            throw new BadRequestException("Invalid upload filename or size");
        }
        String extension = name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (request.contentType() == null || !TYPES.getOrDefault(request.contentType(), Set.of()).contains(extension)) {
            throw new BadRequestException("Unsupported upload content type or extension");
        }
        return extension;
    }
}
