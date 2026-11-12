package org.crochet.util;

import lombok.experimental.UtilityClass;
import org.crochet.model.Comment;

@UtilityClass
public class CommentUtils {

    public String getLink(Comment comment) {
        if (comment != null) {
            if (comment.getBlogPost() != null) {
                return "/blogs/" + comment.getBlogPost().getId();
            } else if (comment.getFreePattern() != null) {
                return "/free-patterns/" + comment.getFreePattern().getId();
            } else {
                return "/shop/" + comment.getProduct().getId();
            }
        }
        return null;
    }

    public String getMessage(Comment comment) {
        if (comment != null) {
            var username = comment.getUser().getName();
            if (comment.getBlogPost() != null) {
                return username + " đã bình luận về bài viết của bạn";
            } else if (comment.getFreePattern() != null) {
                return username + " đã bình luận về chart của bạn";
            } else {
                return username + " đã bình luận về sản phẩm của bạn";
            }
        }
        return null;
    }
}
