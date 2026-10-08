package org.crochet.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RoleType {
    USER("USER"),
    PREMIUM_USER("PREMIUM_USER"),
    ADMIN("ADMIN");

    private final String value;
}
