package org.crochet.service;

import org.crochet.model.BaseEntity;

public interface PermissionService {
    void checkUserPermission(BaseEntity entity);
    void validateUserLoggedIn();
    boolean isAdmin();
    boolean isOwner(BaseEntity entity);
    boolean canAccess(BaseEntity entity);
}
