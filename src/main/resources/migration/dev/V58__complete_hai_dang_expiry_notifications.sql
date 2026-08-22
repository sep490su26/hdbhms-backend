SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- Every Hai Dang contract explicitly marked EXPIRING_SOON must have the
-- tenant reminder seeded. Do not rely on one hard-coded room for this flow.
SET @hdd1_property_id := (
    SELECT property_id
    FROM hdbhms.properties
    WHERE property_code = 'HAI_DANG_1'
      AND deleted_at IS NULL
    LIMIT 1
);
SET @hdd1_seed_now := '2026-08-01 09:00:00';
SET @hdd1_shared_password_hash := '$2a$10$2Dy4Vg1B5BKuiUMPRuTAluvk/0XzLuSgLGaABFHCoWHaUfUtDFGqm';
SET @hdd1_manager_id := COALESCE(
    (
        SELECT user_id
        FROM hdbhms.users
        WHERE email = 'seed.manager@hdbhms.local'
          AND deleted_at IS NULL
        LIMIT 1
    ),
    (
        SELECT user_id
        FROM hdbhms.users
        WHERE role = 'OWNER'
          AND status = 'ACTIVE'
          AND deleted_at IS NULL
        ORDER BY user_id
        LIMIT 1
    )
);
SET @hdd1_portrait_file_id := (
    SELECT file_metadata_id
    FROM hdbhms.file_metadata
    WHERE storage_key = 'identity-samples/anh-chan-dung.webp'
      AND deleted_at IS NULL
    LIMIT 1
);
SET @hdd1_cccd_front_file_id := (
    SELECT file_metadata_id
    FROM hdbhms.file_metadata
    WHERE storage_key = 'identity-samples/cccd-mat-truoc.jpg'
      AND deleted_at IS NULL
    LIMIT 1
);
SET @hdd1_cccd_back_file_id := (
    SELECT file_metadata_id
    FROM hdbhms.file_metadata
    WHERE storage_key = 'identity-samples/cccd-mat-sau.jpg'
      AND deleted_at IS NULL
    LIMIT 1
);

-- Repair the already-applied seed as well: room 405 is occupied and its
-- 31/10/2026 contract is inside the current expiry window.
UPDATE hdbhms.lease_contracts contract
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
SET contract.status = 'EXPIRING_SOON',
    contract.tenant_intention = NULL,
    contract.expected_vacant_date = NULL,
    contract.intention_recorded_at = NULL,
    contract.updated_at = @hdd1_seed_now
WHERE room.property_id = @hdd1_property_id
  AND contract.contract_code = 'HDT_P405_01_11_2025'
  AND contract.status = 'ACTIVE'
    AND contract.end_date = '2026-10-31';

-- Backfill one account with three active room memberships for databases that
-- already ran V48 before the multi-room account was added to the seed.
INSERT INTO hdbhms.users
    (phone, email, password_hash, role, status, last_login_at, email_verified,
     must_change_password, created_at, updated_at, deleted_at)
VALUES
    ('0901309001', 'nguyen.van.khai@haidang1.local', @hdd1_shared_password_hash,
     'TENANT', 'ACTIVE', NULL, TRUE, FALSE, @hdd1_seed_now, @hdd1_seed_now, NULL)
ON DUPLICATE KEY UPDATE
    phone = VALUES(phone),
    email = VALUES(email),
    password_hash = VALUES(password_hash),
    role = 'TENANT',
    status = 'ACTIVE',
    deleted_at = NULL,
    updated_at = VALUES(updated_at);

SET @hdd1_shared_user_id := (
    SELECT user_id
    FROM hdbhms.users
    WHERE email = 'nguyen.van.khai@haidang1.local'
      AND deleted_at IS NULL
    LIMIT 1
);

INSERT INTO hdbhms.tenants (user_id, property_id, created_at, updated_at, deleted_at)
SELECT @hdd1_shared_user_id, @hdd1_property_id, @hdd1_seed_now, @hdd1_seed_now, NULL
WHERE NOT EXISTS (
    SELECT 1
    FROM hdbhms.tenants existing_tenant
    WHERE existing_tenant.user_id = @hdd1_shared_user_id
      AND existing_tenant.property_id = @hdd1_property_id
      AND existing_tenant.deleted_at IS NULL
);

SET @hdd1_shared_tenant_id := (
    SELECT tenant_id
    FROM hdbhms.tenants
    WHERE user_id = @hdd1_shared_user_id
      AND property_id = @hdd1_property_id
      AND deleted_at IS NULL
    LIMIT 1
);

INSERT INTO hdbhms.person_profiles
    (user_id, full_name, dob, gender, phone, email, permanent_address,
     portrait_file_id, created_at, updated_at, deleted_at)
VALUES
    (@hdd1_shared_user_id, 'Nguyễn Văn Khải', '1995-07-17', 'MALE',
     '0901309001', 'nguyen.van.khai@haidang1.local', 'Hà Nội', @hdd1_portrait_file_id,
     @hdd1_seed_now, @hdd1_seed_now, NULL)
ON DUPLICATE KEY UPDATE
    full_name = VALUES(full_name),
    dob = VALUES(dob),
    gender = VALUES(gender),
    phone = VALUES(phone),
    email = VALUES(email),
    permanent_address = VALUES(permanent_address),
    portrait_file_id = VALUES(portrait_file_id),
    deleted_at = NULL,
    updated_at = VALUES(updated_at);

SET @hdd1_shared_profile_id := (
    SELECT person_profile_id
    FROM hdbhms.person_profiles
    WHERE user_id = @hdd1_shared_user_id
      AND deleted_at IS NULL
    LIMIT 1
);

UPDATE hdbhms.lease_contracts contract
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
SET contract.primary_tenant_profile_id = @hdd1_shared_profile_id,
    contract.updated_at = @hdd1_seed_now
WHERE room.property_id = @hdd1_property_id
  AND room.room_code IN ('301', '302', '303')
  AND contract.status IN ('ACTIVE', 'EXPIRING_SOON', 'TERMINATION_PENDING');

UPDATE hdbhms.contract_occupants occupant
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = occupant.contract_id
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
SET occupant.tenant_id = @hdd1_shared_tenant_id,
    occupant.tenant_profile_id = @hdd1_shared_profile_id
WHERE room.property_id = @hdd1_property_id
  AND room.room_code IN ('301', '302', '303')
  AND occupant.occupant_role = 'PRIMARY'
  AND occupant.status = 'ACTIVE'
  AND contract.status IN ('ACTIVE', 'EXPIRING_SOON', 'TERMINATION_PENDING');

INSERT INTO hdbhms.contract_occupants
    (contract_id, tenant_id, tenant_profile_id, occupant_role, move_in_date, move_out_date,
     status, disabled_reason, disabled_by, disabled_at, created_at)
SELECT
    contract.lease_contract_id,
    @hdd1_shared_tenant_id,
    @hdd1_shared_profile_id,
    'PRIMARY',
    contract.start_date,
    NULL,
    'ACTIVE',
    NULL,
    NULL,
    NULL,
    @hdd1_seed_now
FROM hdbhms.lease_contracts contract
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
WHERE room.property_id = @hdd1_property_id
  AND room.room_code IN ('301', '302', '303')
  AND contract.status IN ('ACTIVE', 'EXPIRING_SOON', 'TERMINATION_PENDING')
  AND NOT EXISTS (
      SELECT 1
      FROM hdbhms.contract_occupants existing_occupant
      WHERE existing_occupant.contract_id = contract.lease_contract_id
        AND existing_occupant.occupant_role = 'PRIMARY'
        AND existing_occupant.status = 'ACTIVE'
  );

-- Every existing primary signer must be able to complete the contract and
-- identity-verification flows with a portrait and both CCCD sides.
UPDATE hdbhms.person_profiles profile
JOIN hdbhms.lease_contracts contract
  ON contract.primary_tenant_profile_id = profile.person_profile_id
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
SET profile.portrait_file_id = @hdd1_portrait_file_id,
    profile.updated_at = @hdd1_seed_now
WHERE room.property_id = @hdd1_property_id
  AND contract.deleted_at IS NULL
  AND profile.deleted_at IS NULL
  AND @hdd1_portrait_file_id IS NOT NULL;

UPDATE hdbhms.identity_documents identity_document
JOIN hdbhms.person_profiles profile
  ON profile.person_profile_id = identity_document.profile_id
JOIN hdbhms.lease_contracts contract
  ON contract.primary_tenant_profile_id = profile.person_profile_id
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
SET identity_document.front_file_id = @hdd1_cccd_front_file_id,
    identity_document.back_file_id = @hdd1_cccd_back_file_id,
    identity_document.updated_at = @hdd1_seed_now
WHERE room.property_id = @hdd1_property_id
  AND contract.deleted_at IS NULL
  AND profile.deleted_at IS NULL
  AND identity_document.doc_type = 'CCCD'
  AND identity_document.status = 'ACTIVE'
  AND @hdd1_cccd_front_file_id IS NOT NULL
  AND @hdd1_cccd_back_file_id IS NOT NULL;

INSERT INTO hdbhms.identity_documents
    (profile_id, doc_type, doc_number, issued_date, issued_place, expiry_date,
     raw_ocr_data, front_file_id, back_file_id, status, created_at, updated_at)
SELECT
    signer.person_profile_id,
    'CCCD',
    CONCAT('01', signer.phone),
    '2025-03-18',
    'Bộ Công an',
    '2040-05-23',
    CAST(JSON_OBJECT('nguon', 'du-lieu-mau') AS BINARY),
    @hdd1_cccd_front_file_id,
    @hdd1_cccd_back_file_id,
    'ACTIVE',
    @hdd1_seed_now,
    @hdd1_seed_now
FROM (
    SELECT DISTINCT profile.person_profile_id, profile.phone
    FROM hdbhms.person_profiles profile
    JOIN hdbhms.lease_contracts contract
      ON contract.primary_tenant_profile_id = profile.person_profile_id
    JOIN hdbhms.rooms room
      ON room.room_id = contract.room_id
    WHERE room.property_id = @hdd1_property_id
      AND contract.deleted_at IS NULL
      AND profile.deleted_at IS NULL
) signer
WHERE signer.phone REGEXP '^0[0-9]{9}$'
  AND @hdd1_cccd_front_file_id IS NOT NULL
  AND @hdd1_cccd_back_file_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM hdbhms.identity_documents existing_document
      WHERE existing_document.profile_id = signer.person_profile_id
        AND existing_document.doc_type = 'CCCD'
        AND existing_document.status = 'ACTIVE'
  );

INSERT INTO hdbhms.tenant_account_provisionings
    (tenant_profile_id, user_id, first_contract_id, latest_contract_id, status,
     recipient_email, sent_at, attempt_count, created_at, updated_at)
SELECT
    @hdd1_shared_profile_id,
    @hdd1_shared_user_id,
    MIN(contract.lease_contract_id),
    MAX(contract.lease_contract_id),
    'ACTIVE',
    'nguyen.van.khai@haidang1.local',
    @hdd1_seed_now,
    0,
    @hdd1_seed_now,
    @hdd1_seed_now
FROM hdbhms.lease_contracts contract
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
WHERE room.property_id = @hdd1_property_id
  AND room.room_code IN ('301', '302', '303')
  AND contract.status IN ('ACTIVE', 'EXPIRING_SOON', 'TERMINATION_PENDING')
ON DUPLICATE KEY UPDATE
    user_id = VALUES(user_id),
    first_contract_id = VALUES(first_contract_id),
    latest_contract_id = VALUES(latest_contract_id),
    status = 'ACTIVE',
    recipient_email = VALUES(recipient_email),
    sent_at = VALUES(sent_at),
    failed_at = NULL,
    failure_reason = NULL,
    updated_at = VALUES(updated_at);

-- Re-align legacy trackers with the same calendar milestones used by the
-- runtime. A fixed 30-day interval is not equivalent to minus one month.
UPDATE hdbhms.reminder_trackers tracker
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = tracker.target_id
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
SET tracker.status = 'COMPLETED',
    tracker.completed_at = COALESCE(tracker.completed_at, @hdd1_seed_now),
    tracker.next_due_at = NULL,
    tracker.updated_at = @hdd1_seed_now
WHERE tracker.reminder_key = 'LEASE_EXPIRY_INTENTION'
  AND tracker.target_type = 'CONTRACT'
  AND tracker.audience = 'PRIMARY_TENANT'
  AND tracker.status = 'ACTIVE'
  AND room.property_id = @hdd1_property_id
  AND contract.status = 'EXPIRING_SOON'
  AND contract.tenant_intention IS NOT NULL
  AND contract.end_date >= DATE(@hdd1_seed_now);

UPDATE hdbhms.reminder_trackers tracker
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = tracker.target_id
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
JOIN hdbhms.person_profiles profile
  ON profile.person_profile_id = contract.primary_tenant_profile_id
JOIN hdbhms.users tenant_user
  ON tenant_user.user_id = profile.user_id
SET tracker.recipient_user_id = tenant_user.user_id,
    tracker.status = 'ACTIVE',
    tracker.completed_at = NULL,
    tracker.sent_count =
        CASE WHEN DATE_SUB(contract.end_date, INTERVAL 3 MONTH) <= DATE(@hdd1_seed_now) THEN 1 ELSE 0 END
      + CASE WHEN DATE_SUB(contract.end_date, INTERVAL 2 MONTH) <= DATE(@hdd1_seed_now) THEN 1 ELSE 0 END
      + CASE WHEN DATE_SUB(contract.end_date, INTERVAL 1 MONTH) <= DATE(@hdd1_seed_now) THEN 1 ELSE 0 END,
    tracker.last_sent_at = CASE
        WHEN DATE_SUB(contract.end_date, INTERVAL 1 MONTH) <= DATE(@hdd1_seed_now)
            THEN CONCAT(DATE_SUB(contract.end_date, INTERVAL 1 MONTH), ' 09:00:00')
        WHEN DATE_SUB(contract.end_date, INTERVAL 2 MONTH) <= DATE(@hdd1_seed_now)
            THEN CONCAT(DATE_SUB(contract.end_date, INTERVAL 2 MONTH), ' 09:00:00')
        WHEN DATE_SUB(contract.end_date, INTERVAL 3 MONTH) <= DATE(@hdd1_seed_now)
            THEN CONCAT(DATE_SUB(contract.end_date, INTERVAL 3 MONTH), ' 09:00:00')
        ELSE NULL
    END,
    tracker.next_due_at = CASE
        WHEN DATE_SUB(contract.end_date, INTERVAL 3 MONTH) > DATE(@hdd1_seed_now)
            THEN CONCAT(DATE_SUB(contract.end_date, INTERVAL 3 MONTH), ' 09:00:00')
        WHEN DATE_SUB(contract.end_date, INTERVAL 2 MONTH) > DATE(@hdd1_seed_now)
            THEN CONCAT(DATE_SUB(contract.end_date, INTERVAL 2 MONTH), ' 09:00:00')
        WHEN DATE_SUB(contract.end_date, INTERVAL 1 MONTH) > DATE(@hdd1_seed_now)
            THEN CONCAT(DATE_SUB(contract.end_date, INTERVAL 1 MONTH), ' 09:00:00')
        ELSE NULL
    END,
    tracker.metadata = JSON_OBJECT(
        'contractCode', contract.contract_code,
        'roomCode', room.room_code,
        'endDate', contract.end_date,
        'firstReminderDate', DATE_SUB(contract.end_date, INTERVAL 3 MONTH),
        'secondReminderDate', DATE_SUB(contract.end_date, INTERVAL 2 MONTH),
        'finalReminderDate', DATE_SUB(contract.end_date, INTERVAL 1 MONTH),
        'lastReminderStage', CASE
            WHEN DATE_SUB(contract.end_date, INTERVAL 1 MONTH) <= DATE(@hdd1_seed_now) THEN 'FINAL'
            WHEN DATE_SUB(contract.end_date, INTERVAL 2 MONTH) <= DATE(@hdd1_seed_now) THEN 'SECOND'
            WHEN DATE_SUB(contract.end_date, INTERVAL 3 MONTH) <= DATE(@hdd1_seed_now) THEN 'FIRST'
            ELSE 'PENDING'
        END
    ),
    tracker.updated_at = @hdd1_seed_now
WHERE tracker.reminder_key = 'LEASE_EXPIRY_INTENTION'
  AND tracker.target_type = 'CONTRACT'
  AND tracker.audience = 'PRIMARY_TENANT'
  AND tracker.status = 'ACTIVE'
  AND room.property_id = @hdd1_property_id
  AND contract.status = 'EXPIRING_SOON'
  AND contract.tenant_intention IS NULL
  AND contract.end_date >= DATE(@hdd1_seed_now);

INSERT INTO hdbhms.reminder_trackers
    (reminder_key, target_type, target_id, audience, recipient_user_id, status, sent_count,
     last_sent_at, next_due_at, metadata, created_at, updated_at)
SELECT
    'LEASE_EXPIRY_INTENTION',
    'CONTRACT',
    contract.lease_contract_id,
    'PRIMARY_TENANT',
    tenant_user.user_id,
    'ACTIVE',
    CASE WHEN DATE_SUB(contract.end_date, INTERVAL 3 MONTH) <= DATE(@hdd1_seed_now) THEN 1 ELSE 0 END
      + CASE WHEN DATE_SUB(contract.end_date, INTERVAL 2 MONTH) <= DATE(@hdd1_seed_now) THEN 1 ELSE 0 END
      + CASE WHEN DATE_SUB(contract.end_date, INTERVAL 1 MONTH) <= DATE(@hdd1_seed_now) THEN 1 ELSE 0 END,
    CASE
        WHEN DATE_SUB(contract.end_date, INTERVAL 1 MONTH) <= DATE(@hdd1_seed_now)
            THEN CONCAT(DATE_SUB(contract.end_date, INTERVAL 1 MONTH), ' 09:00:00')
        WHEN DATE_SUB(contract.end_date, INTERVAL 2 MONTH) <= DATE(@hdd1_seed_now)
            THEN CONCAT(DATE_SUB(contract.end_date, INTERVAL 2 MONTH), ' 09:00:00')
        WHEN DATE_SUB(contract.end_date, INTERVAL 3 MONTH) <= DATE(@hdd1_seed_now)
            THEN CONCAT(DATE_SUB(contract.end_date, INTERVAL 3 MONTH), ' 09:00:00')
        ELSE NULL
    END,
    CASE
        WHEN DATE_SUB(contract.end_date, INTERVAL 3 MONTH) > DATE(@hdd1_seed_now)
            THEN CONCAT(DATE_SUB(contract.end_date, INTERVAL 3 MONTH), ' 09:00:00')
        WHEN DATE_SUB(contract.end_date, INTERVAL 2 MONTH) > DATE(@hdd1_seed_now)
            THEN CONCAT(DATE_SUB(contract.end_date, INTERVAL 2 MONTH), ' 09:00:00')
        WHEN DATE_SUB(contract.end_date, INTERVAL 1 MONTH) > DATE(@hdd1_seed_now)
            THEN CONCAT(DATE_SUB(contract.end_date, INTERVAL 1 MONTH), ' 09:00:00')
        ELSE NULL
    END,
    JSON_OBJECT(
        'contractCode', contract.contract_code,
        'roomCode', room.room_code,
        'endDate', contract.end_date,
        'firstReminderDate', DATE_SUB(contract.end_date, INTERVAL 3 MONTH),
        'secondReminderDate', DATE_SUB(contract.end_date, INTERVAL 2 MONTH),
        'finalReminderDate', DATE_SUB(contract.end_date, INTERVAL 1 MONTH),
        'lastReminderStage', CASE
            WHEN DATE_SUB(contract.end_date, INTERVAL 1 MONTH) <= DATE(@hdd1_seed_now) THEN 'FINAL'
            WHEN DATE_SUB(contract.end_date, INTERVAL 2 MONTH) <= DATE(@hdd1_seed_now) THEN 'SECOND'
            WHEN DATE_SUB(contract.end_date, INTERVAL 3 MONTH) <= DATE(@hdd1_seed_now) THEN 'FIRST'
            ELSE 'PENDING'
        END
    ),
    @hdd1_seed_now,
    @hdd1_seed_now
FROM hdbhms.lease_contracts contract
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
JOIN hdbhms.person_profiles profile
  ON profile.person_profile_id = contract.primary_tenant_profile_id
JOIN hdbhms.users tenant_user
  ON tenant_user.user_id = profile.user_id
WHERE room.property_id = @hdd1_property_id
  AND contract.status = 'EXPIRING_SOON'
  AND contract.tenant_intention IS NULL
  AND contract.end_date >= DATE(@hdd1_seed_now)
  AND NOT EXISTS (
      SELECT 1
      FROM hdbhms.reminder_trackers existing_tracker
      WHERE existing_tracker.reminder_key = 'LEASE_EXPIRY_INTENTION'
        AND existing_tracker.target_type = 'CONTRACT'
        AND existing_tracker.target_id = contract.lease_contract_id
        AND existing_tracker.audience = 'PRIMARY_TENANT'
        AND existing_tracker.status = 'ACTIVE'
  );

INSERT INTO hdbhms.notification_outbox
    (event_type, target_type, target_id, recipient_user_id, channel, title, body, payload, status,
     retry_count, max_retries, scheduled_at, sent_at, created_at, is_read)
SELECT
    'LEASE_EXPIRY_REMINDER_FIRST',
    'CONTRACT',
    contract.lease_contract_id,
    tenant_user.user_id,
    'PUSH',
    CONCAT('Hợp đồng ', contract.contract_code, ' sắp hết hạn'),
    CONCAT(
        'Phòng ', room.room_code,
        ' tại ', property.name,
        ' sẽ hết hạn vào ', contract.end_date,
        '. Bạn muốn gia hạn, chuyển phòng hay chuyển đi?'
    ),
    JSON_OBJECT(
        'contractId', contract.lease_contract_id,
        'contractCode', contract.contract_code,
        'roomId', room.room_id,
        'roomName', room.name,
        'roomCode', room.room_code,
        'propertyName', property.name,
        'endDate', contract.end_date,
        'daysRemaining', DATEDIFF(contract.end_date, DATE_SUB(contract.end_date, INTERVAL 3 MONTH)),
        'stage', 'FIRST',
        'targetRoute', '/contract'
    ),
    'SENT',
    0,
    3,
     CONCAT(DATE_SUB(contract.end_date, INTERVAL 3 MONTH), ' 09:00:00'),
     CONCAT(DATE_SUB(contract.end_date, INTERVAL 3 MONTH), ' 09:00:00'),
     CONCAT(DATE_SUB(contract.end_date, INTERVAL 3 MONTH), ' 09:00:00'),
     FALSE
FROM hdbhms.lease_contracts contract
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
JOIN hdbhms.properties property
  ON property.property_id = room.property_id
JOIN hdbhms.person_profiles profile
  ON profile.person_profile_id = contract.primary_tenant_profile_id
JOIN hdbhms.users tenant_user
  ON tenant_user.user_id = profile.user_id
WHERE room.property_id = @hdd1_property_id
  AND contract.status = 'EXPIRING_SOON'
  AND contract.tenant_intention IS NULL
  AND contract.end_date >= DATE(@hdd1_seed_now)
  AND DATE_SUB(contract.end_date, INTERVAL 3 MONTH) <= DATE(@hdd1_seed_now)
  AND NOT EXISTS (
      SELECT 1
      FROM hdbhms.notification_outbox existing_notification
      WHERE existing_notification.event_type = 'LEASE_EXPIRY_REMINDER_FIRST'
        AND existing_notification.target_type = 'CONTRACT'
        AND existing_notification.target_id = contract.lease_contract_id
        AND existing_notification.recipient_user_id = tenant_user.user_id
        AND existing_notification.channel = 'PUSH'
  );

INSERT INTO hdbhms.notification_outbox
    (event_type, target_type, target_id, recipient_user_id, channel, title, body, payload, status,
     retry_count, max_retries, scheduled_at, sent_at, created_at, is_read)
SELECT
    'LEASE_EXPIRY_REMINDER_SECOND',
    'CONTRACT',
    contract.lease_contract_id,
    tenant_user.user_id,
    'PUSH',
    CONCAT('Hợp đồng ', contract.contract_code, ' sắp hết hạn'),
    CONCAT('Phòng ', room.room_code, ' hết hạn hợp đồng ngày ',
           DATE_FORMAT(contract.end_date, '%d/%m/%Y'), '.'),
    JSON_OBJECT(
        'contractId', contract.lease_contract_id,
        'contractCode', contract.contract_code,
        'roomId', room.room_id,
        'roomName', room.name,
        'roomCode', room.room_code,
        'propertyName', property.name,
        'endDate', contract.end_date,
        'daysRemaining', DATEDIFF(contract.end_date, DATE_SUB(contract.end_date, INTERVAL 2 MONTH)),
        'stage', 'SECOND',
        'targetRoute', '/contract'
    ),
    'SENT', 0, 3,
    CONCAT(DATE_SUB(contract.end_date, INTERVAL 2 MONTH), ' 09:00:00'),
    CONCAT(DATE_SUB(contract.end_date, INTERVAL 2 MONTH), ' 09:00:00'),
    CONCAT(DATE_SUB(contract.end_date, INTERVAL 2 MONTH), ' 09:00:00'),
    FALSE
FROM hdbhms.lease_contracts contract
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
JOIN hdbhms.properties property
  ON property.property_id = room.property_id
JOIN hdbhms.person_profiles profile
  ON profile.person_profile_id = contract.primary_tenant_profile_id
JOIN hdbhms.users tenant_user
  ON tenant_user.user_id = profile.user_id
WHERE room.property_id = @hdd1_property_id
  AND contract.status = 'EXPIRING_SOON'
  AND contract.tenant_intention IS NULL
  AND contract.end_date >= DATE(@hdd1_seed_now)
  AND DATE_SUB(contract.end_date, INTERVAL 2 MONTH) <= DATE(@hdd1_seed_now)
  AND NOT EXISTS (
      SELECT 1
      FROM hdbhms.notification_outbox existing_notification
      WHERE existing_notification.event_type = 'LEASE_EXPIRY_REMINDER_SECOND'
        AND existing_notification.target_type = 'CONTRACT'
        AND existing_notification.target_id = contract.lease_contract_id
        AND existing_notification.recipient_user_id = tenant_user.user_id
        AND existing_notification.channel = 'PUSH'
  );

INSERT INTO hdbhms.notification_outbox
    (event_type, target_type, target_id, recipient_user_id, channel, title, body, payload, status,
     retry_count, max_retries, scheduled_at, sent_at, created_at, is_read)
SELECT
    'LEASE_EXPIRY_REMINDER_FINAL',
    'CONTRACT',
    contract.lease_contract_id,
    tenant_user.user_id,
    'PUSH',
    CONCAT('Hợp đồng ', contract.contract_code, ' sắp hết hạn'),
    CONCAT('Phòng ', room.room_code, ' hết hạn hợp đồng ngày ',
           DATE_FORMAT(contract.end_date, '%d/%m/%Y'), '.'),
    JSON_OBJECT(
        'contractId', contract.lease_contract_id,
        'contractCode', contract.contract_code,
        'roomId', room.room_id,
        'roomName', room.name,
        'roomCode', room.room_code,
        'propertyName', property.name,
        'endDate', contract.end_date,
        'daysRemaining', DATEDIFF(contract.end_date, DATE_SUB(contract.end_date, INTERVAL 1 MONTH)),
        'stage', 'FINAL',
        'targetRoute', '/contract'
    ),
    'SENT', 0, 3,
    CONCAT(DATE_SUB(contract.end_date, INTERVAL 1 MONTH), ' 09:00:00'),
    CONCAT(DATE_SUB(contract.end_date, INTERVAL 1 MONTH), ' 09:00:00'),
    CONCAT(DATE_SUB(contract.end_date, INTERVAL 1 MONTH), ' 09:00:00'),
    FALSE
FROM hdbhms.lease_contracts contract
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
JOIN hdbhms.properties property
  ON property.property_id = room.property_id
JOIN hdbhms.person_profiles profile
  ON profile.person_profile_id = contract.primary_tenant_profile_id
JOIN hdbhms.users tenant_user
  ON tenant_user.user_id = profile.user_id
WHERE room.property_id = @hdd1_property_id
  AND contract.status = 'EXPIRING_SOON'
  AND contract.tenant_intention IS NULL
  AND contract.end_date >= DATE(@hdd1_seed_now)
  AND DATE_SUB(contract.end_date, INTERVAL 1 MONTH) <= DATE(@hdd1_seed_now)
  AND NOT EXISTS (
      SELECT 1
      FROM hdbhms.notification_outbox existing_notification
      WHERE existing_notification.event_type = 'LEASE_EXPIRY_REMINDER_FINAL'
        AND existing_notification.target_type = 'CONTRACT'
        AND existing_notification.target_id = contract.lease_contract_id
        AND existing_notification.recipient_user_id = tenant_user.user_id
        AND existing_notification.channel = 'PUSH'
  );

-- Complete the manager side when an expiring contract already has a declared
-- renewal or move-out intention. The idempotency key keeps this safe to rerun.
INSERT INTO hdbhms.manager_tasks
    (title, description, task_type, idempotency_key, assignee_id, room_id, lease_contract_id,
     status, due_date, created_at, updated_at)
SELECT
    CASE contract.tenant_intention
        WHEN 'RENEW' THEN CONCAT('Cần chốt gia hạn hợp đồng ', contract.contract_code)
        ELSE CONCAT('Cần chốt bàn giao phòng ', room.name)
    END,
    CASE contract.tenant_intention
        WHEN 'RENEW' THEN CONCAT(
            'Khách phòng ', room.name,
            ' đã chọn gia hạn. Cần chốt giá, thời hạn, tiền cọc và lịch ký trước ',
            DATE_SUB(contract.end_date, INTERVAL 25 DAY), '.'
        )
        ELSE CONCAT(
            'Hợp đồng ', contract.contract_code,
            ' sắp đến hạn ', contract.end_date,
            '. Khách đã chọn chuyển đi, cần chốt lịch bàn giao.'
        )
    END,
    CASE contract.tenant_intention
        WHEN 'RENEW' THEN 'LEASE_RENEWAL_TERMS_CONFIRMATION'
        ELSE 'LEASE_HANDOVER_CONFIRMATION'
    END,
    CONCAT(
        CASE contract.tenant_intention
            WHEN 'RENEW' THEN 'LEASE_RENEWAL_TERMS_CONFIRMATION'
            ELSE 'LEASE_HANDOVER_CONFIRMATION'
        END,
        ':CONTRACT:',
        contract.lease_contract_id
    ),
    @hdd1_manager_id,
    room.room_id,
    contract.lease_contract_id,
    'PENDING',
    CASE contract.tenant_intention
        WHEN 'RENEW' THEN DATE_ADD(DATE(@hdd1_seed_now), INTERVAL 7 DAY)
        ELSE DATE_ADD(DATE(@hdd1_seed_now), INTERVAL 1 DAY)
    END,
    @hdd1_seed_now,
    @hdd1_seed_now
FROM hdbhms.lease_contracts contract
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
WHERE room.property_id = @hdd1_property_id
  AND contract.status = 'EXPIRING_SOON'
  AND contract.tenant_intention IN ('RENEW', 'MOVE_OUT', 'TRANSFER')
  AND (
      contract.tenant_intention = 'RENEW'
      OR DATEDIFF(
          COALESCE(contract.expected_vacant_date, contract.end_date),
          DATE(@hdd1_seed_now)
      ) <= 14
  )
  AND NOT EXISTS (
      SELECT 1
      FROM hdbhms.manager_tasks existing_task
      WHERE existing_task.idempotency_key = CONCAT(
          CASE contract.tenant_intention
              WHEN 'RENEW' THEN 'LEASE_RENEWAL_TERMS_CONFIRMATION'
              ELSE 'LEASE_HANDOVER_CONFIRMATION'
          END,
          ':CONTRACT:',
          contract.lease_contract_id
      )
  );

INSERT INTO hdbhms.notification_outbox
    (event_type, target_type, target_id, recipient_user_id, channel, title, body, payload, status,
     retry_count, max_retries, scheduled_at, sent_at, created_at, is_read)
SELECT
    CASE task.task_type
        WHEN 'LEASE_RENEWAL_TERMS_CONFIRMATION' THEN 'LEASE_RENEWAL_TERMS_CONFIRMATION_DUE'
        ELSE 'LEASE_HANDOVER_CONFIRMATION_DUE'
    END,
    'MANAGER_TASK',
    task.manager_task_id,
    @hdd1_manager_id,
    notification_channel.channel,
    task.title,
    task.description,
    JSON_OBJECT(
        'taskId', task.manager_task_id,
        'contractId', contract.lease_contract_id,
        'contractCode', contract.contract_code,
        'roomId', room.room_id,
        'roomName', room.name,
        'roomCode', room.room_code,
        'propertyName', property.name,
        'endDate', contract.end_date,
        'dueDate', task.due_date,
        'reason', task.description,
        'targetRoute', CONCAT('/dashboard/contracts/', contract.lease_contract_id)
    ),
    'SENT',
    0,
    3,
    @hdd1_seed_now,
    @hdd1_seed_now,
    @hdd1_seed_now,
    FALSE
FROM hdbhms.manager_tasks task
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = task.lease_contract_id
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
JOIN hdbhms.properties property
  ON property.property_id = room.property_id
CROSS JOIN (
    SELECT 'WEB' AS channel
    UNION ALL
    SELECT 'PUSH' AS channel
) notification_channel
WHERE task.task_type IN ('LEASE_RENEWAL_TERMS_CONFIRMATION', 'LEASE_HANDOVER_CONFIRMATION')
  AND room.property_id = @hdd1_property_id
  AND task.idempotency_key LIKE '%:CONTRACT:%'
  AND NOT EXISTS (
      SELECT 1
      FROM hdbhms.notification_outbox existing_notification
      WHERE existing_notification.target_type = 'MANAGER_TASK'
        AND existing_notification.target_id = task.manager_task_id
        AND existing_notification.recipient_user_id = @hdd1_manager_id
        AND existing_notification.channel = notification_channel.channel
        AND existing_notification.event_type = CASE task.task_type
            WHEN 'LEASE_RENEWAL_TERMS_CONFIRMATION' THEN 'LEASE_RENEWAL_TERMS_CONFIRMATION_DUE'
            ELSE 'LEASE_HANDOVER_CONFIRMATION_DUE'
        END
  );
