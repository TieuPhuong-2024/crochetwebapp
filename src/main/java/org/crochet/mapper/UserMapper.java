package org.crochet.mapper;

import org.crochet.model.User;
import org.crochet.payload.request.SignUpRequest;
import org.crochet.payload.response.AuthResponse;
import org.crochet.payload.response.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface UserMapper {
    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);

    User toEntity(UserResponse userResponse);

    UserResponse toResponse(User user);

    List<UserResponse> toResponses(List<User> users);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "emailVerified", constant = "false")
    @Mapping(target = "provider", expression = "java(org.crochet.enums.AuthProvider.LOCAL)")
    @Mapping(target = "role", expression = "java(org.crochet.enums.RoleType.USER)")
    User toEntity(SignUpRequest request);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "imageUrl", source = "user.imageUrl")
    @Mapping(target = "role", expression = "java(user.getRole() != null ? user.getRole().getValue() : null)")
    @Mapping(target = "accessToken", source = "accessToken")
    @Mapping(target = "refreshToken", source = "refreshToken")
    @Mapping(target = "tokenType", constant = "Bearer")
    AuthResponse toAuthResponse(User user, String accessToken, String refreshToken);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "imageUrl", source = "user.imageUrl")
    @Mapping(target = "role", expression = "java(user.getRole() != null ? user.getRole().getValue() : null)")
    @Mapping(target = "accessToken", source = "accessToken")
    @Mapping(target = "refreshToken", source = "refreshToken")
    @Mapping(target = "tokenType", constant = "Bearer")
    AuthResponse toAuthResponse(UserResponse user, String accessToken, String refreshToken);
}