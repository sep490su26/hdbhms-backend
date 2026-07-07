-- =========================================================
-- V7__seed_10_tenant_mobile_test_accounts.sql
-- Purpose:
--   Seed thêm 10 tài khoản KHÁCH THUÊ để test mobile.
--   Bản này đã sửa để khớp schema V0 hiện tại.
--
-- V0 hiện tại KHÔNG có:
--   - users.full_name
--   - tenant_memberships
--   - tenant_profiles
--   - properties.tenant_id
--   - person_profiles.tenant_id / date_of_birth
--   - identity_documents.tenant_id / person_id
--   - emergency_contacts.tenant_id
--
-- V0 hiện tại dùng:
--   - users(role, status, must_change_password)
--   - tenants(user_id, property_id) như bảng liên kết user thuê cơ sở
--   - person_profiles(dob, phone, email, ...)
--   - identity_documents(profile_id, ...)
--   - emergency_contacts(tenant_profile_id, ...) ; ở schema cũ dùng tenants.id làm tenant_profile_id
--
-- Default password for all accounts: Abc12345
-- BCrypt hash generated with cost 10.
-- =========================================================

SET NAMES utf8mb4;

SET @property_id := COALESCE(
  (SELECT id FROM properties WHERE property_code = 'HD1' AND deleted_at IS NULL LIMIT 1),
  (SELECT id FROM properties WHERE deleted_at IS NULL ORDER BY id LIMIT 1)
);

SET @default_password_hash := '$2a$10$Bakw8whVmG.DHvmiCQ8puOTcQiuzmUPFVIZqjmlSSKQluAxEpIVg6';

DROP TEMPORARY TABLE IF EXISTS v7_mobile_accounts;

CREATE TEMPORARY TABLE v7_mobile_accounts (
  full_name VARCHAR(255) NOT NULL,
  phone VARCHAR(30) NOT NULL,
  email VARCHAR(255) NOT NULL,
  dob DATE NULL,
  gender ENUM('MALE','FEMALE','OTHER','UNKNOWN') NOT NULL DEFAULT 'UNKNOWN',
  permanent_address VARCHAR(1000) NULL,
  doc_number VARCHAR(50) NULL,
  issued_date DATE NULL,
  issued_place VARCHAR(255) NULL,
  expiry_date DATE NULL,
  emergency_name VARCHAR(255) NULL,
  emergency_relationship VARCHAR(100) NULL,
  emergency_phone VARCHAR(30) NULL
) ENGINE = MEMORY;

INSERT INTO v7_mobile_accounts
(full_name, phone, email, dob, gender, permanent_address, doc_number, issued_date, issued_place, expiry_date, emergency_name, emergency_relationship, emergency_phone)
VALUES
('Nguyễn Test Tenant 01', '0362001001', 'tenant.test01@hdbhms.local', '2001-01-11', 'MALE',   NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
('Trần Test Tenant 02',   '0362001002', 'tenant.test02@hdbhms.local', '2002-02-12', 'FEMALE', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
('Lê Test Tenant 03',     '0362001003', 'tenant.test03@hdbhms.local', '2000-03-13', 'MALE',   NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
('Phạm Test Tenant 04',   '0362001004', 'tenant.test04@hdbhms.local', '1999-04-14', 'FEMALE', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
('Hoàng Test Tenant 05',  '0362001005', 'tenant.test05@hdbhms.local', '2001-05-15', 'MALE',   NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
('Vũ Test Tenant 06',     '0362001006', 'tenant.test06@hdbhms.local', '2002-06-16', 'FEMALE', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
('Đặng Test Tenant 07',   '0362001007', 'tenant.test07@hdbhms.local', '2000-07-17', 'MALE',   NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
('Bùi Test Tenant 08',    '0362001008', 'tenant.test08@hdbhms.local', '2001-08-18', 'FEMALE', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
('Tiến Nguyễn Hữu OTP Test', '0349486804', 'tiennguyenhuu97531@gmail.com', '2003-09-19', 'MALE', 'Thạch Thất, Hà Nội', '034203009999', '2021-09-19', 'Cục CSQLHC về TTXH', '2036-09-19', 'Nguyễn Văn Liên', 'Người thân', '0987654321'),
('Đỗ Test Tenant 10 Complete', '0362001010', 'tenant.test10@hdbhms.local', '1998-10-20', 'MALE', 'Thạch Thất, Hà Nội', '036098001010', '2020-10-20', 'Cục CSQLHC về TTXH', '2035-10-20', 'Đỗ Văn Hùng', 'Người thân', '0987654330');

-- 1) Tạo user đăng nhập mobile.
-- V0 không có users.full_name, nên tên nằm ở person_profiles.
INSERT INTO users (phone, email, password_hash, role, status, email_verified, must_change_password)
SELECT a.phone, a.email, @default_password_hash, 'TENANT', 'ACTIVE', TRUE, TRUE
FROM v7_mobile_accounts a
WHERE @property_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM users u
    WHERE u.phone = a.phone
      AND u.deleted_at IS NULL
  );

-- 2) Tạo hồ sơ cá nhân theo schema V0: dob, không phải date_of_birth; không có tenant_id.
INSERT INTO person_profiles (full_name, dob, gender, phone, email, permanent_address, portrait_file_id)
SELECT a.full_name, a.dob, a.gender, a.phone, a.email, a.permanent_address, NULL
FROM v7_mobile_accounts a
WHERE @property_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM person_profiles p
    WHERE p.phone = a.phone
      AND p.deleted_at IS NULL
  );

-- 3) Gắn user vào cơ sở bằng bảng tenants cũ: tenants(user_id, property_id).
INSERT INTO tenants (user_id, property_id)
SELECT u.id, @property_id
FROM v7_mobile_accounts a
JOIN users u ON u.phone = a.phone AND u.deleted_at IS NULL
WHERE @property_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM tenants t
    WHERE t.user_id = u.id
      AND t.property_id = @property_id
      AND t.deleted_at IS NULL
  );

-- 4) Chỉ account 09 và 10 có CCCD để test luồng hồ sơ đã hoàn tất.
-- V0 dùng identity_documents.profile_id, không có tenant_id/person_id.
INSERT INTO identity_documents (profile_id, doc_type, doc_number, issued_date, issued_place, expiry_date, status)
SELECT p.id, 'CCCD', a.doc_number, a.issued_date, a.issued_place, a.expiry_date, 'ACTIVE'
FROM v7_mobile_accounts a
JOIN person_profiles p ON p.phone = a.phone AND p.deleted_at IS NULL
WHERE @property_id IS NOT NULL
  AND a.doc_number IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM identity_documents d
    WHERE d.doc_type = 'CCCD'
      AND d.doc_number = a.doc_number
  );

-- 5) Emergency contact theo schema V0.
-- Cột tenant_profile_id trong V0 được dùng với tenants.id vì không có bảng tenant_profiles.
INSERT INTO emergency_contacts (tenant_profile_id, full_name, relationship, phone)
SELECT t.id, a.emergency_name, a.emergency_relationship, a.emergency_phone
FROM v7_mobile_accounts a
JOIN users u ON u.phone = a.phone AND u.deleted_at IS NULL
JOIN tenants t ON t.user_id = u.id AND t.property_id = @property_id AND t.deleted_at IS NULL
WHERE @property_id IS NOT NULL
  AND a.emergency_phone IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM emergency_contacts ec
    WHERE ec.tenant_profile_id = t.id
      AND ec.phone = a.emergency_phone
  );

DROP TEMPORARY TABLE IF EXISTS v7_mobile_accounts;

-- Quick verification
SELECT
  'V7 tenant mobile test accounts seeded for V0 schema' AS message,
  @property_id AS property_id,
  COUNT(*) AS seeded_user_count
FROM users
WHERE phone IN (
  '0362001001','0362001002','0362001003','0362001004','0362001005',
  '0362001006','0362001007','0362001008','0349486804','0362001010'
)
  AND deleted_at IS NULL;
