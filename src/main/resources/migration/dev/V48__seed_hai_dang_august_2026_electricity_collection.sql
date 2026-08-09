SET NAMES utf8mb4;

-- Align the Hai Dang 1 room master with the August 2026 collection sheet.
SET @property_id := (
    SELECT property_id
    FROM hdbhms.properties
    WHERE property_code = 'HAI_DANG_1'
    LIMIT 1
);

UPDATE hdbhms.rooms
SET listed_price = CASE room_code
    WHEN '101' THEN 2200000
    WHEN '102' THEN 2200000
    WHEN '103' THEN 2100000
    WHEN '104' THEN 2000000
    WHEN '105' THEN 2000000
    WHEN '106' THEN 2000000
    WHEN '201' THEN 2200000
    WHEN '202' THEN 2200000
    WHEN '203' THEN 2100000
    WHEN '204' THEN 2000000
    WHEN '205' THEN 2000000
    WHEN '206' THEN 2000000
    WHEN '207' THEN 2000000
    WHEN '208' THEN 2100000
    WHEN '301' THEN 2200000
    WHEN '302' THEN 2200000
    WHEN '303' THEN 2100000
    WHEN '304' THEN 2000000
    WHEN '305' THEN 2000000
    WHEN '306' THEN 2000000
    WHEN '307' THEN 2000000
    WHEN '308' THEN 2100000
    WHEN '401' THEN 2200000
    WHEN '402' THEN 2200000
    WHEN '403' THEN 2100000
    WHEN '404' THEN 2000000
    WHEN '405' THEN 2000000
    WHEN '406' THEN 2000000
    WHEN '407' THEN 2000000
    WHEN '408' THEN 2100000
    WHEN '501' THEN 2200000
    WHEN '502' THEN 2200000
    WHEN '503' THEN 2100000
    WHEN '504' THEN 2000000
    WHEN '505' THEN 2000000
    WHEN '506' THEN 2000000
    WHEN '507' THEN 2100000
    ELSE listed_price
END
WHERE property_id = @property_id
  AND room_code IN (
      '101', '102', '103', '104', '105', '106',
      '201', '202', '203', '204', '205', '206', '207', '208',
      '301', '302', '303', '304', '305', '306', '307', '308',
      '401', '402', '403', '404', '405', '406', '407', '408',
      '501', '502', '503', '504', '505', '506', '507'
  );

-- The Excel sheet contains electricity only. Keep historical water rows untouched.
INSERT INTO hdbhms.meters (
    room_id,
    meter_type,
    meter_code,
    status,
    installed_at,
    created_at
)
SELECT
    r.room_id,
    'ELECTRICITY',
    CONCAT('HD1-E-', r.room_code),
    'ACTIVE',
    '2025-01-01',
    '2025-01-01 08:00:00'
FROM hdbhms.rooms r
WHERE r.property_id = @property_id
  AND r.room_code IN (
      '101', '102', '103', '104', '105', '106',
      '201', '202', '203', '204', '205', '206', '207', '208',
      '301', '302', '303', '304', '305', '306', '307', '308',
      '401', '402', '403', '404', '405', '406', '407', '408',
      '501', '502', '503', '504', '505', '506', '507'
  )
  AND NOT EXISTS (
      SELECT 1
      FROM hdbhms.meters existing_meter
      WHERE existing_meter.room_id = r.room_id
        AND existing_meter.meter_type = 'ELECTRICITY'
        AND existing_meter.status = 'ACTIVE'
  );

-- The notice is issued in August, but its electricity readings belong to July.
INSERT INTO hdbhms.meter_reading_batches (
    property_id,
    reading_period,
    status,
    confirmed_at,
    created_at,
    total_rooms,
    completed_rooms,
    anomaly_count
)
VALUES (
    @property_id,
    '2026-07',
    'CONFIRMED',
    '2026-07-31 23:59:59',
    '2026-07-31 23:59:59',
    37,
    37,
    0
)
ON DUPLICATE KEY UPDATE
    status = VALUES(status),
    confirmed_at = VALUES(confirmed_at),
    total_rooms = VALUES(total_rooms),
    completed_rooms = VALUES(completed_rooms),
    anomaly_count = VALUES(anomaly_count);

SET @batch_id := (
    SELECT meter_reading_batch_id
    FROM hdbhms.meter_reading_batches
    WHERE property_id = @property_id
      AND reading_period = '2026-07'
    LIMIT 1
);

INSERT INTO hdbhms.meter_readings (
    batch_id,
    meter_id,
    room_id,
    reading_period,
    revision_no,
    previous_value,
    current_value,
    reading_date,
    status,
    purpose,
    source,
    review_status,
    review_count,
    created_at
)
SELECT
    @batch_id,
    m.meter_id,
    r.room_id,
    '2026-07',
    1,
    x.previous_value,
    x.current_value,
    '2026-07-31',
    'CONFIRMED',
    'MONTHLY',
    'EXCEL_IMPORT',
    'NONE',
    0,
    '2026-07-31 23:59:59'
FROM (
    SELECT '101' AS room_code, 2209 AS previous_value, 2428 AS current_value
    UNION ALL SELECT '102', 1885, 1896
    UNION ALL SELECT '103', 2566, 2626
    UNION ALL SELECT '104', 2982, 3301
    UNION ALL SELECT '105', 2495, 2648
    UNION ALL SELECT '106', 6590, 6705
    UNION ALL SELECT '201', 3772, 4069
    UNION ALL SELECT '202', 2650, 2790
    UNION ALL SELECT '203', 2142, 2163
    UNION ALL SELECT '204', 2699, 2869
    UNION ALL SELECT '205', 1948, 1989
    UNION ALL SELECT '206', 2506, 2631
    UNION ALL SELECT '207', 2951, 3304
    UNION ALL SELECT '208', 1910, 2101
    UNION ALL SELECT '301', 3061, 3545
    UNION ALL SELECT '302', 2854, 3093
    UNION ALL SELECT '303', 2516, 2772
    UNION ALL SELECT '304', 1945, 1955
    UNION ALL SELECT '305', 1309, 1447
    UNION ALL SELECT '306', 2053, 2305
    UNION ALL SELECT '307', 2051, 2338
    UNION ALL SELECT '308', 2486, 2614
    UNION ALL SELECT '401', 1960, 2261
    UNION ALL SELECT '402', 3220, 3553
    UNION ALL SELECT '403', 2323, 2348
    UNION ALL SELECT '404', 2471, 2490
    UNION ALL SELECT '405', 1463, 1661
    UNION ALL SELECT '406', 1362, 1446
    UNION ALL SELECT '407', 867, 869
    UNION ALL SELECT '408', 2493, 2614
    UNION ALL SELECT '501', 2691, 2970
    UNION ALL SELECT '502', 3736, 3945
    UNION ALL SELECT '503', 2377, 2535
    UNION ALL SELECT '504', 1722, 1874
    UNION ALL SELECT '505', 526, 568
    UNION ALL SELECT '506', 2098, 2314
    UNION ALL SELECT '507', 1966, 2187
) x
JOIN hdbhms.rooms r
    ON r.property_id = @property_id
   AND r.room_code = x.room_code
JOIN hdbhms.meters m
    ON m.room_id = r.room_id
   AND m.meter_type = 'ELECTRICITY'
   AND m.status = 'ACTIVE'
ON DUPLICATE KEY UPDATE
    batch_id = VALUES(batch_id),
    room_id = VALUES(room_id),
    previous_value = VALUES(previous_value),
    current_value = VALUES(current_value),
    reading_date = VALUES(reading_date),
    status = VALUES(status),
    void_reason = NULL,
    purpose = VALUES(purpose),
    source = VALUES(source),
    review_status = VALUES(review_status),
    review_count = VALUES(review_count);

-- Seed the utility invoices that the confirmed July readings are ready to bill.
-- Contracts with a July final-settlement invoice are intentionally excluded so
-- a checkout/transfer is not charged a second time by the monthly run.
-- Room 402 represents the "contract expiring, tenant has not responded yet"
-- branch. Remove the previously seeded move-out branch before creating the
-- tenant reminder state.
SET @c402 := (
    SELECT lease_contract_id
    FROM hdbhms.lease_contracts
    WHERE contract_code IN ('HD-HDD1-402-2026', 'HD-SEED-402-2026')
    ORDER BY contract_code = 'HD-HDD1-402-2026' DESC
    LIMIT 1
);
SET @r402 := (
    SELECT room_id
    FROM hdbhms.rooms
    WHERE property_id = @property_id
      AND room_code = '402'
    LIMIT 1
);
SET @tenant_demo_user_id := (
    SELECT user_id
    FROM hdbhms.users
    WHERE email = 'seed.tenant@hdbhms.local'
      AND deleted_at IS NULL
    LIMIT 1
);

UPDATE hdbhms.lease_contracts
SET tenant_intention = NULL,
    expected_vacant_date = NULL,
    intention_recorded_at = NULL,
    status = 'EXPIRING_SOON',
    updated_at = '2026-07-30 09:00:00'
WHERE lease_contract_id = @c402;

UPDATE hdbhms.rooms
SET current_status = 'OCCUPIED',
    public_note = 'Seed: hợp đồng sắp hết hạn ngày 2026-08-15, đang chờ khách phản hồi ý định.',
    internal_note = 'Chưa ghi nhận ý định gia hạn, chuyển phòng hoặc chuyển đi.',
    updated_at = '2026-07-30 09:00:00'
WHERE room_id = @r402;

DELETE delivery
FROM hdbhms.notification_deliveries delivery
JOIN hdbhms.notification_outbox n
    ON n.notification_outbox_id = delivery.outbox_id
JOIN hdbhms.manager_tasks task
    ON task.manager_task_id = n.target_id
WHERE n.target_type = 'MANAGER_TASK'
  AND n.event_type = 'LEASE_HANDOVER_CONFIRMATION_DUE'
  AND task.lease_contract_id = @c402;

DELETE n
FROM hdbhms.notification_outbox n
JOIN hdbhms.manager_tasks task
    ON task.manager_task_id = n.target_id
WHERE n.target_type = 'MANAGER_TASK'
  AND n.event_type = 'LEASE_HANDOVER_CONFIRMATION_DUE'
  AND task.lease_contract_id = @c402;

DELETE FROM hdbhms.reminder_trackers
WHERE target_type = 'CONTRACT'
  AND target_id = @c402
  AND reminder_key = 'LEASE_HANDOVER_CONFIRMATION';

DELETE FROM hdbhms.manager_tasks
WHERE lease_contract_id = @c402
  AND task_type = 'LEASE_HANDOVER_CONFIRMATION';

INSERT INTO hdbhms.reminder_trackers
    (reminder_key, target_type, target_id, audience, recipient_user_id, status, sent_count,
     last_sent_at, next_due_at, metadata, created_at, updated_at)
VALUES
    ('LEASE_EXPIRY_INTENTION', 'CONTRACT', @c402, 'PRIMARY_TENANT', @tenant_demo_user_id, 'ACTIVE', 1,
     '2026-07-30 09:00:00', '2026-08-29 09:00:00',
     JSON_OBJECT('endDate', '2026-08-15', 'firstReminderDate', '2026-05-15', 'lastReminderStage', 'FIRST'),
     '2026-07-30 09:00:00', '2026-07-30 09:00:00');

INSERT INTO hdbhms.notification_outbox
    (event_type, target_type, target_id, recipient_user_id, channel, title, body, payload, status,
     retry_count, max_retries, scheduled_at, sent_at, created_at, is_read)
VALUES
    ('LEASE_EXPIRY_REMINDER_FIRST', 'CONTRACT', @c402, @tenant_demo_user_id, 'PUSH',
     'Hợp đồng HD-HDD1-402-2026 sắp hết hạn',
     'Phòng 402 tại Nhà trọ Hải Đăng 1 sẽ hết hạn vào 2026-08-15. Bạn muốn gia hạn, chuyển phòng hay chuyển đi?',
     JSON_OBJECT(
         'contractId', @c402,
         'contractCode', 'HD-HDD1-402-2026',
         'roomId', @r402,
         'roomName', 'Phòng 402',
         'roomCode', '402',
         'propertyName', 'Nhà trọ Hải Đăng 1',
         'endDate', '2026-08-15',
         'daysRemaining', 16,
         'stage', 'FIRST',
         'targetRoute', '/contract'
     ),
     'SENT', 0, 3, '2026-07-30 09:00:00', '2026-07-30 09:00:00', '2026-07-30 09:00:00', FALSE);

SET @utility_account := (
    SELECT collection_account_id
    FROM hdbhms.collection_accounts
    WHERE property_id = @property_id
      AND account_type = 'UTILITY'
      AND status = 'ACTIVE'
    ORDER BY collection_account_id
    LIMIT 1
);

INSERT INTO hdbhms.invoices
    (invoice_code, property_id, room_id, lease_contract_id, invoice_type, revision_no, billing_period,
     issue_date, due_date, status, subtotal_amount, discount_amount, total_amount, paid_amount,
     remaining_amount, collection_account_id, created_by, issued_at, created_at, updated_at)
SELECT
    CONCAT('SEED-INV-', r.room_code, '-2026-07-UTILITY-EXCEL'),
    @property_id,
    r.room_id,
    lc.lease_contract_id,
    'UTILITY',
    1,
    '2026-07',
    '2026-08-01 08:00:00',
    '2026-08-10 23:59:59',
    'ISSUED',
    CAST(CEILING(GREATEST(mr.current_value - mr.previous_value, 0)) AS UNSIGNED) * 3500,
    0,
    CAST(CEILING(GREATEST(mr.current_value - mr.previous_value, 0)) AS UNSIGNED) * 3500,
    0,
    CAST(CEILING(GREATEST(mr.current_value - mr.previous_value, 0)) AS UNSIGNED) * 3500,
    @utility_account,
    (SELECT user_id FROM hdbhms.users WHERE email = 'seed.manager@hdbhms.local' AND deleted_at IS NULL LIMIT 1),
    '2026-08-01 08:00:00',
    '2026-08-01 08:00:00',
    '2026-08-01 08:00:00'
FROM hdbhms.lease_contracts lc
JOIN hdbhms.rooms r
    ON r.room_id = lc.room_id
JOIN hdbhms.meters m
    ON m.room_id = r.room_id
   AND m.meter_type = 'ELECTRICITY'
   AND m.status = 'ACTIVE'
JOIN hdbhms.meter_readings mr
    ON mr.meter_id = m.meter_id
   AND mr.room_id = r.room_id
   AND mr.reading_period = '2026-07'
   AND mr.status = 'CONFIRMED'
WHERE lc.room_id IS NOT NULL
  AND lc.start_date <= '2026-07-31'
  AND lc.end_date >= '2026-07-01'
  AND lc.status IN ('ACTIVE', 'EXPIRING_SOON', 'TERMINATION_PENDING')
  AND NOT EXISTS (
      SELECT 1
      FROM hdbhms.invoices existing_final
      WHERE existing_final.lease_contract_id = lc.lease_contract_id
        AND existing_final.invoice_type = 'FINAL_SETTLEMENT'
        AND existing_final.billing_period = '2026-07'
        AND existing_final.status <> 'VOIDED'
  )
  AND NOT EXISTS (
      SELECT 1
      FROM hdbhms.invoices existing_utility
      WHERE existing_utility.lease_contract_id = lc.lease_contract_id
        AND existing_utility.invoice_type = 'UTILITY'
        AND existing_utility.billing_period = '2026-07'
        AND existing_utility.status <> 'VOIDED'
  )
ON DUPLICATE KEY UPDATE
    room_id = VALUES(room_id),
    total_amount = VALUES(total_amount),
    subtotal_amount = VALUES(subtotal_amount),
    remaining_amount = VALUES(remaining_amount),
    updated_at = VALUES(updated_at);

INSERT INTO hdbhms.invoice_lines
    (invoice_id, line_type, description, quantity, unit_price, meter_reading_id, source_type, source_id, collection_account_id, created_at)
SELECT
    i.invoice_id,
    'ELECTRICITY',
    CONCAT('Dien phong ', r.room_code, ' thang 07/2026 (Excel import)'),
    CAST(CEILING(GREATEST(mr.current_value - mr.previous_value, 0)) AS UNSIGNED),
    3500,
    mr.meter_reading_id,
    'EXCEL_IMPORT',
    mr.meter_reading_id,
    @utility_account,
    '2026-08-01 08:00:00'
FROM hdbhms.invoices i
JOIN hdbhms.lease_contracts lc
    ON lc.lease_contract_id = i.lease_contract_id
JOIN hdbhms.rooms r
    ON r.room_id = lc.room_id
JOIN hdbhms.meters m
    ON m.room_id = r.room_id
   AND m.meter_type = 'ELECTRICITY'
   AND m.status = 'ACTIVE'
JOIN hdbhms.meter_readings mr
    ON mr.meter_id = m.meter_id
   AND mr.room_id = r.room_id
   AND mr.reading_period = '2026-07'
   AND mr.status = 'CONFIRMED'
WHERE i.invoice_type = 'UTILITY'
  AND i.billing_period = '2026-07'
  AND i.invoice_code LIKE 'SEED-INV-%-2026-07-UTILITY-EXCEL'
  AND NOT EXISTS (
      SELECT 1
      FROM hdbhms.invoice_lines existing_line
      WHERE existing_line.invoice_id = i.invoice_id
        AND existing_line.line_type = 'ELECTRICITY'
  );

-- Keep the web notification list consistent with an issued utility invoice.
INSERT INTO hdbhms.notification_outbox
    (event_type, target_type, target_id, recipient_user_id, channel, title, body, payload, status,
     retry_count, max_retries, scheduled_at, sent_at, created_at, is_read)
SELECT
    'INVOICE_ISSUED',
    'INVOICE',
    i.invoice_id,
    tenant.user_id,
    notification_channel.channel,
    CONCAT('Có hóa đơn mới ', i.invoice_code),
    CONCAT('Hóa đơn ', i.invoice_code, ' của phòng ', r.room_code, ' kỳ 2026-07 đã phát hành. Số tiền cần thanh toán: ', i.remaining_amount, ' VND. Hạn thanh toán: 2026-08-10.'),
    JSON_OBJECT('invoiceId', i.invoice_id, 'invoiceCode', i.invoice_code, 'invoiceType', 'UTILITY', 'roomCode', r.room_code, 'propertyName', 'Nhà trọ Hải Đăng 1', 'billingPeriod', '2026-07', 'amount', i.total_amount, 'totalAmount', i.total_amount, 'remainingAmount', i.remaining_amount, 'dueDate', '2026-08-10', 'targetRoute', '/payment'),
    'SENT',
    0,
    3,
    '2026-08-01 08:00:00',
    '2026-08-01 08:00:00',
    '2026-08-01 08:00:00',
    FALSE
FROM hdbhms.invoices i
JOIN hdbhms.lease_contracts lc
    ON lc.lease_contract_id = i.lease_contract_id
JOIN hdbhms.rooms r
    ON r.room_id = lc.room_id
JOIN hdbhms.person_profiles profile
    ON profile.person_profile_id = lc.primary_tenant_profile_id
JOIN hdbhms.users tenant
    ON tenant.user_id = profile.user_id
CROSS JOIN (
    SELECT 'WEB' AS channel
    UNION ALL
    SELECT 'PUSH' AS channel
) notification_channel
WHERE i.invoice_type = 'UTILITY'
  AND i.billing_period = '2026-07'
  AND i.invoice_code LIKE 'SEED-INV-%-2026-07-UTILITY-EXCEL'
  AND NOT EXISTS (
      SELECT 1
      FROM hdbhms.notification_outbox existing_notification
      WHERE existing_notification.event_type = 'INVOICE_ISSUED'
        AND existing_notification.target_type = 'INVOICE'
        AND existing_notification.target_id = i.invoice_id
        AND existing_notification.recipient_user_id = tenant.user_id
        AND existing_notification.channel = notification_channel.channel
  );

-- Align the hand-seeded lifecycle notifications with the application defaults.
UPDATE hdbhms.notification_outbox n
JOIN hdbhms.manager_tasks task
    ON task.manager_task_id = n.target_id
JOIN hdbhms.lease_contracts contract
    ON contract.lease_contract_id = task.lease_contract_id
JOIN hdbhms.rooms room
    ON room.room_id = contract.room_id
JOIN hdbhms.properties property
    ON property.property_id = room.property_id
SET
    n.title = CASE task.task_type
        WHEN 'LEASE_HANDOVER_CONFIRMATION' THEN CONCAT('Cần chốt bàn giao phòng ', room.name)
        WHEN 'LEASE_RENEWAL_TERMS_CONFIRMATION' THEN CONCAT('Cần chốt gia hạn hợp đồng ', contract.contract_code)
    END,
    n.body = CASE task.task_type
        WHEN 'LEASE_HANDOVER_CONFIRMATION' THEN CONCAT(
            'Hợp đồng ', contract.contract_code,
            ' sắp đến hạn ', contract.end_date,
            '. Hạn công việc ', task.due_date,
            '. Lý do: ',
            CASE contract.contract_code
                WHEN 'HD-HDD1-402-2026' THEN 'Khách đã chọn chuyển đi khi hợp đồng sắp hết hạn.'
                ELSE 'Khách đã chọn chuyển đi và dự kiến bàn giao ngày 2026-07-31.'
            END
        )
        WHEN 'LEASE_RENEWAL_TERMS_CONFIRMATION' THEN CONCAT(
            'Khách phòng ', room.name,
            ' đã chọn gia hạn. Cần chốt giá, thời hạn, tiền cọc và lịch ký trước ', task.due_date, '.'
        )
    END,
    n.payload = CASE task.task_type
        WHEN 'LEASE_HANDOVER_CONFIRMATION' THEN JSON_OBJECT(
            'taskId', task.manager_task_id,
            'contractId', contract.lease_contract_id,
            'contractCode', contract.contract_code,
            'roomId', room.room_id,
            'roomName', room.name,
            'roomCode', room.room_code,
            'propertyName', property.name,
            'endDate', contract.end_date,
            'dueDate', task.due_date,
            'reason', CASE contract.contract_code
                WHEN 'HD-HDD1-402-2026' THEN 'Khách đã chọn chuyển đi khi hợp đồng sắp hết hạn.'
                ELSE 'Khách đã chọn chuyển đi và dự kiến bàn giao ngày 2026-07-31.'
            END,
            'targetRoute', CONCAT('/dashboard/contracts/', contract.lease_contract_id)
        )
        ELSE JSON_OBJECT(
            'taskId', task.manager_task_id,
            'contractId', contract.lease_contract_id,
            'contractCode', contract.contract_code,
            'roomId', room.room_id,
            'roomName', room.name,
            'roomCode', room.room_code,
            'propertyName', property.name,
            'endDate', contract.end_date,
            'dueDate', task.due_date,
            'reason', 'Khách đã chọn gia hạn hợp đồng.',
            'targetRoute', CONCAT('/dashboard/contracts/', contract.lease_contract_id)
        )
    END
WHERE n.target_type = 'MANAGER_TASK'
  AND n.event_type IN ('LEASE_HANDOVER_CONFIRMATION_DUE', 'LEASE_RENEWAL_TERMS_CONFIRMATION_DUE')
  AND contract.contract_code IN ('HD-HDD1-402-2026', 'HD-HDD1-403-2026', 'HD-HDD1-404-2026');

UPDATE hdbhms.notification_outbox n
JOIN hdbhms.invoices invoice
    ON invoice.invoice_id = n.target_id
JOIN hdbhms.rooms room
    ON room.room_id = invoice.room_id
JOIN hdbhms.properties property
    ON property.property_id = room.property_id
SET
    n.title = CONCAT('Có hóa đơn mới ', invoice.invoice_code),
    n.body = CONCAT(
        'Hóa đơn ', invoice.invoice_code,
        ' của phòng ', room.room_code,
        ' kỳ ', invoice.billing_period,
        ' đã phát hành. Số tiền cần thanh toán: ', invoice.remaining_amount,
        ' VND. Hạn thanh toán: ', DATE(invoice.due_date), '.'
    ),
    n.payload = JSON_OBJECT(
        'invoiceId', invoice.invoice_id,
        'invoiceCode', invoice.invoice_code,
        'invoiceType', invoice.invoice_type,
        'roomCode', room.room_code,
        'propertyName', property.name,
        'billingPeriod', invoice.billing_period,
        'amount', invoice.total_amount,
        'totalAmount', invoice.total_amount,
        'remainingAmount', invoice.remaining_amount,
        'dueDate', DATE(invoice.due_date),
        'targetRoute', '/payment'
    )
WHERE n.event_type = 'INVOICE_ISSUED'
  AND n.target_type = 'INVOICE'
  AND n.channel IN ('WEB', 'PUSH')
  AND invoice.invoice_code LIKE 'SEED-INV-%-2026-07-UTILITY-EXCEL';

-- Backfill PUSH when an earlier seed only created the WEB notification copy.
INSERT INTO hdbhms.notification_outbox
    (event_type, target_type, target_id, recipient_user_id, channel, title, body, payload, status,
     retry_count, max_retries, scheduled_at, sent_at, created_at, is_read)
SELECT
    existing_notification.event_type,
    existing_notification.target_type,
    existing_notification.target_id,
    existing_notification.recipient_user_id,
    'PUSH',
    existing_notification.title,
    existing_notification.body,
    existing_notification.payload,
    existing_notification.status,
    existing_notification.retry_count,
    existing_notification.max_retries,
    existing_notification.scheduled_at,
    existing_notification.sent_at,
    existing_notification.created_at,
    existing_notification.is_read
FROM hdbhms.notification_outbox existing_notification
LEFT JOIN hdbhms.invoices seed_invoice
    ON seed_invoice.invoice_id = existing_notification.target_id
   AND existing_notification.target_type = 'INVOICE'
LEFT JOIN hdbhms.manager_tasks seed_task
    ON seed_task.manager_task_id = existing_notification.target_id
   AND existing_notification.target_type = 'MANAGER_TASK'
LEFT JOIN hdbhms.lease_contracts seed_contract
    ON seed_contract.lease_contract_id = seed_task.lease_contract_id
WHERE existing_notification.channel = 'WEB'
  AND (
      (
          existing_notification.event_type = 'INVOICE_ISSUED'
          AND seed_invoice.invoice_code LIKE 'SEED-INV-%-2026-07-UTILITY-EXCEL'
      )
      OR (
          existing_notification.event_type IN ('LEASE_HANDOVER_CONFIRMATION_DUE', 'LEASE_RENEWAL_TERMS_CONFIRMATION_DUE')
          AND seed_contract.contract_code IN ('HD-HDD1-402-2026', 'HD-HDD1-403-2026', 'HD-HDD1-404-2026')
      )
  )
  AND NOT EXISTS (
      SELECT 1
      FROM hdbhms.notification_outbox existing_push
      WHERE existing_push.event_type = existing_notification.event_type
        AND existing_push.target_type = existing_notification.target_type
        AND existing_push.target_id = existing_notification.target_id
        AND existing_push.recipient_user_id = existing_notification.recipient_user_id
        AND existing_push.channel = 'PUSH'
  );

SET NAMES utf8mb4;

-- Align the Hai Dang 1 occupancy snapshot with the July 2026 collection workbook.
-- The workbook has one snapshot used to collect August rent and July electricity,
-- so these occupants are active across the July-August billing boundary.
SET @property_id := (
    SELECT property_id
    FROM hdbhms.properties
    WHERE property_code = 'HAI_DANG_1'
      AND deleted_at IS NULL
    LIMIT 1
);
SET @owner_tenant_id := (
    SELECT tenant_id
    FROM hdbhms.tenants
    WHERE user_id = (
        SELECT user_id
        FROM hdbhms.users
        WHERE email = 'seed.owner@hdbhms.local'
          AND deleted_at IS NULL
        LIMIT 1
    )
      AND property_id = @property_id
      AND deleted_at IS NULL
    LIMIT 1
);
SET @password_hash := '$2a$10$2Dy4Vg1B5BKuiUMPRuTAluvk/0XzLuSgLGaABFHCoWHaUfUtDFGqm';
SET @now := '2026-08-01 08:00:00';

DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_excel_occupants;
CREATE TEMPORARY TABLE tmp_hdd1_excel_occupants (
    room_code VARCHAR(10) NOT NULL,
    occupant_no TINYINT UNSIGNED NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    dob DATE NOT NULL,
    gender VARCHAR(20) NOT NULL,
    PRIMARY KEY (room_code, occupant_no),
    UNIQUE KEY uq_tmp_hdd1_occupant_email (email),
    UNIQUE KEY uq_tmp_hdd1_occupant_phone (phone)
);

INSERT INTO tmp_hdd1_excel_occupants
    (room_code, occupant_no, full_name, email, phone, dob, gender)
VALUES
    ('101', 1, 'Nguyễn Minh Quân', 'nguyen.minh.quan.101@haidang1.local', '0901101001', '1998-03-12', 'MALE'),
    ('101', 2, 'Trần Ngọc Anh', 'tran.ngoc.anh.101@haidang1.local', '0901101002', '1999-08-21', 'FEMALE'),
    ('102', 1, 'Lê Hoàng Nam', 'le.hoang.nam.102@haidang1.local', '0901102001', '1997-06-18', 'MALE'),
    ('103', 1, 'Phạm Gia Huy', 'pham.gia.huy.103@haidang1.local', '0901103001', '2000-02-04', 'MALE'),
    ('104', 1, 'Vũ Ngọc Mai', 'vu.ngoc.mai.104@haidang1.local', '0901104001', '1998-11-23', 'FEMALE'),
    ('104', 2, 'Trần Thu Hà', 'tran.thu.ha.104@haidang1.local', '0901104002', '1997-05-16', 'FEMALE'),
    ('105', 1, 'Phạm Quốc Bảo', 'pham.quoc.bao.105@haidang1.local', '0901105001', '1996-09-09', 'MALE'),
    ('105', 2, 'Hoàng Mỹ Linh', 'hoang.my.linh.105@haidang1.local', '0901105002', '1999-01-27', 'FEMALE'),
    ('106', 1, 'Đặng Thành Nam', 'dang.thanh.nam.106@haidang1.local', '0901106001', '1998-07-11', 'MALE'),
    ('201', 1, 'Bùi Đức Long', 'bui.duc.long.201@haidang1.local', '0901201001', '1997-12-02', 'MALE'),
    ('202', 1, 'Nguyễn Thùy Dương', 'nguyen.thuy.duong.202@haidang1.local', '0901202001', '1999-04-19', 'FEMALE'),
    ('202', 2, 'Phan Minh Đức', 'phan.minh.duc.202@haidang1.local', '0901202002', '1998-10-30', 'MALE'),
    ('202', 3, 'Lê Ngọc Lan', 'le.ngoc.lan.202@haidang1.local', '0901202003', '2000-06-07', 'FEMALE'),
    ('203', 1, 'Đỗ Hoàng Anh', 'do.hoang.anh.203@haidang1.local', '0901203001', '1997-03-25', 'MALE'),
    ('204', 1, 'Trịnh Hải Yến', 'trinh.hai.yen.204@haidang1.local', '0901204001', '1998-02-14', 'FEMALE'),
    ('205', 1, 'Võ Minh Khang', 'vo.minh.khang.205@haidang1.local', '0901205001', '1996-08-06', 'MALE'),
    ('206', 1, 'Nguyễn Anh Tuấn', 'nguyen.anh.tuan.206@haidang1.local', '0901206001', '1997-01-22', 'MALE'),
    ('206', 2, 'Đặng Thu Trang', 'dang.thu.trang.206@haidang1.local', '0901206002', '1999-09-13', 'FEMALE'),
    ('207', 1, 'Trần Đức Duy', 'tran.duc.duy.207@haidang1.local', '0901207001', '1998-12-18', 'MALE'),
    ('207', 2, 'Nguyễn Khánh Linh', 'nguyen.khanh.linh.207@haidang1.local', '0901207002', '2000-03-29', 'FEMALE'),
    ('208', 1, 'Phạm Hồng Sơn', 'pham.hong.son.208@haidang1.local', '0901208001', '1996-05-08', 'MALE'),
    ('208', 2, 'Lê Quỳnh Chi', 'le.quynh.chi.208@haidang1.local', '0901208002', '1999-11-01', 'FEMALE'),
    ('301', 1, 'Nguyễn Văn Khải', 'nguyen.van.khai.301@haidang1.local', '0901301001', '1995-07-17', 'MALE'),
    ('301', 2, 'Bùi Thanh Hà', 'bui.thanh.ha.301@haidang1.local', '0901301002', '1998-04-05', 'FEMALE'),
    ('301', 3, 'Trần Gia Bảo', 'tran.gia.bao.301@haidang1.local', '0901301003', '2000-01-31', 'MALE'),
    ('302', 1, 'Đỗ Minh Tâm', 'do.minh.tam.302@haidang1.local', '0901302001', '1997-10-12', 'MALE'),
    ('302', 2, 'Phạm Ngọc Hân', 'pham.ngoc.han.302@haidang1.local', '0901302002', '1999-06-26', 'FEMALE'),
    ('302', 3, 'Nguyễn Đức Anh', 'nguyen.duc.anh.302@haidang1.local', '0901302003', '1998-08-15', 'MALE'),
    ('303', 1, 'Lê Quốc Việt', 'le.quoc.viet.303@haidang1.local', '0901303001', '1996-02-20', 'MALE'),
    ('305', 1, 'Hoàng Anh Tuấn', 'hoang.anh.tuan.305@haidang1.local', '0901305001', '1997-09-28', 'MALE'),
    ('306', 1, 'Nguyễn Thị Mai', 'nguyen.thi.mai.306@haidang1.local', '0901306001', '1998-12-10', 'FEMALE'),
    ('306', 2, 'Trần Văn Phúc', 'tran.van.phuc.306@haidang1.local', '0901306002', '1996-03-03', 'MALE'),
    ('307', 1, 'Vũ Thành Đạt', 'vu.thanh.dat.307@haidang1.local', '0901307001', '1999-05-24', 'MALE'),
    ('307', 2, 'Phạm Thùy Linh', 'pham.thuy.linh.307@haidang1.local', '0901307002', '2000-09-18', 'FEMALE'),
    ('308', 1, 'Nguyễn Hải Đăng', 'nguyen.hai.dang.308@haidang1.local', '0901308001', '1997-11-11', 'MALE'),
    ('401', 1, 'Nguyễn Văn Hùng', 'nguyen.van.hung.401@haidang1.local', '0901401001', '1995-04-27', 'MALE'),
    ('401', 2, 'Trần Thị Hương', 'tran.thi.huong.401@haidang1.local', '0901401002', '1998-07-02', 'FEMALE'),
    ('401', 3, 'Phạm Minh Châu', 'pham.minh.chau.401@haidang1.local', '0901401003', '1999-12-22', 'FEMALE'),
    ('402', 1, 'Nguyễn Đức Thịnh', 'nguyen.duc.thinh.402@haidang1.local', '0901402001', '1996-06-15', 'MALE'),
    ('402', 2, 'Lê Thu Trang', 'le.thu.trang.402@haidang1.local', '0901402002', '1998-10-08', 'FEMALE'),
    ('402', 3, 'Võ Thanh Tùng', 'vo.thanh.tung.402@haidang1.local', '0901402003', '1997-01-09', 'MALE'),
    ('405', 1, 'Dương Minh Đức', 'duong.minh.duc.405@haidang1.local', '0901405001', '1996-05-30', 'MALE'),
    ('406', 1, 'Nguyễn Hoài Nam', 'nguyen.hoai.nam.406@haidang1.local', '0901406001', '1999-02-17', 'MALE'),
    ('408', 1, 'Phạm Thị Hoa', 'pham.thi.hoa.408@haidang1.local', '0901408001', '1997-08-25', 'FEMALE'),
    ('501', 1, 'Lê Văn Phúc', 'le.van.phuc.501@haidang1.local', '0901501001', '1995-11-14', 'MALE'),
    ('501', 2, 'Nguyễn Thị Hạnh', 'nguyen.thi.hanh.501@haidang1.local', '0901501002', '1998-03-06', 'FEMALE'),
    ('502', 1, 'Hoàng Văn Nam', 'hoang.van.nam.502@haidang1.local', '0901502001', '1997-07-19', 'MALE'),
    ('502', 2, 'Trần Ngọc Bích', 'tran.ngoc.bich.502@haidang1.local', '0901502002', '1999-02-11', 'FEMALE'),
    ('503', 1, 'Bùi Minh Khoa', 'bui.minh.khoa.503@haidang1.local', '0901503001', '1996-10-21', 'MALE'),
    ('504', 1, 'Nguyễn Thị Vân', 'nguyen.thi.van.504@haidang1.local', '0901504001', '1998-06-03', 'FEMALE'),
    ('505', 1, 'Phan Quốc Khánh', 'phan.quoc.khanh.505@haidang1.local', '0901505001', '1997-09-07', 'MALE'),
    ('506', 1, 'Đặng Văn Hòa', 'dang.van.hoa.506@haidang1.local', '0901506001', '1996-12-16', 'MALE'),
    ('507', 1, 'Nguyễn Minh Khôi', 'nguyen.minh.khoi.507@haidang1.local', '0901507001', '1998-01-28', 'MALE'),
    ('507', 2, 'Trần Diệu Linh', 'tran.dieu.linh.507@haidang1.local', '0901507002', '2000-05-12', 'FEMALE');

DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_excel_rooms;
CREATE TEMPORARY TABLE tmp_hdd1_excel_rooms (
    room_code VARCHAR(10) NOT NULL PRIMARY KEY,
    occupant_count TINYINT UNSIGNED NOT NULL,
    current_contract_code VARCHAR(80) NULL
);

INSERT INTO tmp_hdd1_excel_rooms (room_code, occupant_count, current_contract_code)
VALUES
    ('101', 2, 'HD-HDD1-101-2026'),
    ('102', 1, 'HD-HDD1-102-2026'),
    ('103', 1, 'HD-HDD1-103-2026'),
    ('104', 2, 'HD-HDD1-104-2026'),
    ('105', 2, 'HD-HDD1-105-2026'),
    ('106', 1, 'HD-HDD1-106-2026'),
    ('201', 1, 'HD-HDD1-201-2026'),
    ('202', 3, 'HD-HDD1-202-2026'),
    ('203', 1, 'HD-HDD1-203-2026'),
    ('204', 1, 'HD-HDD1-204-2026'),
    ('205', 1, 'HD-HDD1-205-2026'),
    ('206', 2, 'HD-HDD1-206-2026'),
    ('207', 2, 'HD-HDD1-207-2026'),
    ('208', 2, 'HD-HDD1-208-2026'),
    ('301', 3, 'HD-HDD1-301-2026-02'),
    ('302', 3, 'HD-HDD1-302-2026'),
    ('303', 1, 'HD-HDD1-303-2026'),
    ('304', 0, NULL),
    ('305', 1, 'HD-HDD1-305-2026'),
    ('306', 2, 'HD-HDD1-306-2026'),
    ('307', 2, 'HD-HDD1-307-2026'),
    ('308', 1, 'HD-HDD1-308-2026'),
    ('401', 3, 'HD-HDD1-401-2026'),
    ('402', 3, 'HD-HDD1-402-2026'),
    ('403', 0, NULL),
    ('404', 0, NULL),
    ('405', 1, 'HD-HDD1-405-2026'),
    ('406', 1, 'HD-HDD1-406-2026'),
    ('407', 0, NULL),
    ('408', 1, 'HD-HDD1-408-2026'),
    ('501', 2, 'HD-HDD1-501-2026'),
    ('502', 2, 'HD-HDD1-502-2026'),
    ('503', 1, 'HD-HDD1-503-2026'),
    ('504', 1, 'HD-HDD1-504-2026'),
    ('505', 1, 'HD-HDD1-505-2026-02'),
    ('506', 1, 'HD-HDD1-506-2026'),
    ('507', 2, 'HD-HDD1-507-2026');

-- Keep the old demo accounts usable, but make every displayed tenant profile realistic.
UPDATE hdbhms.person_profiles
SET full_name = 'Nguyễn Văn Minh',
    email = 'nguyen.van.minh@haidang1.local',
    phone = '0901000001'
WHERE email = 'seed.tenant@hdbhms.local';

UPDATE hdbhms.person_profiles
SET full_name = 'Đỗ Thị Lan',
    email = 'do.thi.lan@haidang1.local',
    phone = '0901000002'
WHERE email = 'seed.tenant405.co@hdbhms.local';

UPDATE hdbhms.person_profiles
SET full_name = 'Đặng Thị Hương',
    email = 'dang.thi.huong@haidang1.local',
    phone = '0901000003'
WHERE email = 'seed.tenant507.stay@hdbhms.local';

-- Keep the legacy demo users linked to the realistic profiles above.
UPDATE hdbhms.users user_account
JOIN hdbhms.person_profiles profile
  ON profile.user_id = user_account.user_id
SET user_account.email = profile.email,
    user_account.phone = profile.phone,
    user_account.updated_at = @now
WHERE profile.email IN (
    'nguyen.van.minh@haidang1.local',
    'do.thi.lan@haidang1.local',
    'dang.thi.huong@haidang1.local'
);

INSERT INTO hdbhms.users
    (phone, email, password_hash, role, status, last_login_at, email_verified,
     must_change_password, created_at, updated_at, deleted_at)
SELECT phone, email, @password_hash, 'TENANT', 'ACTIVE', @now, TRUE, FALSE, @now, @now, NULL
FROM tmp_hdd1_excel_occupants
ON DUPLICATE KEY UPDATE
    phone = VALUES(phone),
    email = VALUES(email),
    status = 'ACTIVE',
    role = 'TENANT',
    deleted_at = NULL,
    updated_at = VALUES(updated_at);

INSERT INTO hdbhms.tenants
    (user_id, property_id, created_at, updated_at, deleted_at)
SELECT u.user_id, @property_id, @now, @now, NULL
FROM hdbhms.users u
JOIN tmp_hdd1_excel_occupants occupant
  ON occupant.email = u.email
LEFT JOIN hdbhms.tenants existing_tenant
  ON existing_tenant.user_id = u.user_id
 AND existing_tenant.property_id = @property_id
 AND existing_tenant.deleted_at IS NULL
WHERE existing_tenant.tenant_id IS NULL;

INSERT INTO hdbhms.person_profiles
    (user_id, full_name, dob, gender, phone, email, permanent_address,
     portrait_file_id, created_at, updated_at, deleted_at)
SELECT u.user_id, occupant.full_name, occupant.dob, occupant.gender, occupant.phone,
       occupant.email, 'Hà Nội', NULL, @now, @now, NULL
FROM tmp_hdd1_excel_occupants occupant
JOIN hdbhms.users u
  ON u.email = occupant.email
ON DUPLICATE KEY UPDATE
    full_name = VALUES(full_name),
    dob = VALUES(dob),
    gender = VALUES(gender),
    phone = VALUES(phone),
    email = VALUES(email),
    deleted_at = NULL,
    updated_at = VALUES(updated_at);

-- Replace demo contract codes with codes that look like real Hai Dang 1 contracts.
-- Use a direct code map so this update does not lock rooms and contracts in
-- opposite order to the room-status updater running in the application.
UPDATE hdbhms.lease_contracts
SET contract_code = CASE contract_code
    WHEN 'HD-SEED-101-2026' THEN 'HD-HDD1-101-2026'
    WHEN 'HD-SEED-102-2026' THEN 'HD-HDD1-102-2026'
    WHEN 'HD-SEED-103-2026' THEN 'HD-HDD1-103-2026'
    WHEN 'HD-SEED-104-2026' THEN 'HD-HDD1-104-2026'
    WHEN 'HD-SEED-105-2026' THEN 'HD-HDD1-105-2026'
    WHEN 'HD-SEED-106-2026' THEN 'HD-HDD1-106-2026'
    WHEN 'HD-SEED-201-2026' THEN 'HD-HDD1-201-2026'
    WHEN 'HD-SEED-202-2026' THEN 'HD-HDD1-202-2026'
    WHEN 'HD-SEED-203-2026' THEN 'HD-HDD1-203-2026'
    WHEN 'HD-SEED-204-2026' THEN 'HD-HDD1-204-2026'
    WHEN 'HD-SEED-205-2026' THEN 'HD-HDD1-205-2026'
    WHEN 'HD-SEED-206-2026' THEN 'HD-HDD1-206-2026'
    WHEN 'HD-SEED-207-2026' THEN 'HD-HDD1-207-2026'
    WHEN 'HD-SEED-208-2026' THEN 'HD-HDD1-208-2026'
    WHEN 'HD-SEED-301-2026' THEN 'HD-HDD1-301-2026'
    WHEN 'HD-SEED-302-2026' THEN 'HD-HDD1-302-2026'
    WHEN 'HD-SEED-401-2026' THEN 'HD-HDD1-401-2026'
    WHEN 'HD-SEED-402-2026' THEN 'HD-HDD1-402-2026'
    WHEN 'HD-SEED-403-2026' THEN 'HD-HDD1-403-2026'
    WHEN 'HD-SEED-404-2026' THEN 'HD-HDD1-404-2026'
    WHEN 'HD-SEED-405-2026' THEN 'HD-HDD1-405-2026'
    WHEN 'HD-SEED-501-2026' THEN 'HD-HDD1-501-2026'
    WHEN 'HD-SEED-502-TRANSFER-DRAFT' THEN 'HD-HDD1-502-2026'
    WHEN 'HD-SEED-503-2026' THEN 'HD-HDD1-503-2026'
    WHEN 'HD-SEED-504-TRANSFER-SIGNED' THEN 'HD-HDD1-504-2026'
    WHEN 'HD-SEED-505-2026' THEN 'HD-HDD1-505-2026'
    WHEN 'HD-SEED-506-TRANSFER-ACTIVE' THEN 'HD-HDD1-506-2026'
    WHEN 'HD-SEED-507-2026' THEN 'HD-HDD1-507-2026'
    ELSE contract_code
END
WHERE contract_code LIKE 'HD-SEED-%';

-- Create current contracts for rooms that were not covered by the original lifecycle seed.
INSERT INTO hdbhms.lease_contracts
    (contract_code, room_id, deposit_form_id, primary_tenant_profile_id,
     start_date, end_date, rent_start_date, monthly_rent, payment_cycle_months,
     deposit_amount, status, tenant_intention, expected_vacant_date,
     intention_recorded_at, previous_contract_id, contract_file_id, signed_at,
     created_by, created_at, updated_at, deleted_at)
SELECT
    excel_room.current_contract_code,
    room.room_id,
    NULL,
    primary_profile.person_profile_id,
    '2026-07-01',
    '2026-12-31',
    '2026-07-01',
    room.listed_price,
    1,
    room.listed_price,
    'ACTIVE',
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    '2026-07-01 09:00:00',
    @owner_tenant_id,
    @now,
    @now,
    NULL
FROM tmp_hdd1_excel_rooms excel_room
JOIN hdbhms.rooms room
  ON room.property_id = @property_id
 AND room.room_code = excel_room.room_code
JOIN tmp_hdd1_excel_occupants primary_occupant
  ON primary_occupant.room_code = excel_room.room_code
 AND primary_occupant.occupant_no = 1
JOIN hdbhms.person_profiles primary_profile
  ON primary_profile.email = primary_occupant.email
WHERE excel_room.current_contract_code IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM hdbhms.lease_contracts existing_contract
      WHERE existing_contract.contract_code = excel_room.current_contract_code
  );

-- Align rent and primary profile for the current contract represented in the workbook.
UPDATE hdbhms.lease_contracts contract
JOIN tmp_hdd1_excel_rooms excel_room
  ON excel_room.current_contract_code = contract.contract_code
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
JOIN tmp_hdd1_excel_occupants primary_occupant
  ON primary_occupant.room_code = excel_room.room_code
 AND primary_occupant.occupant_no = 1
JOIN hdbhms.person_profiles primary_profile
  ON primary_profile.email = primary_occupant.email
SET contract.primary_tenant_profile_id = primary_profile.person_profile_id,
    contract.monthly_rent = room.listed_price,
    contract.deposit_amount = room.listed_price,
    contract.updated_at = @now;

-- The source contracts for rooms 301 and 505 remain historical; their current
-- July-August occupants live under the new current contract created above.
DELETE intention
FROM hdbhms.contract_occupant_intentions intention
JOIN hdbhms.contract_occupants occupant
  ON occupant.contract_occupant_id = intention.contract_occupant_id
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = occupant.contract_id
JOIN tmp_hdd1_excel_rooms excel_room
  ON excel_room.current_contract_code = contract.contract_code;

DELETE occupant
FROM hdbhms.contract_occupants occupant
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = occupant.contract_id
JOIN tmp_hdd1_excel_rooms excel_room
  ON excel_room.current_contract_code = contract.contract_code;

-- Empty rooms in the workbook must not retain active occupants from an older
-- lifecycle scenario. Keep their contracts as history, but clear occupancy.
DELETE intention
FROM hdbhms.contract_occupant_intentions intention
JOIN hdbhms.contract_occupants occupant
  ON occupant.contract_occupant_id = intention.contract_occupant_id
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = occupant.contract_id
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
JOIN tmp_hdd1_excel_rooms excel_room
  ON excel_room.room_code = room.room_code
WHERE excel_room.occupant_count = 0;

DELETE occupant
FROM hdbhms.contract_occupants occupant
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = occupant.contract_id
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
JOIN tmp_hdd1_excel_rooms excel_room
  ON excel_room.room_code = room.room_code
WHERE excel_room.occupant_count = 0;

INSERT INTO hdbhms.contract_occupants
    (contract_id, tenant_id, tenant_profile_id, occupant_role, move_in_date,
     move_out_date, status, disabled_reason, disabled_by, disabled_at, created_at)
SELECT
    contract.lease_contract_id,
    tenant.tenant_id,
    profile.person_profile_id,
    CASE WHEN occupant.occupant_no = 1 THEN 'PRIMARY' ELSE 'CO_OCCUPANT' END,
    '2026-07-01',
    NULL,
    'ACTIVE',
    NULL,
    NULL,
    NULL,
    @now
FROM tmp_hdd1_excel_occupants occupant
JOIN tmp_hdd1_excel_rooms excel_room
  ON excel_room.room_code = occupant.room_code
JOIN hdbhms.lease_contracts contract
  ON contract.contract_code = excel_room.current_contract_code
JOIN hdbhms.person_profiles profile
  ON profile.email = occupant.email
JOIN hdbhms.users user_account
  ON user_account.email = occupant.email
JOIN hdbhms.tenants tenant
  ON tenant.user_id = user_account.user_id
 AND tenant.property_id = @property_id
 AND tenant.deleted_at IS NULL;

-- Transfer contracts keep their real handover dates instead of inheriting
-- the workbook snapshot date used by ordinary July occupants.
UPDATE hdbhms.contract_occupants occupant
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = occupant.contract_id
SET occupant.move_in_date = contract.start_date
WHERE contract.contract_code IN (
    'HD-HDD1-502-2026',
    'HD-HDD1-504-2026',
    'HD-HDD1-506-2026'
)
  AND occupant.status = 'ACTIVE';

-- Every seeded occupant must have a login account and an account-provisioning
-- record, including co-occupants. The provisioning row is what the account
-- management flow uses to show that access has been created for the profile.
INSERT INTO hdbhms.tenant_account_provisionings
    (tenant_profile_id, user_id, first_contract_id, latest_contract_id, status,
     recipient_email, sent_at, attempt_count, created_at, updated_at)
SELECT
    profile.person_profile_id,
    user_account.user_id,
    contract.lease_contract_id,
    contract.lease_contract_id,
    'ACTIVE',
    user_account.email,
    @now,
    0,
    @now,
    @now
FROM tmp_hdd1_excel_occupants occupant
JOIN tmp_hdd1_excel_rooms excel_room
  ON excel_room.room_code = occupant.room_code
 AND excel_room.current_contract_code IS NOT NULL
JOIN hdbhms.lease_contracts contract
  ON contract.contract_code = excel_room.current_contract_code
JOIN hdbhms.person_profiles profile
  ON profile.email = occupant.email
JOIN hdbhms.users user_account
  ON user_account.user_id = profile.user_id
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

-- Also repair provisioning rows created by the earlier demo seed after its
-- profile emails were changed to the Hai Dang 1 identities.
UPDATE hdbhms.tenant_account_provisionings provisioning
JOIN hdbhms.person_profiles profile
  ON profile.person_profile_id = provisioning.tenant_profile_id
JOIN hdbhms.users user_account
  ON user_account.user_id = profile.user_id
SET provisioning.user_id = user_account.user_id,
    provisioning.recipient_email = user_account.email,
    provisioning.status = 'ACTIVE',
    provisioning.updated_at = @now
WHERE profile.email IN (
    'nguyen.van.minh@haidang1.local',
    'do.thi.lan@haidang1.local',
    'dang.thi.huong@haidang1.local'
);

-- Mirror the workbook's occupant count in the room list.
UPDATE hdbhms.rooms room
JOIN tmp_hdd1_excel_rooms excel_room
  ON excel_room.room_code = room.room_code
SET room.current_status = CASE
        WHEN excel_room.occupant_count > 0 THEN 'OCCUPIED'
        WHEN room.room_code = '403' THEN 'SOON_VACANT'
        ELSE 'VACANT'
    END,
    room.updated_at = @now
WHERE room.property_id = @property_id
  AND room.deleted_at IS NULL;

-- Target rooms of unfinished transfer requests are reserved, even when the
-- workbook already lists their incoming occupants.
UPDATE hdbhms.rooms room
JOIN hdbhms.room_transfer_requests transfer_request
  ON transfer_request.target_room_id = room.room_id
SET room.current_status = 'RESERVED_FOR_TRANSFER',
    room.updated_at = @now
WHERE room.property_id = @property_id
  AND room.deleted_at IS NULL
  AND transfer_request.status IN (
      'WAITING_APPROVAL',
      'WAITING_NEW_CONTRACT',
      'WAITING_TARGET_HOLDER_APPROVAL',
      'WAITING_CONTRACT_CONFIRMATION',
      'WAITING_SIGNING',
      'WAITING_EXECUTION'
  );

-- Room 404 is empty in the July-August workbook, so the old renewal branch
-- from the lifecycle demo must not remain actionable.
SET @c404 := (
    SELECT lease_contract_id
    FROM hdbhms.lease_contracts
    WHERE contract_code = 'HD-HDD1-404-2026'
    LIMIT 1
);

UPDATE hdbhms.lease_contracts
SET status = 'AUTO_TERMINATED',
    tenant_intention = 'MOVE_OUT',
    expected_vacant_date = '2026-07-31',
    intention_recorded_at = '2026-07-30 08:00:00',
    updated_at = @now
WHERE lease_contract_id = @c404;

DELETE delivery
FROM hdbhms.notification_deliveries delivery
JOIN hdbhms.notification_outbox n
  ON n.notification_outbox_id = delivery.outbox_id
JOIN hdbhms.manager_tasks task
  ON task.manager_task_id = n.target_id
WHERE n.target_type = 'MANAGER_TASK'
  AND n.event_type = 'LEASE_RENEWAL_TERMS_CONFIRMATION_DUE'
  AND task.lease_contract_id = @c404;

DELETE n
FROM hdbhms.notification_outbox n
JOIN hdbhms.manager_tasks task
  ON task.manager_task_id = n.target_id
WHERE n.target_type = 'MANAGER_TASK'
  AND n.event_type = 'LEASE_RENEWAL_TERMS_CONFIRMATION_DUE'
  AND task.lease_contract_id = @c404;

DELETE FROM hdbhms.manager_tasks
WHERE lease_contract_id = @c404
  AND task_type = 'LEASE_RENEWAL_TERMS_CONFIRMATION';

-- Update denormalized seed references after renaming contract codes.
UPDATE hdbhms.manager_tasks
SET title = REPLACE(title, 'HD-SEED-', 'HD-HDD1-'),
    description = REPLACE(description, 'HD-SEED-', 'HD-HDD1-'),
    idempotency_key = REPLACE(idempotency_key, 'HD-SEED-', 'HD-HDD1-')
WHERE title LIKE '%HD-SEED-%'
   OR description LIKE '%HD-SEED-%'
   OR idempotency_key LIKE '%HD-SEED-%';

UPDATE hdbhms.notification_outbox
SET title = REPLACE(title, 'HD-SEED-', 'HD-HDD1-'),
    body = REPLACE(body, 'HD-SEED-', 'HD-HDD1-'),
    payload = REPLACE(payload, 'HD-SEED-', 'HD-HDD1-')
WHERE title LIKE '%HD-SEED-%'
   OR body LIKE '%HD-SEED-%'
   OR payload LIKE '%HD-SEED-%';

UPDATE hdbhms.change_requests
SET title = REPLACE(title, 'HD-SEED-', 'HD-HDD1-'),
    description = REPLACE(description, 'HD-SEED-', 'HD-HDD1-'),
    request_payload = REPLACE(request_payload, 'HD-SEED-', 'HD-HDD1-')
WHERE title LIKE '%HD-SEED-%'
   OR description LIKE '%HD-SEED-%'
   OR request_payload LIKE '%HD-SEED-%';

DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_excel_occupants;
DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_excel_rooms;
