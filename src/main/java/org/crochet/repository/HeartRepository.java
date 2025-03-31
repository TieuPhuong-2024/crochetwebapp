package org.crochet.repository;

import org.crochet.model.Heart;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HeartRepository extends JpaRepository<Heart, String> {
    // Kiểm tra xem user có thả tim cho free pattern không
    boolean existsByFreePatternIdAndUserId(String freePatternId, String userId);
    
    // Tìm heart theo free pattern ID và user ID
    Optional<Heart> findByFreePatternIdAndUserId(String freePatternId, String userId);
    
    // Đếm số lượng tim cho một free pattern
    long countByFreePatternId(String freePatternId);
    
    // Lấy danh sách free patterns được user thả tim (phân trang)
    Page<Heart> findByUserIdOrderByCreatedDateDesc(String userId, Pageable pageable);
    
    // Lấy danh sách free pattern IDs được user thả tim
    @Query("SELECT h.freePattern.id FROM Heart h WHERE h.user.id = :userId")
    List<String> findFreePatternIdsByUserId(@Param("userId") String userId);
} 