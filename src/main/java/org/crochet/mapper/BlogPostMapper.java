package org.crochet.mapper;

import org.crochet.model.BlogPost;
import org.crochet.payload.request.BlogPostRequest;
import org.crochet.payload.response.BlogPostResponse;
import org.crochet.util.ImageUtils;
import org.crochet.util.ObjectUtils;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = { FileMapper.class })
public interface BlogPostMapper extends PartialUpdate<BlogPost, BlogPostRequest> {
    BlogPostMapper INSTANCE = Mappers.getMapper(BlogPostMapper.class);

    @Mapping(target = "isHome", source = "home")
    BlogPostResponse toResponse(BlogPost blogPost);

    default List<BlogPostResponse> toResponses(Collection<BlogPost> blogPosts) {
        return Optional.ofNullable(blogPosts)
                .map(blogs -> blogs.stream()
                        .map(this::toResponse)
                        .toList())
                .orElse(null);
    }

    @Override
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "files", ignore = true)
    void partialUpdate(BlogPostRequest request, @MappingTarget BlogPost post);

    @AfterMapping
    default void afterPartialUpdate(BlogPostRequest request, @MappingTarget BlogPost post) {
        if (request.getFiles() != null) {
            if (ObjectUtils.isEmpty(request.getFiles())) {
                post.setFiles(null);
            } else {
                var sortedFiles = ImageUtils.sortFiles(request.getFiles());
                post.setFiles(FileMapper.INSTANCE.toEntities(sortedFiles));
            }
        }
    }
}
