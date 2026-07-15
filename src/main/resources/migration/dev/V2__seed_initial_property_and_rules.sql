SET NAMES utf8mb4;

INSERT INTO hdbhms.properties (
    property_code,
    name,
    property_type,
    address_line,
    description,
    status,
    created_at,
    updated_at,
    deleted_at,
    version
) VALUES (
    'HAI_DANG_1',
    'Nhà trọ Hải Đăng 1',
    'BOARDING_HOUSE',
    'Số 70A1, Thôn 4, xã Thạch Hòa, Thạch Thất, Hà Nội',
    'Nhà trọ 5 tầng gồm 37 phòng theo sơ đồ PRD: tầng 1 có 6 phòng, tầng 2-4 mỗi tầng 8 phòng, tầng 5 có 7 phòng.',
    'ACTIVE',
    NOW(6),
    NOW(6),
    NULL,
    0
) AS new_property
ON DUPLICATE KEY UPDATE
    name = new_property.name,
    property_type = new_property.property_type,
    address_line = new_property.address_line,
    description = new_property.description,
    status = new_property.status,
    updated_at = NOW(6),
    deleted_at = NULL;

SET @property_id := (
    SELECT property_id
    FROM hdbhms.properties
    WHERE property_code = 'HAI_DANG_1'
    LIMIT 1
);

INSERT INTO hdbhms.floors (
    property_id,
    floor_code,
    name,
    sort_order,
    status,
    created_at,
    updated_at,
    deleted_at
) VALUES
    (@property_id, 'F1', 'Tầng 1', 1, 'ACTIVE', NOW(6), NOW(6), NULL),
    (@property_id, 'F2', 'Tầng 2', 2, 'ACTIVE', NOW(6), NOW(6), NULL),
    (@property_id, 'F3', 'Tầng 3', 3, 'ACTIVE', NOW(6), NOW(6), NULL),
    (@property_id, 'F4', 'Tầng 4', 4, 'ACTIVE', NOW(6), NOW(6), NULL),
    (@property_id, 'F5', 'Tầng 5', 5, 'ACTIVE', NOW(6), NOW(6), NULL) AS new_floor
ON DUPLICATE KEY UPDATE
    name = new_floor.name,
    sort_order = new_floor.sort_order,
    status = new_floor.status,
    updated_at = NOW(6),
    deleted_at = NULL;

SET @floor_1_id := (
    SELECT floor_id
    FROM hdbhms.floors
    WHERE property_id = @property_id
      AND floor_code = 'F1'
    LIMIT 1
);

SET @floor_2_id := (
    SELECT floor_id
    FROM hdbhms.floors
    WHERE property_id = @property_id
      AND floor_code = 'F2'
    LIMIT 1
);

SET @floor_3_id := (
    SELECT floor_id
    FROM hdbhms.floors
    WHERE property_id = @property_id
      AND floor_code = 'F3'
    LIMIT 1
);

SET @floor_4_id := (
    SELECT floor_id
    FROM hdbhms.floors
    WHERE property_id = @property_id
      AND floor_code = 'F4'
    LIMIT 1
);

SET @floor_5_id := (
    SELECT floor_id
    FROM hdbhms.floors
    WHERE property_id = @property_id
      AND floor_code = 'F5'
    LIMIT 1
);

INSERT INTO hdbhms.rooms (
    property_id,
    floor_id,
    room_code,
    name,
    area_m2,
    listed_price,
    current_status,
    max_occupants,
    public_note,
    internal_note,
    position_x,
    position_y,
    sort_order,
    created_at,
    updated_at,
    deleted_at,
    version
) VALUES
    (@property_id, @floor_1_id, '101', 'Phòng 101', 18.00, 2200000, 'VACANT', 3, 'Phòng tầng 1, phù hợp 1-3 người.', 'Seed V1 - Hải Đăng 1 tầng 1 phòng 101.', 1, 1, 101, NOW(6), NOW(6), NULL, 0),
    (@property_id, @floor_1_id, '102', 'Phòng 102', 18.00, 2200000, 'VACANT', 3, 'Phòng tầng 1, phù hợp 1-3 người.', 'Seed V1 - Hải Đăng 1 tầng 1 phòng 102.', 2, 1, 102, NOW(6), NOW(6), NULL, 0),
    (@property_id, @floor_1_id, '103', 'Phòng 103', 19.00, 2300000, 'VACANT', 3, 'Phòng tầng 1, phù hợp 1-3 người.', 'Seed V1 - Hải Đăng 1 tầng 1 phòng 103.', 3, 1, 103, NOW(6), NOW(6), NULL, 0),
    (@property_id, @floor_1_id, '104', 'Phòng 104', 19.00, 2300000, 'VACANT', 3, 'Phòng tầng 1, phù hợp 1-3 người.', 'Seed V1 - Hải Đăng 1 tầng 1 phòng 104.', 4, 1, 104, NOW(6), NOW(6), NULL, 0),
    (@property_id, @floor_1_id, '105', 'Phòng 105', 20.00, 2400000, 'VACANT', 3, 'Phòng tầng 1, phù hợp 1-3 người.', 'Seed V1 - Hải Đăng 1 tầng 1 phòng 105.', 5, 1, 105, NOW(6), NOW(6), NULL, 0),
    (@property_id, @floor_1_id, '106', 'Phòng 106', 20.00, 2400000, 'VACANT', 3, 'Phòng tầng 1, phù hợp 1-3 người.', 'Seed V1 - Hải Đăng 1 tầng 1 phòng 106.', 6, 1, 106, NOW(6), NOW(6), NULL, 0),

    (@property_id, @floor_2_id, '201', 'Phòng 201', 20.00, 2500000, 'VACANT', 3, 'Phòng tầng 2, phù hợp 1-3 người.', 'Seed V1 - Hải Đăng 1 tầng 2 phòng 201.', 1, 2, 201, NOW(6), NOW(6), NULL, 0),
    (@property_id, @floor_2_id, '202', 'Phòng 202', 20.00, 2500000, 'VACANT', 3, 'Phòng tầng 2, phù hợp 1-3 người.', 'Seed V1 - Hải Đăng 1 tầng 2 phòng 202.', 2, 2, 202, NOW(6), NOW(6), NULL, 0),
    (@property_id, @floor_2_id, '203', 'Phòng 203', 21.00, 2600000, 'VACANT', 3, 'Phòng tầng 2, phù hợp 1-3 người.', 'Seed V1 - Hải Đăng 1 tầng 2 phòng 203.', 3, 2, 203, NOW(6), NOW(6), NULL, 0),
    (@property_id, @floor_2_id, '204', 'Phòng 204', 21.00, 2600000, 'VACANT', 3, 'Phòng tầng 2, phù hợp 1-3 người.', 'Seed V1 - Hải Đăng 1 tầng 2 phòng 204.', 4, 2, 204, NOW(6), NOW(6), NULL, 0),
    (@property_id, @floor_2_id, '205', 'Phòng 205', 22.00, 2700000, 'VACANT', 3, 'Phòng tầng 2, phù hợp 1-3 người.', 'Seed V1 - Hải Đăng 1 tầng 2 phòng 205.', 5, 2, 205, NOW(6), NOW(6), NULL, 0),
    (@property_id, @floor_2_id, '206', 'Phòng 206', 22.00, 2700000, 'VACANT', 3, 'Phòng tầng 2, phù hợp 1-3 người.', 'Seed V1 - Hải Đăng 1 tầng 2 phòng 206.', 6, 2, 206, NOW(6), NOW(6), NULL, 0),
    (@property_id, @floor_2_id, '207', 'Phòng 207', 23.00, 2800000, 'VACANT', 3, 'Phòng tầng 2, phù hợp 1-3 người.', 'Seed V1 - Hải Đăng 1 tầng 2 phòng 207.', 7, 2, 207, NOW(6), NOW(6), NULL, 0),
    (@property_id, @floor_2_id, '208', 'Phòng 208', 23.00, 2800000, 'VACANT', 3, 'Phòng tầng 2, phù hợp 1-3 người.', 'Seed V1 - Hải Đăng 1 tầng 2 phòng 208.', 8, 2, 208, NOW(6), NOW(6), NULL, 0),

    (@property_id, @floor_3_id, '301', 'Phòng 301', 20.00, 2500000, 'VACANT', 3, 'Phòng tầng 3, phù hợp 1-3 người.', 'Seed V1 - Hải Đăng 1 tầng 3 phòng 301.', 1, 3, 301, NOW(6), NOW(6), NULL, 0),
    (@property_id, @floor_3_id, '302', 'Phòng 302', 20.00, 2500000, 'VACANT', 3, 'Phòng tầng 3, phù hợp 1-3 người.', 'Seed V1 - Hải Đăng 1 tầng 3 phòng 302.', 2, 3, 302, NOW(6), NOW(6), NULL, 0),
    (@property_id, @floor_3_id, '303', 'Phòng 303', 21.00, 2600000, 'VACANT', 3, 'Phòng tầng 3, phù hợp 1-3 người.', 'Seed V1 - Hải Đăng 1 tầng 3 phòng 303.', 3, 3, 303, NOW(6), NOW(6), NULL, 0),
    (@property_id, @floor_3_id, '304', 'Phòng 304', 21.00, 2600000, 'VACANT', 3, 'Phòng tầng 3, phù hợp 1-3 người.', 'Seed V1 - Hải Đăng 1 tầng 3 phòng 304.', 4, 3, 304, NOW(6), NOW(6), NULL, 0),
    (@property_id, @floor_3_id, '305', 'Phòng 305', 22.00, 2700000, 'VACANT', 3, 'Phòng tầng 3, phù hợp 1-3 người.', 'Seed V1 - Hải Đăng 1 tầng 3 phòng 305.', 5, 3, 305, NOW(6), NOW(6), NULL, 0),
    (@property_id, @floor_3_id, '306', 'Phòng 306', 22.00, 2700000, 'VACANT', 3, 'Phòng tầng 3, phù hợp 1-3 người.', 'Seed V1 - Hải Đăng 1 tầng 3 phòng 306.', 6, 3, 306, NOW(6), NOW(6), NULL, 0),
    (@property_id, @floor_3_id, '307', 'Phòng 307', 23.00, 2800000, 'VACANT', 3, 'Phòng tầng 3, phù hợp 1-3 người.', 'Seed V1 - Hải Đăng 1 tầng 3 phòng 307.', 7, 3, 307, NOW(6), NOW(6), NULL, 0),
    (@property_id, @floor_3_id, '308', 'Phòng 308', 23.00, 2800000, 'VACANT', 3, 'Phòng tầng 3, phù hợp 1-3 người.', 'Seed V1 - Hải Đăng 1 tầng 3 phòng 308.', 8, 3, 308, NOW(6), NOW(6), NULL, 0),

    (@property_id, @floor_4_id, '401', 'Phòng 401', 20.00, 2500000, 'VACANT', 3, 'Phòng tầng 4, phù hợp 1-3 người.', 'Seed V1 - Hải Đăng 1 tầng 4 phòng 401.', 1, 4, 401, NOW(6), NOW(6), NULL, 0),
    (@property_id, @floor_4_id, '402', 'Phòng 402', 20.00, 2500000, 'VACANT', 3, 'Phòng tầng 4, phù hợp 1-3 người.', 'Seed V1 - Hải Đăng 1 tầng 4 phòng 402.', 2, 4, 402, NOW(6), NOW(6), NULL, 0),
    (@property_id, @floor_4_id, '403', 'Phòng 403', 21.00, 2600000, 'VACANT', 3, 'Phòng tầng 4, phù hợp 1-3 người.', 'Seed V1 - Hải Đăng 1 tầng 4 phòng 403.', 3, 4, 403, NOW(6), NOW(6), NULL, 0),
    (@property_id, @floor_4_id, '404', 'Phòng 404', 21.00, 2600000, 'VACANT', 3, 'Phòng tầng 4, phù hợp 1-3 người.', 'Seed V1 - Hải Đăng 1 tầng 4 phòng 404.', 4, 4, 404, NOW(6), NOW(6), NULL, 0),
    (@property_id, @floor_4_id, '405', 'Phòng 405', 22.00, 2700000, 'VACANT', 3, 'Phòng tầng 4, phù hợp 1-3 người.', 'Seed V1 - Hải Đăng 1 tầng 4 phòng 405.', 5, 4, 405, NOW(6), NOW(6), NULL, 0),
    (@property_id, @floor_4_id, '406', 'Phòng 406', 22.00, 2700000, 'VACANT', 3, 'Phòng tầng 4, phù hợp 1-3 người.', 'Seed V1 - Hải Đăng 1 tầng 4 phòng 406.', 6, 4, 406, NOW(6), NOW(6), NULL, 0),
    (@property_id, @floor_4_id, '407', 'Phòng 407', 23.00, 2800000, 'VACANT', 3, 'Phòng tầng 4, phù hợp 1-3 người.', 'Seed V1 - Hải Đăng 1 tầng 4 phòng 407.', 7, 4, 407, NOW(6), NOW(6), NULL, 0),
    (@property_id, @floor_4_id, '408', 'Phòng 408', 23.00, 2800000, 'VACANT', 3, 'Phòng tầng 4, phù hợp 1-3 người.', 'Seed V1 - Hải Đăng 1 tầng 4 phòng 408.', 8, 4, 408, NOW(6), NOW(6), NULL, 0),

    (@property_id, @floor_5_id, '501', 'Phòng 501', 21.00, 2600000, 'VACANT', 3, 'Phòng tầng 5, phù hợp 1-3 người.', 'Seed V1 - Hải Đăng 1 tầng 5 phòng 501.', 1, 5, 501, NOW(6), NOW(6), NULL, 0),
    (@property_id, @floor_5_id, '502', 'Phòng 502', 21.00, 2600000, 'VACANT', 3, 'Phòng tầng 5, phù hợp 1-3 người.', 'Seed V1 - Hải Đăng 1 tầng 5 phòng 502.', 2, 5, 502, NOW(6), NOW(6), NULL, 0),
    (@property_id, @floor_5_id, '503', 'Phòng 503', 22.00, 2700000, 'VACANT', 3, 'Phòng tầng 5, phù hợp 1-3 người.', 'Seed V1 - Hải Đăng 1 tầng 5 phòng 503.', 3, 5, 503, NOW(6), NOW(6), NULL, 0),
    (@property_id, @floor_5_id, '504', 'Phòng 504', 22.00, 2700000, 'VACANT', 3, 'Phòng tầng 5, phù hợp 1-3 người.', 'Seed V1 - Hải Đăng 1 tầng 5 phòng 504.', 4, 5, 504, NOW(6), NOW(6), NULL, 0),
    (@property_id, @floor_5_id, '505', 'Phòng 505', 23.00, 2800000, 'VACANT', 3, 'Phòng tầng 5, phù hợp 1-3 người.', 'Seed V1 - Hải Đăng 1 tầng 5 phòng 505.', 5, 5, 505, NOW(6), NOW(6), NULL, 0),
    (@property_id, @floor_5_id, '506', 'Phòng 506', 23.00, 2800000, 'VACANT', 3, 'Phòng tầng 5, phù hợp 1-3 người.', 'Seed V1 - Hải Đăng 1 tầng 5 phòng 506.', 6, 5, 506, NOW(6), NOW(6), NULL, 0),
    (@property_id, @floor_5_id, '507', 'Phòng 507', 24.00, 2900000, 'VACANT', 3, 'Phòng tầng 5, phù hợp 1-3 người.', 'Seed V1 - Hải Đăng 1 tầng 5 phòng 507.', 7, 5, 507, NOW(6), NOW(6), NULL, 0) AS new_room
ON DUPLICATE KEY UPDATE
    floor_id = new_room.floor_id,
    name = new_room.name,
    area_m2 = new_room.area_m2,
    listed_price = new_room.listed_price,
    current_status = new_room.current_status,
    max_occupants = new_room.max_occupants,
    public_note = new_room.public_note,
    internal_note = new_room.internal_note,
    position_x = new_room.position_x,
    position_y = new_room.position_y,
    sort_order = new_room.sort_order,
    updated_at = NOW(6),
    deleted_at = NULL;

SET @property_id := (
    SELECT property_id
    FROM hdbhms.properties
    WHERE property_code = 'HAI_DANG_1'
    ORDER BY property_id
    LIMIT 1
);

SET @property_id := COALESCE(
    @property_id,
    (
        SELECT property_id
        FROM hdbhms.properties
        ORDER BY property_id
        LIMIT 1
    )
);

INSERT INTO hdbhms.property_rules (
    property_id,
    rule_code,
    title,
    description,
    default_fine_amount,
    sort_order,
    status,
    created_at,
    updated_at
) VALUES
    (@property_id, 'GENERAL_001', 'Lưu trú đúng số người đã đăng ký', 'Mỗi phòng chỉ lưu trú đúng số lượng người đã đăng ký. Mọi thay đổi cần báo trước cho quản lý.', NULL, 1, 'ACTIVE', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    (@property_id, 'GENERAL_002', 'Không kinh doanh hoặc tàng trữ hàng cấm', 'Không kinh doanh, tàng trữ hàng cấm, chất dễ cháy nổ hoặc vật dụng nguy hiểm trong khu trọ.', NULL, 2, 'ACTIVE', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    (@property_id, 'SECURITY_001', 'Khóa cửa cẩn thận khi ra vào', 'Không cung cấp mật mã, chìa khóa hoặc thẻ ra vào cho người lạ khi chưa được quản lý đồng ý.', NULL, 3, 'ACTIVE', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    (@property_id, 'SECURITY_002', 'Khách đến thăm cần báo trước', 'Khách đến thăm cần thông báo cho chủ trọ hoặc quản lý và tuân thủ khung giờ sinh hoạt chung.', NULL, 4, 'ACTIVE', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    (@property_id, 'HYGIENE_001', 'Đổ rác đúng nơi quy định', 'Không để rác trước cửa phòng, hành lang hoặc khu vực sinh hoạt chung.', NULL, 5, 'ACTIVE', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    (@property_id, 'HYGIENE_002', 'Giữ hành lang thông thoáng', 'Không để giày dép, xe đạp, thùng đồ hoặc vật dụng cá nhân chắn lối đi chung.', NULL, 6, 'ACTIVE', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    (@property_id, 'UTILITY_001', 'Sử dụng điện nước tiết kiệm', 'Tắt thiết bị điện, khóa nước và kiểm tra phòng trước khi ra ngoài.', NULL, 7, 'ACTIVE', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    (@property_id, 'UTILITY_002', 'Không tự ý can thiệp công tơ', 'Không tự ý tháo lắp, di chuyển hoặc can thiệp công tơ điện nước và thiết bị kỹ thuật.', NULL, 8, 'ACTIVE', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    (@property_id, 'WIFI_RESET', 'Không tự ý reset Wi-Fi', 'Không tự ý reset modem hoặc đổi mật khẩu Wi-Fi khi chưa được phép.', 200000, 100, 'ACTIVE', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    (@property_id, 'FINE_UNAUTHORIZED_REPAIR', 'Không tự ý sửa chữa kết cấu phòng', 'Mọi sửa chữa ảnh hưởng đến điện, nước, tường, cửa hoặc thiết bị cố định cần được quản lý duyệt trước.', NULL, 101, 'ACTIVE', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)) AS new_rule
ON DUPLICATE KEY UPDATE
    title = new_rule.title,
    description = new_rule.description,
    default_fine_amount = new_rule.default_fine_amount,
    sort_order = new_rule.sort_order,
    status = new_rule.status,
    updated_at = CURRENT_TIMESTAMP(6);
