-- Seed Hai Dang 1 property, floors, and the 37-room layout from the PRD.

INSERT INTO properties (
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
    'Nha tro Hai Dang 1',
    'BOARDING_HOUSE',
    'So 70A1, Thon 4, xa Thach Hoa, Thach That, Ha Noi',
    'Nha tro 5 tang gom 37 phong theo so do PRD: tang 1 co 6 phong, tang 2-4 moi tang 8 phong, tang 5 co 7 phong.',
    'ACTIVE',
    NOW(6),
    NOW(6),
    NULL,
    0
) ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    property_type = VALUES(property_type),
    address_line = VALUES(address_line),
    description = VALUES(description),
    status = VALUES(status),
    updated_at = NOW(6),
    deleted_at = NULL;

SET @property_id := (
    SELECT id
    FROM properties
    WHERE property_code = 'HAI_DANG_1'
    LIMIT 1
);

INSERT INTO floors (
    property_id,
    floor_code,
    name,
    sort_order,
    status,
    created_at,
    updated_at,
    deleted_at
) VALUES
    (@property_id, 'F1', 'Tang 1', 1, 'ACTIVE', NOW(6), NOW(6), NULL),
    (@property_id, 'F2', 'Tang 2', 2, 'ACTIVE', NOW(6), NOW(6), NULL),
    (@property_id, 'F3', 'Tang 3', 3, 'ACTIVE', NOW(6), NOW(6), NULL),
    (@property_id, 'F4', 'Tang 4', 4, 'ACTIVE', NOW(6), NOW(6), NULL),
    (@property_id, 'F5', 'Tang 5', 5, 'ACTIVE', NOW(6), NOW(6), NULL)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    sort_order = VALUES(sort_order),
    status = VALUES(status),
    updated_at = NOW(6),
    deleted_at = NULL;

SET @floor_1_id := (
    SELECT id FROM floors
    WHERE property_id = @property_id AND floor_code = 'F1'
    LIMIT 1
);
SET @floor_2_id := (
    SELECT id FROM floors
    WHERE property_id = @property_id AND floor_code = 'F2'
    LIMIT 1
);
SET @floor_3_id := (
    SELECT id FROM floors
    WHERE property_id = @property_id AND floor_code = 'F3'
    LIMIT 1
);
SET @floor_4_id := (
    SELECT id FROM floors
    WHERE property_id = @property_id AND floor_code = 'F4'
    LIMIT 1
);
SET @floor_5_id := (
    SELECT id FROM floors
    WHERE property_id = @property_id AND floor_code = 'F5'
    LIMIT 1
);

INSERT INTO rooms (
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
    (@property_id, @floor_1_id, '101', 'Phong 101', 18.00, 2200000, 'VACANT', 3, 'Phong tang 1, phu hop 1-3 nguoi.', 'Seed V21 - Hai Dang 1 floor 1 room 101.', 1, 1, 101, NOW(6), NOW(6), NULL, 0),
    (@property_id, @floor_1_id, '102', 'Phong 102', 18.00, 2200000, 'VACANT', 3, 'Phong tang 1, phu hop 1-3 nguoi.', 'Seed V21 - Hai Dang 1 floor 1 room 102.', 2, 1, 102, NOW(6), NOW(6), NULL, 0),
    (@property_id, @floor_1_id, '103', 'Phong 103', 19.00, 2300000, 'VACANT', 3, 'Phong tang 1, phu hop 1-3 nguoi.', 'Seed V21 - Hai Dang 1 floor 1 room 103.', 3, 1, 103, NOW(6), NOW(6), NULL, 0),
    (@property_id, @floor_1_id, '104', 'Phong 104', 19.00, 2300000, 'VACANT', 3, 'Phong tang 1, phu hop 1-3 nguoi.', 'Seed V21 - Hai Dang 1 floor 1 room 104.', 4, 1, 104, NOW(6), NOW(6), NULL, 0),
    (@property_id, @floor_1_id, '105', 'Phong 105', 20.00, 2400000, 'VACANT', 3, 'Phong tang 1, phu hop 1-3 nguoi.', 'Seed V21 - Hai Dang 1 floor 1 room 105.', 5, 1, 105, NOW(6), NOW(6), NULL, 0),
    (@property_id, @floor_1_id, '106', 'Phong 106', 20.00, 2400000, 'VACANT', 3, 'Phong tang 1, phu hop 1-3 nguoi.', 'Seed V21 - Hai Dang 1 floor 1 room 106.', 6, 1, 106, NOW(6), NOW(6), NULL, 0),

    (@property_id, @floor_2_id, '201', 'Phong 201', 20.00, 2500000, 'VACANT', 3, 'Phong tang 2, phu hop 1-3 nguoi.', 'Seed V21 - Hai Dang 1 floor 2 room 201.', 1, 2, 201, NOW(6), NOW(6), NULL, 0),
    (@property_id, @floor_2_id, '202', 'Phong 202', 20.00, 2500000, 'VACANT', 3, 'Phong tang 2, phu hop 1-3 nguoi.', 'Seed V21 - Hai Dang 1 floor 2 room 202.', 2, 2, 202, NOW(6), NOW(6), NULL, 0),
    (@property_id, @floor_2_id, '203', 'Phong 203', 21.00, 2600000, 'VACANT', 3, 'Phong tang 2, phu hop 1-3 nguoi.', 'Seed V21 - Hai Dang 1 floor 2 room 203.', 3, 2, 203, NOW(6), NOW(6), NULL, 0),
    (@property_id, @floor_2_id, '204', 'Phong 204', 21.00, 2600000, 'VACANT', 3, 'Phong tang 2, phu hop 1-3 nguoi.', 'Seed V21 - Hai Dang 1 floor 2 room 204.', 4, 2, 204, NOW(6), NOW(6), NULL, 0),
    (@property_id, @floor_2_id, '205', 'Phong 205', 22.00, 2700000, 'VACANT', 3, 'Phong tang 2, phu hop 1-3 nguoi.', 'Seed V21 - Hai Dang 1 floor 2 room 205.', 5, 2, 205, NOW(6), NOW(6), NULL, 0),
    (@property_id, @floor_2_id, '206', 'Phong 206', 22.00, 2700000, 'VACANT', 3, 'Phong tang 2, phu hop 1-3 nguoi.', 'Seed V21 - Hai Dang 1 floor 2 room 206.', 6, 2, 206, NOW(6), NOW(6), NULL, 0),
    (@property_id, @floor_2_id, '207', 'Phong 207', 23.00, 2800000, 'VACANT', 3, 'Phong tang 2, phu hop 1-3 nguoi.', 'Seed V21 - Hai Dang 1 floor 2 room 207.', 7, 2, 207, NOW(6), NOW(6), NULL, 0),
    (@property_id, @floor_2_id, '208', 'Phong 208', 23.00, 2800000, 'VACANT', 3, 'Phong tang 2, phu hop 1-3 nguoi.', 'Seed V21 - Hai Dang 1 floor 2 room 208.', 8, 2, 208, NOW(6), NOW(6), NULL, 0),

    (@property_id, @floor_3_id, '301', 'Phong 301', 20.00, 2500000, 'VACANT', 3, 'Phong tang 3, phu hop 1-3 nguoi.', 'Seed V21 - Hai Dang 1 floor 3 room 301.', 1, 3, 301, NOW(6), NOW(6), NULL, 0),
    (@property_id, @floor_3_id, '302', 'Phong 302', 20.00, 2500000, 'VACANT', 3, 'Phong tang 3, phu hop 1-3 nguoi.', 'Seed V21 - Hai Dang 1 floor 3 room 302.', 2, 3, 302, NOW(6), NOW(6), NULL, 0),
    (@property_id, @floor_3_id, '303', 'Phong 303', 21.00, 2600000, 'VACANT', 3, 'Phong tang 3, phu hop 1-3 nguoi.', 'Seed V21 - Hai Dang 1 floor 3 room 303.', 3, 3, 303, NOW(6), NOW(6), NULL, 0),
    (@property_id, @floor_3_id, '304', 'Phong 304', 21.00, 2600000, 'VACANT', 3, 'Phong tang 3, phu hop 1-3 nguoi.', 'Seed V21 - Hai Dang 1 floor 3 room 304.', 4, 3, 304, NOW(6), NOW(6), NULL, 0),
    (@property_id, @floor_3_id, '305', 'Phong 305', 22.00, 2700000, 'VACANT', 3, 'Phong tang 3, phu hop 1-3 nguoi.', 'Seed V21 - Hai Dang 1 floor 3 room 305.', 5, 3, 305, NOW(6), NOW(6), NULL, 0),
    (@property_id, @floor_3_id, '306', 'Phong 306', 22.00, 2700000, 'VACANT', 3, 'Phong tang 3, phu hop 1-3 nguoi.', 'Seed V21 - Hai Dang 1 floor 3 room 306.', 6, 3, 306, NOW(6), NOW(6), NULL, 0),
    (@property_id, @floor_3_id, '307', 'Phong 307', 23.00, 2800000, 'VACANT', 3, 'Phong tang 3, phu hop 1-3 nguoi.', 'Seed V21 - Hai Dang 1 floor 3 room 307.', 7, 3, 307, NOW(6), NOW(6), NULL, 0),
    (@property_id, @floor_3_id, '308', 'Phong 308', 23.00, 2800000, 'VACANT', 3, 'Phong tang 3, phu hop 1-3 nguoi.', 'Seed V21 - Hai Dang 1 floor 3 room 308.', 8, 3, 308, NOW(6), NOW(6), NULL, 0),

    (@property_id, @floor_4_id, '401', 'Phong 401', 20.00, 2500000, 'VACANT', 3, 'Phong tang 4, phu hop 1-3 nguoi.', 'Seed V21 - Hai Dang 1 floor 4 room 401.', 1, 4, 401, NOW(6), NOW(6), NULL, 0),
    (@property_id, @floor_4_id, '402', 'Phong 402', 20.00, 2500000, 'VACANT', 3, 'Phong tang 4, phu hop 1-3 nguoi.', 'Seed V21 - Hai Dang 1 floor 4 room 402.', 2, 4, 402, NOW(6), NOW(6), NULL, 0),
    (@property_id, @floor_4_id, '403', 'Phong 403', 21.00, 2600000, 'VACANT', 3, 'Phong tang 4, phu hop 1-3 nguoi.', 'Seed V21 - Hai Dang 1 floor 4 room 403.', 3, 4, 403, NOW(6), NOW(6), NULL, 0),
    (@property_id, @floor_4_id, '404', 'Phong 404', 21.00, 2600000, 'VACANT', 3, 'Phong tang 4, phu hop 1-3 nguoi.', 'Seed V21 - Hai Dang 1 floor 4 room 404.', 4, 4, 404, NOW(6), NOW(6), NULL, 0),
    (@property_id, @floor_4_id, '405', 'Phong 405', 22.00, 2700000, 'VACANT', 3, 'Phong tang 4, phu hop 1-3 nguoi.', 'Seed V21 - Hai Dang 1 floor 4 room 405.', 5, 4, 405, NOW(6), NOW(6), NULL, 0),
    (@property_id, @floor_4_id, '406', 'Phong 406', 22.00, 2700000, 'VACANT', 3, 'Phong tang 4, phu hop 1-3 nguoi.', 'Seed V21 - Hai Dang 1 floor 4 room 406.', 6, 4, 406, NOW(6), NOW(6), NULL, 0),
    (@property_id, @floor_4_id, '407', 'Phong 407', 23.00, 2800000, 'VACANT', 3, 'Phong tang 4, phu hop 1-3 nguoi.', 'Seed V21 - Hai Dang 1 floor 4 room 407.', 7, 4, 407, NOW(6), NOW(6), NULL, 0),
    (@property_id, @floor_4_id, '408', 'Phong 408', 23.00, 2800000, 'VACANT', 3, 'Phong tang 4, phu hop 1-3 nguoi.', 'Seed V21 - Hai Dang 1 floor 4 room 408.', 8, 4, 408, NOW(6), NOW(6), NULL, 0),

    (@property_id, @floor_5_id, '501', 'Phong 501', 21.00, 2600000, 'VACANT', 3, 'Phong tang 5, phu hop 1-3 nguoi.', 'Seed V21 - Hai Dang 1 floor 5 room 501.', 1, 5, 501, NOW(6), NOW(6), NULL, 0),
    (@property_id, @floor_5_id, '502', 'Phong 502', 21.00, 2600000, 'VACANT', 3, 'Phong tang 5, phu hop 1-3 nguoi.', 'Seed V21 - Hai Dang 1 floor 5 room 502.', 2, 5, 502, NOW(6), NOW(6), NULL, 0),
    (@property_id, @floor_5_id, '503', 'Phong 503', 22.00, 2700000, 'VACANT', 3, 'Phong tang 5, phu hop 1-3 nguoi.', 'Seed V21 - Hai Dang 1 floor 5 room 503.', 3, 5, 503, NOW(6), NOW(6), NULL, 0),
    (@property_id, @floor_5_id, '504', 'Phong 504', 22.00, 2700000, 'VACANT', 3, 'Phong tang 5, phu hop 1-3 nguoi.', 'Seed V21 - Hai Dang 1 floor 5 room 504.', 4, 5, 504, NOW(6), NOW(6), NULL, 0),
    (@property_id, @floor_5_id, '505', 'Phong 505', 23.00, 2800000, 'VACANT', 3, 'Phong tang 5, phu hop 1-3 nguoi.', 'Seed V21 - Hai Dang 1 floor 5 room 505.', 5, 5, 505, NOW(6), NOW(6), NULL, 0),
    (@property_id, @floor_5_id, '506', 'Phong 506', 23.00, 2800000, 'VACANT', 3, 'Phong tang 5, phu hop 1-3 nguoi.', 'Seed V21 - Hai Dang 1 floor 5 room 506.', 6, 5, 506, NOW(6), NOW(6), NULL, 0),
    (@property_id, @floor_5_id, '507', 'Phong 507', 24.00, 2900000, 'VACANT', 3, 'Phong tang 5, phu hop 1-3 nguoi.', 'Seed V21 - Hai Dang 1 floor 5 room 507.', 7, 5, 507, NOW(6), NOW(6), NULL, 0)
ON DUPLICATE KEY UPDATE
    floor_id = VALUES(floor_id),
    name = VALUES(name),
    area_m2 = VALUES(area_m2),
    listed_price = VALUES(listed_price),
    current_status = VALUES(current_status),
    max_occupants = VALUES(max_occupants),
    public_note = VALUES(public_note),
    internal_note = VALUES(internal_note),
    position_x = VALUES(position_x),
    position_y = VALUES(position_y),
    sort_order = VALUES(sort_order),
    updated_at = NOW(6),
    deleted_at = NULL;
