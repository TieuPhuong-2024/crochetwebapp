package org.crochet.payload.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.crochet.enums.RoleType;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {
    private String id;
    private String name;
    @Email
    private String email;
    private RoleType role;
    private String imageUrl;
    private Boolean emailVerified;
    private Instant createdAt;
    private Instant lastModifiedAt;
}