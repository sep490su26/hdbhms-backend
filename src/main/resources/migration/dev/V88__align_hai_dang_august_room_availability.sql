SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- V88 is the final August seed correction. It only changes current August
-- occupancy state; July invoices and July payment allocations remain intact.
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

DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_khai_additions;
CREATE TEMPORARY TABLE tmp_hdd1_khai_additions
(
    room_code VARCHAR(10) NOT NULL PRIMARY KEY
);

INSERT INTO tmp_hdd1_khai_additions (room_code)
VALUES ('306'), ('501');

-- Preserve the previous primary tenant rows as history before assigning the
-- existing active contracts to Khai.
UPDATE hdbhms.contract_occupants occupant
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = occupant.contract_id
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
JOIN tmp_hdd1_khai_additions addition
  ON addition.room_code = room.room_code
SET occupant.status = 'MOVED_OUT',
    occupant.move_out_date = DATE(@hdd1_seed_now),
    occupant.disabled_reason = 'Reassigned to Nguyen Van Khai in the August seed.',
    occupant.disabled_by = @hdd1_manager_id,
    occupant.disabled_at = @hdd1_seed_now
WHERE room.property_id = @hdd1_property_id
  AND occupant.occupant_role = 'PRIMARY'
  AND occupant.status = 'ACTIVE'
  AND occupant.tenant_profile_id <> @hdd1_khai_profile_id;

UPDATE hdbhms.lease_contracts contract
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
JOIN tmp_hdd1_khai_additions addition
  ON addition.room_code = room.room_code
SET contract.primary_tenant_profile_id = @hdd1_khai_profile_id,
    contract.status = 'ACTIVE',
    contract.tenant_intention = NULL,
    contract.expected_vacant_date = NULL,
    contract.intention_recorded_at = NULL,
    contract.updated_at = @hdd1_seed_now
WHERE room.property_id = @hdd1_property_id
  AND contract.deleted_at IS NULL
  AND contract.status IN ('ACTIVE', 'EXPIRING_SOON', 'TERMINATION_PENDING');

-- Room 501 already has an old moved-out Khai occupant row from an earlier
-- seed pass. Reactivate it instead of violating the profile uniqueness key.
UPDATE hdbhms.contract_occupants occupant
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = occupant.contract_id
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
JOIN tmp_hdd1_khai_additions addition
  ON addition.room_code = room.room_code
SET occupant.tenant_id = @hdd1_khai_tenant_id,
    occupant.tenant_profile_id = @hdd1_khai_profile_id,
    occupant.occupant_role = 'PRIMARY',
    occupant.move_in_date = DATE(@hdd1_seed_now),
    occupant.move_out_date = NULL,
    occupant.status = 'ACTIVE',
    occupant.disabled_reason = NULL,
    occupant.disabled_by = NULL,
    occupant.disabled_at = NULL
WHERE room.property_id = @hdd1_property_id
  AND occupant.tenant_profile_id = @hdd1_khai_profile_id;

INSERT INTO hdbhms.contract_occupants
    (contract_id, tenant_id, tenant_profile_id, occupant_role, move_in_date,
     move_out_date, status, disabled_reason, disabled_by, disabled_at, created_at)
SELECT contract.lease_contract_id,
       @hdd1_khai_tenant_id,
       @hdd1_khai_profile_id,
       'PRIMARY',
       DATE(@hdd1_seed_now),
       NULL,
       'ACTIVE',
       NULL,
       NULL,
       NULL,
       @hdd1_seed_now
FROM hdbhms.lease_contracts contract
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
JOIN tmp_hdd1_khai_additions addition
  ON addition.room_code = room.room_code
WHERE room.property_id = @hdd1_property_id
  AND contract.deleted_at IS NULL
  AND contract.status = 'ACTIVE'
  AND NOT EXISTS (
      SELECT 1
      FROM hdbhms.contract_occupants existing_occupant
      WHERE existing_occupant.contract_id = contract.lease_contract_id
        AND existing_occupant.tenant_profile_id = @hdd1_khai_profile_id
  );

UPDATE hdbhms.rooms room
JOIN tmp_hdd1_khai_additions addition
  ON addition.room_code = room.room_code
SET room.current_status = 'OCCUPIED',
    room.public_note = 'Current contract assigned to Nguyen Van Khai.',
    room.internal_note = 'Seed V88: active contract assigned to Nguyen Van Khai.',
    room.updated_at = @hdd1_seed_now
WHERE room.property_id = @hdd1_property_id
  AND room.deleted_at IS NULL;

-- Release the selected rooms in August. Keep the original contract end dates as
-- historical terms; expected_vacant_date records the actual early release.
DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_release_rooms;
CREATE TEMPORARY TABLE tmp_hdd1_release_rooms
(
    room_code VARCHAR(10) NOT NULL PRIMARY KEY
);

INSERT INTO tmp_hdd1_release_rooms (room_code)
VALUES ('101'), ('102'), ('403'), ('405');

UPDATE hdbhms.lease_contracts contract
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
JOIN tmp_hdd1_release_rooms release_room
  ON release_room.room_code = room.room_code
SET contract.status = 'AUTO_TERMINATED',
    contract.tenant_intention = 'MOVE_OUT',
    contract.expected_vacant_date = DATE(@hdd1_seed_now),
    contract.intention_recorded_at = @hdd1_seed_now,
    contract.updated_at = @hdd1_seed_now
WHERE room.property_id = @hdd1_property_id
  AND contract.deleted_at IS NULL
  AND contract.status IN ('ACTIVE', 'EXPIRING_SOON', 'TERMINATION_PENDING');

UPDATE hdbhms.contract_occupants occupant
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = occupant.contract_id
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
JOIN tmp_hdd1_release_rooms release_room
  ON release_room.room_code = room.room_code
SET occupant.status = 'MOVED_OUT',
    occupant.move_out_date = DATE(@hdd1_seed_now),
    occupant.disabled_reason = 'Room released in the August seed.',
    occupant.disabled_by = @hdd1_manager_id,
    occupant.disabled_at = @hdd1_seed_now
WHERE room.property_id = @hdd1_property_id
  AND occupant.status = 'ACTIVE';

INSERT INTO hdbhms.contract_events
    (contract_id, event_type, event_data, created_by, created_at)
SELECT contract.lease_contract_id,
       'AUTO_TERMINATED',
       CAST(JSON_OBJECT(
           'source', 'V88_SEED_AUGUST_ROOM_RELEASE',
           'releasedAt', DATE_FORMAT(@hdd1_seed_now, '%Y-%m-%dT%H:%i:%s'),
           'roomCode', room.room_code
       ) AS BINARY),
       @hdd1_manager_id,
       @hdd1_seed_now
FROM hdbhms.lease_contracts contract
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
JOIN tmp_hdd1_release_rooms release_room
  ON release_room.room_code = room.room_code
WHERE room.property_id = @hdd1_property_id
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
       @hdd1_manager_id,
       @hdd1_seed_now
FROM hdbhms.rooms room
JOIN tmp_hdd1_release_rooms release_room
  ON release_room.room_code = room.room_code
WHERE room.property_id = @hdd1_property_id
  AND room.current_status <> 'VACANT'
  AND NOT EXISTS (
      SELECT 1
      FROM hdbhms.room_status_history existing_history
      WHERE existing_history.room_id = room.room_id
        AND existing_history.to_status = 'VACANT'
        AND existing_history.reason = 'August seed room release completed.'
  );

UPDATE hdbhms.rooms room
JOIN tmp_hdd1_release_rooms release_room
  ON release_room.room_code = room.room_code
SET room.current_status = 'VACANT',
    room.public_note = 'Vacant from August 2026 seed release.',
    room.internal_note = 'Seed V88: released after contract auto-termination.',
    room.updated_at = @hdd1_seed_now
WHERE room.property_id = @hdd1_property_id
  AND room.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM hdbhms.lease_contracts active_contract
      WHERE active_contract.room_id = room.room_id
        AND active_contract.deleted_at IS NULL
        AND active_contract.status IN ('ACTIVE', 'EXPIRING_SOON', 'TERMINATION_PENDING')
  );

-- Expiry reminders are no longer actionable after the rooms are released.
DELETE delivery
FROM hdbhms.notification_deliveries delivery
JOIN hdbhms.notification_outbox notification
  ON notification.notification_outbox_id = delivery.outbox_id
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = notification.target_id
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
JOIN tmp_hdd1_release_rooms release_room
  ON release_room.room_code = room.room_code
WHERE notification.target_type = 'CONTRACT'
  AND notification.event_type LIKE 'LEASE_EXPIRY%';

DELETE notification
FROM hdbhms.notification_outbox notification
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = notification.target_id
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
JOIN tmp_hdd1_release_rooms release_room
  ON release_room.room_code = room.room_code
WHERE notification.target_type = 'CONTRACT'
  AND notification.event_type LIKE 'LEASE_EXPIRY%';

DELETE tracker
FROM hdbhms.reminder_trackers tracker
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = tracker.target_id
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
JOIN tmp_hdd1_release_rooms release_room
  ON release_room.room_code = room.room_code
WHERE tracker.target_type = 'CONTRACT'
  AND tracker.reminder_key LIKE 'LEASE_EXPIRY%';

DELETE delivery
FROM hdbhms.notification_deliveries delivery
JOIN hdbhms.notification_outbox notification
  ON notification.notification_outbox_id = delivery.outbox_id
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = notification.target_id
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
JOIN tmp_hdd1_release_rooms release_room
  ON release_room.room_code = room.room_code
WHERE notification.target_type = 'CONTRACT'
  AND notification.event_type = 'CONTRACT_EXPIRING_SOON_REVIEW';

DELETE notification
FROM hdbhms.notification_outbox notification
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = notification.target_id
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
JOIN tmp_hdd1_release_rooms release_room
  ON release_room.room_code = room.room_code
WHERE notification.target_type = 'CONTRACT'
  AND notification.event_type = 'CONTRACT_EXPIRING_SOON_REVIEW';

-- Remove the three zero-value utility invoices entirely. The nullable FK
-- references are cleared first so this remains safe if a fresh seed contains
-- billing-run metadata for one of these rows.
DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_zero_invoices;
CREATE TEMPORARY TABLE tmp_hdd1_zero_invoices
(
    invoice_id BIGINT UNSIGNED NOT NULL PRIMARY KEY
);

INSERT INTO tmp_hdd1_zero_invoices (invoice_id)
SELECT invoice.invoice_id
FROM hdbhms.invoices invoice
JOIN hdbhms.rooms room
  ON room.room_id = invoice.room_id
WHERE invoice.property_id = @hdd1_property_id
  AND room.property_id = @hdd1_property_id
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
JOIN tmp_hdd1_zero_invoices target_invoice
  ON target_invoice.invoice_id = notification.target_id
WHERE notification.target_type = 'INVOICE';

DELETE notification
FROM hdbhms.notification_outbox notification
JOIN tmp_hdd1_zero_invoices target_invoice
  ON target_invoice.invoice_id = notification.target_id
WHERE notification.target_type = 'INVOICE';

DELETE allocation
FROM hdbhms.payment_allocations allocation
JOIN tmp_hdd1_zero_invoices target_invoice
  ON target_invoice.invoice_id = allocation.invoice_id;

UPDATE hdbhms.deposit_batches deposit_batch
JOIN tmp_hdd1_zero_invoices target_invoice
  ON target_invoice.invoice_id = deposit_batch.invoice_id
SET deposit_batch.invoice_id = NULL,
    deposit_batch.updated_at = @hdd1_seed_now;

UPDATE hdbhms.payment_intents payment_intent
JOIN tmp_hdd1_zero_invoices target_invoice
  ON target_invoice.invoice_id = payment_intent.invoice_id
SET payment_intent.invoice_id = NULL;

UPDATE hdbhms.pending_billing_charges pending_charge
JOIN tmp_hdd1_zero_invoices target_invoice
  ON target_invoice.invoice_id = pending_charge.invoice_id
SET pending_charge.invoice_id = NULL,
    pending_charge.status = 'CANCELLED',
    pending_charge.updated_at = @hdd1_seed_now;

UPDATE hdbhms.room_utility_baselines baseline
JOIN tmp_hdd1_zero_invoices target_invoice
  ON target_invoice.invoice_id = baseline.last_invoice_id
SET baseline.last_invoice_id = NULL,
    baseline.updated_at = @hdd1_seed_now;

UPDATE hdbhms.rule_violations violation
JOIN tmp_hdd1_zero_invoices target_invoice
  ON target_invoice.invoice_id = violation.invoice_id
SET violation.invoice_id = NULL,
    violation.status = CASE
        WHEN violation.status = 'INVOICED' THEN 'RECORDED'
        ELSE violation.status
    END;

UPDATE hdbhms.utility_billing_run_items item
JOIN tmp_hdd1_zero_invoices target_invoice
  ON target_invoice.invoice_id = item.invoice_id
SET item.invoice_id = NULL,
    item.status = 'SKIPPED',
    item.adjustment_reason = 'Removed zero-value seed invoice.';

-- Invoice-line deletion is guarded by a trigger and therefore requires the
-- issued rows to be returned to DRAFT for this seed-only cleanup.
UPDATE hdbhms.invoices invoice
JOIN tmp_hdd1_zero_invoices target_invoice
  ON target_invoice.invoice_id = invoice.invoice_id
SET invoice.status = 'DRAFT',
    invoice.updated_at = @hdd1_seed_now;

DELETE line
FROM hdbhms.invoice_lines line
JOIN tmp_hdd1_zero_invoices target_invoice
  ON target_invoice.invoice_id = line.invoice_id;

DELETE invoice
FROM hdbhms.invoices invoice
JOIN tmp_hdd1_zero_invoices target_invoice
  ON target_invoice.invoice_id = invoice.invoice_id;

-- Recompute Khai's account links after both the new assignments and releases.
UPDATE hdbhms.tenant_account_provisionings provisioning
SET provisioning.user_id = @hdd1_khai_user_id,
    provisioning.first_contract_id = (
        SELECT MIN(contract.lease_contract_id)
        FROM hdbhms.lease_contracts contract
        JOIN hdbhms.rooms room
          ON room.room_id = contract.room_id
        WHERE contract.primary_tenant_profile_id = @hdd1_khai_profile_id
          AND room.property_id = @hdd1_property_id
          AND contract.deleted_at IS NULL
          AND contract.status IN ('ACTIVE', 'EXPIRING_SOON', 'SIGNED', 'CONFIRMED')
    ),
    provisioning.latest_contract_id = (
        SELECT MAX(contract.lease_contract_id)
        FROM hdbhms.lease_contracts contract
        JOIN hdbhms.rooms room
          ON room.room_id = contract.room_id
        WHERE contract.primary_tenant_profile_id = @hdd1_khai_profile_id
          AND room.property_id = @hdd1_property_id
          AND contract.deleted_at IS NULL
          AND contract.status IN ('ACTIVE', 'EXPIRING_SOON', 'SIGNED', 'CONFIRMED')
    ),
    provisioning.status = 'ACTIVE',
    provisioning.recipient_email = @hdd1_khai_email,
    provisioning.updated_at = @hdd1_seed_now
WHERE provisioning.tenant_profile_id = @hdd1_khai_profile_id;

INSERT INTO hdbhms.tenant_account_provisionings
    (tenant_profile_id, user_id, first_contract_id, latest_contract_id, status,
     recipient_email, sent_at, attempt_count, created_at, updated_at)
SELECT @hdd1_khai_profile_id,
       @hdd1_khai_user_id,
       MIN(contract.lease_contract_id),
       MAX(contract.lease_contract_id),
       'ACTIVE',
       @hdd1_khai_email,
       @hdd1_seed_now,
       0,
       @hdd1_seed_now,
       @hdd1_seed_now
FROM hdbhms.lease_contracts contract
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
WHERE contract.primary_tenant_profile_id = @hdd1_khai_profile_id
  AND room.property_id = @hdd1_property_id
  AND contract.deleted_at IS NULL
  AND contract.status IN ('ACTIVE', 'EXPIRING_SOON', 'SIGNED', 'CONFIRMED')
  AND NOT EXISTS (
      SELECT 1
      FROM hdbhms.tenant_account_provisionings existing_provisioning
      WHERE existing_provisioning.tenant_profile_id = @hdd1_khai_profile_id
  )
HAVING MIN(contract.lease_contract_id) IS NOT NULL;

DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_zero_invoices;
DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_release_rooms;
DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_khai_additions;
