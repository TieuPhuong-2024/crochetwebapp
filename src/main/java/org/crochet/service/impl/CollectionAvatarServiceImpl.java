package org.crochet.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.crochet.model.Collection;
import org.crochet.model.FreePattern;
import org.crochet.repository.ColFrepRepo;
import org.crochet.repository.CollectionRepo;
import org.crochet.service.CollectionAvatarService;
import org.crochet.service.UploadLifecycleService;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class CollectionAvatarServiceImpl implements CollectionAvatarService {

    private final CollectionRepo collectionRepo;
    private final ColFrepRepo colFrepRepo;
    private final UploadLifecycleService uploads;

    @Override
    public void updateAvatar(Collection collection, FreePattern freePattern) {
        if (!freePattern.getImages().isEmpty()) {
            var firstImage = freePattern.getImages()
                    .stream()
                    .findFirst()
                    .get()
                    .getFileContent();
            uploads.attach(new UploadLifecycleService.References().urls(firstImage));
            collection.setAvatar(firstImage);
            collectionRepo.save(collection);
        }
    }

    @Override
    public void updateAvatarFromNextPattern(Collection collection) {
        var avatarUrl = colFrepRepo.findFirstAvatarUrlByCollectionId(collection.getId());
        uploads.attach(new UploadLifecycleService.References().urls(avatarUrl.orElse(null)));
        collection.setAvatar(avatarUrl.orElse(null));
        collectionRepo.save(collection);
    }
}
