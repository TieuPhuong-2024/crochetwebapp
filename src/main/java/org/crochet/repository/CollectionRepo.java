package org.crochet.repository;

import org.crochet.model.Collection;
import org.crochet.payload.response.CollectionResponse;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface CollectionRepo extends JpaRepository<Collection, String> {
      @Query("""
                SELECT new org.crochet.payload.response.CollectionResponse(
                    c.id, c.name, c.avatar, COUNT(cf.id), c.user.id
                )
                FROM Collection c
                LEFT JOIN ColFrep cf ON cf.collection.id = c.id
                WHERE c.user.id = :userId
                GROUP BY c.id, c.name, c.avatar, c.user.id, c.createdDate
                ORDER BY c.createdDate DESC
              """)
    List<CollectionResponse> getAllByUserId(@Param("userId") String userId);

    @Query("""
            SELECT
              new org.crochet.payload.response.CollectionResponse (c.id, c.name, c.avatar, COUNT(cf.id), c.user.id)
            FROM
              Collection c
              LEFT JOIN ColFrep cf ON cf.collection.id = c.id
            WHERE
              c.id = :cid AND c.user.id = :userId
            GROUP BY
              c.id,
              c.name,
              c.avatar,
              c.user.id
            """)
    Optional<CollectionResponse> getColById(@Param("userId") String userId,
                                            @Param("cid") String cid);

    @Query("""
            SELECT
              c
            FROM
              Collection c
            LEFT JOIN FETCH c.user
            WHERE
              c.id = :cid
            """)
    Optional<Collection> findColById(@Param("cid") String cid);

    @EntityGraph(attributePaths = {"user", "colfreps", "colfreps.freePattern", "colfreps.freePattern.images"})
    @Query("""
            SELECT
              c
            FROM
              Collection c
            WHERE
              c.id = :cid
            """)
    Optional<Collection> findColWithDetailsById(@Param("cid") String cid);

    @Query("""
            SELECT COUNT(c.id) > 0
            FROM Collection c
            WHERE c.name = :name AND c.user.id = :userId
            """)
    boolean existsCollectionByName(@Param("userId") String userId, @Param("name") String collectionName);

    // Batch delete collections by IDs for a specific user
    @Query("DELETE FROM Collection c WHERE c.id IN :collectionIds AND c.user.id = :userId")
    void deleteByIdsAndUser(@Param("collectionIds") Set<String> collectionIds, @Param("userId") String userId);

    // Find collection names by user for validation
    @Query("SELECT c.name FROM Collection c WHERE c.user.id = :userId")
    Set<String> findCollectionNamesByUser(@Param("userId") String userId);

    // Count total collections for a user
    @Query("SELECT COUNT(c.id) FROM Collection c WHERE c.user.id = :userId")
    long countByUserId(@Param("userId") String userId);
}