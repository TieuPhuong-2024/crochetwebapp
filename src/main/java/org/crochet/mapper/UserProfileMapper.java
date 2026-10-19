package org.crochet.mapper;

import org.crochet.model.User;
import org.crochet.model.UserProfile;
import org.crochet.payload.request.UserProfileRequest;
import org.crochet.payload.response.UserProfileResponse;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserProfileMapper {
    UserProfileMapper INSTANCE = Mappers.getMapper(UserProfileMapper.class);

    @Mapping(target = "id", source = "user.id")
    @Mapping(target = "name", source = "user.name")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "imageUrl", source = "user.imageUrl")
    @Mapping(target = "provider", source = "user.provider")
    @Mapping(target = "emailVerified", source = "user.emailVerified")
    @Mapping(target = "role", source = "user.role")
    @Mapping(target = "phone", source = "userProfile.phone")
    @Mapping(target = "birthDate", source = "userProfile.birthDate")
    @Mapping(target = "gender", source = "userProfile.gender")
    @Mapping(target = "backgroundImageUrl", source = "userProfile.backgroundImageUrl")
    @Mapping(target = "collections", ignore = true)
    @Mapping(target = "recentComments", ignore = true)
    UserProfileResponse toResponse(User user, UserProfile userProfile);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    void updateUserProfile(UserProfileRequest request, @MappingTarget UserProfile userProfile);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    void updateUserInfo(UserProfileRequest request, @MappingTarget User user);
}
