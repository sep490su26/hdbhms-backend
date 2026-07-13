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
    HANDOVER_001(40301, "Handover already confirmed", "Biên bản bàn giao đã được xác nhận", HttpStatus.BAD_REQUEST),

    // --- PORTAL & FILE (20xxx) ---
    HOME_DATA_NOT_FOUND(20101, "Home data not found", "Không tìm thấy dữ liệu trang chủ", HttpStatus.NOT_FOUND),
    FILE_UPLOAD_FAILED(20201, "File upload failed", "Tải tệp lên thất bại", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_DOWNLOAD_FAILED(20202, "File download failed", "Tải tệp xuống thất bại", HttpStatus.INTERNAL_SERVER_ERROR),

    // --- IDENTITY & ACCESS (10xxx) ---
    USER_PROFILE_NOT_FOUND(10202, "User profile not found", "Không tìm thấy hồ sơ người dùng", HttpStatus.NOT_FOUND),
    PERMISSION_REQUEST_NOT_FOUND(10203, "Permission request not found", "Không tìm thấy yêu cầu cấp quyền", HttpStatus.NOT_FOUND),
    EMERGENCY_CONTACT_NOT_FOUND(10204, "Emergency contact not found", "Không tìm thấy liên hệ khẩn cấp", HttpStatus.NOT_FOUND),
    IDENTITY_DOCUMENT_NOT_FOUND(10205, "Identity document not found", "Không tìm thấy giấy tờ tùy thân", HttpStatus.NOT_FOUND),

    // --- OCCUPANCY: PROPERTY & ROOM (404xx) ---
    ROOM_NOT_FOUND(40401, "Room not found", "Không tìm thấy phòng", HttpStatus.NOT_FOUND),
    ROOM_ASSET_NOT_FOUND(40402, "Room asset not found", "Không tìm thấy thiết bị phòng", HttpStatus.NOT_FOUND),
    FLOOR_NOT_FOUND(40403, "Floor not found", "Không tìm thấy tầng", HttpStatus.NOT_FOUND),
    PROPERTY_NOT_FOUND(40404, "Property not found", "Không tìm thấy cơ sở", HttpStatus.NOT_FOUND),
    PROPERTY_RULE_NOT_FOUND(40405, "Property rule not found", "Không tìm thấy nội quy", HttpStatus.NOT_FOUND),
    ROOM_HOLD_NOT_FOUND(40406, "Room hold not found", "Không tìm thấy thông tin giữ phòng", HttpStatus.NOT_FOUND),
    ROOM_IMAGE_NOT_FOUND(40407, "Room image not found", "Không tìm thấy ảnh phòng", HttpStatus.NOT_FOUND),
    PROPERTY_IMAGE_NOT_FOUND(40408, "Property image not found", "Không tìm thấy ảnh cơ sở", HttpStatus.NOT_FOUND),
    ROOM_STATUS_HISTORY_NOT_FOUND(40409, "Room status history not found", "Không tìm thấy lịch sử trạng thái phòng", HttpStatus.NOT_FOUND),
    ROOM_TRANSFER_REQUEST_NOT_FOUND(40410, "Room transfer request not found", "Không tìm thấy yêu cầu chuyển phòng", HttpStatus.NOT_FOUND),

    // --- OCCUPANCY: LEASE & CONTRACT (407xx) ---
    CONTRACT_NOT_FOUND(40701, "Contract not found", "Không tìm thấy hợp đồng", HttpStatus.NOT_FOUND),
    CONTRACT_EVENT_NOT_FOUND(40702, "Contract event not found", "Không tìm thấy sự kiện hợp đồng", HttpStatus.NOT_FOUND),
    CONTRACT_HANDOVER_ITEM_NOT_FOUND(40703, "Handover item not found", "Không tìm thấy thiết bị bàn giao", HttpStatus.NOT_FOUND),
    CONTRACT_HANDOVER_RECORD_NOT_FOUND(40704, "Handover record not found", "Không tìm thấy biên bản bàn giao", HttpStatus.NOT_FOUND),
    CONTRACT_LIQUIDATION_NOT_FOUND(40705, "Liquidation not found", "Không tìm thấy biên bản thanh lý", HttpStatus.NOT_FOUND),
    CONTRACT_OCCUPANT_NOT_FOUND(40706, "Occupant not found", "Không tìm thấy người ở", HttpStatus.NOT_FOUND),
    CONTRACT_TERMINATION_NOTICE_NOT_FOUND(40707, "Termination notice not found", "Không tìm thấy thông báo chấm dứt", HttpStatus.NOT_FOUND),
    TENANT_NOT_FOUND(40708, "Tenant not found", "Không tìm thấy người thuê", HttpStatus.NOT_FOUND),
    LEAD_NOT_FOUND(40709, "Lead not found", "Không tìm thấy khách hàng tiềm năng", HttpStatus.NOT_FOUND),
    RULE_VIOLATION_NOT_FOUND(40710, "Rule violation not found", "Không tìm thấy vi phạm nội quy", HttpStatus.NOT_FOUND),

    // --- OCCUPANCY: DEPOSIT (402xx) ---
    DEPOSIT_AGREEMENT_NOT_FOUND(40201, "Deposit agreement not found", "Không tìm thấy thỏa thuận cọc", HttpStatus.NOT_FOUND),
    DEPOSIT_FORM_NOT_FOUND(40202, "Deposit form not found", "Không tìm thấy biểu mẫu cọc", HttpStatus.NOT_FOUND),
    DEPOSIT_EXTENSION_EVENT_NOT_FOUND(40203, "Deposit extension not found", "Không tìm thấy gia hạn cọc", HttpStatus.NOT_FOUND),

    // --- OCCUPANCY: UTILITY & METER (409xx) ---
    METER_NOT_FOUND(40901, "Meter not found", "Không tìm thấy đồng hồ", HttpStatus.NOT_FOUND),
    METER_READING_NOT_FOUND(40902, "Meter reading not found", "Không tìm thấy chỉ số đồng hồ", HttpStatus.NOT_FOUND),
    METER_READING_BATCH_NOT_FOUND(40903, "Meter reading batch not found", "Không tìm thấy đợt chốt điện nước", HttpStatus.NOT_FOUND),
    METER_READING_IMPORT_ROW_NOT_FOUND(40904, "Meter reading import row not found", "Không tìm thấy dòng import", HttpStatus.NOT_FOUND),
    METER_READING_ANOMALY_NOT_FOUND(40905, "Meter reading anomaly not found", "Không tìm thấy bất thường chỉ số", HttpStatus.NOT_FOUND),
    UTILITY_TARIFF_NOT_FOUND(40906, "Utility tariff not found", "Không tìm thấy bảng giá điện nước", HttpStatus.NOT_FOUND),
    INVALID_METER_READING_VALUE(40907, "Invalid meter reading value", "Chỉ số mới không được nhỏ hơn chỉ số cũ", HttpStatus.BAD_REQUEST),
    METER_READING_BATCH_CANCELLED(40908, "Meter reading batch cancelled", "Đợt chốt điện nước đã bị hủy", HttpStatus.BAD_REQUEST),
    METER_READING_ROOM_NOT_ELIGIBLE(40909, "Meter reading room not eligible", "Phòng không có hợp đồng thuê phát sinh trong kỳ", HttpStatus.BAD_REQUEST),
    METER_READING_NO_ELIGIBLE_ROOMS(40910, "No rooms require meter reading", "Không có phòng cần chốt điện nước trong kỳ này", HttpStatus.BAD_REQUEST),

    // --- BILLING & PAYMENT (30xxx) ---
    TRANSFER_SETTLEMENT_NOT_FOUND(30101, "Transfer settlement not found", "Không tìm thấy quyết toán chuyển phòng", HttpStatus.NOT_FOUND),

    // --- NOTIFICATION (60xxx) ---
    NOTIFICATION_NOT_FOUND(60101, "Notification not found", "Không tìm thấy thông báo", HttpStatus.NOT_FOUND),
    NOTIFICATION_OUTBOX_NOT_FOUND(60102, "Notification outbox not found", "Không tìm thấy thông báo outbox", HttpStatus.NOT_FOUND),
    ;
    int code;
    String message;
    String details;
    HttpStatusCode statusCode;
}
