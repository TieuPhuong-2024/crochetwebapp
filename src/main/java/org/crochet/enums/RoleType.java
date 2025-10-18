package org.crochet.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RoleType {
    USER("USER"),
    VIP_USER("VIP_USER"),
    ADMIN("ADMIN");

    private final String value;
}
