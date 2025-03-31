package org.crochet.service;

import org.crochet.payload.response.FreePatternResponse;
import org.crochet.payload.response.PaginationResponse;

import java.util.List;

public interface HeartService {
    /**
     * Thả tim hoặc bỏ tim free pattern
     *
     * @param freePatternId ID của free pattern
     * @return true nếu đã thả tim, false nếu đã bỏ tim
     */
    boolean toggleHeart(String freePatternId);
    
    /**
     * Kiểm tra xem người dùng đã thả tim free pattern chưa
     *
     * @param freePatternId ID của free pattern
     * @param userId ID của người dùng
     * @return true nếu đã thả tim, false nếu chưa
     */
    boolean isHearted(String freePatternId, String userId);
    
    /**
     * Đếm số lượng tim cho một free pattern
     *
     * @param freePatternId ID của free pattern
     * @return Số lượng tim
     */
    int countHearts(String freePatternId);
    
    /**
     * Lấy danh sách free patterns được người dùng thả tim
     *
     * @param userId ID của người dùng
     * @param pageNo Số trang
     * @param pageSize Kích thước trang
     * @return PaginationResponse chứa danh sách free patterns
     */
    PaginationResponse<FreePatternResponse> getHeartedFreePatterns(String userId, int pageNo, int pageSize);
    
    /**
     * Lấy danh sách IDs của free patterns được người dùng thả tim
     *
     * @param userId ID của người dùng
     * @return Danh sách các ID
     */
    List<String> getHeartedFreePatternIds(String userId);
    
    /**
     * Bổ sung thông tin thả tim vào FreePatternResponse
     *
     * @param freePatternId ID của free pattern
     * @param response FreePatternResponse cần bổ sung thông tin
     */
    void getHeartInfoForFreePattern(String freePatternId, FreePatternResponse response);
} 