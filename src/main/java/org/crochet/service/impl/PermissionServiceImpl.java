package org.crochet.service.impl;

import org.crochet.enums.ResultCode;
import org.crochet.enums.RoleType;
import org.crochet.exception.AccessDeniedException;
import org.crochet.exception.ResourceNotFoundException;
import org.crochet.model.BaseEntity;
import org.crochet.service.PermissionService;
import org.crochet.util.SecurityUtils;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class PermissionServiceImpl implements PermissionService {
    @Override
    public void checkUserPermission(BaseEntity entity, String action) {
        validateUserLoggedIn();

        if (!isAdmin() && !isOwner(entity)) {
            throw new AccessDeniedException(
                    String.format("Không có quyền %s %s", action, entity.getClass().getSimpleName()),
                    ResultCode.MSG_NO_PERMISSION.code()
            );
        }
    }

    @Override
    public void validateUserLoggedIn() {
        var user = SecurityUtils.getCurrentUser();
        if (user == null) {
            throw new ResourceNotFoundException(
                    ResultCode.MSG_USER_LOGIN_REQUIRED.message(),
                    ResultCode.MSG_USER_LOGIN_REQUIRED.code()
            );
        }
    }

    @Override
    public boolean isAdmin() {
        var user = SecurityUtils.getCurrentUser();
        return user != null && user.getRole() == RoleType.ADMIN;
    }

    @Override
    public boolean isOwner(BaseEntity entity) {
        var user = SecurityUtils.getCurrentUser();
        return user != null && Objects.equals(entity.getCreatedBy(), user.getId());
    }
}
