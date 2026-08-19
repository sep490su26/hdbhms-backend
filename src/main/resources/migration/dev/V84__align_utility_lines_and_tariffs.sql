SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE hdbhms;

-- Keep the demo property configured explicitly so billing does not silently
-- fall back to application defaults for electricity and water.
SET @hdd1_property_id := (
    SELECT property_id
    FROM hdbhms.properties
    WHERE property_code = 'HAI_DANG_1'
      AND deleted_at IS NULL
    LIMIT 1
);

INSERT INTO hdbhms.utility_tariffs
    (property_id, utility_type, unit_price, free_allowance,
     service_fee_waive_electricity_threshold, effective_from, effective_to,
     created_by, created_at)
SELECT @hdd1_property_id, 'ELECTRICITY', 3500, 0, NULL,
       '2026-01-01', NULL, NULL, CURRENT_TIMESTAMP(6)
WHERE @hdd1_property_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM hdbhms.utility_tariffs tariff
      WHERE tariff.property_id = @hdd1_property_id
        AND tariff.utility_type = 'ELECTRICITY'
        AND tariff.effective_from <= '2026-07-31'
        AND (tariff.effective_to IS NULL OR tariff.effective_to >= '2026-07-31')
  );

INSERT INTO hdbhms.utility_tariffs
    (property_id, utility_type, unit_price, free_allowance,
     service_fee_waive_electricity_threshold, effective_from, effective_to,
     created_by, created_at)
SELECT @hdd1_property_id, 'WATER', 20000, 6, NULL,
       '2026-01-01', NULL, NULL, CURRENT_TIMESTAMP(6)
WHERE @hdd1_property_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM hdbhms.utility_tariffs tariff
      WHERE tariff.property_id = @hdd1_property_id
        AND tariff.utility_type = 'WATER'
        AND tariff.effective_from <= '2026-07-31'
        AND (tariff.effective_to IS NULL OR tariff.effective_to >= '2026-07-31')
  );

DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_electricity_invoices;
CREATE TEMPORARY TABLE tmp_hdd1_electricity_invoices
(
    invoice_id BIGINT UNSIGNED NOT NULL PRIMARY KEY,
    original_status VARCHAR(30) NOT NULL
);

INSERT INTO tmp_hdd1_electricity_invoices (invoice_id, original_status)
SELECT invoice.invoice_id, invoice.status
FROM hdbhms.invoices invoice
JOIN hdbhms.invoice_lines line
  ON line.invoice_id = invoice.invoice_id
JOIN hdbhms.meter_readings reading
  ON reading.meter_reading_id = line.meter_reading_id
WHERE invoice.property_id = @hdd1_property_id
  AND invoice.status <> 'VOIDED'
  AND line.line_type = 'ELECTRICITY'
  AND reading.previous_value IS NOT NULL
  AND reading.current_value IS NOT NULL
GROUP BY invoice.invoice_id, invoice.status;

-- The application protects issued invoice lines from direct edits. Temporarily
-- move affected seed invoices to DRAFT while rebuilding their generated data.
UPDATE hdbhms.invoices invoice
JOIN tmp_hdd1_electricity_invoices affected
  ON affected.invoice_id = invoice.invoice_id
SET invoice.status = 'DRAFT';

-- Rebuild electricity quantities from the linked meter reading. Existing
-- invoice unit prices are preserved because they are the historical tariff.
UPDATE hdbhms.invoice_lines line
JOIN hdbhms.invoices invoice
  ON invoice.invoice_id = line.invoice_id
JOIN hdbhms.meter_readings reading
  ON reading.meter_reading_id = line.meter_reading_id
LEFT JOIN hdbhms.utility_tariffs tariff
  ON tariff.property_id = invoice.property_id
 AND tariff.utility_type = 'ELECTRICITY'
 AND tariff.effective_from <= reading.reading_date
 AND (tariff.effective_to IS NULL OR tariff.effective_to >= reading.reading_date)
SET line.quantity = CAST(CEILING(GREATEST(
        reading.current_value - reading.previous_value - COALESCE(tariff.free_allowance, 0),
        0
    )) AS UNSIGNED),
    line.unit_price = CASE
        WHEN line.unit_price > 0 THEN line.unit_price
        ELSE COALESCE(tariff.unit_price, 3500)
    END
WHERE invoice.property_id = @hdd1_property_id
  AND invoice.status <> 'VOIDED'
  AND line.line_type = 'ELECTRICITY'
  AND reading.previous_value IS NOT NULL
  AND reading.current_value IS NOT NULL;

-- Recalculate invoice totals after the generated invoice-line amount changes.
UPDATE hdbhms.invoices invoice
JOIN (
    SELECT invoice_id, COALESCE(SUM(amount), 0) AS subtotal_amount
    FROM hdbhms.invoice_lines
    GROUP BY invoice_id
) totals
  ON totals.invoice_id = invoice.invoice_id
JOIN tmp_hdd1_electricity_invoices affected
  ON affected.invoice_id = invoice.invoice_id
SET invoice.subtotal_amount = totals.subtotal_amount,
    invoice.total_amount = GREATEST(totals.subtotal_amount - invoice.discount_amount, 0),
    invoice.remaining_amount = GREATEST(
        GREATEST(totals.subtotal_amount - invoice.discount_amount, 0) - invoice.paid_amount,
        0
    ),
    invoice.status = CASE
        WHEN invoice.paid_amount >= GREATEST(totals.subtotal_amount - invoice.discount_amount, 0)
            THEN 'PAID'
        WHEN invoice.due_date < CURRENT_TIMESTAMP
            THEN 'OVERDUE'
        ELSE 'ISSUED'
    END,
    invoice.updated_at = CURRENT_TIMESTAMP(6)
WHERE invoice.status <> 'VOIDED';

-- Prevent legacy seed rows from exposing a PAID status while carrying debt.
UPDATE hdbhms.invoices invoice
SET invoice.status = CASE
        WHEN invoice.due_date < CURRENT_TIMESTAMP THEN 'OVERDUE'
        ELSE 'ISSUED'
    END,
    invoice.updated_at = CURRENT_TIMESTAMP(6)
WHERE invoice.status = 'PAID'
  AND invoice.remaining_amount > 0;

UPDATE hdbhms.invoices invoice
SET invoice.paid_amount = invoice.total_amount,
    invoice.remaining_amount = 0,
    invoice.updated_at = CURRENT_TIMESTAMP(6)
WHERE invoice.status = 'PAID'
  AND (invoice.paid_amount <> invoice.total_amount OR invoice.remaining_amount <> 0);

DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_electricity_invoices;
SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;
