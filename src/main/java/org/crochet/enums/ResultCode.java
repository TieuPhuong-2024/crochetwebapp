package org.crochet.enums;

import org.springframework.http.HttpStatus;

public enum ResultCode {

    ACTIVE_NOW(1, "Active now", HttpStatus.OK),
    MSG_ACCOUNT_ACTIVATION_LINK(2, "Thank you for registering. Please click on the below link to activate your account:", HttpStatus.OK),
    CONFIRM_YOUR_EMAIL(3, "Confirm your email", HttpStatus.OK),
    MSG_CATEGORY_NOT_FOUND(5, "Category not found", HttpStatus.NOT_FOUND),
    MSG_EMAIL_NOT_VERIFIED(6, "Email not verified", HttpStatus.BAD_REQUEST),
    MSG_INCORRECT_PASSWORD(7, "Incorrect password", HttpStatus.UNAUTHORIZED),
    MSG_EMAIL_ALREADY_IN_USE(8, "Email address already in use", HttpStatus.BAD_REQUEST),
    MSG_USER_NOT_FOUND_WITH_EMAIL(9, "User not found with email ", HttpStatus.NOT_FOUND),
    MSG_USER_NOT_FOUND_WITH_ID(10, "User not found with id ", HttpStatus.NOT_FOUND),
    MSG_USER_REGISTER_SUCCESS(11, "User register success", HttpStatus.OK),
    MSG_RESEND_SUCCESS(12, "Resend success", HttpStatus.OK),
    MSG_EMAIL_ALREADY_CONFIRMED(13, "Email already confirmed", HttpStatus.BAD_REQUEST),
    MSG_TOKEN_EXPIRED(14, "Token expired", HttpStatus.UNAUTHORIZED),
    MSG_SUCCESSFUL_CONFIRMATION(15, "Xác thực thành công (Verified success)", HttpStatus.OK),
    RESET_PASSWORD_LINK(16, "Send successfully with link reset password: ", HttpStatus.OK),
    MSG_PASSWORD_RESET_TOKEN_EXPIRED(17, "Password reset token is expired", HttpStatus.BAD_REQUEST),
    MSG_RESET_PASSWORD_SUCCESS(18, "Reset password success", HttpStatus.OK),
    REFRESH_TOKEN_NOT_IN_DB(19, "Refresh Token is not in DB..!!", HttpStatus.UNAUTHORIZED),
    MSG_BLOG_NOT_FOUND(20, "Blog not found", HttpStatus.NOT_FOUND),
    MSG_USER_NOT_FOUND(22, "User not found", HttpStatus.NOT_FOUND),
    MSG_COMMENT_NOT_FOUND(23, "Comment not found", HttpStatus.NOT_FOUND),
    MSG_CONFIRM_TOKEN_NOT_FOUND(24, "Token not found", HttpStatus.NOT_FOUND),
    MSG_FAILED_SEND_EMAIL(25, "Failed to send email", HttpStatus.INTERNAL_SERVER_ERROR),
    MSG_FREE_PATTERN_NOT_FOUND(26, "Free pattern not found", HttpStatus.NOT_FOUND),
    MSG_PATTERN_NOT_FOUND(27, "Pattern not found", HttpStatus.NOT_FOUND),
    MSG_USER_NOT_FOUND_WITH_TOKEN(31, "User not found with token: ", HttpStatus.NOT_FOUND),
    MSG_PASSWORD_RESET_TOKEN_NOT_FOUND(32, "Password reset token not found", HttpStatus.NOT_FOUND),
    MSG_USER_LOGIN_REQUIRED(33, "User not logged in", HttpStatus.UNAUTHORIZED),
    MSG_PRODUCT_NOT_FOUND(35, "Product not found", HttpStatus.NOT_FOUND),
    MSG_REFRESH_TOKEN_EXPIRED(36, "Refresh token is expired. Please make a new login..!", HttpStatus.UNAUTHORIZED),
    MSG_REFRESH_TOKEN_NOT_FOUND(37, "Refresh token not found: ", HttpStatus.NOT_FOUND),
    ERROR_PARENT_CATEGORY_EXISTS(38, "A category with the same name already exists as a parent.", HttpStatus.BAD_REQUEST),
    ERROR_CHILD_CATEGORY_EXISTS(39, "A category with the same name already exists as a child.", HttpStatus.BAD_REQUEST),
    ERROR_IMAGE_UPLOAD_FAILED(40, "Cannot upload image to Firebase Cloud Storage", HttpStatus.INTERNAL_SERVER_ERROR),
    MSG_BANNER_TYPE_NOT_FOUND(41, "Banner type not found", HttpStatus.NOT_FOUND),
    MSG_BANNER_NOT_FOUND(42, "Banner not found", HttpStatus.NOT_FOUND),
    MSG_BLOG_CATEGORY_NOT_FOUND(43, "Blog category not found.", HttpStatus.NOT_FOUND),
    MSG_SETTINGS_NOT_FOUND(44, "Settings not found", HttpStatus.NOT_FOUND),
    MSG_FORBIDDEN(45, "No permission to access this resource", HttpStatus.FORBIDDEN),
    MSG_COLLECTION_NOT_FOUND(47, "Collection not found", HttpStatus.NOT_FOUND),
    MSG_NO_PERMISSION_MODIFY_COLLECTION(49, "No permission to modify this collection", HttpStatus.FORBIDDEN),
    MSG_NO_PERMISSION_DELETE_COLLECTION(51, "No permission to delete this collection", HttpStatus.FORBIDDEN),
    MSG_NO_PERMISSION_REMOVE_FREE_PATTERN_FROM_COLLECTION(52, "No permission to remove free pattern from collection", HttpStatus.FORBIDDEN),
    MSG_DELETE_SUCCESS(53, "Delete successfully", HttpStatus.OK),
    MSG_CREATE_OR_UPDATE_SUCCESS(54, "Create or update successfully", HttpStatus.OK),
    DATA_INTEGRITY_VIOLATION(55, "Data integrity violation", HttpStatus.INTERNAL_SERVER_ERROR),
    RESET_NOTIFICATION(56, "Reset notification", HttpStatus.OK),
    MSG_RESET_PASSWORD_LINK(57, "Reset password link", HttpStatus.OK),
    RESET_PASSWORD(58, "Reset password", HttpStatus.OK),
    MSG_DUPLICATE_CATEGORY_NAME_UNDER_PROVIDED_PARENTS(59, "Duplicate category name under provided parents", HttpStatus.BAD_REQUEST),
    MSG_LOGOUT_SUCCESS(60, "Logged out success", HttpStatus.OK),
    MSG_NO_PERMISSION_UPDATE_CATEGORY(62, "No permission to update this category", HttpStatus.FORBIDDEN),
    MSG_NO_PERMISSION(63, "No permission", HttpStatus.FORBIDDEN),
    MSG_NO_PERMISSION_DELETE_CATEGORY(61, "No permission to delete this category", HttpStatus.FORBIDDEN),
    INTERNAL_SERVER_ERROR(64, "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR),
    UNAUTHORIZED_ERROR(65, "Unauthorized", HttpStatus.UNAUTHORIZED);

    private final int code;
    private final String message;
    private final HttpStatus status;

    ResultCode(int code, String message, HttpStatus status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }

    public int code() {
        return code;
    }

    public String message() {
        return message;
    }

    public HttpStatus status() {
        return status;
    }

    public static ResultCode fromCode(int code) {
        for (ResultCode rc : ResultCode.values()) {
            if (rc.code == code) {
                return rc;
            }
        }
        return INTERNAL_SERVER_ERROR;
    }
}
