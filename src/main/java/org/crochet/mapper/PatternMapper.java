package org.crochet.mapper;

import org.crochet.model.Pattern;
import org.crochet.payload.request.PatternRequest;
import org.crochet.payload.response.PatternResponse;
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
public interface PatternMapper extends PartialUpdate<Pattern, PatternRequest> {
    PatternMapper INSTANCE = Mappers.getMapper(PatternMapper.class);

    @Mapping(target = "isHome", source = "home")
    @Mapping(target = "currencyCode", source = "currencyCode")
    PatternResponse toResponse(Pattern pattern);

    @Override
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "files", ignore = true)
    void partialUpdate(PatternRequest req, @MappingTarget Pattern pattern);

    @AfterMapping
    default void afterPartialUpdate(PatternRequest req, @MappingTarget Pattern pattern) {
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

    List<PatternResponse> toResponses(Collection<Pattern> patterns);
}
