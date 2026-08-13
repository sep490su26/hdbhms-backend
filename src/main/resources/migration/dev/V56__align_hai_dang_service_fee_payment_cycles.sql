SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- The workbook charges service fee for the whole rent-payment cycle. Keep the
-- contract cycle and the seeded service lines aligned with that rule.
SET @property_id := (
    SELECT property_id
    FROM hdbhms.properties
    WHERE property_code = 'HAI_DANG_1'
      AND deleted_at IS NULL
    LIMIT 1
);
SET @now := '2026-08-01 08:00:00';

UPDATE hdbhms.lease_contracts contract
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
SET contract.payment_cycle_months = CASE room.room_code
    WHEN '101' THEN 3
    WHEN '102' THEN 1
    WHEN '103' THEN 1
    WHEN '104' THEN 1
    WHEN '105' THEN 3
    WHEN '106' THEN 1
    WHEN '201' THEN 1
    WHEN '202' THEN 3
    WHEN '203' THEN 3
    WHEN '204' THEN 3
    WHEN '205' THEN 1
    WHEN '206' THEN 3
    WHEN '207' THEN 1
    WHEN '208' THEN 3
    WHEN '301' THEN 1
    WHEN '302' THEN 1
    WHEN '303' THEN 3
    WHEN '305' THEN 1
    WHEN '306' THEN 1
    WHEN '307' THEN 1
    WHEN '308' THEN 1
    WHEN '401' THEN 3
    WHEN '402' THEN 1
    WHEN '405' THEN 1
    WHEN '406' THEN 1
    WHEN '408' THEN 1
    WHEN '501' THEN 1
    WHEN '502' THEN 1
    WHEN '503' THEN 3
    WHEN '504' THEN 3
    WHEN '505' THEN 1
    WHEN '506' THEN 1
    WHEN '507' THEN 1
    ELSE contract.payment_cycle_months
END,
    contract.updated_at = @now
WHERE room.property_id = @property_id
  AND contract.deleted_at IS NULL
  AND contract.status IN ('SIGNED', 'ACTIVE', 'EXPIRING_SOON', 'TERMINATION_PENDING');

INSERT INTO hdbhms.invoice_lines
    (invoice_id, line_type, description, quantity, unit_price,
     meter_reading_id, source_type, source_id, created_at)
SELECT
    invoice.invoice_id,
    'SERVICE_FEE',
    CONCAT('Phí dịch vụ bổ sung tháng 07/2026 (',
           occupant_count.active_count * GREATEST(contract.payment_cycle_months, 1),
           ' người-tháng)'),
    CEILING((
        occupant_count.active_count * GREATEST(contract.payment_cycle_months, 1) * 50000
        - existing_service.service_amount
    ) / 50000),
    50000,
    NULL,
    'EXCEL_IMPORT',
    invoice.invoice_id,
    @now
FROM hdbhms.invoices invoice
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = invoice.lease_contract_id
JOIN (
    SELECT contract_id, COUNT(*) AS active_count
    FROM hdbhms.contract_occupants
    WHERE status = 'ACTIVE'
    GROUP BY contract_id
) occupant_count
  ON occupant_count.contract_id = contract.lease_contract_id
JOIN (
    SELECT invoice_id, COALESCE(SUM(amount), 0) AS service_amount
    FROM hdbhms.invoice_lines
    WHERE line_type = 'SERVICE_FEE'
    GROUP BY invoice_id
) existing_service
  ON existing_service.invoice_id = invoice.invoice_id
WHERE invoice.property_id = @property_id
  AND invoice.billing_period = '2026-07'
  AND invoice.invoice_type IN ('UTILITY', 'FINAL_SETTLEMENT')
  AND invoice.invoice_code LIKE 'HD_P%'
  AND occupant_count.active_count * GREATEST(contract.payment_cycle_months, 1) * 50000
      > existing_service.service_amount;

INSERT INTO hdbhms.invoice_lines
    (invoice_id, line_type, description, quantity, unit_price,
     meter_reading_id, source_type, source_id, created_at)
SELECT
    invoice.invoice_id,
    'SERVICE_FEE',
    CONCAT('Phí dịch vụ tháng 07/2026 (',
           occupant_count.active_count * GREATEST(contract.payment_cycle_months, 1),
           ' người-tháng)'),
    occupant_count.active_count * GREATEST(contract.payment_cycle_months, 1),
    50000,
    NULL,
    'EXCEL_IMPORT',
    invoice.invoice_id,
    @now
FROM hdbhms.invoices invoice
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = invoice.lease_contract_id
JOIN (
    SELECT contract_id, COUNT(*) AS active_count
    FROM hdbhms.contract_occupants
    WHERE status = 'ACTIVE'
    GROUP BY contract_id
) occupant_count
  ON occupant_count.contract_id = contract.lease_contract_id
LEFT JOIN (
    SELECT invoice_id, COALESCE(SUM(amount), 0) AS service_amount
    FROM hdbhms.invoice_lines
    WHERE line_type = 'SERVICE_FEE'
    GROUP BY invoice_id
) existing_service
  ON existing_service.invoice_id = invoice.invoice_id
WHERE invoice.property_id = @property_id
  AND invoice.billing_period = '2026-07'
  AND invoice.invoice_type IN ('UTILITY', 'FINAL_SETTLEMENT')
  AND invoice.invoice_code LIKE 'HD_P%'
  AND occupant_count.active_count > 0
  AND existing_service.invoice_id IS NULL;

UPDATE hdbhms.invoices invoice
JOIN (
    SELECT invoice_id, COALESCE(SUM(amount), 0) AS recalculated_subtotal
    FROM hdbhms.invoice_lines
    GROUP BY invoice_id
) line_totals
  ON line_totals.invoice_id = invoice.invoice_id
SET invoice.subtotal_amount = line_totals.recalculated_subtotal,
    invoice.total_amount = GREATEST(line_totals.recalculated_subtotal - invoice.discount_amount, 0),
    invoice.remaining_amount = GREATEST(
        GREATEST(line_totals.recalculated_subtotal - invoice.discount_amount, 0) - invoice.paid_amount,
        0
    ),
    invoice.updated_at = @now
WHERE invoice.property_id = @property_id
  AND invoice.billing_period = '2026-07'
  AND invoice.invoice_type IN ('UTILITY', 'FINAL_SETTLEMENT')
  AND invoice.invoice_code LIKE 'HD_P%';

UPDATE hdbhms.notification_outbox notification
JOIN hdbhms.invoices invoice
  ON invoice.invoice_id = notification.target_id
JOIN hdbhms.rooms room
  ON room.room_id = invoice.room_id
SET notification.body = CONCAT(
        'Hóa đơn ', invoice.invoice_code,
        ' của phòng ', room.room_code,
        ' kỳ ', invoice.billing_period,
        ' đã phát hành. Số tiền cần thanh toán: ', invoice.remaining_amount,
        ' VND. Hạn thanh toán: ', DATE(invoice.due_date), '.'
    ),
    notification.payload = JSON_SET(
        notification.payload,
        '$.amount', invoice.total_amount,
        '$.totalAmount', invoice.total_amount,
        '$.remainingAmount', invoice.remaining_amount
    )
WHERE notification.event_type = 'INVOICE_ISSUED'
  AND notification.target_type = 'INVOICE'
  AND invoice.property_id = @property_id
  AND invoice.billing_period = '2026-07'
  AND invoice.invoice_type IN ('UTILITY', 'FINAL_SETTLEMENT')
  AND invoice.invoice_code LIKE 'HD_P%';
