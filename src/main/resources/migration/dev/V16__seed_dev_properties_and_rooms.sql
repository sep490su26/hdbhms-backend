INSERT IGNORE INTO properties (property_code, name, property_type, address_line, description, status)
VALUES
    ('HD1', 'Hải Đăng 1', 'BOARDING_HOUSE', 'Hải Đăng 1', 'Dev seed for viewing customer and room screens', 'ACTIVE'),
    ('HD2', 'Hải Đăng 2', 'BOARDING_HOUSE', 'Hải Đăng 2', 'Dev seed for viewing customer and room screens', 'ACTIVE'),
    ('CHA', 'Căn hộ A', 'APARTMENT', 'Căn hộ A', 'Dev seed for viewing customer and room screens', 'ACTIVE');

UPDATE properties
SET name = 'Hải Đăng 1',
    address_line = 'Hải Đăng 1'
WHERE property_code = 'HD1';

UPDATE properties
SET name = 'Hải Đăng 2',
    address_line = 'Hải Đăng 2'
WHERE property_code = 'HD2';

UPDATE properties
SET name = 'Căn hộ A',
    address_line = 'Căn hộ A'
WHERE property_code = 'CHA';

INSERT IGNORE INTO floors (property_id, floor_code, name, sort_order, status)
SELECT p.id, 'F1', 'Tầng 1', 1, 'ACTIVE'
FROM properties p
WHERE p.property_code IN ('HD1', 'HD2', 'CHA');

INSERT IGNORE INTO floors (property_id, floor_code, name, sort_order, status)
SELECT p.id, 'F2', 'Tầng 2', 2, 'ACTIVE'
FROM properties p
WHERE p.property_code IN ('HD1', 'HD2');

INSERT IGNORE INTO floors (property_id, floor_code, name, sort_order, status)
SELECT p.id, 'F3', 'Tầng 3', 3, 'ACTIVE'
FROM properties p
WHERE p.property_code = 'CHA';

INSERT IGNORE INTO rooms (
    property_id,
    floor_id,
    room_code,
    name,
    area_m2,
    listed_price,
    current_status,
    max_occupants,
    public_note,
    sort_order
)
SELECT p.id, f.id, seed.room_code, seed.room_code, seed.area_m2, seed.listed_price, seed.current_status, seed.max_occupants, seed.public_note, seed.sort_order
FROM properties p
JOIN (
    SELECT 'HD1' property_code, 'F1' seed_floor_code, 'P101' room_code, 24.00 area_m2, 2200000 listed_price, 'VACANT' current_status, 3 max_occupants, 'Phòng sáng, gần thang bộ' public_note, 101 sort_order
    UNION ALL SELECT 'HD1', 'F1', 'P102', 22.00, 2100000, 'VACANT', 3, 'Phòng tiêu chuẩn', 102
    UNION ALL SELECT 'HD1', 'F2', 'P204', 28.00, 2800000, 'RESERVED', 3, 'Phòng góc, ban công nhỏ', 204
    UNION ALL SELECT 'HD2', 'F1', 'P101', 20.00, 2000000, 'VACANT', 2, 'Phòng tầng thấp', 101
    UNION ALL SELECT 'HD2', 'F2', 'P202', 26.00, 2600000, 'MAINTENANCE', 3, 'Đang bảo trì nhẹ', 202
    UNION ALL SELECT 'CHA', 'F1', 'A101', 32.00, 3500000, 'VACANT', 4, 'Căn hộ mini đầy đủ tiện nghi', 101
    UNION ALL SELECT 'CHA', 'F3', 'A301', 35.00, 3900000, 'VACANT', 4, 'View thoáng, có bếp riêng', 301
) seed ON seed.property_code = p.property_code
JOIN floors f ON f.property_id = p.id AND f.floor_code = seed.seed_floor_code
WHERE p.property_code IN ('HD1', 'HD2', 'CHA');
