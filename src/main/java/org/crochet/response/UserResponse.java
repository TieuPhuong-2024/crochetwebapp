package org.crochet.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.crochet.model.AuthProvider;

@Getter
@Setter
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {
  private Long id;
  private String name;
  private String email;
  private String imageUrl;
  private Boolean emailVerified;
  private String password;
  private AuthProvider provider;
  private String providerId;
  private String verificationCode;
}
