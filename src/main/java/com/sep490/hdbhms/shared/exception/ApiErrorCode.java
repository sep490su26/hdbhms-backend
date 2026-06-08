package com.sep490.hdbhms.shared.exception;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum ApiErrorCode {
    UNDEFINED(0, "Undefined", "Undefined", HttpStatus.INTERNAL_SERVER_ERROR),
    ACCOUNT_EXISTED(10102, "Account existed", "Account already exists", HttpStatus.CONFLICT),
    INVALID_USERNAME(10103, "Invalid newUsername", "Username must have more than 3 characters", HttpStatus.BAD_REQUEST),
    INVALID_EMAIL(10103, "Invalid phone", "Invalid phone format", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD(10103, "Invalid password", "Password must be at least 8 characters", HttpStatus.BAD_REQUEST),
    INVALID_CREDENTIALS(10503, "Invalid credentials", "Invalid phone or password, try different phone or password", HttpStatus.UNAUTHORIZED),
    ROLE_NOT_FOUND(10103, "Role not found", "Account's role is not found", HttpStatus.NOT_FOUND),
    ACCOUNT_NOT_FOUND(10201, "Account not found", "Account not found", HttpStatus.NOT_FOUND),
    NEW_PASSWORD_IS_EMPTY(10503, "New password is empty", "Can't change password due to new password being empty", HttpStatus.BAD_REQUEST),
    OLD_PASSWORD(10103, "Old password", "New password must not be the last 3 old passwords", HttpStatus.BAD_REQUEST),
    RESET_PASSWORD_NOT_ALLOWED_YET(10507, "Reset password not allowed yet", "Can't change username until 3 days from the latest change", HttpStatus.FORBIDDEN),
    RESET_PASSWORD_TOKEN_EXPIRED(10503, "Reset password sessionId expired", "Given reset password sessionId is expired", HttpStatus.BAD_REQUEST),
    RESET_PASSWORD_TOKEN_NOT_FOUND(10501, "Reset password sessionId not found", "Reset password sessionId not found", HttpStatus.NOT_FOUND),
    RESET_PASSWORD_TOKEN_MISMATCH(10507, "Reset password sessionId mismatch", "Given reset password sessionId doesn't match with requested account", HttpStatus.CONFLICT),
    OTP_CODE_EXPIRED_OR_MISMATCH(10507, "OTP code expired or mismatch", "Given OTP code is expired or mismatch with current session", HttpStatus.BAD_REQUEST),
    OTP_CODE_NOT_FOUND(10501, "OTP code not found", "OTP code not found", HttpStatus.NOT_FOUND),
    IS_OAUTH2_ACCOUNT(10507, "Is OAuth2 account", "Account is an OAUTH2 account therefore can't change password or verify phone", HttpStatus.CONFLICT),
    REFRESH_TOKEN_EXPIRED(10507, "Refresh sessionId expired", "Refresh sessionId is expired", HttpStatus.CONFLICT),
    ACCOUNT_IS_NOT_ACTIVE(10507, "Account is not active", "Account is not in an active state, no modifying operations are allowed", HttpStatus.FORBIDDEN),
    ACCOUNT_IS_NOT_VERIFIED(10507, "Account is not verified", "Account is not verified, some modifying operations are unavailable", HttpStatus.FORBIDDEN),
    ACCOUNT_IS_ALREADY_VERIFIED(10507, "Account is already verified", "Account is already verified, can't verify account again", HttpStatus.CONFLICT),
    SAME_USERNAME(10304, "Same newUsername", "Can't assign new newUsername which is identical to the current newUsername", HttpStatus.BAD_REQUEST),
    CHANGE_USERNAME_NOT_ALLOWED_YET(10304, "Change username not allowed yet", "Can't change username until 30 days from the latest change", HttpStatus.BAD_REQUEST),
    CHANGE_EMAIL_NOT_ALLOWED_YET(10304, "Change phone not allowed yet", "Can't change phone until 48 hours from the latest change", HttpStatus.BAD_REQUEST),
    SAME_EMAIL(10304, "Same phone", "Can't assign new phone which is identical to the current phone", HttpStatus.BAD_REQUEST),
    SAME_PASSWORD(10304, "Same password", "Can't assign new password which is identical to the current password", HttpStatus.BAD_REQUEST),
    SAME_ROLE(10304, "Same role", "Can't assign new role which is identical to the current role", HttpStatus.BAD_REQUEST),
    SAME_STATUS(10304, "Same status", "Can't assign new status which is identical to the current status", HttpStatus.BAD_REQUEST),
    INVALID_JWT_TOKEN(10507, "Invalid JWT sessionId", "An error occurs during parsing a JWT sessionId", HttpStatus.INTERNAL_SERVER_ERROR),
    UNAUTHENTICATED(10505, "Unauthenticated", "Session expired", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(10504, "Unauthorized", "You do not have permission", HttpStatus.FORBIDDEN),
    MD5_DIGEST_ERROR(30203, "MD5 digest error", "An error occurs during digesting a file to MD5", HttpStatus.INTERNAL_SERVER_ERROR),
    VISIT_001(40101, "Visit request not found", "Không tìm thấy lịch xem phòng", HttpStatus.NOT_FOUND),
    VISIT_002(40102, "Invalid room property", "Phòng không thuộc cơ sở đã chọn", HttpStatus.BAD_REQUEST),
    VISIT_003(40103, "Invalid phone", "Số điện thoại không hợp lệ", HttpStatus.BAD_REQUEST),
    VISIT_004(40104, "Invalid visit status", "Trạng thái không hợp lệ", HttpStatus.BAD_REQUEST),
    VISIT_005(40105, "Visit request access denied", "Không có quyền thao tác", HttpStatus.FORBIDDEN),
    VISIT_006(40106, "Missing required visit request field", "Thiếu thông tin bắt buộc", HttpStatus.BAD_REQUEST),
    VISIT_007(40107, "Invalid appointment time", "Ngày giờ hẹn xem phải sau thời gian hiện tại", HttpStatus.BAD_REQUEST),
    DEPOSIT_001(40201, "Invalid deposit occupancy", "Thông tin số người ở không hợp lệ", HttpStatus.BAD_REQUEST),
    ;
    int code;
    String message;
    String details;
    HttpStatusCode statusCode;
}
