package org.crochet.payload.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileRequest {
    private String name;
    private String imageUrl;
    private String phone;
    private LocalDate birthDate;
    private String gender;
    private String backgroundImageUrl;
}
