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
    end_date = '2026-08-30',
    tenant_intention = NULL,
    expected_vacant_date = NULL,
    intention_recorded_at = NULL,
    updated_at = '2026-08-01 08:00:00'
WHERE lease_contract_id = @c403;

UPDATE hdbhms.rooms
SET current_status = 'OCCUPIED',
    public_note = 'Seed: Room 403 contract expires on 2026-08-30; liquidation flow has not started.',
    internal_note = 'No liquidation intention, request, task, or settlement exists yet.',
    updated_at = '2026-08-01 08:00:00'
WHERE room_id = @r403;

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
    ('101', 1, 'Nguyễn Minh Quân', 'nguyen.minh.quan.101@haidang1.local', '0987346215', '1998-03-12', 'MALE'),
    ('101', 2, 'Trần Ngọc Anh', 'tran.ngoc.anh.101@haidang1.local', '0912865074', '1999-08-21', 'FEMALE'),
    ('102', 1, 'Lê Hoàng Nam', 'le.hoang.nam.102@haidang1.local', '0975128436', '1997-06-18', 'MALE'),
    ('103', 1, 'Phạm Gia Huy', 'pham.gia.huy.103@haidang1.local', '0328694175', '2000-02-04', 'MALE'),
    ('104', 1, 'Vũ Ngọc Mai', 'vu.ngoc.mai.104@haidang1.local', '0931786402', '1998-11-23', 'FEMALE'),
    ('104', 2, 'Trần Thu Hà', 'tran.thu.ha.104@haidang1.local', '0864597231', '1997-05-16', 'FEMALE'),
    ('105', 1, 'Phạm Quốc Bảo', 'pham.quoc.bao.105@haidang1.local', '0906831574', '1996-09-09', 'MALE'),
    ('105', 2, 'Hoàng Mỹ Linh', 'hoang.my.linh.105@haidang1.local', '0397246815', '1999-01-27', 'FEMALE'),
    ('106', 1, 'Đặng Thành Nam', 'dang.thanh.nam.106@haidang1.local', '0981564307', '1998-07-11', 'MALE'),
    ('201', 1, 'Bùi Đức Long', 'bui.duc.long.201@haidang1.local', '0917382056', '1997-12-02', 'MALE'),
    ('202', 1, 'Nguyễn Thùy Dương', 'nguyen.thuy.duong.202@haidang1.local', '0974683192', '1999-04-19', 'FEMALE'),
    ('202', 2, 'Phan Minh Đức', 'phan.minh.duc.202@haidang1.local', '0325147896', '1998-10-30', 'MALE'),
    ('202', 3, 'Lê Ngọc Lan', 'le.ngoc.lan.202@haidang1.local', '0936251478', '2000-06-07', 'FEMALE'),
    ('203', 1, 'Đỗ Hoàng Anh', 'do.hoang.anh.203@haidang1.local', '0861739542', '1997-03-25', 'MALE'),
    ('204', 1, 'Trịnh Hải Yến', 'trinh.hai.yen.204@haidang1.local', '0908452761', '1998-02-14', 'FEMALE'),
    ('205', 1, 'Võ Minh Khang', 'vo.minh.khang.205@haidang1.local', '0396815304', '1996-08-06', 'MALE'),
    ('206', 1, 'Nguyễn Anh Tuấn', 'nguyen.anh.tuan.206@haidang1.local', '0982374165', '1997-01-22', 'MALE'),
    ('206', 2, 'Đặng Thu Trang', 'dang.thu.trang.206@haidang1.local', '0916048273', '1999-09-13', 'FEMALE'),
    ('207', 1, 'Trần Đức Duy', 'tran.duc.duy.207@haidang1.local', '0978153602', '1998-12-18', 'MALE'),
    ('207', 2, 'Nguyễn Khánh Linh', 'nguyen.khanh.linh.207@haidang1.local', '0327469185', '2000-03-29', 'FEMALE'),
    ('208', 1, 'Phạm Hồng Sơn', 'pham.hong.son.208@haidang1.local', '0934526718', '1996-05-08', 'MALE'),
    ('208', 2, 'Lê Quỳnh Chi', 'le.quynh.chi.208@haidang1.local', '0862389405', '1999-11-01', 'FEMALE'),
    ('301', 1, 'Nguyễn Văn Khải', 'nguyen.van.khai.301@haidang1.local', '0918526407', '1995-07-17', 'MALE'),
    ('301', 2, 'Bùi Thanh Hà', 'bui.thanh.ha.301@haidang1.local', '0905718642', '1998-04-05', 'FEMALE'),
    ('301', 3, 'Trần Gia Bảo', 'tran.gia.bao.301@haidang1.local', '0398152746', '2000-01-31', 'MALE'),
    ('302', 1, 'Đỗ Minh Tâm', 'do.minh.tam.302@haidang1.local', '0918526407', '1997-10-12', 'MALE'),
    ('302', 2, 'Phạm Ngọc Hân', 'pham.ngoc.han.302@haidang1.local', '0986041735', '1999-06-26', 'FEMALE'),
    ('302', 3, 'Nguyễn Đức Anh', 'nguyen.duc.anh.302@haidang1.local', '0973265841', '1998-08-15', 'MALE'),
    ('303', 1, 'Lê Quốc Việt', 'le.quoc.viet.303@haidang1.local', '0918526407', '1996-02-20', 'MALE'),
    ('305', 1, 'Hoàng Anh Tuấn', 'hoang.anh.tuan.305@haidang1.local', '0329584176', '1997-09-28', 'MALE'),
    ('306', 1, 'Nguyễn Thị Mai', 'nguyen.thi.mai.306@haidang1.local', '0937061528', '1998-12-10', 'FEMALE'),
    ('306', 2, 'Trần Văn Phúc', 'tran.van.phuc.306@haidang1.local', '0865147932', '1996-03-03', 'MALE'),
    ('307', 1, 'Vũ Thành Đạt', 'vu.thanh.dat.307@haidang1.local', '0903186574', '1999-05-24', 'MALE'),
    ('307', 2, 'Phạm Thùy Linh', 'pham.thuy.linh.307@haidang1.local', '0394628150', '2000-09-18', 'FEMALE'),
    ('308', 1, 'Nguyễn Hải Đăng', 'nguyen.hai.dang.308@haidang1.local', '0987451036', '1997-11-11', 'MALE'),
    ('401', 1, 'Nguyễn Văn Hùng', 'nguyen.van.hung.401@haidang1.local', '0912736845', '1995-04-27', 'MALE'),
    ('401', 2, 'Trần Thị Hương', 'tran.thi.huong.401@haidang1.local', '0976382051', '1998-07-02', 'FEMALE'),
    ('401', 3, 'Phạm Minh Châu', 'pham.minh.chau.401@haidang1.local', '0328174962', '1999-12-22', 'FEMALE'),
     ('402', 1, 'Nguyễn Đức Thịnh', 'nguyen.duc.thinh.402@haidang1.local', '0935841607', '1996-06-15', 'MALE'),
     ('402', 2, 'Lê Thu Trang', 'le.thu.trang.402@haidang1.local', '0869273154', '1998-10-08', 'FEMALE'),
     ('402', 3, 'Võ Thanh Tùng', 'vo.thanh.tung.402@haidang1.local', '0907524816', '1997-01-09', 'MALE'),
     ('403', 1, 'Nguyen Duc Thinh', 'nguyen.duc.thinh.403@haidang1.local', '0395317684', '1996-06-15', 'MALE'),
     ('405', 1, 'Dương Minh Đức', 'duong.minh.duc.405@haidang1.local', '0981682057', '1996-05-30', 'MALE'),
    ('406', 1, 'Nguyễn Hoài Nam', 'nguyen.hoai.nam.406@haidang1.local', '0917463925', '1999-02-17', 'MALE'),
    ('408', 1, 'Phạm Thị Hoa', 'pham.thi.hoa.408@haidang1.local', '0974806153', '1997-08-25', 'FEMALE'),
    ('501', 1, 'Lê Văn Phúc', 'le.van.phuc.501@haidang1.local', '0326951748', '1995-11-14', 'MALE'),
    ('501', 2, 'Nguyễn Thị Hạnh', 'nguyen.thi.hanh.501@haidang1.local', '0932618574', '1998-03-06', 'FEMALE'),
    ('502', 1, 'Hoàng Văn Nam', 'hoang.van.nam.502@haidang1.local', '0864739201', '1997-07-19', 'MALE'),
    ('502', 2, 'Trần Ngọc Bích', 'tran.ngoc.bich.502@haidang1.local', '0906842157', '1999-02-11', 'FEMALE'),
    ('503', 1, 'Bùi Minh Khoa', 'bui.minh.khoa.503@haidang1.local', '0397824605', '1996-10-21', 'MALE'),
    ('504', 1, 'Nguyễn Thị Vân', 'nguyen.thi.van.504@haidang1.local', '0985137642', '1998-06-03', 'FEMALE'),
    ('505', 1, 'Phan Quốc Khánh', 'phan.quoc.khanh.505@haidang1.local', '0916284057', '1997-09-07', 'MALE'),
    ('506', 1, 'Đặng Văn Hòa', 'dang.van.hoa.506@haidang1.local', '0973518264', '1996-12-16', 'MALE'),
    ('507', 1, 'Nguyễn Minh Khôi', 'nguyen.minh.khoi.507@haidang1.local', '0328741659', '1998-01-28', 'MALE'),
    ('507', 2, 'Trần Diệu Linh', 'tran.dieu.linh.507@haidang1.local', '0937195206', '2000-05-12', 'FEMALE');

-- One tenant account intentionally holds three rooms so account-level room
-- filtering and contract visibility can be exercised with realistic data.
UPDATE tmp_hdd1_excel_occupants
SET full_name = 'Nguyễn Văn Khải',
    email = 'nguyen.van.khai@haidang1.local',
    phone = '0918526407',
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
    phone = '0946128375'
WHERE email = 'seed.tenant@hdbhms.local';

UPDATE hdbhms.person_profiles
SET full_name = 'Đỗ Thị Lan',
    email = 'do.thi.lan@haidang1.local',
    phone = '0385726409'
WHERE email = 'seed.tenant405.co@hdbhms.local';

UPDATE hdbhms.person_profiles
SET full_name = 'Đặng Thị Hương',
    email = 'dang.thi.huong@haidang1.local',
    phone = '0874319652'
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

-- Room 306 is the active transfer demo. Keep the start date early enough for
-- the 2/3 tenure rule while keeping the contract active at the seed date.
UPDATE hdbhms.lease_contracts contract
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
SET contract.start_date = '2026-01-01',
    contract.rent_start_date = '2026-01-01',
    contract.end_date = '2026-11-30'
WHERE room.property_id = @property_id
  AND room.room_code = '306'
  AND contract.deleted_at IS NULL
  AND contract.contract_code = 'HD-HDD1-306-2026';

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

-- Keep the expiry scenarios after applying the workbook occupancy snapshot.
-- Rooms 301-303 share Nguyễn Văn Khải's account and represent the 3/2/1-month
-- milestones; room 402 remains a separate expiring-contract scenario.
UPDATE hdbhms.lease_contracts contract
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
SET contract.status = CASE room.room_code
        WHEN '301' THEN 'ACTIVE'
        ELSE 'EXPIRING_SOON'
    END,
    contract.end_date = CASE room.room_code
        WHEN '301' THEN '2026-12-31'
        WHEN '302' THEN '2026-10-31'
        WHEN '303' THEN '2026-09-01'
        WHEN '402' THEN '2026-09-15'
    END,
    contract.tenant_intention = NULL,
    contract.expected_vacant_date = NULL,
    contract.intention_recorded_at = NULL,
    contract.updated_at = @now
WHERE room.property_id = @property_id
  AND room.room_code IN ('301', '302', '303', '402')
  AND contract.deleted_at IS NULL
  AND contract.status IN ('ACTIVE', 'EXPIRING_SOON', 'TERMINATION_PENDING');

UPDATE hdbhms.rooms
SET current_status = CASE room_code
        WHEN '301' THEN 'OCCUPIED'
        ELSE 'SOON_VACANT'
    END,
    public_note = CASE room_code
        WHEN '301' THEN NULL
        WHEN '302' THEN 'Hợp đồng sắp hết hạn ngày 31/10/2026, đang chờ khách phản hồi ý định.'
        WHEN '303' THEN 'Hợp đồng sắp hết hạn ngày 01/09/2026, còn khoảng 1 tháng, đang chờ khách phản hồi ý định.'
        WHEN '402' THEN 'Hợp đồng sắp hết hạn ngày 15/09/2026, đang chờ khách phản hồi ý định.'
    END,
    internal_note = 'Chưa ghi nhận ý định gia hạn, chuyển phòng hoặc chuyển đi.',
    updated_at = @now
WHERE property_id = @property_id
  AND room_code IN ('301', '302', '303', '402')
  AND deleted_at IS NULL;

-- Rebuild expiry reminders after changing the dates so no notification keeps
-- the previous room, recipient, or expiry date.
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
  AND room.property_id = @property_id
  AND room.room_code IN ('301', '302', '303', '402')
  AND contract.status = 'EXPIRING_SOON';

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
  AND room.property_id = @property_id
  AND room.room_code IN ('301', '302', '303', '402')
  AND contract.status = 'EXPIRING_SOON';

DELETE tracker
FROM hdbhms.reminder_trackers tracker
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = tracker.target_id
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
WHERE tracker.reminder_key = 'LEASE_EXPIRY_INTENTION'
  AND tracker.target_type = 'CONTRACT'
  AND room.property_id = @property_id
  AND room.room_code IN ('301', '302', '303', '402')
  AND contract.status = 'EXPIRING_SOON';

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
SET reading.previous_value = CASE room.room_code
        WHEN '304' THEN 1945
        WHEN '407' THEN 867
        ELSE reading.previous_value
    END,
    reading.current_value = CASE room.room_code
        WHEN '304' THEN 1955
        WHEN '407' THEN 869
        ELSE reading.current_value
    END
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
-- utility charge in the seed. These temporary zero-value rows are cleaned up
-- by the final V48 overlay below.
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
SET invoice.status = 'PAID',
    invoice.subtotal_amount = 0,
    invoice.discount_amount = 0,
    invoice.total_amount = 0,
    invoice.paid_amount = 0,
    invoice.remaining_amount = 0,
    invoice.voided_at = NULL,
    invoice.void_reason = NULL,
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

-- Notify the owner and manager about expiring contracts without fabricating a
-- manager task before each tenant chooses renewal, transfer, or move-out.
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
WHERE room.room_code IN ('301', '302', '303', '402')
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
SET @hdd1_seed_now := '2026-08-20 09:00:00';

UPDATE hdbhms.lease_contracts contract
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
SET contract.status = CASE room.room_code
        WHEN '301' THEN 'ACTIVE'
        ELSE 'EXPIRING_SOON'
    END,
    contract.end_date = CASE room.room_code
        WHEN '301' THEN '2026-12-31'
        WHEN '302' THEN '2026-10-31'
        WHEN '303' THEN '2026-09-01'
    END,
    contract.tenant_intention = NULL,
    contract.expected_vacant_date = NULL,
    contract.intention_recorded_at = NULL,
    contract.updated_at = @hdd1_seed_now
WHERE room.property_id = @hdd1_property_id
  AND room.room_code IN ('301', '302', '303')
  AND contract.deleted_at IS NULL
  AND contract.status IN ('ACTIVE', 'EXPIRING_SOON', 'TERMINATION_PENDING');

UPDATE hdbhms.rooms room
SET room.current_status = CASE room.room_code
        WHEN '301' THEN 'OCCUPIED'
        ELSE 'SOON_VACANT'
    END,
    room.public_note = CASE room.room_code
        WHEN '301' THEN NULL
        WHEN '302' THEN 'Hợp đồng sắp hết hạn ngày 31/10/2026.'
        WHEN '303' THEN 'Hợp đồng còn khoảng 1 tháng, hết hạn ngày 01/09/2026.'
    END,
    room.internal_note = 'Demo mốc nhắc hợp đồng 3/2/1 tháng của Nguyễn Văn Khải.',
    room.updated_at = @hdd1_seed_now
WHERE room.property_id = @hdd1_property_id
  AND room.room_code IN ('301', '302', '303')
  AND room.deleted_at IS NULL;

DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_expiry_recipients;
CREATE TEMPORARY TABLE tmp_hdd1_expiry_recipients
(
    contract_id BIGINT UNSIGNED NOT NULL,
    recipient_user_id BIGINT UNSIGNED NOT NULL,
    audience VARCHAR(50) NOT NULL,
    PRIMARY KEY (contract_id, recipient_user_id, audience)
);

INSERT INTO tmp_hdd1_expiry_recipients (contract_id, recipient_user_id, audience)
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
      AND contract.status = 'EXPIRING_SOON'
      AND contract.tenant_intention IS NULL
      AND contract.end_date >= DATE(@hdd1_seed_now)
      AND contract.deleted_at IS NULL
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
      AND contract.status = 'EXPIRING_SOON'
      AND contract.tenant_intention IS NULL
      AND contract.end_date >= DATE(@hdd1_seed_now)
      AND contract.deleted_at IS NULL
) recipients
GROUP BY contract_id, recipient_user_id, audience;

-- Rebuild only the expiry reminder state. Handover/renewal trackers and their
-- manager notifications belong to different branches of the workflow.
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
  AND contract.status = 'EXPIRING_SOON';

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
  AND contract.status = 'EXPIRING_SOON';

DELETE tracker
FROM hdbhms.reminder_trackers tracker
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = tracker.target_id
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
WHERE tracker.reminder_key = 'LEASE_EXPIRY_INTENTION'
  AND tracker.target_type = 'CONTRACT'
  AND room.property_id = @hdd1_property_id
  AND contract.status = 'EXPIRING_SOON';

INSERT INTO hdbhms.reminder_trackers
    (reminder_key, target_type, target_id, audience, recipient_user_id, status, sent_count,
     last_sent_at, next_due_at, metadata, created_at, updated_at)
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
JOIN tmp_hdd1_expiry_recipients recipient
  ON recipient.contract_id = contract.lease_contract_id
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
        AND existing_tracker.audience = recipient.audience
        AND existing_tracker.recipient_user_id = recipient.recipient_user_id
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
    'LEASE_EXPIRY_REMINDER_FIRST',
    'CONTRACT',
    contract.lease_contract_id,
    recipient.recipient_user_id,
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
    'SENT', 0, 3,
    CONCAT(DATE_SUB(contract.end_date, INTERVAL 3 MONTH), ' 09:00:00'),
    CONCAT(DATE_SUB(contract.end_date, INTERVAL 3 MONTH), ' 09:00:00'),
    CONCAT(DATE_SUB(contract.end_date, INTERVAL 3 MONTH), ' 09:00:00'),
    FALSE
FROM hdbhms.lease_contracts contract
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
JOIN hdbhms.properties property
  ON property.property_id = room.property_id
JOIN tmp_hdd1_expiry_recipients recipient
  ON recipient.contract_id = contract.lease_contract_id
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
        AND existing_notification.recipient_user_id = recipient.recipient_user_id
        AND existing_notification.channel = 'PUSH'
  );

INSERT INTO hdbhms.notification_outbox
    (event_type, target_type, target_id, recipient_user_id, channel, title, body, payload, status,
     retry_count, max_retries, scheduled_at, sent_at, created_at, is_read)
SELECT
    'LEASE_EXPIRY_REMINDER_SECOND',
    'CONTRACT',
    contract.lease_contract_id,
    recipient.recipient_user_id,
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
JOIN tmp_hdd1_expiry_recipients recipient
  ON recipient.contract_id = contract.lease_contract_id
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
        AND existing_notification.recipient_user_id = recipient.recipient_user_id
        AND existing_notification.channel = 'PUSH'
  );

INSERT INTO hdbhms.notification_outbox
    (event_type, target_type, target_id, recipient_user_id, channel, title, body, payload, status,
     retry_count, max_retries, scheduled_at, sent_at, created_at, is_read)
SELECT
    'LEASE_EXPIRY_REMINDER_FINAL',
    'CONTRACT',
    contract.lease_contract_id,
    recipient.recipient_user_id,
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
JOIN tmp_hdd1_expiry_recipients recipient
  ON recipient.contract_id = contract.lease_contract_id
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
        AND existing_notification.recipient_user_id = recipient.recipient_user_id
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
DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_expiry_recipients;

-- Final August availability overlay. Keep this at the end of V48 so the
-- source seed itself owns the requested room state before repair migrations.
SET @hdd1_final_property_id := (
    SELECT property_id
    FROM hdbhms.properties
    WHERE property_code = 'HAI_DANG_1'
      AND deleted_at IS NULL
    LIMIT 1
);
SET @hdd1_final_now := '2026-08-20 09:00:00';
SET @hdd1_final_manager_id := (
    SELECT user_id
    FROM hdbhms.users
    WHERE deleted_at IS NULL
      AND status = 'ACTIVE'
      AND email IN (
          'seed.manager@hdbhms.local',
          'seed.owner@hdbhms.local',
          'tranthuhuong90@gmail.com',
          'nguyenminhquang80@gmail.com'
      )
    ORDER BY CASE
        WHEN email = 'seed.manager@hdbhms.local' THEN 0
        WHEN email = 'seed.owner@hdbhms.local' THEN 1
        ELSE 2
    END, user_id
    LIMIT 1
);
SET @hdd1_final_khai_user_id := (
    SELECT user_id
    FROM hdbhms.users
    WHERE (
        phone = '0901309001'
        OR email IN (
            'nguyen.van.khai@haidang1.local',
            'nguyenvankhai95@gmail.com'
        )
    )
      AND deleted_at IS NULL
    ORDER BY CASE WHEN phone = '0901309001' THEN 0 ELSE 1 END, user_id
    LIMIT 1
);
SET @hdd1_final_khai_email := (
    SELECT email
    FROM hdbhms.users
    WHERE user_id = @hdd1_final_khai_user_id
    LIMIT 1
);
SET @hdd1_final_khai_tenant_id := (
    SELECT tenant_id
    FROM hdbhms.tenants
    WHERE user_id = @hdd1_final_khai_user_id
      AND property_id = @hdd1_final_property_id
      AND deleted_at IS NULL
    LIMIT 1
);
SET @hdd1_final_khai_profile_id := (
    SELECT person_profile_id
    FROM hdbhms.person_profiles
    WHERE user_id = @hdd1_final_khai_user_id
      AND deleted_at IS NULL
    LIMIT 1
);

DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_final_khai_rooms;
CREATE TEMPORARY TABLE tmp_hdd1_final_khai_rooms
(
    room_code VARCHAR(10) NOT NULL PRIMARY KEY
);
INSERT INTO tmp_hdd1_final_khai_rooms (room_code)
VALUES ('306'), ('501');

UPDATE hdbhms.contract_occupants occupant
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = occupant.contract_id
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
JOIN tmp_hdd1_final_khai_rooms selected_room
  ON selected_room.room_code = room.room_code
SET occupant.status = 'MOVED_OUT',
    occupant.move_out_date = DATE(@hdd1_final_now),
    occupant.disabled_reason = 'Reassigned to Nguyen Van Khai in the August seed.',
    occupant.disabled_by = @hdd1_final_manager_id,
    occupant.disabled_at = @hdd1_final_now
WHERE room.property_id = @hdd1_final_property_id
  AND occupant.occupant_role = 'PRIMARY'
  AND occupant.status = 'ACTIVE'
  AND occupant.tenant_profile_id <> @hdd1_final_khai_profile_id;

UPDATE hdbhms.lease_contracts contract
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
JOIN tmp_hdd1_final_khai_rooms selected_room
  ON selected_room.room_code = room.room_code
SET contract.primary_tenant_profile_id = @hdd1_final_khai_profile_id,
    contract.status = 'ACTIVE',
    contract.end_date = CASE
        WHEN selected_room.room_code = '306' THEN '2026-11-30'
        ELSE contract.end_date
    END,
    contract.tenant_intention = NULL,
    contract.expected_vacant_date = NULL,
    contract.intention_recorded_at = NULL,
    contract.updated_at = @hdd1_final_now
WHERE room.property_id = @hdd1_final_property_id
  AND contract.deleted_at IS NULL
  AND contract.status IN ('ACTIVE', 'EXPIRING_SOON', 'TERMINATION_PENDING');

UPDATE hdbhms.contract_occupants occupant
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = occupant.contract_id
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
JOIN tmp_hdd1_final_khai_rooms selected_room
  ON selected_room.room_code = room.room_code
SET occupant.tenant_id = @hdd1_final_khai_tenant_id,
    occupant.occupant_role = 'PRIMARY',
    occupant.move_in_date = DATE(@hdd1_final_now),
    occupant.move_out_date = NULL,
    occupant.status = 'ACTIVE',
    occupant.disabled_reason = NULL,
    occupant.disabled_by = NULL,
    occupant.disabled_at = NULL
WHERE room.property_id = @hdd1_final_property_id
  AND occupant.tenant_profile_id = @hdd1_final_khai_profile_id;

INSERT INTO hdbhms.contract_occupants
    (contract_id, tenant_id, tenant_profile_id, occupant_role, move_in_date,
     move_out_date, status, disabled_reason, disabled_by, disabled_at, created_at)
SELECT contract.lease_contract_id,
       @hdd1_final_khai_tenant_id,
       @hdd1_final_khai_profile_id,
       'PRIMARY',
       DATE(@hdd1_final_now),
       NULL,
       'ACTIVE',
       NULL,
       NULL,
       NULL,
       @hdd1_final_now
FROM hdbhms.lease_contracts contract
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
JOIN tmp_hdd1_final_khai_rooms selected_room
  ON selected_room.room_code = room.room_code
WHERE room.property_id = @hdd1_final_property_id
  AND contract.deleted_at IS NULL
  AND contract.status = 'ACTIVE'
  AND NOT EXISTS (
      SELECT 1
      FROM hdbhms.contract_occupants existing_occupant
      WHERE existing_occupant.contract_id = contract.lease_contract_id
        AND existing_occupant.tenant_profile_id = @hdd1_final_khai_profile_id
  );

UPDATE hdbhms.rooms room
JOIN tmp_hdd1_final_khai_rooms selected_room
  ON selected_room.room_code = room.room_code
SET room.current_status = 'OCCUPIED',
    room.public_note = 'Current contract assigned to Nguyen Van Khai.',
    room.internal_note = 'Seed final state: active contract assigned to Nguyen Van Khai.',
    room.updated_at = @hdd1_final_now
WHERE room.property_id = @hdd1_final_property_id
  AND room.deleted_at IS NULL;

DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_final_release_rooms;
CREATE TEMPORARY TABLE tmp_hdd1_final_release_rooms
(
    room_code VARCHAR(10) NOT NULL PRIMARY KEY
);
INSERT INTO tmp_hdd1_final_release_rooms (room_code)
VALUES ('101'), ('102'), ('403'), ('405');

UPDATE hdbhms.lease_contracts contract
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
JOIN tmp_hdd1_final_release_rooms selected_room
  ON selected_room.room_code = room.room_code
SET contract.status = 'AUTO_TERMINATED',
    contract.tenant_intention = 'MOVE_OUT',
    contract.expected_vacant_date = DATE(@hdd1_final_now),
    contract.intention_recorded_at = @hdd1_final_now,
    contract.updated_at = @hdd1_final_now
WHERE room.property_id = @hdd1_final_property_id
  AND contract.deleted_at IS NULL
  AND contract.status IN ('ACTIVE', 'EXPIRING_SOON', 'TERMINATION_PENDING');

UPDATE hdbhms.contract_occupants occupant
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = occupant.contract_id
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
JOIN tmp_hdd1_final_release_rooms selected_room
  ON selected_room.room_code = room.room_code
SET occupant.status = 'MOVED_OUT',
    occupant.move_out_date = DATE(@hdd1_final_now),
    occupant.disabled_reason = 'Room released in the August seed.',
    occupant.disabled_by = @hdd1_final_manager_id,
    occupant.disabled_at = @hdd1_final_now
WHERE room.property_id = @hdd1_final_property_id
  AND occupant.status = 'ACTIVE';

INSERT INTO hdbhms.contract_events
    (contract_id, event_type, event_data, created_by, created_at)
SELECT contract.lease_contract_id,
       'AUTO_TERMINATED',
       CAST(JSON_OBJECT(
           'source', 'V48_SEED_AUGUST_ROOM_RELEASE',
           'releasedAt', DATE_FORMAT(@hdd1_final_now, '%Y-%m-%dT%H:%i:%s'),
           'roomCode', room.room_code
       ) AS BINARY),
       @hdd1_final_manager_id,
       @hdd1_final_now
FROM hdbhms.lease_contracts contract
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
JOIN tmp_hdd1_final_release_rooms selected_room
  ON selected_room.room_code = room.room_code
WHERE room.property_id = @hdd1_final_property_id
  AND contract.status = 'AUTO_TERMINATED'
  AND NOT EXISTS (
      SELECT 1
      FROM hdbhms.contract_events existing_event
      WHERE existing_event.contract_id = contract.lease_contract_id
        AND existing_event.event_type = 'AUTO_TERMINATED'
  );

INSERT INTO hdbhms.room_status_history
    (room_id, from_status, to_status, reason, changed_by, changed_at)
SELECT room.room_id,
       room.current_status,
       'VACANT',
       'August seed room release completed.',
       @hdd1_final_manager_id,
       @hdd1_final_now
FROM hdbhms.rooms room
JOIN tmp_hdd1_final_release_rooms selected_room
  ON selected_room.room_code = room.room_code
WHERE room.property_id = @hdd1_final_property_id
  AND room.current_status <> 'VACANT'
  AND NOT EXISTS (
      SELECT 1
      FROM hdbhms.room_status_history existing_history
      WHERE existing_history.room_id = room.room_id
        AND existing_history.to_status = 'VACANT'
        AND existing_history.reason = 'August seed room release completed.'
  );

UPDATE hdbhms.rooms room
JOIN tmp_hdd1_final_release_rooms selected_room
  ON selected_room.room_code = room.room_code
SET room.current_status = 'VACANT',
    room.public_note = 'Vacant from August 2026 seed release.',
    room.internal_note = 'Seed final state: released after contract auto-termination.',
    room.updated_at = @hdd1_final_now
WHERE room.property_id = @hdd1_final_property_id
  AND room.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM hdbhms.lease_contracts active_contract
      WHERE active_contract.room_id = room.room_id
        AND active_contract.deleted_at IS NULL
        AND active_contract.status IN ('ACTIVE', 'EXPIRING_SOON', 'TERMINATION_PENDING')
  );

DELETE delivery
FROM hdbhms.notification_deliveries delivery
JOIN hdbhms.notification_outbox notification
  ON notification.notification_outbox_id = delivery.outbox_id
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = notification.target_id
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
JOIN tmp_hdd1_final_release_rooms selected_room
  ON selected_room.room_code = room.room_code
WHERE notification.target_type = 'CONTRACT'
  AND (
      notification.event_type LIKE 'LEASE_EXPIRY%'
      OR notification.event_type = 'CONTRACT_EXPIRING_SOON_REVIEW'
  );

DELETE notification
FROM hdbhms.notification_outbox notification
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = notification.target_id
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
JOIN tmp_hdd1_final_release_rooms selected_room
  ON selected_room.room_code = room.room_code
WHERE notification.target_type = 'CONTRACT'
  AND (
      notification.event_type LIKE 'LEASE_EXPIRY%'
      OR notification.event_type = 'CONTRACT_EXPIRING_SOON_REVIEW'
  );

DELETE tracker
FROM hdbhms.reminder_trackers tracker
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = tracker.target_id
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
JOIN tmp_hdd1_final_release_rooms selected_room
  ON selected_room.room_code = room.room_code
WHERE tracker.target_type = 'CONTRACT'
  AND tracker.reminder_key LIKE 'LEASE_EXPIRY%';

-- Delete the three zero-value utility invoices instead of retaining fake paid
-- audit rows. July non-zero invoices and their payment allocations are not
-- touched by this cleanup.
DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_final_zero_invoices;
CREATE TEMPORARY TABLE tmp_hdd1_final_zero_invoices
(
    invoice_id BIGINT UNSIGNED NOT NULL PRIMARY KEY
);
INSERT INTO tmp_hdd1_final_zero_invoices (invoice_id)
SELECT invoice.invoice_id
FROM hdbhms.invoices invoice
JOIN hdbhms.rooms room
  ON room.room_id = invoice.room_id
WHERE invoice.property_id = @hdd1_final_property_id
  AND room.property_id = @hdd1_final_property_id
  AND room.room_code IN ('404', '502', '504')
  AND invoice.invoice_code IN (
      'HD_P404_01_08_2026_DV',
      'HD_P502_01_08_2026_DV',
      'HD_P504_01_08_2026_DV'
  )
  AND invoice.total_amount = 0;

DELETE delivery
FROM hdbhms.notification_deliveries delivery
JOIN hdbhms.notification_outbox notification
  ON notification.notification_outbox_id = delivery.outbox_id
JOIN tmp_hdd1_final_zero_invoices target_invoice
  ON target_invoice.invoice_id = notification.target_id
WHERE notification.target_type = 'INVOICE';
DELETE notification
FROM hdbhms.notification_outbox notification
JOIN tmp_hdd1_final_zero_invoices target_invoice
  ON target_invoice.invoice_id = notification.target_id
WHERE notification.target_type = 'INVOICE';
DELETE allocation
FROM hdbhms.payment_allocations allocation
JOIN tmp_hdd1_final_zero_invoices target_invoice
  ON target_invoice.invoice_id = allocation.invoice_id;
UPDATE hdbhms.deposit_batches deposit_batch
JOIN tmp_hdd1_final_zero_invoices target_invoice
  ON target_invoice.invoice_id = deposit_batch.invoice_id
SET deposit_batch.invoice_id = NULL,
    deposit_batch.updated_at = @hdd1_final_now;
UPDATE hdbhms.payment_intents payment_intent
JOIN tmp_hdd1_final_zero_invoices target_invoice
  ON target_invoice.invoice_id = payment_intent.invoice_id
SET payment_intent.invoice_id = NULL;
UPDATE hdbhms.pending_billing_charges pending_charge
JOIN tmp_hdd1_final_zero_invoices target_invoice
  ON target_invoice.invoice_id = pending_charge.invoice_id
SET pending_charge.invoice_id = NULL,
    pending_charge.status = 'CANCELLED',
    pending_charge.updated_at = @hdd1_final_now;
UPDATE hdbhms.room_utility_baselines baseline
JOIN tmp_hdd1_final_zero_invoices target_invoice
  ON target_invoice.invoice_id = baseline.last_invoice_id
SET baseline.last_invoice_id = NULL,
    baseline.updated_at = @hdd1_final_now;
UPDATE hdbhms.rule_violations violation
JOIN tmp_hdd1_final_zero_invoices target_invoice
  ON target_invoice.invoice_id = violation.invoice_id
SET violation.invoice_id = NULL,
    violation.status = CASE
        WHEN violation.status = 'INVOICED' THEN 'RECORDED'
        ELSE violation.status
    END;
UPDATE hdbhms.utility_billing_run_items item
JOIN tmp_hdd1_final_zero_invoices target_invoice
  ON target_invoice.invoice_id = item.invoice_id
SET item.invoice_id = NULL,
    item.status = 'SKIPPED',
    item.adjustment_reason = 'Removed zero-value seed invoice.';
UPDATE hdbhms.invoices invoice
JOIN tmp_hdd1_final_zero_invoices target_invoice
  ON target_invoice.invoice_id = invoice.invoice_id
SET invoice.status = 'DRAFT',
    invoice.updated_at = @hdd1_final_now;
DELETE line
FROM hdbhms.invoice_lines line
JOIN tmp_hdd1_final_zero_invoices target_invoice
  ON target_invoice.invoice_id = line.invoice_id;
DELETE invoice
FROM hdbhms.invoices invoice
JOIN tmp_hdd1_final_zero_invoices target_invoice
  ON target_invoice.invoice_id = invoice.invoice_id;

UPDATE hdbhms.tenant_account_provisionings provisioning
SET provisioning.user_id = @hdd1_final_khai_user_id,
    provisioning.first_contract_id = (
        SELECT MIN(contract.lease_contract_id)
        FROM hdbhms.lease_contracts contract
        JOIN hdbhms.rooms room ON room.room_id = contract.room_id
        WHERE contract.primary_tenant_profile_id = @hdd1_final_khai_profile_id
          AND room.property_id = @hdd1_final_property_id
          AND contract.deleted_at IS NULL
          AND contract.status IN ('ACTIVE', 'EXPIRING_SOON', 'SIGNED', 'CONFIRMED')
    ),
    provisioning.latest_contract_id = (
        SELECT MAX(contract.lease_contract_id)
        FROM hdbhms.lease_contracts contract
        JOIN hdbhms.rooms room ON room.room_id = contract.room_id
        WHERE contract.primary_tenant_profile_id = @hdd1_final_khai_profile_id
          AND room.property_id = @hdd1_final_property_id
          AND contract.deleted_at IS NULL
          AND contract.status IN ('ACTIVE', 'EXPIRING_SOON', 'SIGNED', 'CONFIRMED')
    ),
    provisioning.status = 'ACTIVE',
    provisioning.recipient_email = @hdd1_final_khai_email,
    provisioning.updated_at = @hdd1_final_now
WHERE provisioning.tenant_profile_id = @hdd1_final_khai_profile_id;

DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_final_zero_invoices;
DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_final_release_rooms;
DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_final_khai_rooms;

SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- Backfill the notification rows that the application would create for the
-- final Hai Dang July invoice snapshot. Keep this migration additive so it
-- does not overwrite notifications created manually or by a later flow.
SET @hdd1_notification_property_id := (
    SELECT property_id
    FROM hdbhms.properties
    WHERE property_code = 'HAI_DANG_1'
      AND deleted_at IS NULL
    LIMIT 1
);
SET @hdd1_notification_now := '2026-08-20 09:00:00';

DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_notification_tenant_recipients;
CREATE TEMPORARY TABLE tmp_hdd1_notification_tenant_recipients
(
    invoice_id BIGINT UNSIGNED NOT NULL,
    recipient_user_id BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (invoice_id, recipient_user_id)
);

-- This matches the room-based recipient lookup used by invoice issue,
-- payment-success, and overdue-reminder flows.
INSERT INTO tmp_hdd1_notification_tenant_recipients (invoice_id, recipient_user_id)
SELECT DISTINCT
    invoice.invoice_id,
    user_account.user_id
FROM hdbhms.invoices invoice
JOIN hdbhms.rooms room
  ON room.room_id = invoice.room_id
JOIN (
    SELECT
        contract.room_id,
        contract.primary_tenant_profile_id AS tenant_profile_id
    FROM hdbhms.lease_contracts contract
    WHERE contract.deleted_at IS NULL
      AND contract.status IN ('SIGNED', 'ACTIVE', 'EXPIRING_SOON', 'TERMINATION_PENDING')
    UNION
    SELECT
        contract.room_id,
        occupant.tenant_profile_id
    FROM hdbhms.contract_occupants occupant
    JOIN hdbhms.lease_contracts contract
      ON contract.lease_contract_id = occupant.contract_id
    WHERE occupant.status = 'ACTIVE'
      AND occupant.tenant_profile_id IS NOT NULL
      AND contract.deleted_at IS NULL
      AND contract.status IN ('SIGNED', 'ACTIVE', 'EXPIRING_SOON', 'TERMINATION_PENDING')
) occupied
  ON occupied.room_id = room.room_id
JOIN hdbhms.person_profiles profile
  ON profile.person_profile_id = occupied.tenant_profile_id
 AND profile.deleted_at IS NULL
JOIN hdbhms.users user_account
  ON user_account.user_id = profile.user_id
 AND user_account.status = 'ACTIVE'
 AND user_account.deleted_at IS NULL
 AND user_account.role = 'TENANT'
WHERE invoice.property_id = @hdd1_notification_property_id
  AND invoice.billing_period = '2026-07'
  AND invoice.status <> 'VOIDED'
  AND invoice.total_amount > 0;

-- Fill missing INVOICE_ISSUED copies, including active co-occupants. Existing
-- rows for rooms released after July remain untouched.
INSERT INTO hdbhms.notification_outbox
    (event_type, target_type, target_id, recipient_user_id, channel, title, body, payload, status,
     retry_count, max_retries, scheduled_at, sent_at, created_at, is_read)
SELECT
    'INVOICE_ISSUED',
    'INVOICE',
    invoice.invoice_id,
    recipient.recipient_user_id,
    channel.channel,
    CONCAT('Có hóa đơn mới ', invoice.invoice_code),
    CONCAT(
        'Hóa đơn ', invoice.invoice_code,
        ' của phòng ', room.room_code,
        ' kỳ ', invoice.billing_period,
        ' đã phát hành. Số tiền cần thanh toán: ', invoice.remaining_amount,
        ' VND. Hạn thanh toán: ', DATE(invoice.due_date), '.'
    ),
    JSON_OBJECT(
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
    ),
    'SENT',
    0,
    3,
    @hdd1_notification_now,
    @hdd1_notification_now,
    @hdd1_notification_now,
    FALSE
FROM hdbhms.invoices invoice
JOIN hdbhms.rooms room
  ON room.room_id = invoice.room_id
JOIN hdbhms.properties property
  ON property.property_id = invoice.property_id
JOIN tmp_hdd1_notification_tenant_recipients recipient
  ON recipient.invoice_id = invoice.invoice_id
CROSS JOIN (
    SELECT 'WEB' AS channel
    UNION ALL
    SELECT 'PUSH'
) channel
WHERE NOT EXISTS (
    SELECT 1
    FROM hdbhms.notification_outbox existing_notification
    WHERE existing_notification.event_type = 'INVOICE_ISSUED'
      AND existing_notification.target_type = 'INVOICE'
      AND existing_notification.target_id = invoice.invoice_id
      AND existing_notification.recipient_user_id = recipient.recipient_user_id
      AND existing_notification.channel = channel.channel
);

-- Manual payment and reconciled payment both publish INVOICE_PAID to the web
-- and push channels after the invoice becomes fully paid.
INSERT INTO hdbhms.notification_outbox
    (event_type, target_type, target_id, recipient_user_id, channel, title, body, payload, status,
     retry_count, max_retries, scheduled_at, sent_at, created_at, is_read)
SELECT
    'INVOICE_PAID',
    'INVOICE',
    invoice.invoice_id,
    recipient.recipient_user_id,
    channel.channel,
    CONCAT('Đã ghi nhận thanh toán hóa đơn ', invoice.invoice_code),
    CONCAT(
        'Hóa đơn ', invoice.invoice_code,
        ' của phòng ', room.room_code,
        ' đã được thanh toán đủ. Số tiền ghi nhận: ', invoice.paid_amount, ' VND.'
    ),
    JSON_OBJECT(
        'invoiceId', invoice.invoice_id,
        'invoiceCode', invoice.invoice_code,
        'invoiceType', invoice.invoice_type,
        'roomCode', room.room_code,
        'propertyName', property.name,
        'billingPeriod', invoice.billing_period,
        'paymentAmount', invoice.paid_amount,
        'paidAmount', invoice.paid_amount,
        'totalAmount', invoice.total_amount,
        'targetRoute', CONCAT('/dashboard/invoices/', invoice.invoice_id)
    ),
    'SENT',
    0,
    3,
    @hdd1_notification_now,
    @hdd1_notification_now,
    @hdd1_notification_now,
    FALSE
FROM hdbhms.invoices invoice
JOIN hdbhms.rooms room
  ON room.room_id = invoice.room_id
JOIN hdbhms.properties property
  ON property.property_id = invoice.property_id
JOIN tmp_hdd1_notification_tenant_recipients recipient
  ON recipient.invoice_id = invoice.invoice_id
CROSS JOIN (
    SELECT 'WEB' AS channel
    UNION ALL
    SELECT 'PUSH'
) channel
WHERE invoice.status = 'PAID'
  AND invoice.total_amount > 0
  AND invoice.invoice_type <> 'DEPOSIT'
  AND NOT EXISTS (
      SELECT 1
      FROM hdbhms.notification_outbox existing_notification
      WHERE existing_notification.event_type = 'INVOICE_PAID'
        AND existing_notification.target_type = 'INVOICE'
        AND existing_notification.target_id = invoice.invoice_id
        AND existing_notification.recipient_user_id = recipient.recipient_user_id
        AND existing_notification.channel = channel.channel
  );

-- The payment listener uses one preferred contact channel per tenant: EMAIL
-- when valid, otherwise SMS. Settlement invoices use their specialized event.
INSERT INTO hdbhms.notification_outbox
    (event_type, target_type, target_id, recipient_user_id, channel, title, body, payload, status,
     retry_count, max_retries, scheduled_at, sent_at, created_at, is_read)
SELECT
    CASE invoice.invoice_type
        WHEN 'FINAL_SETTLEMENT' THEN 'FINAL_SETTLEMENT_PAYMENT_SUCCESS'
        WHEN 'TRANSFER_DIFFERENCE' THEN 'TRANSFER_DIFFERENCE_PAYMENT_SUCCESS'
        ELSE 'INVOICE_PAYMENT_SUCCESS'
    END,
    'INVOICE',
    invoice.invoice_id,
    recipient.recipient_user_id,
    CASE
        WHEN recipient_user.email IS NOT NULL
         AND TRIM(recipient_user.email) <> ''
         AND LOWER(recipient_user.email) NOT LIKE '%@tenant.hdbhms.local'
         AND LOWER(recipient_user.email) NOT LIKE '%tenant.hdbhms.local'
        THEN 'EMAIL'
        ELSE 'SMS'
    END,
    CASE invoice.invoice_type
        WHEN 'FINAL_SETTLEMENT' THEN CONCAT('Đã thanh toán tất toán hợp đồng ', invoice.invoice_code)
        WHEN 'TRANSFER_DIFFERENCE' THEN CONCAT('Thanh toán chênh lệch chuyển phòng thành công - ', invoice.invoice_code)
        ELSE CONCAT('Thanh toán hóa đơn thành công - ', invoice.invoice_code)
    END,
    CONCAT(
        CASE invoice.invoice_type
            WHEN 'FINAL_SETTLEMENT' THEN 'Khoản tất toán hợp đồng '
            ELSE 'Hóa đơn '
        END,
        invoice.invoice_code,
        ' của phòng ', room.room_code,
        ' tại ', property.name,
        ' đã được thanh toán đầy đủ. Số tiền ghi nhận: ', invoice.paid_amount, ' VNĐ. Trân trọng.'
    ),
    JSON_OBJECT(
        'invoiceId', invoice.invoice_id,
        'invoiceCode', invoice.invoice_code,
        'invoiceType', invoice.invoice_type,
        'billingPeriod', invoice.billing_period,
        'period', invoice.billing_period,
        'propertyId', property.property_id,
        'propertyName', property.name,
        'roomId', room.room_id,
        'roomCode', room.room_code,
        'amount', invoice.total_amount,
        'totalAmount', invoice.total_amount,
        'paymentAmount', invoice.paid_amount,
        'paidAmount', invoice.paid_amount,
        'remainingAmount', invoice.remaining_amount,
        'paidAt', @hdd1_notification_now,
        'status', invoice.status,
        'targetRoute', CONCAT('/dashboard/invoices/', invoice.invoice_id)
    ),
    'SENT',
    0,
    3,
    @hdd1_notification_now,
    @hdd1_notification_now,
    @hdd1_notification_now,
    FALSE
FROM hdbhms.invoices invoice
JOIN hdbhms.rooms room
  ON room.room_id = invoice.room_id
JOIN hdbhms.properties property
  ON property.property_id = invoice.property_id
JOIN tmp_hdd1_notification_tenant_recipients recipient
  ON recipient.invoice_id = invoice.invoice_id
JOIN hdbhms.users recipient_user
  ON recipient_user.user_id = recipient.recipient_user_id
WHERE invoice.status = 'PAID'
  AND invoice.total_amount > 0
  AND invoice.invoice_type <> 'DEPOSIT'
  AND NOT EXISTS (
      SELECT 1
      FROM hdbhms.notification_outbox existing_notification
      WHERE existing_notification.event_type = CASE invoice.invoice_type
          WHEN 'FINAL_SETTLEMENT' THEN 'FINAL_SETTLEMENT_PAYMENT_SUCCESS'
          WHEN 'TRANSFER_DIFFERENCE' THEN 'TRANSFER_DIFFERENCE_PAYMENT_SUCCESS'
          ELSE 'INVOICE_PAYMENT_SUCCESS'
      END
        AND existing_notification.target_type = 'INVOICE'
        AND existing_notification.target_id = invoice.invoice_id
        AND existing_notification.recipient_user_id = recipient.recipient_user_id
  );

DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_notification_overdue_stages;
CREATE TEMPORARY TABLE tmp_hdd1_notification_overdue_stages
(
    invoice_id BIGINT UNSIGNED NOT NULL PRIMARY KEY,
    reminder_stage TINYINT UNSIGNED NOT NULL,
    reminder_date DATE NOT NULL
);

-- The scheduler's stage is based on the day after the due date. Only the
-- currently due stage is seeded; later stages are created by later runs.
INSERT INTO tmp_hdd1_notification_overdue_stages (invoice_id, reminder_stage, reminder_date)
SELECT
    invoice.invoice_id,
    CASE
        WHEN DATE(@hdd1_notification_now) < DATE_ADD(DATE(invoice.due_date), INTERVAL 1 DAY) THEN 0
        WHEN DATE(@hdd1_notification_now) < DATE_ADD(DATE(invoice.due_date), INTERVAL 1 MONTH) THEN 1
        WHEN DATE(@hdd1_notification_now) < DATE_ADD(DATE(invoice.due_date), INTERVAL 2 MONTH) THEN 2
        ELSE 3
    END,
    DATE_ADD(DATE(invoice.due_date), INTERVAL 1 DAY)
FROM hdbhms.invoices invoice
WHERE invoice.property_id = @hdd1_notification_property_id
  AND invoice.billing_period = '2026-07'
  AND invoice.status IN ('ISSUED', 'OVERDUE')
  AND invoice.remaining_amount > 0
  AND invoice.due_date < @hdd1_notification_now
  AND invoice.invoice_type IN ('RENT', 'UTILITY');

DELETE FROM tmp_hdd1_notification_overdue_stages
WHERE reminder_stage = 0;

-- Tenant reminder stage 1/2/3, matching BillingManagementService.
INSERT INTO hdbhms.notification_outbox
    (event_type, target_type, target_id, recipient_user_id, channel, title, body, payload, status,
     retry_count, max_retries, scheduled_at, sent_at, created_at, is_read)
SELECT
    CONCAT('INVOICE_OVERDUE_REMINDER_', overdue.reminder_stage),
    'INVOICE',
    invoice.invoice_id,
    recipient.recipient_user_id,
    channel.channel,
    CONCAT('Nhắc thanh toán hóa đơn ', invoice.invoice_code, ' - đợt ', overdue.reminder_stage),
    CONCAT(
        'Hóa đơn ', invoice.invoice_code,
        ' của phòng ', room.room_code,
        ' tại ', property.name,
        ' vẫn còn nợ ', invoice.remaining_amount,
        ' VND. Đây là lần nhắc thanh toán đợt ', overdue.reminder_stage,
        ', bắt đầu từ ngày ', overdue.reminder_date, '. Vui lòng thanh toán sớm.'
    ),
    JSON_OBJECT(
        'invoiceId', invoice.invoice_id,
        'invoiceCode', invoice.invoice_code,
        'roomCode', room.room_code,
        'propertyName', property.name,
        'remainingAmount', invoice.remaining_amount,
        'dueDate', DATE(invoice.due_date),
        'reminderStage', overdue.reminder_stage,
        'reminderDate', overdue.reminder_date,
        'targetRoute', CONCAT('/dashboard/invoices/', invoice.invoice_id)
    ),
    'SENT',
    0,
    3,
    CONCAT(overdue.reminder_date, ' 09:00:00'),
    CONCAT(overdue.reminder_date, ' 09:00:00'),
    @hdd1_notification_now,
    FALSE
FROM hdbhms.invoices invoice
JOIN hdbhms.rooms room
  ON room.room_id = invoice.room_id
JOIN hdbhms.properties property
  ON property.property_id = invoice.property_id
JOIN tmp_hdd1_notification_overdue_stages overdue
  ON overdue.invoice_id = invoice.invoice_id
JOIN tmp_hdd1_notification_tenant_recipients recipient
  ON recipient.invoice_id = invoice.invoice_id
CROSS JOIN (
    SELECT 'WEB' AS channel
    UNION ALL
    SELECT 'PUSH'
) channel
WHERE NOT EXISTS (
    SELECT 1
    FROM hdbhms.notification_outbox existing_notification
    WHERE existing_notification.event_type = CONCAT('INVOICE_OVERDUE_REMINDER_', overdue.reminder_stage)
      AND existing_notification.target_type = 'INVOICE'
      AND existing_notification.target_id = invoice.invoice_id
      AND existing_notification.recipient_user_id = recipient.recipient_user_id
      AND existing_notification.channel = channel.channel
);

DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_notification_owner_recipients;
CREATE TEMPORARY TABLE tmp_hdd1_notification_owner_recipients
(
    recipient_user_id BIGINT UNSIGNED NOT NULL PRIMARY KEY
);

-- Owner escalation includes every active owner and only active managers
-- assigned to Hai Dang 1; inactive user accounts are intentionally excluded.
INSERT INTO tmp_hdd1_notification_owner_recipients (recipient_user_id)
SELECT DISTINCT user_account.user_id
FROM hdbhms.users user_account
LEFT JOIN hdbhms.property_staff_assignments assignment
  ON assignment.staff_user_id = user_account.user_id
 AND assignment.property_id = @hdd1_notification_property_id
 AND assignment.assigned_role = 'MANAGER'
 AND assignment.assignment_status = 'ACTIVE'
 AND assignment.ended_at IS NULL
WHERE user_account.status = 'ACTIVE'
  AND user_account.deleted_at IS NULL
  AND (user_account.role = 'OWNER' OR assignment.staff_user_id IS NOT NULL);

-- Owner/manager escalation is only implemented for RENT and UTILITY.
INSERT INTO hdbhms.notification_outbox
    (event_type, target_type, target_id, recipient_user_id, channel, title, body, payload, status,
     retry_count, max_retries, scheduled_at, sent_at, created_at, is_read)
SELECT
    CONCAT('INVOICE_OVERDUE_OWNER_STAGE_', overdue.reminder_stage),
    'INVOICE',
    invoice.invoice_id,
    owner_recipient.recipient_user_id,
    channel.channel,
    CONCAT('Hóa đơn ', invoice.invoice_code, ' đã quá hạn - đợt ', overdue.reminder_stage),
    CONCAT(
        'Chủ trọ/quản lý: hóa đơn ', invoice.invoice_code,
        ' của phòng ', room.room_code,
        ' tại ', property.name,
        ' đã quá hạn sau ngày ', DATE(invoice.due_date),
        '. Số tiền còn nợ: ', invoice.remaining_amount,
        ' VND. Vui lòng kiểm tra và xử lý công nợ.'
    ),
    JSON_OBJECT(
        'invoiceId', invoice.invoice_id,
        'invoiceCode', invoice.invoice_code,
        'invoiceType', invoice.invoice_type,
        'roomCode', room.room_code,
        'propertyName', property.name,
        'remainingAmount', invoice.remaining_amount,
        'dueDate', DATE(invoice.due_date),
        'overdueStage', overdue.reminder_stage,
        'overdueStageDueDate', overdue.reminder_date,
        'recipientScope', 'OWNER_MANAGER',
        'targetRoute', '/dashboard/billing'
    ),
    'SENT',
    0,
    3,
    CONCAT(overdue.reminder_date, ' 09:00:00'),
    CONCAT(overdue.reminder_date, ' 09:00:00'),
    @hdd1_notification_now,
    FALSE
FROM hdbhms.invoices invoice
JOIN hdbhms.rooms room
  ON room.room_id = invoice.room_id
JOIN hdbhms.properties property
  ON property.property_id = invoice.property_id
JOIN tmp_hdd1_notification_overdue_stages overdue
  ON overdue.invoice_id = invoice.invoice_id
CROSS JOIN tmp_hdd1_notification_owner_recipients owner_recipient
CROSS JOIN (
    SELECT 'WEB' AS channel
    UNION ALL
    SELECT 'PUSH'
) channel
WHERE NOT EXISTS (
    SELECT 1
    FROM hdbhms.notification_outbox existing_notification
    WHERE existing_notification.event_type = CONCAT('INVOICE_OVERDUE_OWNER_STAGE_', overdue.reminder_stage)
      AND existing_notification.target_type = 'INVOICE'
      AND existing_notification.target_id = invoice.invoice_id
      AND existing_notification.recipient_user_id = owner_recipient.recipient_user_id
      AND existing_notification.channel = channel.channel
);

DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_notification_owner_recipients;
DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_notification_overdue_stages;
DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_notification_tenant_recipients;

-- Final contract-state alignment: room 301 remains an active Khai contract;
-- room 302 is the single Khai soon-vacant case used by the expiry workflow.
SET @hdd1_contract_fix_property_id := (
    SELECT property_id
    FROM hdbhms.properties
    WHERE property_code = 'HAI_DANG_1'
      AND deleted_at IS NULL
    LIMIT 1
);
SET @hdd1_contract_fix_now := '2026-08-20 09:00:00';
SET @hdd1_contract_fix_owner_id := (
    SELECT user_id
    FROM hdbhms.users
    WHERE deleted_at IS NULL
      AND status = 'ACTIVE'
      AND role = 'OWNER'
    ORDER BY user_id
    LIMIT 1
);
SET @hdd1_contract_fix_manager_id := (
    SELECT staff_user_id
    FROM hdbhms.property_staff_assignments
    WHERE property_id = @hdd1_contract_fix_property_id
      AND assigned_role = 'MANAGER'
      AND assignment_status = 'ACTIVE'
      AND ended_at IS NULL
    ORDER BY is_primary DESC, property_staff_assignment_id
    LIMIT 1
);
SET @hdd1_contract_fix_khai_profile_id := (
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
    contract.updated_at = @hdd1_contract_fix_now
WHERE room.property_id = @hdd1_contract_fix_property_id
  AND room.room_code IN ('301', '302')
  AND contract.primary_tenant_profile_id = @hdd1_contract_fix_khai_profile_id
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
    room.updated_at = @hdd1_contract_fix_now
WHERE room.property_id = @hdd1_contract_fix_property_id
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
  AND room.property_id = @hdd1_contract_fix_property_id
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
  AND room.property_id = @hdd1_contract_fix_property_id
  AND room.room_code IN ('301', '302');

DELETE tracker
FROM hdbhms.reminder_trackers tracker
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = tracker.target_id
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
WHERE tracker.reminder_key = 'LEASE_EXPIRY_INTENTION'
  AND tracker.target_type = 'CONTRACT'
  AND room.property_id = @hdd1_contract_fix_property_id
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
    @hdd1_contract_fix_now,
    @hdd1_contract_fix_now,
    @hdd1_contract_fix_now,
    FALSE
FROM hdbhms.lease_contracts contract
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
JOIN hdbhms.properties property
  ON property.property_id = room.property_id
CROSS JOIN (
    SELECT @hdd1_contract_fix_owner_id AS recipient_user_id
    UNION
    SELECT @hdd1_contract_fix_manager_id
) recipient
CROSS JOIN (
    SELECT 'WEB' AS channel
    UNION ALL
    SELECT 'PUSH'
) channel
WHERE room.property_id = @hdd1_contract_fix_property_id
  AND room.room_code = '302'
  AND contract.status = 'EXPIRING_SOON'
  AND contract.tenant_intention IS NULL
  AND recipient.recipient_user_id IS NOT NULL;

DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_contract_fix_recipients;
CREATE TEMPORARY TABLE tmp_hdd1_contract_fix_recipients
(
    recipient_user_id BIGINT UNSIGNED NOT NULL,
    audience VARCHAR(50) NOT NULL,
    PRIMARY KEY (recipient_user_id, audience)
);

INSERT INTO tmp_hdd1_contract_fix_recipients (recipient_user_id, audience)
SELECT user_account.user_id, 'PRIMARY_TENANT'
FROM hdbhms.lease_contracts contract
JOIN hdbhms.person_profiles profile
  ON profile.person_profile_id = contract.primary_tenant_profile_id
JOIN hdbhms.users user_account
  ON user_account.user_id = profile.user_id
WHERE contract.room_id = (
          SELECT room_id FROM hdbhms.rooms
          WHERE property_id = @hdd1_contract_fix_property_id AND room_code = '302'
          LIMIT 1
      )
  AND contract.status = 'EXPIRING_SOON'
  AND user_account.status = 'ACTIVE'
  AND user_account.deleted_at IS NULL
UNION
SELECT user_account.user_id, 'CO_OCCUPANT'
FROM hdbhms.lease_contracts contract
JOIN hdbhms.contract_occupants occupant
  ON occupant.contract_id = contract.lease_contract_id
 AND occupant.occupant_role = 'CO_OCCUPANT'
 AND occupant.status = 'ACTIVE'
JOIN hdbhms.person_profiles profile
  ON profile.person_profile_id = occupant.tenant_profile_id
JOIN hdbhms.users user_account
  ON user_account.user_id = profile.user_id
WHERE contract.room_id = (
          SELECT room_id FROM hdbhms.rooms
          WHERE property_id = @hdd1_contract_fix_property_id AND room_code = '302'
          LIMIT 1
      )
  AND contract.status = 'EXPIRING_SOON'
  AND user_account.status = 'ACTIVE'
  AND user_account.deleted_at IS NULL;

INSERT INTO hdbhms.notification_outbox
    (event_type, target_type, target_id, recipient_user_id, channel, title, body, payload, status,
     retry_count, max_retries, scheduled_at, sent_at, created_at, is_read)
SELECT
    'LEASE_EXPIRY_REMINDER_FIRST',
    'CONTRACT',
    contract.lease_contract_id,
    recipient.recipient_user_id,
    'PUSH',
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
        'daysRemaining', DATEDIFF(contract.end_date, DATE(@hdd1_contract_fix_now)),
        'stage', 'FIRST',
        'targetRoute', '/contract'
    ),
    'SENT', 0, 3,
    DATE_SUB(contract.end_date, INTERVAL 3 MONTH),
    DATE_SUB(contract.end_date, INTERVAL 3 MONTH),
    @hdd1_contract_fix_now,
    FALSE
FROM hdbhms.lease_contracts contract
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
JOIN hdbhms.properties property
  ON property.property_id = room.property_id
JOIN tmp_hdd1_contract_fix_recipients recipient
  ON 1 = 1
WHERE room.property_id = @hdd1_contract_fix_property_id
  AND room.room_code = '302'
  AND contract.status = 'EXPIRING_SOON'
  AND contract.tenant_intention IS NULL
  AND DATE_SUB(contract.end_date, INTERVAL 3 MONTH) <= DATE(@hdd1_contract_fix_now);

INSERT INTO hdbhms.reminder_trackers
    (reminder_key, target_type, target_id, audience, recipient_user_id, status, sent_count,
     last_sent_at, next_due_at, metadata, created_at, updated_at)
SELECT
    'LEASE_EXPIRY_INTENTION',
    'CONTRACT',
    contract.lease_contract_id,
    recipient.audience,
    recipient.recipient_user_id,
    'ACTIVE',
    1,
    DATE_SUB(contract.end_date, INTERVAL 3 MONTH),
    DATE_SUB(contract.end_date, INTERVAL 2 MONTH),
    JSON_OBJECT(
        'endDate', contract.end_date,
        'firstReminderDate', DATE_SUB(contract.end_date, INTERVAL 3 MONTH),
        'lastReminderStage', 'FIRST'
    ),
    @hdd1_contract_fix_now,
    @hdd1_contract_fix_now
FROM hdbhms.lease_contracts contract
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
JOIN tmp_hdd1_contract_fix_recipients recipient
  ON 1 = 1
WHERE room.property_id = @hdd1_contract_fix_property_id
  AND room.room_code = '302'
  AND contract.status = 'EXPIRING_SOON'
  AND contract.tenant_intention IS NULL
  AND DATE_SUB(contract.end_date, INTERVAL 3 MONTH) <= DATE(@hdd1_contract_fix_now);

DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_contract_fix_recipients;
