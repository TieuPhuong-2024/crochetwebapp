package org.crochet.mapper;

import org.crochet.model.Comment;
import org.crochet.payload.response.CommentResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import java.util.Collection;
import java.util.List;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CommentMapper {
    CommentMapper INSTANCE = Mappers.getMapper(CommentMapper.class);
    
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.username", target = "username")
    @Mapping(target = "userAvatar", expression = "java(comment.getUser().getUserProfile() != null ? comment.getUser().getUserProfile().getBackgroundImageUrl() : null)")
    @Mapping(source = "parent.id", target = "parentId")
    @Mapping(source = "mentionedUserId", target = "mentionedUserId")
    @Mapping(target = "replies", ignore = true)
    @Mapping(target = "replyCount", ignore = true)
    @Mapping(target = "mentionedUsername", ignore = true)
    CommentResponse toResponse(Comment comment);
    
    List<CommentResponse> toResponse(Collection<Comment> comments);
}