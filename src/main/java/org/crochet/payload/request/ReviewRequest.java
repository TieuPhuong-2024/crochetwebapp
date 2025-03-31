package org.crochet.payload.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewRequest {
    private String id; // ID của review (null nếu tạo mới)
    
    @NotNull
    @NotBlank
    private String freePatternId; // ID của free pattern được đánh giá
    
    @NotNull
    @Min(1)
    @Max(5)
    private Integer rating; // Đánh giá từ 1-5 sao
    
    private String title; // Tiêu đề đánh giá
    
    private String content; // Nội dung đánh giá
} 