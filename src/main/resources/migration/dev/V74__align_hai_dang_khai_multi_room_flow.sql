SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- Give Nguyen Van Khai several active/expiring contracts so one tenant
-- account can exercise normal, transfer, liquidation, and blocked branches.
SET @hdd1_property_id := (
    SELECT property_id
    FROM hdbhms.properties
    WHERE property_code = 'HAI_DANG_1'
      AND deleted_at IS NULL
    LIMIT 1
);
SET @hdd1_seed_now := '2026-08-16 09:00:00';
SET @hdd1_manager_id := (
    SELECT COALESCE(
        (
            SELECT user_id
            FROM hdbhms.users
            WHERE email = 'seed.manager@hdbhms.local'
              AND deleted_at IS NULL
            LIMIT 1
        ),
        (
            SELECT user_id
            FROM hdbhms.users
            WHERE email = 'tranthuhuong90@gmail.com'
              AND deleted_at IS NULL
            LIMIT 1
        ),
        (
            SELECT user_id
            FROM hdbhms.users
            WHERE role = 'MANAGER'
              AND status = 'ACTIVE'
              AND deleted_at IS NULL
            ORDER BY user_id
            LIMIT 1
        )
    )
);
SET @hdd1_khai_user_id := (
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
SET @hdd1_khai_email := (
    SELECT email
    FROM hdbhms.users
    WHERE user_id = @hdd1_khai_user_id
    LIMIT 1
);
SET @hdd1_khai_tenant_id := (
    SELECT tenant_id
    FROM hdbhms.tenants
    WHERE user_id = @hdd1_khai_user_id
      AND property_id = @hdd1_property_id
      AND deleted_at IS NULL
    LIMIT 1
);
SET @hdd1_khai_profile_id := (
    SELECT person_profile_id
    FROM hdbhms.person_profiles
    WHERE user_id = @hdd1_khai_user_id
      AND deleted_at IS NULL
    LIMIT 1
);

DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_khai_contracts;
CREATE TEMPORARY TABLE tmp_hdd1_khai_contracts (
    room_code VARCHAR(10) NOT NULL PRIMARY KEY,
    end_date DATE NOT NULL,
    desired_status VARCHAR(30) NOT NULL
);

INSERT INTO tmp_hdd1_khai_contracts (room_code, end_date, desired_status)
VALUES
    ('301', '2026-12-31', 'ACTIVE'),
    -- The 302 contract has passed the 2/3 tenure threshold on 2026-08-16,
    -- so the tenant can exercise the room-transfer flow.
    ('302', '2026-11-30', 'ACTIVE'),
    ('303', '2026-12-31', 'ACTIVE'),
    ('401', '2026-12-31', 'ACTIVE'),
    ('501', '2026-12-31', 'ACTIVE'),
    ('507', '2026-12-31', 'ACTIVE'),
    -- These are the three remaining expiring rooms; move their contract
    -- profiles to Khai so one account can exercise expiry scenarios too.
    ('402', '2026-09-15', 'EXPIRING_SOON'),
    ('403', '2026-08-30', 'EXPIRING_SOON'),
    ('405', '2026-08-30', 'EXPIRING_SOON');

-- Keep the selected current contracts in their intended lifecycle state and
-- make Khai their primary tenant. Historical/liquidated contracts are
-- excluded by their status.
UPDATE hdbhms.lease_contracts contract
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
JOIN tmp_hdd1_khai_contracts scenario
  ON scenario.room_code = room.room_code
SET contract.primary_tenant_profile_id = @hdd1_khai_profile_id,
    contract.status = scenario.desired_status,
    contract.end_date = scenario.end_date,
    contract.tenant_intention = NULL,
    contract.expected_vacant_date = NULL,
    contract.intention_recorded_at = NULL,
    contract.updated_at = @hdd1_seed_now
WHERE room.property_id = @hdd1_property_id
  AND contract.deleted_at IS NULL
  AND contract.status IN ('ACTIVE', 'EXPIRING_SOON', 'TERMINATION_PENDING');

-- The old primary occupants are kept as history. Existing co-occupants
-- remain so the primary-leaves-while-others-stay branch is still available.
UPDATE hdbhms.contract_occupants occupant
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = occupant.contract_id
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
JOIN tmp_hdd1_khai_contracts scenario
  ON scenario.room_code = room.room_code
SET occupant.status = 'MOVED_OUT',
    occupant.move_out_date = DATE(@hdd1_seed_now),
    occupant.disabled_reason = 'Thay đổi người đứng tên seed để kiểm thử luồng hợp đồng.',
    occupant.disabled_by = @hdd1_manager_id,
    occupant.disabled_at = @hdd1_seed_now
WHERE room.property_id = @hdd1_property_id
  AND occupant.occupant_role = 'PRIMARY'
  AND occupant.status = 'ACTIVE'
  AND occupant.tenant_profile_id <> @hdd1_khai_profile_id;

INSERT INTO hdbhms.contract_occupants
    (contract_id, tenant_id, tenant_profile_id, occupant_role, move_in_date,
     move_out_date, status, disabled_reason, disabled_by, disabled_at, created_at)
SELECT
    contract.lease_contract_id,
    @hdd1_khai_tenant_id,
    @hdd1_khai_profile_id,
    'PRIMARY',
    contract.start_date,
    NULL,
    'ACTIVE',
    NULL,
    NULL,
    NULL,
    @hdd1_seed_now
FROM hdbhms.lease_contracts contract
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
JOIN tmp_hdd1_khai_contracts scenario
  ON scenario.room_code = room.room_code
WHERE room.property_id = @hdd1_property_id
  AND contract.deleted_at IS NULL
  AND contract.status = scenario.desired_status
  AND NOT EXISTS (
      SELECT 1
      FROM hdbhms.contract_occupants existing_occupant
      WHERE existing_occupant.contract_id = contract.lease_contract_id
        AND existing_occupant.occupant_role = 'PRIMARY'
        AND existing_occupant.status = 'ACTIVE'
  );

UPDATE hdbhms.rooms room
JOIN tmp_hdd1_khai_contracts scenario
  ON scenario.room_code = room.room_code
SET room.current_status = CASE
        WHEN scenario.desired_status = 'EXPIRING_SOON' THEN 'SOON_VACANT'
        ELSE 'OCCUPIED'
    END,
    room.public_note = CONCAT(
        CASE
            WHEN scenario.desired_status = 'EXPIRING_SOON'
                THEN 'Hợp đồng sắp hết hạn ngày '
            ELSE 'Hợp đồng đang hiệu lực đến ngày '
        END,
        DATE_FORMAT(scenario.end_date, '%d/%m/%Y'),
        '; hồ sơ đã chuyển cho tài khoản Nguyễn Văn Khải.'
    ),
    room.internal_note = CASE
        WHEN scenario.desired_status = 'EXPIRING_SOON'
            THEN 'Hợp đồng sắp hết hạn được gắn vào tài khoản Nguyễn Văn Khải để kiểm thử luồng thông báo và bàn giao.'
        ELSE 'Hợp đồng active được gắn vào tài khoản Nguyễn Văn Khải để kiểm thử luồng trên một tài khoản.'
    END,
    room.updated_at = @hdd1_seed_now
WHERE room.property_id = @hdd1_property_id
  AND room.deleted_at IS NULL;

-- Point old seeded requests for these current contracts back to the actual
-- primary tenant account, if a previous migration created them under a demo
-- user.
UPDATE hdbhms.change_requests request
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = request.target_id
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
JOIN tmp_hdd1_khai_contracts scenario
  ON scenario.room_code = room.room_code
SET request.requester_id = @hdd1_khai_user_id,
    request.requester_role = 'TENANT',
    request.updated_at = @hdd1_seed_now
WHERE request.target_type = 'CONTRACT'
  AND request.request_type <> 'ROOM_TRANSFER'
  AND request.requester_role = 'TENANT';

UPDATE hdbhms.room_transfer_requests transfer_request
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = transfer_request.old_contract_id
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
JOIN tmp_hdd1_khai_contracts scenario
  ON scenario.room_code = room.room_code
SET transfer_request.requester_id = @hdd1_khai_tenant_id,
    transfer_request.updated_at = @hdd1_seed_now;

UPDATE hdbhms.change_requests request
JOIN hdbhms.room_transfer_requests transfer_request
  ON transfer_request.room_transfer_request_id = request.target_id
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = transfer_request.old_contract_id
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
JOIN tmp_hdd1_khai_contracts scenario
  ON scenario.room_code = room.room_code
SET request.requester_id = @hdd1_khai_user_id,
    request.requester_role = 'TENANT',
    request.updated_at = @hdd1_seed_now
WHERE request.requester_role = 'TENANT';

UPDATE hdbhms.change_request_events event
JOIN hdbhms.change_requests request
  ON request.change_request_id = event.request_id
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = request.target_id
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
JOIN tmp_hdd1_khai_contracts scenario
  ON scenario.room_code = room.room_code
SET event.acted_by = @hdd1_khai_user_id
WHERE event.from_status IS NULL
  AND request.request_type <> 'ROOM_TRANSFER'
  AND event.to_status = 'PENDING';

-- Remove seeded requests that belong to Khai's rooms. Transfer requests use
-- a generic CONTRACT target in legacy seed data, so match them by request type
-- and old room instead of assuming target_id is a lease contract id.
DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_khai_transfer_requests;
CREATE TEMPORARY TABLE tmp_hdd1_khai_transfer_requests (
    room_transfer_request_id BIGINT UNSIGNED NOT NULL PRIMARY KEY
);

INSERT INTO tmp_hdd1_khai_transfer_requests (room_transfer_request_id)
SELECT transfer_request.room_transfer_request_id
FROM hdbhms.room_transfer_requests transfer_request
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = transfer_request.old_contract_id
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
JOIN tmp_hdd1_khai_contracts scenario
  ON scenario.room_code = room.room_code
WHERE room.property_id = @hdd1_property_id;

DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_khai_change_requests;
CREATE TEMPORARY TABLE tmp_hdd1_khai_change_requests (
    change_request_id BIGINT UNSIGNED NOT NULL PRIMARY KEY
);

INSERT INTO tmp_hdd1_khai_change_requests (change_request_id)
SELECT request.change_request_id
FROM hdbhms.change_requests request
WHERE (
        request.target_type = 'CONTRACT'
        AND request.request_type <> 'ROOM_TRANSFER'
        AND EXISTS (
            SELECT 1
            FROM hdbhms.lease_contracts contract
            JOIN hdbhms.rooms room
              ON room.room_id = contract.room_id
            JOIN tmp_hdd1_khai_contracts scenario
              ON scenario.room_code = room.room_code
            WHERE contract.lease_contract_id = request.target_id
              AND room.property_id = @hdd1_property_id
        )
    )
    OR (
        request.request_type = 'ROOM_TRANSFER'
        AND EXISTS (
            SELECT 1
            FROM hdbhms.room_transfer_requests transfer_request
            JOIN tmp_hdd1_khai_transfer_requests selected_transfer
              ON selected_transfer.room_transfer_request_id = transfer_request.room_transfer_request_id
            WHERE transfer_request.room_transfer_request_id = request.target_id
               OR transfer_request.request_code = request.request_code
        )
    )
    OR EXISTS (
        SELECT 1
        FROM hdbhms.rooms payload_room
        WHERE payload_room.property_id = @hdd1_property_id
          AND payload_room.room_code IN (
              '301', '302', '303', '401', '501', '507', '402', '403', '405'
          )
          AND (
              payload_room.room_code = JSON_UNQUOTE(
                  JSON_EXTRACT(request.request_payload, '$.roomCode')
              )
              OR payload_room.room_id = CAST(
                  JSON_UNQUOTE(JSON_EXTRACT(request.request_payload, '$.roomId'))
                  AS UNSIGNED
              )
          )
    );

DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_khai_expenses;
CREATE TEMPORARY TABLE tmp_hdd1_khai_expenses (
    operating_expense_id BIGINT UNSIGNED NOT NULL PRIMARY KEY
);

INSERT INTO tmp_hdd1_khai_expenses (operating_expense_id)
SELECT expense_approval.operating_expense_id
FROM hdbhms.expense_approval_requests expense_approval
JOIN tmp_hdd1_khai_change_requests selected_request
  ON selected_request.change_request_id = expense_approval.change_request_id;

DELETE delivery
FROM hdbhms.notification_deliveries delivery
JOIN hdbhms.notification_outbox notification
  ON notification.notification_outbox_id = delivery.outbox_id
JOIN tmp_hdd1_khai_change_requests selected_request
  ON selected_request.change_request_id = notification.target_id
WHERE notification.target_type = 'CHANGE_REQUEST';

DELETE notification
FROM hdbhms.notification_outbox notification
JOIN tmp_hdd1_khai_change_requests selected_request
  ON selected_request.change_request_id = notification.target_id
WHERE notification.target_type = 'CHANGE_REQUEST';

DELETE event
FROM hdbhms.change_request_events event
JOIN tmp_hdd1_khai_change_requests selected_request
  ON selected_request.change_request_id = event.request_id;

DELETE permission_grant
FROM hdbhms.permission_grants permission_grant
JOIN tmp_hdd1_khai_change_requests selected_request
  ON selected_request.change_request_id = permission_grant.source_change_request_id;

DELETE attachment
FROM hdbhms.expense_attachments attachment
JOIN tmp_hdd1_khai_expenses selected_expense
  ON selected_expense.operating_expense_id = attachment.operating_expense_id;

DELETE payment
FROM hdbhms.expense_payments payment
JOIN tmp_hdd1_khai_expenses selected_expense
  ON selected_expense.operating_expense_id = payment.operating_expense_id;

DELETE expense_approval
FROM hdbhms.expense_approval_requests expense_approval
JOIN tmp_hdd1_khai_expenses selected_expense
  ON selected_expense.operating_expense_id = expense_approval.operating_expense_id;

DELETE expense
FROM hdbhms.operating_expenses expense
JOIN tmp_hdd1_khai_expenses selected_expense
  ON selected_expense.operating_expense_id = expense.operating_expense_id;

DELETE request
FROM hdbhms.change_requests request
JOIN tmp_hdd1_khai_change_requests selected_request
  ON selected_request.change_request_id = request.change_request_id;

DELETE transfer_request
FROM hdbhms.room_transfer_requests transfer_request
JOIN tmp_hdd1_khai_transfer_requests selected_transfer
  ON selected_transfer.room_transfer_request_id = transfer_request.room_transfer_request_id;

-- Remove expiry reminders that no longer apply after rooms 301-303 return to
-- an active contract state.
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
  AND room.property_id = @hdd1_property_id
  AND room.room_code IN ('301', '302', '303')
  AND contract.status = 'ACTIVE';

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
  AND room.property_id = @hdd1_property_id
  AND room.room_code IN ('301', '302', '303')
  AND contract.status = 'ACTIVE';

DELETE tracker
FROM hdbhms.reminder_trackers tracker
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = tracker.target_id
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
WHERE tracker.target_type = 'CONTRACT'
  AND tracker.reminder_key = 'LEASE_EXPIRY_INTENTION'
  AND room.property_id = @hdd1_property_id
  AND room.room_code IN ('301', '302', '303')
  AND contract.status = 'ACTIVE';

-- Reassign the expiring-contract reminders to Khai after moving the primary
-- tenant profile. Manager/owner review notifications remain unchanged.
UPDATE hdbhms.reminder_trackers tracker
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = tracker.target_id
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
JOIN tmp_hdd1_khai_contracts scenario
  ON scenario.room_code = room.room_code
SET tracker.recipient_user_id = @hdd1_khai_user_id,
    tracker.updated_at = @hdd1_seed_now
WHERE tracker.reminder_key = 'LEASE_EXPIRY_INTENTION'
  AND tracker.target_type = 'CONTRACT'
  AND tracker.audience = 'PRIMARY_TENANT'
  AND scenario.desired_status = 'EXPIRING_SOON'
  AND contract.status = 'EXPIRING_SOON';

UPDATE hdbhms.notification_outbox notification
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = notification.target_id
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
JOIN tmp_hdd1_khai_contracts scenario
  ON scenario.room_code = room.room_code
SET notification.recipient_user_id = @hdd1_khai_user_id
WHERE notification.target_type = 'CONTRACT'
  AND notification.event_type IN (
      'LEASE_EXPIRY_REMINDER_FIRST',
      'LEASE_EXPIRY_REMINDER_SECOND',
      'LEASE_EXPIRY_REMINDER_FINAL'
  )
  AND scenario.desired_status = 'EXPIRING_SOON'
  AND contract.status = 'EXPIRING_SOON';

-- Clear outstanding debt for the selected rooms without deleting invoice
-- history. Each outstanding balance receives an idempotent manual payment so
-- the invoice/payment relationship remains consistent with normal payments.
DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_debt_invoices;
CREATE TEMPORARY TABLE tmp_hdd1_debt_invoices (
    invoice_id              BIGINT UNSIGNED NOT NULL PRIMARY KEY,
    room_code               VARCHAR(10) NOT NULL,
    outstanding_amount      BIGINT UNSIGNED NOT NULL,
    provider_transaction_id VARCHAR(255) NOT NULL
);

INSERT INTO tmp_hdd1_debt_invoices
    (invoice_id, room_code, outstanding_amount, provider_transaction_id)
SELECT
    invoice.invoice_id,
    room.room_code,
    invoice.remaining_amount,
    CONCAT('SEED-CLEAR-DEBT-', room.room_code, '-', invoice.invoice_id)
FROM hdbhms.invoices invoice
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = invoice.lease_contract_id
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
WHERE room.property_id = @hdd1_property_id
  AND room.room_code IN ('301', '302', '303', '402')
  AND contract.deleted_at IS NULL
  AND invoice.status IN ('ISSUED', 'PARTIALLY_PAID', 'OVERDUE')
  AND invoice.remaining_amount > 0;

INSERT INTO hdbhms.payment_transactions
    (provider, provider_transaction_id, amount, transaction_time,
     payer_name, payer_account, content, status, raw_payload,
     confirmed_by, confirmed_at, created_at)
SELECT
    'MANUAL',
    debt.provider_transaction_id,
    debt.outstanding_amount,
    @hdd1_seed_now,
    'Dữ liệu mẫu',
    'SEED-CLEAR-DEBT',
    CONCAT('Tất toán công nợ phòng ', debt.room_code, ' - hóa đơn ', invoice.invoice_code),
    'ALLOCATED',
    CAST(JSON_OBJECT(
        'source', 'V74_SEED_DEBT_CLEANUP',
        'roomCode', debt.room_code,
        'invoiceId', debt.invoice_id,
        'amount', debt.outstanding_amount
    ) AS BINARY),
    @hdd1_manager_id,
    @hdd1_seed_now,
    @hdd1_seed_now
FROM tmp_hdd1_debt_invoices debt
JOIN hdbhms.invoices invoice
  ON invoice.invoice_id = debt.invoice_id
WHERE NOT EXISTS (
    SELECT 1
    FROM hdbhms.payment_transactions existing_payment
    WHERE existing_payment.provider = 'MANUAL'
      AND existing_payment.provider_transaction_id = debt.provider_transaction_id
);

INSERT INTO hdbhms.payment_allocations
    (payment_transaction_id, invoice_id, amount, allocated_by, allocated_at)
SELECT
    payment.payment_transaction_id,
    debt.invoice_id,
    debt.outstanding_amount,
    @hdd1_manager_id,
    @hdd1_seed_now
FROM tmp_hdd1_debt_invoices debt
JOIN hdbhms.payment_transactions payment
  ON payment.provider = 'MANUAL'
 AND payment.provider_transaction_id = debt.provider_transaction_id
WHERE NOT EXISTS (
    SELECT 1
    FROM hdbhms.payment_allocations existing_allocation
    WHERE existing_allocation.payment_transaction_id = payment.payment_transaction_id
      AND existing_allocation.invoice_id = debt.invoice_id
);

UPDATE hdbhms.invoices invoice
JOIN tmp_hdd1_debt_invoices debt
  ON debt.invoice_id = invoice.invoice_id
SET invoice.status = 'PAID',
    invoice.paid_amount = invoice.total_amount,
    invoice.remaining_amount = 0,
    invoice.updated_at = @hdd1_seed_now;

-- Remove stale debt references and escalation trackers for these rooms after
-- the invoices have been settled.
UPDATE hdbhms.room_transfer_requests transfer_request
JOIN hdbhms.debt_snapshots snapshot
  ON snapshot.debt_snapshot_id = transfer_request.debt_snapshot_id
JOIN hdbhms.rooms room
  ON room.room_id = snapshot.room_id
SET transfer_request.debt_snapshot_id = NULL,
    transfer_request.updated_at = @hdd1_seed_now
WHERE room.property_id = @hdd1_property_id
  AND room.room_code IN ('301', '302', '303', '402');

DELETE tracker
FROM hdbhms.debt_notice_trackers tracker
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = tracker.lease_contract_id
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
WHERE room.property_id = @hdd1_property_id
  AND room.room_code IN ('301', '302', '303', '402');

DELETE snapshot
FROM hdbhms.debt_snapshots snapshot
JOIN hdbhms.rooms room
  ON room.room_id = snapshot.room_id
WHERE room.property_id = @hdd1_property_id
  AND room.room_code IN ('301', '302', '303', '402');

SET @hdd1_khai_first_contract_id := (
    SELECT MIN(contract.lease_contract_id)
    FROM hdbhms.lease_contracts contract
    JOIN hdbhms.rooms room
      ON room.room_id = contract.room_id
    JOIN tmp_hdd1_khai_contracts scenario
      ON scenario.room_code = room.room_code
    WHERE room.property_id = @hdd1_property_id
      AND contract.primary_tenant_profile_id = @hdd1_khai_profile_id
      AND contract.deleted_at IS NULL
);
SET @hdd1_khai_latest_contract_id := (
    SELECT MAX(contract.lease_contract_id)
    FROM hdbhms.lease_contracts contract
    JOIN hdbhms.rooms room
      ON room.room_id = contract.room_id
    JOIN tmp_hdd1_khai_contracts scenario
      ON scenario.room_code = room.room_code
    WHERE room.property_id = @hdd1_property_id
      AND contract.primary_tenant_profile_id = @hdd1_khai_profile_id
      AND contract.deleted_at IS NULL
);

UPDATE hdbhms.tenant_account_provisionings provisioning
SET provisioning.user_id = @hdd1_khai_user_id,
    provisioning.first_contract_id = @hdd1_khai_first_contract_id,
    provisioning.latest_contract_id = @hdd1_khai_latest_contract_id,
    provisioning.status = 'ACTIVE',
    provisioning.recipient_email = @hdd1_khai_email,
    provisioning.updated_at = @hdd1_seed_now
WHERE provisioning.tenant_profile_id = @hdd1_khai_profile_id;

INSERT INTO hdbhms.tenant_account_provisionings
    (tenant_profile_id, user_id, first_contract_id, latest_contract_id, status,
     recipient_email, sent_at, attempt_count, created_at, updated_at)
SELECT
    @hdd1_khai_profile_id,
    @hdd1_khai_user_id,
    @hdd1_khai_first_contract_id,
    @hdd1_khai_latest_contract_id,
    'ACTIVE',
    @hdd1_khai_email,
    @hdd1_seed_now,
    0,
    @hdd1_seed_now,
    @hdd1_seed_now
WHERE @hdd1_khai_profile_id IS NOT NULL
  AND @hdd1_khai_first_contract_id IS NOT NULL
ON DUPLICATE KEY UPDATE
    user_id = VALUES(user_id),
    first_contract_id = VALUES(first_contract_id),
    latest_contract_id = VALUES(latest_contract_id),
    status = 'ACTIVE',
    recipient_email = VALUES(recipient_email),
    sent_at = VALUES(sent_at),
    updated_at = VALUES(updated_at);
