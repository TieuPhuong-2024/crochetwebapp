package org.crochet.service.impl;

import org.crochet.enums.ResultCode;
import org.crochet.enums.RoleType;
import org.crochet.exception.ForbiddenException;
import org.crochet.exception.UnauthorizedException;
import org.crochet.model.BaseEntity;
import org.crochet.service.PermissionService;
import org.springframework.stereotype.Service;

import java.util.Objects;

import static org.crochet.util.SecurityUtils.getCurrentUser;

@Service("permissionService")
public class PermissionServiceImpl implements PermissionService {

    @Override
    public void checkUserPermission(BaseEntity entity) {
        validateUserLoggedIn();

        if (!canAccess(entity)) {
            throw new ForbiddenException(
                    ResultCode.MSG_FORBIDDEN.message(),
                    ResultCode.MSG_NO_PERMISSION.code()
            );
        }
    }

    @Override
    public boolean canAccess(BaseEntity entity) {
        var user = getCurrentUser();
        if (user == null) {
            return false;
        }
        return isAdmin() || isOwner(entity);
    }

    @Override
    public void validateUserLoggedIn() {
        var user = getCurrentUser();
        if (user == null) {
            throw new UnauthorizedException(
                    ResultCode.MSG_USER_LOGIN_REQUIRED.message(),
                    ResultCode.MSG_USER_LOGIN_REQUIRED.code()
            );
        }
    }

    @Override
    public boolean isAdmin() {
        var user = getCurrentUser();
        return user != null && user.getRole() == RoleType.ADMIN;
    }

    @Override
    public boolean isOwner(BaseEntity entity) {
        var user = getCurrentUser();
        return user != null && entity != null && Objects.equals(entity.getCreatedBy(), user.getId());
    }
}
