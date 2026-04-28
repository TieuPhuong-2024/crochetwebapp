package org.crochet.service.impl;

import lombok.RequiredArgsConstructor;
import org.crochet.enums.ResultCode;
import org.crochet.exception.AccessDeniedException;
import org.crochet.exception.BadRequestException;
import org.crochet.exception.ResourceNotFoundException;
import org.crochet.model.ColFrep;
import org.crochet.model.Collection;
import org.crochet.model.FreePattern;
import org.crochet.model.User;
import org.crochet.payload.response.CollectionResponse;
import org.crochet.repository.ColFrepRepo;
import org.crochet.repository.CollectionRepo;
import org.crochet.repository.FreePatternRepository;
import org.crochet.service.CollectionAvatarService;
import org.crochet.service.CollectionService;
import org.crochet.util.SecurityUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CollectionServiceImpl implements CollectionService {
    private final CollectionRepo collectionRepo;
    private final FreePatternRepository freePatternRepository;
    private final ColFrepRepo colFrepRepo;
    private final CollectionAvatarService avatarService;

    /**
     * Add a free pattern to a collection
     *
     * @param collectionId  collection id
     * @param freePatternId free pattern id
     */
    @Override
    @CacheEvict(value = "userCollections", key = "#root.target.getCurrentUserId()")
    public void addFreePatternToCollection(String collectionId, String freePatternId) {
        var userId = getCurrentUserId();

        if (colFrepRepo.existsByFreePatternAndUserOptimized(freePatternId, userId)) {
            throw new BadRequestException("Free pattern already exists in user collections");
        }

        // Get collection with user pre-fetched (avoids extra LAZY load query)
        var collection = collectionRepo.findColById(collectionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ResultCode.MSG_COLLECTION_NOT_FOUND.message(),
                        ResultCode.MSG_COLLECTION_NOT_FOUND.code()));

        if (!collection.getUser().getId().equals(userId)) {
            throw new AccessDeniedException(
                    ResultCode.MSG_NO_PERMISSION_MODIFY_COLLECTION.message(),
                    ResultCode.MSG_NO_PERMISSION_MODIFY_COLLECTION.code());
        }

        // Check if this will be the first item in the collection to determine avatar
        // update
        boolean isFirstItem = !colFrepRepo.existsByCollectionId(collectionId);

        FreePattern freePattern = freePatternRepository.findById(freePatternId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ResultCode.MSG_FREE_PATTERN_NOT_FOUND.message(),
                        ResultCode.MSG_FREE_PATTERN_NOT_FOUND.code()));

        ColFrep colFrep = new ColFrep();
        colFrep.setCollection(collection);
        colFrep.setFreePattern(freePattern);
        colFrepRepo.save(colFrep);

        // Update avatar only if this is the first pattern added to the collection
        if (isFirstItem) {
            avatarService.updateAvatar(collection, freePattern);
        }
    }

    /**
     * Creates a new collection with the specified name for the current user.
     *
     * @param name the name of the collection to be created
     * @throws BadRequestException       if a collection with the given name already
     *                                   exists
     * @throws ResourceNotFoundException if the user associated with the current
     *                                   session cannot be found
     */
    @Override
    @CacheEvict(value = "userCollections", key = "#root.target.getCurrentUserId()")
    public void createCollection(String name) {
        var user = requireCurrentUser();

        if (collectionRepo.existsCollectionByName(user.getId(), name)) {
            throw new BadRequestException("Collection name already exists");
        }

        Collection collection = new Collection();
        collection.setName(name);
        collection.setUser(user);

        collectionRepo.save(collection);
    }

    /**
     * Update a collection
     *
     * @param collectionId collection id
     * @param name         update collection request
     */
    @Override
    @CacheEvict(value = "userCollections", key = "#root.target.getCurrentUserId()")
    public void updateCollection(String collectionId, String name) {
        var user = requireCurrentUser();

        if (collectionRepo.existsCollectionByName(user.getId(), name)) {
            throw new BadRequestException("Collection name already exists");
        }

        int updated = collectionRepo.updateCollectionName(collectionId, user.getId(), name);
        if (updated == 0) {
            throw new ResourceNotFoundException(
                    ResultCode.MSG_COLLECTION_NOT_FOUND.message(),
                    ResultCode.MSG_COLLECTION_NOT_FOUND.code());
        }
    }

    /**
     * Remove a free pattern from a collection
     *
     * @param freePatternId free pattern id
     */
    @Override
    @CacheEvict(value = "userCollections", key = "#root.target.getCurrentUserId()")
    public void removeFreePatternFromCollection(String freePatternId) {
        var user = requireCurrentUser();
        var collection = colFrepRepo.findCollectionByUserAndFreePattern(user.getId(), freePatternId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ResultCode.MSG_COLLECTION_NOT_FOUND.message(),
                        ResultCode.MSG_COLLECTION_NOT_FOUND.code()));
        colFrepRepo.removeByFreePatternAndCollectionId(freePatternId, collection.getId());
        avatarService.updateAvatarFromNextPattern(collection);
    }

    /**
     * Get a collection by id
     *
     * @param collectionId collection id
     * @return collection
     */
    @Override
    @Transactional(readOnly = true)
    public CollectionResponse getCollectionById(String userId, String collectionId) {
        return collectionRepo.getColById(userId, collectionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ResultCode.MSG_COLLECTION_NOT_FOUND.message(),
                        ResultCode.MSG_COLLECTION_NOT_FOUND.code()));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "userCollections", key = "#userId")
    public List<CollectionResponse> getAllByUserId(String userId) {
        return collectionRepo.getAllByUserId(userId);
    }

    /**
     * Deletes a collection associated with the specified collection ID for the
     * currently logged-in user.
     * Ensures the user has the necessary permissions to delete the requested
     * collection.
     *
     * @param collectionId the unique identifier of the collection to be deleted
     * @throws ResourceNotFoundException if the current user cannot be retrieved
     * @throws AccessDeniedException     if the user does not have permission to
     *                                   modify the specified collection
     */
    @Override
    @CacheEvict(value = "userCollections", key = "#root.target.getCurrentUserId()")
    public void deleteCollection(String collectionId) {
        var user = requireCurrentUser();

        int deleted = collectionRepo.deleteByIdAndUserId(collectionId, user.getId());
        if (deleted == 0) {
            throw new ResourceNotFoundException(
                    ResultCode.MSG_COLLECTION_NOT_FOUND.message(),
                    ResultCode.MSG_COLLECTION_NOT_FOUND.code());
        }
    }

    /**
     * Helper method to get current user ID for cache eviction
     */
    public String getCurrentUserId() {
        var user = SecurityUtils.getCurrentUser();
        return user != null ? user.getId() : null;
    }

    /**
     * Helper method to get the current authenticated user or throw an exception.
     *
     * @return the current authenticated User
     * @throws ResourceNotFoundException if no user is currently authenticated
     */
    private User requireCurrentUser() {
        var user = SecurityUtils.getCurrentUser();
        if (user == null) {
            throw new ResourceNotFoundException(
                    ResultCode.MSG_USER_NOT_FOUND.message(),
                    ResultCode.MSG_USER_NOT_FOUND.code());
        }
        return user;
    }
}