package org.crochet.repository;

import org.crochet.enums.TargetType;
import org.crochet.model.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface LikeRepository extends JpaRepository<Like, String> {
    Optional<Like> findByUserIdAndTargetIdAndTargetType(String userId, String targetId, TargetType targetType);

    boolean existsByUserIdAndTargetIdAndTargetType(String userId, String targetId, TargetType targetType);

    long countByTargetIdAndTargetType(String targetId, TargetType targetType);

    @Query("SELECT l.targetId FROM Like l WHERE l.user.id = :userId AND l.targetType = :targetType AND l.targetId IN :targetIds")
    List<String> findLikedTargetIds(@Param("userId") String userId,
            @Param("targetType") TargetType targetType,
            @Param("targetIds") Collection<String> targetIds);

    @Modifying
    @Query("DELETE FROM Like l WHERE l.user.id = :userId AND l.targetId = :targetId AND l.targetType = :targetType")
    void deleteByUserIdAndTargetIdAndTargetType(@Param("userId") String userId,
                                                  @Param("targetId") String targetId,
                                                  @Param("targetType") TargetType targetType);
}
