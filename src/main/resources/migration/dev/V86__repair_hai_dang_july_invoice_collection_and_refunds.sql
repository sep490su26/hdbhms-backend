SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- Make the final July demo state explicit. Revenue is calculated from valid
-- payment allocations, so invoice status alone is not sufficient.
SET @hdd1_property_id := (
    SELECT property_id
    FROM hdbhms.properties
    WHERE property_code = 'HAI_DANG_1'
      AND deleted_at IS NULL
    LIMIT 1
);
SET @hdd1_seed_now := '2026-08-20 09:00:00';
SET @hdd1_manager_id := (
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

-- Keep Khai's July invoices and two useful debt/liquidation demo tenants
-- unpaid. Room 403 and room 503 are intentionally retained as debt cases.
DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_july_unpaid;
CREATE TEMPORARY TABLE tmp_hdd1_july_unpaid (
    invoice_id BIGINT UNSIGNED NOT NULL PRIMARY KEY
);

INSERT INTO tmp_hdd1_july_unpaid (invoice_id)
SELECT DISTINCT invoice.invoice_id
FROM hdbhms.invoices invoice
JOIN hdbhms.rooms room
  ON room.room_id = invoice.room_id
LEFT JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = invoice.lease_contract_id
LEFT JOIN hdbhms.person_profiles profile
  ON profile.person_profile_id = contract.primary_tenant_profile_id
WHERE invoice.property_id = @hdd1_property_id
  AND room.property_id = @hdd1_property_id
  AND invoice.billing_period = '2026-07'
  AND invoice.invoice_type <> 'DEPOSIT'
  AND (
      profile.email = 'nguyenvankhai95@gmail.com'
      OR profile.email IN (
          'nguyenducthinh96.2@gmail.com',
          'buiminhkhoa96@gmail.com'
      )
  );

-- V74 created synthetic debt-clearing payments for Khai's rooms. Remove only
-- those allocations and orphaned synthetic transactions; real payments stay.
DELETE allocation
FROM hdbhms.payment_allocations allocation
JOIN hdbhms.payment_transactions payment
  ON payment.payment_transaction_id = allocation.payment_transaction_id
JOIN tmp_hdd1_july_unpaid unpaid
  ON unpaid.invoice_id = allocation.invoice_id
WHERE payment.provider = 'MANUAL'
  AND payment.provider_transaction_id LIKE 'SEED-CLEAR-DEBT-%';

DELETE payment
FROM hdbhms.payment_transactions payment
LEFT JOIN hdbhms.payment_allocations allocation
  ON allocation.payment_transaction_id = payment.payment_transaction_id
WHERE payment.provider = 'MANUAL'
  AND payment.provider_transaction_id LIKE 'SEED-CLEAR-DEBT-%'
  AND allocation.payment_allocation_id IS NULL;

DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_july_unpaid_balance;
CREATE TEMPORARY TABLE tmp_hdd1_july_unpaid_balance (
    invoice_id      BIGINT UNSIGNED NOT NULL PRIMARY KEY,
    paid_amount     BIGINT UNSIGNED NOT NULL,
    remaining_amount BIGINT UNSIGNED NOT NULL
);

INSERT INTO tmp_hdd1_july_unpaid_balance
    (invoice_id, paid_amount, remaining_amount)
SELECT unpaid.invoice_id,
       LEAST(invoice.total_amount, COALESCE(SUM(
           CASE
               WHEN payment.status IN ('MATCHED', 'PARTIALLY_ALLOCATED', 'ALLOCATED')
                   THEN allocation.amount
               ELSE 0
           END
       ), 0)),
       GREATEST(invoice.total_amount - LEAST(invoice.total_amount, COALESCE(SUM(
           CASE
               WHEN payment.status IN ('MATCHED', 'PARTIALLY_ALLOCATED', 'ALLOCATED')
                   THEN allocation.amount
               ELSE 0
           END
       ), 0)), 0)
FROM tmp_hdd1_july_unpaid unpaid
JOIN hdbhms.invoices invoice
  ON invoice.invoice_id = unpaid.invoice_id
LEFT JOIN hdbhms.payment_allocations allocation
  ON allocation.invoice_id = invoice.invoice_id
LEFT JOIN hdbhms.payment_transactions payment
  ON payment.payment_transaction_id = allocation.payment_transaction_id
GROUP BY unpaid.invoice_id, invoice.total_amount;

UPDATE hdbhms.invoices invoice
JOIN tmp_hdd1_july_unpaid_balance balance
  ON balance.invoice_id = invoice.invoice_id
SET invoice.status = 'OVERDUE',
    invoice.paid_amount = balance.paid_amount,
    invoice.remaining_amount = balance.remaining_amount,
    invoice.voided_at = NULL,
    invoice.void_reason = NULL,
    invoice.updated_at = @hdd1_seed_now;

-- Remove VOIDED from the July seed before rebuilding the paid collection set.
-- The current rows are zero-value audit invoices, so this does not invent
-- revenue; non-zero rows are collected by the payment block below.
UPDATE hdbhms.invoices invoice
JOIN hdbhms.rooms room
  ON room.room_id = invoice.room_id
SET invoice.status = CASE
        WHEN invoice.paid_amount >= invoice.total_amount THEN 'PAID'
        ELSE 'OVERDUE'
    END,
    invoice.remaining_amount = GREATEST(invoice.total_amount - invoice.paid_amount, 0),
    invoice.voided_at = NULL,
    invoice.void_reason = NULL,
    invoice.updated_at = @hdd1_seed_now
WHERE room.property_id = @hdd1_property_id
  AND invoice.billing_period = '2026-07'
  AND invoice.status = 'VOIDED';

DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_july_paid_collection;
CREATE TEMPORARY TABLE tmp_hdd1_july_paid_collection (
    invoice_id       BIGINT UNSIGNED NOT NULL PRIMARY KEY,
    amount_to_collect BIGINT UNSIGNED NOT NULL
);

INSERT INTO tmp_hdd1_july_paid_collection (invoice_id, amount_to_collect)
SELECT collection.invoice_id, collection.amount_to_collect
FROM (
    SELECT invoice.invoice_id,
           GREATEST(invoice.total_amount - LEAST(
               invoice.total_amount,
               COALESCE(SUM(
                   CASE
                       WHEN payment.status IN ('MATCHED', 'PARTIALLY_ALLOCATED', 'ALLOCATED')
                           THEN allocation.amount
                       ELSE 0
                   END
               ), 0)
           ), 0) AS amount_to_collect
    FROM hdbhms.invoices invoice
    JOIN hdbhms.rooms room
      ON room.room_id = invoice.room_id
    LEFT JOIN tmp_hdd1_july_unpaid unpaid
      ON unpaid.invoice_id = invoice.invoice_id
    LEFT JOIN hdbhms.payment_allocations allocation
      ON allocation.invoice_id = invoice.invoice_id
    LEFT JOIN hdbhms.payment_transactions payment
      ON payment.payment_transaction_id = allocation.payment_transaction_id
    WHERE room.property_id = @hdd1_property_id
      AND invoice.billing_period = '2026-07'
      AND invoice.invoice_type <> 'DEPOSIT'
      AND unpaid.invoice_id IS NULL
    GROUP BY invoice.invoice_id, invoice.total_amount
) collection
WHERE collection.amount_to_collect > 0;

-- Use July transaction dates so the revenue report includes this seeded cash
-- collection in 2026-07 rather than in the migration execution month.
INSERT INTO hdbhms.payment_transactions
    (provider, provider_transaction_id, amount, transaction_time,
     payer_name, payer_account, content, status, raw_payload,
     confirmed_by, confirmed_at, created_at)
SELECT
    'MANUAL',
    CONCAT('SEED-JULY-COLLECTION-', collection.invoice_id),
    collection.amount_to_collect,
    '2026-07-31 18:00:00',
    'Seed July collection',
    'SEED-JULY-COLLECTION',
    CONCAT('July 2026 collection - invoice ', invoice.invoice_code),
    'ALLOCATED',
    CAST(JSON_OBJECT(
        'source', 'V86_SEED_JULY_COLLECTION',
        'invoiceId', collection.invoice_id,
        'amount', collection.amount_to_collect
    ) AS BINARY),
    @hdd1_manager_id,
    '2026-07-31 18:00:00',
    @hdd1_seed_now
FROM tmp_hdd1_july_paid_collection collection
JOIN hdbhms.invoices invoice
  ON invoice.invoice_id = collection.invoice_id
WHERE NOT EXISTS (
    SELECT 1
    FROM hdbhms.payment_transactions existing_payment
    WHERE existing_payment.provider = 'MANUAL'
      AND existing_payment.provider_transaction_id =
          CONCAT('SEED-JULY-COLLECTION-', collection.invoice_id)
);

INSERT INTO hdbhms.payment_allocations
    (payment_transaction_id, invoice_id, amount, allocated_by, allocated_at)
SELECT payment.payment_transaction_id,
       collection.invoice_id,
       collection.amount_to_collect,
       @hdd1_manager_id,
       '2026-07-31 18:00:00'
FROM tmp_hdd1_july_paid_collection collection
JOIN hdbhms.payment_transactions payment
  ON payment.provider = 'MANUAL'
 AND payment.provider_transaction_id =
        CONCAT('SEED-JULY-COLLECTION-', collection.invoice_id)
WHERE NOT EXISTS (
    SELECT 1
    FROM hdbhms.payment_allocations existing_allocation
    WHERE existing_allocation.payment_transaction_id = payment.payment_transaction_id
      AND existing_allocation.invoice_id = collection.invoice_id
);

-- All non-exempt July invoices are fully paid, including zero-value rows that
-- used to be VOIDED. Khai and the two selected demo tenants remain overdue.
UPDATE hdbhms.invoices invoice
JOIN hdbhms.rooms room
  ON room.room_id = invoice.room_id
LEFT JOIN tmp_hdd1_july_unpaid unpaid
  ON unpaid.invoice_id = invoice.invoice_id
SET invoice.status = 'PAID',
    invoice.paid_amount = invoice.total_amount,
    invoice.remaining_amount = 0,
    invoice.voided_at = NULL,
    invoice.void_reason = NULL,
    invoice.updated_at = @hdd1_seed_now
WHERE room.property_id = @hdd1_property_id
  AND invoice.billing_period = '2026-07'
  AND invoice.invoice_type <> 'DEPOSIT'
  AND unpaid.invoice_id IS NULL;

-- Backfill a cash-flow expense for any already-confirmed liquidation that has
-- a positive deposit refund. The current Hai Dang liquidation refunds zero, so
-- this block is intentionally a no-op for the current seed.
DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_refund_liquidations;
CREATE TEMPORARY TABLE tmp_hdd1_refund_liquidations (
    contract_liquidation_id BIGINT UNSIGNED NOT NULL PRIMARY KEY,
    property_id             BIGINT UNSIGNED NOT NULL,
    room_id                 BIGINT UNSIGNED NOT NULL,
    amount                  BIGINT UNSIGNED NOT NULL,
    liquidation_date        DATE NOT NULL
);

INSERT INTO tmp_hdd1_refund_liquidations
    (contract_liquidation_id, property_id, room_id, amount, liquidation_date)
SELECT liquidation.contract_liquidation_id,
       room.property_id,
       room.room_id,
       liquidation.deposit_refund_amount,
       liquidation.liquidation_date
FROM hdbhms.contract_liquidations liquidation
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = liquidation.contract_id
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
WHERE liquidation.status = 'CONFIRMED'
  AND liquidation.deposit_refund_amount > 0
  AND NOT EXISTS (
      SELECT 1
      FROM hdbhms.operating_expenses existing_expense
      WHERE existing_expense.expense_code = CONCAT(
          'LIQ-REFUND-', liquidation.contract_liquidation_id
      )
  );

INSERT INTO hdbhms.operating_expenses
    (property_id, room_id, ticket_id, expense_code, expense_type,
     description, amount, expense_date, paid_by_user_id, receipt_file_id,
     status, approved_by, approved_at, created_by, created_at)
SELECT refund.property_id,
       refund.room_id,
       NULL,
       CONCAT('LIQ-REFUND-', refund.contract_liquidation_id),
       'OTHER',
       CONCAT('Deposit refund for confirmed liquidation #', refund.contract_liquidation_id),
       refund.amount,
       refund.liquidation_date,
       @hdd1_manager_id,
       NULL,
       'PAID',
       @hdd1_manager_id,
       refund.liquidation_date,
       @hdd1_manager_id,
       @hdd1_seed_now
FROM tmp_hdd1_refund_liquidations refund;

INSERT INTO hdbhms.expense_payments
    (operating_expense_id, payment_date, payment_method, payment_reference,
     receipt_file_id, paid_by_user_id, paid_at, note, created_at)
SELECT expense.operating_expense_id,
       refund.liquidation_date,
       'BANK_TRANSFER',
       CONCAT('LIQ-REFUND-', refund.contract_liquidation_id),
       NULL,
       @hdd1_manager_id,
       refund.liquidation_date,
       'Seeded deposit refund expense for confirmed liquidation.',
       @hdd1_seed_now
FROM tmp_hdd1_refund_liquidations refund
JOIN hdbhms.operating_expenses expense
  ON expense.expense_code = CONCAT('LIQ-REFUND-', refund.contract_liquidation_id)
WHERE NOT EXISTS (
    SELECT 1
    FROM hdbhms.expense_payments existing_payment
    WHERE existing_payment.operating_expense_id = expense.operating_expense_id
);

DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_refund_liquidations;
DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_july_paid_collection;
DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_july_unpaid_balance;
DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_july_unpaid;
