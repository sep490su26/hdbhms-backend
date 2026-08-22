SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- Normalize notification history after the final demo seed. Historical rows
-- must be created before they are scheduled/sent, while future reminders stay
-- pending until their calendar milestone.
SET @hdd1_property_id := (
    SELECT property_id
    FROM hdbhms.properties
    WHERE property_code = 'HAI_DANG_1'
      AND deleted_at IS NULL
    LIMIT 1
);
SET @hdd1_seed_now := '2026-08-22 09:00:00';
SET @hdd1_owner_id := (
    SELECT user_id
    FROM hdbhms.users
    WHERE role = 'OWNER'
      AND status = 'ACTIVE'
      AND deleted_at IS NULL
    ORDER BY user_id
    LIMIT 1
);
SET @hdd1_manager_id := (
    SELECT staff_user_id
    FROM hdbhms.property_staff_assignments
    WHERE property_id = @hdd1_property_id
      AND assigned_role = 'MANAGER'
      AND assignment_status = 'ACTIVE'
      AND ended_at IS NULL
    ORDER BY is_primary DESC, property_staff_assignment_id
    LIMIT 1
);

-- Rebuild the three Khai expiry scenarios from current active occupants. The
-- older overlays created duplicate rows and retained the former primary
-- occupants of room 402.
DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_expiry_contracts;
CREATE TEMPORARY TABLE tmp_hdd1_expiry_contracts
(
    contract_id BIGINT UNSIGNED NOT NULL PRIMARY KEY
);

INSERT INTO tmp_hdd1_expiry_contracts (contract_id)
SELECT contract.lease_contract_id
FROM hdbhms.lease_contracts contract
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
WHERE room.property_id = @hdd1_property_id
  AND room.room_code IN ('302', '303', '402')
  AND contract.status = 'EXPIRING_SOON'
  AND contract.tenant_intention IS NULL
  AND contract.deleted_at IS NULL;

DELETE delivery
FROM hdbhms.notification_deliveries delivery
JOIN hdbhms.notification_outbox notification
  ON notification.notification_outbox_id = delivery.outbox_id
JOIN tmp_hdd1_expiry_contracts selected_contract
  ON selected_contract.contract_id = notification.target_id
WHERE notification.target_type = 'CONTRACT'
  AND notification.event_type IN (
      'CONTRACT_EXPIRING_SOON_REVIEW',
      'LEASE_EXPIRY_REMINDER_FIRST',
      'LEASE_EXPIRY_REMINDER_SECOND',
      'LEASE_EXPIRY_REMINDER_FINAL'
  );

DELETE notification
FROM hdbhms.notification_outbox notification
JOIN tmp_hdd1_expiry_contracts selected_contract
  ON selected_contract.contract_id = notification.target_id
WHERE notification.target_type = 'CONTRACT'
  AND notification.event_type IN (
      'CONTRACT_EXPIRING_SOON_REVIEW',
      'LEASE_EXPIRY_REMINDER_FIRST',
      'LEASE_EXPIRY_REMINDER_SECOND',
      'LEASE_EXPIRY_REMINDER_FINAL'
  );

DELETE tracker
FROM hdbhms.reminder_trackers tracker
JOIN tmp_hdd1_expiry_contracts selected_contract
  ON selected_contract.contract_id = tracker.target_id
WHERE tracker.reminder_key = 'LEASE_EXPIRY_INTENTION'
  AND tracker.target_type = 'CONTRACT';

DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_expiry_recipients;
CREATE TEMPORARY TABLE tmp_hdd1_expiry_recipients
(
    contract_id       BIGINT UNSIGNED NOT NULL,
    recipient_user_id BIGINT UNSIGNED NOT NULL,
    audience           VARCHAR(30) NOT NULL,
    PRIMARY KEY (contract_id, recipient_user_id, audience)
);

INSERT INTO tmp_hdd1_expiry_recipients
    (contract_id, recipient_user_id, audience)
SELECT contract.lease_contract_id, profile.user_id, 'PRIMARY_TENANT'
FROM hdbhms.lease_contracts contract
JOIN tmp_hdd1_expiry_contracts selected_contract
  ON selected_contract.contract_id = contract.lease_contract_id
JOIN hdbhms.person_profiles profile
  ON profile.person_profile_id = contract.primary_tenant_profile_id
 AND profile.deleted_at IS NULL
JOIN hdbhms.users user_account
  ON user_account.user_id = profile.user_id
 AND user_account.status = 'ACTIVE'
 AND user_account.deleted_at IS NULL;

INSERT INTO tmp_hdd1_expiry_recipients
    (contract_id, recipient_user_id, audience)
SELECT contract.lease_contract_id, profile.user_id, 'CO_OCCUPANT'
FROM hdbhms.lease_contracts contract
JOIN tmp_hdd1_expiry_contracts selected_contract
  ON selected_contract.contract_id = contract.lease_contract_id
JOIN hdbhms.contract_occupants occupant
  ON occupant.contract_id = contract.lease_contract_id
 AND occupant.occupant_role = 'CO_OCCUPANT'
 AND occupant.status = 'ACTIVE'
JOIN hdbhms.person_profiles profile
  ON profile.person_profile_id = occupant.tenant_profile_id
 AND profile.deleted_at IS NULL
JOIN hdbhms.users user_account
  ON user_account.user_id = profile.user_id
 AND user_account.status = 'ACTIVE'
 AND user_account.deleted_at IS NULL;

DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_expiry_stages;
CREATE TEMPORARY TABLE tmp_hdd1_expiry_stages
(
    contract_id  BIGINT UNSIGNED NOT NULL,
    stage_name   VARCHAR(10) NOT NULL,
    event_type   VARCHAR(100) NOT NULL,
    scheduled_at DATETIME(6) NOT NULL,
    PRIMARY KEY (contract_id, stage_name)
);

INSERT INTO tmp_hdd1_expiry_stages
    (contract_id, stage_name, event_type, scheduled_at)
SELECT contract.lease_contract_id,
       stage.stage_name,
       stage.event_type,
       CAST(CONCAT(
           DATE_SUB(contract.end_date, INTERVAL stage.months_before MONTH),
           ' 09:00:00'
       ) AS DATETIME(6))
FROM hdbhms.lease_contracts contract
JOIN tmp_hdd1_expiry_contracts selected_contract
  ON selected_contract.contract_id = contract.lease_contract_id
CROSS JOIN (
    SELECT 'FIRST' AS stage_name, 'LEASE_EXPIRY_REMINDER_FIRST' AS event_type, 3 AS months_before
    UNION ALL
    SELECT 'SECOND', 'LEASE_EXPIRY_REMINDER_SECOND', 2
    UNION ALL
    SELECT 'FINAL', 'LEASE_EXPIRY_REMINDER_FINAL', 1
) stage;

-- Seed all tenant reminders, not just reminders that have already fired. A
-- future reminder is a real scheduled record, so it must not look sent yet.
INSERT INTO hdbhms.notification_outbox
    (event_type, target_type, target_id, recipient_user_id, channel, title, body,
     payload, status, retry_count, max_retries, scheduled_at, sent_at,
     created_at, is_read, read_at)
SELECT stage.event_type,
       'CONTRACT',
       contract.lease_contract_id,
       recipient.recipient_user_id,
       'PUSH',
       CASE stage.stage_name
           WHEN 'FIRST' THEN CONCAT('Hợp đồng ', contract.contract_code, ' sắp hết hạn')
           WHEN 'SECOND' THEN CONCAT('Bạn chưa phản hồi về hợp đồng ', contract.contract_code)
           ELSE CONCAT('Nhắc lần cuối về hợp đồng ', contract.contract_code)
       END,
       CASE stage.stage_name
           WHEN 'FIRST' THEN CONCAT(
               'Phòng ', room.room_code, ' sẽ hết hạn hợp đồng vào ngày ',
               DATE_FORMAT(contract.end_date, '%d/%m/%Y'),
               '. Vui lòng chọn gia hạn, chuyển phòng hoặc chuyển đi.'
           )
           WHEN 'SECOND' THEN CONCAT(
               'Bạn chưa phản hồi về hợp đồng phòng ', room.room_code,
               '. Vui lòng chọn ý định trước ngày ',
               DATE_FORMAT(contract.end_date, '%d/%m/%Y'), '.'
           )
           ELSE CONCAT(
               'Hợp đồng phòng ', room.room_code, ' sắp hết hạn vào ngày ',
               DATE_FORMAT(contract.end_date, '%d/%m/%Y'),
               '. Vui lòng phản hồi để tránh chậm xử lý bàn giao.'
           )
       END,
       JSON_OBJECT(
           'contractId', contract.lease_contract_id,
           'contractCode', contract.contract_code,
           'roomId', room.room_id,
           'roomName', room.name,
           'roomCode', room.room_code,
           'propertyName', property.name,
           'endDate', contract.end_date,
           'daysRemaining', DATEDIFF(contract.end_date, DATE(stage.scheduled_at)),
           'stage', stage.stage_name,
           'targetRoute', '/contract'
       ),
       CASE WHEN stage.scheduled_at <= @hdd1_seed_now THEN 'SENT' ELSE 'PENDING' END,
       0,
       3,
       stage.scheduled_at,
       CASE WHEN stage.scheduled_at <= @hdd1_seed_now THEN stage.scheduled_at ELSE NULL END,
       CASE
           WHEN stage.scheduled_at <= @hdd1_seed_now
               THEN DATE_SUB(stage.scheduled_at, INTERVAL 5 MINUTE)
           ELSE @hdd1_seed_now
       END,
       FALSE,
       NULL
FROM hdbhms.lease_contracts contract
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
JOIN hdbhms.properties property
  ON property.property_id = room.property_id
JOIN tmp_hdd1_expiry_recipients recipient
  ON recipient.contract_id = contract.lease_contract_id
JOIN tmp_hdd1_expiry_stages stage
  ON stage.contract_id = contract.lease_contract_id;

-- Keep the reminder tracker aligned with the same calendar. After the final
-- reminder, next_due_at is the next day because that is when deposit-forfeiture
-- processing is allowed to run.
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
    SUM(stage.scheduled_at <= @hdd1_seed_now),
    MAX(CASE WHEN stage.scheduled_at <= @hdd1_seed_now THEN stage.scheduled_at ELSE NULL END),
    CASE
        WHEN SUM(stage.scheduled_at <= @hdd1_seed_now) >= 3
            THEN DATE_ADD(MAX(stage.scheduled_at), INTERVAL 1 DAY)
        ELSE MIN(CASE WHEN stage.scheduled_at > @hdd1_seed_now THEN stage.scheduled_at ELSE NULL END)
    END,
    JSON_OBJECT(
        'contractCode', contract.contract_code,
        'roomCode', room.room_code,
        'endDate', contract.end_date,
        'firstReminderDate', MIN(CASE WHEN stage.stage_name = 'FIRST' THEN DATE(stage.scheduled_at) END),
        'secondReminderDate', MIN(CASE WHEN stage.stage_name = 'SECOND' THEN DATE(stage.scheduled_at) END),
        'finalReminderDate', MIN(CASE WHEN stage.stage_name = 'FINAL' THEN DATE(stage.scheduled_at) END),
        'lastReminderStage', CASE
            WHEN SUM(stage.scheduled_at <= @hdd1_seed_now) >= 3 THEN 'FINAL'
            WHEN SUM(stage.scheduled_at <= @hdd1_seed_now) = 2 THEN 'SECOND'
            WHEN SUM(stage.scheduled_at <= @hdd1_seed_now) = 1 THEN 'FIRST'
            ELSE 'PENDING'
        END
    ),
    CASE
        WHEN MIN(stage.scheduled_at) <= @hdd1_seed_now
            THEN DATE_SUB(MIN(stage.scheduled_at), INTERVAL 5 MINUTE)
        ELSE @hdd1_seed_now
    END,
    CASE
        WHEN MAX(CASE WHEN stage.scheduled_at <= @hdd1_seed_now THEN stage.scheduled_at ELSE NULL END) IS NOT NULL
            THEN DATE_ADD(MAX(CASE WHEN stage.scheduled_at <= @hdd1_seed_now THEN stage.scheduled_at ELSE NULL END), INTERVAL 5 MINUTE)
        ELSE @hdd1_seed_now
    END
FROM hdbhms.lease_contracts contract
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
JOIN tmp_hdd1_expiry_recipients recipient
  ON recipient.contract_id = contract.lease_contract_id
JOIN tmp_hdd1_expiry_stages stage
  ON stage.contract_id = contract.lease_contract_id
GROUP BY contract.lease_contract_id, contract.contract_code, contract.end_date,
         room.room_code, recipient.audience, recipient.recipient_user_id;

DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_management_recipients;
CREATE TEMPORARY TABLE tmp_hdd1_management_recipients
(
    recipient_user_id BIGINT UNSIGNED NOT NULL PRIMARY KEY
);

INSERT INTO tmp_hdd1_management_recipients (recipient_user_id)
SELECT @hdd1_owner_id
WHERE @hdd1_owner_id IS NOT NULL
UNION
SELECT @hdd1_manager_id
WHERE @hdd1_manager_id IS NOT NULL;

-- Management review is recorded when the latest reminder milestone becomes
-- due, so its history no longer appears after the reminder it refers to.
INSERT INTO hdbhms.notification_outbox
    (event_type, target_type, target_id, recipient_user_id, channel, title, body,
     payload, status, retry_count, max_retries, scheduled_at, sent_at,
     created_at, is_read, read_at)
SELECT 'CONTRACT_EXPIRING_SOON_REVIEW',
       'CONTRACT',
       contract.lease_contract_id,
       management_recipient.recipient_user_id,
       notification_channel.channel,
       CONCAT('Hợp đồng phòng ', room.room_code, ' sắp hết hạn'),
       CONCAT(
           'Hợp đồng ', contract.contract_code, ' của phòng ', room.room_code,
           ' sẽ hết hạn vào ngày ', DATE_FORMAT(contract.end_date, '%d/%m/%Y'),
           '. Vui lòng theo dõi và xử lý khi người thuê phản hồi.'
       ),
       JSON_OBJECT(
           'contractId', contract.lease_contract_id,
           'contractCode', contract.contract_code,
           'roomId', room.room_id,
           'roomName', room.name,
           'roomCode', room.room_code,
           'propertyName', property.name,
           'endDate', contract.end_date,
           'targetRoute', CONCAT('/dashboard/contracts/', contract.lease_contract_id)
       ),
       'SENT',
       0,
       3,
       review.review_at,
       review.review_at,
       DATE_SUB(review.review_at, INTERVAL 5 MINUTE),
       FALSE,
       NULL
FROM hdbhms.lease_contracts contract
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
JOIN hdbhms.properties property
  ON property.property_id = room.property_id
JOIN tmp_hdd1_expiry_contracts selected_contract
  ON selected_contract.contract_id = contract.lease_contract_id
JOIN (
    SELECT contract_id, MAX(scheduled_at) AS review_at
    FROM tmp_hdd1_expiry_stages
    WHERE scheduled_at <= @hdd1_seed_now
    GROUP BY contract_id
) review
  ON review.contract_id = contract.lease_contract_id
CROSS JOIN tmp_hdd1_management_recipients management_recipient
CROSS JOIN (
    SELECT 'WEB' AS channel
    UNION ALL
    SELECT 'PUSH'
) notification_channel;

-- Correct older seeded invoice/contract rows too. This only moves a creation
-- timestamp that was written after its scheduled time; it does not rewrite a
-- valid user-generated history.
UPDATE hdbhms.notification_outbox notification
LEFT JOIN hdbhms.lease_contracts contract
  ON notification.target_type = 'CONTRACT'
 AND notification.target_id = contract.lease_contract_id
LEFT JOIN hdbhms.rooms contract_room
  ON contract_room.room_id = contract.room_id
LEFT JOIN hdbhms.invoices invoice
  ON notification.target_type = 'INVOICE'
 AND notification.target_id = invoice.invoice_id
SET notification.created_at = CASE
        WHEN notification.scheduled_at IS NOT NULL
            THEN DATE_SUB(notification.scheduled_at, INTERVAL 5 MINUTE)
        WHEN notification.sent_at IS NOT NULL
            THEN DATE_SUB(notification.sent_at, INTERVAL 5 MINUTE)
        ELSE notification.created_at
    END,
    notification.sent_at = CASE
        WHEN notification.sent_at IS NOT NULL
         AND notification.scheduled_at IS NOT NULL
         AND notification.sent_at < notification.scheduled_at
            THEN DATE_ADD(notification.scheduled_at, INTERVAL 5 SECOND)
        ELSE notification.sent_at
    END
WHERE (
       contract_room.property_id = @hdd1_property_id
    OR invoice.property_id = @hdd1_property_id
)
  AND notification.created_at > COALESCE(notification.scheduled_at, notification.sent_at)
   OR (
       (
              contract_room.property_id = @hdd1_property_id
           OR invoice.property_id = @hdd1_property_id
       )
       AND notification.sent_at IS NOT NULL
       AND notification.sent_at < notification.scheduled_at
   );

-- Seed the receive timestamp for every sent notification that belongs to the
-- property. Keep unread notifications unread; the delivery row represents
-- receipt, while read_at is only populated after a user opens it.
INSERT INTO hdbhms.notification_deliveries
    (outbox_id, provider_message_id, delivery_status, delivered_at, read_at, created_at)
SELECT notification.notification_outbox_id,
       CONCAT('seed-delivery-', notification.notification_outbox_id),
       'SENT',
       DATE_ADD(notification.sent_at, INTERVAL 2 MINUTE),
       CASE
           WHEN notification.is_read = TRUE
               THEN COALESCE(notification.read_at, DATE_ADD(notification.sent_at, INTERVAL 7 MINUTE))
           ELSE NULL
       END,
       DATE_ADD(notification.sent_at, INTERVAL 2 MINUTE)
FROM hdbhms.notification_outbox notification
LEFT JOIN hdbhms.notification_deliveries existing_delivery
  ON existing_delivery.outbox_id = notification.notification_outbox_id
LEFT JOIN hdbhms.lease_contracts contract
  ON notification.target_type = 'CONTRACT'
 AND notification.target_id = contract.lease_contract_id
LEFT JOIN hdbhms.rooms contract_room
  ON contract_room.room_id = contract.room_id
LEFT JOIN hdbhms.invoices invoice
  ON notification.target_type = 'INVOICE'
 AND notification.target_id = invoice.invoice_id
WHERE existing_delivery.notification_delivery_id IS NULL
  AND notification.status = 'SENT'
  AND notification.sent_at IS NOT NULL
  AND (
         contract_room.property_id = @hdd1_property_id
      OR invoice.property_id = @hdd1_property_id
  );

-- Existing delivery rows must never claim receipt before the outbox send.
UPDATE hdbhms.notification_deliveries delivery
JOIN hdbhms.notification_outbox notification
  ON notification.notification_outbox_id = delivery.outbox_id
LEFT JOIN hdbhms.lease_contracts contract
  ON notification.target_type = 'CONTRACT'
 AND notification.target_id = contract.lease_contract_id
LEFT JOIN hdbhms.rooms contract_room
  ON contract_room.room_id = contract.room_id
LEFT JOIN hdbhms.invoices invoice
  ON notification.target_type = 'INVOICE'
 AND notification.target_id = invoice.invoice_id
SET delivery.delivered_at = CASE
        WHEN delivery.delivered_at IS NULL OR delivery.delivered_at < notification.sent_at
            THEN DATE_ADD(notification.sent_at, INTERVAL 2 MINUTE)
        ELSE delivery.delivered_at
    END,
    delivery.created_at = CASE
        WHEN delivery.created_at < notification.sent_at
            THEN DATE_ADD(notification.sent_at, INTERVAL 2 MINUTE)
        ELSE delivery.created_at
    END,
    delivery.read_at = CASE
        WHEN delivery.read_at IS NOT NULL AND delivery.read_at < notification.sent_at
            THEN DATE_ADD(notification.sent_at, INTERVAL 7 MINUTE)
        ELSE delivery.read_at
    END
WHERE notification.status = 'SENT'
  AND notification.sent_at IS NOT NULL
  AND (
         contract_room.property_id = @hdd1_property_id
      OR invoice.property_id = @hdd1_property_id
  );

DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_expiry_contracts;
DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_expiry_recipients;
DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_expiry_stages;
DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_management_recipients;
