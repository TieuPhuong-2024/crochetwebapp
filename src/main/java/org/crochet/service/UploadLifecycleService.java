package org.crochet.service;

import org.crochet.model.File;
import org.crochet.model.UploadAsset;
import org.crochet.properties.R2Properties;
import org.crochet.repository.UploadAssetRepository;
import org.crochet.util.SecurityUtils;
import org.crochet.util.UploadReferenceParser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

@Service
public class UploadLifecycleService {
    public static final class References {
        private final List<File> files = new ArrayList<>();
        private final List<String> urls = new ArrayList<>();
        private final List<String> contents = new ArrayList<>();

        public References files(Collection<File> values) {
            if (values != null) {
                values.stream().filter(Objects::nonNull).forEach(files::add);
            }
            return this;
        }

        public References urls(String... values) {
            if (values != null) {
                for (String value : values) {
                    if (value != null) {
                        urls.add(value);
                    }
                }
            }
            return this;
        }

        public References content(String... values) {
            if (values != null) {
                for (String value : values) {
                    if (value != null) {
                        contents.add(value);
                    }
                }
            }
            return this;
        }

        public List<File> files() {
            return Collections.unmodifiableList(files);
        }

        public List<String> urls() {
            return Collections.unmodifiableList(urls);
        }

        public List<String> contents() {
            return Collections.unmodifiableList(contents);
        }
    }

    private final UploadAssetRepository repository;
    private final R2Storage storage;
    private final R2Properties properties;
    private final UploadReferenceParser parser;
    private final Clock clock;

    public UploadLifecycleService(UploadAssetRepository repository, R2Storage storage, R2Properties properties,
                                  UploadReferenceParser parser, Clock clock) {
        this.repository = repository;
        this.storage = storage;
        this.properties = properties;
        this.parser = parser;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void attach(References batch) {
        if (batch == null) {
            return;
        }
        List<FileReference> pairs = new ArrayList<>();
        TreeSet<String> keys = new TreeSet<>();
        for (File file : batch.files()) {
            if (file == null) {
                continue;
            }
            String urlKey = parser.managedKey(file.getFileContent()).orElse(null);
            String fileKey = parser.fileKey(file.getFileName());
            pairs.add(new FileReference(fileKey, urlKey));
            if (urlKey != null) {
                keys.add(urlKey);
            }
            if (fileKey != null) {
                keys.add(fileKey);
            }
        }
        for (String url : batch.urls()) {
            parser.managedKey(url).ifPresent(keys::add);
        }
        for (String content : batch.contents()) {
            for (String url : parser.contentUrls(content)) {
                parser.managedKey(url).ifPresent(keys::add);
            }
        }
        // Every reference of one domain save is locked in a single sorted pass. Splitting a save
        // into multiple attach calls can invert lock order and deadlock concurrent saves.
        Map<String, UploadAsset> locked = lock(keys);
        for (FileReference pair : pairs) {
            if ((locked.containsKey(pair.fileKey()) || locked.containsKey(pair.urlKey()))
                    && !Objects.equals(pair.fileKey(), pair.urlKey())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Upload filename and URL do not match");
            }
        }
        locked.values().forEach(this::attach);
    }

    private Map<String, UploadAsset> lock(TreeSet<String> keys) {
        Map<String, UploadAsset> locked = new HashMap<>();
        // All callers, including cleanup, use this same database row lock. Unknown keys stay legacy.
        for (String key : keys) {
            repository.lockByObjectKey(key).ifPresent(asset -> {
                if (!key.equals(asset.getObjectKey())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Upload key is not canonical");
                }
                locked.put(key, asset);
            });
        }
        return locked;
    }

    private void attach(UploadAsset asset) {
        properties.validateScope(asset.getBucketName(), asset.getObjectKey());
        if (!properties.getEnvironment().equals(asset.getEnvironment())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Upload belongs to another environment");
        }
        if (asset.getState() == UploadAsset.State.ATTACHED) {
            return;
        }
        if (asset.getState() != UploadAsset.State.PENDING || !asset.getExpiresAt().isAfter(clock.instant())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Upload reservation is no longer attachable");
        }
        var user = SecurityUtils.getCurrentUser();
        if (user == null || !asset.getOwnerId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Upload belongs to another user");
        }
        R2Storage.Metadata metadata = storage.head(asset.getBucketName(), asset.getObjectKey());
        if (metadata.size() != asset.getSize() || !asset.getContentType().equalsIgnoreCase(metadata.contentType())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Uploaded size or content type does not match reservation");
        }
        if (!asset.getExpiresAt().isAfter(clock.instant())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Upload reservation has expired");
        }
        // Dirty checking is committed (or rolled back) with the caller's domain persistence.
        asset.attach();
    }

    private record FileReference(String fileKey, String urlKey) { }
}
