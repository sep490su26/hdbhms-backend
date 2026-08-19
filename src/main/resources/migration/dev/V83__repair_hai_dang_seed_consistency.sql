SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- Repair the final Hai Dang snapshot after the earlier lifecycle scenarios have
-- run. This keeps the demo workflow data aligned with the July workbook.
SET @hdd1_property_id := (
    SELECT property_id
    FROM hdbhms.properties
    WHERE property_code = 'HAI_DANG_1'
      AND deleted_at IS NULL
    LIMIT 1
);
SET @hdd1_seed_now := '2026-08-01 09:00:00';
SET @hdd1_manager_id := (
    SELECT user_id
    FROM hdbhms.users
    WHERE email IN ('seed.manager@hdbhms.local', 'tranthuhuong90@gmail.com')
      AND deleted_at IS NULL
    ORDER BY email = 'seed.manager@hdbhms.local' DESC, user_id
    LIMIT 1
);
SET @hdd1_owner_id := (
    SELECT user_id
    FROM hdbhms.users
    WHERE email IN ('seed.owner@hdbhms.local', 'nguyenminhquang80@gmail.com')
      AND deleted_at IS NULL
    ORDER BY email = 'seed.owner@hdbhms.local' DESC, user_id
    LIMIT 1
);

DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_expected_primary;
CREATE TEMPORARY TABLE tmp_hdd1_expected_primary
(
    room_code       VARCHAR(10) NOT NULL PRIMARY KEY,
    profile_email   VARCHAR(255) NOT NULL,
    desired_status  VARCHAR(30) NOT NULL,
    desired_end_date DATE NOT NULL
);

INSERT INTO tmp_hdd1_expected_primary
    (room_code, profile_email, desired_status, desired_end_date)
VALUES
    ('301', 'nguyenvankhai95@gmail.com', 'EXPIRING_SOON', '2026-11-01'),
    ('302', 'nguyenvankhai95@gmail.com', 'EXPIRING_SOON', '2026-10-01'),
    ('303', 'nguyenvankhai95@gmail.com', 'EXPIRING_SOON', '2026-09-01'),
    ('401', 'nguyenvanhung95@gmail.com', 'ACTIVE', '2026-12-31'),
    ('402', 'nguyenducthinh96@gmail.com', 'EXPIRING_SOON', '2026-09-15'),
    ('403', 'nguyenducthinh96.2@gmail.com', 'EXPIRING_SOON', '2026-08-30'),
    ('405', 'duongminhduc96@gmail.com', 'ACTIVE', '2026-12-31'),
    ('501', 'levanphuc95@gmail.com', 'ACTIVE', '2026-12-31'),
    ('507', 'nguyenminhkhoi98@gmail.com', 'ACTIVE', '2026-12-31');

-- V74 temporarily attached several unrelated rooms to Khai's account. Keep
-- Khai's three expiry milestones, but restore the workbook primary tenant for
-- the other lifecycle rooms.
UPDATE hdbhms.lease_contracts contract
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
JOIN tmp_hdd1_expected_primary expected
  ON expected.room_code = room.room_code
JOIN hdbhms.person_profiles profile
  ON profile.email = expected.profile_email
 AND profile.deleted_at IS NULL
SET contract.primary_tenant_profile_id = profile.person_profile_id,
    contract.status = expected.desired_status,
    contract.end_date = expected.desired_end_date,
    contract.tenant_intention = NULL,
    contract.expected_vacant_date = NULL,
    contract.intention_recorded_at = NULL,
    contract.updated_at = @hdd1_seed_now
WHERE room.property_id = @hdd1_property_id
  AND contract.deleted_at IS NULL
  AND contract.status IN ('ACTIVE', 'EXPIRING_SOON', 'SIGNED', 'CONFIRMED', 'TERMINATION_PENDING');

UPDATE hdbhms.contract_occupants occupant
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = occupant.contract_id
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
JOIN tmp_hdd1_expected_primary expected
  ON expected.room_code = room.room_code
JOIN hdbhms.person_profiles profile
  ON profile.email = expected.profile_email
 AND profile.deleted_at IS NULL
SET occupant.status = 'MOVED_OUT',
    occupant.move_out_date = DATE(@hdd1_seed_now),
    occupant.disabled_reason = 'Replaced by the workbook primary tenant.',
    occupant.disabled_at = @hdd1_seed_now
WHERE room.property_id = @hdd1_property_id
  AND occupant.occupant_role = 'PRIMARY'
  AND occupant.status = 'ACTIVE'
  AND occupant.tenant_profile_id <> profile.person_profile_id;

UPDATE hdbhms.contract_occupants occupant
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = occupant.contract_id
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
JOIN tmp_hdd1_expected_primary expected
  ON expected.room_code = room.room_code
JOIN hdbhms.person_profiles profile
  ON profile.email = expected.profile_email
 AND profile.deleted_at IS NULL
SET occupant.status = 'ACTIVE',
    occupant.move_out_date = NULL,
    occupant.disabled_reason = NULL,
    occupant.disabled_by = NULL,
    occupant.disabled_at = NULL
WHERE room.property_id = @hdd1_property_id
  AND occupant.occupant_role = 'PRIMARY'
  AND occupant.tenant_profile_id = profile.person_profile_id
  AND occupant.status <> 'ACTIVE';

INSERT INTO hdbhms.contract_occupants
    (contract_id, tenant_id, tenant_profile_id, occupant_role, move_in_date,
     move_out_date, status, disabled_reason, disabled_by, disabled_at, created_at)
SELECT
    contract.lease_contract_id,
    tenant.tenant_id,
    profile.person_profile_id,
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
JOIN tmp_hdd1_expected_primary expected
  ON expected.room_code = room.room_code
JOIN hdbhms.person_profiles profile
  ON profile.email = expected.profile_email
 AND profile.deleted_at IS NULL
JOIN hdbhms.tenants tenant
  ON tenant.user_id = profile.user_id
 AND tenant.property_id = @hdd1_property_id
 AND tenant.deleted_at IS NULL
WHERE room.property_id = @hdd1_property_id
  AND contract.deleted_at IS NULL
  AND contract.status IN ('ACTIVE', 'EXPIRING_SOON', 'SIGNED', 'CONFIRMED')
  AND NOT EXISTS (
      SELECT 1
      FROM hdbhms.contract_occupants existing_occupant
      WHERE existing_occupant.contract_id = contract.lease_contract_id
        AND existing_occupant.occupant_role = 'PRIMARY'
        AND existing_occupant.tenant_profile_id = profile.person_profile_id
        AND existing_occupant.status = 'ACTIVE'
  );

UPDATE hdbhms.rooms room
JOIN tmp_hdd1_expected_primary expected
  ON expected.room_code = room.room_code
SET room.current_status = CASE
        WHEN expected.desired_status = 'EXPIRING_SOON' THEN 'SOON_VACANT'
        ELSE 'OCCUPIED'
    END,
    room.public_note = CASE
        WHEN expected.desired_status = 'EXPIRING_SOON'
            THEN CONCAT('Hợp đồng sắp hết hạn ngày ', DATE_FORMAT(expected.desired_end_date, '%d/%m/%Y'), '.')
        ELSE NULL
    END,
    room.internal_note = CASE
        WHEN expected.desired_status = 'EXPIRING_SOON'
            THEN 'Chưa ghi nhận ý định gia hạn, chuyển phòng hoặc chuyển đi.'
        ELSE NULL
    END,
    room.updated_at = @hdd1_seed_now
WHERE room.property_id = @hdd1_property_id
  AND room.deleted_at IS NULL;

-- Keep account provisioning links on the current contracts of each restored
-- primary profile instead of a contract from the temporary Khai scenario.
UPDATE hdbhms.tenant_account_provisionings provisioning
JOIN hdbhms.person_profiles profile
  ON profile.person_profile_id = provisioning.tenant_profile_id
SET provisioning.first_contract_id = (
        SELECT MIN(contract.lease_contract_id)
        FROM hdbhms.lease_contracts contract
        JOIN hdbhms.rooms room ON room.room_id = contract.room_id
        WHERE contract.primary_tenant_profile_id = profile.person_profile_id
          AND room.property_id = @hdd1_property_id
          AND contract.deleted_at IS NULL
          AND contract.status IN ('ACTIVE', 'EXPIRING_SOON', 'SIGNED', 'CONFIRMED')
    ),
    provisioning.latest_contract_id = (
        SELECT MAX(contract.lease_contract_id)
        FROM hdbhms.lease_contracts contract
        JOIN hdbhms.rooms room ON room.room_id = contract.room_id
        WHERE contract.primary_tenant_profile_id = profile.person_profile_id
          AND room.property_id = @hdd1_property_id
          AND contract.deleted_at IS NULL
          AND contract.status IN ('ACTIVE', 'EXPIRING_SOON', 'SIGNED', 'CONFIRMED')
    ),
    provisioning.user_id = profile.user_id,
    provisioning.recipient_email = profile.email,
    provisioning.status = 'ACTIVE',
    provisioning.updated_at = @hdd1_seed_now
WHERE profile.email IN (
    SELECT expected.profile_email
    FROM tmp_hdd1_expected_primary expected
);

-- A renewal request for the already vacant room 404 is no longer actionable.
DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_invalid_change_requests;
CREATE TEMPORARY TABLE tmp_hdd1_invalid_change_requests
(
    change_request_id BIGINT UNSIGNED NOT NULL PRIMARY KEY
);

INSERT INTO tmp_hdd1_invalid_change_requests (change_request_id)
SELECT request.change_request_id
FROM hdbhms.change_requests request
JOIN hdbhms.lease_contracts contract
  ON request.target_type = 'CONTRACT'
 AND contract.lease_contract_id = request.target_id
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
WHERE room.property_id = @hdd1_property_id
  AND request.request_type = 'CONTRACT_RENEWAL'
  AND contract.status = 'AUTO_TERMINATED';

DELETE delivery
FROM hdbhms.notification_deliveries delivery
JOIN hdbhms.notification_outbox notification
  ON notification.notification_outbox_id = delivery.outbox_id
JOIN tmp_hdd1_invalid_change_requests invalid_request
  ON invalid_request.change_request_id = notification.target_id
WHERE notification.target_type = 'CHANGE_REQUEST';

DELETE notification
FROM hdbhms.notification_outbox notification
JOIN tmp_hdd1_invalid_change_requests invalid_request
  ON invalid_request.change_request_id = notification.target_id
WHERE notification.target_type = 'CHANGE_REQUEST';

DELETE permission_grant
FROM hdbhms.permission_grants permission_grant
JOIN tmp_hdd1_invalid_change_requests invalid_request
  ON invalid_request.change_request_id = permission_grant.source_change_request_id;

DELETE expense_approval
FROM hdbhms.expense_approval_requests expense_approval
JOIN tmp_hdd1_invalid_change_requests invalid_request
  ON invalid_request.change_request_id = expense_approval.change_request_id;

DELETE event
FROM hdbhms.change_request_events event
JOIN tmp_hdd1_invalid_change_requests invalid_request
  ON invalid_request.change_request_id = event.request_id;

DELETE request
FROM hdbhms.change_requests request
JOIN tmp_hdd1_invalid_change_requests invalid_request
  ON invalid_request.change_request_id = request.change_request_id;

-- A room-transfer change request targets its old contract. Older seed data
-- used the transfer row id, which made the request open the wrong room.
UPDATE hdbhms.change_requests request
JOIN hdbhms.room_transfer_requests transfer_request
  ON transfer_request.request_code = request.request_code
JOIN hdbhms.rooms old_room
  ON old_room.room_id = transfer_request.old_room_id
SET request.target_type = 'CONTRACT',
    request.target_id = transfer_request.old_contract_id,
    request.updated_at = @hdd1_seed_now
WHERE request.request_type = 'ROOM_TRANSFER'
  AND old_room.property_id = @hdd1_property_id;

-- Incoming contracts that start in August must not carry a July utility debt.
DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_pre_start_invoices;
CREATE TEMPORARY TABLE tmp_hdd1_pre_start_invoices
(
    invoice_id BIGINT UNSIGNED NOT NULL PRIMARY KEY
);

INSERT INTO tmp_hdd1_pre_start_invoices (invoice_id)
SELECT invoice.invoice_id
FROM hdbhms.invoices invoice
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = invoice.lease_contract_id
JOIN hdbhms.rooms room
  ON room.room_id = invoice.room_id
WHERE invoice.property_id = @hdd1_property_id
  AND invoice.invoice_type = 'UTILITY'
  AND invoice.billing_period = '2026-07'
  AND invoice.status <> 'VOIDED'
  AND contract.start_date > '2026-07-31'
  AND room.room_code IN ('502', '504');

UPDATE hdbhms.invoices invoice
JOIN tmp_hdd1_pre_start_invoices invalid_invoice
  ON invalid_invoice.invoice_id = invoice.invoice_id
SET invoice.status = 'DRAFT',
    invoice.updated_at = @hdd1_seed_now;

DELETE delivery
FROM hdbhms.notification_deliveries delivery
JOIN hdbhms.notification_outbox notification
  ON notification.notification_outbox_id = delivery.outbox_id
JOIN tmp_hdd1_pre_start_invoices invalid_invoice
  ON invalid_invoice.invoice_id = notification.target_id
WHERE notification.target_type = 'INVOICE';

DELETE notification
FROM hdbhms.notification_outbox notification
JOIN tmp_hdd1_pre_start_invoices invalid_invoice
  ON invalid_invoice.invoice_id = notification.target_id
WHERE notification.target_type = 'INVOICE';

DELETE line
FROM hdbhms.invoice_lines line
JOIN tmp_hdd1_pre_start_invoices invalid_invoice
  ON invalid_invoice.invoice_id = line.invoice_id;

UPDATE hdbhms.invoices invoice
JOIN tmp_hdd1_pre_start_invoices invalid_invoice
  ON invalid_invoice.invoice_id = invoice.invoice_id
SET invoice.status = 'VOIDED',
    invoice.subtotal_amount = 0,
    invoice.discount_amount = 0,
    invoice.total_amount = 0,
    invoice.paid_amount = 0,
    invoice.remaining_amount = 0,
    invoice.voided_at = @hdd1_seed_now,
    invoice.void_reason = 'Utility period predates the contract start date.',
    invoice.updated_at = @hdd1_seed_now;

-- The July reading for room 401 is 1960 -> 2261. Repair the old lifecycle
-- invoice before recalculating its partial-payment balance.
UPDATE hdbhms.invoices invoice
SET invoice.status = 'DRAFT',
    invoice.updated_at = @hdd1_seed_now
WHERE invoice.invoice_code = 'HD_P401_01_07_2026_DV'
  AND invoice.status <> 'DRAFT';

UPDATE hdbhms.invoice_lines line
JOIN hdbhms.invoices invoice
  ON invoice.invoice_id = line.invoice_id
JOIN hdbhms.meter_readings reading
  ON reading.meter_reading_id = line.meter_reading_id
SET line.quantity = CAST(CEILING(GREATEST(reading.current_value - reading.previous_value, 0)) AS UNSIGNED),
    line.unit_price = 3500,
    line.description = 'Electricity room 401 July 2026'
WHERE invoice.invoice_code = 'HD_P401_01_07_2026_DV'
  AND line.line_type = 'ELECTRICITY'
  AND reading.reading_period = '2026-07';

UPDATE hdbhms.invoices invoice
JOIN (
    SELECT invoice_id, COALESCE(SUM(amount), 0) AS line_total
    FROM hdbhms.invoice_lines
    GROUP BY invoice_id
) totals
  ON totals.invoice_id = invoice.invoice_id
SET invoice.subtotal_amount = totals.line_total,
    invoice.total_amount = GREATEST(totals.line_total - invoice.discount_amount, 0),
    invoice.remaining_amount = GREATEST(
        GREATEST(totals.line_total - invoice.discount_amount, 0) - invoice.paid_amount,
        0
    ),
    invoice.status = 'ISSUED',
    invoice.updated_at = @hdd1_seed_now
WHERE invoice.invoice_code = 'HD_P401_01_07_2026_DV';

UPDATE hdbhms.invoices
SET status = 'OVERDUE',
    updated_at = @hdd1_seed_now
WHERE invoice_code = 'HD_P401_01_07_2026_DV'
  AND remaining_amount > 0;

-- Normalize legacy electricity meter aliases so every room has the same seed
-- identifier without changing the meter relationship.
UPDATE hdbhms.meters meter
JOIN hdbhms.rooms room
  ON room.room_id = meter.room_id
SET meter.meter_code = CONCAT('HD1-E-', room.room_code)
WHERE room.property_id = @hdd1_property_id
  AND meter.meter_type = 'ELECTRICITY'
  AND meter.status = 'ACTIVE';

-- Room 502 and 504 are occupied in the transfer workflow even though their
-- contracts are CONFIRMED/SIGNED, so they also need the standard asset list.
INSERT INTO hdbhms.room_assets
    (room_id, asset_name, asset_category, quantity, current_condition, description,
     image_file_id, created_at, updated_at, deleted_at)
SELECT
    room.room_id,
    asset.asset_name,
    asset.asset_category,
    asset.quantity,
    'GOOD',
    asset.asset_description,
    NULL,
    @hdd1_seed_now,
    @hdd1_seed_now,
    NULL
FROM hdbhms.rooms room
CROSS JOIN (
    SELECT 'Điều hòa + Remote' AS asset_name, 'Thiết bị điện tử' AS asset_category, 1 AS quantity,
           'Tài sản mặc định của phòng.' AS asset_description
    UNION ALL SELECT 'Thiết bị vệ sinh + phòng tắm', 'Thiết bị vệ sinh', 1,
                     'Xí, vòi xịt, vòi sen, lavabo, gương, phụ kiện'
    UNION ALL SELECT 'Bình nóng lạnh', 'Thiết bị điện tử', 1, 'Tài sản mặc định của phòng.'
    UNION ALL SELECT 'Tủ quần áo 3 buồng', 'Nội thất', 1, 'Tài sản mặc định của phòng.'
    UNION ALL SELECT 'Bàn học', 'Nội thất', 1, 'Tài sản mặc định của phòng.'
    UNION ALL SELECT 'Giường đôi/tầng + Dát giường', 'Nội thất', 1, 'Tài sản mặc định của phòng.'
    UNION ALL SELECT 'Cửa đi + cửa sổ', 'Cơ sở hạ tầng', 1, 'Tài sản mặc định của phòng.'
    UNION ALL SELECT 'Modem Internet', 'Thiết bị điện tử', 1, 'Tài sản mặc định của phòng.'
    UNION ALL SELECT 'Hệ thống điện: công tắc, ổ cắm, bóng điện', 'Cơ sở hạ tầng', 1,
                     'Tài sản mặc định của phòng.'
) asset
WHERE room.property_id = @hdd1_property_id
  AND room.current_status IN ('OCCUPIED', 'SOON_VACANT', 'RESERVED_FOR_TRANSFER')
  AND EXISTS (
      SELECT 1
      FROM hdbhms.lease_contracts contract
      JOIN hdbhms.contract_occupants occupant
        ON occupant.contract_id = contract.lease_contract_id
       AND occupant.status = 'ACTIVE'
      WHERE contract.room_id = room.room_id
        AND contract.deleted_at IS NULL
        AND contract.status IN ('ACTIVE', 'EXPIRING_SOON', 'SIGNED', 'CONFIRMED')
  )
  AND NOT EXISTS (
      SELECT 1
      FROM hdbhms.room_assets existing_asset
      WHERE existing_asset.room_id = room.room_id
        AND existing_asset.asset_name = asset.asset_name
        AND existing_asset.deleted_at IS NULL
  );

-- Rebuild July invoice notifications from the repaired primary tenant. This
-- removes stale deliveries left behind when V74 changed contract profiles.
DELETE delivery
FROM hdbhms.notification_deliveries delivery
JOIN hdbhms.notification_outbox notification
  ON notification.notification_outbox_id = delivery.outbox_id
JOIN hdbhms.invoices invoice
  ON invoice.invoice_id = notification.target_id
JOIN hdbhms.rooms room
  ON room.room_id = invoice.room_id
WHERE notification.target_type = 'INVOICE'
  AND notification.event_type = 'INVOICE_ISSUED'
  AND invoice.property_id = @hdd1_property_id
  AND invoice.billing_period = '2026-07';

DELETE notification
FROM hdbhms.notification_outbox notification
JOIN hdbhms.invoices invoice
  ON invoice.invoice_id = notification.target_id
JOIN hdbhms.rooms room
  ON room.room_id = invoice.room_id
WHERE notification.target_type = 'INVOICE'
  AND notification.event_type = 'INVOICE_ISSUED'
  AND invoice.property_id = @hdd1_property_id
  AND invoice.billing_period = '2026-07';

INSERT INTO hdbhms.notification_outbox
    (event_type, target_type, target_id, recipient_user_id, channel, title, body, payload, status,
     retry_count, max_retries, scheduled_at, sent_at, created_at, is_read)
SELECT
    'INVOICE_ISSUED',
    'INVOICE',
    invoice.invoice_id,
    profile.user_id,
    channel.channel,
    CONCAT('Có hóa đơn mới ', invoice.invoice_code),
    CONCAT('Hóa đơn ', invoice.invoice_code, ' của phòng ', room.room_code,
           ' kỳ ', invoice.billing_period, ' đã phát hành. Số tiền còn lại: ',
           invoice.remaining_amount, ' VND.'),
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
    @hdd1_seed_now,
    @hdd1_seed_now,
    @hdd1_seed_now,
    FALSE
FROM hdbhms.invoices invoice
JOIN hdbhms.rooms room
  ON room.room_id = invoice.room_id
JOIN hdbhms.properties property
  ON property.property_id = invoice.property_id
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = invoice.lease_contract_id
JOIN hdbhms.person_profiles profile
  ON profile.person_profile_id = contract.primary_tenant_profile_id
 AND profile.deleted_at IS NULL
CROSS JOIN (
    SELECT 'WEB' AS channel
    UNION ALL
    SELECT 'PUSH'
) channel
WHERE invoice.property_id = @hdd1_property_id
  AND invoice.invoice_type = 'UTILITY'
  AND invoice.billing_period = '2026-07'
  AND invoice.status <> 'VOIDED';

-- Rebuild tenant and management expiry notifications from the same contract
-- set. Co-occupants receive the same expiry reminders as the primary tenant.
DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_expiry_recipients;
CREATE TEMPORARY TABLE tmp_hdd1_expiry_recipients
(
    contract_id       BIGINT UNSIGNED NOT NULL,
    recipient_user_id BIGINT UNSIGNED NOT NULL,
    audience           VARCHAR(30) NOT NULL,
    PRIMARY KEY (contract_id, recipient_user_id, audience)
);

INSERT INTO tmp_hdd1_expiry_recipients
    (contract_id, recipient_user_id, audience)
SELECT contract.lease_contract_id, profile.user_id, 'PRIMARY_TENANT'
FROM hdbhms.lease_contracts contract
JOIN hdbhms.rooms room ON room.room_id = contract.room_id
JOIN hdbhms.person_profiles profile
  ON profile.person_profile_id = contract.primary_tenant_profile_id
 AND profile.deleted_at IS NULL
WHERE room.property_id = @hdd1_property_id
  AND contract.status = 'EXPIRING_SOON'
  AND contract.tenant_intention IS NULL
  AND contract.deleted_at IS NULL
UNION
SELECT contract.lease_contract_id, profile.user_id, 'CO_OCCUPANT'
FROM hdbhms.lease_contracts contract
JOIN hdbhms.rooms room ON room.room_id = contract.room_id
JOIN hdbhms.contract_occupants occupant
  ON occupant.contract_id = contract.lease_contract_id
 AND occupant.occupant_role = 'CO_OCCUPANT'
 AND occupant.status = 'ACTIVE'
JOIN hdbhms.person_profiles profile
  ON profile.person_profile_id = occupant.tenant_profile_id
 AND profile.deleted_at IS NULL
WHERE room.property_id = @hdd1_property_id
  AND contract.status = 'EXPIRING_SOON'
  AND contract.tenant_intention IS NULL
  AND contract.deleted_at IS NULL;

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
      'LEASE_EXPIRY_REMINDER_FINAL',
      'CONTRACT_EXPIRING_SOON_REVIEW'
  )
  AND room.property_id = @hdd1_property_id;

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
      'LEASE_EXPIRY_REMINDER_FINAL',
      'CONTRACT_EXPIRING_SOON_REVIEW'
  )
  AND room.property_id = @hdd1_property_id;

DELETE tracker
FROM hdbhms.reminder_trackers tracker
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = tracker.target_id
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
WHERE tracker.reminder_key = 'LEASE_EXPIRY_INTENTION'
  AND tracker.target_type = 'CONTRACT'
  AND room.property_id = @hdd1_property_id;

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
JOIN hdbhms.rooms room ON room.room_id = contract.room_id
JOIN tmp_hdd1_expiry_recipients recipient
  ON recipient.contract_id = contract.lease_contract_id
WHERE room.property_id = @hdd1_property_id
  AND contract.status = 'EXPIRING_SOON'
  AND contract.tenant_intention IS NULL
  AND contract.deleted_at IS NULL;

DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_expiry_stages;
CREATE TEMPORARY TABLE tmp_hdd1_expiry_stages
(
    stage_name VARCHAR(10) NOT NULL PRIMARY KEY,
    month_offset TINYINT UNSIGNED NOT NULL
);
INSERT INTO tmp_hdd1_expiry_stages (stage_name, month_offset)
VALUES ('FIRST', 3), ('SECOND', 2), ('FINAL', 1);

INSERT INTO hdbhms.notification_outbox
    (event_type, target_type, target_id, recipient_user_id, channel, title, body, payload, status,
     retry_count, max_retries, scheduled_at, sent_at, created_at, is_read)
SELECT
    CONCAT('LEASE_EXPIRY_REMINDER_', stage.stage_name),
    'CONTRACT',
    contract.lease_contract_id,
    recipient.recipient_user_id,
    'PUSH',
    CONCAT('Hợp đồng ', contract.contract_code, ' sắp hết hạn'),
    CONCAT('Phòng ', room.room_code, ' sẽ hết hạn vào ', contract.end_date,
           '. Vui lòng phản hồi ý định gia hạn, chuyển phòng hoặc chuyển đi.'),
    JSON_OBJECT(
        'contractId', contract.lease_contract_id,
        'contractCode', contract.contract_code,
        'roomId', room.room_id,
        'roomName', room.name,
        'roomCode', room.room_code,
        'propertyName', property.name,
        'endDate', contract.end_date,
        'daysRemaining', DATEDIFF(contract.end_date, DATE_SUB(contract.end_date, INTERVAL stage.month_offset MONTH)),
        'stage', stage.stage_name,
        'targetRoute', '/contract'
    ),
    'SENT',
    0,
    3,
    CONCAT(DATE_SUB(contract.end_date, INTERVAL stage.month_offset MONTH), ' 09:00:00'),
    CONCAT(DATE_SUB(contract.end_date, INTERVAL stage.month_offset MONTH), ' 09:00:00'),
    CONCAT(DATE_SUB(contract.end_date, INTERVAL stage.month_offset MONTH), ' 09:00:00'),
    FALSE
FROM hdbhms.lease_contracts contract
JOIN hdbhms.rooms room ON room.room_id = contract.room_id
JOIN hdbhms.properties property ON property.property_id = room.property_id
JOIN tmp_hdd1_expiry_recipients recipient
  ON recipient.contract_id = contract.lease_contract_id
CROSS JOIN tmp_hdd1_expiry_stages stage
WHERE room.property_id = @hdd1_property_id
  AND contract.status = 'EXPIRING_SOON'
  AND contract.tenant_intention IS NULL
  AND contract.deleted_at IS NULL
  AND DATE_SUB(contract.end_date, INTERVAL stage.month_offset MONTH) <= DATE(@hdd1_seed_now);

INSERT INTO hdbhms.notification_outbox
    (event_type, target_type, target_id, recipient_user_id, channel, title, body, payload, status,
     retry_count, max_retries, scheduled_at, sent_at, created_at, is_read)
SELECT
    'CONTRACT_EXPIRING_SOON_REVIEW',
    'CONTRACT',
    contract.lease_contract_id,
    recipient.recipient_user_id,
    channel.channel,
    CONCAT('Hợp đồng phòng ', room.room_code, ' sắp hết hạn'),
    CONCAT('Hợp đồng ', contract.contract_code, ' của phòng ', room.room_code,
           ' sẽ hết hạn vào ', contract.end_date, '. Vui lòng theo dõi và xử lý.'),
    JSON_OBJECT(
        'contractId', contract.lease_contract_id,
        'contractCode', contract.contract_code,
        'roomId', room.room_id,
        'roomName', room.name,
        'roomCode', room.room_code,
        'propertyName', property.name,
        'endDate', contract.end_date,
        'targetRoute', CONCAT('/dashboard/contracts/', contract.lease_contract_id)
    ),
    'SENT',
    0,
    3,
    @hdd1_seed_now,
    @hdd1_seed_now,
    @hdd1_seed_now,
    FALSE
FROM hdbhms.lease_contracts contract
JOIN hdbhms.rooms room ON room.room_id = contract.room_id
JOIN hdbhms.properties property ON property.property_id = room.property_id
JOIN (
    SELECT @hdd1_owner_id AS recipient_user_id
    UNION
    SELECT @hdd1_manager_id
) recipient
JOIN (
    SELECT 'WEB' AS channel
    UNION ALL
    SELECT 'PUSH'
) channel
WHERE room.property_id = @hdd1_property_id
  AND contract.status = 'EXPIRING_SOON'
  AND contract.tenant_intention IS NULL
  AND contract.deleted_at IS NULL
  AND recipient.recipient_user_id IS NOT NULL;

-- Keep the debt dashboard tied to the current settlement state. Room 403 is
-- intentionally retained as the over-limit debt scenario; room 505 is paid.
UPDATE hdbhms.debt_snapshots snapshot
JOIN hdbhms.rooms room ON room.room_id = snapshot.room_id
SET snapshot.utility_debt_amount = 630000,
    snapshot.mixed_debt_amount = snapshot.rent_debt_amount + 630000 + snapshot.other_debt_amount
WHERE room.property_id = @hdd1_property_id
  AND room.room_code = '503'
  AND snapshot.snapshot_date = '2026-07-30';

UPDATE hdbhms.room_transfer_requests transfer_request
JOIN hdbhms.rooms old_room ON old_room.room_id = transfer_request.old_room_id
JOIN hdbhms.rooms target_room ON target_room.room_id = transfer_request.target_room_id
SET transfer_request.debt_snapshot_id = CASE
        WHEN transfer_request.request_code = 'CP_P504_30_07_2026' THEN (
            SELECT snapshot.debt_snapshot_id
            FROM hdbhms.debt_snapshots snapshot
            JOIN hdbhms.rooms room ON room.room_id = snapshot.room_id
            WHERE room.room_code = '503'
              AND snapshot.snapshot_date = '2026-07-30'
            LIMIT 1
        )
        ELSE NULL
    END,
    transfer_request.updated_at = @hdd1_seed_now
WHERE old_room.property_id = @hdd1_property_id
  AND target_room.property_id = @hdd1_property_id
  AND transfer_request.request_code IN ('CP_P504_30_07_2026', 'CP_P506_20_07_2026');

DELETE snapshot
FROM hdbhms.debt_snapshots snapshot
JOIN hdbhms.rooms room ON room.room_id = snapshot.room_id
WHERE room.property_id = @hdd1_property_id
  AND room.room_code = '505'
  AND snapshot.snapshot_date = '2026-07-20';

DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_expected_primary;
DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_invalid_change_requests;
DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_pre_start_invoices;
DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_expiry_recipients;
DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_expiry_stages;
