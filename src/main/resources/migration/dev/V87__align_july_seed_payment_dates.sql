SET @hdd1_property_id := (
    SELECT property_id
    FROM hdbhms.properties
    WHERE property_code = 'HAI_DANG_1'
      AND deleted_at IS NULL
    LIMIT 1
);

-- V74 settles room 402 through a synthetic payment. Keep that July invoice in
-- the July revenue period even when V74 ran on a later migration date.
UPDATE hdbhms.payment_transactions payment
JOIN hdbhms.payment_allocations allocation
  ON allocation.payment_transaction_id = payment.payment_transaction_id
JOIN hdbhms.invoices invoice
  ON invoice.invoice_id = allocation.invoice_id
JOIN hdbhms.rooms room
  ON room.room_id = invoice.room_id
SET payment.transaction_time = '2026-07-31 18:00:00'
WHERE payment.provider = 'MANUAL'
  AND payment.provider_transaction_id LIKE 'SEED-CLEAR-DEBT-%'
  AND room.property_id = @hdd1_property_id
  AND invoice.billing_period = '2026-07';
