package org.crochet.service.impl;

import lombok.RequiredArgsConstructor;
import org.crochet.enums.ResultCode;
import org.crochet.exception.AccessDeniedException;
import org.crochet.exception.BadRequestException;
import org.crochet.exception.ResourceNotFoundException;
import org.crochet.model.ColFrep;
import org.crochet.model.Collection;
import org.crochet.model.FreePattern;
import org.crochet.payload.response.CollectionResponse;
import org.crochet.repository.ColFrepRepo;
import org.crochet.repository.CollectionRepo;
import org.crochet.repository.FreePatternRepository;
import org.crochet.service.CollectionAvatarService;
import org.crochet.service.CollectionService;
import org.crochet.util.ObjectUtils;
import org.crochet.util.SecurityUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        
        // Tối ưu: Sử dụng truy vấn EXISTS thay vì COUNT để kiểm tra nhanh hơn
        if (colFrepRepo.existsByFreePatternAndUserOptimized(freePatternId, userId)) {
            throw new BadRequestException("Free pattern already exists in user collections");
        }

        // Tối ưu: Sử dụng truy vấn có JOIN FETCH để lấy collection và user trong một lần truy vấn
        var collection = collectionRepo.findColWithUserById(collectionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ResultCode.MSG_COLLECTION_NOT_FOUND.message(),
                        ResultCode.MSG_COLLECTION_NOT_FOUND.code()
                ));

        // Kiểm tra quyền sở hữu collection
        if (!collection.getUser().getId().equals(userId)) {
            throw new AccessDeniedException(
                    ResultCode.MSG_NO_PERMISSION_MODIFY_COLLECTION.message(),
                    ResultCode.MSG_NO_PERMISSION_MODIFY_COLLECTION.code()
            );
        }

        FreePattern freePattern = freePatternRepository.findById(freePatternId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ResultCode.MSG_FREE_PATTERN_NOT_FOUND.message(),
                        ResultCode.MSG_FREE_PATTERN_NOT_FOUND.code()
                ));

        ColFrep colFrep = new ColFrep();
        colFrep.setCollection(collection);
        colFrep.setFreePattern(freePattern);
        colFrepRepo.save(colFrep);
        
        // Tối ưu: Sử dụng countByCollectionIdFast thay vì countByCollectionIdOptimized
        long count = colFrepRepo.countByCollectionIdFast(collection.getId());
        if (count == 1) {
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
        var user = SecurityUtils.getCurrentUser();
        if (user == null) {
            throw new ResourceNotFoundException(
                    ResultCode.MSG_USER_NOT_FOUND.message(),
                    ResultCode.MSG_USER_NOT_FOUND.code()
            );
        }

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
    @Cacheable(value = "userCollections", key = "#root.target.getCurrentUserId()")
    public void updateCollection(String collectionId, String name) {
        var user = SecurityUtils.getCurrentUser();
        if (user == null) {
            throw new ResourceNotFoundException(
                    ResultCode.MSG_USER_NOT_FOUND.message(),
                    ResultCode.MSG_USER_NOT_FOUND.code()
            );
        }

        var col = collectionRepo.findColById(collectionId)
                .orElseThrow(() -> new AccessDeniedException(
                        ResultCode.MSG_NO_PERMISSION_MODIFY_COLLECTION.message(),
                        ResultCode.MSG_NO_PERMISSION_MODIFY_COLLECTION.code()
                ));

        if (collectionRepo.existsCollectionByName(user.getId(), name)) {
            throw new BadRequestException("Collection name already exists");
        }

        col.setName(name);
        collectionRepo.save(col);
    }

    /**
     * Remove a free pattern from a collection
     *
     * @param freePatternId free pattern id
     */
    @Override
    @CacheEvict(value = "userCollections", key = "#root.target.getCurrentUserId()")
    public void removeFreePatternFromCollection(String freePatternId) {
        var userId = getCurrentUserId();
        if (userId == null) {
            throw new ResourceNotFoundException(
                    ResultCode.MSG_USER_LOGIN_REQUIRED.message(),
                    ResultCode.MSG_USER_LOGIN_REQUIRED.code()
            );
        }
        
        // Tối ưu: Sử dụng truy vấn có JOIN FETCH để lấy collection trong một lần truy vấn
        var collection = colFrepRepo.findCollectionByUserAndFreePattern(userId, freePatternId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ResultCode.MSG_COLLECTION_NOT_FOUND.message(),
                        ResultCode.MSG_COLLECTION_NOT_FOUND.code()
                ));
                
        // Tối ưu: Sử dụng truy vấn xóa trực tiếp thay vì load entity
        colFrepRepo.removeByFreePattern(freePatternId);
        
        // Cập nhật avatar từ pattern tiếp theo
        avatarService.updateAvatarFromNextPattern(collection);
    }

    /**
     * Get a collection by id
     *
     * @param collectionId collection id
     * @return collection
     */
    @Override
    public CollectionResponse getCollectionById(String userId, String collectionId) {
        return collectionRepo.getColById(userId, collectionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ResultCode.MSG_COLLECTION_NOT_FOUND.message(),
                        ResultCode.MSG_COLLECTION_NOT_FOUND.code()
                ));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "userCollections", key = "#userId")
    public List<CollectionResponse> getAllByUserId(String userId) {
        return collectionRepo.getAllByUserId(userId);
    }

    /**
     * Deletes a collection associated with the specified collection ID for the currently logged-in user.
     * Ensures the user has the necessary permissions to delete the requested collection.
     *
     * @param collectionId the unique identifier of the collection to be deleted
     * @throws ResourceNotFoundException if the current user cannot be retrieved
     * @throws AccessDeniedException     if the user does not have permission to modify the specified collection
     */
    @Override
    @Cacheable(value = "userCollections", key = "#root.target.getCurrentUserId()")
    public void deleteCollection(String collectionId) {
        var user = SecurityUtils.getCurrentUser();
        if (user == null) {
            throw new ResourceNotFoundException(
                    ResultCode.MSG_USER_NOT_FOUND.message(),
                    ResultCode.MSG_USER_NOT_FOUND.code()
            );
        }

        var col = collectionRepo.findColById(collectionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ResultCode.MSG_COLLECTION_NOT_FOUND.message(),
                        ResultCode.MSG_COLLECTION_NOT_FOUND.code()
                ));

        if (ObjectUtils.notEqual(col.getUser().getId(), user.getId())) {
            throw new AccessDeniedException(
                    ResultCode.MSG_NO_PERMISSION_DELETE_COLLECTION.message(),
                    ResultCode.MSG_NO_PERMISSION_DELETE_COLLECTION.code()
            );
        }

        collectionRepo.delete(col);
    }

    /**
     * Check if a free pattern is in a collection
     *
     * @param freePatternId free pattern id
     * @return true if the free pattern is in the collection, false otherwise
     */
    @Override
    @Transactional(readOnly = true)
    public boolean checkFreePatternInCollection(String freePatternId) {
        var userId = getCurrentUserId();
        if (userId == null) {
            throw new ResourceNotFoundException(
                    ResultCode.MSG_USER_LOGIN_REQUIRED.message(),
                    ResultCode.MSG_USER_LOGIN_REQUIRED.code()
            );
        }
        // Tối ưu: Sử dụng truy vấn EXISTS thay vì COUNT để kiểm tra nhanh hơn
        return colFrepRepo.existsByFreePatternAndUserOptimized(freePatternId, userId);
    }

    /**
     * Check if multiple free patterns are in collections for the current user
     * This method is optimized to avoid N+1 query problem
     *
     * @param freePatternIds list of free pattern ids to check
     * @return Map of free pattern id to boolean indicating if it's in a collection
     */
    @Override
    @Transactional(readOnly = true)
    public Map<String, Boolean> checkFreePatternsInCollection(Set<String> freePatternIds) {
        var user = SecurityUtils.getCurrentUser();
        if (user == null) {
            throw new ResourceNotFoundException(
                    ResultCode.MSG_USER_LOGIN_REQUIRED.message(),
                    ResultCode.MSG_USER_LOGIN_REQUIRED.code()
            );
        }
        
        if (freePatternIds == null || freePatternIds.isEmpty()) {
            return Collections.emptyMap();
        }
        
        // Tối ưu: Sử dụng truy vấn đơn để lấy tất cả pattern IDs đã được thêm vào collection
        Set<String> patternsInCollection = colFrepRepo.findPatternIdsInUserCollections(freePatternIds, user.getId());
        
        // Tạo map kết quả với một lần duyệt
        Map<String, Boolean> result = new HashMap<>(freePatternIds.size());
        for (String patternId : freePatternIds) {
            result.put(patternId, patternsInCollection.contains(patternId));
        }
        
        return result;
    }

    /**
     * Helper method để lấy current user ID cho cache eviction
     */
    public String getCurrentUserId() {
        var user = SecurityUtils.getCurrentUser();
        return user != null ? user.getId() : null;
    }
}