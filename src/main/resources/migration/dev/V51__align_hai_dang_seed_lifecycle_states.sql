SET NAMES utf8mb4;

-- Backfill lifecycle relationships for the Hai Dang demo after V42/V48.
SET @property_id := (
    SELECT property_id
    FROM hdbhms.properties
    WHERE property_code = 'HAI_DANG_1'
      AND deleted_at IS NULL
    LIMIT 1
);

SET @manager_id := (
    SELECT user_id
    FROM hdbhms.users
    WHERE email = 'seed.manager@hdbhms.local'
      AND deleted_at IS NULL
    LIMIT 1
);

SET @tenant_user_id := (
    SELECT user_id
    FROM hdbhms.users
    WHERE email IN (
        'nguyen.van.minh@haidang1.local',
        'seed.tenant@hdbhms.local'
    )
      AND deleted_at IS NULL
    ORDER BY email = 'nguyen.van.minh@haidang1.local' DESC
    LIMIT 1
);

SET @lease_signed_file := (
    SELECT file_metadata_id
    FROM hdbhms.file_metadata
    WHERE storage_key = 'seed-demo/files/lease-signed-401.pdf'
    LIMIT 1
);

SET @receipt_file := (
    SELECT file_metadata_id
    FROM hdbhms.file_metadata
    WHERE storage_key = 'seed-demo/files/receipt-301.pdf'
    LIMIT 1
);

SET @r402 := (SELECT room_id FROM hdbhms.rooms WHERE property_id = @property_id AND room_code = '402' LIMIT 1);
SET @r403 := (SELECT room_id FROM hdbhms.rooms WHERE property_id = @property_id AND room_code = '403' LIMIT 1);
SET @r401 := (SELECT room_id FROM hdbhms.rooms WHERE property_id = @property_id AND room_code = '401' LIMIT 1);
SET @r301 := (SELECT room_id FROM hdbhms.rooms WHERE property_id = @property_id AND room_code = '301' LIMIT 1);
SET @r501 := (SELECT room_id FROM hdbhms.rooms WHERE property_id = @property_id AND room_code = '501' LIMIT 1);
SET @r502 := (SELECT room_id FROM hdbhms.rooms WHERE property_id = @property_id AND room_code = '502' LIMIT 1);
SET @r503 := (SELECT room_id FROM hdbhms.rooms WHERE property_id = @property_id AND room_code = '503' LIMIT 1);
SET @r504 := (SELECT room_id FROM hdbhms.rooms WHERE property_id = @property_id AND room_code = '504' LIMIT 1);
SET @r505 := (SELECT room_id FROM hdbhms.rooms WHERE property_id = @property_id AND room_code = '505' LIMIT 1);
SET @r506 := (SELECT room_id FROM hdbhms.rooms WHERE property_id = @property_id AND room_code = '506' LIMIT 1);

SET @c402 := (SELECT lease_contract_id FROM hdbhms.lease_contracts WHERE contract_code = 'HD-HDD1-402-2026' LIMIT 1);
SET @c403 := (SELECT lease_contract_id FROM hdbhms.lease_contracts WHERE contract_code = 'HD-HDD1-403-2026' LIMIT 1);
SET @c501 := (SELECT lease_contract_id FROM hdbhms.lease_contracts WHERE contract_code = 'HD-HDD1-501-2026' LIMIT 1);
SET @c502 := (SELECT lease_contract_id FROM hdbhms.lease_contracts WHERE contract_code = 'HD-HDD1-502-2026' LIMIT 1);
SET @c503 := (SELECT lease_contract_id FROM hdbhms.lease_contracts WHERE contract_code = 'HD-HDD1-503-2026' LIMIT 1);
SET @c504 := (SELECT lease_contract_id FROM hdbhms.lease_contracts WHERE contract_code = 'HD-HDD1-504-2026' LIMIT 1);
SET @c505 := (SELECT lease_contract_id FROM hdbhms.lease_contracts WHERE contract_code = 'HD-HDD1-505-2026' LIMIT 1);
SET @c506 := (SELECT lease_contract_id FROM hdbhms.lease_contracts WHERE contract_code = 'HD-HDD1-506-2026' LIMIT 1);
SET @c301 := (SELECT lease_contract_id FROM hdbhms.lease_contracts WHERE contract_code = 'HD-HDD1-301-2026' LIMIT 1);

UPDATE hdbhms.rooms
SET current_status = CASE room_code
        WHEN '402' THEN 'OCCUPIED'
        WHEN '403' THEN 'SOON_VACANT'
        ELSE current_status
    END,
    public_note = CASE room_code
        WHEN '402' THEN 'Seed: Hợp đồng sắp hết hạn ngày 2026-08-15, đang chờ khách phản hồi ý định.'
        WHEN '403' THEN 'Seed: Đang xử lý thanh lý, phòng sẽ trống sau khi hoàn tất bàn giao.'
        ELSE public_note
    END,
    updated_at = '2026-07-30 09:00:00'
WHERE property_id = @property_id
  AND room_code IN ('402', '403')
  AND deleted_at IS NULL;

UPDATE hdbhms.lease_contracts
SET tenant_intention = NULL,
    expected_vacant_date = NULL,
    intention_recorded_at = NULL,
    status = 'EXPIRING_SOON',
    updated_at = '2026-07-30 09:00:00'
WHERE lease_contract_id = @c402;

UPDATE hdbhms.lease_contracts
SET status = 'TERMINATION_PENDING',
    tenant_intention = 'MOVE_OUT',
    expected_vacant_date = '2026-07-31',
    intention_recorded_at = '2026-07-30 08:00:00',
    updated_at = '2026-07-30 09:00:00'
WHERE lease_contract_id = @c403;

-- A completed transfer keeps the source contract detached from expiry intention tracking.
UPDATE hdbhms.lease_contracts
SET tenant_intention = NULL,
    expected_vacant_date = NULL,
    intention_recorded_at = NULL,
    updated_at = '2026-07-30 09:00:00'
WHERE lease_contract_id = @c505;

-- Transfer-generated contracts must mirror the service-created contract chain.
UPDATE hdbhms.lease_contracts
SET previous_contract_id = @c501,
    monthly_rent = (SELECT listed_price FROM hdbhms.rooms WHERE room_id = @r502),
    status = 'CONFIRMED',
    signed_file_id = NULL,
    signed_uploaded_by = NULL,
    signed_at = NULL,
    updated_at = '2026-07-30 10:00:00'
WHERE lease_contract_id = @c502;

UPDATE hdbhms.lease_contracts
SET previous_contract_id = @c503,
    monthly_rent = (SELECT listed_price FROM hdbhms.rooms WHERE room_id = @r504),
    status = 'SIGNED',
    signed_file_id = @lease_signed_file,
    signed_uploaded_by = @manager_id,
    signed_at = '2026-07-30 11:30:00',
    updated_at = '2026-07-30 11:30:00'
WHERE lease_contract_id = @c504;

UPDATE hdbhms.lease_contracts
SET previous_contract_id = @c505,
    monthly_rent = (SELECT listed_price FROM hdbhms.rooms WHERE room_id = @r506),
    status = 'ACTIVE',
    signed_file_id = @lease_signed_file,
    signed_uploaded_by = @manager_id,
    signed_at = '2026-07-20 09:00:00',
    updated_at = '2026-07-20 09:00:00'
WHERE lease_contract_id = @c506;

-- Room 301 is already liquidated, so its required confirmed move-out handover must exist.
SET @m301e := (
    SELECT meter_id FROM hdbhms.meters
    WHERE room_id = @r301 AND meter_type = 'ELECTRICITY' AND status = 'ACTIVE'
    ORDER BY meter_id LIMIT 1
);
SET @m301w := (
    SELECT meter_id FROM hdbhms.meters
    WHERE room_id = @r301 AND meter_type = 'WATER' AND status = 'ACTIVE'
    ORDER BY meter_id LIMIT 1
);
SET @mr301e := (
    SELECT meter_reading_id FROM hdbhms.meter_readings
    WHERE meter_id = @m301e AND reading_period = '2026-07' AND status = 'CONFIRMED'
    ORDER BY meter_reading_id DESC LIMIT 1
);
SET @mr301w := (
    SELECT meter_reading_id FROM hdbhms.meter_readings
    WHERE meter_id = @m301w AND reading_period = '2026-07' AND status = 'CONFIRMED'
    ORDER BY meter_reading_id DESC LIMIT 1
);

INSERT INTO hdbhms.contract_handover_records
    (contract_id, room_id, handover_type, handover_date, electricity_reading_id, water_reading_id,
     note, status, confirmed_by, confirmed_at, created_at, signed_document_id)
SELECT
    @c301, @r301, 'MOVE_OUT', '2026-07-25 09:30:00', @mr301e, @mr301w,
    'Seed completed move-out handover.', 'CONFIRMED', @manager_id,
    '2026-07-25 09:35:00', '2026-07-25 09:30:00', @receipt_file
WHERE @c301 IS NOT NULL
  AND @mr301e IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM hdbhms.contract_handover_records
      WHERE contract_id = @c301
        AND handover_type = 'MOVE_OUT'
  );

-- The activation workflow requires an electricity reading and signed handover document.
SET @m401e := (
    SELECT meter_id FROM hdbhms.meters
    WHERE room_id = @r401 AND meter_type = 'ELECTRICITY' AND status = 'ACTIVE'
    ORDER BY meter_id LIMIT 1
);

INSERT INTO hdbhms.meter_readings
    (batch_id, meter_id, room_id, reading_period, revision_no, previous_value, current_value,
     reading_date, photo_file_id, status, void_reason, created_by, created_at, purpose, source,
     review_status, review_count)
SELECT
    NULL, @m401e, @r401, '2026-01', 1, 0, 4010, '2026-01-01', NULL, 'CONFIRMED', NULL,
    @manager_id, '2026-01-01 09:00:00', 'HANDOVER', 'MANUAL', 'NONE', 0
WHERE @m401e IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM hdbhms.meter_readings
      WHERE meter_id = @m401e
        AND reading_period = '2026-01'
        AND status = 'CONFIRMED'
  );

SET @mr401e := (
    SELECT meter_reading_id FROM hdbhms.meter_readings
    WHERE meter_id = @m401e AND reading_period = '2026-01' AND status = 'CONFIRMED'
    ORDER BY meter_reading_id DESC LIMIT 1
);

UPDATE hdbhms.contract_handover_records
SET electricity_reading_id = @mr401e,
    status = 'CONFIRMED',
    confirmed_by = @manager_id,
    confirmed_at = '2026-01-01 09:35:00'
WHERE contract_id = (
        SELECT lease_contract_id
        FROM hdbhms.lease_contracts
        WHERE contract_code = 'HD-HDD1-401-2026'
        LIMIT 1
    )
  AND handover_type = 'MOVE_IN';

-- Transfer execution requires a confirmed handover with an electricity reading.
SET @mr503e := (
    SELECT mr.meter_reading_id
    FROM hdbhms.meter_readings mr
    JOIN hdbhms.meters meter ON meter.meter_id = mr.meter_id
    WHERE mr.room_id = @r503
      AND meter.meter_type = 'ELECTRICITY'
      AND mr.reading_period = '2026-07'
      AND mr.status = 'CONFIRMED'
    ORDER BY mr.meter_reading_id DESC
    LIMIT 1
);
SET @mr505e := (
    SELECT mr.meter_reading_id
    FROM hdbhms.meter_readings mr
    JOIN hdbhms.meters meter ON meter.meter_id = mr.meter_id
    WHERE mr.room_id = @r505
      AND meter.meter_type = 'ELECTRICITY'
      AND mr.reading_period = '2026-07'
      AND mr.status = 'CONFIRMED'
    ORDER BY mr.meter_reading_id DESC
    LIMIT 1
);
SET @mr506e := (
    SELECT mr.meter_reading_id
    FROM hdbhms.meter_readings mr
    JOIN hdbhms.meters meter ON meter.meter_id = mr.meter_id
    WHERE mr.room_id = @r506
      AND meter.meter_type = 'ELECTRICITY'
      AND mr.reading_period = '2026-07'
      AND mr.status = 'CONFIRMED'
    ORDER BY mr.meter_reading_id DESC
    LIMIT 1
);

UPDATE hdbhms.contract_handover_records
SET electricity_reading_id = @mr503e
WHERE contract_id = @c503
  AND handover_type = 'TRANSFER_OUT';

UPDATE hdbhms.contract_handover_records
SET electricity_reading_id = @mr505e
WHERE contract_id = @c505
  AND handover_type = 'TRANSFER_OUT';

UPDATE hdbhms.contract_handover_records
SET electricity_reading_id = @mr506e
WHERE contract_id = @c506
  AND handover_type = 'TRANSFER_IN';

-- Remove the obsolete 402 handover branch and recreate the tenant reminder branch.
DELETE delivery
FROM hdbhms.notification_deliveries delivery
JOIN hdbhms.notification_outbox n
    ON n.notification_outbox_id = delivery.outbox_id
JOIN hdbhms.manager_tasks task ON task.manager_task_id = n.target_id
WHERE n.target_type = 'MANAGER_TASK'
  AND n.event_type = 'LEASE_HANDOVER_CONFIRMATION_DUE'
  AND task.lease_contract_id = @c402;

DELETE n
FROM hdbhms.notification_outbox n
JOIN hdbhms.manager_tasks task ON task.manager_task_id = n.target_id
WHERE n.target_type = 'MANAGER_TASK'
  AND n.event_type = 'LEASE_HANDOVER_CONFIRMATION_DUE'
  AND task.lease_contract_id = @c402;

DELETE FROM hdbhms.reminder_trackers
WHERE target_type = 'CONTRACT'
  AND target_id = @c402
  AND reminder_key IN ('LEASE_HANDOVER_CONFIRMATION', 'LEASE_EXPIRY_INTENTION');

DELETE FROM hdbhms.manager_tasks
WHERE lease_contract_id = @c402
  AND task_type = 'LEASE_HANDOVER_CONFIRMATION';

INSERT INTO hdbhms.reminder_trackers
    (reminder_key, target_type, target_id, audience, recipient_user_id, status, sent_count,
     last_sent_at, next_due_at, metadata, created_at, updated_at)
VALUES
    ('LEASE_EXPIRY_INTENTION', 'CONTRACT', @c402, 'PRIMARY_TENANT', @tenant_user_id, 'ACTIVE', 1,
     '2026-07-30 09:00:00', '2026-08-29 09:00:00',
     JSON_OBJECT('endDate', '2026-08-15', 'firstReminderDate', '2026-05-15', 'lastReminderStage', 'FIRST'),
     '2026-07-30 09:00:00', '2026-07-30 09:00:00');

DELETE delivery
FROM hdbhms.notification_deliveries delivery
JOIN hdbhms.notification_outbox n
    ON n.notification_outbox_id = delivery.outbox_id
WHERE n.event_type = 'LEASE_EXPIRY_REMINDER_FIRST'
  AND n.target_type = 'CONTRACT'
  AND n.target_id = @c402
  AND n.recipient_user_id = @tenant_user_id;

DELETE FROM hdbhms.notification_outbox
WHERE event_type = 'LEASE_EXPIRY_REMINDER_FIRST'
  AND target_type = 'CONTRACT'
  AND target_id = @c402
  AND recipient_user_id = @tenant_user_id;

INSERT INTO hdbhms.notification_outbox
    (event_type, target_type, target_id, recipient_user_id, channel, title, body, payload, status,
     retry_count, max_retries, scheduled_at, sent_at, created_at, is_read)
VALUES
    ('LEASE_EXPIRY_REMINDER_FIRST', 'CONTRACT', @c402, @tenant_user_id, 'PUSH',
     'Hợp đồng HD-HDD1-402-2026 sắp hết hạn',
     'Phòng 402 tại Nhà trọ Hải Đăng 1 sẽ hết hạn vào 2026-08-15. Bạn muốn gia hạn, chuyển phòng hay chuyển đi?',
     JSON_OBJECT('contractId', @c402, 'contractCode', 'HD-HDD1-402-2026', 'roomId', @r402,
                  'roomName', 'Phòng 402', 'roomCode', '402', 'propertyName', 'Nhà trọ Hải Đăng 1', 'endDate', '2026-08-15',
                 'daysRemaining', 16, 'stage', 'FIRST', 'targetRoute', '/contract'),
      'SENT', 0, 3, '2026-07-30 09:00:00', '2026-07-30 09:00:00', '2026-07-30 09:00:00', FALSE);

-- Keep persisted custom templates aligned with NotificationTemplateDefaults.
UPDATE hdbhms.notification_templates
SET title_template = 'Cần gặp trực tiếp khách thuê nợ quá hạn',
    body_template = CONCAT(
        '[[', '$', '{roomName}]] tại [[', '$', '{propertyName}]] có tổng nợ [[',
        '$', '{totalDebt}]] VND. Hạn xử lý: [[', '$', '{dueDate}]].'
    ),
    status = 'ACTIVE'
WHERE template_key = 'DEBT_DIRECT_VISIT_REQUIRED'
  AND channel = 'PUSH';

UPDATE hdbhms.notification_templates
SET title_template = 'Bạn được đề cử làm người đại diện phòng',
    body_template = CONCAT(
        'Yêu cầu [[', '$', '{requestCode}]] cần bạn xác nhận làm người đại diện mới của [[',
        '$', '{oldRoomName}]] sau khi người hiện tại chuyển đi. Vui lòng phản hồi để quản lý tiếp tục xử lý.'
    ),
    status = 'ACTIVE'
WHERE template_key = 'ROOM_TRANSFER_HOLDER_NOMINATION_REQUESTED'
  AND channel IN ('PUSH', 'WEB');
