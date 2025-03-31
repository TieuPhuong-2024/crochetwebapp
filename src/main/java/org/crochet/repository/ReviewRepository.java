package org.crochet.repository;

import org.crochet.model.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, String> {
    // Tìm review theo freePatternId và userId
    Optional<Review> findByFreePatternIdAndUserId(String freePatternId, String userId);
    
    // Lấy danh sách reviews theo freePatternId
    Page<Review> findByFreePatternIdOrderByCreatedDateDesc(String freePatternId, Pageable pageable);
    
    // Đếm số lượng reviews theo freePatternId
    long countByFreePatternId(String freePatternId);
    
    // Tính rating trung bình theo freePatternId
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.freePattern.id = :freePatternId")
    Double getAverageRatingByFreePatternId(@Param("freePatternId") String freePatternId);
    
    // Lấy danh sách reviews của một user
    Page<Review> findByUserIdOrderByCreatedDateDesc(String userId, Pageable pageable);
    
    // Lấy reviews gần đây nhất của một user
    @Query("SELECT r FROM Review r WHERE r.user.id = :userId ORDER BY r.createdDate DESC")
    List<Review> findRecentReviewsByUserId(@Param("userId") String userId, Pageable pageable);
} 