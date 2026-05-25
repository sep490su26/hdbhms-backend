-- =========================================================
-- TroManager / HDBHMS seed data for mobile & web testing
-- Run this AFTER V0__init.sql on a fresh hdbhms database.
-- Accounts:
--   OWNER:   0900000001 / Tenant@123
--   MANAGER: 0900000002 / Tenant@123
--   Tenant 1,2,3: password has been changed -> Changed@123 and full CCCD data
--   Tenant 4,5: newly issued account -> Tenant@123, must_change_password=TRUE, pending CCCD upload
-- Password hash note: Tenant@123 uses $2a$12$gSi..., Changed@123 uses $2a$12$jyB...
-- =========================================================

SET NAMES utf8mb4;

-- Current V0 may be missing fields required by User entity.
-- Use conditional ALTER via information_schema so this seed remains safe if V0 already has them.
SET @ddl := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE users ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE AFTER active_unique_token',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'must_change_password'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE users ADD COLUMN password_changed_at DATETIME(6) NULL AFTER must_change_password',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'password_changed_at'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @OLD_FOREIGN_KEY_CHECKS := @@FOREIGN_KEY_CHECKS;
SET FOREIGN_KEY_CHECKS = 0;
START TRANSACTION;

-- ---------------------------------------------------------
-- 1) Internal accounts: owner + manager
-- ---------------------------------------------------------
INSERT INTO users (phone, email, password_hash, role, status, last_login_at, email_verified, must_change_password, password_changed_at)
VALUES
('0900000001', 'owner@haidang.test',   '$2a$12$gSi468uj6uRk2mmNFyJ5QON8DKXAK3SW8vy0FtVI0QXG/BWbPrKSW', 'OWNER',   'ACTIVE', NOW(6), TRUE, FALSE, '2026-01-01 08:00:00.000000'),
('0900000002', 'manager@haidang.test', '$2a$12$gSi468uj6uRk2mmNFyJ5QON8DKXAK3SW8vy0FtVI0QXG/BWbPrKSW', 'MANAGER', 'ACTIVE', NOW(6), TRUE, FALSE, '2026-01-01 08:00:00.000000');
SET @owner_user_id   := (SELECT id FROM users WHERE phone='0900000001');
SET @manager_user_id := (SELECT id FROM users WHERE phone='0900000002');

-- ---------------------------------------------------------
-- 2) Property, floor 2, rooms 201-206
-- ---------------------------------------------------------
INSERT INTO properties (property_code, name, property_type, address_line, description, status)
VALUES ('HD1-SEED', 'Nhà trọ Hải Đăng 1 - Seed Test', 'BOARDING_HOUSE',
        'Số 70A1, Thôn 4, xã Thạch Hoà, Thạch Thất, Hà Nội',
        'Seed data: 5 tài khoản khách thuê ở phòng 201-205, phòng 206 trống để test chuyển phòng/đặt cọc.', 'ACTIVE');
SET @property_id := LAST_INSERT_ID();

-- owner/manager tenant rows, used by FKs such as lease_contracts.created_by
INSERT INTO tenants (user_id, property_id) VALUES (@owner_user_id, @property_id), (@manager_user_id, @property_id);
SET @owner_tenant_id   := (SELECT id FROM tenants WHERE user_id=@owner_user_id AND property_id=@property_id);
SET @manager_tenant_id := (SELECT id FROM tenants WHERE user_id=@manager_user_id AND property_id=@property_id);

INSERT INTO floors (property_id, floor_code, name, sort_order, status)
VALUES (@property_id, 'F2', 'Tầng 2', 2, 'ACTIVE');
SET @floor2_id := LAST_INSERT_ID();

INSERT INTO rooms (property_id, floor_id, room_code, name, area_m2, listed_price, current_status, max_occupants, public_note, position_x, position_y, sort_order)
VALUES
(@property_id, @floor2_id, '201', 'Phòng 201', 20.00, 2200000, 'OCCUPIED', 3, 'Phòng đang thuê - tài khoản đã đổi mật khẩu, CCCD đầy đủ', 0,   0, 1),
(@property_id, @floor2_id, '202', 'Phòng 202', 22.00, 2300000, 'OCCUPIED', 3, 'Phòng đang thuê - tài khoản đã đổi mật khẩu, CCCD đầy đủ', 120, 0, 2),
(@property_id, @floor2_id, '203', 'Phòng 203', 18.00, 2100000, 'OCCUPIED', 3, 'Phòng đang thuê - đã đổi mật khẩu, CCCD đầy đủ', 240, 0, 3),
(@property_id, @floor2_id, '204', 'Phòng 204', 24.00, 2500000, 'OCCUPIED', 3, 'Phòng đang thuê - tài khoản mới cấp, cần đổi mật khẩu và bổ sung CCCD', 360, 0, 4),
(@property_id, @floor2_id, '205', 'Phòng 205', 19.00, 2150000, 'OCCUPIED', 3, 'Phòng đang thuê - tài khoản mới cấp, cần đổi mật khẩu và bổ sung CCCD', 480, 0, 5),
(@property_id, @floor2_id, '206', 'Phòng 206', 25.00, 2600000, 'VACANT',   3, 'Phòng trống dùng để test chuyển phòng/đặt cọc', 600, 0, 6);
SET @room201 := (SELECT id FROM rooms WHERE property_id=@property_id AND room_code='201');
SET @room202 := (SELECT id FROM rooms WHERE property_id=@property_id AND room_code='202');
SET @room203 := (SELECT id FROM rooms WHERE property_id=@property_id AND room_code='203');
SET @room204 := (SELECT id FROM rooms WHERE property_id=@property_id AND room_code='204');
SET @room205 := (SELECT id FROM rooms WHERE property_id=@property_id AND room_code='205');
SET @room206 := (SELECT id FROM rooms WHERE property_id=@property_id AND room_code='206');

INSERT INTO room_status_display_configs (room_status, color_hex, label)
VALUES
('VACANT','#22C55E','Trống'),('RESERVED','#EAB308','Đang đặt cọc'),('OCCUPIED','#2563EB','Đang thuê'),
('SOON_VACANT','#F97316','Sắp trống'),('MAINTENANCE','#EF4444','Bảo trì'),('EXPIRED','#A855F7','Hết hạn HĐ');

-- Assets for handover / room detail screens
INSERT INTO room_assets (room_id, asset_name, asset_category, quantity, current_condition, description)
SELECT r.id, a.asset_name, a.asset_category, a.quantity, a.current_condition, a.description
FROM rooms r
JOIN (
    SELECT 'Điều hoà + remote' asset_name, 'ELECTRIC' asset_category, 1 quantity, 'GOOD' current_condition, 'Hoạt động bình thường' description UNION ALL
    SELECT 'Bình nóng lạnh', 'ELECTRIC', 1, 'GOOD', 'Hoạt động bình thường' UNION ALL
    SELECT 'Tủ quần áo 3 buồng', 'FURNITURE', 1, 'GOOD', 'Bàn giao kèm phòng' UNION ALL
    SELECT 'Bàn học', 'FURNITURE', 1, 'GOOD', 'Bàn giao kèm phòng' UNION ALL
    SELECT 'Modem internet', 'NETWORK', 1, 'GOOD', 'Không tự ý reset mật khẩu modem'
) a
WHERE r.property_id=@property_id AND r.room_code IN ('201','202','203','204','205');

-- ---------------------------------------------------------
-- 3) Tariffs, bank accounts, meters and readings
-- ---------------------------------------------------------
INSERT INTO utility_tariffs (property_id, utility_type, unit_price, free_allowance, service_fee_waive_electricity_threshold, effective_from, created_by)
VALUES
(@property_id, 'ELECTRICITY', 3500, 0, NULL, '2026-01-01', @owner_user_id),
(@property_id, 'WATER', 20000, 6, NULL, '2026-01-01', @owner_user_id),
(@property_id, 'SERVICE_FEE', 50000, 0, 100000, '2026-01-01', @owner_user_id);

INSERT INTO collection_accounts (property_id, account_type, bank_name, account_number, account_holder, provider, status)
VALUES
(@property_id, 'RENT', 'Agribank', '3213882010001', 'DANG VAN NHUAN', 'BANK', 'ACTIVE'),
(@property_id, 'UTILITY', 'Vietcombank', '123456789001', 'DANG VAN NHUAN', 'BANK', 'ACTIVE'),
(@property_id, 'DEPOSIT', 'MB Bank', '999888777001', 'DANG VAN NHUAN', 'BANK', 'ACTIVE');
SET @rent_account_id    := (SELECT id FROM collection_accounts WHERE property_id=@property_id AND account_type='RENT');
SET @utility_account_id := (SELECT id FROM collection_accounts WHERE property_id=@property_id AND account_type='UTILITY');
SET @deposit_account_id := (SELECT id FROM collection_accounts WHERE property_id=@property_id AND account_type='DEPOSIT');

INSERT INTO meters (room_id, meter_type, meter_code, status, installed_at)
VALUES
(@room201,'ELECTRICITY','E-201','ACTIVE','2025-01-01'),(@room201,'WATER','W-201','ACTIVE','2025-01-01'),
(@room202,'ELECTRICITY','E-202','ACTIVE','2025-01-01'),(@room202,'WATER','W-202','ACTIVE','2025-01-01'),
(@room203,'ELECTRICITY','E-203','ACTIVE','2025-01-01'),(@room203,'WATER','W-203','ACTIVE','2025-01-01'),
(@room204,'ELECTRICITY','E-204','ACTIVE','2025-01-01'),(@room204,'WATER','W-204','ACTIVE','2025-01-01'),
(@room205,'ELECTRICITY','E-205','ACTIVE','2025-01-01'),(@room205,'WATER','W-205','ACTIVE','2025-01-01'),
(@room206,'ELECTRICITY','E-206','ACTIVE','2025-01-01'),(@room206,'WATER','W-206','ACTIVE','2025-01-01');

SET @e201 := (SELECT id FROM meters WHERE room_id=@room201 AND meter_type='ELECTRICITY'); SET @w201 := (SELECT id FROM meters WHERE room_id=@room201 AND meter_type='WATER');
SET @e202 := (SELECT id FROM meters WHERE room_id=@room202 AND meter_type='ELECTRICITY'); SET @w202 := (SELECT id FROM meters WHERE room_id=@room202 AND meter_type='WATER');
SET @e203 := (SELECT id FROM meters WHERE room_id=@room203 AND meter_type='ELECTRICITY'); SET @w203 := (SELECT id FROM meters WHERE room_id=@room203 AND meter_type='WATER');
SET @e204 := (SELECT id FROM meters WHERE room_id=@room204 AND meter_type='ELECTRICITY'); SET @w204 := (SELECT id FROM meters WHERE room_id=@room204 AND meter_type='WATER');
SET @e205 := (SELECT id FROM meters WHERE room_id=@room205 AND meter_type='ELECTRICITY'); SET @w205 := (SELECT id FROM meters WHERE room_id=@room205 AND meter_type='WATER');

INSERT INTO meter_reading_batches (property_id, reading_period, source, status, created_by, confirmed_by, confirmed_at)
VALUES (@property_id, '2026-05', 'MANUAL', 'CONFIRMED', @manager_user_id, @manager_user_id, NOW(6));
SET @batch_202605 := LAST_INSERT_ID();

-- previous_value/current_value are realistic enough for invoice & utility-history screens
INSERT INTO meter_readings (batch_id, meter_id, room_id, reading_period, previous_value, current_value, reading_date, source, status, created_by)
VALUES
(@batch_202605, @e201, @room201, '2026-05', 1250, 1380, '2026-05-01', 'MANUAL', 'CONFIRMED', @manager_user_id),
(@batch_202605, @w201, @room201, '2026-05',   85,   98, '2026-05-01', 'MANUAL', 'CONFIRMED', @manager_user_id),
(@batch_202605, @e202, @room202, '2026-05', 2100, 2125, '2026-05-01', 'MANUAL', 'CONFIRMED', @manager_user_id),
(@batch_202605, @w202, @room202, '2026-05',  130,  136, '2026-05-01', 'MANUAL', 'CONFIRMED', @manager_user_id),
(@batch_202605, @e203, @room203, '2026-05',  980, 1110, '2026-05-01', 'MANUAL', 'CONFIRMED', @manager_user_id),
(@batch_202605, @w203, @room203, '2026-05',   70,   80, '2026-05-01', 'MANUAL', 'CONFIRMED', @manager_user_id),
(@batch_202605, @e204, @room204, '2026-05', 1500, 1620, '2026-05-01', 'MANUAL', 'CONFIRMED', @manager_user_id),
(@batch_202605, @w204, @room204, '2026-05',  200,  208, '2026-05-01', 'MANUAL', 'CONFIRMED', @manager_user_id),
(@batch_202605, @e205, @room205, '2026-05',  760,  785, '2026-05-01', 'MANUAL', 'CONFIRMED', @manager_user_id),
(@batch_202605, @w205, @room205, '2026-05',   41,   46, '2026-05-01', 'MANUAL', 'CONFIRMED', @manager_user_id);

-- ---------------------------------------------------------
-- 4) 5 tenant accounts: profiles, files, tenant rows, contracts
-- ---------------------------------------------------------
INSERT INTO users (phone, email, password_hash, role, status, last_login_at, email_verified, must_change_password, password_changed_at)
VALUES
('0912000201', 'p201@tenant.test', '$2a$12$jyBRInWHUy2iU1dcNRXKkuQu41tXwWYOE7fBTTSBNMYXuhIaN/Ts2', 'TENANT', 'ACTIVE', '2026-05-10 08:00:00.000000', TRUE, FALSE, '2026-05-10 08:00:00.000000'),
('0912000202', 'p202@tenant.test', '$2a$12$jyBRInWHUy2iU1dcNRXKkuQu41tXwWYOE7fBTTSBNMYXuhIaN/Ts2', 'TENANT', 'ACTIVE', '2026-05-11 09:15:00.000000', TRUE, FALSE, '2026-05-11 09:15:00.000000'),
('0912000203', 'p203@tenant.test', '$2a$12$jyBRInWHUy2iU1dcNRXKkuQu41tXwWYOE7fBTTSBNMYXuhIaN/Ts2', 'TENANT', 'ACTIVE', '2026-05-12 10:20:00.000000', TRUE, FALSE, '2026-05-12 10:20:00.000000'),
('0912000204', 'p204@tenant.test', '$2a$12$gSi468uj6uRk2mmNFyJ5QON8DKXAK3SW8vy0FtVI0QXG/BWbPrKSW', 'TENANT', 'ACTIVE', NULL, TRUE, TRUE, NULL),
('0912000205', 'p205@tenant.test', '$2a$12$gSi468uj6uRk2mmNFyJ5QON8DKXAK3SW8vy0FtVI0QXG/BWbPrKSW', 'TENANT', 'ACTIVE', NULL, TRUE, TRUE, NULL);
SET @u201 := (SELECT id FROM users WHERE phone='0912000201');
SET @u202 := (SELECT id FROM users WHERE phone='0912000202');
SET @u203 := (SELECT id FROM users WHERE phone='0912000203');
SET @u204 := (SELECT id FROM users WHERE phone='0912000204');
SET @u205 := (SELECT id FROM users WHERE phone='0912000205');

INSERT INTO file_metadata (owner_user_id, storage_key, original_name, mime_type, size_bytes, sha256_checksum, category, is_sensitive)
VALUES
(@u201,'seed/tenant-201/portrait.jpg','portrait-201.jpg','image/jpeg',180000,REPEAT('a',64),'PORTRAIT_PHOTO',TRUE),
(@u201,'seed/tenant-201/cccd-front.jpg','cccd-front-201.jpg','image/jpeg',220000,REPEAT('b',64),'ID_CARD',TRUE),
(@u201,'seed/tenant-201/cccd-back.jpg','cccd-back-201.jpg','image/jpeg',215000,REPEAT('c',64),'ID_CARD',TRUE),
(@u202,'seed/tenant-202/portrait.jpg','portrait-202.jpg','image/jpeg',175000,REPEAT('d',64),'PORTRAIT_PHOTO',TRUE),
(@u202,'seed/tenant-202/cccd-front.jpg','cccd-front-202.jpg','image/jpeg',225000,REPEAT('e',64),'ID_CARD',TRUE),
(@u202,'seed/tenant-202/cccd-back.jpg','cccd-back-202.jpg','image/jpeg',210000,REPEAT('f',64),'ID_CARD',TRUE),
(@u203,'seed/tenant-203/portrait.jpg','portrait-203.jpg','image/jpeg',176000,REPEAT('1',64),'PORTRAIT_PHOTO',TRUE),
(@u203,'seed/tenant-203/cccd-front.jpg','cccd-front-203.jpg','image/jpeg',226000,REPEAT('2',64),'ID_CARD',TRUE),
(@u203,'seed/tenant-203/cccd-back.jpg','cccd-back-203.jpg','image/jpeg',211000,REPEAT('3',64),'ID_CARD',TRUE);
SET @p201_portrait := (SELECT id FROM file_metadata WHERE storage_key='seed/tenant-201/portrait.jpg');
SET @p201_front    := (SELECT id FROM file_metadata WHERE storage_key='seed/tenant-201/cccd-front.jpg');
SET @p201_back     := (SELECT id FROM file_metadata WHERE storage_key='seed/tenant-201/cccd-back.jpg');
SET @p202_portrait := (SELECT id FROM file_metadata WHERE storage_key='seed/tenant-202/portrait.jpg');
SET @p202_front    := (SELECT id FROM file_metadata WHERE storage_key='seed/tenant-202/cccd-front.jpg');
SET @p202_back     := (SELECT id FROM file_metadata WHERE storage_key='seed/tenant-202/cccd-back.jpg');
SET @p203_portrait := (SELECT id FROM file_metadata WHERE storage_key='seed/tenant-203/portrait.jpg');
SET @p203_front    := (SELECT id FROM file_metadata WHERE storage_key='seed/tenant-203/cccd-front.jpg');
SET @p203_back     := (SELECT id FROM file_metadata WHERE storage_key='seed/tenant-203/cccd-back.jpg');

INSERT INTO person_profiles (full_name, dob, gender, phone, email, permanent_address, portrait_file_id)
VALUES
('Nguyễn Văn An',     '2001-03-15', 'MALE',   '0912000201', 'p201@tenant.test', 'Thôn Đại Đồng, xã Thạch Hòa, Thạch Thất, Hà Nội', @p201_portrait),
('Trần Thị Bình',     '2002-07-20', 'FEMALE', '0912000202', 'p202@tenant.test', 'Phường Phú Đô, Nam Từ Liêm, Hà Nội', @p202_portrait),
('Lê Minh Cường',     '2003-01-09', 'MALE',   '0912000203', 'p203@tenant.test', 'Xã Bình Yên, Thạch Thất, Hà Nội', @p203_portrait),
('Phạm Thu Dung',     '2004-11-23', 'FEMALE', '0912000204', 'p204@tenant.test', 'Chưa cập nhật - tài khoản mới cấp', NULL),
('Hoàng Gia Khánh',   '2001-12-05', 'MALE',   '0912000205', 'p205@tenant.test', 'Chưa cập nhật - tài khoản mới cấp', NULL);
SET @profile201 := (SELECT id FROM person_profiles WHERE phone='0912000201');
SET @profile202 := (SELECT id FROM person_profiles WHERE phone='0912000202');
SET @profile203 := (SELECT id FROM person_profiles WHERE phone='0912000203');
SET @profile204 := (SELECT id FROM person_profiles WHERE phone='0912000204');
SET @profile205 := (SELECT id FROM person_profiles WHERE phone='0912000205');

-- Only accounts 201, 202, 203 have full CCCD data; accounts 204, 205 intentionally do not.
INSERT INTO identity_documents (profile_id, doc_type, doc_number, issued_date, issued_place, expiry_date, raw_ocr_data, front_file_id, back_file_id, status)
VALUES
(@profile201, 'CCCD', '001201000201', '2021-04-01', 'Cục CSQLHC về TTXH', '2036-04-01', CAST('{"seed":"full_kyc","room":"201"}' AS BINARY), @p201_front, @p201_back, 'ACTIVE'),
(@profile202, 'CCCD', '001202000202', '2021-05-12', 'Cục CSQLHC về TTXH', '2036-05-12', CAST('{"seed":"full_kyc","room":"202"}' AS BINARY), @p202_front, @p202_back, 'ACTIVE'),
(@profile203, 'CCCD', '001203000203', '2022-06-18', 'Cục CSQLHC về TTXH', '2037-06-18', CAST('{"seed":"full_kyc","room":"203"}' AS BINARY), @p203_front, @p203_back, 'ACTIVE');

INSERT INTO tenants (user_id, property_id)
VALUES (@u201,@property_id),(@u202,@property_id),(@u203,@property_id),(@u204,@property_id),(@u205,@property_id);
SET @tenant201 := (SELECT id FROM tenants WHERE user_id=@u201 AND property_id=@property_id);
SET @tenant202 := (SELECT id FROM tenants WHERE user_id=@u202 AND property_id=@property_id);
SET @tenant203 := (SELECT id FROM tenants WHERE user_id=@u203 AND property_id=@property_id);
SET @tenant204 := (SELECT id FROM tenants WHERE user_id=@u204 AND property_id=@property_id);
SET @tenant205 := (SELECT id FROM tenants WHERE user_id=@u205 AND property_id=@property_id);

INSERT INTO vehicles (profile_id, vehicle_type, license_plate, status)
VALUES
(@profile201,'MOTORBIKE','29B1-20101','ACTIVE'),(@profile201,'BICYCLE','BIKE-201','ACTIVE'),
(@profile202,'MOTORBIKE','29B1-20202','ACTIVE'),(@profile203,'MOTORBIKE','29B1-20303','ACTIVE'),
(@profile204,'MOTORBIKE','29B1-20404','ACTIVE'),(@profile205,'MOTORBIKE','29B1-20505','ACTIVE');

INSERT INTO emergency_contacts (tenant_profile_id, full_name, relationship, phone)
VALUES
(@tenant201,'Nguyễn Văn Hùng','Bố','0982000201'),
(@tenant202,'Trần Thị Hoa','Mẹ','0982000202'),
(@tenant203,'Lê Văn Sơn','Anh trai','0982000203'),
(@tenant204,'Phạm Văn Lâm','Bố','0982000204'),
(@tenant205,'Hoàng Thị Lan','Mẹ','0982000205');

-- Deposits converted to leases
INSERT INTO deposit_agreements (deposit_code, room_id, tenant_id, depositor_person_profile_id, amount, expected_move_in_date, expected_lease_sign_date, payment_due_at, deposit_expires_at, status, confirmed_at, note)
VALUES
('COC-2026-201', @room201, @tenant201, @profile201, 2200000, '2026-03-05', '2026-03-05', '2026-03-01 23:59:00', '2026-03-15', 'CONVERTED_TO_LEASE', '2026-03-01 09:00:00', 'Seed: đã chuyển thành HĐ'),
('COC-2026-202', @room202, @tenant202, @profile202, 2300000, '2026-03-11', '2026-03-11', '2026-03-03 23:59:00', '2026-03-20', 'CONVERTED_TO_LEASE', '2026-03-03 09:00:00', 'Seed: đã chuyển thành HĐ'),
('COC-2026-203', @room203, @tenant203, @profile203, 2100000, '2026-02-01', '2026-02-01', '2026-01-25 23:59:00', '2026-02-10', 'CONVERTED_TO_LEASE', '2026-01-25 09:00:00', 'Seed: đã đủ CCCD và đã đổi mật khẩu'),
('COC-2026-204', @room204, @tenant204, @profile204, 2500000, '2026-02-10', '2026-02-10', '2026-02-01 23:59:00', '2026-02-20', 'CONVERTED_TO_LEASE', '2026-02-01 09:00:00', 'Seed: tài khoản mới cấp, chưa đổi mật khẩu lần đầu, chưa đủ CCCD'),
('COC-2026-205', @room205, @tenant205, @profile205, 2150000, '2026-04-15', '2026-04-15', '2026-04-10 23:59:00', '2026-04-25', 'CONVERTED_TO_LEASE', '2026-04-10 09:00:00', 'Seed: tài khoản mới cấp, chưa đổi mật khẩu lần đầu; vào sau ngày 11 nên rent_start_date từ tháng sau');
SET @dep201 := (SELECT id FROM deposit_agreements WHERE deposit_code='COC-2026-201');
SET @dep202 := (SELECT id FROM deposit_agreements WHERE deposit_code='COC-2026-202');
SET @dep203 := (SELECT id FROM deposit_agreements WHERE deposit_code='COC-2026-203');
SET @dep204 := (SELECT id FROM deposit_agreements WHERE deposit_code='COC-2026-204');
SET @dep205 := (SELECT id FROM deposit_agreements WHERE deposit_code='COC-2026-205');

INSERT INTO lease_contracts (contract_code, room_id, deposit_agreement_id, primary_tenant_profile_id, start_date, end_date, rent_start_date, monthly_rent, payment_cycle_months, deposit_amount, status, signed_at, created_by)
VALUES
('HD-2026-201', @room201, @dep201, @tenant201, '2026-03-05', '2027-03-04', '2026-03-05', 2000000, 1, 2200000, 'ACTIVE', '2026-03-05 10:00:00', @owner_tenant_id),
('HD-2026-202', @room202, @dep202, @tenant202, '2026-03-11', '2027-03-10', '2026-04-01', 2300000, 1, 2300000, 'ACTIVE', '2026-03-11 10:00:00', @owner_tenant_id),
('HD-2026-203', @room203, @dep203, @tenant203, '2026-02-01', '2027-01-31', '2026-02-01', 2100000, 3, 2100000, 'ACTIVE', '2026-02-01 10:00:00', @owner_tenant_id),
('HD-2026-204', @room204, @dep204, @tenant204, '2026-02-10', '2027-02-09', '2026-02-10', 2500000, 1, 2500000, 'ACTIVE', '2026-02-10 10:00:00', @owner_tenant_id),
('HD-2026-205', @room205, @dep205, @tenant205, '2026-04-15', '2027-04-14', '2026-05-01', 2150000, 1, 2150000, 'ACTIVE', '2026-04-15 10:00:00', @owner_tenant_id);
SET @contract201 := (SELECT id FROM lease_contracts WHERE contract_code='HD-2026-201');
SET @contract202 := (SELECT id FROM lease_contracts WHERE contract_code='HD-2026-202');
SET @contract203 := (SELECT id FROM lease_contracts WHERE contract_code='HD-2026-203');
SET @contract204 := (SELECT id FROM lease_contracts WHERE contract_code='HD-2026-204');
SET @contract205 := (SELECT id FROM lease_contracts WHERE contract_code='HD-2026-205');

INSERT INTO contract_occupants (contract_id, tenant_id, occupant_role, move_in_date, status)
VALUES
(@contract201, @tenant201, 'PRIMARY', '2026-03-05', 'ACTIVE'),
(@contract202, @tenant202, 'PRIMARY', '2026-03-11', 'ACTIVE'),
(@contract203, @tenant203, 'PRIMARY', '2026-02-01', 'ACTIVE'),
(@contract204, @tenant204, 'PRIMARY', '2026-02-10', 'ACTIVE'),
(@contract205, @tenant205, 'PRIMARY', '2026-04-15', 'ACTIVE');

INSERT INTO contract_events (contract_id, event_type, event_data, created_by)
VALUES
(@contract201,'SIGNED',CAST('{"seed":true,"room":"201"}' AS BINARY),@owner_user_id),
(@contract202,'SIGNED',CAST('{"seed":true,"room":"202"}' AS BINARY),@owner_user_id),
(@contract203,'SIGNED',CAST('{"seed":true,"room":"203","payment_cycle_months":3}' AS BINARY),@owner_user_id),
(@contract204,'SIGNED',CAST('{"seed":true,"room":"204"}' AS BINARY),@owner_user_id),
(@contract205,'SIGNED',CAST('{"seed":true,"room":"205","rent_start_date":"2026-05-01"}' AS BINARY),@owner_user_id);

-- Discount/rent override for P201 to test contract price override
INSERT INTO rent_overrides (contract_id, billing_period, override_monthly_rent, reason, approved_by)
VALUES (@contract201, '2026-05', 1900000, 'Seed: khách ở lâu, giảm giá tháng 05/2026 để test màn ghi đè giá', @owner_user_id);

-- ---------------------------------------------------------
-- 5) Invoices, invoice lines, QR/payment intents and payment history
-- ---------------------------------------------------------
-- 2026-04 paid invoices for history screen
INSERT INTO invoices (invoice_code, property_id, room_id, contract_id, invoice_type, billing_period, issue_date, due_date, status, subtotal_amount, total_amount, paid_amount, remaining_amount, collection_account_id, created_by, issued_at)
VALUES
('HD-2026-04-201-RENT', @property_id, @room201, @contract201, 'RENT',    '2026-04', '2026-04-01', '2026-04-15', 'PAID', 2000000, 2000000, 2000000, 0, @rent_account_id, @owner_user_id, '2026-04-01 08:00:00'),
('HD-2026-04-201-UTL',  @property_id, @room201, @contract201, 'UTILITY', '2026-04', '2026-04-01', '2026-04-05', 'PAID',  525000,  525000,  525000, 0, @utility_account_id, @owner_user_id, '2026-04-01 08:00:00'),
('HD-2026-04-202-RENT', @property_id, @room202, @contract202, 'RENT',    '2026-04', '2026-04-01', '2026-04-15', 'PAID', 2300000, 2300000, 2300000, 0, @rent_account_id, @owner_user_id, '2026-04-01 08:00:00'),
('HD-2026-04-202-UTL',  @property_id, @room202, @contract202, 'UTILITY', '2026-04', '2026-04-01', '2026-04-05', 'PAID',   87500,   87500,   87500, 0, @utility_account_id, @owner_user_id, '2026-04-01 08:00:00');

-- 2026-05 invoices: mix ISSUED, OVERDUE, PARTIALLY_PAID, PAID for payment testing
INSERT INTO invoices (invoice_code, property_id, room_id, contract_id, invoice_type, billing_period, issue_date, due_date, status, subtotal_amount, total_amount, paid_amount, remaining_amount, collection_account_id, created_by, issued_at)
VALUES
('HD-2026-05-201-RENT', @property_id, @room201, @contract201, 'RENT',    '2026-05', '2026-05-01', '2026-05-15', 'ISSUED', 1900000, 1900000,       0, 1900000, @rent_account_id, @owner_user_id, '2026-05-01 08:00:00'),
('HD-2026-05-201-UTL',  @property_id, @room201, @contract201, 'UTILITY', '2026-05', '2026-05-01', '2026-05-05', 'ISSUED',  645000,  645000,       0,  645000, @utility_account_id, @owner_user_id, '2026-05-01 08:00:00'),
('HD-2026-05-202-RENT', @property_id, @room202, @contract202, 'RENT',    '2026-05', '2026-05-01', '2026-05-15', 'PAID',   2300000, 2300000, 2300000,       0, @rent_account_id, @owner_user_id, '2026-05-01 08:00:00'),
('HD-2026-05-202-UTL',  @property_id, @room202, @contract202, 'UTILITY', '2026-05', '2026-05-01', '2026-05-05', 'PAID',    87500,   87500,   87500,       0, @utility_account_id, @owner_user_id, '2026-05-01 08:00:00'),
('HD-2026-05-203-RENT', @property_id, @room203, @contract203, 'RENT',    '2026-05', '2026-05-01', '2026-05-15', 'OVERDUE',6300000, 6300000,       0, 6300000, @rent_account_id, @owner_user_id, '2026-05-01 08:00:00'),
('HD-2026-05-203-UTL',  @property_id, @room203, @contract203, 'UTILITY', '2026-05', '2026-05-01', '2026-05-05', 'OVERDUE',  585000,  585000,       0,  585000, @utility_account_id, @owner_user_id, '2026-05-01 08:00:00'),
('HD-2026-05-204-RENT', @property_id, @room204, @contract204, 'RENT',    '2026-05', '2026-05-01', '2026-05-15', 'PARTIALLY_PAID',2500000,2500000,1000000,1500000,@rent_account_id,@owner_user_id,'2026-05-01 08:00:00'),
('HD-2026-05-204-UTL',  @property_id, @room204, @contract204, 'UTILITY', '2026-05', '2026-05-01', '2026-05-05', 'ISSUED',  530000,  530000,       0,  530000, @utility_account_id, @owner_user_id, '2026-05-01 08:00:00'),
('HD-2026-05-205-RENT', @property_id, @room205, @contract205, 'RENT',    '2026-05', '2026-05-01', '2026-05-15', 'ISSUED', 2150000, 2150000,       0, 2150000, @rent_account_id, @owner_user_id, '2026-05-01 08:00:00'),
('HD-2026-05-205-UTL',  @property_id, @room205, @contract205, 'UTILITY', '2026-05', '2026-05-01', '2026-05-05', 'ISSUED',   87500,   87500,       0,   87500, @utility_account_id, @owner_user_id, '2026-05-01 08:00:00');

-- Convenient invoice id variables
SET @inv201_rent := (SELECT id FROM invoices WHERE invoice_code='HD-2026-05-201-RENT'); SET @inv201_utl := (SELECT id FROM invoices WHERE invoice_code='HD-2026-05-201-UTL');
SET @inv202_rent := (SELECT id FROM invoices WHERE invoice_code='HD-2026-05-202-RENT'); SET @inv202_utl := (SELECT id FROM invoices WHERE invoice_code='HD-2026-05-202-UTL');
SET @inv203_rent := (SELECT id FROM invoices WHERE invoice_code='HD-2026-05-203-RENT'); SET @inv203_utl := (SELECT id FROM invoices WHERE invoice_code='HD-2026-05-203-UTL');
SET @inv204_rent := (SELECT id FROM invoices WHERE invoice_code='HD-2026-05-204-RENT'); SET @inv204_utl := (SELECT id FROM invoices WHERE invoice_code='HD-2026-05-204-UTL');
SET @inv205_rent := (SELECT id FROM invoices WHERE invoice_code='HD-2026-05-205-RENT'); SET @inv205_utl := (SELECT id FROM invoices WHERE invoice_code='HD-2026-05-205-UTL');

-- Invoice lines, amounts are generated = quantity * unit_price. For WATER, quantity is billable m3 after 6m3 free.
INSERT INTO invoice_lines (invoice_id, line_type, description, quantity, unit_price, meter_reading_id, collection_account_id)
VALUES
(@inv201_rent,'ROOM_RENT','Tiền phòng 201 tháng 05/2026 - đã áp dụng giảm giá',1,1900000,NULL,@rent_account_id),
(@inv201_utl,'ELECTRICITY','Điện T5/2026: 130 kWh × 3.500đ',130,3500,(SELECT id FROM meter_readings WHERE meter_id=@e201 AND reading_period='2026-05'),@utility_account_id),
(@inv201_utl,'WATER','Nước T5/2026: (13-6)m³ × 20.000đ',7,20000,(SELECT id FROM meter_readings WHERE meter_id=@w201 AND reading_period='2026-05'),@utility_account_id),
(@inv201_utl,'SERVICE_FEE','Phí dịch vụ T5/2026',1,50000,NULL,@utility_account_id),
(@inv202_rent,'ROOM_RENT','Tiền phòng 202 tháng 05/2026',1,2300000,NULL,@rent_account_id),
(@inv202_utl,'ELECTRICITY','Điện T5/2026: 25 kWh × 3.500đ',25,3500,(SELECT id FROM meter_readings WHERE meter_id=@e202 AND reading_period='2026-05'),@utility_account_id),
(@inv202_utl,'SERVICE_FEE','Miễn phí DV vì tiền điện < 100.000đ',1,0,NULL,@utility_account_id),
(@inv203_rent,'ROOM_RENT','Tiền phòng 203 chu kỳ 3 tháng T5-T7/2026',3,2100000,NULL,@rent_account_id),
(@inv203_utl,'ELECTRICITY','Điện T5/2026: 130 kWh × 3.500đ',130,3500,(SELECT id FROM meter_readings WHERE meter_id=@e203 AND reading_period='2026-05'),@utility_account_id),
(@inv203_utl,'WATER','Nước T5/2026: (10-6)m³ × 20.000đ',4,20000,(SELECT id FROM meter_readings WHERE meter_id=@w203 AND reading_period='2026-05'),@utility_account_id),
(@inv203_utl,'SERVICE_FEE','Phí dịch vụ T5/2026',1,50000,NULL,@utility_account_id),
(@inv204_rent,'ROOM_RENT','Tiền phòng 204 tháng 05/2026',1,2500000,NULL,@rent_account_id),
(@inv204_utl,'ELECTRICITY','Điện T5/2026: 120 kWh × 3.500đ',120,3500,(SELECT id FROM meter_readings WHERE meter_id=@e204 AND reading_period='2026-05'),@utility_account_id),
(@inv204_utl,'WATER','Nước T5/2026: (8-6)m³ × 20.000đ',2,20000,(SELECT id FROM meter_readings WHERE meter_id=@w204 AND reading_period='2026-05'),@utility_account_id),
(@inv204_utl,'SERVICE_FEE','Phí dịch vụ T5/2026',1,50000,NULL,@utility_account_id),
(@inv204_utl,'MAINTENANCE_COMPENSATION','Bồi thường remote điều hoà hỏng',1,20000,NULL,@utility_account_id),
(@inv205_rent,'ROOM_RENT','Tiền phòng 205 tháng đầu tính tiền 05/2026',1,2150000,NULL,@rent_account_id),
(@inv205_utl,'ELECTRICITY','Điện T5/2026: 25 kWh × 3.500đ',25,3500,(SELECT id FROM meter_readings WHERE meter_id=@e205 AND reading_period='2026-05'),@utility_account_id),
(@inv205_utl,'SERVICE_FEE','Miễn phí DV vì tiền điện < 100.000đ',1,0,NULL,@utility_account_id);

-- Payment intents / QR content for unpaid and partial invoices
INSERT INTO payment_intents (invoice_id, amount, provider, collection_account_id, payment_content, qr_payload, status, expires_at)
VALUES
(@inv201_rent,1900000,'VIETQR',@rent_account_id,   'HD-2026-05-201-RENT', 'vietqr://seed/HD-2026-05-201-RENT', 'PENDING', '2026-05-31 23:59:59'),
(@inv201_utl, 645000,'VIETQR',@utility_account_id,'HD-2026-05-201-UTL',  'vietqr://seed/HD-2026-05-201-UTL',  'PENDING', '2026-05-31 23:59:59'),
(@inv203_rent,6300000,'VIETQR',@rent_account_id,   'HD-2026-05-203-RENT', 'vietqr://seed/HD-2026-05-203-RENT', 'PENDING', '2026-05-31 23:59:59'),
(@inv203_utl, 585000,'VIETQR',@utility_account_id,'HD-2026-05-203-UTL',  'vietqr://seed/HD-2026-05-203-UTL',  'PENDING', '2026-05-31 23:59:59'),
(@inv204_rent,1500000,'VIETQR',@rent_account_id,   'HD-2026-05-204-RENT', 'vietqr://seed/HD-2026-05-204-RENT', 'PENDING', '2026-05-31 23:59:59'),
(@inv204_utl, 530000,'VIETQR',@utility_account_id,'HD-2026-05-204-UTL',  'vietqr://seed/HD-2026-05-204-UTL',  'PENDING', '2026-05-31 23:59:59'),
(@inv205_rent,2150000,'VIETQR',@rent_account_id,   'HD-2026-05-205-RENT', 'vietqr://seed/HD-2026-05-205-RENT', 'PENDING', '2026-05-31 23:59:59'),
(@inv205_utl,  87500,'VIETQR',@utility_account_id,'HD-2026-05-205-UTL',  'vietqr://seed/HD-2026-05-205-UTL',  'PENDING', '2026-05-31 23:59:59');

-- Paid/partial payment history
INSERT INTO payment_transactions (provider, provider_transaction_id, collection_account_id, amount, transaction_time, payer_name, payer_account, content, status, confirmed_by, confirmed_at)
VALUES
('BANK','SEED-TXN-202-RENT-202605',@rent_account_id,2300000,'2026-05-02 09:30:00','Tran Thi Binh','9704xxxx202','HD-2026-05-202-RENT','MATCHED',@manager_user_id,'2026-05-02 09:31:00'),
('BANK','SEED-TXN-202-UTL-202605', @utility_account_id,87500,'2026-05-02 09:35:00','Tran Thi Binh','9704xxxx202','HD-2026-05-202-UTL','MATCHED',@manager_user_id,'2026-05-02 09:36:00'),
('BANK','SEED-TXN-204-PART-202605',@rent_account_id,1000000,'2026-05-03 10:00:00','Pham Thu Dung','9704xxxx204','HD-2026-05-204-RENT','PARTIALLY_ALLOCATED',@manager_user_id,'2026-05-03 10:01:00');
SET @txn202rent := (SELECT id FROM payment_transactions WHERE provider_transaction_id='SEED-TXN-202-RENT-202605');
SET @txn202utl  := (SELECT id FROM payment_transactions WHERE provider_transaction_id='SEED-TXN-202-UTL-202605');
SET @txn204part := (SELECT id FROM payment_transactions WHERE provider_transaction_id='SEED-TXN-204-PART-202605');
INSERT INTO payment_allocations (payment_transaction_id, invoice_id, amount, allocated_by)
VALUES (@txn202rent,@inv202_rent,2300000,@manager_user_id),(@txn202utl,@inv202_utl,87500,@manager_user_id),(@txn204part,@inv204_rent,1000000,@manager_user_id);

-- Debt snapshots for dashboard / debt badge / transfer request screen
INSERT INTO debt_snapshots (room_id, contract_id, snapshot_date, rent_debt_amount, utility_debt_amount, other_debt_amount, rent_debt_months, utility_debt_months, mixed_debt_amount, debt_limit_amount, is_over_limit)
VALUES
(@room201,@contract201,'2026-05-17',1900000,645000,0,1,1,2545000,1333333,TRUE),
(@room202,@contract202,'2026-05-17',0,0,0,0,0,0,1533333,FALSE),
(@room203,@contract203,'2026-05-17',6300000,585000,0,3,1,6885000,4200000,TRUE),
(@room204,@contract204,'2026-05-17',1500000,530000,0,1,1,2030000,1666666,TRUE),
(@room205,@contract205,'2026-05-17',2150000,87500,0,1,1,2237500,1433333,TRUE);
SET @debt203 := (SELECT id FROM debt_snapshots WHERE room_id=@room203 AND snapshot_date='2026-05-17');

-- Double-entry style ledger examples
INSERT INTO ledger_entries (entry_code, entry_date, source_type, source_id, account_code, debit_amount, credit_amount, description)
VALUES
('LEDGER-SEED-202-RENT-DR','2026-05-02','PAYMENT',@txn202rent,'CASH_RENT',2300000,0,'Thu tiền phòng 202 tháng 05/2026'),
('LEDGER-SEED-202-RENT-CR','2026-05-02','PAYMENT',@txn202rent,'AR_RENT',0,2300000,'Cấn trừ công nợ phòng 202'),
('LEDGER-SEED-202-UTL-DR','2026-05-02','PAYMENT',@txn202utl,'CASH_UTILITY',87500,0,'Thu điện nước phòng 202 tháng 05/2026'),
('LEDGER-SEED-202-UTL-CR','2026-05-02','PAYMENT',@txn202utl,'AR_UTILITY',0,87500,'Cấn trừ công nợ điện nước phòng 202');

-- ---------------------------------------------------------
-- 6) Extra data for screens: deposit, OCR, transfer, maintenance, notifications, audit
-- ---------------------------------------------------------
-- Vacant room 206 has a deposit form to test deposit pipeline without affecting 5 occupied rooms.
INSERT INTO deposit_forms (room_id, id_number, full_name, email, phone, expected_move_in_date, expected_lease_sign_date, payment_due_at, deposit_expires_at, status, confirmed_at)
VALUES (@room206, '001206000206', 'Vũ Minh Tân', 'tan206@lead.test', '0912000206', '2026-06-05', '2026-06-05', '2026-05-20 23:59:59', '2026-06-20', 'APPROVED', '2026-05-17 09:00:00');

-- OCR review-required job for tenant 203 identity verification screen
INSERT INTO file_metadata (owner_user_id, storage_key, original_name, mime_type, size_bytes, sha256_checksum, category, is_sensitive)
VALUES (@u203, 'seed/tenant-203/ocr-cccd-front.jpg', 'ocr-cccd-front-203.jpg', 'image/jpeg', 230000, REPEAT('3',64), 'OCR_INPUT', TRUE);
SET @ocr_file_203 := LAST_INSERT_ID();
INSERT INTO ocr_jobs (input_file_id, document_type, status, target_type, target_id, raw_result)
VALUES (@ocr_file_203, 'IDENTITY_DOCUMENT', 'REVIEW_REQUIRED', 'PERSON_PROFILE', @profile203, CAST('{"provider":"seed","note":"needs_review"}' AS BINARY));
SET @ocr_job_203 := LAST_INSERT_ID();
INSERT INTO ocr_extracted_fields (ocr_job_id, field_name, extracted_value, corrected_value, confidence, status)
VALUES
(@ocr_job_203,'full_name','LE MINH CUONG',NULL,0.9500,'EXTRACTED'),
(@ocr_job_203,'cccd_number','001203000203',NULL,0.9900,'EXTRACTED'),
(@ocr_job_203,'date_of_birth','09/01/2003',NULL,0.8800,'EXTRACTED'),
(@ocr_job_203,'permanent_address','Chua cap nhat',NULL,0.7200,'EXTRACTED');

-- Transfer request from room 203 to vacant room 206
INSERT INTO room_transfer_requests (request_code, requester_id, old_contract_id, old_room_id, target_room_id, requested_transfer_date, reason, status, debt_snapshot_id, eligibility_checked_at, is_eligible_at_creation, eligibility_snapshot)
VALUES ('TR-2026-203-206', @tenant203, @contract203, @room203, @room206, '2026-06-01', 'Muốn chuyển sang phòng rộng hơn để tiện học tập', 'PENDING', @debt203, NOW(6), TRUE, CAST('{"elapsed_ratio":0.75,"transfer_count_12m":0,"target_room_status":"VACANT"}' AS BINARY));
SET @transfer203 := LAST_INSERT_ID();
INSERT INTO transfer_settlements (transfer_request_id, old_room_remaining_value, new_room_required_value, difference_amount, settlement_type, confirmed_by, confirmed_at)
VALUES (@transfer203, 4200000, 5200000, 1000000, 'TENANT_PAY_MORE', @owner_user_id, NOW(6));

-- Maintenance ticket for room 204 with tenant compensation line already reflected in utility invoice
INSERT INTO maintenance_tickets (ticket_code, property_id, room_id, contract_id, created_by, ticket_scope, priority, category, title, description, status, assigned_to, worker_name, repair_items, completed_at)
VALUES ('SC-2026-204-001', @property_id, @room204, @contract204, @u204, 'TENANT_ROOM', 'HIGH', 'ASSET_DAMAGE', 'Remote điều hoà bị hỏng', 'Khách báo remote điều hoà không hoạt động, cần thay mới.', 'COMPLETED', @manager_user_id, 'Thợ Hùng - 0987654321', 'Thay remote mới', '2026-05-04 17:30:00');
SET @ticket204 := LAST_INSERT_ID();
INSERT INTO maintenance_ticket_events (ticket_id, from_status, to_status, note, created_by)
VALUES (@ticket204,NULL,'PENDING_ACCEPTANCE','Khách tạo ticket',@u204),(@ticket204,'PENDING_ACCEPTANCE','COMPLETED','Đã thay remote mới',@manager_user_id);
INSERT INTO maintenance_costs (ticket_id, cost_type, description, amount, paid_by, charge_invoice_id, created_by)
VALUES (@ticket204, 'TENANT_COMPENSATION', 'Remote điều hoà mới', 200000, 'TENANT', @inv204_utl, @manager_user_id);

-- Property rule and violation for room 201 to test violation/fine screens
INSERT INTO property_rules (property_id, rule_code, title, description, default_fine_amount, sort_order, status)
VALUES (@property_id, 'WIFI_RESET', 'Không tự ý reset modem wifi', 'Không tự ý reset mật khẩu modem wifi. Vi phạm phạt 200.000đ/lần.', 200000, 1, 'ACTIVE');
SET @rule_wifi := LAST_INSERT_ID();
INSERT INTO rule_violations (property_id, room_id, contract_id, tenant_profile_id, rule_id, violation_date, description, fine_amount, status, created_by)
VALUES (@property_id, @room201, @contract201, @tenant201, @rule_wifi, '2026-05-06', 'Reset mật khẩu modem khi chưa được phép', 200000, 'RECORDED', @manager_user_id);

-- Notifications for each tenant: invoices, debt, identity prompt, transfer
INSERT INTO notification_templates (template_key, channel, title_template, body_template, status)
VALUES
('INVOICE_ISSUED','PUSH','Hóa đơn {room_code}','Hóa đơn {billing_period}: {amount}đ. Hạn thanh toán: {due_date}','ACTIVE'),
('IDENTITY_REQUIRED','PUSH','Cần bổ sung CCCD','Tài khoản mới cấp cần hoàn tất xác thực danh tính.','ACTIVE'),
('DEBT_WARNING','PUSH','Cảnh báo công nợ','Phòng {room_code} đang có công nợ quá hạn.','ACTIVE');

INSERT INTO notification_outbox (event_type, target_type, target_id, recipient_user_id, channel, title, body, payload, status, scheduled_at, sent_at)
VALUES
('INVOICE_ISSUED','INVOICE',@inv201_rent,@u201,'PUSH','Hóa đơn tháng 05/2026','Phòng 201 cần thanh toán tiền phòng 1.900.000đ và điện nước 645.000đ.', JSON_OBJECT('room_code','201','billing_period','2026-05'), 'SENT','2026-05-01 08:00:00','2026-05-01 08:00:05'),
('INVOICE_PAID','INVOICE',@inv202_rent,@u202,'PUSH','Thanh toán thành công','Phòng 202 đã thanh toán đủ hóa đơn tháng 05/2026.', JSON_OBJECT('room_code','202','billing_period','2026-05'), 'SENT','2026-05-02 09:40:00','2026-05-02 09:40:05'),
('DEBT_WARNING','DEBT_SNAPSHOT',@debt203,@u203,'PUSH','Cảnh báo công nợ','Phòng 203 đang quá hạn tiền phòng chu kỳ 3 tháng.', JSON_OBJECT('room_code','203','amount',6885000), 'PENDING','2026-05-17 08:00:00',NULL),
('IDENTITY_REQUIRED','PERSON_PROFILE',@profile204,@u204,'PUSH','Cần bổ sung CCCD','Vui lòng hoàn tất xác thực CCCD cho tài khoản mới cấp.', JSON_OBJECT('room_code','204'), 'SENT','2026-05-17 08:10:00','2026-05-17 08:10:05'),
('IDENTITY_REQUIRED','PERSON_PROFILE',@profile205,@u205,'PUSH','Cần bổ sung CCCD','Vui lòng hoàn tất xác thực CCCD cho tài khoản mới cấp.', JSON_OBJECT('room_code','205'), 'SENT','2026-05-17 08:10:00','2026-05-17 08:10:05'),
('TRANSFER_REQUEST_CREATED','ROOM_TRANSFER_REQUEST',@transfer203,@owner_user_id,'PUSH','Có đơn chuyển phòng mới','Phòng 203 muốn chuyển sang phòng 206.', JSON_OBJECT('from','203','to','206'), 'SENT','2026-05-17 09:00:00','2026-05-17 09:00:04');
SET @outbox_first := (SELECT MIN(id) FROM notification_outbox WHERE event_type='INVOICE_ISSUED' AND recipient_user_id=@u201);
INSERT INTO notification_deliveries (outbox_id, provider_message_id, delivery_status, delivered_at, read_at)
VALUES (@outbox_first, 'seed-msg-001', 'READ', '2026-05-01 08:00:06', '2026-05-01 08:05:00');

INSERT INTO scheduled_tasks (task_type, target_type, target_id, due_at, status, payload)
VALUES
('INVOICE_REMINDER','INVOICE',@inv203_rent,'2026-05-18 08:00:00','PENDING',CAST('{"room_code":"203","reminder":"overdue_3_days"}' AS BINARY)),
('CONTRACT_EXPIRY','CONTRACT',@contract201,'2026-12-04 08:00:00','PENDING',CAST('{"room_code":"201","months_before":3}' AS BINARY));

-- Audit samples
INSERT INTO audit_logs (actor_user_id, action, entity_type, entity_id, before_json, after_json, ip_address, user_agent)
VALUES
(@owner_user_id,'SEED_DATA_CREATED','PROPERTY',@property_id,NULL,CAST('{"seed":"hdbhms_test_data","rooms":["201","202","203","204","205"]}' AS BINARY),'127.0.0.1','seed-script'),
(@u201,'LOGIN_SUCCESS','USER',@u201,NULL,CAST('{"room":"201"}' AS BINARY),'127.0.0.1','mobile-seed'),
(@manager_user_id,'SENSITIVE_DATA_VIEWED','IDENTITY_DOCUMENT',(SELECT id FROM identity_documents WHERE profile_id=@profile201),NULL,CAST('{"reason":"seed audit sample"}' AS BINARY),'127.0.0.1','web-seed');

COMMIT;
SET FOREIGN_KEY_CHECKS = @OLD_FOREIGN_KEY_CHECKS;

-- Quick check after running:
-- SELECT u.phone, u.email, u.role, u.status, r.room_code, c.contract_code
-- FROM users u
-- JOIN tenants t ON t.user_id = u.id
-- JOIN lease_contracts c ON c.primary_tenant_profile_id = t.id
-- JOIN rooms r ON r.id = c.room_id
-- WHERE u.role='TENANT' AND r.property_id=@property_id
-- ORDER BY r.room_code;
