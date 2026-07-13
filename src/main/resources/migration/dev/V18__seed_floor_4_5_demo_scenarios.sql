SET NAMES utf8mb4;

-- Demo dataset scope: HAI_DANG_1, rooms 401-408 and 501-507 only.
-- All identifiers use natural keys; numeric primary keys remain database-generated.
SET @property_id := (SELECT property_id FROM hdbhms.properties WHERE property_code = 'HAI_DANG_1' LIMIT 1);
SET @password_hash := '$2a$10$2Dy4Vg1B5BKuiUMPRuTAluvk/0XzLuSgLGaABFHCoWHaUfUtDFGqm';

SET @r401 := (SELECT room_id FROM hdbhms.rooms WHERE property_id=@property_id AND room_code='401' AND deleted_at IS NULL LIMIT 1);
SET @r402 := (SELECT room_id FROM hdbhms.rooms WHERE property_id=@property_id AND room_code='402' AND deleted_at IS NULL LIMIT 1);
SET @r403 := (SELECT room_id FROM hdbhms.rooms WHERE property_id=@property_id AND room_code='403' AND deleted_at IS NULL LIMIT 1);
SET @r404 := (SELECT room_id FROM hdbhms.rooms WHERE property_id=@property_id AND room_code='404' AND deleted_at IS NULL LIMIT 1);
SET @r405 := (SELECT room_id FROM hdbhms.rooms WHERE property_id=@property_id AND room_code='405' AND deleted_at IS NULL LIMIT 1);
SET @r406 := (SELECT room_id FROM hdbhms.rooms WHERE property_id=@property_id AND room_code='406' AND deleted_at IS NULL LIMIT 1);
SET @r407 := (SELECT room_id FROM hdbhms.rooms WHERE property_id=@property_id AND room_code='407' AND deleted_at IS NULL LIMIT 1);
SET @r408 := (SELECT room_id FROM hdbhms.rooms WHERE property_id=@property_id AND room_code='408' AND deleted_at IS NULL LIMIT 1);
SET @r501 := (SELECT room_id FROM hdbhms.rooms WHERE property_id=@property_id AND room_code='501' AND deleted_at IS NULL LIMIT 1);
SET @r502 := (SELECT room_id FROM hdbhms.rooms WHERE property_id=@property_id AND room_code='502' AND deleted_at IS NULL LIMIT 1);
SET @r503 := (SELECT room_id FROM hdbhms.rooms WHERE property_id=@property_id AND room_code='503' AND deleted_at IS NULL LIMIT 1);
SET @r504 := (SELECT room_id FROM hdbhms.rooms WHERE property_id=@property_id AND room_code='504' AND deleted_at IS NULL LIMIT 1);
SET @r505 := (SELECT room_id FROM hdbhms.rooms WHERE property_id=@property_id AND room_code='505' AND deleted_at IS NULL LIMIT 1);
SET @r506 := (SELECT room_id FROM hdbhms.rooms WHERE property_id=@property_id AND room_code='506' AND deleted_at IS NULL LIMIT 1);
SET @r507 := (SELECT room_id FROM hdbhms.rooms WHERE property_id=@property_id AND room_code='507' AND deleted_at IS NULL LIMIT 1);

-- Accounts. Login password for every account is Test12345678.
INSERT INTO hdbhms.users
    (phone, email, password_hash, role, status, last_login_at, email_verified, must_change_password, created_at, updated_at, deleted_at)
VALUES
    ('0988000001','demo.owner@hdbhms.local',@password_hash,'OWNER','ACTIVE','2026-07-10 08:00:00',TRUE,FALSE,'2026-01-01 08:00:00','2026-07-10 08:00:00',NULL),
    ('0988000002','demo.manager@hdbhms.local',@password_hash,'MANAGER','ACTIVE','2026-07-10 08:10:00',TRUE,FALSE,'2026-01-01 08:05:00','2026-07-10 08:10:00',NULL),
    ('0988000003','demo.accountant@hdbhms.local',@password_hash,'ACCOUNTANT','ACTIVE','2026-07-10 08:20:00',TRUE,FALSE,'2026-01-01 08:10:00','2026-07-10 08:20:00',NULL),
    ('0988000004','demo.guest@hdbhms.local',@password_hash,'LEAD','PENDING_CONTRACT',NULL,TRUE,FALSE,'2026-07-01 09:00:00','2026-07-01 09:00:00',NULL),
    ('0988404001','demo.tenant404@hdbhms.local',@password_hash,'TENANT','ACTIVE','2026-07-10 19:00:00',TRUE,FALSE,'2025-09-01 08:00:00','2026-07-10 19:00:00',NULL),
    ('0988404002','demo.tenant404.co1@hdbhms.local',@password_hash,'TENANT','ACTIVE','2026-07-09 19:00:00',TRUE,FALSE,'2025-09-01 08:05:00','2026-07-09 19:00:00',NULL),
    ('0988404003','demo.tenant404.co2@hdbhms.local',@password_hash,'TENANT','ACTIVE',NULL,TRUE,FALSE,'2025-09-01 08:10:00','2025-09-01 08:10:00',NULL),
    ('0988405001','demo.tenant405@hdbhms.local',@password_hash,'TENANT','ACTIVE','2026-07-08 20:00:00',TRUE,FALSE,'2026-01-01 08:00:00','2026-07-08 20:00:00',NULL),
    ('0988405002','demo.tenant405.pending@hdbhms.local',@password_hash,'TENANT','PENDING_CONTRACT',NULL,FALSE,TRUE,'2026-07-05 08:00:00','2026-07-05 08:00:00',NULL),
    ('0988406001','demo.tenant406@hdbhms.local',@password_hash,'TENANT','ACTIVE','2026-07-10 20:00:00',TRUE,FALSE,'2025-09-01 08:00:00','2026-07-10 20:00:00',NULL),
    ('0988407001','demo.tenant407@hdbhms.local',@password_hash,'TENANT','ACTIVE','2026-07-09 20:00:00',TRUE,FALSE,'2025-07-01 08:00:00','2026-07-09 20:00:00',NULL),
    ('0988501001','demo.tenant501@hdbhms.local',@password_hash,'TENANT','ACTIVE','2026-07-10 21:00:00',TRUE,FALSE,'2025-10-01 08:00:00','2026-07-10 21:00:00',NULL),
    ('0988502001','demo.tenant502@hdbhms.local',@password_hash,'TENANT','ACTIVE','2026-07-10 21:10:00',TRUE,FALSE,'2025-10-01 08:00:00','2026-07-10 21:10:00',NULL),
    ('0988503001','demo.tenant503@hdbhms.local',@password_hash,'TENANT','ACTIVE','2026-07-10 21:20:00',TRUE,FALSE,'2025-01-01 08:00:00','2026-07-10 21:20:00',NULL),
    ('0988506001','demo.tenant506@hdbhms.local',@password_hash,'TENANT','ACTIVE','2026-07-10 21:30:00',TRUE,FALSE,'2025-01-01 08:00:00','2026-07-10 21:30:00',NULL),
    ('0988507001','demo.tenant507.history@hdbhms.local',@password_hash,'TENANT','ACTIVE',NULL,TRUE,FALSE,'2024-01-01 08:00:00','2026-06-30 18:00:00',NULL);

SET @owner_id := (SELECT user_id FROM hdbhms.users WHERE email='demo.owner@hdbhms.local' AND deleted_at IS NULL LIMIT 1);
SET @manager_id := (SELECT user_id FROM hdbhms.users WHERE email='demo.manager@hdbhms.local' AND deleted_at IS NULL LIMIT 1);
SET @accountant_id := (SELECT user_id FROM hdbhms.users WHERE email='demo.accountant@hdbhms.local' AND deleted_at IS NULL LIMIT 1);
SET @guest_id := (SELECT user_id FROM hdbhms.users WHERE email='demo.guest@hdbhms.local' AND deleted_at IS NULL LIMIT 1);

-- Profiles: one intentionally incomplete co-occupant profile demonstrates onboarding gaps.
INSERT INTO hdbhms.person_profiles
    (user_id, full_name, dob, gender, phone, email, permanent_address, portrait_file_id, created_at, updated_at, deleted_at)
SELECT u.user_id, d.full_name, d.dob, d.gender, u.phone, u.email, d.address, NULL, d.created_at, d.created_at, NULL
FROM (
    SELECT 'demo.owner@hdbhms.local' email,'Nguyễn Minh Chủ' full_name,'1980-05-12' dob,'MALE' gender,'Hà Nội (dữ liệu demo)' address,'2026-01-01 08:00:00' created_at UNION ALL
    SELECT 'demo.manager@hdbhms.local','Trần Mai Quản Lý','1990-03-20','FEMALE','Hà Nội (dữ liệu demo)','2026-01-01 08:05:00' UNION ALL
    SELECT 'demo.accountant@hdbhms.local','Lê An Kế Toán','1992-09-18','FEMALE','Hà Nội (dữ liệu demo)','2026-01-01 08:10:00' UNION ALL
    SELECT 'demo.guest@hdbhms.local','Phạm Gia Khách','2003-04-15','MALE','Hà Nội (dữ liệu demo)','2026-07-01 09:00:00' UNION ALL
    SELECT 'demo.tenant404@hdbhms.local','Đỗ Hoàng Anh','2002-01-15','MALE','Hà Nội (dữ liệu demo)','2025-09-01 08:00:00' UNION ALL
    SELECT 'demo.tenant404.co1@hdbhms.local','Vũ Ngọc Mai','2002-06-21','FEMALE','Hà Nội (dữ liệu demo)','2025-09-01 08:05:00' UNION ALL
    SELECT 'demo.tenant404.co2@hdbhms.local','Bùi Đức Long','2001-11-02','MALE','Hà Nội (dữ liệu demo)','2025-09-01 08:10:00' UNION ALL
    SELECT 'demo.tenant405@hdbhms.local','Nguyễn Minh Khoa','2001-08-09','MALE','Hà Nam (dữ liệu demo)','2026-01-01 08:00:00' UNION ALL
    SELECT 'demo.tenant405.pending@hdbhms.local','Người Ở Chung Chưa Hoàn Thiện',NULL,'UNKNOWN',NULL,'2026-07-05 08:00:00' UNION ALL
    SELECT 'demo.tenant406@hdbhms.local','Trần Thu Hà','2000-12-12','FEMALE','Nam Định (dữ liệu demo)','2025-09-01 08:00:00' UNION ALL
    SELECT 'demo.tenant407@hdbhms.local','Lê Văn Hết Hạn','1999-07-07','MALE','Thái Bình (dữ liệu demo)','2025-07-01 08:00:00' UNION ALL
    SELECT 'demo.tenant501@hdbhms.local','Phạm Quốc Bảo','2002-02-14','MALE','Ninh Bình (dữ liệu demo)','2025-10-01 08:00:00' UNION ALL
    SELECT 'demo.tenant502@hdbhms.local','Hoàng Mỹ Linh','2002-10-10','FEMALE','Hải Dương (dữ liệu demo)','2025-10-01 08:00:00' UNION ALL
    SELECT 'demo.tenant503@hdbhms.local','Đặng Thành Nam','2000-05-05','MALE','Hà Nội (dữ liệu demo)','2025-01-01 08:00:00' UNION ALL
    SELECT 'demo.tenant506@hdbhms.local','Ngô Hồng Nhung','2001-03-03','FEMALE','Bắc Ninh (dữ liệu demo)','2025-01-01 08:00:00' UNION ALL
    SELECT 'demo.tenant507.history@hdbhms.local','Đinh Gia Huy','1999-09-09','MALE','Hà Nội (dữ liệu demo)','2024-01-01 08:00:00'
) d JOIN hdbhms.users u ON u.email=d.email AND u.deleted_at IS NULL;

SET @p_guest := (SELECT person_profile_id FROM hdbhms.person_profiles WHERE user_id=@guest_id LIMIT 1);
SET @p404 := (SELECT pp.person_profile_id FROM hdbhms.person_profiles pp JOIN hdbhms.users u ON u.user_id=pp.user_id WHERE u.email='demo.tenant404@hdbhms.local' LIMIT 1);
SET @p404c1 := (SELECT pp.person_profile_id FROM hdbhms.person_profiles pp JOIN hdbhms.users u ON u.user_id=pp.user_id WHERE u.email='demo.tenant404.co1@hdbhms.local' LIMIT 1);
SET @p404c2 := (SELECT pp.person_profile_id FROM hdbhms.person_profiles pp JOIN hdbhms.users u ON u.user_id=pp.user_id WHERE u.email='demo.tenant404.co2@hdbhms.local' LIMIT 1);
SET @p405 := (SELECT pp.person_profile_id FROM hdbhms.person_profiles pp JOIN hdbhms.users u ON u.user_id=pp.user_id WHERE u.email='demo.tenant405@hdbhms.local' LIMIT 1);
SET @p405pending := (SELECT pp.person_profile_id FROM hdbhms.person_profiles pp JOIN hdbhms.users u ON u.user_id=pp.user_id WHERE u.email='demo.tenant405.pending@hdbhms.local' LIMIT 1);
SET @p406 := (SELECT pp.person_profile_id FROM hdbhms.person_profiles pp JOIN hdbhms.users u ON u.user_id=pp.user_id WHERE u.email='demo.tenant406@hdbhms.local' LIMIT 1);
SET @p407 := (SELECT pp.person_profile_id FROM hdbhms.person_profiles pp JOIN hdbhms.users u ON u.user_id=pp.user_id WHERE u.email='demo.tenant407@hdbhms.local' LIMIT 1);
SET @p501 := (SELECT pp.person_profile_id FROM hdbhms.person_profiles pp JOIN hdbhms.users u ON u.user_id=pp.user_id WHERE u.email='demo.tenant501@hdbhms.local' LIMIT 1);
SET @p502 := (SELECT pp.person_profile_id FROM hdbhms.person_profiles pp JOIN hdbhms.users u ON u.user_id=pp.user_id WHERE u.email='demo.tenant502@hdbhms.local' LIMIT 1);
SET @p503 := (SELECT pp.person_profile_id FROM hdbhms.person_profiles pp JOIN hdbhms.users u ON u.user_id=pp.user_id WHERE u.email='demo.tenant503@hdbhms.local' LIMIT 1);
SET @p506 := (SELECT pp.person_profile_id FROM hdbhms.person_profiles pp JOIN hdbhms.users u ON u.user_id=pp.user_id WHERE u.email='demo.tenant506@hdbhms.local' LIMIT 1);
SET @p507 := (SELECT pp.person_profile_id FROM hdbhms.person_profiles pp JOIN hdbhms.users u ON u.user_id=pp.user_id WHERE u.email='demo.tenant507.history@hdbhms.local' LIMIT 1);

INSERT INTO hdbhms.file_metadata
    (owner_user_id,storage_key,original_name,mime_type,size_bytes,sha256_checksum,category,is_sensitive,created_at,deleted_at)
VALUES
    (@owner_id,'local/manager-contract-before.pdf','hop-dong-thue-demo-draft.pdf','application/pdf',1,NULL,'LEASE_CONTRACT_DRAFT',TRUE,'2026-01-01 08:00:00',NULL),
    (@owner_id,'local/manager-contract-after.pdf','hop-dong-thue-demo-signed.pdf','application/pdf',1,NULL,'CONTRACT',TRUE,'2026-01-01 08:00:00',NULL),
    (@owner_id,'local/deposit/Hợp đồng TNT.pdf','hop-dong-coc-demo.pdf','application/pdf',1,NULL,'DEPOSIT_CONTRACT',TRUE,'2026-01-01 08:00:00',NULL),
    (@owner_id,'local/test-id-front.png','cccd-demo-front.png','image/png',1,NULL,'ID_CARD',TRUE,'2026-01-01 08:00:00',NULL),
    (@owner_id,'local/test-id-back.png','cccd-demo-back.png','image/png',1,NULL,'ID_CARD',TRUE,'2026-01-01 08:00:00',NULL),
    (@owner_id,'local/test-portrait.png','anh-bao-tri-demo.png','image/png',1,NULL,'MAINTENANCE',FALSE,'2026-01-01 08:00:00',NULL);

SET @lease_draft_file := (SELECT file_metadata_id FROM hdbhms.file_metadata WHERE storage_key='local/manager-contract-before.pdf' AND original_name='hop-dong-thue-demo-draft.pdf' ORDER BY file_metadata_id DESC LIMIT 1);
SET @lease_signed_file := (SELECT file_metadata_id FROM hdbhms.file_metadata WHERE storage_key='local/manager-contract-after.pdf' AND original_name='hop-dong-thue-demo-signed.pdf' ORDER BY file_metadata_id DESC LIMIT 1);
SET @deposit_file := (SELECT file_metadata_id FROM hdbhms.file_metadata WHERE original_name='hop-dong-coc-demo.pdf' ORDER BY file_metadata_id DESC LIMIT 1);
SET @id_front_file := (SELECT file_metadata_id FROM hdbhms.file_metadata WHERE original_name='cccd-demo-front.png' ORDER BY file_metadata_id DESC LIMIT 1);
SET @id_back_file := (SELECT file_metadata_id FROM hdbhms.file_metadata WHERE original_name='cccd-demo-back.png' ORDER BY file_metadata_id DESC LIMIT 1);
SET @maintenance_file := (SELECT file_metadata_id FROM hdbhms.file_metadata WHERE original_name='anh-bao-tri-demo.png' ORDER BY file_metadata_id DESC LIMIT 1);

UPDATE hdbhms.person_profiles
SET portrait_file_id=@maintenance_file
WHERE user_id IN (@owner_id,@manager_id,@accountant_id,@guest_id);

UPDATE hdbhms.person_profiles
SET portrait_file_id=@maintenance_file
WHERE person_profile_id IN (@p404,@p404c1,@p404c2,@p405,@p406,@p407,@p501,@p502,@p503,@p506,@p507);

-- Complete identity records for every demo resident except the deliberately incomplete room 405 co-occupant.
INSERT INTO hdbhms.identity_documents
    (profile_id,doc_type,doc_number,issued_date,issued_place,expiry_date,raw_ocr_data,front_file_id,back_file_id,status,created_at,updated_at)
SELECT pp.person_profile_id,'CCCD',d.doc_number,'2021-01-15','Cục CSQLHC về TTXH','2036-01-15',NULL,@id_front_file,@id_back_file,'ACTIVE','2026-01-01 08:00:00','2026-01-01 08:00:00'
FROM (
    SELECT 'demo.tenant404@hdbhms.local' email,'079200000401' doc_number UNION ALL
    SELECT 'demo.tenant404.co1@hdbhms.local','079200000402' UNION ALL
    SELECT 'demo.tenant404.co2@hdbhms.local','079200000403' UNION ALL
    SELECT 'demo.tenant405@hdbhms.local','079200000405' UNION ALL
    SELECT 'demo.tenant406@hdbhms.local','079200000406' UNION ALL
    SELECT 'demo.tenant407@hdbhms.local','079200000407' UNION ALL
    SELECT 'demo.tenant501@hdbhms.local','079200000501' UNION ALL
    SELECT 'demo.tenant502@hdbhms.local','079200000502' UNION ALL
    SELECT 'demo.tenant503@hdbhms.local','079200000503' UNION ALL
    SELECT 'demo.tenant506@hdbhms.local','079200000506' UNION ALL
    SELECT 'demo.tenant507.history@hdbhms.local','079200000507'
) d JOIN hdbhms.users u ON u.email=d.email
JOIN hdbhms.person_profiles pp ON pp.user_id=u.user_id;

INSERT INTO hdbhms.tenants (user_id,property_id,created_at,updated_at,deleted_at)
SELECT user_id,@property_id,created_at,updated_at,NULL
FROM hdbhms.users
WHERE role='TENANT' AND email LIKE 'demo.%@hdbhms.local' AND deleted_at IS NULL;

INSERT INTO hdbhms.emergency_contacts (tenant_profile_id,full_name,relationship,phone,created_at)
SELECT pp.person_profile_id,CONCAT('Liên hệ ',pp.full_name),'Người thân',CONCAT('0977',RIGHT(u.phone,6)),'2026-01-01 08:00:00'
FROM hdbhms.person_profiles pp JOIN hdbhms.users u ON u.user_id=pp.user_id
WHERE u.role='TENANT' AND u.email LIKE 'demo.%@hdbhms.local' AND u.email <> 'demo.tenant405.pending@hdbhms.local';

INSERT INTO hdbhms.vehicles (profile_id,vehicle_type,license_plate,image_file_id,status,created_at,deleted_at)
VALUES
    (@p404,'MOTORBIKE','29-DM404.01',NULL,'ACTIVE','2025-09-01 08:00:00',NULL),
    (@p404,'BICYCLE','DEMO-404-BIKE',NULL,'ACTIVE','2025-09-01 08:00:00',NULL),
    (@p501,'MOTORBIKE','29-DM501.01',NULL,'ACTIVE','2025-10-01 08:00:00',NULL);

INSERT INTO hdbhms.role_promotions (user_id,role,status,property_id,approved_at,created_at,updated_at,deleted_at)
VALUES
    (@manager_id,'MANAGER','ACTIVE',@property_id,'2026-01-01 09:00:00','2026-01-01 08:00:00','2026-01-01 09:00:00',NULL),
    (@accountant_id,'ACCOUNTANT','ACTIVE',@property_id,'2026-01-01 09:00:00','2026-01-01 08:00:00','2026-01-01 09:00:00',NULL);

INSERT INTO hdbhms.property_staff_assignments
    (property_id,staff_user_id,assigned_role,assignment_status,is_primary,notes,assigned_by_user_id,started_at,ended_at,created_at,updated_at)
VALUES
    (@property_id,@manager_id,'MANAGER','ACTIVE',TRUE,'Quản lý chính - dữ liệu demo',@owner_id,'2026-01-01 09:00:00',NULL,'2026-01-01 09:00:00','2026-01-01 09:00:00'),
    (@property_id,@accountant_id,'ACCOUNTANT','ACTIVE',FALSE,'Kế toán demo chỉ xem báo cáo',@owner_id,'2026-01-01 09:00:00',NULL,'2026-01-01 09:00:00','2026-01-01 09:00:00');

INSERT INTO hdbhms.login_history (user_id,status,ip_address,user_agent,method,session_id,device_id,logged_in_at)
VALUES
    (@owner_id,'SUCCESS','127.0.0.1','Demo Web Browser','PASSWORD','DEMO-OWNER-SESSION','DEMO-WEB','2026-07-10 08:00:00'),
    (@manager_id,'SUCCESS','127.0.0.1','Demo Web Browser','PASSWORD','DEMO-MANAGER-SESSION','DEMO-WEB','2026-07-10 08:10:00'),
    (@accountant_id,'SUCCESS','127.0.0.1','Demo Web Browser','PASSWORD','DEMO-ACCOUNTANT-SESSION','DEMO-WEB','2026-07-10 08:20:00'),
    (@guest_id,'INVALID_PASSWORD','127.0.0.1','Demo Public Browser','PASSWORD',NULL,'DEMO-WEB','2026-07-10 08:30:00'),
    (@guest_id,'ACCOUNT_LOCKED','127.0.0.1','Demo Public Browser','PASSWORD',NULL,'DEMO-WEB','2026-07-10 08:35:00');

-- Collection and tariff configuration used by billing and reports.
INSERT INTO hdbhms.collection_accounts
    (property_id,account_type,bank_name,account_number,account_holder,provider,status,created_at)
VALUES
    (@property_id,'RENT','Ngân hàng Demo','DEMO-RENT-001','HDBHMS DEMO','BANK','ACTIVE','2026-01-01 08:00:00'),
    (@property_id,'UTILITY','Ngân hàng Demo','DEMO-UTILITY-001','HDBHMS DEMO','BANK','ACTIVE','2026-01-01 08:00:00'),
    (@property_id,'DEPOSIT','Ngân hàng Demo','DEMO-DEPOSIT-001','HDBHMS DEMO','BANK','ACTIVE','2026-01-01 08:00:00'),
    (@property_id,'OPERATING','Tiền mặt Demo','DEMO-OPERATING-001','HDBHMS DEMO','CASH','ACTIVE','2026-01-01 08:00:00');

SET @rent_account := (SELECT collection_account_id FROM hdbhms.collection_accounts WHERE account_number='DEMO-RENT-001' LIMIT 1);
SET @utility_account := (SELECT collection_account_id FROM hdbhms.collection_accounts WHERE account_number='DEMO-UTILITY-001' LIMIT 1);
SET @deposit_account := (SELECT collection_account_id FROM hdbhms.collection_accounts WHERE account_number='DEMO-DEPOSIT-001' LIMIT 1);
SET @operating_account := (SELECT collection_account_id FROM hdbhms.collection_accounts WHERE account_number='DEMO-OPERATING-001' LIMIT 1);

INSERT INTO hdbhms.utility_tariffs
    (property_id,utility_type,unit_price,free_allowance,service_fee_waive_electricity_threshold,effective_from,effective_to,created_by,created_at)
VALUES
    (@property_id,'ELECTRICITY',3500,0,NULL,'2026-01-01',NULL,@owner_id,'2026-01-01 08:00:00'),
    (@property_id,'WATER',20000,6,NULL,'2026-01-01',NULL,@owner_id,'2026-01-01 08:00:00'),
    (@property_id,'SERVICE_FEE',50000,0,100000,'2026-01-01',NULL,@owner_id,'2026-01-01 08:00:00');

-- Room scenario states.
UPDATE hdbhms.rooms SET current_status='RESERVED_FOR_TRANSFER',internal_note='DEMO: đơn chuyển phòng đã duyệt, chờ bước tiếp theo',updated_at='2026-07-10 08:00:00' WHERE room_id=@r401;
UPDATE hdbhms.rooms SET current_status='RESERVED',internal_note='DEMO: đặt cọc thành công, chờ ký hợp đồng thuê',updated_at='2026-07-10 08:00:00' WHERE room_id=@r402;
UPDATE hdbhms.rooms SET current_status='ON_HOLD',internal_note='DEMO: QR cọc đang chờ thanh toán; có lịch sử thất bại/hủy/hết hạn',updated_at='2026-07-10 08:00:00' WHERE room_id=@r403;
UPDATE hdbhms.rooms SET current_status='OCCUPIED',internal_note='DEMO: 3 người thuê, hồ sơ đầy đủ, điện thấp được miễn phí dịch vụ',updated_at='2026-07-10 08:00:00' WHERE room_id=@r404;
UPDATE hdbhms.rooms SET current_status='OCCUPIED',internal_note='DEMO: có người ở chung đang chờ hoàn thiện hồ sơ/tài khoản',updated_at='2026-07-10 08:00:00' WHERE room_id=@r405;
UPDATE hdbhms.rooms SET current_status='SOON_VACANT',internal_note='DEMO: hợp đồng sắp hết hạn, đã báo chuyển đi và có công nợ',updated_at='2026-07-10 08:00:00' WHERE room_id=@r406;
UPDATE hdbhms.rooms SET current_status='EXPIRED',internal_note='DEMO: hợp đồng đã hết hạn nhưng người thuê vẫn còn ở',updated_at='2026-07-10 08:00:00' WHERE room_id=@r407;
UPDATE hdbhms.rooms SET current_status='MAINTENANCE',internal_note='DEMO: bảo trì thuộc chi phí vận hành nhà trọ',updated_at='2026-07-10 08:00:00' WHERE room_id=@r408;
UPDATE hdbhms.rooms SET current_status='OCCUPIED',internal_note='DEMO: ma trận hóa đơn/thanh toán/công nợ/vi phạm',updated_at='2026-07-10 08:00:00' WHERE room_id=@r501;
UPDATE hdbhms.rooms SET current_status='OCCUPIED',internal_note='DEMO: toàn bộ vòng đời phiếu sự cố',updated_at='2026-07-10 08:00:00' WHERE room_id=@r502;
UPDATE hdbhms.rooms SET current_status='OCCUPIED',internal_note='DEMO: chuyển sang phòng đắt hơn, chờ thanh toán chênh lệch',updated_at='2026-07-10 08:00:00' WHERE room_id=@r503;
UPDATE hdbhms.rooms SET current_status='RESERVED_FOR_TRANSFER',internal_note='DEMO: phòng đích đang giữ cho yêu cầu chuyển phòng 503',updated_at='2026-07-10 08:00:00' WHERE room_id=@r504;
UPDATE hdbhms.rooms SET current_status='VACANT',internal_note='DEMO: phòng trống công khai; có lịch sử hợp đồng/chuyển phòng',updated_at='2026-07-10 08:00:00' WHERE room_id=@r505;
UPDATE hdbhms.rooms SET current_status='OCCUPIED',internal_note='DEMO: hợp đồng đã gia hạn, giữ lịch sử hợp đồng cũ',updated_at='2026-07-10 08:00:00' WHERE room_id=@r506;
UPDATE hdbhms.rooms SET current_status='VACANT',internal_note='DEMO: đã thanh lý, bàn giao hỏng/mất thiết bị và bồi thường',updated_at='2026-07-10 08:00:00' WHERE room_id=@r507;

INSERT INTO hdbhms.room_status_history (room_id,from_status,to_status,reason,changed_by,changed_at)
VALUES
    (@r401,'VACANT','RESERVED','Đơn chuyển phòng đã được duyệt (demo)',@manager_id,'2026-07-09 09:00:00'),
    (@r402,'VACANT','RESERVED','Thanh toán cọc thành công (demo)',@manager_id,'2026-07-01 10:00:00'),
    (@r403,'VACANT','RESERVED','Giữ tạm khi xử lý thanh toán cọc (demo)',NULL,'2026-07-10 08:00:00'),
    (@r404,'RESERVED','OCCUPIED','Hợp đồng thuê đã kích hoạt (demo)',@owner_id,'2025-09-01 09:00:00'),
    (@r405,'RESERVED','OCCUPIED','Hợp đồng thuê đã kích hoạt (demo)',@owner_id,'2026-01-01 09:00:00'),
    (@r406,'OCCUPIED','SOON_VACANT','Người thuê xác nhận chuyển đi (demo)',@manager_id,'2026-06-01 09:00:00'),
    (@r407,'OCCUPIED','EXPIRED','Hợp đồng hết hạn chưa xử lý (demo)',NULL,'2026-07-06 00:00:00'),
    (@r408,'VACANT','MAINTENANCE','Sửa hệ thống điện phòng (demo)',@manager_id,'2026-07-01 08:00:00'),
    (@r501,'RESERVED','OCCUPIED','Hợp đồng thuê đã kích hoạt (demo)',@owner_id,'2025-10-01 09:00:00'),
    (@r502,'RESERVED','OCCUPIED','Hợp đồng thuê đã kích hoạt (demo)',@owner_id,'2025-10-01 09:00:00'),
    (@r503,'RESERVED','OCCUPIED','Hợp đồng thuê đã kích hoạt (demo)',@owner_id,'2025-01-01 09:00:00'),
    (@r504,'VACANT','RESERVED','Giữ phòng cho chuyển phòng 503 (demo)',@manager_id,'2026-07-08 09:00:00'),
    (@r505,'OCCUPIED','VACANT','Đã chuyển người thuê sang phòng 507 (demo lịch sử)',@manager_id,'2025-12-01 09:00:00'),
    (@r506,'RESERVED','OCCUPIED','Hợp đồng gia hạn đang hiệu lực (demo)',@owner_id,'2026-01-01 09:00:00'),
    (@r507,'OCCUPIED','VACANT','Đã thanh lý và hoàn tất bàn giao (demo)',@owner_id,'2026-06-30 18:00:00');

-- Assets and meters for every room in scope.
INSERT INTO hdbhms.room_assets
    (room_id,asset_name,asset_category,quantity,current_condition,description,image_file_id,created_at,updated_at,deleted_at)
SELECT r.room_id,a.asset_name,a.asset_category,a.quantity,
       CASE WHEN r.room_code='408' AND a.asset_name='Bình nóng lạnh' THEN 'BROKEN'
            WHEN r.room_code='507' AND a.asset_name='Remote điều hòa' THEN 'MISSING'
            WHEN r.room_code='507' AND a.asset_name='Bình nóng lạnh' THEN 'ATTENTION'
            ELSE 'GOOD' END,
       CONCAT('Thiết bị demo phòng ',r.room_code),NULL,'2026-01-01 08:00:00','2026-07-01 08:00:00',NULL
FROM hdbhms.rooms r
JOIN (SELECT 'Điều hòa' asset_name,'APPLIANCE' asset_category,1 quantity UNION ALL
      SELECT 'Remote điều hòa','ACCESSORY',1 UNION ALL
      SELECT 'Bình nóng lạnh','APPLIANCE',1 UNION ALL
      SELECT 'Giường','FURNITURE',1) a
WHERE r.property_id=@property_id AND r.room_code IN ('401','402','403','404','405','406','407','408','501','502','503','504','505','506','507') AND r.deleted_at IS NULL;

INSERT INTO hdbhms.meters (room_id,meter_type,meter_code,status,installed_at,created_at)
SELECT r.room_id,m.meter_type,CONCAT('DEMO-',m.prefix,'-',r.room_code),'ACTIVE','2025-01-01','2025-01-01 08:00:00'
FROM hdbhms.rooms r
JOIN (SELECT 'ELECTRICITY' meter_type,'E' prefix UNION ALL SELECT 'WATER','W') m
WHERE r.property_id=@property_id AND r.room_code IN ('401','402','403','404','405','406','407','408','501','502','503','504','505','506','507') AND r.deleted_at IS NULL;

INSERT INTO hdbhms.meter_reading_batches
    (property_id,reading_period,status,imported_file_id,created_by,confirmed_by,confirmed_at,created_at,total_rooms,completed_rooms,anomaly_count)
VALUES
    (@property_id,'2026-04','CONFIRMED',NULL,@manager_id,@manager_id,'2026-04-30 20:00:00','2026-04-30 18:00:00',15,15,0),
    (@property_id,'2026-05','CONFIRMED',NULL,@manager_id,@manager_id,'2026-05-31 20:00:00','2026-05-31 18:00:00',15,15,0),
    (@property_id,'2026-06','CONFIRMED',NULL,@manager_id,@manager_id,'2026-06-30 20:00:00','2026-06-30 18:00:00',15,15,1);

SET @batch_apr := (SELECT meter_reading_batch_id FROM hdbhms.meter_reading_batches WHERE property_id=@property_id AND reading_period='2026-04' ORDER BY meter_reading_batch_id DESC LIMIT 1);
SET @batch_may := (SELECT meter_reading_batch_id FROM hdbhms.meter_reading_batches WHERE property_id=@property_id AND reading_period='2026-05' ORDER BY meter_reading_batch_id DESC LIMIT 1);
SET @batch_jun := (SELECT meter_reading_batch_id FROM hdbhms.meter_reading_batches WHERE property_id=@property_id AND reading_period='2026-06' ORDER BY meter_reading_batch_id DESC LIMIT 1);

INSERT INTO hdbhms.meter_readings
    (batch_id,meter_id,room_id,reading_period,revision_no,previous_value,current_value,reading_date,photo_file_id,status,void_reason,created_by,created_at,purpose,source,review_status,review_count)
SELECT @batch_apr,m.meter_id,r.room_id,'2026-04',1,
       CASE WHEN m.meter_type='ELECTRICITY' THEN CAST(r.room_code AS UNSIGNED)*10 ELSE CAST(r.room_code AS UNSIGNED) END,
       CASE WHEN m.meter_type='ELECTRICITY' THEN CAST(r.room_code AS UNSIGNED)*10+30 ELSE CAST(r.room_code AS UNSIGNED)+6 END,
       '2026-04-30',NULL,'CONFIRMED',NULL,@manager_id,'2026-04-30 20:00:00','MONTHLY','EXCEL_IMPORT','NONE',0
FROM hdbhms.rooms r JOIN hdbhms.meters m ON m.room_id=r.room_id AND m.status='ACTIVE'
WHERE r.property_id=@property_id AND r.room_code IN ('401','402','403','404','405','406','407','408','501','502','503','504','505','506','507');

INSERT INTO hdbhms.meter_readings
    (batch_id,meter_id,room_id,reading_period,revision_no,previous_value,current_value,reading_date,photo_file_id,status,void_reason,created_by,created_at,purpose,source,review_status,review_count)
SELECT @batch_may,m.meter_id,r.room_id,'2026-05',1,
       CASE WHEN m.meter_type='ELECTRICITY' THEN CAST(r.room_code AS UNSIGNED)*10+30 ELSE CAST(r.room_code AS UNSIGNED)+6 END,
       CASE WHEN m.meter_type='ELECTRICITY' THEN CAST(r.room_code AS UNSIGNED)*10+62 ELSE CAST(r.room_code AS UNSIGNED)+13 END,
       '2026-05-31',NULL,'CONFIRMED',NULL,@manager_id,'2026-05-31 20:00:00','MONTHLY','MANUAL','NONE',0
FROM hdbhms.rooms r JOIN hdbhms.meters m ON m.room_id=r.room_id AND m.status='ACTIVE'
WHERE r.property_id=@property_id AND r.room_code IN ('401','402','403','404','405','406','407','408','501','502','503','504','505','506','507');

INSERT INTO hdbhms.meter_readings
    (batch_id,meter_id,room_id,reading_period,revision_no,previous_value,current_value,reading_date,photo_file_id,status,void_reason,created_by,created_at,purpose,source,review_status,review_count)
SELECT @batch_jun,m.meter_id,r.room_id,'2026-06',1,
       CASE WHEN m.meter_type='ELECTRICITY' THEN CAST(r.room_code AS UNSIGNED)*10+62 ELSE CAST(r.room_code AS UNSIGNED)+13 END,
       CASE WHEN m.meter_type='ELECTRICITY' THEN CAST(r.room_code AS UNSIGNED)*10+62+
              CASE WHEN r.room_code='404' THEN 20 WHEN r.room_code='501' THEN 90 ELSE 35 END
            ELSE CAST(r.room_code AS UNSIGNED)+13+CASE WHEN r.room_code='404' THEN 5 ELSE 8 END END,
       '2026-06-30',CASE WHEN r.room_code IN ('404','501') THEN @maintenance_file ELSE NULL END,'CONFIRMED',NULL,@manager_id,'2026-06-30 20:00:00','MONTHLY','MANUAL','NONE',0
FROM hdbhms.rooms r JOIN hdbhms.meters m ON m.room_id=r.room_id AND m.status='ACTIVE'
WHERE r.property_id=@property_id AND r.room_code IN ('401','402','403','404','405','406','407','408','501','502','503','504','505','506','507');

SET @mr501e_jun := (SELECT mr.meter_reading_id FROM hdbhms.meter_readings mr JOIN hdbhms.meters m ON m.meter_id=mr.meter_id WHERE mr.room_id=@r501 AND mr.reading_period='2026-06' AND m.meter_type='ELECTRICITY' AND mr.status='CONFIRMED' LIMIT 1);
INSERT INTO hdbhms.meter_reading_anomalies
    (meter_reading_id,anomaly_type,message,severity,resolved_at,resolved_by,created_at,batch_id)
VALUES (@mr501e_jun,'HIGH_USAGE','Điện phòng 501 tăng trên 150% so với kỳ trước (demo)','HIGH',NULL,NULL,'2026-06-30 20:00:00',@batch_jun);

-- Tenant and deposit natural-key variables.
SET @u404 := (SELECT user_id FROM hdbhms.users WHERE email='demo.tenant404@hdbhms.local' LIMIT 1);
SET @u404c1 := (SELECT user_id FROM hdbhms.users WHERE email='demo.tenant404.co1@hdbhms.local' LIMIT 1);
SET @u404c2 := (SELECT user_id FROM hdbhms.users WHERE email='demo.tenant404.co2@hdbhms.local' LIMIT 1);
SET @u405 := (SELECT user_id FROM hdbhms.users WHERE email='demo.tenant405@hdbhms.local' LIMIT 1);
SET @u405pending := (SELECT user_id FROM hdbhms.users WHERE email='demo.tenant405.pending@hdbhms.local' LIMIT 1);
SET @u406 := (SELECT user_id FROM hdbhms.users WHERE email='demo.tenant406@hdbhms.local' LIMIT 1);
SET @u407 := (SELECT user_id FROM hdbhms.users WHERE email='demo.tenant407@hdbhms.local' LIMIT 1);
SET @u501 := (SELECT user_id FROM hdbhms.users WHERE email='demo.tenant501@hdbhms.local' LIMIT 1);
SET @u502 := (SELECT user_id FROM hdbhms.users WHERE email='demo.tenant502@hdbhms.local' LIMIT 1);
SET @u503 := (SELECT user_id FROM hdbhms.users WHERE email='demo.tenant503@hdbhms.local' LIMIT 1);
SET @u506 := (SELECT user_id FROM hdbhms.users WHERE email='demo.tenant506@hdbhms.local' LIMIT 1);
SET @u507 := (SELECT user_id FROM hdbhms.users WHERE email='demo.tenant507.history@hdbhms.local' LIMIT 1);

SET @t404 := (SELECT tenant_id FROM hdbhms.tenants WHERE user_id=@u404 AND property_id=@property_id AND deleted_at IS NULL LIMIT 1);
SET @t404c1 := (SELECT tenant_id FROM hdbhms.tenants WHERE user_id=@u404c1 AND property_id=@property_id AND deleted_at IS NULL LIMIT 1);
SET @t404c2 := (SELECT tenant_id FROM hdbhms.tenants WHERE user_id=@u404c2 AND property_id=@property_id AND deleted_at IS NULL LIMIT 1);
SET @t405 := (SELECT tenant_id FROM hdbhms.tenants WHERE user_id=@u405 AND property_id=@property_id AND deleted_at IS NULL LIMIT 1);
SET @t405pending := (SELECT tenant_id FROM hdbhms.tenants WHERE user_id=@u405pending AND property_id=@property_id AND deleted_at IS NULL LIMIT 1);
SET @t406 := (SELECT tenant_id FROM hdbhms.tenants WHERE user_id=@u406 AND property_id=@property_id AND deleted_at IS NULL LIMIT 1);
SET @t407 := (SELECT tenant_id FROM hdbhms.tenants WHERE user_id=@u407 AND property_id=@property_id AND deleted_at IS NULL LIMIT 1);
SET @t501 := (SELECT tenant_id FROM hdbhms.tenants WHERE user_id=@u501 AND property_id=@property_id AND deleted_at IS NULL LIMIT 1);
SET @t502 := (SELECT tenant_id FROM hdbhms.tenants WHERE user_id=@u502 AND property_id=@property_id AND deleted_at IS NULL LIMIT 1);
SET @t503 := (SELECT tenant_id FROM hdbhms.tenants WHERE user_id=@u503 AND property_id=@property_id AND deleted_at IS NULL LIMIT 1);
SET @t506 := (SELECT tenant_id FROM hdbhms.tenants WHERE user_id=@u506 AND property_id=@property_id AND deleted_at IS NULL LIMIT 1);
SET @t507 := (SELECT tenant_id FROM hdbhms.tenants WHERE user_id=@u507 AND property_id=@property_id AND deleted_at IS NULL LIMIT 1);

-- Deposit forms, holds and agreements cover success, pending, failure, expiry and cancellation.
INSERT INTO hdbhms.deposit_forms
    (room_id,id_number,id_issue_date,id_issue_place,id_front_file_id,id_back_file_id,portrait_file_id,full_name,dob,email,phone,permanent_address,expected_move_in_date,expected_lease_sign_date,payment_due_at,deposit_expires_at,status,confirmed_at,reject_reason,created_at,deposit_months,payment_cycle_months,occupant_count)
VALUES
    (@r402,'079200000004','2021-01-15','Cục CSQLHC về TTXH',@id_front_file,@id_back_file,@maintenance_file,'Phạm Gia Khách','2003-04-15','demo.guest@hdbhms.local','0988000004','Hà Nội (dữ liệu demo)','2026-08-01','2026-07-25','2026-07-01 10:00:00','2026-08-15','APPROVED','2026-07-01 09:30:00',NULL,'2026-07-01 09:00:00',1,1,1),
    (@r403,'079200000031','2021-01-15','Cục CSQLHC về TTXH',NULL,NULL,NULL,'Khách Chờ Thanh Toán','2003-01-01','demo.deposit.pending@hdbhms.local','0988000031','Hà Nội (dữ liệu demo)','2026-08-10','2026-08-01','2026-12-31 23:00:00','2026-12-31','APPROVED','2026-07-10 08:00:00',NULL,'2026-07-10 07:50:00',1,1,1),
    (@r403,'079200000032','2021-01-15','Cục CSQLHC về TTXH',NULL,NULL,NULL,'Khách Thanh Toán Thất Bại','2003-01-02','demo.deposit.failed@hdbhms.local','0988000032','Hà Nội (dữ liệu demo)','2026-07-01','2026-06-25','2026-06-20 10:00:00','2026-07-10','APPROVED','2026-06-20 09:00:00',NULL,'2026-06-20 08:50:00',1,1,1),
    (@r403,'079200000033','2021-01-15','Cục CSQLHC về TTXH',NULL,NULL,NULL,'Khách Đã Hủy Cọc','2003-01-03','demo.deposit.cancelled@hdbhms.local','0988000033','Hà Nội (dữ liệu demo)','2026-06-01','2026-05-25','2026-05-20 10:00:00','2026-06-10','APPROVED','2026-05-20 09:00:00',NULL,'2026-05-20 08:50:00',1,1,1),
    (@r403,'079200000034','2021-01-15','Cục CSQLHC về TTXH',NULL,NULL,NULL,'Khách Quá Hạn Mất Cọc','2003-01-04','demo.deposit.expired@hdbhms.local','0988000034','Hà Nội (dữ liệu demo)','2026-05-01','2026-04-25','2026-04-20 10:00:00','2026-05-15','APPROVED','2026-04-20 09:00:00',NULL,'2026-04-20 08:50:00',1,1,1);

SET @form402 := (SELECT deposit_form_id FROM hdbhms.deposit_forms WHERE email='demo.guest@hdbhms.local' ORDER BY deposit_form_id DESC LIMIT 1);
SET @form403pending := (SELECT deposit_form_id FROM hdbhms.deposit_forms WHERE email='demo.deposit.pending@hdbhms.local' ORDER BY deposit_form_id DESC LIMIT 1);
SET @form403failed := (SELECT deposit_form_id FROM hdbhms.deposit_forms WHERE email='demo.deposit.failed@hdbhms.local' ORDER BY deposit_form_id DESC LIMIT 1);
SET @form403cancelled := (SELECT deposit_form_id FROM hdbhms.deposit_forms WHERE email='demo.deposit.cancelled@hdbhms.local' ORDER BY deposit_form_id DESC LIMIT 1);
SET @form403expired := (SELECT deposit_form_id FROM hdbhms.deposit_forms WHERE email='demo.deposit.expired@hdbhms.local' ORDER BY deposit_form_id DESC LIMIT 1);

INSERT INTO hdbhms.room_holds (room_id,tenant_id,status,expires_at,created_at,released_at)
VALUES
    (@r402,NULL,'CONFIRMED','2026-08-15 23:59:59','2026-07-01 09:00:00',NULL),
    (@r403,NULL,'PAYMENT_PROCESSING','2026-12-31 23:00:00','2026-07-10 07:50:00',NULL),
    (@r403,NULL,'EXPIRED','2026-06-20 10:00:00','2026-06-20 08:50:00','2026-06-20 10:00:00'),
    (@r403,NULL,'CANCELLED','2026-05-20 10:00:00','2026-05-20 08:50:00','2026-05-20 09:30:00');

SET @hold402 := (SELECT room_hold_id FROM hdbhms.room_holds WHERE room_id=@r402 AND status='CONFIRMED' ORDER BY room_hold_id DESC LIMIT 1);
SET @hold403pending := (SELECT room_hold_id FROM hdbhms.room_holds WHERE room_id=@r403 AND status='PAYMENT_PROCESSING' ORDER BY room_hold_id DESC LIMIT 1);

INSERT INTO hdbhms.deposit_agreements
    (deposit_code,room_id,room_hold_id,deposit_form_id,tenant_id,lead_id,depositor_person_profile_id,amount,expected_move_in_date,expected_lease_sign_date,payment_due_at,deposit_expires_at,extension_count,max_extensions,status,confirmed_at,contract_file_id,signed_file_id,signed_at,signed_uploaded_by,note,forfeiture_reason,refunded_amount,created_at,updated_at)
VALUES
    ('DEMO-DEP-402-SUCCESS',@r402,@hold402,@form402,NULL,NULL,@p_guest,2600000,'2026-08-01','2026-07-25','2026-07-01 10:00:00','2026-08-15',0,1,'CONFIRMED','2026-07-01 10:00:00',@deposit_file,@deposit_file,'2026-07-01 10:30:00',@owner_id,'Đặt cọc thành công - chờ ký hợp đồng thuê',NULL,NULL,'2026-07-01 09:00:00','2026-07-01 10:30:00'),
    ('DEMO-DEP-403-PENDING',@r403,@hold403pending,@form403pending,NULL,NULL,NULL,2600000,'2026-08-10','2026-08-01','2026-12-31 23:00:00','2026-12-31',0,1,'PENDING_PAYMENT',NULL,@deposit_file,NULL,NULL,NULL,'Đang chờ thanh toán QR',NULL,NULL,'2026-07-10 07:50:00','2026-07-10 07:50:00'),
    ('DEMO-DEP-403-FAILED',@r403,NULL,@form403failed,NULL,NULL,NULL,2600000,'2026-07-01','2026-06-25','2026-06-20 10:00:00','2026-07-10',0,1,'CANCELLED',NULL,@deposit_file,NULL,NULL,NULL,'Thanh toán thất bại, phòng đã được nhả',NULL,NULL,'2026-06-20 08:50:00','2026-06-20 10:00:00'),
    ('DEMO-DEP-403-CANCELLED',@r403,NULL,@form403cancelled,NULL,NULL,NULL,2600000,'2026-06-01','2026-05-25','2026-05-20 10:00:00','2026-06-10',0,1,'CANCELLED',NULL,@deposit_file,NULL,NULL,NULL,'Khách chủ động hủy trước thanh toán',NULL,NULL,'2026-05-20 08:50:00','2026-05-20 09:30:00'),
    ('DEMO-DEP-403-FORFEITED',@r403,NULL,@form403expired,NULL,NULL,NULL,2600000,'2026-05-01','2026-04-25','2026-04-20 10:00:00','2026-05-15',0,1,'FORFEITED','2026-04-20 10:00:00',@deposit_file,@deposit_file,'2026-04-20 10:30:00',@owner_id,'Quá hạn ký hợp đồng','Khách không đến ký đúng hạn',0,'2026-04-20 08:50:00','2026-05-15 18:00:00');

-- Preceding deposits for lease contracts; renewed contracts inherit the old deposit through history.
INSERT INTO hdbhms.deposit_agreements
    (deposit_code,room_id,room_hold_id,deposit_form_id,tenant_id,lead_id,depositor_person_profile_id,amount,expected_move_in_date,expected_lease_sign_date,payment_due_at,deposit_expires_at,extension_count,max_extensions,status,confirmed_at,contract_file_id,signed_file_id,signed_at,signed_uploaded_by,note,forfeiture_reason,refunded_amount,created_at,updated_at)
VALUES
    ('DEMO-DEP-404',@r404,NULL,NULL,@t404,NULL,@p404,2450000,'2025-09-01','2025-08-25','2025-08-20 10:00:00','2025-09-15',0,1,'CONVERTED_TO_LEASE','2025-08-20 10:00:00',@deposit_file,@deposit_file,'2025-08-20 10:30:00',@owner_id,'Đã chuyển thành hợp đồng thuê',NULL,NULL,'2025-08-20 09:00:00','2025-09-01 09:00:00'),
    ('DEMO-DEP-405',@r405,NULL,NULL,@t405,NULL,@p405,2550000,'2026-01-01','2025-12-25','2025-12-20 10:00:00','2026-01-15',0,1,'CONVERTED_TO_LEASE','2025-12-20 10:00:00',@deposit_file,@deposit_file,'2025-12-20 10:30:00',@owner_id,'Đã chuyển thành hợp đồng thuê',NULL,NULL,'2025-12-20 09:00:00','2026-01-01 09:00:00'),
    ('DEMO-DEP-406',@r406,NULL,NULL,@t406,NULL,@p406,2600000,'2025-09-01','2025-08-25','2025-08-20 10:00:00','2025-09-15',0,1,'CONVERTED_TO_LEASE','2025-08-20 10:00:00',@deposit_file,@deposit_file,'2025-08-20 10:30:00',@owner_id,'Đã chuyển thành hợp đồng thuê',NULL,NULL,'2025-08-20 09:00:00','2025-09-01 09:00:00'),
    ('DEMO-DEP-407',@r407,NULL,NULL,@t407,NULL,@p407,2700000,'2025-07-01','2025-06-25','2025-06-20 10:00:00','2025-07-15',0,1,'CONVERTED_TO_LEASE','2025-06-20 10:00:00',@deposit_file,@deposit_file,'2025-06-20 10:30:00',@owner_id,'Đã chuyển thành hợp đồng thuê',NULL,NULL,'2025-06-20 09:00:00','2025-07-01 09:00:00'),
    ('DEMO-DEP-501',@r501,NULL,NULL,@t501,NULL,@p501,2600000,'2025-10-01','2025-09-25','2025-09-20 10:00:00','2025-10-15',0,1,'CONVERTED_TO_LEASE','2025-09-20 10:00:00',@deposit_file,@deposit_file,'2025-09-20 10:30:00',@owner_id,'Đã chuyển thành hợp đồng thuê',NULL,NULL,'2025-09-20 09:00:00','2025-10-01 09:00:00'),
    ('DEMO-DEP-502',@r502,NULL,NULL,@t502,NULL,@p502,2600000,'2025-10-01','2025-09-25','2025-09-20 10:00:00','2025-10-15',0,1,'CONVERTED_TO_LEASE','2025-09-20 10:00:00',@deposit_file,@deposit_file,'2025-09-20 10:30:00',@owner_id,'Đã chuyển thành hợp đồng thuê',NULL,NULL,'2025-09-20 09:00:00','2025-10-01 09:00:00'),
    ('DEMO-DEP-503',@r503,NULL,NULL,@t503,NULL,@p503,2400000,'2025-01-01','2024-12-25','2024-12-20 10:00:00','2025-01-15',0,1,'CONVERTED_TO_LEASE','2024-12-20 10:00:00',@deposit_file,@deposit_file,'2024-12-20 10:30:00',@owner_id,'Đã chuyển thành hợp đồng thuê',NULL,NULL,'2024-12-20 09:00:00','2025-01-01 09:00:00'),
    ('DEMO-DEP-506',@r506,NULL,NULL,@t506,NULL,@p506,2700000,'2025-01-01','2024-12-25','2024-12-20 10:00:00','2025-01-15',0,1,'CONVERTED_TO_LEASE','2024-12-20 10:00:00',@deposit_file,@deposit_file,'2024-12-20 10:30:00',@owner_id,'Đã chuyển thành hợp đồng thuê',NULL,NULL,'2024-12-20 09:00:00','2025-01-01 09:00:00'),
    ('DEMO-DEP-505-HISTORY',@r505,NULL,NULL,@t507,NULL,@p507,3000000,'2024-01-01','2023-12-25','2023-12-20 10:00:00','2024-01-15',0,1,'CONVERTED_TO_LEASE','2023-12-20 10:00:00',@deposit_file,@deposit_file,'2023-12-20 10:30:00',@owner_id,'Cọc lịch sử trước chuyển phòng',NULL,NULL,'2023-12-20 09:00:00','2024-01-01 09:00:00');

SET @dep402 := (SELECT deposit_agreement_id FROM hdbhms.deposit_agreements WHERE deposit_code='DEMO-DEP-402-SUCCESS');
SET @dep404 := (SELECT deposit_agreement_id FROM hdbhms.deposit_agreements WHERE deposit_code='DEMO-DEP-404');
SET @dep405 := (SELECT deposit_agreement_id FROM hdbhms.deposit_agreements WHERE deposit_code='DEMO-DEP-405');
SET @dep406 := (SELECT deposit_agreement_id FROM hdbhms.deposit_agreements WHERE deposit_code='DEMO-DEP-406');
SET @dep407 := (SELECT deposit_agreement_id FROM hdbhms.deposit_agreements WHERE deposit_code='DEMO-DEP-407');
SET @dep501 := (SELECT deposit_agreement_id FROM hdbhms.deposit_agreements WHERE deposit_code='DEMO-DEP-501');
SET @dep502 := (SELECT deposit_agreement_id FROM hdbhms.deposit_agreements WHERE deposit_code='DEMO-DEP-502');
SET @dep503 := (SELECT deposit_agreement_id FROM hdbhms.deposit_agreements WHERE deposit_code='DEMO-DEP-503');
SET @dep506 := (SELECT deposit_agreement_id FROM hdbhms.deposit_agreements WHERE deposit_code='DEMO-DEP-506');
SET @dep505hist := (SELECT deposit_agreement_id FROM hdbhms.deposit_agreements WHERE deposit_code='DEMO-DEP-505-HISTORY');

-- Lease lifecycle: draft, pending signature, active, expiring, expired, renewed, transferred and liquidated.
INSERT INTO hdbhms.lease_contracts
    (contract_code,room_id,deposit_agreement_id,primary_tenant_profile_id,start_date,end_date,rent_start_date,monthly_rent,payment_cycle_months,deposit_amount,status,tenant_intention,expected_vacant_date,intention_recorded_at,previous_contract_id,contract_file_id,signed_file_id,signed_uploaded_by,signed_at,created_by,created_at,updated_at,deleted_at,version)
VALUES
    ('DEMO-LEASE-505-DRAFT',@r505,NULL,@p_guest,'2026-09-01','2027-08-31','2026-09-01',2800000,1,2800000,'DRAFT',NULL,NULL,NULL,NULL,@lease_draft_file,NULL,NULL,NULL,NULL,'2026-07-01 09:00:00','2026-07-01 09:00:00',NULL,0),
    ('DEMO-LEASE-402-PENDING',@r402,@dep402,@p_guest,'2026-08-01','2027-07-31','2026-08-01',2600000,1,2600000,'PENDING_SIGNATURE',NULL,NULL,NULL,NULL,@lease_draft_file,NULL,NULL,NULL,NULL,'2026-07-02 09:00:00','2026-07-02 09:00:00',NULL,0),
    ('DEMO-LEASE-404-ACTIVE',@r404,@dep404,@p404,'2025-09-01','2027-08-31','2025-09-01',2450000,1,2450000,'ACTIVE',NULL,NULL,NULL,NULL,@lease_draft_file,@lease_signed_file,@owner_id,'2025-09-01 08:30:00',@t404,'2025-08-20 09:00:00','2025-09-01 09:00:00',NULL,0),
    ('DEMO-LEASE-405-ACTIVE',@r405,@dep405,@p405,'2026-01-01','2026-12-31','2026-01-01',2550000,1,2550000,'ACTIVE',NULL,NULL,NULL,NULL,@lease_draft_file,@lease_signed_file,@owner_id,'2026-01-01 08:30:00',@t405,'2025-12-20 09:00:00','2026-01-01 09:00:00',NULL,0),
    ('DEMO-LEASE-406-EXPIRING',@r406,@dep406,@p406,'2025-09-01','2026-08-31','2025-09-01',2600000,3,2600000,'EXPIRING_SOON','MOVE_OUT','2026-08-31','2026-06-01 09:00:00',NULL,@lease_draft_file,@lease_signed_file,@owner_id,'2025-09-01 08:30:00',@t406,'2025-08-20 09:00:00','2026-06-01 09:00:00',NULL,0),
    ('DEMO-LEASE-407-EXPIRED',@r407,@dep407,@p407,'2025-07-01','2026-07-05','2025-07-01',2700000,1,2700000,'EXPIRED',NULL,NULL,NULL,NULL,@lease_draft_file,@lease_signed_file,@owner_id,'2025-07-01 08:30:00',@t407,'2025-06-20 09:00:00','2026-07-06 00:00:00',NULL,0),
    ('DEMO-LEASE-501-ACTIVE',@r501,@dep501,@p501,'2025-10-01','2027-09-30','2025-10-01',2600000,1,2600000,'ACTIVE',NULL,NULL,NULL,NULL,@lease_draft_file,@lease_signed_file,@owner_id,'2025-10-01 08:30:00',@t501,'2025-09-20 09:00:00','2025-10-01 09:00:00',NULL,0),
    ('DEMO-LEASE-502-ACTIVE',@r502,@dep502,@p502,'2025-10-01','2027-09-30','2025-10-01',2600000,1,2600000,'ACTIVE',NULL,NULL,NULL,NULL,@lease_draft_file,@lease_signed_file,@owner_id,'2025-10-01 08:30:00',@t502,'2025-09-20 09:00:00','2025-10-01 09:00:00',NULL,0),
    ('DEMO-LEASE-503-ACTIVE',@r503,@dep503,@p503,'2025-01-01','2027-12-31','2025-01-01',2400000,3,2400000,'ACTIVE',NULL,NULL,NULL,NULL,@lease_draft_file,@lease_signed_file,@owner_id,'2025-01-01 08:30:00',@t503,'2024-12-20 09:00:00','2025-01-01 09:00:00',NULL,0),
    ('DEMO-LEASE-506-OLD',@r506,@dep506,@p506,'2025-01-01','2025-12-31','2025-01-01',2600000,1,2600000,'RENEWED','RENEW',NULL,'2025-10-01 09:00:00',NULL,@lease_draft_file,@lease_signed_file,@owner_id,'2025-01-01 08:30:00',@t506,'2024-12-20 09:00:00','2025-12-15 09:00:00',NULL,0),
    ('DEMO-LEASE-505-TRANSFERRED',@r505,@dep505hist,@p507,'2024-01-01','2025-12-31','2024-01-01',3000000,3,3000000,'TRANSFERRED','TRANSFER',NULL,'2025-11-15 09:00:00',NULL,@lease_draft_file,@lease_signed_file,@owner_id,'2024-01-01 08:30:00',@t507,'2023-12-20 09:00:00','2025-12-01 09:00:00',NULL,0);

SET @c506old := (SELECT lease_contract_id FROM hdbhms.lease_contracts WHERE contract_code='DEMO-LEASE-506-OLD');
SET @c505old := (SELECT lease_contract_id FROM hdbhms.lease_contracts WHERE contract_code='DEMO-LEASE-505-TRANSFERRED');

INSERT INTO hdbhms.lease_contracts
    (contract_code,room_id,deposit_agreement_id,primary_tenant_profile_id,start_date,end_date,rent_start_date,monthly_rent,payment_cycle_months,deposit_amount,status,tenant_intention,expected_vacant_date,intention_recorded_at,previous_contract_id,contract_file_id,signed_file_id,signed_uploaded_by,signed_at,created_by,created_at,updated_at,deleted_at,version)
VALUES
    ('DEMO-LEASE-506-RENEWED',@r506,NULL,@p506,'2026-01-01','2026-12-31','2026-01-01',2700000,1,2600000,'ACTIVE',NULL,NULL,NULL,@c506old,@lease_draft_file,@lease_signed_file,@owner_id,'2026-01-01 08:30:00',@t506,'2025-12-15 09:00:00','2026-01-01 09:00:00',NULL,0),
    ('DEMO-LEASE-507-LIQUIDATED',@r507,NULL,@p507,'2025-12-01','2026-06-30','2025-12-01',2900000,3,3000000,'LIQUIDATED','MOVE_OUT','2026-06-30','2026-05-30 09:00:00',@c505old,@lease_draft_file,@lease_signed_file,@owner_id,'2025-12-01 08:30:00',@t507,'2025-11-20 09:00:00','2026-06-30 18:00:00',NULL,0);

SET @c402 := (SELECT lease_contract_id FROM hdbhms.lease_contracts WHERE contract_code='DEMO-LEASE-402-PENDING');
SET @c404 := (SELECT lease_contract_id FROM hdbhms.lease_contracts WHERE contract_code='DEMO-LEASE-404-ACTIVE');
SET @c405 := (SELECT lease_contract_id FROM hdbhms.lease_contracts WHERE contract_code='DEMO-LEASE-405-ACTIVE');
SET @c406 := (SELECT lease_contract_id FROM hdbhms.lease_contracts WHERE contract_code='DEMO-LEASE-406-EXPIRING');
SET @c407 := (SELECT lease_contract_id FROM hdbhms.lease_contracts WHERE contract_code='DEMO-LEASE-407-EXPIRED');
SET @c501 := (SELECT lease_contract_id FROM hdbhms.lease_contracts WHERE contract_code='DEMO-LEASE-501-ACTIVE');
SET @c502 := (SELECT lease_contract_id FROM hdbhms.lease_contracts WHERE contract_code='DEMO-LEASE-502-ACTIVE');
SET @c503 := (SELECT lease_contract_id FROM hdbhms.lease_contracts WHERE contract_code='DEMO-LEASE-503-ACTIVE');
SET @c506 := (SELECT lease_contract_id FROM hdbhms.lease_contracts WHERE contract_code='DEMO-LEASE-506-RENEWED');
SET @c507 := (SELECT lease_contract_id FROM hdbhms.lease_contracts WHERE contract_code='DEMO-LEASE-507-LIQUIDATED');

INSERT INTO hdbhms.contract_occupants
    (contract_id,tenant_id,tenant_profile_id,occupant_role,move_in_date,move_out_date,status,disabled_reason,disabled_by,disabled_at,created_at)
VALUES
    (@c404,@t404,@p404,'PRIMARY','2025-09-01',NULL,'ACTIVE',NULL,NULL,NULL,'2025-09-01 09:00:00'),
    (@c404,@t404c1,@p404c1,'CO_OCCUPANT','2025-09-01',NULL,'ACTIVE',NULL,NULL,NULL,'2025-09-01 09:00:00'),
    (@c404,@t404c2,@p404c2,'CO_OCCUPANT','2025-09-01',NULL,'ACTIVE',NULL,NULL,NULL,'2025-09-01 09:00:00'),
    (@c405,@t405,@p405,'PRIMARY','2026-01-01',NULL,'ACTIVE',NULL,NULL,NULL,'2026-01-01 09:00:00'),
    (@c405,@t405pending,@p405pending,'CO_OCCUPANT','2026-07-05',NULL,'ACTIVE',NULL,NULL,NULL,'2026-07-05 09:00:00'),
    (@c406,@t406,@p406,'PRIMARY','2025-09-01',NULL,'ACTIVE',NULL,NULL,NULL,'2025-09-01 09:00:00'),
    (@c407,@t407,@p407,'PRIMARY','2025-07-01',NULL,'ACTIVE',NULL,NULL,NULL,'2025-07-01 09:00:00'),
    (@c501,@t501,@p501,'PRIMARY','2025-10-01',NULL,'ACTIVE',NULL,NULL,NULL,'2025-10-01 09:00:00'),
    (@c502,@t502,@p502,'PRIMARY','2025-10-01',NULL,'ACTIVE',NULL,NULL,NULL,'2025-10-01 09:00:00'),
    (@c503,@t503,@p503,'PRIMARY','2025-01-01',NULL,'ACTIVE',NULL,NULL,NULL,'2025-01-01 09:00:00'),
    (@c506,@t506,@p506,'PRIMARY','2026-01-01',NULL,'ACTIVE',NULL,NULL,NULL,'2026-01-01 09:00:00'),
    (@c505old,@t507,@p507,'PRIMARY','2024-01-01','2025-12-01','MOVED_OUT',NULL,NULL,NULL,'2024-01-01 09:00:00'),
    (@c507,@t507,@p507,'PRIMARY','2025-12-01','2026-06-30','MOVED_OUT',NULL,NULL,NULL,'2025-12-01 09:00:00');

INSERT INTO hdbhms.tenant_account_provisionings
    (tenant_profile_id,user_id,first_contract_id,latest_contract_id,status,recipient_email,sent_at,failed_at,failure_reason,attempt_count,last_attempt_at,created_at,updated_at)
VALUES
    (@p404,@u404,@c404,@c404,'ACTIVE','demo.tenant404@hdbhms.local','2025-09-01 09:05:00',NULL,NULL,1,'2025-09-01 09:05:00','2025-09-01 09:00:00','2025-09-01 09:05:00'),
    (@p404c1,@u404c1,@c404,@c404,'ACTIVE','demo.tenant404.co1@hdbhms.local','2025-09-01 09:05:00',NULL,NULL,1,'2025-09-01 09:05:00','2025-09-01 09:00:00','2025-09-01 09:05:00'),
    (@p404c2,@u404c2,@c404,@c404,'ACTIVE','demo.tenant404.co2@hdbhms.local','2025-09-01 09:05:00',NULL,NULL,1,'2025-09-01 09:05:00','2025-09-01 09:00:00','2025-09-01 09:05:00'),
    (@p405,@u405,@c405,@c405,'ACTIVE','demo.tenant405@hdbhms.local','2026-01-01 09:05:00',NULL,NULL,1,'2026-01-01 09:05:00','2026-01-01 09:00:00','2026-01-01 09:05:00'),
    (@p405pending,@u405pending,@c405,@c405,'PENDING','demo.tenant405.pending@hdbhms.local',NULL,NULL,NULL,0,NULL,'2026-07-05 09:00:00','2026-07-05 09:00:00'),
    (@p406,@u406,@c406,@c406,'ACTIVE','demo.tenant406@hdbhms.local','2025-09-01 09:05:00',NULL,NULL,1,'2025-09-01 09:05:00','2025-09-01 09:00:00','2025-09-01 09:05:00'),
    (@p407,@u407,@c407,@c407,'ACTIVE','demo.tenant407@hdbhms.local','2025-07-01 09:05:00',NULL,NULL,1,'2025-07-01 09:05:00','2025-07-01 09:00:00','2025-07-01 09:05:00'),
    (@p501,@u501,@c501,@c501,'ACTIVE','demo.tenant501@hdbhms.local','2025-10-01 09:05:00',NULL,NULL,1,'2025-10-01 09:05:00','2025-10-01 09:00:00','2025-10-01 09:05:00'),
    (@p502,@u502,@c502,@c502,'ACTIVE','demo.tenant502@hdbhms.local','2025-10-01 09:05:00',NULL,NULL,1,'2025-10-01 09:05:00','2025-10-01 09:00:00','2025-10-01 09:05:00'),
    (@p503,@u503,@c503,@c503,'ACTIVE','demo.tenant503@hdbhms.local','2025-01-01 09:05:00',NULL,NULL,1,'2025-01-01 09:05:00','2025-01-01 09:00:00','2025-01-01 09:05:00'),
    (@p506,@u506,@c506old,@c506,'ACTIVE','demo.tenant506@hdbhms.local','2025-01-01 09:05:00',NULL,NULL,1,'2025-01-01 09:05:00','2025-01-01 09:00:00','2026-01-01 09:05:00'),
    (@p507,@u507,@c505old,@c507,'ACTIVE','demo.tenant507.history@hdbhms.local','2024-01-01 09:05:00',NULL,NULL,1,'2024-01-01 09:05:00','2024-01-01 09:00:00','2026-06-30 18:00:00');

INSERT INTO hdbhms.contract_events (contract_id,event_type,event_data,created_by,created_at)
VALUES
    (@c404,'SIGNED',CAST('{"scenario":"active-multi-occupant"}' AS BINARY),@owner_id,'2025-09-01 08:30:00'),
    (@c406,'INTENTION_RECORDED',CAST('{"intention":"MOVE_OUT","expectedVacantDate":"2026-08-31"}' AS BINARY),@manager_id,'2026-06-01 09:00:00'),
    (@c407,'EXPIRED',CAST('{"reason":"contract-end-date-passed"}' AS BINARY),NULL,'2026-07-06 00:00:00'),
    (@c506old,'RENEWED',CAST('{"newContractCode":"DEMO-LEASE-506-RENEWED"}' AS BINARY),@owner_id,'2025-12-15 09:00:00'),
    (@c506,'SIGNED',CAST('{"scenario":"renewal"}' AS BINARY),@owner_id,'2026-01-01 08:30:00'),
    (@c505old,'TRANSFERRED',CAST('{"targetRoom":"507"}' AS BINARY),@manager_id,'2025-12-01 09:00:00'),
    (@c507,'LIQUIDATED',CAST('{"moveOutDate":"2026-06-30"}' AS BINARY),@owner_id,'2026-06-30 18:00:00');

INSERT INTO hdbhms.contract_termination_notices
    (contract_id,notice_by,notice_user_id,notice_date,expected_termination_date,reason,evidence_file_id,status,decided_by,decided_at,created_at)
VALUES (@c406,'TENANT',@u406,'2026-06-01','2026-08-31','Kết thúc hợp đồng khi hết hạn để chuyển đi',NULL,'SUBMITTED',NULL,NULL,'2026-06-01 09:00:00');

INSERT INTO hdbhms.contract_handover_records
    (contract_id,room_id,handover_type,handover_date,electricity_reading_id,water_reading_id,note,status,confirmed_by,confirmed_at,created_at,signed_document_id)
VALUES
    (@c404,@r404,'MOVE_IN','2025-09-01 09:00:00',NULL,NULL,'Bàn giao đầu vào đầy đủ','CONFIRMED',@owner_id,'2025-09-01 09:30:00','2025-09-01 09:00:00',@lease_signed_file),
    (@c507,@r507,'MOVE_OUT','2026-06-30 16:00:00',NULL,NULL,'Bàn giao ra: thiếu remote, bình nóng lạnh cần sửa','CONFIRMED',@owner_id,'2026-06-30 17:00:00','2026-06-30 16:00:00',@lease_signed_file);

SET @handover404 := (SELECT contract_handover_record_id FROM hdbhms.contract_handover_records WHERE contract_id=@c404 AND handover_type='MOVE_IN' ORDER BY contract_handover_record_id DESC LIMIT 1);
SET @handover507 := (SELECT contract_handover_record_id FROM hdbhms.contract_handover_records WHERE contract_id=@c507 AND handover_type='MOVE_OUT' ORDER BY contract_handover_record_id DESC LIMIT 1);

INSERT INTO hdbhms.contract_handover_items
    (handover_record_id,room_asset_id,asset_name,quantity,condition_status,note,evidence_file_id,compensation_amount,compensation_invoice_id,created_at)
VALUES
    (@handover404,NULL,'Điều hòa',1,'GOOD','Hoạt động bình thường',NULL,NULL,NULL,'2025-09-01 09:00:00'),
    (@handover404,NULL,'Bình nóng lạnh',1,'GOOD','Hoạt động bình thường',NULL,NULL,NULL,'2025-09-01 09:00:00'),
    (@handover507,NULL,'Remote điều hòa',1,'MISSING','Thiếu khi bàn giao ra',@maintenance_file,300000,NULL,'2026-06-30 16:00:00'),
    (@handover507,NULL,'Bình nóng lạnh',1,'BROKEN','Người thuê chịu một phần chi phí sửa chữa',@maintenance_file,450000,NULL,'2026-06-30 16:00:00');

-- Billing matrix. Every invoice total is reconciled to its generated invoice-line amounts.
SET @mr404e_jun := (SELECT mr.meter_reading_id FROM hdbhms.meter_readings mr JOIN hdbhms.meters m ON m.meter_id=mr.meter_id WHERE mr.room_id=@r404 AND mr.reading_period='2026-06' AND m.meter_type='ELECTRICITY' AND mr.status='CONFIRMED' LIMIT 1);
SET @mr404w_jun := (SELECT mr.meter_reading_id FROM hdbhms.meter_readings mr JOIN hdbhms.meters m ON m.meter_id=mr.meter_id WHERE mr.room_id=@r404 AND mr.reading_period='2026-06' AND m.meter_type='WATER' AND mr.status='CONFIRMED' LIMIT 1);
SET @mr501w_jun := (SELECT mr.meter_reading_id FROM hdbhms.meter_readings mr JOIN hdbhms.meters m ON m.meter_id=mr.meter_id WHERE mr.room_id=@r501 AND mr.reading_period='2026-06' AND m.meter_type='WATER' AND mr.status='CONFIRMED' LIMIT 1);
SET @mr501e_apr := (SELECT mr.meter_reading_id FROM hdbhms.meter_readings mr JOIN hdbhms.meters m ON m.meter_id=mr.meter_id WHERE mr.room_id=@r501 AND mr.reading_period='2026-04' AND m.meter_type='ELECTRICITY' AND mr.status='CONFIRMED' LIMIT 1);
SET @mr501w_apr := (SELECT mr.meter_reading_id FROM hdbhms.meter_readings mr JOIN hdbhms.meters m ON m.meter_id=mr.meter_id WHERE mr.room_id=@r501 AND mr.reading_period='2026-04' AND m.meter_type='WATER' AND mr.status='CONFIRMED' LIMIT 1);

INSERT INTO hdbhms.invoices
    (invoice_code,property_id,room_id,lease_contract_id,deposit_agreement_id,deposit_batch_id,invoice_type,revision_no,billing_period,issue_date,due_date,status,subtotal_amount,discount_amount,total_amount,paid_amount,remaining_amount,collection_account_id,created_by,issued_at,voided_at,void_reason,created_at,updated_at,version)
VALUES
    ('DEMO-INV-402-DEPOSIT',@property_id,@r402,NULL,@dep402,NULL,'DEPOSIT',1,'2026-07','2026-07-01 09:00:00','2026-07-01 10:00:00','PAID',2600000,0,2600000,2600000,0,@deposit_account,@owner_id,'2026-07-01 09:00:00',NULL,NULL,'2026-07-01 09:00:00','2026-07-01 10:00:00',0),
    ('DEMO-INV-404-2026-06-RENT',@property_id,@r404,@c404,NULL,NULL,'RENT',1,'2026-06','2026-06-01 08:00:00','2026-06-15 23:59:59','PAID',2450000,0,2450000,2450000,0,@rent_account,@manager_id,'2026-06-01 08:00:00',NULL,NULL,'2026-06-01 08:00:00','2026-06-02 09:00:00',0),
    ('DEMO-INV-404-2026-06-UTILITY',@property_id,@r404,@c404,NULL,NULL,'UTILITY',1,'2026-06','2026-07-01 08:00:00','2026-07-05 23:59:59','PAID',70000,0,70000,70000,0,@utility_account,@manager_id,'2026-07-01 08:00:00',NULL,NULL,'2026-07-01 08:00:00','2026-07-02 09:00:00',0),
    ('DEMO-INV-501-2026-07-DRAFT',@property_id,@r501,@c501,NULL,NULL,'RENT',1,'2026-07','2026-07-01 08:00:00','2026-07-15 23:59:59','DRAFT',2500000,0,2500000,0,2500000,@rent_account,@manager_id,NULL,NULL,NULL,'2026-07-01 08:00:00','2026-07-01 08:00:00',0),
    ('DEMO-INV-501-2026-06-ISSUED',@property_id,@r501,@c501,NULL,NULL,'RENT',1,'2026-06','2026-06-01 08:00:00','2026-06-15 23:59:59','ISSUED',2600000,0,2600000,0,2600000,@rent_account,@manager_id,'2026-06-01 08:00:00',NULL,NULL,'2026-06-01 08:00:00','2026-06-01 08:00:00',0),
    ('DEMO-INV-501-2026-06-PARTIAL',@property_id,@r501,@c501,NULL,NULL,'UTILITY',1,'2026-06','2026-07-01 08:00:00','2026-07-05 23:59:59','PARTIALLY_PAID',500000,0,500000,200000,300000,@utility_account,@manager_id,'2026-07-01 08:00:00',NULL,NULL,'2026-07-01 08:00:00','2026-07-03 09:00:00',0),
    ('DEMO-INV-501-2026-05-PAID',@property_id,@r501,@c501,NULL,NULL,'RENT',1,'2026-05','2026-05-01 08:00:00','2026-05-15 23:59:59','PAID',2600000,0,2600000,2600000,0,@rent_account,@manager_id,'2026-05-01 08:00:00',NULL,NULL,'2026-05-01 08:00:00','2026-05-03 09:00:00',0),
    ('DEMO-INV-501-2026-04-OVERDUE',@property_id,@r501,@c501,NULL,NULL,'UTILITY',1,'2026-04','2026-05-01 08:00:00','2026-05-05 23:59:59','OVERDUE',1000000,0,1000000,0,1000000,@utility_account,@manager_id,'2026-05-01 08:00:00',NULL,NULL,'2026-05-01 08:00:00','2026-05-06 00:00:00',0),
    ('DEMO-INV-501-2026-03-VOIDED',@property_id,@r501,@c501,NULL,NULL,'RENT',1,'2026-03','2026-03-01 08:00:00','2026-03-15 23:59:59','VOIDED',2600000,0,2600000,0,2600000,@rent_account,@manager_id,'2026-03-01 08:00:00','2026-03-02 09:00:00','Hủy do tạo nhầm kỳ demo','2026-03-01 08:00:00','2026-03-02 09:00:00',0),
    ('DEMO-INV-501-WIFI-FINE',@property_id,@r501,@c501,NULL,NULL,'OTHER',1,'2026-06','2026-06-20 08:00:00','2026-06-25 23:59:59','ISSUED',200000,0,200000,0,200000,@rent_account,@manager_id,'2026-06-20 08:00:00',NULL,NULL,'2026-06-20 08:00:00','2026-06-20 08:00:00',0),
    ('DEMO-INV-502-COMPENSATION',@property_id,@r502,@c502,NULL,NULL,'COMPENSATION',1,'2026-06','2026-06-25 08:00:00','2026-06-30 23:59:59','PAID',450000,0,450000,450000,0,@rent_account,@manager_id,'2026-06-25 08:00:00',NULL,NULL,'2026-06-25 08:00:00','2026-06-26 09:00:00',0),
    ('DEMO-INV-503-TRANSFER-DIFF',@property_id,@r503,@c503,NULL,NULL,'TRANSFER_DIFFERENCE',1,'2026-07','2026-07-08 09:00:00','2026-07-15 23:59:59','ISSUED',600000,0,600000,0,600000,@rent_account,@manager_id,'2026-07-08 09:00:00',NULL,NULL,'2026-07-08 09:00:00','2026-07-08 09:00:00',0),
    ('DEMO-INV-507-FINAL',@property_id,@r507,@c507,NULL,NULL,'FINAL_SETTLEMENT',1,'2026-06','2026-06-30 16:30:00','2026-06-30 23:59:59','PAID',750000,0,750000,750000,0,@rent_account,@owner_id,'2026-06-30 16:30:00',NULL,NULL,'2026-06-30 16:30:00','2026-06-30 17:30:00',0);

SET @inv402dep := (SELECT invoice_id FROM hdbhms.invoices WHERE invoice_code='DEMO-INV-402-DEPOSIT');
SET @inv404rent := (SELECT invoice_id FROM hdbhms.invoices WHERE invoice_code='DEMO-INV-404-2026-06-RENT');
SET @inv404utility := (SELECT invoice_id FROM hdbhms.invoices WHERE invoice_code='DEMO-INV-404-2026-06-UTILITY');
SET @inv501draft := (SELECT invoice_id FROM hdbhms.invoices WHERE invoice_code='DEMO-INV-501-2026-07-DRAFT');
SET @inv501issued := (SELECT invoice_id FROM hdbhms.invoices WHERE invoice_code='DEMO-INV-501-2026-06-ISSUED');
SET @inv501partial := (SELECT invoice_id FROM hdbhms.invoices WHERE invoice_code='DEMO-INV-501-2026-06-PARTIAL');
SET @inv501paid := (SELECT invoice_id FROM hdbhms.invoices WHERE invoice_code='DEMO-INV-501-2026-05-PAID');
SET @inv501overdue := (SELECT invoice_id FROM hdbhms.invoices WHERE invoice_code='DEMO-INV-501-2026-04-OVERDUE');
SET @inv501voided := (SELECT invoice_id FROM hdbhms.invoices WHERE invoice_code='DEMO-INV-501-2026-03-VOIDED');
SET @inv501fine := (SELECT invoice_id FROM hdbhms.invoices WHERE invoice_code='DEMO-INV-501-WIFI-FINE');
SET @inv502comp := (SELECT invoice_id FROM hdbhms.invoices WHERE invoice_code='DEMO-INV-502-COMPENSATION');
SET @inv503diff := (SELECT invoice_id FROM hdbhms.invoices WHERE invoice_code='DEMO-INV-503-TRANSFER-DIFF');
SET @inv507final := (SELECT invoice_id FROM hdbhms.invoices WHERE invoice_code='DEMO-INV-507-FINAL');

INSERT INTO hdbhms.invoice_lines
    (invoice_id,line_type,description,quantity,unit_price,meter_reading_id,source_type,source_id,collection_account_id,created_at)
VALUES
    (@inv402dep,'OTHER','Tiền đặt cọc phòng 402',1,2600000,NULL,'DEPOSIT_AGREEMENT',@dep402,@deposit_account,'2026-07-01 09:00:00'),
    (@inv404rent,'ROOM_RENT','Tiền phòng 404 tháng 06/2026',1,2450000,NULL,'LEASE_CONTRACT',@c404,@rent_account,'2026-06-01 08:00:00'),
    (@inv404utility,'ELECTRICITY','Điện 20 kWh - dưới ngưỡng miễn phí dịch vụ',20,3500,@mr404e_jun,'METER_READING',@mr404e_jun,@utility_account,'2026-07-01 08:00:00'),
    (@inv404utility,'WATER','Nước trong định mức miễn 6 m3',0,20000,@mr404w_jun,'METER_READING',@mr404w_jun,@utility_account,'2026-07-01 08:00:00'),
    (@inv404utility,'SERVICE_FEE','Phí dịch vụ được miễn vì tiền điện < 100.000đ',0,50000,@mr404e_jun,'UTILITY_POLICY',NULL,@utility_account,'2026-07-01 08:00:00'),
    (@inv501draft,'ROOM_RENT','Tiền phòng tháng 07/2026 theo giá điều chỉnh',1,2500000,NULL,'RENT_OVERRIDE',NULL,@rent_account,'2026-07-01 08:00:00'),
    (@inv501issued,'ROOM_RENT','Tiền phòng tháng 06/2026 chưa thanh toán',1,2600000,NULL,'LEASE_CONTRACT',@c501,@rent_account,'2026-06-01 08:00:00'),
    (@inv501partial,'ELECTRICITY','Điện bất thường 90 kWh',90,3500,@mr501e_jun,'METER_READING',@mr501e_jun,@utility_account,'2026-07-01 08:00:00'),
    (@inv501partial,'WATER','Nước tính phí sau định mức 6 m3',2,20000,@mr501w_jun,'METER_READING',@mr501w_jun,@utility_account,'2026-07-01 08:00:00'),
    (@inv501partial,'SERVICE_FEE','Phí dịch vụ tháng 06/2026',1,50000,@mr501e_jun,'UTILITY_POLICY',NULL,@utility_account,'2026-07-01 08:00:00'),
    (@inv501partial,'MANUAL_ADJUSTMENT','Phát sinh điều chỉnh minh họa',1,95000,NULL,'MANUAL',NULL,@utility_account,'2026-07-01 08:00:00'),
    (@inv501paid,'ROOM_RENT','Tiền phòng tháng 05/2026',1,2600000,NULL,'LEASE_CONTRACT',@c501,@rent_account,'2026-05-01 08:00:00'),
    (@inv501overdue,'ELECTRICITY','Điện kỳ 04/2026',30,3500,@mr501e_apr,'METER_READING',@mr501e_apr,@utility_account,'2026-05-01 08:00:00'),
    (@inv501overdue,'WATER','Nước tính phí kỳ 04/2026',1,20000,@mr501w_apr,'METER_READING',@mr501w_apr,@utility_account,'2026-05-01 08:00:00'),
    (@inv501overdue,'SERVICE_FEE','Phí dịch vụ kỳ 04/2026',1,50000,@mr501e_apr,'UTILITY_POLICY',NULL,@utility_account,'2026-05-01 08:00:00'),
    (@inv501overdue,'MANUAL_ADJUSTMENT','Khoản phát sinh quá hạn demo',1,825000,NULL,'MANUAL',NULL,@utility_account,'2026-05-01 08:00:00'),
    (@inv501voided,'ROOM_RENT','Hóa đơn tiền phòng tạo nhầm - đã hủy',1,2600000,NULL,'LEASE_CONTRACT',@c501,@rent_account,'2026-03-01 08:00:00'),
    (@inv501fine,'VIOLATION_FINE','Phạt tự ý reset modem Wi-Fi',1,200000,NULL,'RULE_VIOLATION',NULL,@rent_account,'2026-06-20 08:00:00'),
    (@inv502comp,'MAINTENANCE_COMPENSATION','Bồi thường sửa thiết bị do người thuê chịu',1,450000,NULL,'MAINTENANCE_TICKET',NULL,@rent_account,'2026-06-25 08:00:00'),
    (@inv503diff,'TRANSFER_DIFFERENCE','Chênh lệch chuyển từ phòng 503 sang 504 (2 tháng còn lại)',1,600000,NULL,'ROOM_TRANSFER',NULL,@rent_account,'2026-07-08 09:00:00'),
    (@inv507final,'DEPOSIT_DEDUCTION','Khấu trừ cọc do thiếu remote điều hòa',1,300000,NULL,'HANDOVER',@handover507,@rent_account,'2026-06-30 16:30:00'),
    (@inv507final,'MAINTENANCE_COMPENSATION','Bồi thường sửa bình nóng lạnh',1,450000,NULL,'HANDOVER',@handover507,@rent_account,'2026-06-30 16:30:00');

INSERT INTO hdbhms.rent_overrides (contract_id,billing_period,override_monthly_rent,reason,approved_by,created_at)
VALUES (@c501,'2026-07',2500000,'Điều chỉnh giá tháng thực tập/tạm vắng (demo)',@owner_id,'2026-06-25 09:00:00');

-- Payment intents cover every supported business outcome.
INSERT INTO hdbhms.payment_intents
    (invoice_id,deposit_agreement_id,deposit_batch_id,invoice_payment_group_id,amount,provider,collection_account_id,payment_content,provider_order_code,qr_payload,status,expires_at,created_at)
VALUES
    (@inv402dep,@dep402,NULL,NULL,2600000,'PAYOS',@deposit_account,'DEMO DEP 402','DEMO-ORDER-DEP-402','DEMO-QR-DEP-402','SUCCEEDED','2026-07-01 10:00:00','2026-07-01 09:00:00'),
    (NULL,(SELECT deposit_agreement_id FROM hdbhms.deposit_agreements WHERE deposit_code='DEMO-DEP-403-PENDING'),NULL,NULL,2600000,'PAYOS',@deposit_account,'DEMO DEP 403 PENDING','DEMO-ORDER-DEP-403-PENDING','DEMO-QR-DEP-403-PENDING','PENDING','2026-12-31 23:00:00','2026-07-10 07:50:00'),
    (NULL,(SELECT deposit_agreement_id FROM hdbhms.deposit_agreements WHERE deposit_code='DEMO-DEP-403-FAILED'),NULL,NULL,2600000,'PAYOS',@deposit_account,'DEMO DEP 403 FAILED','DEMO-ORDER-DEP-403-FAILED',NULL,'FAILED','2026-06-20 10:00:00','2026-06-20 08:50:00'),
    (NULL,(SELECT deposit_agreement_id FROM hdbhms.deposit_agreements WHERE deposit_code='DEMO-DEP-403-CANCELLED'),NULL,NULL,2600000,'PAYOS',@deposit_account,'DEMO DEP 403 CANCELLED','DEMO-ORDER-DEP-403-CANCELLED',NULL,'CANCELLED','2026-05-20 10:00:00','2026-05-20 08:50:00'),
    (NULL,(SELECT deposit_agreement_id FROM hdbhms.deposit_agreements WHERE deposit_code='DEMO-DEP-403-FORFEITED'),NULL,NULL,2600000,'PAYOS',@deposit_account,'DEMO DEP 403 EXPIRED','DEMO-ORDER-DEP-403-EXPIRED',NULL,'EXPIRED','2026-04-20 10:00:00','2026-04-20 08:50:00'),
    (@inv501issued,NULL,NULL,NULL,2600000,'PAYOS',@rent_account,'DEMO INV 501 ISSUED','DEMO-ORDER-501-ISSUED','DEMO-QR-501-ISSUED','PENDING','2026-12-31 23:59:59','2026-06-01 08:00:00'),
    (@inv501partial,NULL,NULL,NULL,200000,'PAYOS',@utility_account,'DEMO INV 501 PARTIAL','DEMO-ORDER-501-PARTIAL','DEMO-QR-501-PARTIAL','SUCCEEDED','2026-07-03 09:00:00','2026-07-03 08:00:00'),
    (@inv501overdue,NULL,NULL,NULL,1000000,'PAYOS',@utility_account,'DEMO INV 501 FAILED','DEMO-ORDER-501-FAILED',NULL,'FAILED','2026-06-01 09:00:00','2026-06-01 08:00:00'),
    (@inv503diff,NULL,NULL,NULL,600000,'PAYOS',@rent_account,'DEMO TRANSFER 503 504','DEMO-ORDER-TRANSFER-503-504','DEMO-QR-TRANSFER-503-504','PENDING','2026-12-31 23:59:59','2026-07-08 09:00:00');

INSERT INTO hdbhms.payment_transactions
    (provider,provider_transaction_id,collection_account_id,amount,transaction_time,payer_name,payer_account,content,status,raw_payload,confirmed_by,confirmed_at,created_at)
VALUES
    ('PAYOS','DEMO-TXN-DEP-402',@deposit_account,2600000,'2026-07-01 09:55:00','Phạm Gia Khách','DEMO-PAYER-402','DEMO DEP 402','ALLOCATED',CAST('{"code":"00","callback":"success"}' AS BINARY),@manager_id,'2026-07-01 10:00:00','2026-07-01 09:55:00'),
    ('BANK','DEMO-TXN-404-PAID',@rent_account,2520000,'2026-07-02 09:00:00','Đỗ Hoàng Anh','DEMO-PAYER-404','DEMO 404 RENT UTILITY','ALLOCATED',NULL,@manager_id,'2026-07-02 09:05:00','2026-07-02 09:00:00'),
    ('BANK','DEMO-TXN-501-PAID',@rent_account,2600000,'2026-05-03 09:00:00','Phạm Quốc Bảo','DEMO-PAYER-501','DEMO INV 501 MAY','ALLOCATED',NULL,@manager_id,'2026-05-03 09:05:00','2026-05-03 09:00:00'),
    ('PAYOS','DEMO-TXN-501-PARTIAL',@utility_account,200000,'2026-07-03 09:00:00','Phạm Quốc Bảo','DEMO-PAYER-501','DEMO INV 501 PARTIAL','PARTIALLY_ALLOCATED',CAST('{"code":"00","callback":"partial"}' AS BINARY),@manager_id,'2026-07-03 09:05:00','2026-07-03 09:00:00'),
    ('PAYOS','DEMO-TXN-501-REJECTED',@utility_account,1000000,'2026-06-01 09:00:00','Phạm Quốc Bảo','DEMO-PAYER-501','DEMO INV 501 FAILED','REJECTED',CAST('{"code":"99","callback":"failed"}' AS BINARY),NULL,NULL,'2026-06-01 09:00:00'),
    ('BANK','DEMO-TXN-501-PENDING',@rent_account,2600000,'2026-07-10 09:00:00','Người chuyển chưa rõ','DEMO-PAYER-UNKNOWN','NO MATCH CONTENT','PENDING_RECONCILE',NULL,NULL,NULL,'2026-07-10 09:00:00'),
    ('PAYOS','DEMO-TXN-501-DUPLICATE-CALLBACK',@rent_account,2600000,'2026-05-03 09:00:05','Phạm Quốc Bảo','DEMO-PAYER-501','Duplicate callback of DEMO-TXN-501-PAID','DUPLICATE',CAST('{"duplicateOf":"DEMO-TXN-501-PAID"}' AS BINARY),@manager_id,'2026-05-03 09:05:05','2026-05-03 09:00:05'),
    ('CASH','DEMO-TXN-502-COMP',@rent_account,450000,'2026-06-26 09:00:00','Hoàng Mỹ Linh',NULL,'DEMO INV 502 COMP','ALLOCATED',NULL,@manager_id,'2026-06-26 09:05:00','2026-06-26 09:00:00'),
    ('CASH','DEMO-TXN-507-FINAL',@rent_account,750000,'2026-06-30 17:30:00','Đinh Gia Huy',NULL,'DEMO INV 507 FINAL','ALLOCATED',NULL,@owner_id,'2026-06-30 17:35:00','2026-06-30 17:30:00');

SET @tx402 := (SELECT payment_transaction_id FROM hdbhms.payment_transactions WHERE provider='PAYOS' AND provider_transaction_id='DEMO-TXN-DEP-402');
SET @tx404 := (SELECT payment_transaction_id FROM hdbhms.payment_transactions WHERE provider='BANK' AND provider_transaction_id='DEMO-TXN-404-PAID');
SET @tx501paid := (SELECT payment_transaction_id FROM hdbhms.payment_transactions WHERE provider='BANK' AND provider_transaction_id='DEMO-TXN-501-PAID');
SET @tx501partial := (SELECT payment_transaction_id FROM hdbhms.payment_transactions WHERE provider='PAYOS' AND provider_transaction_id='DEMO-TXN-501-PARTIAL');
SET @tx502 := (SELECT payment_transaction_id FROM hdbhms.payment_transactions WHERE provider='CASH' AND provider_transaction_id='DEMO-TXN-502-COMP');
SET @tx507 := (SELECT payment_transaction_id FROM hdbhms.payment_transactions WHERE provider='CASH' AND provider_transaction_id='DEMO-TXN-507-FINAL');

INSERT INTO hdbhms.payment_allocations (payment_transaction_id,invoice_id,amount,allocated_by,allocated_at)
VALUES
    (@tx402,@inv402dep,2600000,@manager_id,'2026-07-01 10:00:00'),
    (@tx404,@inv404rent,2450000,@manager_id,'2026-07-02 09:05:00'),
    (@tx404,@inv404utility,70000,@manager_id,'2026-07-02 09:05:00'),
    (@tx501paid,@inv501paid,2600000,@manager_id,'2026-05-03 09:05:00'),
    (@tx501partial,@inv501partial,200000,@manager_id,'2026-07-03 09:05:00'),
    (@tx502,@inv502comp,450000,@manager_id,'2026-06-26 09:05:00'),
    (@tx507,@inv507final,750000,@owner_id,'2026-06-30 17:35:00');

INSERT INTO hdbhms.invoice_payment_groups
    (invoice_id,collection_account_id,group_type,amount,payment_intent_id,status,created_at)
VALUES
    (@inv501issued,@rent_account,'RENT',2600000,(SELECT payment_intent_id FROM hdbhms.payment_intents WHERE provider_order_code='DEMO-ORDER-501-ISSUED'),'PENDING','2026-06-01 08:00:00'),
    (@inv501partial,@utility_account,'UTILITY',500000,(SELECT payment_intent_id FROM hdbhms.payment_intents WHERE provider_order_code='DEMO-ORDER-501-PARTIAL'),'PARTIALLY_PAID','2026-07-01 08:00:00'),
    (@inv503diff,@rent_account,'OTHER',600000,(SELECT payment_intent_id FROM hdbhms.payment_intents WHERE provider_order_code='DEMO-ORDER-TRANSFER-503-504'),'PENDING','2026-07-08 09:00:00');

SET @wifi_rule := (SELECT property_rule_id FROM hdbhms.property_rules WHERE property_id=@property_id AND rule_code='WIFI_RESET' LIMIT 1);
INSERT INTO hdbhms.rule_violations
    (property_id,room_id,contract_id,tenant_profile_id,rule_id,violation_date,description,fine_amount,invoice_id,evidence_file_id,status,created_by,created_at)
VALUES
    (@property_id,@r501,@c501,@p501,@wifi_rule,'2026-06-20','Tự ý reset modem Wi-Fi - dữ liệu demo',200000,@inv501fine,NULL,'INVOICED',@manager_id,'2026-06-20 08:00:00'),
    (@property_id,@r502,@c502,@p502,(SELECT property_rule_id FROM hdbhms.property_rules WHERE property_id=@property_id AND rule_code='FINE_UNAUTHORIZED_REPAIR' LIMIT 1),'2026-05-10','Tự ý sửa thiết bị nhưng được nhắc nhở, miễn phạt',0,NULL,NULL,'WAIVED',@manager_id,'2026-05-10 08:00:00');

UPDATE hdbhms.contract_handover_items
SET compensation_invoice_id=@inv507final
WHERE handover_record_id=@handover507 AND compensation_amount IS NOT NULL;

INSERT INTO hdbhms.contract_liquidations
    (contract_id,liquidation_date,reason,deposit_amount,deposit_deduction_amount,deposit_deduction_reason,deposit_refund_amount,final_invoice_id,signed_file_id,status,created_by,created_at)
VALUES (@c507,'2026-06-30','Người thuê chuyển đi sau khi hoàn tất nghĩa vụ',3000000,750000,'Thiếu remote và sửa bình nóng lạnh',2250000,@inv507final,@lease_signed_file,'CONFIRMED',@owner_id,'2026-06-30 18:00:00');

INSERT INTO hdbhms.ledger_entries
    (entry_code,entry_date,source_type,source_id,account_code,debit_amount,credit_amount,description,posted_at,reversed_entry_id)
VALUES
    ('DEMO-LEDGER-DEP-402','2026-07-01','PAYMENT',@tx402,'CASH_DEPOSIT',2600000,0,'Thu cọc phòng 402','2026-07-01 10:00:00',NULL),
    ('DEMO-LEDGER-RENT-501','2026-05-03','PAYMENT',@tx501paid,'CASH_RENT',2600000,0,'Thu tiền phòng 501','2026-05-03 09:05:00',NULL),
    ('DEMO-LEDGER-COMP-502','2026-06-26','PAYMENT',@tx502,'CASH_COMPENSATION',450000,0,'Thu bồi thường sửa chữa phòng 502','2026-06-26 09:05:00',NULL);

-- Debt snapshots and escalation tasks.
INSERT INTO hdbhms.debt_snapshots
    (room_id,contract_id,snapshot_date,rent_debt_amount,utility_debt_amount,other_debt_amount,rent_debt_months,utility_debt_months,mixed_debt_amount,debt_limit_amount,is_over_limit,created_at)
VALUES
    (@r406,@c406,'2026-07-10',2600000,400000,0,1,1,3000000,5200000,FALSE,'2026-07-10 07:00:00'),
    (@r501,@c501,'2026-07-10',2600000,1300000,200000,1,2,4100000,1733333,TRUE,'2026-07-10 07:00:00'),
    (@r503,@c503,'2026-07-08',0,0,0,0,0,0,4800000,FALSE,'2026-07-08 07:00:00');

SET @debt406 := (SELECT debt_snapshot_id FROM hdbhms.debt_snapshots WHERE room_id=@r406 AND snapshot_date='2026-07-10');
SET @debt501 := (SELECT debt_snapshot_id FROM hdbhms.debt_snapshots WHERE room_id=@r501 AND snapshot_date='2026-07-10');
SET @debt503 := (SELECT debt_snapshot_id FROM hdbhms.debt_snapshots WHERE room_id=@r503 AND snapshot_date='2026-07-08');

INSERT INTO hdbhms.debt_notice_trackers
    (lease_contract_id,unresponsive_count,last_notice_date,created_at,updated_at)
VALUES
    (@c406,1,'2026-07-07','2026-07-01 08:00:00','2026-07-07 08:00:00'),
    (@c501,3,'2026-07-10','2026-05-08 08:00:00','2026-07-10 08:00:00');

INSERT INTO hdbhms.manager_tasks
    (title,description,assignee_id,room_id,lease_contract_id,status,due_date,completed_at,created_at,updated_at)
VALUES
    ('Liên hệ công nợ phòng 501','Đã gửi 3 lần nhắc nhưng chưa phản hồi',@manager_id,@r501,@c501,'PENDING','2026-07-12',NULL,'2026-07-10 08:00:00','2026-07-10 08:00:00'),
    ('Chuẩn bị bàn giao phòng 406','Hợp đồng sắp hết hạn và khách đã báo chuyển đi',@manager_id,@r406,@c406,'PENDING','2026-08-17',NULL,'2026-07-01 08:00:00','2026-07-01 08:00:00'),
    ('Kiểm tra hợp đồng hết hạn phòng 407','Phải xử lý trong 2 ngày',@manager_id,@r407,@c407,'PENDING','2026-07-07',NULL,'2026-07-06 08:00:00','2026-07-06 08:00:00');

INSERT INTO hdbhms.scheduled_tasks
    (task_type,target_type,target_id,due_at,status,retry_count,payload,executed_at,created_at)
VALUES
    ('INVOICE_REMINDER','INVOICE',@inv501overdue,'2026-05-08 08:00:00','DONE',0,CAST('{"daysOverdue":3,"roomCode":"501"}' AS BINARY),'2026-05-08 08:00:05','2026-05-05 08:00:00'),
    ('DEBT_WARNING','DEBT_SNAPSHOT',@debt501,'2026-07-10 08:00:00','DONE',0,CAST('{"overLimit":true,"roomCode":"501"}' AS BINARY),'2026-07-10 08:00:05','2026-07-10 07:00:00'),
    ('CONTRACT_EXPIRY','CONTRACT',@c406,'2026-07-15 08:00:00','PENDING',0,CAST('{"roomCode":"406","milestone":"six-weeks"}' AS BINARY),NULL,'2026-07-01 08:00:00'),
    ('CONTRACT_EXPIRY','CONTRACT',@c407,'2026-07-07 08:00:00','PENDING',0,CAST('{"roomCode":"407","milestone":"expired-plus-two-days"}' AS BINARY),NULL,'2026-07-06 08:00:00');

-- Maintenance lifecycle. The current schema has no WAITING_PAYMENT status; tenant charges are represented by invoices/pending charges.
INSERT INTO hdbhms.maintenance_tickets
    (ticket_code,property_id,room_id,contract_id,created_by,ticket_scope,priority,category,title,description,status,rejection_reason,assigned_to,worker_name,external_repairman_name,external_repairman_phone,external_repair_provider,external_repair_note,repairman_phone,repair_items,completed_at,created_at,updated_at)
VALUES
    ('DEMO-MT-502-PENDING',@property_id,@r502,@c502,@u502,'TENANT_ROOM','MEDIUM','ELECTRICITY','Ổ cắm chập chờ tiếp nhận','Ổ cắm gần bàn học phát tia lửa, cần kiểm tra','PENDING_ACCEPTANCE',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-07-10 07:00:00','2026-07-10 07:00:00'),
    ('DEMO-MT-502-ACCEPTED',@property_id,@r502,@c502,@u502,'TENANT_ROOM','LOW','PLUMBING','Vòi nước rò đã tiếp nhận','Vòi lavabo rò nhẹ','ACCEPTED',NULL,@manager_id,'Thợ demo A',NULL,NULL,NULL,NULL,'0988999901','Thay gioăng vòi',NULL,'2026-07-08 07:00:00','2026-07-08 08:00:00'),
    ('DEMO-MT-502-INPROGRESS',@property_id,@r502,@c502,@u502,'TENANT_ROOM','HIGH','APPLIANCE','Điều hòa đang sửa','Điều hòa không mát','IN_PROGRESS',NULL,@manager_id,NULL,'Nguyễn Văn Thợ','0988999902','Điện lạnh Demo','Đang chờ linh kiện',NULL,'Vệ sinh và thay tụ',NULL,'2026-07-06 07:00:00','2026-07-09 08:00:00'),
    ('DEMO-MT-502-WAITING',@property_id,@r502,@c502,@u502,'TENANT_ROOM','MEDIUM','FURNITURE','Bàn học chờ khách xác nhận','Đã thay mặt bàn mới','WAITING_CONFIRMATION',NULL,@manager_id,'Thợ demo B',NULL,NULL,NULL,NULL,'0988999903','Thay mặt bàn',NULL,'2026-07-01 07:00:00','2026-07-05 08:00:00'),
    ('DEMO-MT-502-COMPLETED',@property_id,@r502,@c502,@u502,'TENANT_ROOM','HIGH','APPLIANCE','Sửa bình nóng lạnh hoàn tất','Hỏng do sử dụng sai, người thuê chịu chi phí','COMPLETED',NULL,@manager_id,'Thợ demo C',NULL,NULL,NULL,NULL,'0988999904','Thay thanh đốt', '2026-06-26 10:00:00','2026-06-20 07:00:00','2026-06-26 10:00:00'),
    ('DEMO-MT-502-REJECTED',@property_id,@r502,@c502,@u502,'TENANT_ROOM','LOW','OTHER','Yêu cầu ngoài phạm vi','Yêu cầu thay thiết bị cá nhân không thuộc tài sản phòng','REJECTED','Thiết bị cá nhân, không thuộc phạm vi nhà trọ',@manager_id,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-06-15 07:00:00','2026-06-15 08:00:00'),
    ('DEMO-MT-408-OPERATION',@property_id,@r408,NULL,@manager_id,'PROPERTY_OPERATION','URGENT','ELECTRICITY','Sửa hệ thống điện phòng trống','Thay dây điện âm tường trước khi cho thuê lại','IN_PROGRESS',NULL,@manager_id,NULL,'Công ty Điện Demo','0988999905','Điện Demo Co.','Chi phí do nhà trọ chịu',NULL,'Thay dây và aptomat',NULL,'2026-07-01 07:00:00','2026-07-10 08:00:00');

SET @mt502pending := (SELECT maintenance_ticket_id FROM hdbhms.maintenance_tickets WHERE ticket_code='DEMO-MT-502-PENDING');
SET @mt502accepted := (SELECT maintenance_ticket_id FROM hdbhms.maintenance_tickets WHERE ticket_code='DEMO-MT-502-ACCEPTED');
SET @mt502progress := (SELECT maintenance_ticket_id FROM hdbhms.maintenance_tickets WHERE ticket_code='DEMO-MT-502-INPROGRESS');
SET @mt502waiting := (SELECT maintenance_ticket_id FROM hdbhms.maintenance_tickets WHERE ticket_code='DEMO-MT-502-WAITING');
SET @mt502completed := (SELECT maintenance_ticket_id FROM hdbhms.maintenance_tickets WHERE ticket_code='DEMO-MT-502-COMPLETED');
SET @mt502rejected := (SELECT maintenance_ticket_id FROM hdbhms.maintenance_tickets WHERE ticket_code='DEMO-MT-502-REJECTED');
SET @mt408 := (SELECT maintenance_ticket_id FROM hdbhms.maintenance_tickets WHERE ticket_code='DEMO-MT-408-OPERATION');

INSERT INTO hdbhms.maintenance_ticket_events
    (ticket_id,from_status,to_status,action,note,created_by,created_at)
VALUES
    (@mt502pending,NULL,'PENDING_ACCEPTANCE','SUBMIT','Người thuê tạo phiếu',@u502,'2026-07-10 07:00:00'),
    (@mt502accepted,NULL,'PENDING_ACCEPTANCE','SUBMIT','Người thuê tạo phiếu',@u502,'2026-07-08 07:00:00'),
    (@mt502accepted,'PENDING_ACCEPTANCE','ACCEPTED','ACCEPT','Quản lý đã tiếp nhận',@manager_id,'2026-07-08 08:00:00'),
    (@mt502progress,'ACCEPTED','IN_PROGRESS','START_WORK','Đã giao thợ ngoài',@manager_id,'2026-07-06 09:00:00'),
    (@mt502waiting,'IN_PROGRESS','WAITING_CONFIRMATION','FINISH_WORK','Đã sửa xong, chờ khách xác nhận',@manager_id,'2026-07-05 08:00:00'),
    (@mt502completed,'IN_PROGRESS','WAITING_CONFIRMATION','FINISH_WORK','Phát sinh bồi thường 450.000đ',@manager_id,'2026-06-25 08:00:00'),
    (@mt502completed,'WAITING_CONFIRMATION','COMPLETED','CONFIRM','Khách xác nhận sửa tốt và đã thanh toán',@u502,'2026-06-26 10:00:00'),
    (@mt502rejected,'PENDING_ACCEPTANCE','REJECTED','REJECT','Thiết bị cá nhân',@manager_id,'2026-06-15 08:00:00'),
    (@mt408,NULL,'PENDING_ACCEPTANCE','SUBMIT','Quản lý tạo phiếu vận hành',@manager_id,'2026-07-01 07:00:00'),
    (@mt408,'PENDING_ACCEPTANCE','ACCEPTED','ACCEPT','Chủ trọ duyệt chi phí vận hành',@owner_id,'2026-07-01 08:00:00'),
    (@mt408,'ACCEPTED','IN_PROGRESS','START_WORK','Đang thay dây điện',@manager_id,'2026-07-02 08:00:00');

INSERT INTO hdbhms.maintenance_costs
    (ticket_id,cost_type,description,amount,paid_by,cost_responsibility,charge_invoice_id,receipt_file_id,created_by,created_at)
VALUES
    (@mt502completed,'MATERIAL','Thay thanh đốt bình nóng lạnh',450000,'TENANT','TENANT',@inv502comp,NULL,@manager_id,'2026-06-25 08:00:00'),
    (@mt408,'MATERIAL','Dây điện và aptomat',900000,'LANDLORD','PROPERTY',NULL,NULL,@manager_id,'2026-07-02 08:00:00'),
    (@mt408,'LABOR','Nhân công điện',300000,'LANDLORD','PROPERTY',NULL,NULL,@manager_id,'2026-07-02 08:00:00');

INSERT INTO hdbhms.maintenance_reviews (ticket_id,reviewer_user_id,rating,comment,created_at)
VALUES (@mt502completed,@u502,5,'Sửa nhanh, thiết bị hoạt động tốt - đánh giá demo','2026-06-26 10:05:00');

INSERT INTO hdbhms.pending_billing_charges
    (property_id,room_id,contract_id,source_type,source_id,line_type,description,amount,billing_period,scheduled_issue_at,due_date,status,invoice_id,failure_reason,created_by,created_at,updated_at)
VALUES (@property_id,@r502,@c502,'MAINTENANCE_TICKET',@mt502waiting,'MAINTENANCE_COMPENSATION','Chi phí sửa bàn đang chờ lập hóa đơn (thay thế WAITING_PAYMENT chưa có trong ticket enum)',300000,'2026-07','2026-07-31 08:00:00','2026-08-05 23:59:59','SCHEDULED',NULL,NULL,@manager_id,'2026-07-05 08:00:00','2026-07-05 08:00:00');

INSERT INTO hdbhms.operating_expenses
    (property_id,room_id,ticket_id,expense_code,expense_type,description,amount,expense_date,paid_by_user_id,receipt_file_id,status,approved_by,approved_at,created_by,created_at)
VALUES
    (@property_id,@r408,@mt408,'DEMO-EXP-408-REPAIR','REPAIR','Sửa hệ thống điện phòng 408',1200000,'2026-07-02',@owner_id,NULL,'PAID',@owner_id,'2026-07-01 08:00:00',@manager_id,'2026-07-01 07:00:00'),
    (@property_id,NULL,NULL,'DEMO-EXP-COMMON-ELECTRIC','COMMON_UTILITY','Tiền điện khu vực chung tháng 06/2026',600000,'2026-06-30',@owner_id,NULL,'PAID',@owner_id,'2026-06-30 09:00:00',@accountant_id,'2026-06-30 08:00:00'),
    (@property_id,NULL,NULL,'DEMO-EXP-SUPPLIES','SUPPLIES','Vật tư vệ sinh tháng 06/2026',300000,'2026-06-25',@owner_id,NULL,'APPROVED',@owner_id,'2026-06-25 09:00:00',@manager_id,'2026-06-25 08:00:00');

SET @exp408 := (SELECT operating_expense_id FROM hdbhms.operating_expenses WHERE expense_code='DEMO-EXP-408-REPAIR');
INSERT INTO hdbhms.ledger_entries
    (entry_code,entry_date,source_type,source_id,account_code,debit_amount,credit_amount,description,posted_at,reversed_entry_id)
VALUES ('DEMO-LEDGER-EXP-408','2026-07-02','EXPENSE',@exp408,'OPERATING_EXPENSE',0,1200000,'Chi sửa chữa phòng 408','2026-07-02 08:00:00',NULL);

-- Room transfer matrix: requested, approved, rejected, waiting payment and completed.
INSERT INTO hdbhms.room_transfer_requests
    (request_code,requester_id,old_contract_id,old_room_id,target_room_id,transferring_tenant_profile_ids,nominated_holder_profile_id,target_transfer_type,target_contract_id,requested_transfer_date,reason,reserved_slots,reservation_expires_at,target_holder_approved_by,target_holder_approved_at,target_holder_rejected_at,status,positive_difference_settlement_type,debt_snapshot_id,new_contract_id,replacement_old_contract_id,created_at,updated_at)
VALUES
    ('DEMO-TR-501-REQUESTED',@t501,@c501,@r501,@r505,JSON_ARRAY(@p501),@p501,'FULL_ROOM',NULL,'2026-08-01','Muốn chuyển sang phòng đang trống',1,NULL,NULL,NULL,NULL,'REQUESTED',NULL,@debt501,NULL,NULL,'2026-07-10 08:00:00','2026-07-10 08:00:00'),
    ('DEMO-TR-501-REJECTED',@t501,@c501,@r501,@r408,JSON_ARRAY(@p501),@p501,'FULL_ROOM',NULL,'2026-08-01','Thử chuyển vào phòng đang bảo trì',1,NULL,NULL,NULL,NULL,'REJECTED',NULL,@debt501,NULL,NULL,'2026-07-09 08:00:00','2026-07-09 09:00:00'),
    ('DEMO-TR-502-APPROVED',@t502,@c502,@r502,@r401,JSON_ARRAY(@p502),@p502,'FULL_ROOM',NULL,'2026-08-15','Đã được quản lý duyệt, chờ tenant xác nhận',1,'2026-12-31 23:59:59',@manager_id,'2026-07-09 09:00:00',NULL,'MANAGER_APPROVED',NULL,NULL,NULL,NULL,'2026-07-09 08:00:00','2026-07-09 09:00:00'),
    ('DEMO-TR-503-WAITING-PAYMENT',@t503,@c503,@r503,@r504,JSON_ARRAY(@p503),@p503,'FULL_ROOM',NULL,'2026-08-01','Chuyển sang phòng rộng và đắt hơn',1,'2026-12-31 23:59:59',@manager_id,'2026-07-08 08:30:00',NULL,'WAITING_PAYMENT','TENANT_PAY_MORE',@debt503,NULL,NULL,'2026-07-08 08:00:00','2026-07-08 09:00:00'),
    ('DEMO-TR-505-507-COMPLETED',@t507,@c505old,@r505,@r507,JSON_ARRAY(@p507),@p507,'FULL_ROOM',@c507,'2025-12-01','Chuyển sang phòng rẻ hơn, hoàn chênh lệch',1,NULL,@manager_id,'2025-11-20 09:00:00',NULL,'COMPLETED','CREDIT_NEXT_CONTRACT',NULL,@c507,NULL,'2025-11-15 08:00:00','2025-12-01 10:00:00');

SET @tr503 := (SELECT room_transfer_request_id FROM hdbhms.room_transfer_requests WHERE request_code='DEMO-TR-503-WAITING-PAYMENT');
SET @tr507 := (SELECT room_transfer_request_id FROM hdbhms.room_transfer_requests WHERE request_code='DEMO-TR-505-507-COMPLETED');

INSERT INTO hdbhms.transfer_settlements
    (transfer_request_id,old_room_remaining_value,new_room_required_value,difference_amount,settlement_type,positive_difference_settlement_type,old_room_final_invoice_id,transfer_difference_invoice_id,confirmed_by,confirmed_at,created_at)
VALUES
    (@tr503,4800000,5400000,600000,'TENANT_PAY_MORE','TENANT_PAY_MORE',NULL,@inv503diff,@manager_id,'2026-07-08 09:00:00','2026-07-08 09:00:00'),
    (@tr507,3000000,2900000,100000,'REFUND_NOW',NULL,NULL,NULL,@owner_id,'2025-12-01 10:00:00','2025-12-01 10:00:00');

-- Change requests and temporary sensitive-profile permission demonstrate role isolation.
INSERT INTO hdbhms.change_requests
    (request_code,request_type,requester_id,requester_role,target_type,target_id,title,description,request_payload,evidence_file_id,assigned_role,assigned_to,status,resolution_note,resolved_by,resolved_at,created_at,updated_at)
VALUES
    ('DEMO-CR-405-CO-OCCUPANT','ADD_CO_OCCUPANT',@u405,'TENANT','TENANT_PROFILE',@p405pending,'Thêm người ở chung phòng 405','Hồ sơ và tài khoản đang chờ duyệt',JSON_OBJECT('roomId',@r405,'contractId',@c405),NULL,'OWNER',@owner_id,'UNDER_REVIEW',NULL,NULL,NULL,'2026-07-05 09:00:00','2026-07-06 09:00:00'),
    ('DEMO-CR-406-MOVE-OUT','MOVE_OUT',@u406,'TENANT','CONTRACT',@c406,'Yêu cầu kết thúc hợp đồng','Chuyển đi khi hợp đồng hết hạn ngày 31/08/2026',JSON_OBJECT('expectedTerminationDate','2026-08-31'),NULL,'MANAGER',@manager_id,'PENDING',NULL,NULL,NULL,'2026-06-01 09:00:00','2026-06-01 09:00:00'),
    ('DEMO-CR-PROFILE-ACCESS-APPROVED','TENANT_PROFILE_ACCESS',@manager_id,'MANAGER','TENANT_PROFILE',@p404,'Xin xem hồ sơ khách phòng 404','Đối chiếu hồ sơ phục vụ bàn giao',NULL,NULL,'OWNER',@owner_id,'APPROVED','Cho phép xem trong 30 ngày',@owner_id,'2026-07-01 09:00:00','2026-07-01 08:00:00','2026-07-01 09:00:00'),
    ('DEMO-CR-PROFILE-ACCESS-REJECTED','TENANT_PROFILE_ACCESS',@manager_id,'MANAGER','TENANT_PROFILE',@p405,'Xin xem hồ sơ khách phòng 405','Không có lý do nghiệp vụ đầy đủ',NULL,NULL,'OWNER',@owner_id,'REJECTED','Từ chối do lý do chưa đầy đủ',@owner_id,'2026-07-02 09:00:00','2026-07-02 08:00:00','2026-07-02 09:00:00');

SET @cr405 := (SELECT change_request_id FROM hdbhms.change_requests WHERE request_code='DEMO-CR-405-CO-OCCUPANT');
SET @cr406 := (SELECT change_request_id FROM hdbhms.change_requests WHERE request_code='DEMO-CR-406-MOVE-OUT');
SET @crAccess := (SELECT change_request_id FROM hdbhms.change_requests WHERE request_code='DEMO-CR-PROFILE-ACCESS-APPROVED');

INSERT INTO hdbhms.change_request_events (request_id,from_status,to_status,note,acted_by,acted_at)
VALUES
    (@cr405,'PENDING','UNDER_REVIEW','Quản lý đã tiếp nhận hồ sơ',@manager_id,'2026-07-06 09:00:00'),
    (@cr406,NULL,'PENDING','Người thuê gửi yêu cầu',@u406,'2026-06-01 09:00:00'),
    (@crAccess,'UNDER_REVIEW','APPROVED','Chủ trọ cấp quyền tạm thời',@owner_id,'2026-07-01 09:00:00');

INSERT INTO hdbhms.permission_grants
    (grantee_user_id,target_type,target_id,source_change_request_id,granted_by,reason,duration_code,granted_at,expires_at,revoked_at,revoked_by,revoke_reason,created_at,updated_at)
VALUES (@manager_id,'TENANT_PROFILE',@p404,@crAccess,@owner_id,'Đối chiếu hồ sơ phục vụ bàn giao','DAYS_30','2026-07-01 09:00:00','2026-12-31 23:59:59',NULL,NULL,NULL,'2026-07-01 09:00:00','2026-07-01 09:00:00');

SET @grantProfile404 := (SELECT permission_grant_id FROM hdbhms.permission_grants WHERE source_change_request_id=@crAccess ORDER BY permission_grant_id DESC LIMIT 1);
INSERT INTO hdbhms.permission_access_audit_logs
    (permission_grant_id,viewer_user_id,target_type,target_id,action,reason,ip_address,user_agent,viewed_at,created_at)
VALUES
    (@grantProfile404,@manager_id,'TENANT_PROFILE',@p404,'VIEW_TENANT_PROFILE','Demo: manager xem hồ sơ sau khi Owner cấp quyền','127.0.0.1','HDBHMS demo seed','2026-07-01 09:05:00','2026-07-01 09:05:00');

-- Public/guest journey and auditable staff actions.
INSERT INTO hdbhms.visit_requests
    (property_id,room_id,visitor_name,visitor_phone,visitor_email,preferred_start,notes,created_at,deleted_at,deleted_by,status,updated_at)
VALUES
    (@property_id,@r505,'Khách xem phòng demo mới','0988999505','visitor.505@hdbhms.local','2026-07-15 09:00:00','Muốn xem phòng trống 505','2026-07-10 09:00:00',NULL,NULL,'NOT_VIEWED','2026-07-10 09:00:00'),
    (@property_id,@r402,'Khách đã được tư vấn demo','0988999402','visitor.402@hdbhms.local','2026-07-12 15:00:00','Đã xem phòng và chuyển sang luồng đặt cọc','2026-07-01 09:00:00',NULL,NULL,'VIEWED','2026-07-02 09:00:00'),
    (@property_id,@r408,'Khách chọn nhầm phòng bảo trì','0988999408','visitor.408@hdbhms.local','2026-07-13 10:00:00','Yêu cầu không hợp lệ vì phòng đang bảo trì','2026-07-03 09:00:00',NULL,NULL,'DISMISSED','2026-07-03 10:00:00');

INSERT INTO hdbhms.audit_logs
    (actor_user_id,action,entity_type,entity_id,before_json,after_json,ip_address,user_agent,created_at)
VALUES
    (@manager_id,'UPDATE_ROOM_STATUS','ROOM',@r408,CAST('{"status":"VACANT"}' AS BINARY),CAST('{"status":"MAINTENANCE"}' AS BINARY),'127.0.0.1','HDBHMS demo seed','2026-07-01 07:00:00'),
    (@owner_id,'APPROVE_CHANGE_REQUEST','CHANGE_REQUEST',@crAccess,CAST('{"status":"UNDER_REVIEW"}' AS BINARY),CAST('{"status":"APPROVED"}' AS BINARY),'127.0.0.1','HDBHMS demo seed','2026-07-01 09:00:00'),
    (@accountant_id,'RECONCILE_PAYMENT','INVOICE',@inv501partial,NULL,CAST('{"status":"PARTIALLY_PAID","paidAmount":1000000}' AS BINARY),'127.0.0.1','HDBHMS demo seed','2026-07-05 10:00:00');

-- Notifications: read/unread and domain-specific event coverage for web/mobile inboxes.
INSERT INTO hdbhms.notification_outbox
    (event_type,target_type,target_id,recipient_user_id,channel,title,body,payload,status,retry_count,max_retries,last_error,scheduled_at,sent_at,created_at,is_read,read_at,next_retry_at)
VALUES
    ('INVOICE_ISSUED','INVOICE',@inv501issued,@u501,'PUSH','Hóa đơn mới phòng 501','Hóa đơn tiền phòng tháng 06/2026 đã phát hành.',JSON_OBJECT('roomCode','501','invoiceCode','DEMO-INV-501-2026-06-ISSUED'),'SENT',0,3,NULL,'2026-06-01 08:00:00','2026-06-01 08:00:05','2026-06-01 08:00:00',TRUE,'2026-06-01 20:00:00',NULL),
    ('INVOICE_OVERDUE','INVOICE',@inv501overdue,@u501,'PUSH','Hóa đơn quá hạn','Phòng 501 có hóa đơn điện nước quá hạn.',JSON_OBJECT('roomCode','501','amount',1000000),'SENT',0,3,NULL,'2026-05-08 08:00:00','2026-05-08 08:00:05','2026-05-08 08:00:00',FALSE,NULL,NULL),
    ('PAYMENT_CONFIRMED','INVOICE',@inv404utility,@u404,'PUSH','Thanh toán thành công','Đã ghi nhận thanh toán hóa đơn phòng 404.',JSON_OBJECT('roomCode','404','amount',70000),'SENT',0,3,NULL,'2026-07-02 09:05:00','2026-07-02 09:05:05','2026-07-02 09:05:00',TRUE,'2026-07-02 09:10:00',NULL),
    ('CONTRACT_EXPIRING','CONTRACT',@c406,@u406,'PUSH','Hợp đồng sắp hết hạn','Hợp đồng phòng 406 sắp hết hạn ngày 31/08/2026.',JSON_OBJECT('roomCode','406','endDate','2026-08-31'),'SENT',0,3,NULL,'2026-06-01 08:00:00','2026-06-01 08:00:05','2026-06-01 08:00:00',FALSE,NULL,NULL),
    ('CONTRACT_EXPIRED','CONTRACT',@c407,@u407,'PUSH','Hợp đồng đã hết hạn','Hợp đồng phòng 407 đã hết hạn, vui lòng liên hệ quản lý.',JSON_OBJECT('roomCode','407'),'SENT',0,3,NULL,'2026-07-06 08:00:00','2026-07-06 08:00:05','2026-07-06 08:00:00',FALSE,NULL,NULL),
    ('MAINTENANCE_UPDATED','MAINTENANCE_TICKET',@mt502progress,@u502,'PUSH','Sự cố đang được xử lý','Điều hòa phòng 502 đang được thợ xử lý.',JSON_OBJECT('roomCode','502','status','IN_PROGRESS'),'SENT',0,3,NULL,'2026-07-06 09:00:00','2026-07-06 09:00:05','2026-07-06 09:00:00',TRUE,'2026-07-06 10:00:00',NULL),
    ('ROOM_TRANSFER_APPROVED','ROOM_TRANSFER',(SELECT room_transfer_request_id FROM hdbhms.room_transfer_requests WHERE request_code='DEMO-TR-502-APPROVED'),@u502,'PUSH','Đơn chuyển phòng đã được duyệt','Yêu cầu chuyển sang phòng 401 đã được quản lý duyệt.',JSON_OBJECT('targetRoom','401'),'SENT',0,3,NULL,'2026-07-09 09:00:00','2026-07-09 09:00:05','2026-07-09 09:00:00',FALSE,NULL,NULL),
    ('ROOM_TRANSFER_PAYMENT_REQUIRED','ROOM_TRANSFER',@tr503,@u503,'PUSH','Cần thanh toán chênh lệch','Vui lòng thanh toán 600.000đ để tiếp tục chuyển sang phòng 504.',JSON_OBJECT('targetRoom','504','amount',600000),'SENT',0,3,NULL,'2026-07-08 09:00:00','2026-07-08 09:00:05','2026-07-08 09:00:00',FALSE,NULL,NULL),
    ('PROFILE_ACCESS_REQUESTED','CHANGE_REQUEST',@crAccess,@owner_id,'WEB','Yêu cầu xem hồ sơ khách','Quản lý xin xem hồ sơ người thuê phòng 404.',JSON_OBJECT('roomCode','404'),'SENT',0,3,NULL,'2026-07-01 08:00:00','2026-07-01 08:00:05','2026-07-01 08:00:00',TRUE,'2026-07-01 08:30:00',NULL),
    ('DEBT_WARNING','DEBT_SNAPSHOT',@debt501,@manager_id,'WEB','Cảnh báo công nợ phòng 501','Công nợ phòng 501 đã vượt ngưỡng demo.',JSON_OBJECT('roomCode','501','overLimit',TRUE),'PENDING',1,3,'Demo: chờ retry để hiển thị trạng thái pending','2026-07-10 08:00:00',NULL,'2026-07-10 08:00:00',FALSE,NULL,'2026-07-11 08:00:00');

SET @notiPaid404 := (SELECT notification_outbox_id FROM hdbhms.notification_outbox WHERE event_type='PAYMENT_CONFIRMED' AND recipient_user_id=@u404 ORDER BY notification_outbox_id DESC LIMIT 1);
SET @notiOverdue501 := (SELECT notification_outbox_id FROM hdbhms.notification_outbox WHERE event_type='INVOICE_OVERDUE' AND recipient_user_id=@u501 ORDER BY notification_outbox_id DESC LIMIT 1);
INSERT INTO hdbhms.notification_deliveries
    (outbox_id,provider_message_id,delivery_status,error_message,delivered_at,read_at,created_at)
VALUES
    (@notiPaid404,'DEMO-PUSH-404-PAID','READ',NULL,'2026-07-02 09:05:10','2026-07-02 09:10:00','2026-07-02 09:05:05'),
    (@notiOverdue501,'DEMO-PUSH-501-OVERDUE','DELIVERED',NULL,'2026-05-08 08:00:10',NULL,'2026-05-08 08:00:05');
