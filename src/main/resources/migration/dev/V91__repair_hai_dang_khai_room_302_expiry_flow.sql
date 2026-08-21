SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- Repair databases that already ran V90. Room 301 is an active Khai
-- contract; room 302 is the only Khai room in the soon-vacant demo.
SET @hdd1_fix_property_id := (
    SELECT property_id
    FROM hdbhms.properties
    WHERE property_code = 'HAI_DANG_1'
      AND deleted_at IS NULL
    LIMIT 1
);
SET @hdd1_fix_now := '2026-08-20 09:00:00';
SET @hdd1_fix_owner_id := (
    SELECT user_id
    FROM hdbhms.users
    WHERE deleted_at IS NULL
      AND status = 'ACTIVE'
      AND role = 'OWNER'
    ORDER BY user_id
    LIMIT 1
);
SET @hdd1_fix_manager_id := (
    SELECT staff_user_id
    FROM hdbhms.property_staff_assignments
    WHERE property_id = @hdd1_fix_property_id
      AND assigned_role = 'MANAGER'
      AND assignment_status = 'ACTIVE'
      AND ended_at IS NULL
    ORDER BY is_primary DESC, property_staff_assignment_id
    LIMIT 1
);
SET @hdd1_fix_khai_profile_id := (
    SELECT profile.person_profile_id
    FROM hdbhms.person_profiles profile
    JOIN hdbhms.users user_account
      ON user_account.user_id = profile.user_id
    WHERE profile.deleted_at IS NULL
      AND user_account.deleted_at IS NULL
      AND (
          user_account.email IN ('nguyenvankhai95@gmail.com', 'nguyen.van.khai@haidang1.local')
          OR user_account.phone = '0918526407'
          OR profile.phone = '0918526407'
      )
    ORDER BY profile.person_profile_id
    LIMIT 1
);

UPDATE hdbhms.lease_contracts contract
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
SET contract.status = CASE room.room_code
        WHEN '301' THEN 'ACTIVE'
        WHEN '302' THEN 'EXPIRING_SOON'
    END,
    contract.end_date = CASE room.room_code
        WHEN '301' THEN '2026-12-31'
        WHEN '302' THEN '2026-10-31'
    END,
    contract.tenant_intention = NULL,
    contract.expected_vacant_date = NULL,
    contract.intention_recorded_at = NULL,
    contract.updated_at = @hdd1_fix_now
WHERE room.property_id = @hdd1_fix_property_id
  AND room.room_code IN ('301', '302')
  AND contract.primary_tenant_profile_id = @hdd1_fix_khai_profile_id
  AND contract.deleted_at IS NULL
  AND contract.status IN ('ACTIVE', 'EXPIRING_SOON', 'TERMINATION_PENDING');

UPDATE hdbhms.rooms room
SET room.current_status = CASE room.room_code
        WHEN '301' THEN 'OCCUPIED'
        WHEN '302' THEN 'SOON_VACANT'
    END,
    room.public_note = CASE room.room_code
        WHEN '301' THEN NULL
        WHEN '302' THEN 'Contract expires on 31/10/2026; tenant intention is pending.'
    END,
    room.internal_note = CASE room.room_code
        WHEN '301' THEN 'Active Khai contract; not part of the expiry demo.'
        WHEN '302' THEN 'Expiry demo: first reminder sent; second reminder due on 31/08/2026.'
    END,
    room.updated_at = @hdd1_fix_now
WHERE room.property_id = @hdd1_fix_property_id
  AND room.room_code IN ('301', '302')
  AND room.deleted_at IS NULL;

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
      'CONTRACT_EXPIRING_SOON_REVIEW',
      'LEASE_EXPIRY_REMINDER_FIRST',
      'LEASE_EXPIRY_REMINDER_SECOND',
      'LEASE_EXPIRY_REMINDER_FINAL'
  )
  AND room.property_id = @hdd1_fix_property_id
  AND room.room_code IN ('301', '302');

DELETE notification
FROM hdbhms.notification_outbox notification
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = notification.target_id
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
WHERE notification.target_type = 'CONTRACT'
  AND notification.event_type IN (
      'CONTRACT_EXPIRING_SOON_REVIEW',
      'LEASE_EXPIRY_REMINDER_FIRST',
      'LEASE_EXPIRY_REMINDER_SECOND',
      'LEASE_EXPIRY_REMINDER_FINAL'
  )
  AND room.property_id = @hdd1_fix_property_id
  AND room.room_code IN ('301', '302');

DELETE tracker
FROM hdbhms.reminder_trackers tracker
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = tracker.target_id
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
WHERE tracker.reminder_key = 'LEASE_EXPIRY_INTENTION'
  AND tracker.target_type = 'CONTRACT'
  AND room.property_id = @hdd1_fix_property_id
  AND room.room_code IN ('301', '302');

INSERT INTO hdbhms.notification_outbox
    (event_type, target_type, target_id, recipient_user_id, channel, title, body, payload, status,
     retry_count, max_retries, scheduled_at, sent_at, created_at, is_read)
SELECT
    'CONTRACT_EXPIRING_SOON_REVIEW',
    'CONTRACT',
    contract.lease_contract_id,
    recipient.recipient_user_id,
    channel.channel,
    CONCAT('Contract room ', room.room_code, ' is expiring soon'),
    CONCAT('Contract ', contract.contract_code, ' for room ', room.room_code,
           ' expires on ', contract.end_date, ' and tenant intention is pending.'),
    JSON_OBJECT(
        'contractId', contract.lease_contract_id,
        'contractCode', contract.contract_code,
        'roomId', room.room_id,
        'roomCode', room.room_code,
        'propertyName', property.name,
        'endDate', contract.end_date,
        'targetRoute', CONCAT('/dashboard/contracts/', contract.lease_contract_id)
    ),
    'SENT', 0, 3,
    @hdd1_fix_now, @hdd1_fix_now, @hdd1_fix_now, FALSE
FROM hdbhms.lease_contracts contract
JOIN hdbhms.rooms room ON room.room_id = contract.room_id
JOIN hdbhms.properties property ON property.property_id = room.property_id
CROSS JOIN (
    SELECT @hdd1_fix_owner_id AS recipient_user_id
    UNION
    SELECT @hdd1_fix_manager_id
) recipient
CROSS JOIN (SELECT 'WEB' AS channel UNION ALL SELECT 'PUSH') channel
WHERE room.property_id = @hdd1_fix_property_id
  AND room.room_code = '302'
  AND contract.status = 'EXPIRING_SOON'
  AND contract.tenant_intention IS NULL
  AND recipient.recipient_user_id IS NOT NULL;

DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_fix_recipients;
CREATE TEMPORARY TABLE tmp_hdd1_fix_recipients
(
    recipient_user_id BIGINT UNSIGNED NOT NULL,
    audience VARCHAR(50) NOT NULL,
    PRIMARY KEY (recipient_user_id, audience)
);

INSERT INTO tmp_hdd1_fix_recipients (recipient_user_id, audience)
SELECT user_account.user_id, 'PRIMARY_TENANT'
FROM hdbhms.lease_contracts contract
JOIN hdbhms.rooms room ON room.room_id = contract.room_id
JOIN hdbhms.person_profiles profile
  ON profile.person_profile_id = contract.primary_tenant_profile_id
JOIN hdbhms.users user_account ON user_account.user_id = profile.user_id
WHERE room.property_id = @hdd1_fix_property_id
  AND room.room_code = '302'
  AND contract.status = 'EXPIRING_SOON'
  AND user_account.status = 'ACTIVE'
  AND user_account.deleted_at IS NULL
UNION
SELECT user_account.user_id, 'CO_OCCUPANT'
FROM hdbhms.lease_contracts contract
JOIN hdbhms.rooms room ON room.room_id = contract.room_id
JOIN hdbhms.contract_occupants occupant
  ON occupant.contract_id = contract.lease_contract_id
 AND occupant.occupant_role = 'CO_OCCUPANT'
 AND occupant.status = 'ACTIVE'
JOIN hdbhms.person_profiles profile
  ON profile.person_profile_id = occupant.tenant_profile_id
JOIN hdbhms.users user_account ON user_account.user_id = profile.user_id
WHERE room.property_id = @hdd1_fix_property_id
  AND room.room_code = '302'
  AND contract.status = 'EXPIRING_SOON'
  AND user_account.status = 'ACTIVE'
  AND user_account.deleted_at IS NULL;

INSERT INTO hdbhms.notification_outbox
    (event_type, target_type, target_id, recipient_user_id, channel, title, body, payload, status,
     retry_count, max_retries, scheduled_at, sent_at, created_at, is_read)
SELECT
    'LEASE_EXPIRY_REMINDER_FIRST', 'CONTRACT', contract.lease_contract_id,
    recipient.recipient_user_id, 'PUSH',
    CONCAT('Contract ', contract.contract_code, ' expires soon'),
    CONCAT('Room ', room.room_code, ' expires on ', contract.end_date,
           '. Please choose renewal, transfer, or move-out.'),
    JSON_OBJECT(
        'contractId', contract.lease_contract_id,
        'contractCode', contract.contract_code,
        'roomId', room.room_id,
        'roomCode', room.room_code,
        'propertyName', property.name,
        'endDate', contract.end_date,
        'daysRemaining', DATEDIFF(contract.end_date, DATE(@hdd1_fix_now)),
        'stage', 'FIRST',
        'targetRoute', '/contract'
    ),
    'SENT', 0, 3,
    DATE_SUB(contract.end_date, INTERVAL 3 MONTH),
    DATE_SUB(contract.end_date, INTERVAL 3 MONTH),
    @hdd1_fix_now, FALSE
FROM hdbhms.lease_contracts contract
JOIN hdbhms.rooms room ON room.room_id = contract.room_id
JOIN hdbhms.properties property ON property.property_id = room.property_id
JOIN tmp_hdd1_fix_recipients recipient ON 1 = 1
WHERE room.property_id = @hdd1_fix_property_id
  AND room.room_code = '302'
  AND contract.status = 'EXPIRING_SOON'
  AND contract.tenant_intention IS NULL
  AND DATE_SUB(contract.end_date, INTERVAL 3 MONTH) <= DATE(@hdd1_fix_now);

INSERT INTO hdbhms.reminder_trackers
    (reminder_key, target_type, target_id, audience, recipient_user_id, status, sent_count,
     last_sent_at, next_due_at, metadata, created_at, updated_at)
SELECT
    'LEASE_EXPIRY_INTENTION', 'CONTRACT', contract.lease_contract_id,
    recipient.audience, recipient.recipient_user_id, 'ACTIVE', 1,
    DATE_SUB(contract.end_date, INTERVAL 3 MONTH),
    DATE_SUB(contract.end_date, INTERVAL 2 MONTH),
    JSON_OBJECT(
        'endDate', contract.end_date,
        'firstReminderDate', DATE_SUB(contract.end_date, INTERVAL 3 MONTH),
        'lastReminderStage', 'FIRST'
    ),
    @hdd1_fix_now, @hdd1_fix_now
FROM hdbhms.lease_contracts contract
JOIN hdbhms.rooms room ON room.room_id = contract.room_id
JOIN tmp_hdd1_fix_recipients recipient ON 1 = 1
WHERE room.property_id = @hdd1_fix_property_id
  AND room.room_code = '302'
  AND contract.status = 'EXPIRING_SOON'
  AND contract.tenant_intention IS NULL
  AND DATE_SUB(contract.end_date, INTERVAL 3 MONTH) <= DATE(@hdd1_fix_now);

DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_fix_recipients;
