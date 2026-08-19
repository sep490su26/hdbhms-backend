-- Invoice payment is all-or-nothing. Preserve legacy unpaid balances while
-- mapping old partial rows to the normal issued/overdue lifecycle.
UPDATE hdbhms.payment_intents pi
JOIN hdbhms.invoices i ON i.invoice_id = pi.invoice_id
SET pi.status = 'EXPIRED'
WHERE i.status = 'PARTIALLY_PAID'
  AND pi.status IN ('CREATED', 'PENDING');

UPDATE hdbhms.invoices
SET status = CASE
                 WHEN remaining_amount <= 0 THEN 'PAID'
                 WHEN due_date <= CURRENT_TIMESTAMP THEN 'OVERDUE'
                 ELSE 'ISSUED'
             END
WHERE status = 'PARTIALLY_PAID';

ALTER TABLE hdbhms.invoices
    MODIFY COLUMN status ENUM ('DRAFT', 'ISSUED', 'PAID', 'OVERDUE', 'VOIDED')
        DEFAULT 'DRAFT' NOT NULL;
