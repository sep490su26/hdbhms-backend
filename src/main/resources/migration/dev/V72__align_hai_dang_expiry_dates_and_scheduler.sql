SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- Keep the Hai Dang expiry scenarios aligned with the August demo date.
-- Room 402 remains in the soon-vacant branch and is not an expired case.
SET @hdd1_property_id := (
    SELECT property_id
    FROM hdbhms.properties
    WHERE property_code = 'HAI_DANG_1'
      AND deleted_at IS NULL
    LIMIT 1
);
SET @hdd1_seed_now := '2026-08-01 09:00:00';

UPDATE hdbhms.lease_contracts contract
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
SET contract.end_date = '2026-08-30',
    contract.status = 'EXPIRING_SOON',
    contract.tenant_intention = NULL,
    contract.expected_vacant_date = NULL,
    contract.intention_recorded_at = NULL,
    contract.updated_at = @hdd1_seed_now
WHERE room.property_id = @hdd1_property_id
  AND room.room_code = '403'
  AND contract.contract_code = 'HDT_P403_01_10_2025'
  AND contract.end_date IN ('2026-09-30', '2026-08-30');

UPDATE hdbhms.lease_contracts contract
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
SET contract.end_date = '2026-08-30',
    contract.status = 'EXPIRING_SOON',
    contract.tenant_intention = NULL,
    contract.expected_vacant_date = NULL,
    contract.intention_recorded_at = NULL,
    contract.updated_at = @hdd1_seed_now
WHERE room.property_id = @hdd1_property_id
  AND room.room_code = '405'
  AND contract.contract_code = 'HDT_P405_01_11_2025'
  AND contract.end_date IN ('2026-10-31', '2026-08-30');

UPDATE hdbhms.rooms
SET public_note = CASE room_code
        WHEN '403' THEN 'Hợp đồng phòng 403 sắp hết hạn ngày 30/08/2026; chưa bắt đầu thanh lý.'
        WHEN '405' THEN 'Hợp đồng phòng 405 sắp hết hạn ngày 30/08/2026; chưa ghi nhận ý định của khách.'
        ELSE public_note
    END,
    updated_at = @hdd1_seed_now
WHERE property_id = @hdd1_property_id
  AND room_code IN ('403', '405');

-- Remove stale expiry notifications before rebuilding the three calendar stages.
DELETE delivery
FROM hdbhms.notification_deliveries delivery
JOIN hdbhms.notification_outbox notification
  ON notification.notification_outbox_id = delivery.outbox_id
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = notification.target_id
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
WHERE notification.target_type = 'CONTRACT'
  AND notification.event_type IN (
      'LEASE_EXPIRY_REMINDER_FIRST',
      'LEASE_EXPIRY_REMINDER_SECOND',
      'LEASE_EXPIRY_REMINDER_FINAL'
  )
  AND room.property_id = @hdd1_property_id
  AND room.room_code IN ('403', '405');

DELETE notification
FROM hdbhms.notification_outbox notification
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = notification.target_id
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
WHERE notification.target_type = 'CONTRACT'
  AND notification.event_type IN (
      'LEASE_EXPIRY_REMINDER_FIRST',
      'LEASE_EXPIRY_REMINDER_SECOND',
      'LEASE_EXPIRY_REMINDER_FINAL'
  )
  AND room.property_id = @hdd1_property_id
  AND room.room_code IN ('403', '405');

DELETE tracker
FROM hdbhms.reminder_trackers tracker
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = tracker.target_id
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
WHERE tracker.reminder_key = 'LEASE_EXPIRY_INTENTION'
  AND tracker.target_type = 'CONTRACT'
  AND room.property_id = @hdd1_property_id
  AND room.room_code IN ('403', '405');

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
    3,
    CONCAT(DATE_SUB(contract.end_date, INTERVAL 1 MONTH), ' 16:00:00'),
    NULL,
    JSON_OBJECT(
        'contractCode', contract.contract_code,
        'roomCode', room.room_code,
        'endDate', contract.end_date,
        'firstReminderDate', DATE_SUB(contract.end_date, INTERVAL 3 MONTH),
        'secondReminderDate', DATE_SUB(contract.end_date, INTERVAL 2 MONTH),
        'finalReminderDate', DATE_SUB(contract.end_date, INTERVAL 1 MONTH),
        'lastReminderStage', 'FINAL'
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
  AND room.room_code IN ('403', '405')
  AND contract.status = 'EXPIRING_SOON'
  AND contract.end_date = '2026-08-30';

INSERT INTO hdbhms.notification_outbox
    (event_type, target_type, target_id, recipient_user_id, channel, title, body, payload, status,
     retry_count, max_retries, scheduled_at, sent_at, created_at, is_read)
SELECT
    stage.event_type,
    'CONTRACT',
    contract.lease_contract_id,
    tenant_user.user_id,
    'PUSH',
    CONCAT('Nhắc hợp đồng phòng ', room.room_code),
    CONCAT(
        'Hợp đồng phòng ', room.room_code,
        ' sẽ hết hạn vào ', contract.end_date,
        '. Đây là lần nhắc ', stage.stage_label,
        ' theo lịch 3/2/1 tháng.'
    ),
    JSON_OBJECT(
        'contractId', contract.lease_contract_id,
        'contractCode', contract.contract_code,
        'roomId', room.room_id,
        'roomName', room.name,
        'roomCode', room.room_code,
        'propertyName', property.name,
        'endDate', contract.end_date,
        'daysRemaining', DATEDIFF(contract.end_date, DATE_SUB(contract.end_date, INTERVAL stage.months_before MONTH)),
        'stage', stage.stage_label,
        'targetRoute', '/contract'
    ),
    'SENT',
    0,
    3,
    CONCAT(DATE_SUB(contract.end_date, INTERVAL stage.months_before MONTH), ' 16:00:00'),
    CONCAT(DATE_SUB(contract.end_date, INTERVAL stage.months_before MONTH), ' 16:00:00'),
    CONCAT(DATE_SUB(contract.end_date, INTERVAL stage.months_before MONTH), ' 16:00:00'),
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
CROSS JOIN (
    SELECT 'FIRST' AS stage_label, 'LEASE_EXPIRY_REMINDER_FIRST' AS event_type, 3 AS months_before
    UNION ALL
    SELECT 'SECOND', 'LEASE_EXPIRY_REMINDER_SECOND', 2
    UNION ALL
    SELECT 'FINAL', 'LEASE_EXPIRY_REMINDER_FINAL', 1
) stage
WHERE room.property_id = @hdd1_property_id
  AND room.room_code IN ('403', '405')
  AND contract.status = 'EXPIRING_SOON'
  AND contract.end_date = '2026-08-30';

-- Existing databases must not keep the old 00:05 recurring schedule.
UPDATE hdbhms.scheduled_tasks
SET schedule_expression = 'DAILY:16:00',
    due_at = CASE
        WHEN TIME(CURRENT_TIMESTAMP) < '16:00:00'
            THEN CONCAT(CURRENT_DATE, ' 16:00:00')
        ELSE CONCAT(DATE_ADD(CURRENT_DATE, INTERVAL 1 DAY), ' 16:00:00')
    END
WHERE task_type = 'CONTRACT_LIFECYCLE_SCAN'
  AND target_type = 'SYSTEM_JOB'
  AND target_id = 0
  AND recurring = 1;
