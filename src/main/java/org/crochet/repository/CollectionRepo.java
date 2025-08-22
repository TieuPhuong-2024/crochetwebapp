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

    @EntityGraph(attributePaths = {"user"})
    @Query("""
            SELECT c
            FROM Collection c
            WHERE c.id = :cid
            """)
    Optional<Collection> findColWithUserById(@Param("cid") String cid);

    @Query("""
            SELECT COUNT(c.id) > 0
            FROM Collection c
            WHERE c.name = :name AND c.user.id = :userId
            """)
    boolean existsCollectionByName(@Param("userId") String userId, @Param("name") String collectionName);

}