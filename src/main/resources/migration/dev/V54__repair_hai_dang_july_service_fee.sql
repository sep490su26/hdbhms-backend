SET NAMES utf8mb4;

-- Repair databases that already applied V48 before the per-occupant service
-- fee was added to the Hai Dang July utility invoices.
SET @property_id := (
    SELECT property_id
    FROM hdbhms.properties
    WHERE property_code = 'HAI_DANG_1'
      AND deleted_at IS NULL
    LIMIT 1
);
SET @now := '2026-08-01 08:00:00';

INSERT INTO hdbhms.utility_tariffs
    (property_id, utility_type, unit_price, free_allowance,
     service_fee_waive_electricity_threshold, effective_from, effective_to,
     created_by, created_at)
SELECT @property_id, 'SERVICE_FEE', 50000, 0, NULL, '2026-01-01', NULL, NULL, @now
WHERE @property_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM hdbhms.utility_tariffs tariff
      WHERE tariff.property_id = @property_id
        AND tariff.utility_type = 'SERVICE_FEE'
        AND tariff.effective_from <= '2026-07-31'
        AND (tariff.effective_to IS NULL OR tariff.effective_to >= '2026-07-31')
  );

DELETE line
FROM hdbhms.invoice_lines line
JOIN hdbhms.invoices invoice
  ON invoice.invoice_id = line.invoice_id
LEFT JOIN (
    SELECT contract_id, COUNT(*) AS active_count
    FROM hdbhms.contract_occupants
    WHERE status = 'ACTIVE'
    GROUP BY contract_id
) occupant_count
  ON occupant_count.contract_id = invoice.lease_contract_id
WHERE invoice.invoice_type = 'UTILITY'
  AND invoice.billing_period = '2026-07'
  AND invoice.invoice_code LIKE 'SEED-INV-%-2026-07-UTILITY-EXCEL'
  AND line.line_type = 'SERVICE_FEE'
  AND COALESCE(occupant_count.active_count, 0) = 0;

UPDATE hdbhms.invoice_lines line
JOIN hdbhms.invoices invoice
  ON invoice.invoice_id = line.invoice_id
JOIN (
    SELECT contract_id, COUNT(*) AS active_count
    FROM hdbhms.contract_occupants
    WHERE status = 'ACTIVE'
    GROUP BY contract_id
) occupant_count
  ON occupant_count.contract_id = invoice.lease_contract_id
SET line.quantity = occupant_count.active_count,
    line.unit_price = 50000,
    line.description = CONCAT('Phi dich vu thang 07/2026 (', occupant_count.active_count, ' nguoi)')
WHERE invoice.invoice_type = 'UTILITY'
  AND invoice.billing_period = '2026-07'
  AND invoice.invoice_code LIKE 'SEED-INV-%-2026-07-UTILITY-EXCEL'
  AND line.line_type = 'SERVICE_FEE';

INSERT INTO hdbhms.invoice_lines
    (invoice_id, line_type, description, quantity, unit_price,
     meter_reading_id, source_type, source_id, created_at)
SELECT
    invoice.invoice_id,
    'SERVICE_FEE',
    CONCAT('Phi dich vu thang 07/2026 (', occupant_count.active_count, ' nguoi)'),
    occupant_count.active_count,
    50000,
    NULL,
    'EXCEL_IMPORT',
    invoice.invoice_id,
    @now
FROM hdbhms.invoices invoice
JOIN (
    SELECT contract_id, COUNT(*) AS active_count
    FROM hdbhms.contract_occupants
    WHERE status = 'ACTIVE'
    GROUP BY contract_id
) occupant_count
  ON occupant_count.contract_id = invoice.lease_contract_id
WHERE invoice.invoice_type = 'UTILITY'
  AND invoice.billing_period = '2026-07'
  AND invoice.invoice_code LIKE 'SEED-INV-%-2026-07-UTILITY-EXCEL'
  AND occupant_count.active_count > 0
  AND NOT EXISTS (
      SELECT 1
      FROM hdbhms.invoice_lines existing_line
      WHERE existing_line.invoice_id = invoice.invoice_id
        AND existing_line.line_type = 'SERVICE_FEE'
  );

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
WHERE invoice.invoice_type = 'UTILITY'
  AND invoice.billing_period = '2026-07'
  AND invoice.invoice_code LIKE 'SEED-INV-%-2026-07-UTILITY-EXCEL';

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
  AND invoice.invoice_type = 'UTILITY'
  AND invoice.billing_period = '2026-07'
  AND invoice.invoice_code LIKE 'SEED-INV-%-2026-07-UTILITY-EXCEL';
