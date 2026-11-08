package org.crochet.repository;

import jakarta.persistence.LockModeType;
import org.crochet.model.RefreshToken;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepo extends JpaRepository<RefreshToken, String> {
    @EntityGraph(attributePaths = {"user"})
    Optional<RefreshToken> findByToken(String token);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"user"})
    @Query("select r from RefreshToken r where r.token = :token")
    Optional<RefreshToken> findByTokenForUpdate(@Param("token") String token);

    @Query("""
            select r
            from RefreshToken r
            where r.user.id = ?1
            and r.expiresAt > current_timestamp
            and r.revoked = false
            """)
    List<RefreshToken> findAllValidRefreshTokenByUserId(String userId);

    @EntityGraph(attributePaths = {"user"})
    Optional<RefreshToken> findFirstByUser_IdAndRevokedFalseAndExpiresAtAfterOrderByExpiresAtDesc(
            String userId,
            java.time.LocalDateTime now);
}