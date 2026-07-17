package org.crochet.mapper;

import org.crochet.model.Product;
import org.crochet.payload.request.ProductRequest;
import org.crochet.payload.response.ProductResponse;
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
public interface ProductMapper {
    ProductMapper INSTANCE = Mappers.getMapper(ProductMapper.class);

    @Mapping(target = "isHome", source = "home")
    @Mapping(target = "currencyCode", source = "currencyCode")
    ProductResponse toResponse(Product product);

    List<ProductResponse> toResponses(Collection<Product> products);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "images", ignore = true)
    void update(ProductRequest request, @MappingTarget Product product);

    @AfterMapping
    default void afterUpdate(ProductRequest request, @MappingTarget Product product) {
        if (request.getImages() != null) {
            if (ObjectUtils.isEmpty(request.getImages())) {
                product.setImages(null);
            } else {
                var sortedImages = ImageUtils.sortFiles(request.getImages());
                product.setImages(FileMapper.INSTANCE.toEntities(sortedImages));
            }
        }
    }
}
