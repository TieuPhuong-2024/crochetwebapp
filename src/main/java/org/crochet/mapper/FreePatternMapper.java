package org.crochet.mapper;

import org.crochet.model.FreePattern;
import org.crochet.payload.request.FreePatternRequest;
import org.crochet.payload.response.FreePatternResponse;
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

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = { FileMapper.class, CategoryMapper.class })
public interface FreePatternMapper {
    FreePatternMapper INSTANCE = Mappers.getMapper(FreePatternMapper.class);

    @Mapping(target = "isHome", source = "home")
    FreePatternResponse toResponse(FreePattern pattern);

    List<FreePatternResponse> toResponses(Collection<FreePattern> freePatterns);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "files", ignore = true)
    void update(FreePatternRequest req, @MappingTarget FreePattern freePattern);

    @AfterMapping
    default void afterUpdate(FreePatternRequest req, @MappingTarget FreePattern pattern) {
        if (req.getImages() != null) {
            if (ObjectUtils.isEmpty(req.getImages())) {
                pattern.setImages(null);
            } else {
                var sortedImages = ImageUtils.sortFiles(req.getImages());
                pattern.setImages(FileMapper.INSTANCE.toEntities(sortedImages));
            }
        }
        if (req.getFiles() != null) {
            if (ObjectUtils.isEmpty(req.getFiles())) {
                pattern.setFiles(null);
            } else {
                var sortedFiles = ImageUtils.sortFiles(req.getFiles());
                pattern.setFiles(FileMapper.INSTANCE.toEntities(sortedFiles));
            }
        }
    }
}