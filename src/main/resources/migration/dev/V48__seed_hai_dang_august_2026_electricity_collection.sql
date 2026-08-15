SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

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
    UNION ALL SELECT '304', 1945, 1945
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
    UNION ALL SELECT '407', 867, 867
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
SET current_status = 'SOON_VACANT',
    public_note = 'Hợp đồng sắp hết hạn ngày 15/08/2026, đang chờ khách phản hồi ý định.',
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

-- Reset room 403 to the start of the liquidation flow. The old lifecycle
-- scenario left a request, handover task, final invoice, and draft liquidation.
SET @c403 := (
    SELECT lease_contract_id
    FROM hdbhms.lease_contracts contract
    JOIN hdbhms.rooms room
      ON room.room_id = contract.room_id
    WHERE room.property_id = @property_id
      AND room.room_code = '403'
      AND contract.contract_code IN ('HD-HDD1-403-2026', 'HD-SEED-403-2026')
      AND contract.deleted_at IS NULL
    ORDER BY contract.contract_code = 'HD-HDD1-403-2026' DESC
    LIMIT 1
);

SET @r403 := (
    SELECT room_id
    FROM hdbhms.rooms
    WHERE property_id = @property_id
      AND room_code = '403'
    LIMIT 1
);

SET @cr403 := (
    SELECT change_request_id
    FROM hdbhms.change_requests
    WHERE request_code = 'TLHD_P403_30_07_2026'
    LIMIT 1
);

SET @inv403_final := (
    SELECT invoice_id
    FROM hdbhms.invoices
    WHERE invoice_code = 'HD_P403_30_07_2026_QT'
    LIMIT 1
);

SET @task403 := (
    SELECT manager_task_id
    FROM hdbhms.manager_tasks
    WHERE lease_contract_id = @c403
      AND task_type = 'LEASE_HANDOVER_CONFIRMATION'
    LIMIT 1
);

DELETE delivery
FROM hdbhms.notification_deliveries delivery
JOIN hdbhms.notification_outbox notification
  ON notification.notification_outbox_id = delivery.outbox_id
JOIN hdbhms.manager_tasks task
  ON task.manager_task_id = notification.target_id
WHERE notification.target_type = 'MANAGER_TASK'
  AND task.lease_contract_id = @c403
  AND task.task_type = 'LEASE_HANDOVER_CONFIRMATION';

DELETE FROM hdbhms.notification_outbox
WHERE (target_type = 'MANAGER_TASK' AND target_id = @task403)
   OR (target_type = 'CHANGE_REQUEST' AND target_id = @cr403)
   OR (event_type = 'LEASE_HANDOVER_CONFIRMATION_DUE'
       AND target_type = 'MANAGER_TASK'
       AND target_id IN (
           SELECT manager_task_id
           FROM hdbhms.manager_tasks
           WHERE lease_contract_id = @c403
             AND task_type = 'LEASE_HANDOVER_CONFIRMATION'
       ));

DELETE FROM hdbhms.notification_deliveries
WHERE outbox_id IN (
    SELECT notification_outbox_id
    FROM hdbhms.notification_outbox
    WHERE target_type = 'INVOICE'
      AND target_id = @inv403_final
);

DELETE FROM hdbhms.notification_outbox
WHERE target_type = 'INVOICE'
  AND target_id = @inv403_final;

DELETE FROM hdbhms.reminder_trackers
WHERE related_task_id IN (
          SELECT manager_task_id
          FROM hdbhms.manager_tasks
          WHERE lease_contract_id = @c403
            AND task_type = 'LEASE_HANDOVER_CONFIRMATION'
      )
   OR (target_type = 'CONTRACT' AND target_id = @c403
       AND reminder_key = 'LEASE_HANDOVER_CONFIRMATION');

DELETE FROM hdbhms.change_request_events
WHERE request_id = @cr403;

DELETE FROM hdbhms.change_requests
WHERE change_request_id = @cr403
   OR request_code = 'TLHD_P403_30_07_2026';

DELETE FROM hdbhms.contract_liquidations
WHERE contract_id = @c403;

DELETE intention
FROM hdbhms.contract_occupant_intentions intention
JOIN hdbhms.contract_occupants occupant
  ON occupant.contract_occupant_id = intention.contract_occupant_id
WHERE occupant.contract_id = @c403;

DELETE FROM hdbhms.contract_occupants
WHERE contract_id = @c403;

UPDATE hdbhms.contract_handover_items item
SET compensation_invoice_id = NULL
WHERE compensation_invoice_id = @inv403_final;

UPDATE hdbhms.rule_violations violation
SET invoice_id = NULL
WHERE invoice_id = @inv403_final;

UPDATE hdbhms.maintenance_costs cost
SET charge_invoice_id = NULL
WHERE charge_invoice_id = @inv403_final;

UPDATE hdbhms.transfer_settlements settlement
SET old_room_final_invoice_id = NULL,
    transfer_difference_invoice_id = NULL
WHERE old_room_final_invoice_id = @inv403_final
   OR transfer_difference_invoice_id = @inv403_final;

UPDATE hdbhms.deposit_batches deposit_batch
SET invoice_id = NULL
WHERE invoice_id = @inv403_final;

UPDATE hdbhms.deposit_batches deposit_batch
JOIN hdbhms.payment_intents intent
  ON intent.deposit_batch_id = deposit_batch.deposit_batch_id
SET deposit_batch.payment_intent_id = NULL
WHERE intent.invoice_id = @inv403_final;

DELETE FROM hdbhms.contract_handover_items
WHERE handover_record_id IN (
    SELECT contract_handover_record_id
    FROM hdbhms.contract_handover_records
    WHERE contract_id = @c403
      AND handover_type = 'MOVE_OUT'
);

DELETE FROM hdbhms.contract_handover_records
WHERE contract_id = @c403
  AND handover_type = 'MOVE_OUT';

UPDATE hdbhms.invoices
SET status = 'DRAFT',
    paid_amount = 0,
    remaining_amount = 0,
    updated_at = '2026-08-01 08:00:00'
WHERE invoice_code = 'HD_P403_30_07_2026_QT';

DELETE line
FROM hdbhms.invoice_lines line
JOIN hdbhms.invoices invoice
  ON invoice.invoice_id = line.invoice_id
WHERE invoice.invoice_code = 'HD_P403_30_07_2026_QT';

DELETE allocation
FROM hdbhms.payment_allocations allocation
JOIN hdbhms.invoices invoice
  ON invoice.invoice_id = allocation.invoice_id
WHERE invoice.invoice_code = 'HD_P403_30_07_2026_QT';

UPDATE hdbhms.invoice_payment_groups payment_group
SET payment_intent_id = NULL
WHERE payment_group.invoice_id = @inv403_final;

UPDATE hdbhms.room_deposit_failures failure
JOIN hdbhms.payment_intents intent
  ON intent.payment_intent_id = failure.payment_intent_id
SET failure.payment_intent_id = NULL
WHERE intent.invoice_id = @inv403_final;

DELETE payment_group
FROM hdbhms.invoice_payment_groups payment_group
JOIN hdbhms.invoices invoice
  ON invoice.invoice_id = payment_group.invoice_id
WHERE invoice.invoice_code = 'HD_P403_30_07_2026_QT';

DELETE payment_intent
FROM hdbhms.payment_intents payment_intent
JOIN hdbhms.invoices invoice
  ON invoice.invoice_id = payment_intent.invoice_id
WHERE invoice.invoice_code = 'HD_P403_30_07_2026_QT';

DELETE item
FROM hdbhms.utility_billing_run_items item
JOIN hdbhms.invoices invoice
  ON invoice.invoice_id = item.invoice_id
WHERE invoice.invoice_code = 'HD_P403_30_07_2026_QT';

DELETE charge
FROM hdbhms.pending_billing_charges charge
JOIN hdbhms.invoices invoice
  ON invoice.invoice_id = charge.invoice_id
WHERE invoice.invoice_code = 'HD_P403_30_07_2026_QT';

DELETE FROM hdbhms.invoices
WHERE invoice_code = 'HD_P403_30_07_2026_QT';

DELETE FROM hdbhms.manager_tasks
WHERE manager_task_id = @task403
   OR (lease_contract_id = @c403 AND task_type = 'LEASE_HANDOVER_CONFIRMATION');

UPDATE hdbhms.lease_contracts
SET status = 'EXPIRING_SOON',
    tenant_intention = NULL,
    expected_vacant_date = NULL,
    intention_recorded_at = NULL,
    updated_at = '2026-08-01 08:00:00'
WHERE lease_contract_id = @c403;

UPDATE hdbhms.rooms
SET current_status = 'OCCUPIED',
    public_note = 'Seed: Room 403 contract expires on 2026-09-30; liquidation flow has not started.',
    internal_note = 'No liquidation intention, request, task, or settlement exists yet.',
    updated_at = '2026-08-01 08:00:00'
WHERE room_id = @r403;

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
    CONCAT('HD_P', r.room_code, '_01_08_2026_DV'),
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
CONCAT('Điện phòng ', r.room_code, ' tháng 07/2026 (nhập từ Excel)'),
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
  AND i.invoice_code LIKE 'HD_P%_01_08_2026_DV'
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
  AND i.invoice_code LIKE 'HD_P%_01_08_2026_DV'
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
                ELSE 'Khách đã chọn chuyển đi và dự kiến bàn giao ngày 2026-08-31.'
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
                ELSE 'Khách đã chọn chuyển đi và dự kiến bàn giao ngày 2026-08-31.'
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
  AND invoice.invoice_code LIKE 'HD_P%_01_08_2026_DV';

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
          AND seed_invoice.invoice_code LIKE 'HD_P%_01_08_2026_DV'
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

SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

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
    PRIMARY KEY (room_code, occupant_no)
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
     ('403', 1, 'Nguyen Duc Thinh', 'nguyen.duc.thinh.403@haidang1.local', '0901403001', '1996-06-15', 'MALE'),
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

-- One tenant account intentionally holds three rooms so account-level room
-- filtering and contract visibility can be exercised with realistic data.
UPDATE tmp_hdd1_excel_occupants
SET full_name = 'Nguyễn Văn Khải',
    email = 'nguyen.van.khai@haidang1.local',
    phone = '0901309001',
    dob = '1995-07-17',
    gender = 'MALE'
WHERE (room_code = '301' AND occupant_no = 1)
   OR (room_code = '302' AND occupant_no = 1)
   OR (room_code = '303' AND occupant_no = 1);

DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_excel_rooms;
CREATE TEMPORARY TABLE tmp_hdd1_excel_rooms (
    room_code VARCHAR(10) NOT NULL PRIMARY KEY,
    occupant_count TINYINT UNSIGNED NOT NULL,
    current_contract_code VARCHAR(80) NULL,
    payment_cycle_months TINYINT UNSIGNED NOT NULL
);

INSERT INTO tmp_hdd1_excel_rooms
    (room_code, occupant_count, current_contract_code, payment_cycle_months)
VALUES
    ('101', 2, 'HD-HDD1-101-2026', 3),
    ('102', 1, 'HD-HDD1-102-2026', 1),
    ('103', 1, 'HD-HDD1-103-2026', 1),
    ('104', 2, 'HD-HDD1-104-2026', 1),
    ('105', 2, 'HD-HDD1-105-2026', 3),
    ('106', 1, 'HD-HDD1-106-2026', 1),
    ('201', 1, 'HD-HDD1-201-2026', 1),
    ('202', 3, 'HD-HDD1-202-2026', 3),
    ('203', 1, 'HD-HDD1-203-2026', 3),
    ('204', 1, 'HD-HDD1-204-2026', 3),
    ('205', 1, 'HD-HDD1-205-2026', 1),
    ('206', 2, 'HD-HDD1-206-2026', 3),
    ('207', 2, 'HD-HDD1-207-2026', 1),
    ('208', 2, 'HD-HDD1-208-2026', 3),
    ('301', 3, 'HD-HDD1-301-2026-02', 1),
    ('302', 3, 'HD-HDD1-302-2026', 1),
    ('303', 1, 'HD-HDD1-303-2026', 3),
    ('304', 0, NULL, 0),
    ('305', 1, 'HD-HDD1-305-2026', 1),
    ('306', 2, 'HD-HDD1-306-2026', 1),
    ('307', 2, 'HD-HDD1-307-2026', 1),
    ('308', 1, 'HD-HDD1-308-2026', 1),
    ('401', 3, 'HD-HDD1-401-2026', 3),
    ('402', 3, 'HD-HDD1-402-2026', 1),
     ('403', 1, 'HD-HDD1-403-2026', 1),
    ('404', 0, NULL, 0),
    ('405', 1, 'HD-HDD1-405-2026', 1),
    ('406', 1, 'HD-HDD1-406-2026', 1),
    ('407', 0, NULL, 0),
    ('408', 1, 'HD-HDD1-408-2026', 1),
    ('501', 2, 'HD-HDD1-501-2026', 1),
    ('502', 2, 'HD-HDD1-502-2026', 1),
    ('503', 1, 'HD-HDD1-503-2026', 3),
    ('504', 1, 'HD-HDD1-504-2026', 3),
    ('505', 1, 'HD-HDD1-505-2026-02', 1),
    ('506', 1, 'HD-HDD1-506-2026', 1),
    ('507', 2, 'HD-HDD1-507-2026', 1);

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
SELECT DISTINCT phone, email, @password_hash, 'TENANT', 'ACTIVE', @now, TRUE, FALSE, @now, @now, NULL
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
SELECT DISTINCT u.user_id, @property_id, @now, @now, NULL
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
SELECT DISTINCT u.user_id, occupant.full_name, occupant.dob, occupant.gender, occupant.phone,
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

-- Replace demo contract codes with temporary Hai Dang 1 aliases. The final
-- block below converts these aliases to the filename-compatible HDT_P format.
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
    excel_room.payment_cycle_months,
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
    contract.payment_cycle_months = excel_room.payment_cycle_months,
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
    MIN(contract.lease_contract_id),
    MAX(contract.lease_contract_id),
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
GROUP BY profile.person_profile_id, user_account.user_id, user_account.email
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

-- V48 is the complete Hai Dang seed. Remove obsolete May and June billing
-- history created by the earlier lifecycle demo before keeping July only.
SET @manager_id := (
    SELECT user_id
    FROM hdbhms.users
    WHERE email = 'seed.manager@hdbhms.local'
      AND deleted_at IS NULL
    LIMIT 1
);
SET @owner_id := (
    SELECT user_id
    FROM hdbhms.users
    WHERE email = 'seed.owner@hdbhms.local'
      AND deleted_at IS NULL
    LIMIT 1
);

CREATE TEMPORARY TABLE tmp_hdd1_old_invoices
(
    invoice_id BIGINT UNSIGNED PRIMARY KEY
);

INSERT INTO tmp_hdd1_old_invoices (invoice_id)
SELECT invoice_id
FROM hdbhms.invoices
WHERE property_id = @property_id
  AND billing_period IN ('2026-05', '2026-06');

CREATE TEMPORARY TABLE tmp_hdd1_old_payment_transactions
(
    payment_transaction_id BIGINT UNSIGNED PRIMARY KEY
);

INSERT INTO tmp_hdd1_old_payment_transactions (payment_transaction_id)
SELECT DISTINCT transaction.payment_transaction_id
FROM hdbhms.payment_transactions transaction
LEFT JOIN hdbhms.payment_allocations allocation
  ON allocation.payment_transaction_id = transaction.payment_transaction_id
LEFT JOIN tmp_hdd1_old_invoices old_invoice
  ON old_invoice.invoice_id = allocation.invoice_id
WHERE old_invoice.invoice_id IS NOT NULL;

CREATE TEMPORARY TABLE tmp_hdd1_old_readings
(
    meter_reading_id BIGINT UNSIGNED PRIMARY KEY
);

INSERT INTO tmp_hdd1_old_readings (meter_reading_id)
SELECT reading.meter_reading_id
FROM hdbhms.meter_readings reading
JOIN hdbhms.rooms room
  ON room.room_id = reading.room_id
WHERE room.property_id = @property_id
  AND reading.reading_period IN ('2026-05', '2026-06');

CREATE TEMPORARY TABLE tmp_hdd1_old_batches
(
    meter_reading_batch_id BIGINT UNSIGNED PRIMARY KEY
);

INSERT INTO tmp_hdd1_old_batches (meter_reading_batch_id)
SELECT batch.meter_reading_batch_id
FROM hdbhms.meter_reading_batches batch
WHERE batch.property_id = @property_id
  AND batch.reading_period IN ('2026-05', '2026-06');

UPDATE hdbhms.contract_handover_records handover
SET handover.electricity_reading_id = NULL
WHERE handover.electricity_reading_id IN (
    SELECT meter_reading_id FROM tmp_hdd1_old_readings
);

UPDATE hdbhms.contract_handover_records handover
SET handover.water_reading_id = NULL
WHERE handover.water_reading_id IN (
    SELECT meter_reading_id FROM tmp_hdd1_old_readings
);

UPDATE hdbhms.room_utility_baselines baseline
SET baseline.last_invoice_id = NULL
WHERE baseline.last_invoice_id IN (
    SELECT invoice_id FROM tmp_hdd1_old_invoices
);

UPDATE hdbhms.utility_billing_run_items item
SET item.invoice_id = NULL
WHERE item.invoice_id IN (
    SELECT invoice_id FROM tmp_hdd1_old_invoices
);

UPDATE hdbhms.utility_billing_run_items item
JOIN tmp_hdd1_old_readings old_reading
  ON old_reading.meter_reading_id = item.electricity_reading_id
  OR old_reading.meter_reading_id = item.water_reading_id
SET item.electricity_reading_id = NULL,
    item.water_reading_id = NULL;

UPDATE hdbhms.pending_billing_charges charge
SET charge.invoice_id = NULL
WHERE charge.invoice_id IN (
    SELECT invoice_id FROM tmp_hdd1_old_invoices
);

UPDATE hdbhms.rule_violations violation
SET violation.invoice_id = NULL
WHERE violation.invoice_id IN (
    SELECT invoice_id FROM tmp_hdd1_old_invoices
);

UPDATE hdbhms.maintenance_costs cost
SET cost.charge_invoice_id = NULL
WHERE cost.charge_invoice_id IN (
    SELECT invoice_id FROM tmp_hdd1_old_invoices
);

UPDATE hdbhms.transfer_settlements settlement
SET settlement.old_room_final_invoice_id = NULL
WHERE settlement.old_room_final_invoice_id IN (
    SELECT invoice_id FROM tmp_hdd1_old_invoices
);

UPDATE hdbhms.transfer_settlements settlement
SET settlement.transfer_difference_invoice_id = NULL
WHERE settlement.transfer_difference_invoice_id IN (
    SELECT invoice_id FROM tmp_hdd1_old_invoices
);

UPDATE hdbhms.contract_liquidations liquidation
SET liquidation.final_invoice_id = NULL
WHERE liquidation.final_invoice_id IN (
    SELECT invoice_id FROM tmp_hdd1_old_invoices
);

UPDATE hdbhms.contract_handover_items item
SET item.compensation_invoice_id = NULL
WHERE item.compensation_invoice_id IN (
    SELECT invoice_id FROM tmp_hdd1_old_invoices
);

UPDATE hdbhms.deposit_batches deposit_batch
SET deposit_batch.invoice_id = NULL
WHERE deposit_batch.invoice_id IN (
    SELECT invoice_id FROM tmp_hdd1_old_invoices
);

UPDATE hdbhms.deposit_batches deposit_batch
JOIN hdbhms.payment_intents intent
  ON intent.deposit_batch_id = deposit_batch.deposit_batch_id
SET deposit_batch.payment_intent_id = NULL
WHERE intent.invoice_id IN (
    SELECT invoice_id FROM tmp_hdd1_old_invoices
);

UPDATE hdbhms.room_deposit_failures failure
JOIN hdbhms.payment_intents intent
  ON intent.payment_intent_id = failure.payment_intent_id
SET failure.payment_intent_id = NULL
WHERE intent.invoice_id IN (
    SELECT invoice_id FROM tmp_hdd1_old_invoices
);

DELETE delivery
FROM hdbhms.notification_deliveries delivery
JOIN hdbhms.notification_outbox notification
  ON notification.notification_outbox_id = delivery.outbox_id
WHERE notification.target_type = 'INVOICE'
  AND notification.target_id IN (
      SELECT invoice_id FROM tmp_hdd1_old_invoices
  );

DELETE FROM hdbhms.notification_outbox
WHERE target_type = 'INVOICE'
  AND target_id IN (
      SELECT invoice_id FROM tmp_hdd1_old_invoices
  );

DELETE FROM hdbhms.payment_allocations
WHERE invoice_id IN (
    SELECT invoice_id FROM tmp_hdd1_old_invoices
);

DELETE FROM hdbhms.payment_intents
WHERE invoice_id IN (
    SELECT invoice_id FROM tmp_hdd1_old_invoices
);

UPDATE hdbhms.invoices invoice
SET invoice.status = 'DRAFT',
    invoice.paid_amount = 0,
    invoice.remaining_amount = 0
WHERE invoice.invoice_id IN (
    SELECT invoice_id FROM tmp_hdd1_old_invoices
);

DELETE line
FROM hdbhms.invoice_lines line
JOIN tmp_hdd1_old_invoices old_invoice
  ON old_invoice.invoice_id = line.invoice_id;

DELETE FROM hdbhms.invoices
WHERE invoice_id IN (
    SELECT invoice_id FROM tmp_hdd1_old_invoices
);

DELETE anomaly
FROM hdbhms.meter_reading_anomalies anomaly
WHERE anomaly.meter_reading_id IN (
    SELECT meter_reading_id FROM tmp_hdd1_old_readings
)
   OR anomaly.batch_id IN (
       SELECT meter_reading_batch_id FROM tmp_hdd1_old_batches
   );

DELETE FROM hdbhms.meter_readings
WHERE meter_reading_id IN (
    SELECT meter_reading_id FROM tmp_hdd1_old_readings
);

DELETE FROM hdbhms.meter_reading_batches
WHERE meter_reading_batch_id IN (
    SELECT meter_reading_batch_id FROM tmp_hdd1_old_batches
);

DELETE FROM hdbhms.payment_transactions
WHERE payment_transaction_id IN (
    SELECT payment_transaction_id FROM tmp_hdd1_old_payment_transactions
);

-- Keep the July workbook as the final source of truth after the lifecycle
-- seed has created its historical contracts and settlement invoices.
UPDATE hdbhms.meter_readings reading
JOIN hdbhms.rooms room
  ON room.room_id = reading.room_id
SET reading.current_value = reading.previous_value
WHERE room.property_id = @property_id
  AND room.room_code IN ('304', '407')
  AND reading.reading_period = '2026-07'
  AND reading.status = 'CONFIRMED';

UPDATE hdbhms.lease_contracts contract
JOIN tmp_hdd1_excel_rooms excel_room
  ON excel_room.current_contract_code = contract.contract_code
SET contract.payment_cycle_months = excel_room.payment_cycle_months,
    contract.updated_at = @now;

-- The July settlement belongs to the historical contract, while the room
-- snapshot belongs to the current contract created above. Link monthly seed
-- invoices to that current contract before the export reads occupants.
UPDATE hdbhms.invoices invoice
JOIN hdbhms.rooms room
  ON room.room_id = invoice.room_id
JOIN tmp_hdd1_excel_rooms excel_room
  ON excel_room.room_code = room.room_code
JOIN hdbhms.lease_contracts contract
  ON contract.contract_code = excel_room.current_contract_code
 AND contract.deleted_at IS NULL
SET invoice.lease_contract_id = contract.lease_contract_id,
    invoice.updated_at = @now
WHERE invoice.property_id = @property_id
  AND invoice.invoice_type = 'UTILITY'
  AND invoice.billing_period = '2026-07'
  AND invoice.invoice_code LIKE 'HD_P%_01_08_2026_DV'
  AND invoice.status <> 'VOIDED'
  AND excel_room.current_contract_code IS NOT NULL;

-- The first invoice insert runs before the current contracts are seeded. Fill
-- in the occupied rooms whose contracts were created later in this migration.
INSERT INTO hdbhms.invoices
    (invoice_code, property_id, room_id, lease_contract_id, invoice_type, revision_no, billing_period,
     issue_date, due_date, status, subtotal_amount, discount_amount, total_amount, paid_amount,
     remaining_amount, collection_account_id, created_by, issued_at, created_at, updated_at)
SELECT
    CONCAT('HD_P', room.room_code, '_01_08_2026_DV'),
    @property_id,
    room.room_id,
    contract.lease_contract_id,
    'UTILITY',
    1,
    '2026-07',
    '2026-08-01 08:00:00',
    '2026-08-10 23:59:59',
    'ISSUED',
    CAST(CEILING(GREATEST(reading.current_value - reading.previous_value, 0)) AS UNSIGNED) * 3500,
    0,
    CAST(CEILING(GREATEST(reading.current_value - reading.previous_value, 0)) AS UNSIGNED) * 3500,
    0,
    CAST(CEILING(GREATEST(reading.current_value - reading.previous_value, 0)) AS UNSIGNED) * 3500,
    @utility_account,
    (SELECT user_id
     FROM hdbhms.users
     WHERE email = 'seed.manager@hdbhms.local'
       AND deleted_at IS NULL
     LIMIT 1),
    '2026-08-01 08:00:00',
    '2026-08-01 08:00:00',
    '2026-08-01 08:00:00'
FROM tmp_hdd1_excel_rooms excel_room
JOIN hdbhms.rooms room
  ON room.property_id = @property_id
 AND room.room_code = excel_room.room_code
JOIN hdbhms.lease_contracts contract
  ON contract.contract_code = excel_room.current_contract_code
 AND contract.deleted_at IS NULL
JOIN hdbhms.meters meter
  ON meter.room_id = room.room_id
 AND meter.meter_type = 'ELECTRICITY'
 AND meter.status = 'ACTIVE'
JOIN hdbhms.meter_readings reading
  ON reading.meter_id = meter.meter_id
 AND reading.room_id = room.room_id
 AND reading.reading_period = '2026-07'
 AND reading.status = 'CONFIRMED'
WHERE excel_room.occupant_count > 0
  AND excel_room.payment_cycle_months > 0
  AND NOT EXISTS (
      SELECT 1
      FROM hdbhms.invoices final_invoice
      WHERE final_invoice.room_id = room.room_id
        AND final_invoice.invoice_type = 'FINAL_SETTLEMENT'
        AND final_invoice.billing_period = '2026-07'
        AND final_invoice.status <> 'VOIDED'
  )
  AND NOT EXISTS (
      SELECT 1
      FROM hdbhms.invoices existing_utility
      WHERE existing_utility.lease_contract_id = contract.lease_contract_id
        AND existing_utility.invoice_type = 'UTILITY'
        AND existing_utility.billing_period = '2026-07'
        AND existing_utility.revision_no = 1
  );

INSERT INTO hdbhms.invoice_lines
    (invoice_id, line_type, description, quantity, unit_price, meter_reading_id,
     source_type, source_id, collection_account_id, created_at)
SELECT
    invoice.invoice_id,
    'ELECTRICITY',
CONCAT('Điện phòng ', room.room_code, ' tháng 07/2026 (nhập từ Excel)'),
    CAST(CEILING(GREATEST(reading.current_value - reading.previous_value, 0)) AS UNSIGNED),
    3500,
    reading.meter_reading_id,
    'EXCEL_IMPORT',
    reading.meter_reading_id,
    @utility_account,
    '2026-08-01 08:00:00'
FROM hdbhms.invoices invoice
JOIN hdbhms.rooms room
  ON room.room_id = invoice.room_id
JOIN hdbhms.meters meter
  ON meter.room_id = room.room_id
 AND meter.meter_type = 'ELECTRICITY'
 AND meter.status = 'ACTIVE'
JOIN hdbhms.meter_readings reading
  ON reading.meter_id = meter.meter_id
 AND reading.room_id = room.room_id
 AND reading.reading_period = '2026-07'
 AND reading.status = 'CONFIRMED'
WHERE invoice.property_id = @property_id
  AND invoice.invoice_type = 'UTILITY'
  AND invoice.billing_period = '2026-07'
  AND invoice.invoice_code LIKE 'HD_P%_01_08_2026_DV'
  AND invoice.status <> 'VOIDED'
  AND NOT EXISTS (
      SELECT 1
      FROM hdbhms.invoice_lines existing_line
      WHERE existing_line.invoice_id = invoice.invoice_id
        AND existing_line.line_type = 'ELECTRICITY'
  );

-- Utility seed invoices include the per-person service fee from the sheet.
INSERT INTO hdbhms.invoice_lines
    (invoice_id, line_type, description, quantity, unit_price,
     meter_reading_id, source_type, source_id, collection_account_id, created_at)
SELECT
    invoice.invoice_id,
    'SERVICE_FEE',
    CONCAT('Phí dịch vụ tháng 07/2026 (',
           excel_room.occupant_count * excel_room.payment_cycle_months,
           ' người-tháng)'),
    excel_room.occupant_count * excel_room.payment_cycle_months,
    50000,
    NULL,
    'EXCEL_IMPORT',
    invoice.invoice_id,
    @utility_account,
    '2026-08-01 08:00:00'
FROM hdbhms.invoices invoice
JOIN hdbhms.rooms room
  ON room.room_id = invoice.room_id
JOIN tmp_hdd1_excel_rooms excel_room
  ON excel_room.room_code = room.room_code
WHERE invoice.property_id = @property_id
  AND invoice.invoice_type = 'UTILITY'
  AND invoice.billing_period = '2026-07'
  AND invoice.invoice_code LIKE 'HD_P%_01_08_2026_DV'
  AND invoice.status <> 'VOIDED'
  AND excel_room.occupant_count > 0
  AND excel_room.payment_cycle_months > 0
  AND NOT EXISTS (
      SELECT 1
      FROM hdbhms.invoice_lines existing_line
      WHERE existing_line.invoice_id = invoice.invoice_id
        AND existing_line.line_type = 'SERVICE_FEE'
  );

UPDATE hdbhms.invoices invoice
JOIN (
    SELECT invoice_id, COALESCE(SUM(amount), 0) AS subtotal_amount
    FROM hdbhms.invoice_lines
    GROUP BY invoice_id
) line_total
  ON line_total.invoice_id = invoice.invoice_id
SET invoice.subtotal_amount = line_total.subtotal_amount,
    invoice.total_amount = line_total.subtotal_amount,
    invoice.remaining_amount = GREATEST(
        line_total.subtotal_amount - invoice.paid_amount,
        0
    ),
    invoice.updated_at = @now
WHERE invoice.property_id = @property_id
  AND invoice.invoice_type = 'UTILITY'
  AND invoice.billing_period = '2026-07'
  AND invoice.invoice_code LIKE 'HD_P%_01_08_2026_DV'
  AND invoice.status <> 'VOIDED';

-- Backfill notifications for invoices created after the initial notification
-- insert, once their current contracts and tenant accounts exist.
INSERT INTO hdbhms.notification_outbox
    (event_type, target_type, target_id, recipient_user_id, channel, title, body, payload, status,
     retry_count, max_retries, scheduled_at, sent_at, created_at, is_read)
SELECT
    'INVOICE_ISSUED',
    'INVOICE',
    invoice.invoice_id,
    tenant.user_id,
    notification_channel.channel,
    CONCAT('Có hóa đơn mới ', invoice.invoice_code),
    CONCAT('Hóa đơn ', invoice.invoice_code, ' của phòng ', room.room_code,
           ' kỳ 2026-07 đã phát hành. Số tiền cần thanh toán: ',
           invoice.remaining_amount, ' VND. Hạn thanh toán: 2026-08-10.'),
    JSON_OBJECT(
        'invoiceId', invoice.invoice_id,
        'invoiceCode', invoice.invoice_code,
        'invoiceType', 'UTILITY',
        'roomCode', room.room_code,
        'propertyName', 'Nhà trọ Hải Đăng 1',
        'billingPeriod', '2026-07',
        'amount', invoice.total_amount,
        'totalAmount', invoice.total_amount,
        'remainingAmount', invoice.remaining_amount,
        'dueDate', '2026-08-10',
        'targetRoute', '/payment'
    ),
    'SENT',
    0,
    3,
    '2026-08-01 08:00:00',
    '2026-08-01 08:00:00',
    '2026-08-01 08:00:00',
    FALSE
FROM hdbhms.invoices invoice
JOIN hdbhms.rooms room
  ON room.room_id = invoice.room_id
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = invoice.lease_contract_id
JOIN hdbhms.person_profiles profile
  ON profile.person_profile_id = contract.primary_tenant_profile_id
JOIN hdbhms.users tenant
  ON tenant.user_id = profile.user_id
CROSS JOIN (
    SELECT 'WEB' AS channel
    UNION ALL
    SELECT 'PUSH' AS channel
) notification_channel
WHERE invoice.property_id = @property_id
  AND invoice.invoice_type = 'UTILITY'
  AND invoice.billing_period = '2026-07'
  AND invoice.invoice_code LIKE 'HD_P%_01_08_2026_DV'
  AND invoice.status <> 'VOIDED'
  AND NOT EXISTS (
      SELECT 1
      FROM hdbhms.notification_outbox existing_notification
      WHERE existing_notification.event_type = 'INVOICE_ISSUED'
        AND existing_notification.target_type = 'INVOICE'
        AND existing_notification.target_id = invoice.invoice_id
        AND existing_notification.recipient_user_id = tenant.user_id
        AND existing_notification.channel = notification_channel.channel
  );

-- Vacant rooms and rooms already settled must not leave a second monthly
-- utility invoice in the seed. Keep the invoice record as a voided audit row.
CREATE TEMPORARY TABLE tmp_hdd1_void_utility_invoices
(
    invoice_id BIGINT UNSIGNED PRIMARY KEY
);

INSERT INTO tmp_hdd1_void_utility_invoices (invoice_id)
SELECT invoice.invoice_id
FROM hdbhms.invoices invoice
JOIN hdbhms.rooms room
  ON room.room_id = invoice.room_id
JOIN tmp_hdd1_excel_rooms excel_room
  ON excel_room.room_code = room.room_code
WHERE invoice.property_id = @property_id
  AND invoice.invoice_type = 'UTILITY'
  AND invoice.billing_period = '2026-07'
  AND invoice.invoice_code LIKE 'HD_P%_01_08_2026_DV'
  AND invoice.status <> 'VOIDED'
  AND (
      excel_room.occupant_count = 0
      OR EXISTS (
          SELECT 1
          FROM hdbhms.invoices final_invoice
          WHERE final_invoice.room_id = room.room_id
            AND final_invoice.invoice_type = 'FINAL_SETTLEMENT'
            AND final_invoice.billing_period = '2026-07'
            AND final_invoice.status <> 'VOIDED'
      )
  );

DELETE delivery
FROM hdbhms.notification_deliveries delivery
JOIN hdbhms.notification_outbox notification
  ON notification.notification_outbox_id = delivery.outbox_id
JOIN tmp_hdd1_void_utility_invoices target
  ON target.invoice_id = notification.target_id
WHERE notification.target_type = 'INVOICE';

DELETE notification
FROM hdbhms.notification_outbox notification
JOIN tmp_hdd1_void_utility_invoices target
  ON target.invoice_id = notification.target_id
WHERE notification.target_type = 'INVOICE';

UPDATE hdbhms.invoices invoice
JOIN tmp_hdd1_void_utility_invoices target
  ON target.invoice_id = invoice.invoice_id
SET invoice.status = 'DRAFT',
    invoice.paid_amount = 0,
    invoice.remaining_amount = 0,
    invoice.updated_at = @now;

DELETE line
FROM hdbhms.invoice_lines line
JOIN tmp_hdd1_void_utility_invoices target
  ON target.invoice_id = line.invoice_id;

UPDATE hdbhms.invoices invoice
JOIN tmp_hdd1_void_utility_invoices target
  ON target.invoice_id = invoice.invoice_id
SET invoice.status = 'VOIDED',
    invoice.subtotal_amount = 0,
    invoice.discount_amount = 0,
    invoice.total_amount = 0,
    invoice.paid_amount = 0,
    invoice.remaining_amount = 0,
    invoice.voided_at = @now,
invoice.void_reason = 'Phòng trống hoặc đã có hóa đơn chốt kỳ 07/2026.',
    invoice.updated_at = @now;

-- Sync the two settlement invoices whose electricity belongs to the July
-- workbook. Water and compensation lines remain available for handover flow.
CREATE TEMPORARY TABLE tmp_hdd1_final_invoices
(
    invoice_id BIGINT UNSIGNED PRIMARY KEY
);

INSERT INTO tmp_hdd1_final_invoices (invoice_id)
SELECT invoice.invoice_id
FROM hdbhms.invoices invoice
JOIN hdbhms.rooms room
  ON room.room_id = invoice.room_id
WHERE invoice.property_id = @property_id
  AND room.room_code IN ('301', '505')
  AND invoice.invoice_type = 'FINAL_SETTLEMENT'
  AND invoice.billing_period = '2026-07'
  AND invoice.status <> 'VOIDED';

CREATE TEMPORARY TABLE tmp_hdd1_final_state
(
    invoice_id BIGINT UNSIGNED PRIMARY KEY,
    original_status VARCHAR(30) NOT NULL,
    original_paid_amount BIGINT UNSIGNED NOT NULL
);

INSERT INTO tmp_hdd1_final_state
    (invoice_id, original_status, original_paid_amount)
SELECT invoice.invoice_id, invoice.status, invoice.paid_amount
FROM hdbhms.invoices invoice
JOIN tmp_hdd1_final_invoices target
  ON target.invoice_id = invoice.invoice_id;

UPDATE hdbhms.invoices invoice
JOIN tmp_hdd1_final_invoices target
  ON target.invoice_id = invoice.invoice_id
SET invoice.status = 'DRAFT';

DELETE line
FROM hdbhms.invoice_lines line
JOIN tmp_hdd1_final_invoices target
  ON target.invoice_id = line.invoice_id
WHERE line.line_type IN ('ELECTRICITY', 'SERVICE_FEE');

INSERT INTO hdbhms.invoice_lines
    (invoice_id, line_type, description, quantity, unit_price, meter_reading_id,
     source_type, source_id, created_at)
SELECT
    invoice.invoice_id,
    'ELECTRICITY',
CONCAT('Điện phòng ', room.room_code, ' tháng 07/2026 (Excel)'),
    reading.current_value - reading.previous_value,
    3500,
    reading.meter_reading_id,
    'EXCEL_IMPORT',
    reading.meter_reading_id,
    '2026-08-01 08:00:00'
FROM hdbhms.invoices invoice
JOIN tmp_hdd1_final_invoices target
  ON target.invoice_id = invoice.invoice_id
JOIN hdbhms.rooms room
  ON room.room_id = invoice.room_id
JOIN hdbhms.meters meter
  ON meter.room_id = room.room_id
 AND meter.meter_type = 'ELECTRICITY'
 AND meter.status = 'ACTIVE'
JOIN hdbhms.meter_readings reading
  ON reading.meter_id = meter.meter_id
 AND reading.room_id = room.room_id
 AND reading.reading_period = '2026-07'
 AND reading.status = 'CONFIRMED';

INSERT INTO hdbhms.invoice_lines
    (invoice_id, line_type, description, quantity, unit_price,
     source_type, source_id, created_at)
SELECT
    invoice.invoice_id,
    'SERVICE_FEE',
    CONCAT('Phí dịch vụ tháng 07/2026 (',
           excel_room.occupant_count * excel_room.payment_cycle_months,
           ' người-tháng)'),
    excel_room.occupant_count * excel_room.payment_cycle_months,
    50000,
    'EXCEL_IMPORT',
    invoice.invoice_id,
    '2026-08-01 08:00:00'
FROM hdbhms.invoices invoice
JOIN hdbhms.rooms room
  ON room.room_id = invoice.room_id
JOIN tmp_hdd1_final_invoices target
  ON target.invoice_id = invoice.invoice_id
JOIN tmp_hdd1_excel_rooms excel_room
  ON excel_room.room_code = room.room_code
WHERE excel_room.occupant_count > 0
  AND excel_room.payment_cycle_months > 0;

UPDATE hdbhms.invoices invoice
JOIN (
    SELECT invoice_id, COALESCE(SUM(amount), 0) AS subtotal_amount
    FROM hdbhms.invoice_lines
    GROUP BY invoice_id
) line_total
  ON line_total.invoice_id = invoice.invoice_id
JOIN tmp_hdd1_final_state original_state
  ON original_state.invoice_id = invoice.invoice_id
SET invoice.subtotal_amount = line_total.subtotal_amount,
    invoice.total_amount = line_total.subtotal_amount,
    invoice.paid_amount = CASE
        WHEN original_state.original_status = 'PAID' THEN line_total.subtotal_amount
        ELSE LEAST(original_state.original_paid_amount, line_total.subtotal_amount)
    END,
    invoice.remaining_amount = GREATEST(
        line_total.subtotal_amount - CASE
            WHEN original_state.original_status = 'PAID' THEN line_total.subtotal_amount
            ELSE LEAST(original_state.original_paid_amount, line_total.subtotal_amount)
        END,
        0
    ),
    invoice.status = original_state.original_status,
    invoice.updated_at = @now;

UPDATE hdbhms.payment_allocations allocation
JOIN tmp_hdd1_final_state original_state
  ON original_state.invoice_id = allocation.invoice_id
JOIN hdbhms.invoices invoice
  ON invoice.invoice_id = allocation.invoice_id
SET allocation.amount = invoice.total_amount
WHERE original_state.original_status = 'PAID';

UPDATE hdbhms.payment_transactions transaction
JOIN hdbhms.payment_allocations allocation
  ON allocation.payment_transaction_id = transaction.payment_transaction_id
JOIN tmp_hdd1_final_state original_state
  ON original_state.invoice_id = allocation.invoice_id
SET transaction.amount = allocation.amount
WHERE original_state.original_status = 'PAID';

-- Match seeded contract codes with the PDF filename format used by the
-- contract document flow, for example HDT_P401_01_01_2026.
CREATE TEMPORARY TABLE tmp_hdd1_contract_code_map
(
    old_code VARCHAR(80) NOT NULL PRIMARY KEY,
    new_code VARCHAR(80) NOT NULL UNIQUE
);

INSERT INTO tmp_hdd1_contract_code_map (old_code, new_code)
SELECT contract.contract_code,
       CONCAT(
           'HDT_P',
           room.room_code,
           '_',
           DATE_FORMAT(contract.start_date, '%d_%m_%Y')
       )
FROM hdbhms.lease_contracts contract
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
WHERE contract.contract_code LIKE 'HD-HDD1-%'
  AND contract.deleted_at IS NULL;

UPDATE hdbhms.lease_contracts contract
JOIN tmp_hdd1_contract_code_map code_map
  ON code_map.old_code = contract.contract_code
SET contract.contract_code = code_map.new_code,
    contract.updated_at = @now;

UPDATE hdbhms.manager_tasks task
JOIN tmp_hdd1_contract_code_map code_map
  ON LOCATE(code_map.old_code, task.title) > 0
  OR LOCATE(code_map.old_code, task.description) > 0
  OR LOCATE(code_map.old_code, task.idempotency_key) > 0
SET task.title = REPLACE(task.title, code_map.old_code, code_map.new_code),
    task.description = REPLACE(task.description, code_map.old_code, code_map.new_code),
    task.idempotency_key = REPLACE(task.idempotency_key, code_map.old_code, code_map.new_code);

UPDATE hdbhms.notification_outbox notification
JOIN tmp_hdd1_contract_code_map code_map
  ON LOCATE(code_map.old_code, notification.title) > 0
  OR LOCATE(code_map.old_code, notification.body) > 0
  OR LOCATE(code_map.old_code, notification.payload) > 0
SET notification.title = REPLACE(notification.title, code_map.old_code, code_map.new_code),
    notification.body = REPLACE(notification.body, code_map.old_code, code_map.new_code),
    notification.payload = REPLACE(notification.payload, code_map.old_code, code_map.new_code);

UPDATE hdbhms.change_requests request
JOIN tmp_hdd1_contract_code_map code_map
  ON LOCATE(code_map.old_code, request.title) > 0
  OR LOCATE(code_map.old_code, request.description) > 0
   OR LOCATE(code_map.old_code, request.request_payload) > 0
SET request.title = REPLACE(request.title, code_map.old_code, code_map.new_code),
    request.description = REPLACE(request.description, code_map.old_code, code_map.new_code),
    request.request_payload = REPLACE(request.request_payload, code_map.old_code, code_map.new_code);

-- Recreate the management notifications for the seeded requests after all
-- request/contract references have been normalized. Only requests that still
-- require action are notified, so completed/rejected history stays quiet.
INSERT INTO hdbhms.notification_outbox
    (event_type, target_type, target_id, recipient_user_id, channel, title, body, payload, status,
     retry_count, max_retries, scheduled_at, sent_at, created_at, is_read)
SELECT
    'CHANGE_REQUEST_CREATED',
    'CHANGE_REQUEST',
    request.change_request_id,
    COALESCE(
        request.assigned_to,
        CASE request.assigned_role
            WHEN 'OWNER' THEN @owner_id
            WHEN 'MANAGER' THEN @manager_id
            ELSE NULL
        END
    ),
    notification_channel.channel,
    CONCAT(
        'Có ',
        CASE request.request_type
            WHEN 'CONTRACT_LIQUIDATION' THEN 'yêu cầu thanh lý hợp đồng'
            WHEN 'CONTRACT_RENEWAL' THEN 'yêu cầu gia hạn hợp đồng'
            WHEN 'ROOM_TRANSFER' THEN 'yêu cầu chuyển phòng'
            WHEN 'TENANT_PROFILE_ACCESS' THEN 'yêu cầu xem hồ sơ khách thuê'
            ELSE 'yêu cầu mới'
        END,
        ' cần xử lý'
    ),
    CONCAT(
        request.request_code,
        ' - ',
        request.title,
        ' đang ',
        CASE request.status
            WHEN 'PENDING' THEN 'chờ tiếp nhận'
            WHEN 'UNDER_REVIEW' THEN 'được xem xét'
            WHEN 'PROCESSING' THEN 'được xử lý'
            ELSE 'cần xử lý'
        END,
        '. Nội dung: ',
        request.description,
        '.'
    ),
    JSON_OBJECT(
        'requestId', request.change_request_id,
        'requestCode', request.request_code,
        'requestType', request.request_type,
        'requestTypeLabel', CASE request.request_type
            WHEN 'CONTRACT_LIQUIDATION' THEN 'Yêu cầu thanh lý hợp đồng'
            WHEN 'CONTRACT_RENEWAL' THEN 'Yêu cầu gia hạn hợp đồng'
            WHEN 'ROOM_TRANSFER' THEN 'Yêu cầu chuyển phòng'
            WHEN 'TENANT_PROFILE_ACCESS' THEN 'Yêu cầu xem hồ sơ khách thuê'
            ELSE 'Yêu cầu'
        END,
        'title', request.title,
        'description', request.description,
        'requesterId', request.requester_id,
        'requesterRole', request.requester_role,
        'assignedRole', request.assigned_role,
        'targetType', request.target_type,
        'targetId', request.target_id,
        'roomCode', request_room.room_code,
        'propertyName', COALESCE(request_property.name, request_payload_property.name),
        'targetRoute', CONCAT('/dashboard/requests?requestId=', request.change_request_id)
    ),
    'SENT',
    0,
    3,
    @now,
    @now,
    @now,
    FALSE
FROM hdbhms.change_requests request
LEFT JOIN hdbhms.lease_contracts request_contract
  ON request.request_type IN ('CONTRACT_LIQUIDATION', 'CONTRACT_RENEWAL')
 AND request.target_type = 'CONTRACT'
 AND request.target_id = request_contract.lease_contract_id
LEFT JOIN hdbhms.room_transfer_requests transfer_request
  ON request.request_type = 'ROOM_TRANSFER'
 AND transfer_request.request_code = request.request_code
LEFT JOIN hdbhms.rooms request_room
  ON request_room.room_id = COALESCE(request_contract.room_id, transfer_request.old_room_id)
LEFT JOIN hdbhms.properties request_property
  ON request_property.property_id = request_room.property_id
LEFT JOIN hdbhms.properties request_payload_property
  ON request_payload_property.property_id = CAST(
      JSON_UNQUOTE(JSON_EXTRACT(request.request_payload, '$.propertyId'))
      AS UNSIGNED
  )
CROSS JOIN (
    SELECT 'WEB' AS channel
    UNION ALL
    SELECT 'PUSH' AS channel
) notification_channel
WHERE COALESCE(request_property.property_id, request_payload_property.property_id) = @property_id
  AND request.assigned_role IN ('OWNER', 'MANAGER')
  AND request.status IN ('PENDING', 'UNDER_REVIEW', 'PROCESSING')
  AND NOT (
      request.request_type = 'CONTRACT_RENEWAL'
      AND request_contract.status = 'AUTO_TERMINATED'
  )
  AND COALESCE(
      request.assigned_to,
      CASE request.assigned_role
          WHEN 'OWNER' THEN @owner_id
          WHEN 'MANAGER' THEN @manager_id
          ELSE NULL
      END
  ) IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM hdbhms.notification_outbox existing_notification
      WHERE existing_notification.event_type = 'CHANGE_REQUEST_CREATED'
        AND existing_notification.target_type = 'CHANGE_REQUEST'
        AND existing_notification.target_id = request.change_request_id
        AND existing_notification.recipient_user_id = COALESCE(
            request.assigned_to,
            CASE request.assigned_role
                WHEN 'OWNER' THEN @owner_id
                WHEN 'MANAGER' THEN @manager_id
                ELSE NULL
            END
        )
        AND existing_notification.channel = notification_channel.channel
  );

-- Room 402 has an expiring contract with no tenant intention yet. Notify the
-- owner and manager to monitor it without fabricating a manager task before
-- the tenant chooses renewal, transfer, or move-out.
INSERT INTO hdbhms.notification_outbox
    (event_type, target_type, target_id, recipient_user_id, channel, title, body, payload, status,
     retry_count, max_retries, scheduled_at, sent_at, created_at, is_read)
SELECT
    'CONTRACT_EXPIRING_SOON_REVIEW',
    'CONTRACT',
    contract.lease_contract_id,
    recipient.recipient_user_id,
    notification_channel.channel,
    CONCAT('Hợp đồng phòng ', room.room_code, ' sắp hết hạn'),
    CONCAT(
        'Hợp đồng ', contract.contract_code,
        ' của phòng ', room.room_code,
        ' sẽ hết hạn vào ', contract.end_date,
        '. Khách thuê chưa ghi nhận ý định; vui lòng theo dõi và xử lý khi có phản hồi.'
    ),
    JSON_OBJECT(
        'contractId', contract.lease_contract_id,
        'contractCode', contract.contract_code,
        'roomId', room.room_id,
        'roomName', room.name,
        'roomCode', room.room_code,
        'propertyName', property.name,
        'endDate', contract.end_date,
        'tenantIntention', contract.tenant_intention,
        'targetRoute', CONCAT('/dashboard/contracts/', contract.lease_contract_id)
    ),
    'SENT',
    0,
    3,
    @now,
    @now,
    @now,
    FALSE
FROM hdbhms.lease_contracts contract
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
JOIN hdbhms.properties property
  ON property.property_id = room.property_id
CROSS JOIN (
    SELECT @owner_id AS recipient_user_id
    UNION
    SELECT @manager_id AS recipient_user_id
) recipient
CROSS JOIN (
    SELECT 'WEB' AS channel
    UNION ALL
    SELECT 'PUSH' AS channel
) notification_channel
WHERE contract.lease_contract_id = @c402
  AND contract.status = 'EXPIRING_SOON'
  AND contract.tenant_intention IS NULL
  AND recipient.recipient_user_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM hdbhms.notification_outbox existing_notification
      WHERE existing_notification.event_type = 'CONTRACT_EXPIRING_SOON_REVIEW'
        AND existing_notification.target_type = 'CONTRACT'
        AND existing_notification.target_id = contract.lease_contract_id
        AND existing_notification.recipient_user_id = recipient.recipient_user_id
        AND existing_notification.channel = notification_channel.channel
  );

-- Initialize demo requests from the primary tenant represented by each contract.
-- Keep this block after contract-code normalization so later lifecycle migrations
-- do not need a second seed-only requester alignment migration.
UPDATE hdbhms.change_requests request
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = request.target_id
JOIN hdbhms.person_profiles profile
  ON profile.person_profile_id = contract.primary_tenant_profile_id
SET request.requester_id = profile.user_id,
    request.requester_role = 'TENANT',
    request.updated_at = @now
WHERE request.target_type = 'CONTRACT'
  AND request.request_type IN ('CONTRACT_LIQUIDATION', 'CONTRACT_RENEWAL')
  AND contract.contract_code IN (
      'HDT_P301_01_01_2026',
      'HDT_P302_01_01_2026',
      'HDT_P404_01_09_2025',
      'HDT_P507_01_01_2026'
  );

UPDATE hdbhms.room_transfer_requests transfer_request
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = transfer_request.old_contract_id
JOIN hdbhms.person_profiles profile
  ON profile.person_profile_id = contract.primary_tenant_profile_id
SET transfer_request.requester_id = profile.user_id,
    transfer_request.updated_at = @now
WHERE transfer_request.request_code IN (
      'CP_P406_30_07_2026',
      'CP_P502_30_07_2026',
      'CP_P504_30_07_2026',
      'CP_P506_20_07_2026',
      'CP_P407_14_08_2026'
  );

UPDATE hdbhms.change_requests request
JOIN hdbhms.room_transfer_requests transfer_request
  ON transfer_request.room_transfer_request_id = request.target_id
SET request.requester_id = transfer_request.requester_id,
    request.requester_role = 'TENANT',
    request.updated_at = @now
WHERE request.request_type = 'ROOM_TRANSFER'
  AND request.target_type = 'CONTRACT'
  AND transfer_request.request_code IN (
      'CP_P406_30_07_2026',
      'CP_P502_30_07_2026',
      'CP_P504_30_07_2026',
      'CP_P506_20_07_2026',
      'CP_P407_14_08_2026'
  );

UPDATE hdbhms.change_request_events event
JOIN hdbhms.change_requests request
  ON request.change_request_id = event.request_id
SET event.acted_by = request.requester_id
WHERE event.from_status IS NULL
  AND event.to_status = 'PENDING'
  AND request.request_code IN (
      'TLHD_P301_25_07_2026',
      'TLHD_P302_30_07_2026',
      'TLHD_P507_30_07_2026',
      'GHHD_P404_30_07_2026',
      'CP_P406_30_07_2026',
      'CP_P502_30_07_2026',
      'CP_P504_30_07_2026',
      'CP_P506_20_07_2026',
      'CP_P407_14_08_2026'
  );

-- Keep the expiry-state alignment in the base seed so the reminder timeline
-- below is not undone by V51/V58 compatibility repairs.
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
SET contract.status = 'EXPIRING_SOON',
    contract.tenant_intention = NULL,
    contract.expected_vacant_date = NULL,
    contract.intention_recorded_at = NULL,
    contract.updated_at = @hdd1_seed_now
WHERE room.property_id = @hdd1_property_id
  AND contract.contract_code = 'HDT_P405_01_11_2025'
  AND contract.status = 'ACTIVE'
  AND contract.end_date = '2026-10-31';

-- Rebuild the Hai Dang demo reminder timeline from the contract end date.
-- The previous seed used a fixed 30-day interval, which skipped calendar milestones.
UPDATE hdbhms.reminder_trackers tracker
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = tracker.target_id
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
SET tracker.sent_count =
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
  );

-- Align existing first reminders with their actual three-month milestone.
UPDATE hdbhms.notification_outbox notification
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = notification.target_id
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
SET notification.scheduled_at = CONCAT(DATE_SUB(contract.end_date, INTERVAL 3 MONTH), ' 09:00:00'),
    notification.sent_at = CONCAT(DATE_SUB(contract.end_date, INTERVAL 3 MONTH), ' 09:00:00'),
    notification.payload = JSON_SET(
        COALESCE(notification.payload, JSON_OBJECT()),
        '$.daysRemaining', DATEDIFF(contract.end_date, DATE_SUB(contract.end_date, INTERVAL 3 MONTH)),
        '$.stage', 'FIRST'
    )
WHERE notification.event_type = 'LEASE_EXPIRY_REMINDER_FIRST'
  AND notification.target_type = 'CONTRACT'
  AND room.property_id = @hdd1_property_id
  AND contract.status = 'EXPIRING_SOON'
  AND contract.tenant_intention IS NULL
  AND contract.end_date >= DATE(@hdd1_seed_now);

INSERT INTO hdbhms.notification_outbox
    (event_type, target_type, target_id, recipient_user_id, channel, title, body, payload, status,
     retry_count, max_retries, scheduled_at, sent_at, created_at, is_read)
SELECT
    'LEASE_EXPIRY_REMINDER_SECOND',
    'CONTRACT',
    contract.lease_contract_id,
    tenant_user.user_id,
    'PUSH',
    CONCAT('Bạn chưa phản hồi về hợp đồng ', contract.contract_code),
    CONCAT('Vui lòng chọn ý định cho phòng ', room.name,
           ' trước ngày hết hạn ', contract.end_date,
           ' để quản lý sắp xếp kịp thời.'),
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
JOIN hdbhms.rooms room ON room.room_id = contract.room_id
JOIN hdbhms.properties property ON property.property_id = room.property_id
JOIN hdbhms.person_profiles profile ON profile.person_profile_id = contract.primary_tenant_profile_id
JOIN hdbhms.users tenant_user ON tenant_user.user_id = profile.user_id
WHERE room.property_id = @hdd1_property_id
  AND contract.status = 'EXPIRING_SOON'
  AND contract.tenant_intention IS NULL
  AND contract.end_date >= DATE(@hdd1_seed_now)
  AND DATE_SUB(contract.end_date, INTERVAL 2 MONTH) <= DATE(@hdd1_seed_now)
  AND NOT EXISTS (
      SELECT 1 FROM hdbhms.notification_outbox existing_notification
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
    CONCAT('Nhắc lần cuối về hợp đồng ', contract.contract_code),
    CONCAT('Hợp đồng phòng ', room.name,
           ' sắp hết hạn vào ', contract.end_date,
           '. Vui lòng phản hồi để tránh chậm xử lý bàn giao hoặc gia hạn.'),
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
JOIN hdbhms.rooms room ON room.room_id = contract.room_id
JOIN hdbhms.properties property ON property.property_id = room.property_id
JOIN hdbhms.person_profiles profile ON profile.person_profile_id = contract.primary_tenant_profile_id
JOIN hdbhms.users tenant_user ON tenant_user.user_id = profile.user_id
WHERE room.property_id = @hdd1_property_id
  AND contract.status = 'EXPIRING_SOON'
  AND contract.tenant_intention IS NULL
  AND contract.end_date >= DATE(@hdd1_seed_now)
  AND DATE_SUB(contract.end_date, INTERVAL 1 MONTH) <= DATE(@hdd1_seed_now)
  AND NOT EXISTS (
      SELECT 1 FROM hdbhms.notification_outbox existing_notification
      WHERE existing_notification.event_type = 'LEASE_EXPIRY_REMINDER_FINAL'
        AND existing_notification.target_type = 'CONTRACT'
        AND existing_notification.target_id = contract.lease_contract_id
        AND existing_notification.recipient_user_id = tenant_user.user_id
        AND existing_notification.channel = 'PUSH'
  );

DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_excel_occupants;
DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_excel_rooms;
DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_void_utility_invoices;
DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_final_invoices;
DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_final_state;
DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_old_invoices;
DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_old_payment_transactions;
DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_old_readings;
DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_old_batches;
DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_contract_code_map;
