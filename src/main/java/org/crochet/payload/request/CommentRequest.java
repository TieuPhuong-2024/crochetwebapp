package org.crochet.payload.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CommentRequest {
    // ID for update, not required for create
    private String id;
    
    // Các ID đối tượng (chỉ được chỉ định một trong ba loại)
    private String blogPostId;
    private String productId;
    private String freePatternId;
    
    // ID của comment cha (null nếu là root comment)
    private String parentId;
    
    // Nội dung comment
    @NotBlank(message = "Nội dung không được để trống")
    private String content;
    
    // ID của người dùng được mention (có thể null)
    private String mentionedUserId;
}
