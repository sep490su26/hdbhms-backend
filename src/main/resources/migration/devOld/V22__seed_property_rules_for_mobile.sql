SET NAMES utf8mb4;

SET @property_id := (
    SELECT id
    FROM properties
    WHERE property_code = 'HAI_DANG_1'
    ORDER BY id
    LIMIT 1
);
SET @property_id := COALESCE(@property_id, (SELECT id FROM properties ORDER BY id LIMIT 1));

INSERT INTO property_rules
(property_id, rule_code, title, description, default_fine_amount, sort_order, status, created_at, updated_at)
VALUES
(@property_id, 'GENERAL_001',
 'Lưu trú đúng số người đã đăng ký',
 'Mỗi phòng chỉ lưu trú đúng số lượng người đã đăng ký. Mọi thay đổi cần báo trước cho quản lý.',
 NULL, 1, 'ACTIVE', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
(@property_id, 'GENERAL_002',
 'Không kinh doanh hoặc tàng trữ hàng cấm',
 'Không kinh doanh, tàng trữ hàng cấm, chất dễ cháy nổ hoặc vật dụng nguy hiểm trong khu trọ.',
 NULL, 2, 'ACTIVE', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
(@property_id, 'SECURITY_001',
 'Khóa cửa cẩn thận khi ra vào',
 'Không cung cấp mật mã, chìa khóa hoặc thẻ ra vào cho người lạ khi chưa được quản lý đồng ý.',
 NULL, 3, 'ACTIVE', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
(@property_id, 'SECURITY_002',
 'Khách đến thăm cần báo trước',
 'Khách đến thăm cần thông báo cho chủ trọ hoặc quản lý và tuân thủ khung giờ sinh hoạt chung.',
 NULL, 4, 'ACTIVE', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
(@property_id, 'HYGIENE_001',
 'Đổ rác đúng nơi quy định',
 'Không để rác trước cửa phòng, hành lang hoặc khu vực sinh hoạt chung.',
 NULL, 5, 'ACTIVE', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
(@property_id, 'HYGIENE_002',
 'Giữ hành lang thông thoáng',
 'Không để giày dép, xe đạp, thùng đồ hoặc vật dụng cá nhân chắn lối đi chung.',
 NULL, 6, 'ACTIVE', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
(@property_id, 'UTILITY_001',
 'Sử dụng điện nước tiết kiệm',
 'Tắt thiết bị điện, khóa nước và kiểm tra phòng trước khi ra ngoài.',
 NULL, 7, 'ACTIVE', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
(@property_id, 'UTILITY_002',
 'Không tự ý can thiệp công tơ',
 'Không tự ý tháo lắp, di chuyển hoặc can thiệp công tơ điện nước và thiết bị kỹ thuật.',
 NULL, 8, 'ACTIVE', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
(@property_id, 'WIFI_RESET',
 'Không tự ý reset Wi-Fi',
 'Không tự ý reset modem hoặc đổi mật khẩu Wi-Fi khi chưa được phép.',
 200000, 100, 'ACTIVE', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
(@property_id, 'FINE_UNAUTHORIZED_REPAIR',
 'Không tự ý sửa chữa kết cấu phòng',
 'Mọi sửa chữa ảnh hưởng đến điện, nước, tường, cửa hoặc thiết bị cố định cần được quản lý duyệt trước.',
 NULL, 101, 'ACTIVE', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE
    title = VALUES(title),
    description = VALUES(description),
    default_fine_amount = VALUES(default_fine_amount),
    sort_order = VALUES(sort_order),
    status = VALUES(status),
    updated_at = CURRENT_TIMESTAMP(6);
