package org.crochet.service;

import org.crochet.payload.response.CollectionResponse;

import java.util.List;

public interface CollectionService {
    void addFreePatternToCollection(String collectionId, String freePatternId);

    void createCollection(String name);

    void updateCollection(String collectionId, String name);

    void removeFreePatternFromCollection(String freePatternId);

    CollectionResponse getCollectionById(String userId, String collectionId);

    List<CollectionResponse> getAllByUserId(String userId);

    void deleteCollection(String collectionId);

    boolean checkFreePatternInCollection(String freePatternId);
    
    /**
     * Check if multiple free patterns are in collections for the current user
     * This method is optimized to avoid N+1 query problem
     *
     * @param freePatternIds list of free pattern ids to check
     * @return Map of free pattern id to boolean indicating if it's in a collection
     */
    java.util.Map<String, Boolean> checkFreePatternsInCollection(java.util.Set<String> freePatternIds);
}
