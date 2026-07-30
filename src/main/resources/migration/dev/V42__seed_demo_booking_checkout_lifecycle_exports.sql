SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS hdbhms.seed_demo_booking_checkout_lifecycle_exports_v42;

DELIMITER //

CREATE PROCEDURE hdbhms.seed_demo_booking_checkout_lifecycle_exports_v42()
BEGIN
    SET @property_id := (
        SELECT property_id
        FROM hdbhms.properties
        WHERE property_code = 'HAI_DANG_1'
          AND deleted_at IS NULL
        LIMIT 1
    );

    IF @property_id IS NOT NULL
        AND NOT EXISTS (
            SELECT 1
            FROM hdbhms.lease_contracts
            WHERE contract_code = 'HD-SEED-401-2026'
              AND deleted_at IS NULL
        ) THEN
        START TRANSACTION;

        SET @seed_now := '2026-07-30 09:00:00';
        SET @password_hash := '$2a$10$2Dy4Vg1B5BKuiUMPRuTAluvk/0XzLuSgLGaABFHCoWHaUfUtDFGqm';

        INSERT INTO hdbhms.users
            (phone, email, password_hash, role, status, last_login_at, email_verified, must_change_password, created_at, updated_at, deleted_at)
        VALUES
            ('0977000001', 'seed.owner@hdbhms.local', @password_hash, 'OWNER', 'ACTIVE', @seed_now, TRUE, FALSE, '2026-01-01 08:00:00', @seed_now, NULL),
            ('0977000002', 'seed.manager@hdbhms.local', @password_hash, 'MANAGER', 'ACTIVE', @seed_now, TRUE, FALSE, '2026-01-01 08:00:00', @seed_now, NULL),
            ('0977000400', 'seed.tenant@hdbhms.local', @password_hash, 'TENANT', 'ACTIVE', @seed_now, TRUE, FALSE, '2025-09-01 08:00:00', @seed_now, NULL),
            ('0977000456', 'seed.tenant405.co@hdbhms.local', @password_hash, 'TENANT', 'ACTIVE', @seed_now, TRUE, FALSE, '2025-11-01 08:00:00', @seed_now, NULL),
            ('0977000507', 'seed.tenant507.stay@hdbhms.local', @password_hash, 'TENANT', 'ACTIVE', @seed_now, TRUE, FALSE, '2026-01-01 08:00:00', @seed_now, NULL);

        SET @owner_id := (SELECT user_id FROM hdbhms.users WHERE email = 'seed.owner@hdbhms.local' AND deleted_at IS NULL LIMIT 1);
        SET @manager_id := (SELECT user_id FROM hdbhms.users WHERE email = 'seed.manager@hdbhms.local' AND deleted_at IS NULL LIMIT 1);
        SET @tenant_demo_user_id := (SELECT user_id FROM hdbhms.users WHERE email = 'seed.tenant@hdbhms.local' AND deleted_at IS NULL LIMIT 1);
        SET @u405_co := (SELECT user_id FROM hdbhms.users WHERE email = 'seed.tenant405.co@hdbhms.local' AND deleted_at IS NULL LIMIT 1);
        SET @u507_stay := (SELECT user_id FROM hdbhms.users WHERE email = 'seed.tenant507.stay@hdbhms.local' AND deleted_at IS NULL LIMIT 1);

        INSERT INTO hdbhms.collection_accounts
            (property_id, account_type, bank_name, account_number, account_holder, provider, status, created_at)
        VALUES
            (@property_id, 'RENT', 'Seed Bank', 'SEED-RENT-001', 'HDBHMS SEED', 'BANK', 'ACTIVE', '2026-01-01 08:00:00'),
            (@property_id, 'UTILITY', 'Seed Bank', 'SEED-UTILITY-001', 'HDBHMS SEED', 'BANK', 'ACTIVE', '2026-01-01 08:00:00'),
            (@property_id, 'OPERATING', 'Seed Cash', 'SEED-OPERATING-001', 'HDBHMS SEED', 'CASH', 'ACTIVE', '2026-01-01 08:00:00') AS new_account
        ON DUPLICATE KEY UPDATE
            property_id = new_account.property_id,
            bank_name = new_account.bank_name,
            account_holder = new_account.account_holder,
            status = new_account.status;
        SET @rent_account := (SELECT collection_account_id FROM hdbhms.collection_accounts WHERE account_number = 'SEED-RENT-001' AND account_type = 'RENT' LIMIT 1);
        SET @utility_account := (SELECT collection_account_id FROM hdbhms.collection_accounts WHERE account_number = 'SEED-UTILITY-001' AND account_type = 'UTILITY' LIMIT 1);

        UPDATE hdbhms.rooms
        SET current_status = CASE room_code
                WHEN '301' THEN 'VACANT'
                WHEN '302' THEN 'OCCUPIED'
                WHEN '401' THEN 'OCCUPIED'
                WHEN '402' THEN 'SOON_VACANT'
                WHEN '403' THEN 'OCCUPIED'
                WHEN '404' THEN 'OCCUPIED'
                WHEN '405' THEN 'OCCUPIED'
                WHEN '406' THEN 'VACANT'
                WHEN '407' THEN 'VACANT'
                WHEN '408' THEN 'MAINTENANCE'
                WHEN '501' THEN 'OCCUPIED'
                WHEN '502' THEN 'RESERVED_FOR_TRANSFER'
                WHEN '503' THEN 'OCCUPIED'
                WHEN '504' THEN 'RESERVED_FOR_TRANSFER'
                WHEN '505' THEN 'VACANT'
                WHEN '506' THEN 'OCCUPIED'
                WHEN '507' THEN 'OCCUPIED'
                ELSE current_status
            END,
            public_note = CASE room_code
                WHEN '301' THEN 'Seed: thanh lý đã hoàn tất.'
                WHEN '302' THEN 'Seed: yêu cầu thanh lý bị từ chối.'
                WHEN '401' THEN 'Seed: phòng đang ở, đủ hóa đơn và file xuất.'
                WHEN '402' THEN 'Seed: phòng sắp trống từ 2026-08-15.'
                WHEN '403' THEN 'Seed: thanh lý đang xử lý, có hóa đơn tất toán.'
                WHEN '404' THEN 'Seed: phòng sắp hết hạn, demo gia hạn hợp đồng.'
                WHEN '405' THEN 'Seed: nguồn chuyển phòng, người đứng tên chọn người ở cùng chuyển cùng.'
                WHEN '406' THEN 'Seed: phòng đích cho yêu cầu chuyển phòng mới tạo.'
                WHEN '407' THEN 'Seed: phòng trống có thể đặt lịch xem.'
                WHEN '408' THEN 'Seed: phòng bảo trì, không cho đặt lịch.'
                WHEN '501' THEN 'Seed: nguồn chuyển phòng đang chờ ký hợp đồng mới.'
                WHEN '502' THEN 'Seed: phòng đích chuyển phòng chờ ký hợp đồng.'
                WHEN '503' THEN 'Seed: chuyển phòng đã bàn giao, còn hóa đơn chốt.'
                WHEN '504' THEN 'Seed: phòng đích chờ hoàn tất chuyển phòng.'
                WHEN '505' THEN 'Seed: phòng nguồn đã chuyển đi xong.'
                WHEN '506' THEN 'Seed: phòng đích chuyển phòng đã hoàn tất.'
                WHEN '507' THEN 'Seed: người đứng tên rời đi, người ở cùng muốn ở lại.'
                ELSE public_note
            END,
            internal_note = CASE room_code
                WHEN '402' THEN 'Ngày dự kiến trống: 2026-08-15'
                WHEN '408' THEN 'Trạng thái bảo trì demo.'
                WHEN '502' THEN 'Reserved by transfer request CP_P502_30_07_2026'
                WHEN '504' THEN 'Reserved by transfer request CP_P504_30_07_2026'
                WHEN '507' THEN 'Needs holder replacement flow, room remains occupied.'
                ELSE internal_note
            END,
            updated_at = @seed_now
        WHERE property_id = @property_id
          AND room_code IN ('301', '302', '401', '402', '403', '404', '405', '406', '407', '408', '501', '502', '503', '504', '505', '506', '507')
          AND deleted_at IS NULL;

        SET @r401 := (SELECT room_id FROM hdbhms.rooms WHERE property_id = @property_id AND room_code = '401' LIMIT 1);
        SET @r402 := (SELECT room_id FROM hdbhms.rooms WHERE property_id = @property_id AND room_code = '402' LIMIT 1);
        SET @r403 := (SELECT room_id FROM hdbhms.rooms WHERE property_id = @property_id AND room_code = '403' LIMIT 1);
        SET @r404 := (SELECT room_id FROM hdbhms.rooms WHERE property_id = @property_id AND room_code = '404' LIMIT 1);
        SET @r405 := (SELECT room_id FROM hdbhms.rooms WHERE property_id = @property_id AND room_code = '405' LIMIT 1);
        SET @r406 := (SELECT room_id FROM hdbhms.rooms WHERE property_id = @property_id AND room_code = '406' LIMIT 1);
        SET @r407 := (SELECT room_id FROM hdbhms.rooms WHERE property_id = @property_id AND room_code = '407' LIMIT 1);
        SET @r408 := (SELECT room_id FROM hdbhms.rooms WHERE property_id = @property_id AND room_code = '408' LIMIT 1);
        SET @r501 := (SELECT room_id FROM hdbhms.rooms WHERE property_id = @property_id AND room_code = '501' LIMIT 1);
        SET @r502 := (SELECT room_id FROM hdbhms.rooms WHERE property_id = @property_id AND room_code = '502' LIMIT 1);
        SET @r503 := (SELECT room_id FROM hdbhms.rooms WHERE property_id = @property_id AND room_code = '503' LIMIT 1);
        SET @r504 := (SELECT room_id FROM hdbhms.rooms WHERE property_id = @property_id AND room_code = '504' LIMIT 1);
        SET @r505 := (SELECT room_id FROM hdbhms.rooms WHERE property_id = @property_id AND room_code = '505' LIMIT 1);
        SET @r506 := (SELECT room_id FROM hdbhms.rooms WHERE property_id = @property_id AND room_code = '506' LIMIT 1);
        SET @r507 := (SELECT room_id FROM hdbhms.rooms WHERE property_id = @property_id AND room_code = '507' LIMIT 1);
        SET @r301 := (SELECT room_id FROM hdbhms.rooms WHERE property_id = @property_id AND room_code = '301' LIMIT 1);
        SET @r302 := (SELECT room_id FROM hdbhms.rooms WHERE property_id = @property_id AND room_code = '302' LIMIT 1);

        INSERT INTO hdbhms.file_metadata
            (owner_user_id, storage_key, original_name, mime_type, size_bytes, sha256_checksum, category, is_sensitive, created_at, deleted_at)
        VALUES
            (@owner_id, 'seed-demo/files/room-sample.jpg', 'seed-room-sample.jpg', 'image/jpeg', 180000, REPEAT('a', 64), 'ROOM_IMAGE', FALSE, '2026-01-01 08:00:00', NULL),
            (@owner_id, 'seed-demo/files/portrait.jpg', 'seed-portrait.jpg', 'image/jpeg', 160000, REPEAT('b', 64), 'PORTRAIT_PHOTO', TRUE, '2026-01-01 08:00:00', NULL),
            (@owner_id, 'seed-demo/files/cccd-front.jpg', 'seed-cccd-front.jpg', 'image/jpeg', 220000, REPEAT('c', 64), 'ID_CARD', TRUE, '2026-01-01 08:00:00', NULL),
            (@owner_id, 'seed-demo/files/cccd-back.jpg', 'seed-cccd-back.jpg', 'image/jpeg', 210000, REPEAT('d', 64), 'ID_CARD', TRUE, '2026-01-01 08:00:00', NULL),
            (@owner_id, 'seed-demo/files/lease-signed-401.pdf', 'HDT_P401_01_01_2026.pdf', 'application/pdf', 50000, REPEAT('e', 64), 'CONTRACT', TRUE, '2026-01-01 08:00:00', NULL),
            (@owner_id, 'seed-demo/files/lease-draft-404.pdf', 'HDT_P404_30_07_2026.pdf', 'application/pdf', 50000, REPEAT('f', 64), 'LEASE_CONTRACT_DRAFT', TRUE, '2026-07-30 08:00:00', NULL),
            (@owner_id, 'seed-demo/files/handover-401.pdf', 'BBBG_P401_01_01_2026.pdf', 'application/pdf', 48000, REPEAT('1', 64), 'HANDOVER_DOCUMENT', TRUE, '2026-01-01 08:00:00', NULL),
            (@owner_id, 'seed-demo/files/liquidation-403.pdf', 'TLHD_P403_30_07_2026.pdf', 'application/pdf', 46000, REPEAT('2', 64), 'CONTRACT', TRUE, '2026-07-30 08:00:00', NULL),
            (@owner_id, 'seed-demo/files/receipt-301.pdf', 'bien-nhan-checkout-301.pdf', 'application/pdf', 30000, REPEAT('3', 64), 'RECEIPT', TRUE, '2026-07-25 08:00:00', NULL);

        SET @room_image_file := (SELECT file_metadata_id FROM hdbhms.file_metadata WHERE storage_key = 'seed-demo/files/room-sample.jpg' LIMIT 1);
        SET @portrait_file := (SELECT file_metadata_id FROM hdbhms.file_metadata WHERE storage_key = 'seed-demo/files/portrait.jpg' LIMIT 1);
        SET @id_front_file := (SELECT file_metadata_id FROM hdbhms.file_metadata WHERE storage_key = 'seed-demo/files/cccd-front.jpg' LIMIT 1);
        SET @id_back_file := (SELECT file_metadata_id FROM hdbhms.file_metadata WHERE storage_key = 'seed-demo/files/cccd-back.jpg' LIMIT 1);
        SET @lease_signed_file := (SELECT file_metadata_id FROM hdbhms.file_metadata WHERE storage_key = 'seed-demo/files/lease-signed-401.pdf' LIMIT 1);
        SET @lease_draft_404_file := (SELECT file_metadata_id FROM hdbhms.file_metadata WHERE storage_key = 'seed-demo/files/lease-draft-404.pdf' LIMIT 1);
        SET @handover_401_file := (SELECT file_metadata_id FROM hdbhms.file_metadata WHERE storage_key = 'seed-demo/files/handover-401.pdf' LIMIT 1);
        SET @liquidation_403_file := (SELECT file_metadata_id FROM hdbhms.file_metadata WHERE storage_key = 'seed-demo/files/liquidation-403.pdf' LIMIT 1);
        SET @receipt_file := (SELECT file_metadata_id FROM hdbhms.file_metadata WHERE storage_key = 'seed-demo/files/receipt-301.pdf' LIMIT 1);

        INSERT INTO hdbhms.room_images (room_id, file_id, sort_order, created_at)
        VALUES
            (@r402, @room_image_file, 1, '2026-01-01 08:00:00'),
            (@r407, @room_image_file, 1, '2026-01-01 08:00:00'),
            (@r408, @room_image_file, 1, '2026-01-01 08:00:00');

        INSERT INTO hdbhms.tenants
            (user_id, property_id, created_at, updated_at, deleted_at)
        VALUES
            (@owner_id, @property_id, '2026-01-01 08:00:00', @seed_now, NULL),
            (@manager_id, @property_id, '2026-01-01 08:00:00', @seed_now, NULL),
            (@tenant_demo_user_id, @property_id, '2026-01-01 08:00:00', @seed_now, NULL),
            (@u405_co, @property_id, '2026-01-01 08:00:00', @seed_now, NULL),
            (@u507_stay, @property_id, '2026-01-01 08:00:00', @seed_now, NULL);

        SET @owner_tenant := (SELECT tenant_id FROM hdbhms.tenants WHERE user_id = @owner_id AND property_id = @property_id AND deleted_at IS NULL LIMIT 1);
        SET @tenant_demo_tenant_id := (SELECT tenant_id FROM hdbhms.tenants WHERE user_id = @tenant_demo_user_id AND property_id = @property_id AND deleted_at IS NULL LIMIT 1);
        SET @tenant405_co := (SELECT tenant_id FROM hdbhms.tenants WHERE user_id = @u405_co AND property_id = @property_id AND deleted_at IS NULL LIMIT 1);
        SET @tenant507_stay := (SELECT tenant_id FROM hdbhms.tenants WHERE user_id = @u507_stay AND property_id = @property_id AND deleted_at IS NULL LIMIT 1);

        INSERT INTO hdbhms.property_staff_assignments
            (property_id, staff_user_id, assigned_role, assignment_status, is_primary, notes, assigned_by_user_id, started_at, ended_at, created_at, updated_at)
        VALUES
            (@property_id, @manager_id, 'MANAGER', 'ACTIVE', TRUE, 'Seed demo primary manager.', @owner_id, '2026-01-01 08:00:00', NULL, '2026-01-01 08:00:00', @seed_now);

        INSERT INTO hdbhms.person_profiles
            (user_id, full_name, dob, gender, phone, email, permanent_address, portrait_file_id, created_at, updated_at, deleted_at)
        VALUES
            (@owner_id, 'Nguyễn Minh Chủ Nhà', '1980-01-01', 'MALE', '0977000001', 'seed.owner@hdbhms.local', 'Hà Nội', @portrait_file, '2026-01-01 08:00:00', @seed_now, NULL),
            (@manager_id, 'Trần Thu Quản Lý', '1990-01-01', 'FEMALE', '0977000002', 'seed.manager@hdbhms.local', 'Hà Nội', @portrait_file, '2026-01-01 08:00:00', @seed_now, NULL),
            (@tenant_demo_user_id, 'Nguyễn Văn Tenant Demo', '2001-01-01', 'MALE', '0977000400', 'seed.tenant@hdbhms.local', 'Hà Nội', @portrait_file, '2026-01-01 08:00:00', @seed_now, NULL),
            (@u405_co, 'Đỗ Thị Seed 405 Ở Cùng', '2005-06-05', 'FEMALE', '0977000456', 'seed.tenant405.co@hdbhms.local', 'Hà Nội', @portrait_file, '2026-01-01 08:00:00', @seed_now, NULL),
            (@u507_stay, 'Đặng Thị Seed 507 Ở Lại', '2002-04-24', 'FEMALE', '0977000507', 'seed.tenant507.stay@hdbhms.local', 'Hà Nội', @portrait_file, '2026-01-01 08:00:00', @seed_now, NULL);

        SET @p401 := (SELECT person_profile_id FROM hdbhms.person_profiles WHERE email = 'seed.tenant@hdbhms.local' LIMIT 1);
        SET @p405_co := (SELECT person_profile_id FROM hdbhms.person_profiles WHERE email = 'seed.tenant405.co@hdbhms.local' LIMIT 1);
        SET @p507_stay := (SELECT person_profile_id FROM hdbhms.person_profiles WHERE email = 'seed.tenant507.stay@hdbhms.local' LIMIT 1);

        INSERT INTO hdbhms.identity_documents
            (profile_id, doc_type, doc_number, issued_date, issued_place, expiry_date, raw_ocr_data, front_file_id, back_file_id, status, created_at, updated_at)
        VALUES
            (@p401, 'CCCD', '099900000401', '2021-01-15', 'Bộ Công An', '2036-01-15', CAST(JSON_OBJECT('seed', 'demo', 'room', '401') AS BINARY), @id_front_file, @id_back_file, 'ACTIVE', '2026-01-01 08:00:00', @seed_now),
            (@p405_co, 'CCCD', '099900000456', '2021-01-15', 'Bộ Công An', '2036-01-15', CAST(JSON_OBJECT('seed', 'demo', 'room', '405', 'role', 'co') AS BINARY), @id_front_file, @id_back_file, 'ACTIVE', '2026-01-01 08:00:00', @seed_now),
            (@p507_stay, 'CCCD', '099900000507', '2021-01-15', 'Bộ Công An', '2036-01-15', CAST(JSON_OBJECT('seed', 'demo', 'room', '507', 'role', 'stay') AS BINARY), @id_front_file, @id_back_file, 'ACTIVE', '2026-01-01 08:00:00', @seed_now);

        INSERT INTO hdbhms.vehicles
            (profile_id, vehicle_type, license_plate, image_file_id, status, created_at, deleted_at)
        VALUES
            (@p401, 'MOTORBIKE', '29-SEED-401', NULL, 'ACTIVE', '2026-01-01 08:00:00', NULL),
            (@p405_co, 'MOTORBIKE', '29-SEED-456', NULL, 'ACTIVE', '2026-01-01 08:00:00', NULL),
            (@p507_stay, 'MOTORBIKE', '29-SEED-507', NULL, 'ACTIVE', '2026-01-01 08:00:00', NULL);

        INSERT INTO hdbhms.emergency_contacts
            (tenant_profile_id, full_name, relationship, phone, created_at)
        VALUES
            (@p401, 'Liên hệ Seed 401', 'Người thân', '0988000401', '2026-01-01 08:00:00'),
            (@p405_co, 'Liên hệ Seed 405 Ở Cùng', 'Người thân', '0988000456', '2026-01-01 08:00:00'),
            (@p507_stay, 'Liên hệ Seed 507 Ở Lại', 'Người thân', '0988000507', '2026-01-01 08:00:00');

        INSERT INTO hdbhms.lease_contracts
            (contract_code, room_id, deposit_agreement_id, primary_tenant_profile_id, start_date, end_date, rent_start_date, monthly_rent, payment_cycle_months, deposit_amount, status, tenant_intention, expected_vacant_date, intention_recorded_at, previous_contract_id, contract_file_id, signed_at, created_by, created_at, updated_at, deleted_at)
        VALUES
            ('HD-SEED-401-2026', @r401, NULL, @p401, '2026-01-01', '2026-12-31', '2026-01-01', 2400000, 1, 2400000, 'ACTIVE', NULL, NULL, NULL, NULL, @lease_signed_file, '2026-01-01 09:00:00', @owner_tenant, '2026-01-01 08:00:00', @seed_now, NULL),
            ('HD-SEED-402-2026', @r402, NULL, @p401, '2025-09-01', '2026-08-15', '2025-09-01', 2500000, 1, 2500000, 'EXPIRING_SOON', 'MOVE_OUT', '2026-08-15', '2026-07-20 08:00:00', NULL, NULL, '2025-09-01 09:00:00', @owner_tenant, '2025-09-01 08:00:00', @seed_now, NULL),
            ('HD-SEED-403-2026', @r403, NULL, @p401, '2025-10-01', '2026-09-30', '2025-10-01', 2300000, 1, 2300000, 'TERMINATION_PENDING', 'MOVE_OUT', '2026-07-31', '2026-07-20 08:00:00', NULL, NULL, '2025-10-01 09:00:00', @owner_tenant, '2025-10-01 08:00:00', @seed_now, NULL),
            ('HD-SEED-404-2026', @r404, NULL, @p401, '2025-09-01', '2026-08-31', '2025-09-01', 2600000, 1, 2600000, 'EXPIRING_SOON', 'RENEW', NULL, '2026-07-21 08:00:00', NULL, @lease_draft_404_file, '2025-09-01 09:00:00', @owner_tenant, '2025-09-01 08:00:00', @seed_now, NULL),
            ('HD-SEED-405-2026', @r405, NULL, @p401, '2025-11-01', '2026-10-31', '2025-11-01', 2200000, 1, 2200000, 'ACTIVE', NULL, NULL, NULL, NULL, @lease_signed_file, '2025-11-01 09:00:00', @owner_tenant, '2025-11-01 08:00:00', @seed_now, NULL),
            ('HD-SEED-501-2026', @r501, NULL, @p401, '2026-01-01', '2026-12-31', '2026-01-01', 2500000, 1, 2500000, 'ACTIVE', NULL, NULL, NULL, NULL, @lease_signed_file, '2026-01-01 09:00:00', @owner_tenant, '2026-01-01 08:00:00', @seed_now, NULL),
            ('HD-SEED-502-TRANSFER-DRAFT', @r502, NULL, @p401, '2026-08-10', '2026-12-31', '2026-08-10', 2500000, 1, 2500000, 'CONFIRMED', NULL, NULL, NULL, NULL, @lease_signed_file, NULL, @owner_tenant, '2026-07-30 10:00:00', @seed_now, NULL),
            ('HD-SEED-503-2026', @r503, NULL, @p401, '2026-01-01', '2026-12-31', '2026-01-01', 2300000, 1, 2300000, 'ACTIVE', NULL, NULL, NULL, NULL, @lease_signed_file, '2026-01-01 09:00:00', @owner_tenant, '2026-01-01 08:00:00', @seed_now, NULL),
            ('HD-SEED-504-TRANSFER-SIGNED', @r504, NULL, @p401, '2026-08-08', '2026-12-31', '2026-08-08', 2300000, 1, 2300000, 'SIGNED', NULL, NULL, NULL, NULL, @lease_signed_file, '2026-07-30 11:30:00', @owner_tenant, '2026-07-30 10:30:00', @seed_now, NULL),
            ('HD-SEED-505-2026', @r505, NULL, @p401, '2026-01-01', '2026-12-31', '2026-01-01', 2200000, 1, 2200000, 'TRANSFERRED', 'TRANSFER', '2026-07-20', '2026-07-20 08:00:00', NULL, @lease_signed_file, '2026-01-01 09:00:00', @owner_tenant, '2026-01-01 08:00:00', @seed_now, NULL),
            ('HD-SEED-506-TRANSFER-ACTIVE', @r506, NULL, @p401, '2026-07-20', '2026-12-31', '2026-07-20', 2600000, 1, 2200000, 'ACTIVE', NULL, NULL, NULL, NULL, @lease_signed_file, '2026-07-20 09:00:00', @owner_tenant, '2026-07-20 08:00:00', @seed_now, NULL),
            ('HD-SEED-507-2026', @r507, NULL, @p401, '2026-01-01', '2026-12-31', '2026-01-01', 2450000, 1, 2450000, 'ACTIVE', NULL, NULL, NULL, NULL, @lease_signed_file, '2026-01-01 09:00:00', @owner_tenant, '2026-01-01 08:00:00', @seed_now, NULL),
            ('HD-SEED-301-2026', @r301, NULL, @p401, '2026-01-01', '2026-07-25', '2026-01-01', 2200000, 1, 2200000, 'LIQUIDATED', 'MOVE_OUT', '2026-07-25', '2026-07-20 08:00:00', NULL, @lease_signed_file, '2026-01-01 09:00:00', @owner_tenant, '2026-01-01 08:00:00', @seed_now, NULL),
            ('HD-SEED-302-2026', @r302, NULL, @p401, '2026-01-01', '2026-12-31', '2026-01-01', 2350000, 1, 2350000, 'ACTIVE', NULL, NULL, NULL, NULL, @lease_signed_file, '2026-01-01 09:00:00', @owner_tenant, '2026-01-01 08:00:00', @seed_now, NULL);

        SET @c401 := (SELECT lease_contract_id FROM hdbhms.lease_contracts WHERE contract_code = 'HD-SEED-401-2026' LIMIT 1);
        SET @c402 := (SELECT lease_contract_id FROM hdbhms.lease_contracts WHERE contract_code = 'HD-SEED-402-2026' LIMIT 1);
        SET @c403 := (SELECT lease_contract_id FROM hdbhms.lease_contracts WHERE contract_code = 'HD-SEED-403-2026' LIMIT 1);
        SET @c404 := (SELECT lease_contract_id FROM hdbhms.lease_contracts WHERE contract_code = 'HD-SEED-404-2026' LIMIT 1);
        SET @c405 := (SELECT lease_contract_id FROM hdbhms.lease_contracts WHERE contract_code = 'HD-SEED-405-2026' LIMIT 1);
        SET @c501 := (SELECT lease_contract_id FROM hdbhms.lease_contracts WHERE contract_code = 'HD-SEED-501-2026' LIMIT 1);
        SET @c502new := (SELECT lease_contract_id FROM hdbhms.lease_contracts WHERE contract_code = 'HD-SEED-502-TRANSFER-DRAFT' LIMIT 1);
        SET @c503 := (SELECT lease_contract_id FROM hdbhms.lease_contracts WHERE contract_code = 'HD-SEED-503-2026' LIMIT 1);
        SET @c504new := (SELECT lease_contract_id FROM hdbhms.lease_contracts WHERE contract_code = 'HD-SEED-504-TRANSFER-SIGNED' LIMIT 1);
        SET @c505 := (SELECT lease_contract_id FROM hdbhms.lease_contracts WHERE contract_code = 'HD-SEED-505-2026' LIMIT 1);
        SET @c506new := (SELECT lease_contract_id FROM hdbhms.lease_contracts WHERE contract_code = 'HD-SEED-506-TRANSFER-ACTIVE' LIMIT 1);
        SET @c507 := (SELECT lease_contract_id FROM hdbhms.lease_contracts WHERE contract_code = 'HD-SEED-507-2026' LIMIT 1);
        SET @c301 := (SELECT lease_contract_id FROM hdbhms.lease_contracts WHERE contract_code = 'HD-SEED-301-2026' LIMIT 1);
        SET @c302 := (SELECT lease_contract_id FROM hdbhms.lease_contracts WHERE contract_code = 'HD-SEED-302-2026' LIMIT 1);

        INSERT INTO hdbhms.contract_occupants
            (contract_id, tenant_id, tenant_profile_id, occupant_role, move_in_date, move_out_date, status, disabled_reason, disabled_by, disabled_at, created_at)
        VALUES
            (@c401, @tenant_demo_tenant_id, @p401, 'PRIMARY', '2026-01-01', NULL, 'ACTIVE', NULL, NULL, NULL, '2026-01-01 08:00:00'),
            (@c402, @tenant_demo_tenant_id, @p401, 'PRIMARY', '2025-09-01', NULL, 'ACTIVE', NULL, NULL, NULL, '2025-09-01 08:00:00'),
            (@c403, @tenant_demo_tenant_id, @p401, 'PRIMARY', '2025-10-01', NULL, 'ACTIVE', NULL, NULL, NULL, '2025-10-01 08:00:00'),
            (@c404, @tenant_demo_tenant_id, @p401, 'PRIMARY', '2025-09-01', NULL, 'ACTIVE', NULL, NULL, NULL, '2025-09-01 08:00:00'),
            (@c405, @tenant_demo_tenant_id, @p401, 'PRIMARY', '2025-11-01', NULL, 'ACTIVE', NULL, NULL, NULL, '2025-11-01 08:00:00'),
            (@c405, @tenant405_co, @p405_co, 'CO_OCCUPANT', '2025-11-01', NULL, 'ACTIVE', NULL, NULL, NULL, '2025-11-01 08:00:00'),
            (@c501, @tenant_demo_tenant_id, @p401, 'PRIMARY', '2026-01-01', NULL, 'ACTIVE', NULL, NULL, NULL, '2026-01-01 08:00:00'),
            (@c503, @tenant_demo_tenant_id, @p401, 'PRIMARY', '2026-01-01', NULL, 'ACTIVE', NULL, NULL, NULL, '2026-01-01 08:00:00'),
            (@c505, @tenant_demo_tenant_id, @p401, 'PRIMARY', '2026-01-01', '2026-07-20', 'MOVED_OUT', NULL, NULL, NULL, '2026-01-01 08:00:00'),
            (@c506new, @tenant_demo_tenant_id, @p401, 'PRIMARY', '2026-07-20', NULL, 'ACTIVE', NULL, NULL, NULL, '2026-07-20 08:00:00'),
            (@c507, @tenant_demo_tenant_id, @p401, 'PRIMARY', '2026-01-01', NULL, 'ACTIVE', NULL, NULL, NULL, '2026-01-01 08:00:00'),
            (@c507, @tenant507_stay, @p507_stay, 'CO_OCCUPANT', '2026-01-01', NULL, 'ACTIVE', NULL, NULL, NULL, '2026-01-01 08:00:00'),
            (@c301, @tenant_demo_tenant_id, @p401, 'PRIMARY', '2026-01-01', '2026-07-25', 'MOVED_OUT', NULL, NULL, NULL, '2026-01-01 08:00:00'),
            (@c302, @tenant_demo_tenant_id, @p401, 'PRIMARY', '2026-01-01', NULL, 'ACTIVE', NULL, NULL, NULL, '2026-01-01 08:00:00');

        INSERT INTO hdbhms.tenant_account_provisionings
            (tenant_profile_id, user_id, first_contract_id, latest_contract_id, status, recipient_email, sent_at, attempt_count, created_at, updated_at)
        VALUES
            (@p401, @tenant_demo_user_id, @c401, @c506new, 'ACTIVE', 'seed.tenant@hdbhms.local', @seed_now, 0, @seed_now, @seed_now),
            (@p405_co, @u405_co, @c405, @c405, 'ACTIVE', 'seed.tenant405.co@hdbhms.local', @seed_now, 0, @seed_now, @seed_now),
            (@p507_stay, @u507_stay, @c507, @c507, 'ACTIVE', 'seed.tenant507.stay@hdbhms.local', @seed_now, 0, @seed_now, @seed_now);

        INSERT INTO hdbhms.room_assets
            (room_id, asset_name, asset_category, quantity, current_condition, description, image_file_id, created_at, updated_at, deleted_at)
        VALUES
            (@r401, 'Điều hòa', 'ELECTRIC', 1, 'GOOD', 'Tài sản demo cho biên bản bàn giao.', NULL, '2026-01-01 08:00:00', @seed_now, NULL),
            (@r401, 'Bình nóng lạnh', 'ELECTRIC', 1, 'GOOD', 'Tài sản demo cho biên bản bàn giao.', NULL, '2026-01-01 08:00:00', @seed_now, NULL),
            (@r404, 'Tủ quần áo', 'FURNITURE', 1, 'GOOD', 'Tài sản demo cho gia hạn hợp đồng.', NULL, '2026-01-01 08:00:00', @seed_now, NULL);

        INSERT INTO hdbhms.contract_handover_records
            (contract_id, room_id, handover_type, handover_date, electricity_reading_id, water_reading_id, note, status, confirmed_by, confirmed_at, created_at, signed_document_id)
        VALUES
            (@c401, @r401, 'MOVE_IN', '2026-01-01 09:30:00', NULL, NULL, 'Biên bản bàn giao khi nhận phòng để demo xuất file.', 'CONFIRMED', @manager_id, '2026-01-01 09:35:00', '2026-01-01 09:30:00', @handover_401_file);
        SET @handover401 := LAST_INSERT_ID();

        INSERT INTO hdbhms.contract_handover_items
            (handover_record_id, room_asset_id, asset_name, quantity, condition_status, note, evidence_file_id, compensation_amount, compensation_invoice_id, created_at)
        SELECT @handover401, room_asset_id, asset_name, quantity, current_condition, 'Hạng mục bàn giao demo.', NULL, NULL, NULL, '2026-01-01 09:30:00'
        FROM hdbhms.room_assets
        WHERE room_id = @r401
          AND deleted_at IS NULL;

        INSERT INTO hdbhms.visit_requests
            (property_id, room_id, visitor_name, visitor_phone, visitor_email, preferred_start, notes, created_at, deleted_at, deleted_by, status, updated_at)
        VALUES
            (@property_id, @r407, 'Khách xem phòng trống Seed', '0988777407', 'visitor407@seed.local', '2026-07-30 09:00:00', 'Muốn xem phòng trống 407.', '2026-07-29 08:10:00', NULL, NULL, 'NOT_VIEWED', '2026-07-29 08:10:00'),
            (@property_id, @r402, 'Khách xem phòng sắp trống Seed', '0988777402', 'visitor402@seed.local', '2026-08-01 15:00:00', 'Muốn xem phòng sắp trống 402.', '2026-07-29 08:20:00', NULL, NULL, 'VIEWED', '2026-07-29 09:00:00'),
            (@property_id, @r408, 'Khách xem phòng bảo trì Seed', '0988777408', 'visitor408@seed.local', '2026-07-31 10:00:00', 'Phòng bảo trì, yêu cầu bị bỏ qua để demo bộ lọc.', '2026-07-29 08:30:00', NULL, NULL, 'DISMISSED', '2026-07-29 09:00:00');

        INSERT INTO hdbhms.invoices
            (invoice_code, property_id, room_id, lease_contract_id, deposit_agreement_id, deposit_batch_id, invoice_type, revision_no, billing_period, issue_date, due_date, status, subtotal_amount, discount_amount, total_amount, paid_amount, remaining_amount, collection_account_id, created_by, issued_at, voided_at, void_reason, created_at, updated_at)
        VALUES
            ('SEED-INV-401-2026-05-RENT-PAID', @property_id, @r401, @c401, NULL, NULL, 'RENT', 1, '2026-05', '2026-05-01 08:00:00', '2026-05-05 23:59:59', 'PAID', 2400000, 0, 2400000, 2400000, 0, @rent_account, @owner_id, '2026-05-01 08:00:00', NULL, NULL, '2026-05-01 08:00:00', '2026-05-03 09:05:00'),
            ('SEED-INV-401-2026-05-UTILITY-PAID', @property_id, @r401, @c401, NULL, NULL, 'UTILITY', 1, '2026-05', '2026-05-01 08:00:00', '2026-05-05 23:59:59', 'PAID', 320000, 0, 320000, 320000, 0, @utility_account, @owner_id, '2026-05-01 08:00:00', NULL, NULL, '2026-05-01 08:00:00', '2026-05-03 09:10:00'),
            ('SEED-INV-401-2026-06-RENT-PAID', @property_id, @r401, @c401, NULL, NULL, 'RENT', 1, '2026-06', '2026-06-01 08:00:00', '2026-06-05 23:59:59', 'PAID', 2400000, 0, 2400000, 2400000, 0, @rent_account, @owner_id, '2026-06-01 08:00:00', NULL, NULL, '2026-06-01 08:00:00', '2026-06-03 09:05:00'),
            ('SEED-INV-401-2026-06-UTILITY-PAID', @property_id, @r401, @c401, NULL, NULL, 'UTILITY', 1, '2026-06', '2026-06-01 08:00:00', '2026-06-05 23:59:59', 'PAID', 345000, 0, 345000, 345000, 0, @utility_account, @owner_id, '2026-06-01 08:00:00', NULL, NULL, '2026-06-01 08:00:00', '2026-06-03 09:10:00'),
            ('SEED-INV-402-2026-06-RENT-PAID', @property_id, @r402, @c402, NULL, NULL, 'RENT', 1, '2026-06', '2026-06-01 08:00:00', '2026-06-05 23:59:59', 'PAID', 2500000, 0, 2500000, 2500000, 0, @rent_account, @owner_id, '2026-06-01 08:00:00', NULL, NULL, '2026-06-01 08:00:00', '2026-06-04 09:05:00'),
            ('SEED-INV-402-2026-06-UTILITY-PAID', @property_id, @r402, @c402, NULL, NULL, 'UTILITY', 1, '2026-06', '2026-06-01 08:00:00', '2026-06-05 23:59:59', 'PAID', 300000, 0, 300000, 300000, 0, @utility_account, @owner_id, '2026-06-01 08:00:00', NULL, NULL, '2026-06-01 08:00:00', '2026-06-04 09:10:00'),
            ('SEED-INV-404-2026-06-RENT-PAID', @property_id, @r404, @c404, NULL, NULL, 'RENT', 1, '2026-06', '2026-06-01 08:00:00', '2026-06-05 23:59:59', 'PAID', 2600000, 0, 2600000, 2600000, 0, @rent_account, @owner_id, '2026-06-01 08:00:00', NULL, NULL, '2026-06-01 08:00:00', '2026-06-04 10:05:00'),
            ('SEED-INV-501-2026-06-RENT-PAID', @property_id, @r501, @c501, NULL, NULL, 'RENT', 1, '2026-06', '2026-06-01 08:00:00', '2026-06-05 23:59:59', 'PAID', 2500000, 0, 2500000, 2500000, 0, @rent_account, @owner_id, '2026-06-01 08:00:00', NULL, NULL, '2026-06-01 08:00:00', '2026-06-05 08:35:00'),
            ('SEED-INV-501-2026-06-UTILITY-PAID', @property_id, @r501, @c501, NULL, NULL, 'UTILITY', 1, '2026-06', '2026-06-01 08:00:00', '2026-06-05 23:59:59', 'PAID', 330000, 0, 330000, 330000, 0, @utility_account, @owner_id, '2026-06-01 08:00:00', NULL, NULL, '2026-06-01 08:00:00', '2026-06-05 08:40:00'),
            ('SEED-INV-302-2026-06-RENT-OVERDUE', @property_id, @r302, @c302, NULL, NULL, 'RENT', 1, '2026-06', '2026-06-01 08:00:00', '2026-06-05 23:59:59', 'OVERDUE', 2350000, 0, 2350000, 0, 2350000, @rent_account, @owner_id, '2026-06-01 08:00:00', NULL, NULL, '2026-06-01 08:00:00', @seed_now),
            ('SEED-INV-302-2026-06-UTILITY-OVERDUE', @property_id, @r302, @c302, NULL, NULL, 'UTILITY', 1, '2026-06', '2026-06-01 08:00:00', '2026-06-05 23:59:59', 'OVERDUE', 310000, 0, 310000, 0, 310000, @utility_account, @owner_id, '2026-06-01 08:00:00', NULL, NULL, '2026-06-01 08:00:00', @seed_now),
            ('SEED-INV-401-2026-07-RENT-PAID', @property_id, @r401, @c401, NULL, NULL, 'RENT', 1, '2026-07', '2026-07-01 08:00:00', '2026-07-05 23:59:59', 'PAID', 2400000, 0, 2400000, 2400000, 0, @rent_account, @owner_id, '2026-07-01 08:00:00', NULL, NULL, '2026-07-01 08:00:00', @seed_now),
            ('SEED-INV-401-2026-07-UTILITY-PAID', @property_id, @r401, @c401, NULL, NULL, 'UTILITY', 1, '2026-07', '2026-07-01 08:00:00', '2026-07-05 23:59:59', 'PAID', 360000, 0, 360000, 360000, 0, @utility_account, @owner_id, '2026-07-01 08:00:00', NULL, NULL, '2026-07-01 08:00:00', @seed_now),
            ('SEED-INV-403-2026-07-FINAL-ISSUED', @property_id, @r403, @c403, NULL, NULL, 'FINAL_SETTLEMENT', 1, '2026-07', '2026-07-30 08:00:00', '2026-08-03 23:59:59', 'ISSUED', 1850000, 0, 1850000, 0, 1850000, @rent_account, @owner_id, '2026-07-30 08:00:00', NULL, NULL, '2026-07-30 08:00:00', @seed_now),
            ('SEED-INV-503-TRANSFER-OUT-ISSUED', @property_id, @r503, @c503, NULL, NULL, 'FINAL_SETTLEMENT', 1, '2026-07', '2026-07-30 11:00:00', '2026-08-03 23:59:59', 'ISSUED', 480000, 0, 480000, 0, 480000, @utility_account, @manager_id, '2026-07-30 11:00:00', NULL, NULL, '2026-07-30 11:00:00', @seed_now),
            ('SEED-INV-505-TRANSFER-OUT-PAID', @property_id, @r505, @c505, NULL, NULL, 'FINAL_SETTLEMENT', 1, '2026-07', '2026-07-20 11:00:00', '2026-07-23 23:59:59', 'PAID', 350000, 0, 350000, 350000, 0, @utility_account, @manager_id, '2026-07-20 11:00:00', NULL, NULL, '2026-07-20 11:00:00', '2026-07-20 12:00:00'),
            ('SEED-INV-301-2026-07-FINAL-PAID', @property_id, @r301, @c301, NULL, NULL, 'FINAL_SETTLEMENT', 1, '2026-07', '2026-07-25 09:30:00', '2026-07-30 23:59:59', 'PAID', 750000, 0, 750000, 750000, 0, @rent_account, @owner_id, '2026-07-25 09:30:00', NULL, NULL, '2026-07-25 09:30:00', '2026-07-25 12:00:00');

        SET @inv401mayRent := (SELECT invoice_id FROM hdbhms.invoices WHERE invoice_code = 'SEED-INV-401-2026-05-RENT-PAID' LIMIT 1);
        SET @inv401mayUtility := (SELECT invoice_id FROM hdbhms.invoices WHERE invoice_code = 'SEED-INV-401-2026-05-UTILITY-PAID' LIMIT 1);
        SET @inv401junRent := (SELECT invoice_id FROM hdbhms.invoices WHERE invoice_code = 'SEED-INV-401-2026-06-RENT-PAID' LIMIT 1);
        SET @inv401junUtility := (SELECT invoice_id FROM hdbhms.invoices WHERE invoice_code = 'SEED-INV-401-2026-06-UTILITY-PAID' LIMIT 1);
        SET @inv402junRent := (SELECT invoice_id FROM hdbhms.invoices WHERE invoice_code = 'SEED-INV-402-2026-06-RENT-PAID' LIMIT 1);
        SET @inv402junUtility := (SELECT invoice_id FROM hdbhms.invoices WHERE invoice_code = 'SEED-INV-402-2026-06-UTILITY-PAID' LIMIT 1);
        SET @inv404junRent := (SELECT invoice_id FROM hdbhms.invoices WHERE invoice_code = 'SEED-INV-404-2026-06-RENT-PAID' LIMIT 1);
        SET @inv501junRent := (SELECT invoice_id FROM hdbhms.invoices WHERE invoice_code = 'SEED-INV-501-2026-06-RENT-PAID' LIMIT 1);
        SET @inv501junUtility := (SELECT invoice_id FROM hdbhms.invoices WHERE invoice_code = 'SEED-INV-501-2026-06-UTILITY-PAID' LIMIT 1);
        SET @inv302junRent := (SELECT invoice_id FROM hdbhms.invoices WHERE invoice_code = 'SEED-INV-302-2026-06-RENT-OVERDUE' LIMIT 1);
        SET @inv302junUtility := (SELECT invoice_id FROM hdbhms.invoices WHERE invoice_code = 'SEED-INV-302-2026-06-UTILITY-OVERDUE' LIMIT 1);
        SET @inv401rent := (SELECT invoice_id FROM hdbhms.invoices WHERE invoice_code = 'SEED-INV-401-2026-07-RENT-PAID' LIMIT 1);
        SET @inv401utility := (SELECT invoice_id FROM hdbhms.invoices WHERE invoice_code = 'SEED-INV-401-2026-07-UTILITY-PAID' LIMIT 1);
        SET @inv403final := (SELECT invoice_id FROM hdbhms.invoices WHERE invoice_code = 'SEED-INV-403-2026-07-FINAL-ISSUED' LIMIT 1);
        SET @inv503final := (SELECT invoice_id FROM hdbhms.invoices WHERE invoice_code = 'SEED-INV-503-TRANSFER-OUT-ISSUED' LIMIT 1);
        SET @inv505final := (SELECT invoice_id FROM hdbhms.invoices WHERE invoice_code = 'SEED-INV-505-TRANSFER-OUT-PAID' LIMIT 1);
        SET @inv301final := (SELECT invoice_id FROM hdbhms.invoices WHERE invoice_code = 'SEED-INV-301-2026-07-FINAL-PAID' LIMIT 1);

        INSERT INTO hdbhms.meters
            (room_id, meter_type, meter_code, status, installed_at, created_at)
        SELECT r.room_id, meter_def.meter_type, CONCAT('SEED-', meter_def.prefix, '-', r.room_code), 'ACTIVE', '2025-01-01', '2025-01-01 08:00:00'
        FROM hdbhms.rooms r
        JOIN (
            SELECT 'ELECTRICITY' AS meter_type, 'E' AS prefix
            UNION ALL
            SELECT 'WATER', 'W'
        ) meter_def
        WHERE r.room_id IN (@r301, @r302, @r401, @r402, @r403, @r501, @r503, @r505)
          AND NOT EXISTS (
              SELECT 1
              FROM hdbhms.meters existing_meter
              WHERE existing_meter.room_id = r.room_id
                AND existing_meter.meter_type = meter_def.meter_type
                AND existing_meter.status = 'ACTIVE'
          );

        SET @m301e := (SELECT meter_id FROM hdbhms.meters WHERE room_id = @r301 AND meter_type = 'ELECTRICITY' AND status = 'ACTIVE' LIMIT 1);
        SET @m301w := (SELECT meter_id FROM hdbhms.meters WHERE room_id = @r301 AND meter_type = 'WATER' AND status = 'ACTIVE' LIMIT 1);
        SET @m302e := (SELECT meter_id FROM hdbhms.meters WHERE room_id = @r302 AND meter_type = 'ELECTRICITY' AND status = 'ACTIVE' LIMIT 1);
        SET @m302w := (SELECT meter_id FROM hdbhms.meters WHERE room_id = @r302 AND meter_type = 'WATER' AND status = 'ACTIVE' LIMIT 1);
        SET @m401e := (SELECT meter_id FROM hdbhms.meters WHERE room_id = @r401 AND meter_type = 'ELECTRICITY' AND status = 'ACTIVE' LIMIT 1);
        SET @m401w := (SELECT meter_id FROM hdbhms.meters WHERE room_id = @r401 AND meter_type = 'WATER' AND status = 'ACTIVE' LIMIT 1);
        SET @m402e := (SELECT meter_id FROM hdbhms.meters WHERE room_id = @r402 AND meter_type = 'ELECTRICITY' AND status = 'ACTIVE' LIMIT 1);
        SET @m402w := (SELECT meter_id FROM hdbhms.meters WHERE room_id = @r402 AND meter_type = 'WATER' AND status = 'ACTIVE' LIMIT 1);
        SET @m403e := (SELECT meter_id FROM hdbhms.meters WHERE room_id = @r403 AND meter_type = 'ELECTRICITY' AND status = 'ACTIVE' LIMIT 1);
        SET @m403w := (SELECT meter_id FROM hdbhms.meters WHERE room_id = @r403 AND meter_type = 'WATER' AND status = 'ACTIVE' LIMIT 1);
        SET @m501e := (SELECT meter_id FROM hdbhms.meters WHERE room_id = @r501 AND meter_type = 'ELECTRICITY' AND status = 'ACTIVE' LIMIT 1);
        SET @m501w := (SELECT meter_id FROM hdbhms.meters WHERE room_id = @r501 AND meter_type = 'WATER' AND status = 'ACTIVE' LIMIT 1);
        SET @m503e := (SELECT meter_id FROM hdbhms.meters WHERE room_id = @r503 AND meter_type = 'ELECTRICITY' AND status = 'ACTIVE' LIMIT 1);
        SET @m503w := (SELECT meter_id FROM hdbhms.meters WHERE room_id = @r503 AND meter_type = 'WATER' AND status = 'ACTIVE' LIMIT 1);
        SET @m505e := (SELECT meter_id FROM hdbhms.meters WHERE room_id = @r505 AND meter_type = 'ELECTRICITY' AND status = 'ACTIVE' LIMIT 1);
        SET @m505w := (SELECT meter_id FROM hdbhms.meters WHERE room_id = @r505 AND meter_type = 'WATER' AND status = 'ACTIVE' LIMIT 1);

        INSERT INTO hdbhms.meter_readings
            (batch_id, meter_id, room_id, reading_period, revision_no, previous_value, current_value, reading_date, photo_file_id, status, void_reason, created_by, created_at, purpose, source, review_status, review_count)
        VALUES
            (NULL, @m401e, @r401, '2026-05', 1, 4010, 4080, '2026-05-31', NULL, 'CONFIRMED', NULL, @manager_id, '2026-05-31 08:00:00', 'MONTHLY', 'MANUAL', 'NONE', 0),
            (NULL, @m401w, @r401, '2026-05', 1, 401, 406, '2026-05-31', NULL, 'CONFIRMED', NULL, @manager_id, '2026-05-31 08:00:00', 'MONTHLY', 'MANUAL', 'NONE', 0),
            (NULL, @m401e, @r401, '2026-06', 1, 4080, 4150, '2026-06-30', NULL, 'CONFIRMED', NULL, @manager_id, '2026-06-30 08:00:00', 'MONTHLY', 'MANUAL', 'NONE', 0),
            (NULL, @m401w, @r401, '2026-06', 1, 406, 411, '2026-06-30', NULL, 'CONFIRMED', NULL, @manager_id, '2026-06-30 08:00:00', 'MONTHLY', 'MANUAL', 'NONE', 0),
            (NULL, @m401e, @r401, '2026-07', 1, 4150, 4230, '2026-07-31', NULL, 'CONFIRMED', NULL, @manager_id, '2026-07-31 08:00:00', 'MONTHLY', 'MANUAL', 'NONE', 0),
            (NULL, @m401w, @r401, '2026-07', 1, 411, 415, '2026-07-31', NULL, 'CONFIRMED', NULL, @manager_id, '2026-07-31 08:00:00', 'MONTHLY', 'MANUAL', 'NONE', 0),
            (NULL, @m402e, @r402, '2026-06', 1, 4020, 4080, '2026-06-30', NULL, 'CONFIRMED', NULL, @manager_id, '2026-06-30 08:00:00', 'MONTHLY', 'MANUAL', 'NONE', 0),
            (NULL, @m402w, @r402, '2026-06', 1, 402, 405, '2026-06-30', NULL, 'CONFIRMED', NULL, @manager_id, '2026-06-30 08:00:00', 'MONTHLY', 'MANUAL', 'NONE', 0),
            (NULL, @m501e, @r501, '2026-06', 1, 5010, 5090, '2026-06-30', NULL, 'CONFIRMED', NULL, @manager_id, '2026-06-30 08:00:00', 'MONTHLY', 'MANUAL', 'NONE', 0),
            (NULL, @m501w, @r501, '2026-06', 1, 501, 506, '2026-06-30', NULL, 'CONFIRMED', NULL, @manager_id, '2026-06-30 08:00:00', 'MONTHLY', 'MANUAL', 'NONE', 0),
            (NULL, @m302e, @r302, '2026-06', 1, 3020, 3090, '2026-06-30', NULL, 'CONFIRMED', NULL, @manager_id, '2026-06-30 08:00:00', 'MONTHLY', 'MANUAL', 'NONE', 0),
            (NULL, @m302w, @r302, '2026-06', 1, 302, 307, '2026-06-30', NULL, 'CONFIRMED', NULL, @manager_id, '2026-06-30 08:00:00', 'MONTHLY', 'MANUAL', 'NONE', 0),
            (NULL, @m403e, @r403, '2026-07', 1, 4030, 4130, '2026-07-30', NULL, 'CONFIRMED', NULL, @manager_id, '2026-07-30 08:00:00', 'MOVE_OUT', 'MANUAL', 'NONE', 0),
            (NULL, @m403w, @r403, '2026-07', 1, 403, 408, '2026-07-30', NULL, 'CONFIRMED', NULL, @manager_id, '2026-07-30 08:00:00', 'MOVE_OUT', 'MANUAL', 'NONE', 0),
            (NULL, @m503e, @r503, '2026-07', 1, 5030, 5120, '2026-07-30', NULL, 'CONFIRMED', NULL, @manager_id, '2026-07-30 11:00:00', 'TRANSFER', 'MANUAL', 'NONE', 0),
            (NULL, @m503w, @r503, '2026-07', 1, 503, 509, '2026-07-30', NULL, 'CONFIRMED', NULL, @manager_id, '2026-07-30 11:00:00', 'TRANSFER', 'MANUAL', 'NONE', 0),
            (NULL, @m505e, @r505, '2026-07', 1, 5050, 5140, '2026-07-20', NULL, 'CONFIRMED', NULL, @manager_id, '2026-07-20 11:00:00', 'TRANSFER', 'MANUAL', 'NONE', 0),
            (NULL, @m505w, @r505, '2026-07', 1, 505, 509, '2026-07-20', NULL, 'CONFIRMED', NULL, @manager_id, '2026-07-20 11:00:00', 'TRANSFER', 'MANUAL', 'NONE', 0),
            (NULL, @m301e, @r301, '2026-07', 1, 3010, 3110, '2026-07-25', NULL, 'CONFIRMED', NULL, @manager_id, '2026-07-25 09:30:00', 'MOVE_OUT', 'MANUAL', 'NONE', 0),
            (NULL, @m301w, @r301, '2026-07', 1, 301, 306, '2026-07-25', NULL, 'CONFIRMED', NULL, @manager_id, '2026-07-25 09:30:00', 'MOVE_OUT', 'MANUAL', 'NONE', 0);

        SET @mr301eJul := (SELECT meter_reading_id FROM hdbhms.meter_readings WHERE meter_id = @m301e AND reading_period = '2026-07' AND status = 'CONFIRMED' LIMIT 1);
        SET @mr301wJul := (SELECT meter_reading_id FROM hdbhms.meter_readings WHERE meter_id = @m301w AND reading_period = '2026-07' AND status = 'CONFIRMED' LIMIT 1);
        SET @mr302eJun := (SELECT meter_reading_id FROM hdbhms.meter_readings WHERE meter_id = @m302e AND reading_period = '2026-06' AND status = 'CONFIRMED' LIMIT 1);
        SET @mr302wJun := (SELECT meter_reading_id FROM hdbhms.meter_readings WHERE meter_id = @m302w AND reading_period = '2026-06' AND status = 'CONFIRMED' LIMIT 1);
        SET @mr401eMay := (SELECT meter_reading_id FROM hdbhms.meter_readings WHERE meter_id = @m401e AND reading_period = '2026-05' AND status = 'CONFIRMED' LIMIT 1);
        SET @mr401wMay := (SELECT meter_reading_id FROM hdbhms.meter_readings WHERE meter_id = @m401w AND reading_period = '2026-05' AND status = 'CONFIRMED' LIMIT 1);
        SET @mr401eJun := (SELECT meter_reading_id FROM hdbhms.meter_readings WHERE meter_id = @m401e AND reading_period = '2026-06' AND status = 'CONFIRMED' LIMIT 1);
        SET @mr401wJun := (SELECT meter_reading_id FROM hdbhms.meter_readings WHERE meter_id = @m401w AND reading_period = '2026-06' AND status = 'CONFIRMED' LIMIT 1);
        SET @mr401eJul := (SELECT meter_reading_id FROM hdbhms.meter_readings WHERE meter_id = @m401e AND reading_period = '2026-07' AND status = 'CONFIRMED' LIMIT 1);
        SET @mr401wJul := (SELECT meter_reading_id FROM hdbhms.meter_readings WHERE meter_id = @m401w AND reading_period = '2026-07' AND status = 'CONFIRMED' LIMIT 1);
        SET @mr402eJun := (SELECT meter_reading_id FROM hdbhms.meter_readings WHERE meter_id = @m402e AND reading_period = '2026-06' AND status = 'CONFIRMED' LIMIT 1);
        SET @mr402wJun := (SELECT meter_reading_id FROM hdbhms.meter_readings WHERE meter_id = @m402w AND reading_period = '2026-06' AND status = 'CONFIRMED' LIMIT 1);
        SET @mr403eJul := (SELECT meter_reading_id FROM hdbhms.meter_readings WHERE meter_id = @m403e AND reading_period = '2026-07' AND status = 'CONFIRMED' LIMIT 1);
        SET @mr403wJul := (SELECT meter_reading_id FROM hdbhms.meter_readings WHERE meter_id = @m403w AND reading_period = '2026-07' AND status = 'CONFIRMED' LIMIT 1);
        SET @mr501eJun := (SELECT meter_reading_id FROM hdbhms.meter_readings WHERE meter_id = @m501e AND reading_period = '2026-06' AND status = 'CONFIRMED' LIMIT 1);
        SET @mr501wJun := (SELECT meter_reading_id FROM hdbhms.meter_readings WHERE meter_id = @m501w AND reading_period = '2026-06' AND status = 'CONFIRMED' LIMIT 1);
        SET @mr503eJul := (SELECT meter_reading_id FROM hdbhms.meter_readings WHERE meter_id = @m503e AND reading_period = '2026-07' AND status = 'CONFIRMED' LIMIT 1);
        SET @mr503wJul := (SELECT meter_reading_id FROM hdbhms.meter_readings WHERE meter_id = @m503w AND reading_period = '2026-07' AND status = 'CONFIRMED' LIMIT 1);
        SET @mr505eJul := (SELECT meter_reading_id FROM hdbhms.meter_readings WHERE meter_id = @m505e AND reading_period = '2026-07' AND status = 'CONFIRMED' LIMIT 1);
        SET @mr505wJul := (SELECT meter_reading_id FROM hdbhms.meter_readings WHERE meter_id = @m505w AND reading_period = '2026-07' AND status = 'CONFIRMED' LIMIT 1);

        INSERT INTO hdbhms.invoice_lines
            (invoice_id, line_type, description, quantity, unit_price, meter_reading_id, source_type, source_id, collection_account_id, created_at)
        VALUES
            (@inv401mayRent, 'ROOM_RENT', 'Tien phong 401 thang 05/2026', 1, 2400000, NULL, 'SEED_DEMO', @c401, @rent_account, '2026-05-01 08:00:00'),
            (@inv401mayUtility, 'ELECTRICITY', 'Dien phong 401 thang 05/2026', 70, 3500, @mr401eMay, 'SEED_DEMO', @c401, @utility_account, '2026-05-01 08:00:00'),
            (@inv401mayUtility, 'WATER', 'Nuoc phong 401 thang 05/2026', 5, 15000, @mr401wMay, 'SEED_DEMO', @c401, @utility_account, '2026-05-01 08:00:00'),
            (@inv401junRent, 'ROOM_RENT', 'Tien phong 401 thang 06/2026', 1, 2400000, NULL, 'SEED_DEMO', @c401, @rent_account, '2026-06-01 08:00:00'),
            (@inv401junUtility, 'ELECTRICITY', 'Dien phong 401 thang 06/2026', 70, 3500, @mr401eJun, 'SEED_DEMO', @c401, @utility_account, '2026-06-01 08:00:00'),
            (@inv401junUtility, 'WATER', 'Nuoc phong 401 thang 06/2026', 5, 20000, @mr401wJun, 'SEED_DEMO', @c401, @utility_account, '2026-06-01 08:00:00'),
            (@inv402junRent, 'ROOM_RENT', 'Tien phong 402 thang 06/2026', 1, 2500000, NULL, 'SEED_DEMO', @c402, @rent_account, '2026-06-01 08:00:00'),
            (@inv402junUtility, 'ELECTRICITY', 'Dien phong 402 thang 06/2026', 60, 3500, @mr402eJun, 'SEED_DEMO', @c402, @utility_account, '2026-06-01 08:00:00'),
            (@inv402junUtility, 'WATER', 'Nuoc phong 402 thang 06/2026', 3, 30000, @mr402wJun, 'SEED_DEMO', @c402, @utility_account, '2026-06-01 08:00:00'),
            (@inv404junRent, 'ROOM_RENT', 'Tien phong 404 thang 06/2026', 1, 2600000, NULL, 'SEED_DEMO', @c404, @rent_account, '2026-06-01 08:00:00'),
            (@inv501junRent, 'ROOM_RENT', 'Tien phong 501 thang 06/2026', 1, 2500000, NULL, 'SEED_DEMO', @c501, @rent_account, '2026-06-01 08:00:00'),
            (@inv501junUtility, 'ELECTRICITY', 'Dien phong 501 thang 06/2026', 80, 3500, @mr501eJun, 'SEED_DEMO', @c501, @utility_account, '2026-06-01 08:00:00'),
            (@inv501junUtility, 'WATER', 'Nuoc phong 501 thang 06/2026', 5, 10000, @mr501wJun, 'SEED_DEMO', @c501, @utility_account, '2026-06-01 08:00:00'),
            (@inv302junRent, 'ROOM_RENT', 'No tien phong 302 thang 06/2026', 1, 2350000, NULL, 'SEED_DEMO_DEBT', @c302, @rent_account, '2026-06-01 08:00:00'),
            (@inv302junUtility, 'ELECTRICITY', 'No dien phong 302 thang 06/2026', 70, 3500, @mr302eJun, 'SEED_DEMO_DEBT', @c302, @utility_account, '2026-06-01 08:00:00'),
            (@inv302junUtility, 'WATER', 'No nuoc phong 302 thang 06/2026', 5, 13000, @mr302wJun, 'SEED_DEMO_DEBT', @c302, @utility_account, '2026-06-01 08:00:00'),
            (@inv401rent, 'ROOM_RENT', 'Tien phong 401 thang 07/2026', 1, 2400000, NULL, 'SEED_DEMO', @c401, @rent_account, '2026-07-01 08:00:00'),
            (@inv401utility, 'ELECTRICITY', 'Dien phong 401 thang 07/2026', 80, 3500, @mr401eJul, 'SEED_DEMO', @c401, @utility_account, '2026-07-01 08:00:00'),
            (@inv401utility, 'WATER', 'Nuoc phong 401 thang 07/2026', 4, 20000, @mr401wJul, 'SEED_DEMO', @c401, @utility_account, '2026-07-01 08:00:00'),
            (@inv403final, 'ROOM_RENT', 'Tien phong con lai khi thanh ly phong 403', 1, 1200000, NULL, 'CONTRACT_LIQUIDATION', @c403, @rent_account, '2026-07-30 08:00:00'),
            (@inv403final, 'ELECTRICITY', 'Dien phong 403 khi thanh ly', 100, 3500, @mr403eJul, 'CONTRACT_LIQUIDATION', @c403, @utility_account, '2026-07-30 08:00:00'),
            (@inv403final, 'WATER', 'Nuoc phong 403 khi thanh ly', 5, 20000, @mr403wJul, 'CONTRACT_LIQUIDATION', @c403, @utility_account, '2026-07-30 08:00:00'),
            (@inv403final, 'MAINTENANCE_COMPENSATION', 'Den bu tai san phong 403', 1, 200000, NULL, 'CONTRACT_LIQUIDATION', @c403, @rent_account, '2026-07-30 08:00:00'),
            (@inv503final, 'ELECTRICITY', 'Dien chot phong 503 khi chuyen phong', 90, 4000, @mr503eJul, 'ROOM_TRANSFER', @c503, @utility_account, '2026-07-30 11:00:00'),
            (@inv503final, 'WATER', 'Nuoc chot phong 503 khi chuyen phong', 6, 20000, @mr503wJul, 'ROOM_TRANSFER', @c503, @utility_account, '2026-07-30 11:00:00'),
            (@inv505final, 'ELECTRICITY', 'Dien chot phong 505 khi chuyen phong', 90, 3000, @mr505eJul, 'ROOM_TRANSFER', @c505, @utility_account, '2026-07-20 11:00:00'),
            (@inv505final, 'WATER', 'Nuoc chot phong 505 khi chuyen phong', 4, 20000, @mr505wJul, 'ROOM_TRANSFER', @c505, @utility_account, '2026-07-20 11:00:00'),
            (@inv301final, 'ELECTRICITY', 'Dien chot phong 301', 100, 3500, @mr301eJul, 'CONTRACT_LIQUIDATION', @c301, @utility_account, '2026-07-25 09:30:00'),
            (@inv301final, 'WATER', 'Nuoc chot phong 301', 5, 20000, @mr301wJul, 'CONTRACT_LIQUIDATION', @c301, @utility_account, '2026-07-25 09:30:00'),
            (@inv301final, 'MAINTENANCE_COMPENSATION', 'Phi ve sinh phong 301', 1, 300000, NULL, 'CONTRACT_LIQUIDATION', @c301, @rent_account, '2026-07-25 09:30:00');

        INSERT INTO hdbhms.payment_transactions
            (provider, provider_transaction_id, collection_account_id, amount, transaction_time, payer_name, payer_account, content, status, raw_payload, confirmed_by, confirmed_at, created_at)
        VALUES
            ('BANK', 'SEED-TXN-401-RENT-202605', @rent_account, 2400000, '2026-05-03 09:00:00', 'Nguyen Van Tenant Demo', 'SEED-PAYER-401', 'SEED-INV-401-2026-05-RENT-PAID', 'MATCHED', CAST(JSON_OBJECT('seed', 'demo') AS BINARY), @manager_id, '2026-05-03 09:05:00', '2026-05-03 09:00:00'),
            ('BANK', 'SEED-TXN-401-UTILITY-202605', @utility_account, 320000, '2026-05-03 09:10:00', 'Nguyen Van Tenant Demo', 'SEED-PAYER-401', 'SEED-INV-401-2026-05-UTILITY-PAID', 'MATCHED', CAST(JSON_OBJECT('seed', 'demo') AS BINARY), @manager_id, '2026-05-03 09:15:00', '2026-05-03 09:10:00'),
            ('BANK', 'SEED-TXN-401-RENT-202606', @rent_account, 2400000, '2026-06-03 09:00:00', 'Nguyen Van Tenant Demo', 'SEED-PAYER-401', 'SEED-INV-401-2026-06-RENT-PAID', 'MATCHED', CAST(JSON_OBJECT('seed', 'demo') AS BINARY), @manager_id, '2026-06-03 09:05:00', '2026-06-03 09:00:00'),
            ('BANK', 'SEED-TXN-401-UTILITY-202606', @utility_account, 345000, '2026-06-03 09:10:00', 'Nguyen Van Tenant Demo', 'SEED-PAYER-401', 'SEED-INV-401-2026-06-UTILITY-PAID', 'MATCHED', CAST(JSON_OBJECT('seed', 'demo') AS BINARY), @manager_id, '2026-06-03 09:15:00', '2026-06-03 09:10:00'),
            ('BANK', 'SEED-TXN-402-RENT-202606', @rent_account, 2500000, '2026-06-04 09:00:00', 'Nguyen Van Tenant Demo', 'SEED-PAYER-402', 'SEED-INV-402-2026-06-RENT-PAID', 'MATCHED', CAST(JSON_OBJECT('seed', 'demo') AS BINARY), @manager_id, '2026-06-04 09:05:00', '2026-06-04 09:00:00'),
            ('BANK', 'SEED-TXN-402-UTILITY-202606', @utility_account, 300000, '2026-06-04 09:10:00', 'Nguyen Van Tenant Demo', 'SEED-PAYER-402', 'SEED-INV-402-2026-06-UTILITY-PAID', 'MATCHED', CAST(JSON_OBJECT('seed', 'demo') AS BINARY), @manager_id, '2026-06-04 09:15:00', '2026-06-04 09:10:00'),
            ('BANK', 'SEED-TXN-404-RENT-202606', @rent_account, 2600000, '2026-06-04 10:00:00', 'Nguyen Van Tenant Demo', 'SEED-PAYER-404', 'SEED-INV-404-2026-06-RENT-PAID', 'MATCHED', CAST(JSON_OBJECT('seed', 'demo') AS BINARY), @manager_id, '2026-06-04 10:05:00', '2026-06-04 10:00:00'),
            ('BANK', 'SEED-TXN-501-RENT-202606', @rent_account, 2500000, '2026-06-05 08:30:00', 'Nguyen Van Tenant Demo', 'SEED-PAYER-501', 'SEED-INV-501-2026-06-RENT-PAID', 'MATCHED', CAST(JSON_OBJECT('seed', 'demo') AS BINARY), @manager_id, '2026-06-05 08:35:00', '2026-06-05 08:30:00'),
            ('BANK', 'SEED-TXN-501-UTILITY-202606', @utility_account, 330000, '2026-06-05 08:40:00', 'Nguyen Van Tenant Demo', 'SEED-PAYER-501', 'SEED-INV-501-2026-06-UTILITY-PAID', 'MATCHED', CAST(JSON_OBJECT('seed', 'demo') AS BINARY), @manager_id, '2026-06-05 08:45:00', '2026-06-05 08:40:00'),
            ('BANK', 'SEED-TXN-401-RENT-202607', @rent_account, 2400000, '2026-07-02 09:00:00', 'Nguyễn Văn Tenant Demo', 'SEED-PAYER-401', 'SEED-INV-401-2026-07-RENT-PAID', 'MATCHED', CAST(JSON_OBJECT('seed', 'demo') AS BINARY), @manager_id, '2026-07-02 09:05:00', '2026-07-02 09:00:00'),
            ('BANK', 'SEED-TXN-401-UTILITY-202607', @utility_account, 360000, '2026-07-02 09:10:00', 'Nguyễn Văn Tenant Demo', 'SEED-PAYER-401', 'SEED-INV-401-2026-07-UTILITY-PAID', 'MATCHED', CAST(JSON_OBJECT('seed', 'demo') AS BINARY), @manager_id, '2026-07-02 09:15:00', '2026-07-02 09:10:00'),
            ('BANK', 'SEED-TXN-505-FINAL-202607', @utility_account, 350000, '2026-07-20 12:00:00', 'Bùi Văn Seed 505', 'SEED-PAYER-505', 'SEED-INV-505-TRANSFER-OUT-PAID', 'MATCHED', CAST(JSON_OBJECT('seed', 'demo') AS BINARY), @manager_id, '2026-07-20 12:05:00', '2026-07-20 12:00:00'),
            ('BANK', 'SEED-TXN-301-FINAL-202607', @rent_account, 750000, '2026-07-25 12:00:00', 'Lê Thị Seed 301', 'SEED-PAYER-301', 'SEED-INV-301-2026-07-FINAL-PAID', 'MATCHED', CAST(JSON_OBJECT('seed', 'demo') AS BINARY), @manager_id, '2026-07-25 12:05:00', '2026-07-25 12:00:00');

        SET @txn401mayRent := (SELECT payment_transaction_id FROM hdbhms.payment_transactions WHERE provider_transaction_id = 'SEED-TXN-401-RENT-202605' LIMIT 1);
        SET @txn401mayUtility := (SELECT payment_transaction_id FROM hdbhms.payment_transactions WHERE provider_transaction_id = 'SEED-TXN-401-UTILITY-202605' LIMIT 1);
        SET @txn401junRent := (SELECT payment_transaction_id FROM hdbhms.payment_transactions WHERE provider_transaction_id = 'SEED-TXN-401-RENT-202606' LIMIT 1);
        SET @txn401junUtility := (SELECT payment_transaction_id FROM hdbhms.payment_transactions WHERE provider_transaction_id = 'SEED-TXN-401-UTILITY-202606' LIMIT 1);
        SET @txn402junRent := (SELECT payment_transaction_id FROM hdbhms.payment_transactions WHERE provider_transaction_id = 'SEED-TXN-402-RENT-202606' LIMIT 1);
        SET @txn402junUtility := (SELECT payment_transaction_id FROM hdbhms.payment_transactions WHERE provider_transaction_id = 'SEED-TXN-402-UTILITY-202606' LIMIT 1);
        SET @txn404junRent := (SELECT payment_transaction_id FROM hdbhms.payment_transactions WHERE provider_transaction_id = 'SEED-TXN-404-RENT-202606' LIMIT 1);
        SET @txn501junRent := (SELECT payment_transaction_id FROM hdbhms.payment_transactions WHERE provider_transaction_id = 'SEED-TXN-501-RENT-202606' LIMIT 1);
        SET @txn501junUtility := (SELECT payment_transaction_id FROM hdbhms.payment_transactions WHERE provider_transaction_id = 'SEED-TXN-501-UTILITY-202606' LIMIT 1);
        SET @txn401rent := (SELECT payment_transaction_id FROM hdbhms.payment_transactions WHERE provider_transaction_id = 'SEED-TXN-401-RENT-202607' LIMIT 1);
        SET @txn401utility := (SELECT payment_transaction_id FROM hdbhms.payment_transactions WHERE provider_transaction_id = 'SEED-TXN-401-UTILITY-202607' LIMIT 1);
        SET @txn505final := (SELECT payment_transaction_id FROM hdbhms.payment_transactions WHERE provider_transaction_id = 'SEED-TXN-505-FINAL-202607' LIMIT 1);
        SET @txn301final := (SELECT payment_transaction_id FROM hdbhms.payment_transactions WHERE provider_transaction_id = 'SEED-TXN-301-FINAL-202607' LIMIT 1);

        INSERT INTO hdbhms.payment_allocations
            (payment_transaction_id, invoice_id, amount, allocated_by, allocated_at)
        VALUES
            (@txn401mayRent, @inv401mayRent, 2400000, @manager_id, '2026-05-03 09:05:00'),
            (@txn401mayUtility, @inv401mayUtility, 320000, @manager_id, '2026-05-03 09:15:00'),
            (@txn401junRent, @inv401junRent, 2400000, @manager_id, '2026-06-03 09:05:00'),
            (@txn401junUtility, @inv401junUtility, 345000, @manager_id, '2026-06-03 09:15:00'),
            (@txn402junRent, @inv402junRent, 2500000, @manager_id, '2026-06-04 09:05:00'),
            (@txn402junUtility, @inv402junUtility, 300000, @manager_id, '2026-06-04 09:15:00'),
            (@txn404junRent, @inv404junRent, 2600000, @manager_id, '2026-06-04 10:05:00'),
            (@txn501junRent, @inv501junRent, 2500000, @manager_id, '2026-06-05 08:35:00'),
            (@txn501junUtility, @inv501junUtility, 330000, @manager_id, '2026-06-05 08:45:00'),
            (@txn401rent, @inv401rent, 2400000, @manager_id, '2026-07-02 09:05:00'),
            (@txn401utility, @inv401utility, 360000, @manager_id, '2026-07-02 09:15:00'),
            (@txn505final, @inv505final, 350000, @manager_id, '2026-07-20 12:05:00'),
            (@txn301final, @inv301final, 750000, @manager_id, '2026-07-25 12:05:00');

        INSERT INTO hdbhms.debt_snapshots
            (room_id, contract_id, snapshot_date, rent_debt_amount, utility_debt_amount, other_debt_amount, rent_debt_months, utility_debt_months, mixed_debt_amount, debt_limit_amount, is_over_limit, created_at)
        VALUES
            (@r403, @c403, '2026-07-30', 1200000, 450000, 200000, 1, 1, 1850000, 1533333, TRUE, '2026-07-30 08:00:00'),
            (@r302, @c302, '2026-07-30', 2350000, 310000, 0, 1, 1, 2660000, 1566666, TRUE, '2026-07-30 08:00:00'),
            (@r405, @c405, '2026-07-30', 0, 0, 0, 0, 0, 0, 1466666, FALSE, '2026-07-30 08:00:00'),
            (@r501, @c501, '2026-07-30', 0, 0, 0, 0, 0, 0, 1666666, FALSE, '2026-07-30 08:00:00'),
            (@r503, @c503, '2026-07-30', 0, 480000, 0, 0, 1, 480000, 1533333, FALSE, '2026-07-30 08:00:00'),
            (@r505, @c505, '2026-07-20', 0, 350000, 0, 0, 1, 350000, 1466666, FALSE, '2026-07-20 08:00:00');
        SET @debt302 := (SELECT debt_snapshot_id FROM hdbhms.debt_snapshots WHERE room_id = @r302 AND snapshot_date = '2026-07-30' LIMIT 1);
        SET @debt405 := (SELECT debt_snapshot_id FROM hdbhms.debt_snapshots WHERE room_id = @r405 AND snapshot_date = '2026-07-30' LIMIT 1);
        SET @debt501 := (SELECT debt_snapshot_id FROM hdbhms.debt_snapshots WHERE room_id = @r501 AND snapshot_date = '2026-07-30' LIMIT 1);
        SET @debt503 := (SELECT debt_snapshot_id FROM hdbhms.debt_snapshots WHERE room_id = @r503 AND snapshot_date = '2026-07-30' LIMIT 1);
        SET @debt505 := (SELECT debt_snapshot_id FROM hdbhms.debt_snapshots WHERE room_id = @r505 AND snapshot_date = '2026-07-20' LIMIT 1);

        INSERT INTO hdbhms.change_requests
            (request_code, request_type, requester_id, requester_role, target_type, target_id, title, description, request_payload, evidence_file_id, assigned_role, assigned_to, status, resolution_note, resolved_by, resolved_at, created_at, updated_at)
        VALUES
            ('TLHD_P403_30_07_2026', 'CONTRACT_LIQUIDATION', @tenant_demo_user_id, 'TENANT', 'CONTRACT', @c403, 'Thanh lý hợp đồng phòng 403', 'Khách yêu cầu thanh lý và tạo hóa đơn quyết toán cuối cùng.', JSON_OBJECT('contractId', @c403, 'roomCode', '403', 'finalInvoiceId', @inv403final, 'liquidationDate', '2026-07-31', 'liquidationStage', 'WAITING_PAYMENT'), NULL, 'OWNER', @owner_id, 'PROCESSING', NULL, @owner_id, '2026-07-30 09:30:00', '2026-07-30 08:00:00', '2026-07-30 09:30:00'),
            ('GHHD_P404_30_07_2026', 'CONTRACT_RENEWAL', @tenant_demo_user_id, 'TENANT', 'CONTRACT', @c404, 'Gia hạn hợp đồng phòng 404', 'Khách muốn gia hạn thêm 12 tháng.', JSON_OBJECT('contractId', @c404, 'roomCode', '404', 'oldEndDate', '2026-08-31', 'newStartDate', '2026-09-01', 'newEndDate', '2027-08-31', 'monthlyRent', 2700000), NULL, 'OWNER', @owner_id, 'PENDING', NULL, NULL, NULL, '2026-07-30 08:05:00', '2026-07-30 08:05:00'),
            ('CP_P406_30_07_2026', 'ROOM_TRANSFER', @tenant_demo_user_id, 'TENANT', 'CONTRACT', NULL, 'Chuyển phòng 405 sang 406 cùng người ở cùng', 'Người đứng tên chọn người ở cùng trong phòng 405 chuyển cùng sang phòng trống 406.', JSON_OBJECT('oldContractId', @c405, 'oldRoomCode', '405', 'targetRoomCode', '406', 'transferringProfileIds', JSON_ARRAY(@p401, @p405_co), 'selectedCoOccupantProfileIds', JSON_ARRAY(@p405_co), 'sourceRoomWillBeEmpty', TRUE), NULL, 'MANAGER', @manager_id, 'PENDING', NULL, NULL, NULL, '2026-07-30 08:10:00', '2026-07-30 08:10:00'),
            ('CP_P502_30_07_2026', 'ROOM_TRANSFER', @tenant_demo_user_id, 'TENANT', 'CONTRACT', NULL, 'Chuyển phòng 501 sang 502', 'Đã xác nhận hợp đồng mới, đang chờ upload/ký hợp đồng.', JSON_OBJECT('oldContractId', @c501, 'oldRoomCode', '501', 'targetRoomCode', '502', 'newContractId', @c502new), NULL, 'MANAGER', @manager_id, 'PROCESSING', NULL, @manager_id, '2026-07-30 10:00:00', '2026-07-30 08:15:00', '2026-07-30 10:00:00'),
            ('CP_P504_30_07_2026', 'ROOM_TRANSFER', @tenant_demo_user_id, 'TENANT', 'CONTRACT', NULL, 'Chuyển phòng 503 sang 504', 'Đã bàn giao phòng cũ, còn hóa đơn điện nước chốt phòng chưa thanh toán.', JSON_OBJECT('oldContractId', @c503, 'oldRoomCode', '503', 'targetRoomCode', '504', 'newContractId', @c504new, 'oldRoomFinalInvoiceId', @inv503final), NULL, 'MANAGER', @manager_id, 'PROCESSING', NULL, @manager_id, '2026-07-30 11:30:00', '2026-07-30 08:20:00', '2026-07-30 11:30:00'),
            ('CP_P506_20_07_2026', 'ROOM_TRANSFER', @tenant_demo_user_id, 'TENANT', 'CONTRACT', NULL, 'Chuyển phòng 505 sang 506', 'Yêu cầu chuyển phòng đã hoàn tất.', JSON_OBJECT('oldContractId', @c505, 'oldRoomCode', '505', 'targetRoomCode', '506', 'newContractId', @c506new, 'oldRoomFinalInvoiceId', @inv505final), NULL, 'MANAGER', @manager_id, 'COMPLETED', 'Đã hoàn tất chuyển phòng.', @manager_id, '2026-07-20 12:30:00', '2026-07-20 08:00:00', '2026-07-20 12:30:00'),
            ('TLHD_P507_30_07_2026', 'CONTRACT_LIQUIDATION', @tenant_demo_user_id, 'TENANT', 'CONTRACT', @c507, 'Thanh lý hợp đồng phòng 507 có người ở lại', 'Người đứng tên muốn rời đi, người ở cùng muốn tiếp tục thuê và cần lập hợp đồng thay thế.', JSON_OBJECT('contractId', @c507, 'roomCode', '507', 'liquidationDate', '2026-08-05', 'liquidationMode', 'PRIMARY_LEAVES_CO_OCCUPANT_STAYS', 'leavingProfileIds', JSON_ARRAY(@p401), 'stayingProfileIds', JSON_ARRAY(@p507_stay), 'replacementPrimaryTenantProfileId', @p507_stay, 'requiresReplacementContract', TRUE, 'roomWillRemainOccupied', TRUE, 'reason', 'Người đứng tên rời đi, người ở cùng muốn ở lại.'), NULL, 'OWNER', @owner_id, 'PENDING', NULL, NULL, NULL, '2026-07-30 08:35:00', '2026-07-30 08:35:00'),
            ('TLHD_P301_25_07_2026', 'CONTRACT_LIQUIDATION', @tenant_demo_user_id, 'TENANT', 'CONTRACT', @c301, 'Thanh lý hợp đồng phòng 301', 'Thanh lý đã hoàn tất, hóa đơn tất toán đã thanh toán.', JSON_OBJECT('contractId', @c301, 'roomCode', '301', 'liquidationDate', '2026-07-25', 'finalInvoiceId', @inv301final, 'finalInvoicePaid', true, 'liquidationStage', 'CONFIRMED', 'depositRefundStatus', 'NOT_REQUIRED', 'liquidationChecklist', JSON_OBJECT('handoverConfirmed', true, 'finalInvoicePaid', true, 'depositRefundConfirmed', true, 'signedDocumentUploaded', true, 'canConfirm', true)), NULL, 'OWNER', @owner_id, 'COMPLETED', 'Đã hoàn tất thanh lý hợp đồng.', @owner_id, '2026-07-25 12:30:00', '2026-07-25 08:30:00', '2026-07-25 12:30:00'),
            ('TLHD_P302_30_07_2026', 'CONTRACT_LIQUIDATION', @tenant_demo_user_id, 'TENANT', 'CONTRACT', @c302, 'Thanh lý hợp đồng phòng 302', 'Yêu cầu thanh lý bị từ chối vì chưa đủ điều kiện.', JSON_OBJECT('contractId', @c302, 'roomCode', '302', 'liquidationDate', '2026-08-01', 'reason', 'Khách chưa hoàn tất công nợ.'), NULL, 'OWNER', @owner_id, 'REJECTED', 'Khách chưa hoàn tất công nợ, chưa thể thanh lý.', @owner_id, '2026-07-30 09:30:00', '2026-07-30 08:40:00', '2026-07-30 09:30:00'),
            ('XHS_P401_30_07_2026', 'TENANT_PROFILE_ACCESS', @manager_id, 'MANAGER', 'TENANT_PROFILE', @p401, 'Xin xem hồ sơ khách phòng 401', 'Seed quyền xem hồ sơ đã được duyệt cho báo cáo công an và demo hồ sơ nhạy cảm.', JSON_OBJECT('roomCode', '401', 'propertyId', @property_id), NULL, 'OWNER', @owner_id, 'APPROVED', 'Cho phép xem hồ sơ trong 30 ngày.', @owner_id, '2026-07-30 09:00:00', '2026-07-30 08:15:00', '2026-07-30 09:00:00');

        SET @cr403 := (SELECT change_request_id FROM hdbhms.change_requests WHERE request_code = 'TLHD_P403_30_07_2026' LIMIT 1);
        SET @cr404 := (SELECT change_request_id FROM hdbhms.change_requests WHERE request_code = 'GHHD_P404_30_07_2026' LIMIT 1);
        SET @cr406 := (SELECT change_request_id FROM hdbhms.change_requests WHERE request_code = 'CP_P406_30_07_2026' LIMIT 1);
        SET @cr502 := (SELECT change_request_id FROM hdbhms.change_requests WHERE request_code = 'CP_P502_30_07_2026' LIMIT 1);
        SET @cr504 := (SELECT change_request_id FROM hdbhms.change_requests WHERE request_code = 'CP_P504_30_07_2026' LIMIT 1);
        SET @cr506 := (SELECT change_request_id FROM hdbhms.change_requests WHERE request_code = 'CP_P506_20_07_2026' LIMIT 1);
        SET @cr507 := (SELECT change_request_id FROM hdbhms.change_requests WHERE request_code = 'TLHD_P507_30_07_2026' LIMIT 1);
        SET @cr301 := (SELECT change_request_id FROM hdbhms.change_requests WHERE request_code = 'TLHD_P301_25_07_2026' LIMIT 1);
        SET @cr302 := (SELECT change_request_id FROM hdbhms.change_requests WHERE request_code = 'TLHD_P302_30_07_2026' LIMIT 1);
        SET @crAccess := (SELECT change_request_id FROM hdbhms.change_requests WHERE request_code = 'XHS_P401_30_07_2026' LIMIT 1);

        INSERT INTO hdbhms.room_transfer_requests
            (request_code, requester_id, old_contract_id, old_room_id, target_room_id, transferring_tenant_profile_ids, nominated_holder_profile_id, target_transfer_type, target_contract_id, requested_transfer_date, reason, reserved_slots, reservation_expires_at, target_holder_approved_by, target_holder_approved_at, target_holder_rejected_at, status, positive_difference_settlement_type, debt_snapshot_id, eligibility_checked_at, is_eligible_at_creation, eligibility_snapshot, violation_snapshot, transfer_history_snapshot, approved_by, approved_at, executed_at, completed_at, new_contract_id, replacement_old_contract_id, created_at, updated_at)
        VALUES
            ('CP_P406_30_07_2026', @tenant_demo_tenant_id, @c405, @r405, @r406, JSON_ARRAY(@p401, @p405_co), NULL, 'NEW_CONTRACT', NULL, '2026-08-10', 'Người đứng tên chọn người ở cùng chuyển cùng sang phòng 406.', 0, NULL, NULL, NULL, NULL, 'REQUESTED', NULL, @debt405, '2026-07-30 08:10:00', TRUE, JSON_OBJECT('seed', 'requested-with-co-occupant', 'sourceRoomWillBeEmpty', TRUE, 'movingProfileIds', JSON_ARRAY(@p401, @p405_co)), JSON_ARRAY(), JSON_OBJECT('transferCount12m', 0), NULL, NULL, NULL, NULL, NULL, NULL, '2026-07-30 08:10:00', '2026-07-30 08:10:00'),
            ('CP_P502_30_07_2026', @tenant_demo_tenant_id, @c501, @r501, @r502, JSON_ARRAY(@p401), @p401, 'NEW_CONTRACT', NULL, '2026-08-10', 'Chờ upload hợp đồng đã ký.', 1, '2026-08-09 23:59:59', @manager_id, '2026-07-30 09:30:00', NULL, 'WAITING_SIGNING', 'NO_DIFFERENCE', @debt501, '2026-07-30 08:15:00', TRUE, JSON_OBJECT('seed', 'waiting-signing'), JSON_ARRAY(), JSON_OBJECT('transferCount12m', 0), @manager_id, '2026-07-30 09:30:00', NULL, NULL, @c502new, NULL, '2026-07-30 08:15:00', '2026-07-30 10:00:00'),
            ('CP_P504_30_07_2026', @tenant_demo_tenant_id, @c503, @r503, @r504, JSON_ARRAY(@p401), @p401, 'NEW_CONTRACT', NULL, '2026-08-08', 'Đã bàn giao phòng cũ, chờ thanh toán hóa đơn chốt.', 1, NULL, @manager_id, '2026-07-30 10:30:00', NULL, 'WAITING_EXECUTION', 'NO_DIFFERENCE', @debt503, '2026-07-30 08:20:00', TRUE, JSON_OBJECT('seed', 'waiting-execution'), JSON_ARRAY(), JSON_OBJECT('transferCount12m', 0), @manager_id, '2026-07-30 10:30:00', '2026-07-30 11:30:00', NULL, @c504new, NULL, '2026-07-30 08:20:00', '2026-07-30 11:30:00'),
            ('CP_P506_20_07_2026', @tenant_demo_tenant_id, @c505, @r505, @r506, JSON_ARRAY(@p401), @p401, 'NEW_CONTRACT', NULL, '2026-07-20', 'Chuyển phòng đã hoàn tất.', 1, NULL, @manager_id, '2026-07-20 09:00:00', NULL, 'COMPLETED', 'NO_DIFFERENCE', @debt505, '2026-07-20 08:00:00', TRUE, JSON_OBJECT('seed', 'completed'), JSON_ARRAY(), JSON_OBJECT('transferCount12m', 0), @manager_id, '2026-07-20 09:00:00', '2026-07-20 11:00:00', '2026-07-20 12:30:00', @c506new, NULL, '2026-07-20 08:00:00', '2026-07-20 12:30:00');

        SET @tr406 := (SELECT room_transfer_request_id FROM hdbhms.room_transfer_requests WHERE request_code = 'CP_P406_30_07_2026' LIMIT 1);
        SET @tr502 := (SELECT room_transfer_request_id FROM hdbhms.room_transfer_requests WHERE request_code = 'CP_P502_30_07_2026' LIMIT 1);
        SET @tr504 := (SELECT room_transfer_request_id FROM hdbhms.room_transfer_requests WHERE request_code = 'CP_P504_30_07_2026' LIMIT 1);
        SET @tr506 := (SELECT room_transfer_request_id FROM hdbhms.room_transfer_requests WHERE request_code = 'CP_P506_20_07_2026' LIMIT 1);

        UPDATE hdbhms.change_requests SET target_id = @tr406 WHERE change_request_id = @cr406;
        UPDATE hdbhms.change_requests SET target_id = @tr502 WHERE change_request_id = @cr502;
        UPDATE hdbhms.change_requests SET target_id = @tr504 WHERE change_request_id = @cr504;
        UPDATE hdbhms.change_requests SET target_id = @tr506 WHERE change_request_id = @cr506;

        INSERT INTO hdbhms.transfer_settlements
            (transfer_request_id, old_room_remaining_value, new_room_required_value, difference_amount, settlement_type, positive_difference_settlement_type, old_room_final_invoice_id, transfer_difference_invoice_id, confirmed_by, confirmed_at, created_at)
        VALUES
            (@tr504, 480000, 480000, 0, 'NO_DIFFERENCE', 'NO_DIFFERENCE', @inv503final, NULL, @manager_id, '2026-07-30 11:30:00', '2026-07-30 11:30:00'),
            (@tr506, 350000, 350000, 0, 'NO_DIFFERENCE', 'NO_DIFFERENCE', @inv505final, NULL, @manager_id, '2026-07-20 12:30:00', '2026-07-20 12:30:00');

        INSERT INTO hdbhms.contract_handover_records
            (contract_id, room_id, handover_type, handover_date, electricity_reading_id, water_reading_id, note, status, confirmed_by, confirmed_at, created_at, signed_document_id)
        VALUES
            (@c503, @r503, 'TRANSFER_OUT', '2026-07-30 11:30:00', NULL, NULL, 'Seed transfer-out handover, waiting final execution.', 'CONFIRMED', @manager_id, '2026-07-30 11:35:00', '2026-07-30 11:30:00', NULL),
            (@c505, @r505, 'TRANSFER_OUT', '2026-07-20 11:00:00', NULL, NULL, 'Seed completed transfer-out handover.', 'CONFIRMED', @manager_id, '2026-07-20 11:05:00', '2026-07-20 11:00:00', NULL),
            (@c506new, @r506, 'TRANSFER_IN', '2026-07-20 12:00:00', NULL, NULL, 'Seed completed transfer-in handover.', 'CONFIRMED', @manager_id, '2026-07-20 12:05:00', '2026-07-20 12:00:00', NULL);

        INSERT INTO hdbhms.contract_liquidations
            (contract_id, liquidation_date, reason, deposit_amount, deposit_deduction_amount, deposit_deduction_reason, deposit_refund_amount, final_invoice_id, signed_file_id, status, created_by, created_at)
        VALUES
            (@c403, '2026-07-31', 'Seed demo thanh lý hợp đồng.', 2300000, 200000, 'Đền bù tài sản phòng 403.', 2100000, @inv403final, @liquidation_403_file, 'DRAFT', @manager_id, '2026-07-30 09:30:00'),
            (@c301, '2026-07-25', 'Seed completed liquidation.', 2200000, 2200000, 'Khấu trừ toàn bộ cọc để tất toán demo.', 0, @inv301final, @receipt_file, 'CONFIRMED', @manager_id, '2026-07-25 12:30:00');

        INSERT INTO hdbhms.change_request_events
            (request_id, from_status, to_status, note, acted_by, acted_at)
        VALUES
            (@cr403, NULL, 'PENDING', 'Khách tạo yêu cầu thanh lý.', @tenant_demo_user_id, '2026-07-30 08:00:00'),
            (@cr403, 'PENDING', 'UNDER_REVIEW', 'Quản lý tiếp nhận yêu cầu thanh lý.', @manager_id, '2026-07-30 08:30:00'),
            (@cr403, 'UNDER_REVIEW', 'APPROVED', 'Chủ nhà duyệt thanh lý.', @owner_id, '2026-07-30 09:00:00'),
            (@cr403, 'APPROVED', 'PROCESSING', 'Đang xử lý hóa đơn checkout và hoàn cọc.', @manager_id, '2026-07-30 09:30:00'),
            (@cr404, NULL, 'PENDING', 'Khách tạo yêu cầu gia hạn.', @tenant_demo_user_id, '2026-07-30 08:05:00'),
            (@cr406, NULL, 'PENDING', 'Khách tạo yêu cầu chuyển phòng cùng người ở cùng.', @tenant_demo_user_id, '2026-07-30 08:10:00'),
            (@cr502, NULL, 'PENDING', 'Khách tạo yêu cầu chuyển phòng.', @tenant_demo_user_id, '2026-07-30 08:15:00'),
            (@cr502, 'PENDING', 'PROCESSING', 'Đã tạo hợp đồng chuyển phòng, chờ ký.', @manager_id, '2026-07-30 10:00:00'),
            (@cr504, NULL, 'PENDING', 'Khách tạo yêu cầu chuyển phòng.', @tenant_demo_user_id, '2026-07-30 08:20:00'),
            (@cr504, 'PENDING', 'PROCESSING', 'Đã bàn giao phòng cũ, chờ thanh toán hóa đơn chốt.', @manager_id, '2026-07-30 11:30:00'),
            (@cr506, NULL, 'PENDING', 'Khách tạo yêu cầu chuyển phòng.', @tenant_demo_user_id, '2026-07-20 08:00:00'),
            (@cr506, 'PENDING', 'COMPLETED', 'Đã hoàn tất chuyển phòng.', @manager_id, '2026-07-20 12:30:00'),
            (@cr507, NULL, 'PENDING', 'Khách tạo yêu cầu thanh lý nhưng có người ở cùng muốn ở lại.', @tenant_demo_user_id, '2026-07-30 08:35:00'),
            (@cr301, NULL, 'PENDING', 'Khách tạo yêu cầu thanh lý.', @tenant_demo_user_id, '2026-07-25 08:30:00'),
            (@cr301, 'PENDING', 'COMPLETED', 'Đã hoàn tất thanh lý hợp đồng.', @owner_id, '2026-07-25 12:30:00'),
            (@cr302, NULL, 'PENDING', 'Khách tạo yêu cầu thanh lý.', @tenant_demo_user_id, '2026-07-30 08:40:00'),
            (@cr302, 'PENDING', 'REJECTED', 'Từ chối vì khách chưa hoàn tất công nợ.', @owner_id, '2026-07-30 09:30:00'),
            (@crAccess, 'PENDING', 'APPROVED', 'Chủ nhà cấp quyền xem hồ sơ.', @owner_id, '2026-07-30 09:00:00');

        INSERT INTO hdbhms.permission_grants
            (grantee_user_id, target_type, target_id, source_change_request_id, granted_by, reason, duration_code, granted_at, expires_at, revoked_at, revoked_by, revoke_reason, created_at, updated_at)
        VALUES
            (@manager_id, 'TENANT_PROFILE', @p401, @crAccess, @owner_id, 'Seed quyền xem hồ sơ đã được duyệt.', 'DAYS_30', '2026-07-30 09:00:00', '2026-08-29 23:59:59', NULL, NULL, NULL, '2026-07-30 09:00:00', '2026-07-30 09:00:00');

        INSERT INTO hdbhms.operating_expenses
            (property_id, room_id, ticket_id, expense_code, expense_type, description, amount, expense_date, paid_by_user_id, receipt_file_id, status, approved_by, approved_at, created_by, created_at)
        VALUES
            (@property_id, @r408, NULL, 'SEED-EXP-408-REPAIR', 'REPAIR', 'Chi phí bảo trì Seed cho export và báo cáo advisor.', 1200000, '2026-07-15', @owner_id, @receipt_file, 'PAID', @owner_id, '2026-07-15 09:00:00', @manager_id, '2026-07-15 08:00:00');
        SET @expense408 := LAST_INSERT_ID();

        INSERT INTO hdbhms.expense_payments
            (operating_expense_id, payment_date, payment_method, payment_reference, receipt_file_id, paid_by_user_id, paid_at, note, created_at)
        VALUES
            (@expense408, '2026-07-15', 'BANK_TRANSFER', 'SEED-EXP-408-REPAIR', @receipt_file, @owner_id, '2026-07-15 09:10:00', 'Seed thanh toán chi phí đã trả.', '2026-07-15 09:10:00');

        COMMIT;
    END IF;
END//

DELIMITER ;

CALL hdbhms.seed_demo_booking_checkout_lifecycle_exports_v42();

DROP PROCEDURE IF EXISTS hdbhms.seed_demo_booking_checkout_lifecycle_exports_v42;
