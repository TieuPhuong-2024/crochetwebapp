package org.crochet.payload.request;

import lombok.Data;
import org.crochet.enums.RoleType;

@Data
public class UpdateUserRequest {
    private String id;
    private String name;
    private RoleType role;
}
