SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- Keep Nguyen Van Khai's three contracts on deterministic 3/2/1-month
-- expiry milestones. V48 owns the full Hai Dang seed; this migration keeps
-- an already-running dev database aligned with the same workflow.
SET @hdd1_property_id := (
    SELECT property_id
    FROM hdbhms.properties
    WHERE property_code = 'HAI_DANG_1'
      AND deleted_at IS NULL
    LIMIT 1
);
SET @hdd1_khai_profile_id := (
    SELECT profile.person_profile_id
    FROM hdbhms.person_profiles profile
    JOIN hdbhms.users user_account
      ON user_account.user_id = profile.user_id
    WHERE profile.deleted_at IS NULL
      AND user_account.deleted_at IS NULL
      AND (
          user_account.email IN (
              'nguyen.van.khai@haidang1.local',
              'nguyenvankhai95@gmail.com'
          )
          OR user_account.phone IN ('0901309001', '0918526407')
          OR profile.phone IN ('0901309001', '0918526407')
      )
    ORDER BY CASE WHEN EXISTS (
                   SELECT 1
                   FROM hdbhms.lease_contracts existing_contract
                   JOIN hdbhms.rooms existing_room
                     ON existing_room.room_id = existing_contract.room_id
                   WHERE existing_contract.primary_tenant_profile_id = profile.person_profile_id
                     AND existing_room.property_id = @hdd1_property_id
                     AND existing_room.room_code IN ('301', '302', '303')
                     AND existing_contract.deleted_at IS NULL
               ) THEN 0 ELSE 1 END,
             CASE WHEN user_account.phone IN ('0901309001', '0918526407') THEN 0 ELSE 1 END,
             profile.person_profile_id
    LIMIT 1
);
SET @hdd1_seed_now := '2026-08-01 09:00:00';

DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_khai_expiry_stages;
CREATE TEMPORARY TABLE tmp_hdd1_khai_expiry_stages
(
    room_code VARCHAR(10) NOT NULL PRIMARY KEY,
    end_date DATE NOT NULL
);

INSERT INTO tmp_hdd1_khai_expiry_stages (room_code, end_date)
VALUES
    ('302', '2026-10-31'),
    ('303', '2026-09-01');

UPDATE hdbhms.lease_contracts contract
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
JOIN tmp_hdd1_khai_expiry_stages stage
  ON stage.room_code = room.room_code
SET contract.end_date = stage.end_date,
    contract.status = 'EXPIRING_SOON',
    contract.tenant_intention = NULL,
    contract.expected_vacant_date = NULL,
    contract.intention_recorded_at = NULL,
    contract.updated_at = @hdd1_seed_now
WHERE room.property_id = @hdd1_property_id
  AND contract.primary_tenant_profile_id = @hdd1_khai_profile_id
  AND contract.deleted_at IS NULL
  AND contract.status IN ('ACTIVE', 'EXPIRING_SOON', 'TERMINATION_PENDING');

UPDATE hdbhms.rooms room
JOIN tmp_hdd1_khai_expiry_stages stage
  ON stage.room_code = room.room_code
SET room.current_status = 'SOON_VACANT',
    room.public_note = CONCAT(
        'Hợp đồng của Nguyễn Văn Khải hết hạn ngày ',
        DATE_FORMAT(stage.end_date, '%d/%m/%Y'),
        ', demo mốc ',
        CASE TIMESTAMPDIFF(MONTH, DATE(@hdd1_seed_now), stage.end_date)
            WHEN 3 THEN '3'
            WHEN 2 THEN '2'
            ELSE '1'
        END,
        ' tháng.'
    ),
    room.internal_note = 'Demo mốc nhắc hợp đồng 3/2/1 tháng của Nguyễn Văn Khải.',
    room.updated_at = @hdd1_seed_now
WHERE room.property_id = @hdd1_property_id
  AND room.deleted_at IS NULL;

DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_khai_expiry_recipients;
CREATE TEMPORARY TABLE tmp_hdd1_khai_expiry_recipients
(
    contract_id BIGINT UNSIGNED NOT NULL,
    recipient_user_id BIGINT UNSIGNED NOT NULL,
    audience VARCHAR(50) NOT NULL,
    PRIMARY KEY (contract_id, recipient_user_id, audience)
);

INSERT INTO tmp_hdd1_khai_expiry_recipients (contract_id, recipient_user_id, audience)
SELECT contract_id, recipient_user_id, audience
FROM (
    SELECT
        contract.lease_contract_id AS contract_id,
        user_account.user_id AS recipient_user_id,
        'PRIMARY_TENANT' AS audience
    FROM hdbhms.lease_contracts contract
    JOIN hdbhms.rooms room
      ON room.room_id = contract.room_id
    JOIN hdbhms.person_profiles profile
      ON profile.person_profile_id = contract.primary_tenant_profile_id
     AND profile.deleted_at IS NULL
    JOIN hdbhms.users user_account
      ON user_account.user_id = profile.user_id
     AND user_account.deleted_at IS NULL
    WHERE room.property_id = @hdd1_property_id
      AND room.room_code IN ('301', '302', '303')
      AND contract.primary_tenant_profile_id = @hdd1_khai_profile_id
      AND contract.status = 'EXPIRING_SOON'
    UNION ALL
    SELECT
        contract.lease_contract_id,
        user_account.user_id,
        'CO_OCCUPANT'
    FROM hdbhms.lease_contracts contract
    JOIN hdbhms.rooms room
      ON room.room_id = contract.room_id
    JOIN hdbhms.contract_occupants occupant
      ON occupant.contract_id = contract.lease_contract_id
     AND occupant.occupant_role = 'CO_OCCUPANT'
     AND occupant.status = 'ACTIVE'
    JOIN hdbhms.person_profiles profile
      ON profile.person_profile_id = occupant.tenant_profile_id
     AND profile.deleted_at IS NULL
    JOIN hdbhms.users user_account
      ON user_account.user_id = profile.user_id
     AND user_account.deleted_at IS NULL
    WHERE room.property_id = @hdd1_property_id
      AND room.room_code IN ('301', '302', '303')
      AND contract.primary_tenant_profile_id = @hdd1_khai_profile_id
      AND contract.status = 'EXPIRING_SOON'
) recipients
GROUP BY contract_id, recipient_user_id, audience;

DELETE delivery
FROM hdbhms.notification_deliveries delivery
JOIN hdbhms.notification_outbox notification
  ON notification.notification_outbox_id = delivery.outbox_id
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = notification.target_id
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
JOIN tmp_hdd1_khai_expiry_stages stage
  ON stage.room_code = room.room_code
WHERE notification.target_type = 'CONTRACT'
  AND notification.event_type IN (
      'LEASE_EXPIRY_REMINDER_FIRST',
      'LEASE_EXPIRY_REMINDER_SECOND',
      'LEASE_EXPIRY_REMINDER_FINAL'
  )
  AND room.property_id = @hdd1_property_id
  AND contract.primary_tenant_profile_id = @hdd1_khai_profile_id;

DELETE notification
FROM hdbhms.notification_outbox notification
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = notification.target_id
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
JOIN tmp_hdd1_khai_expiry_stages stage
  ON stage.room_code = room.room_code
WHERE notification.target_type = 'CONTRACT'
  AND notification.event_type IN (
      'LEASE_EXPIRY_REMINDER_FIRST',
      'LEASE_EXPIRY_REMINDER_SECOND',
      'LEASE_EXPIRY_REMINDER_FINAL'
  )
  AND room.property_id = @hdd1_property_id
  AND contract.primary_tenant_profile_id = @hdd1_khai_profile_id;

DELETE tracker
FROM hdbhms.reminder_trackers tracker
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = tracker.target_id
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
JOIN tmp_hdd1_khai_expiry_stages stage
  ON stage.room_code = room.room_code
WHERE tracker.reminder_key = 'LEASE_EXPIRY_INTENTION'
  AND tracker.target_type = 'CONTRACT'
  AND room.property_id = @hdd1_property_id
  AND contract.primary_tenant_profile_id = @hdd1_khai_profile_id;

INSERT INTO hdbhms.reminder_trackers
    (reminder_key, target_type, target_id, audience, recipient_user_id, status,
     sent_count, last_sent_at, next_due_at, metadata, created_at, updated_at)
SELECT
    'LEASE_EXPIRY_INTENTION',
    'CONTRACT',
    contract.lease_contract_id,
    recipient.audience,
    recipient.recipient_user_id,
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
        ELSE DATE_ADD(@hdd1_seed_now, INTERVAL 1 DAY)
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
JOIN tmp_hdd1_khai_expiry_recipients recipient
  ON recipient.contract_id = contract.lease_contract_id
WHERE room.property_id = @hdd1_property_id
  AND contract.primary_tenant_profile_id = @hdd1_khai_profile_id
  AND contract.status = 'EXPIRING_SOON';

INSERT INTO hdbhms.notification_outbox
    (event_type, target_type, target_id, recipient_user_id, channel, title, body, payload, status,
     retry_count, max_retries, scheduled_at, sent_at, created_at, is_read)
SELECT
    CASE reminder_stage.stage
        WHEN 'FIRST' THEN 'LEASE_EXPIRY_REMINDER_FIRST'
        WHEN 'SECOND' THEN 'LEASE_EXPIRY_REMINDER_SECOND'
        ELSE 'LEASE_EXPIRY_REMINDER_FINAL'
    END,
    'CONTRACT',
    contract.lease_contract_id,
    recipient.recipient_user_id,
    'PUSH',
    CASE reminder_stage.stage
        WHEN 'FIRST' THEN CONCAT('Hợp đồng ', contract.contract_code, ' sắp hết hạn')
        WHEN 'SECOND' THEN CONCAT('Bạn chưa phản hồi về hợp đồng ', contract.contract_code)
        ELSE CONCAT('Nhắc lần cuối về hợp đồng ', contract.contract_code)
    END,
    CASE reminder_stage.stage
        WHEN 'FIRST' THEN CONCAT('Phòng ', room.room_code, ' sẽ hết hạn vào ', contract.end_date,
                                 '. Bạn muốn gia hạn, chuyển phòng hay chuyển đi?')
        WHEN 'SECOND' THEN CONCAT('Vui lòng chọn ý định cho phòng ', room.name,
                                  ' trước ngày hết hạn ', contract.end_date,
                                  ' để quản lý sắp xếp kịp thời.')
        ELSE CONCAT('Hợp đồng phòng ', room.name, ' sắp hết hạn vào ', contract.end_date,
                    '. Vui lòng phản hồi để tránh chậm xử lý bàn giao hoặc gia hạn.')
    END,
    JSON_OBJECT(
        'contractId', contract.lease_contract_id,
        'contractCode', contract.contract_code,
        'roomId', room.room_id,
        'roomName', room.name,
        'roomCode', room.room_code,
        'endDate', contract.end_date,
        'daysRemaining', DATEDIFF(contract.end_date, DATE(@hdd1_seed_now)),
        'stage', reminder_stage.stage,
        'targetRoute', '/contract'
    ),
    'SENT', 0, 3,
    @hdd1_seed_now,
    @hdd1_seed_now,
    @hdd1_seed_now,
    FALSE
FROM hdbhms.lease_contracts contract
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
JOIN tmp_hdd1_khai_expiry_recipients recipient
  ON recipient.contract_id = contract.lease_contract_id
CROSS JOIN (
    SELECT 'FIRST' AS stage
    UNION ALL SELECT 'SECOND'
    UNION ALL SELECT 'FINAL'
) reminder_stage
WHERE room.property_id = @hdd1_property_id
  AND contract.primary_tenant_profile_id = @hdd1_khai_profile_id
  AND contract.status = 'EXPIRING_SOON'
  AND (
      (reminder_stage.stage = 'FIRST'
       AND DATE_SUB(contract.end_date, INTERVAL 3 MONTH) <= DATE(@hdd1_seed_now))
      OR (reminder_stage.stage = 'SECOND'
          AND DATE_SUB(contract.end_date, INTERVAL 2 MONTH) <= DATE(@hdd1_seed_now))
      OR (reminder_stage.stage = 'FINAL'
          AND DATE_SUB(contract.end_date, INTERVAL 1 MONTH) <= DATE(@hdd1_seed_now))
  );

DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_khai_expiry_recipients;
DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_khai_expiry_stages;
