ALTER TABLE hdbhms.rent_overrides
    ADD COLUMN discount_amount BIGINT UNSIGNED NOT NULL DEFAULT 0 AFTER override_monthly_rent;

-- Convert legacy monthly-price overrides into invoice discounts before new writes use the field.
UPDATE hdbhms.rent_overrides override_row
JOIN hdbhms.lease_contracts contract_row
  ON contract_row.lease_contract_id = override_row.contract_id
SET override_row.discount_amount = GREATEST(
        contract_row.monthly_rent - override_row.override_monthly_rent,
        0
    )
WHERE override_row.discount_amount = 0
  AND override_row.override_monthly_rent < contract_row.monthly_rent;

UPDATE hdbhms.invoice_lines line
JOIN hdbhms.invoices invoice
  ON invoice.invoice_id = line.invoice_id
JOIN hdbhms.rent_overrides override_row
  ON override_row.contract_id = invoice.lease_contract_id
 AND override_row.billing_period = invoice.billing_period
JOIN hdbhms.lease_contracts contract_row
  ON contract_row.lease_contract_id = override_row.contract_id
SET line.unit_price = contract_row.monthly_rent
WHERE invoice.invoice_type = 'RENT'
  AND invoice.status <> 'VOIDED'
  AND line.line_type = 'ROOM_RENT';

UPDATE hdbhms.invoices invoice
JOIN (
    SELECT invoice_id, SUM(quantity * unit_price) AS recalculated_subtotal
    FROM hdbhms.invoice_lines
    GROUP BY invoice_id
) line_totals
  ON line_totals.invoice_id = invoice.invoice_id
JOIN hdbhms.rent_overrides override_row
  ON override_row.contract_id = invoice.lease_contract_id
 AND override_row.billing_period = invoice.billing_period
SET invoice.discount_amount = LEAST(override_row.discount_amount, line_totals.recalculated_subtotal),
    invoice.subtotal_amount = line_totals.recalculated_subtotal,
    invoice.total_amount = GREATEST(
        line_totals.recalculated_subtotal - LEAST(override_row.discount_amount, line_totals.recalculated_subtotal),
        0
    ),
    invoice.remaining_amount = GREATEST(
        line_totals.recalculated_subtotal - LEAST(override_row.discount_amount, line_totals.recalculated_subtotal)
            - invoice.paid_amount,
        0
    )
WHERE invoice.invoice_type = 'RENT'
  AND invoice.status <> 'VOIDED';

-- Remove discounts that were previously written to utility invoices.
UPDATE hdbhms.invoices invoice
SET invoice.discount_amount = 0,
    invoice.total_amount = invoice.subtotal_amount,
    invoice.remaining_amount = GREATEST(invoice.subtotal_amount - invoice.paid_amount, 0),
    invoice.status = CASE
        WHEN invoice.status = 'PAID' AND invoice.paid_amount < invoice.subtotal_amount
            THEN 'PARTIALLY_PAID'
        ELSE invoice.status
    END
WHERE invoice.invoice_type = 'UTILITY'
  AND invoice.discount_amount > 0
  AND invoice.status <> 'VOIDED';
