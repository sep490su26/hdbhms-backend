SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- Backfill the notification rows that the application would create for the
-- final Hai Dang July invoice snapshot. Keep this migration additive so it
-- does not overwrite notifications created manually or by a later flow.
SET @hdd1_notification_property_id := (
    SELECT property_id
    FROM hdbhms.properties
    WHERE property_code = 'HAI_DANG_1'
      AND deleted_at IS NULL
    LIMIT 1
);
SET @hdd1_notification_now := '2026-08-20 09:00:00';

DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_notification_tenant_recipients;
CREATE TEMPORARY TABLE tmp_hdd1_notification_tenant_recipients
(
    invoice_id BIGINT UNSIGNED NOT NULL,
    recipient_user_id BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (invoice_id, recipient_user_id)
);

-- This matches the room-based recipient lookup used by invoice issue,
-- payment-success, and overdue-reminder flows.
INSERT INTO tmp_hdd1_notification_tenant_recipients (invoice_id, recipient_user_id)
SELECT DISTINCT
    invoice.invoice_id,
    user_account.user_id
FROM hdbhms.invoices invoice
JOIN hdbhms.rooms room
  ON room.room_id = invoice.room_id
JOIN (
    SELECT
        contract.room_id,
        contract.primary_tenant_profile_id AS tenant_profile_id
    FROM hdbhms.lease_contracts contract
    WHERE contract.deleted_at IS NULL
      AND contract.status IN ('SIGNED', 'ACTIVE', 'EXPIRING_SOON', 'TERMINATION_PENDING')
    UNION
    SELECT
        contract.room_id,
        occupant.tenant_profile_id
    FROM hdbhms.contract_occupants occupant
    JOIN hdbhms.lease_contracts contract
      ON contract.lease_contract_id = occupant.contract_id
    WHERE occupant.status = 'ACTIVE'
      AND occupant.tenant_profile_id IS NOT NULL
      AND contract.deleted_at IS NULL
      AND contract.status IN ('SIGNED', 'ACTIVE', 'EXPIRING_SOON', 'TERMINATION_PENDING')
) occupied
  ON occupied.room_id = room.room_id
JOIN hdbhms.person_profiles profile
  ON profile.person_profile_id = occupied.tenant_profile_id
 AND profile.deleted_at IS NULL
JOIN hdbhms.users user_account
  ON user_account.user_id = profile.user_id
 AND user_account.status = 'ACTIVE'
 AND user_account.deleted_at IS NULL
 AND user_account.role = 'TENANT'
WHERE invoice.property_id = @hdd1_notification_property_id
  AND invoice.billing_period = '2026-07'
  AND invoice.status <> 'VOIDED'
  AND invoice.total_amount > 0;

-- Fill missing INVOICE_ISSUED copies, including active co-occupants. Existing
-- rows for rooms released after July remain untouched.
INSERT INTO hdbhms.notification_outbox
    (event_type, target_type, target_id, recipient_user_id, channel, title, body, payload, status,
     retry_count, max_retries, scheduled_at, sent_at, created_at, is_read)
SELECT
    'INVOICE_ISSUED',
    'INVOICE',
    invoice.invoice_id,
    recipient.recipient_user_id,
    channel.channel,
    CONCAT('Có hóa đơn mới ', invoice.invoice_code),
    CONCAT(
        'Hóa đơn ', invoice.invoice_code,
        ' của phòng ', room.room_code,
        ' kỳ ', invoice.billing_period,
        ' đã phát hành. Số tiền cần thanh toán: ', invoice.remaining_amount,
        ' VND. Hạn thanh toán: ', DATE(invoice.due_date), '.'
    ),
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
    @hdd1_notification_now,
    @hdd1_notification_now,
    @hdd1_notification_now,
    FALSE
FROM hdbhms.invoices invoice
JOIN hdbhms.rooms room
  ON room.room_id = invoice.room_id
JOIN hdbhms.properties property
  ON property.property_id = invoice.property_id
JOIN tmp_hdd1_notification_tenant_recipients recipient
  ON recipient.invoice_id = invoice.invoice_id
CROSS JOIN (
    SELECT 'WEB' AS channel
    UNION ALL
    SELECT 'PUSH'
) channel
WHERE NOT EXISTS (
    SELECT 1
    FROM hdbhms.notification_outbox existing_notification
    WHERE existing_notification.event_type = 'INVOICE_ISSUED'
      AND existing_notification.target_type = 'INVOICE'
      AND existing_notification.target_id = invoice.invoice_id
      AND existing_notification.recipient_user_id = recipient.recipient_user_id
      AND existing_notification.channel = channel.channel
);

-- Manual payment and reconciled payment both publish INVOICE_PAID to the web
-- and push channels after the invoice becomes fully paid.
INSERT INTO hdbhms.notification_outbox
    (event_type, target_type, target_id, recipient_user_id, channel, title, body, payload, status,
     retry_count, max_retries, scheduled_at, sent_at, created_at, is_read)
SELECT
    'INVOICE_PAID',
    'INVOICE',
    invoice.invoice_id,
    recipient.recipient_user_id,
    channel.channel,
    CONCAT('Đã ghi nhận thanh toán hóa đơn ', invoice.invoice_code),
    CONCAT(
        'Hóa đơn ', invoice.invoice_code,
        ' của phòng ', room.room_code,
        ' đã được thanh toán đủ. Số tiền ghi nhận: ', invoice.paid_amount, ' VND.'
    ),
    JSON_OBJECT(
        'invoiceId', invoice.invoice_id,
        'invoiceCode', invoice.invoice_code,
        'invoiceType', invoice.invoice_type,
        'roomCode', room.room_code,
        'propertyName', property.name,
        'billingPeriod', invoice.billing_period,
        'paymentAmount', invoice.paid_amount,
        'paidAmount', invoice.paid_amount,
        'totalAmount', invoice.total_amount,
        'targetRoute', CONCAT('/dashboard/invoices/', invoice.invoice_id)
    ),
    'SENT',
    0,
    3,
    @hdd1_notification_now,
    @hdd1_notification_now,
    @hdd1_notification_now,
    FALSE
FROM hdbhms.invoices invoice
JOIN hdbhms.rooms room
  ON room.room_id = invoice.room_id
JOIN hdbhms.properties property
  ON property.property_id = invoice.property_id
JOIN tmp_hdd1_notification_tenant_recipients recipient
  ON recipient.invoice_id = invoice.invoice_id
CROSS JOIN (
    SELECT 'WEB' AS channel
    UNION ALL
    SELECT 'PUSH'
) channel
WHERE invoice.status = 'PAID'
  AND invoice.total_amount > 0
  AND invoice.invoice_type <> 'DEPOSIT'
  AND NOT EXISTS (
      SELECT 1
      FROM hdbhms.notification_outbox existing_notification
      WHERE existing_notification.event_type = 'INVOICE_PAID'
        AND existing_notification.target_type = 'INVOICE'
        AND existing_notification.target_id = invoice.invoice_id
        AND existing_notification.recipient_user_id = recipient.recipient_user_id
        AND existing_notification.channel = channel.channel
  );

-- The payment listener uses one preferred contact channel per tenant: EMAIL
-- when valid, otherwise SMS. Settlement invoices use their specialized event.
INSERT INTO hdbhms.notification_outbox
    (event_type, target_type, target_id, recipient_user_id, channel, title, body, payload, status,
     retry_count, max_retries, scheduled_at, sent_at, created_at, is_read)
SELECT
    CASE invoice.invoice_type
        WHEN 'FINAL_SETTLEMENT' THEN 'FINAL_SETTLEMENT_PAYMENT_SUCCESS'
        WHEN 'TRANSFER_DIFFERENCE' THEN 'TRANSFER_DIFFERENCE_PAYMENT_SUCCESS'
        ELSE 'INVOICE_PAYMENT_SUCCESS'
    END,
    'INVOICE',
    invoice.invoice_id,
    recipient.recipient_user_id,
    CASE
        WHEN recipient_user.email IS NOT NULL
         AND TRIM(recipient_user.email) <> ''
         AND LOWER(recipient_user.email) NOT LIKE '%@tenant.hdbhms.local'
         AND LOWER(recipient_user.email) NOT LIKE '%tenant.hdbhms.local'
        THEN 'EMAIL'
        ELSE 'SMS'
    END,
    CASE invoice.invoice_type
        WHEN 'FINAL_SETTLEMENT' THEN CONCAT('Đã thanh toán tất toán hợp đồng ', invoice.invoice_code)
        WHEN 'TRANSFER_DIFFERENCE' THEN CONCAT('Thanh toán chênh lệch chuyển phòng thành công - ', invoice.invoice_code)
        ELSE CONCAT('Thanh toán hóa đơn thành công - ', invoice.invoice_code)
    END,
    CONCAT(
        CASE invoice.invoice_type
            WHEN 'FINAL_SETTLEMENT' THEN 'Khoản tất toán hợp đồng '
            ELSE 'Hóa đơn '
        END,
        invoice.invoice_code,
        ' của phòng ', room.room_code,
        ' tại ', property.name,
        ' đã được thanh toán đầy đủ. Số tiền ghi nhận: ', invoice.paid_amount, ' VNĐ. Trân trọng.'
    ),
    JSON_OBJECT(
        'invoiceId', invoice.invoice_id,
        'invoiceCode', invoice.invoice_code,
        'invoiceType', invoice.invoice_type,
        'billingPeriod', invoice.billing_period,
        'period', invoice.billing_period,
        'propertyId', property.property_id,
        'propertyName', property.name,
        'roomId', room.room_id,
        'roomCode', room.room_code,
        'amount', invoice.total_amount,
        'totalAmount', invoice.total_amount,
        'paymentAmount', invoice.paid_amount,
        'paidAmount', invoice.paid_amount,
        'remainingAmount', invoice.remaining_amount,
        'paidAt', @hdd1_notification_now,
        'status', invoice.status,
        'targetRoute', CONCAT('/dashboard/invoices/', invoice.invoice_id)
    ),
    'SENT',
    0,
    3,
    @hdd1_notification_now,
    @hdd1_notification_now,
    @hdd1_notification_now,
    FALSE
FROM hdbhms.invoices invoice
JOIN hdbhms.rooms room
  ON room.room_id = invoice.room_id
JOIN hdbhms.properties property
  ON property.property_id = invoice.property_id
JOIN tmp_hdd1_notification_tenant_recipients recipient
  ON recipient.invoice_id = invoice.invoice_id
JOIN hdbhms.users recipient_user
  ON recipient_user.user_id = recipient.recipient_user_id
WHERE invoice.status = 'PAID'
  AND invoice.total_amount > 0
  AND invoice.invoice_type <> 'DEPOSIT'
  AND NOT EXISTS (
      SELECT 1
      FROM hdbhms.notification_outbox existing_notification
      WHERE existing_notification.event_type = CASE invoice.invoice_type
          WHEN 'FINAL_SETTLEMENT' THEN 'FINAL_SETTLEMENT_PAYMENT_SUCCESS'
          WHEN 'TRANSFER_DIFFERENCE' THEN 'TRANSFER_DIFFERENCE_PAYMENT_SUCCESS'
          ELSE 'INVOICE_PAYMENT_SUCCESS'
      END
        AND existing_notification.target_type = 'INVOICE'
        AND existing_notification.target_id = invoice.invoice_id
        AND existing_notification.recipient_user_id = recipient.recipient_user_id
  );

DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_notification_overdue_stages;
CREATE TEMPORARY TABLE tmp_hdd1_notification_overdue_stages
(
    invoice_id BIGINT UNSIGNED NOT NULL PRIMARY KEY,
    reminder_stage TINYINT UNSIGNED NOT NULL,
    reminder_date DATE NOT NULL
);

-- The scheduler's stage is based on the day after the due date. Only the
-- currently due stage is seeded; later stages are created by later runs.
INSERT INTO tmp_hdd1_notification_overdue_stages (invoice_id, reminder_stage, reminder_date)
SELECT
    invoice.invoice_id,
    CASE
        WHEN DATE(@hdd1_notification_now) < DATE_ADD(DATE(invoice.due_date), INTERVAL 1 DAY) THEN 0
        WHEN DATE(@hdd1_notification_now) < DATE_ADD(DATE(invoice.due_date), INTERVAL 1 MONTH) THEN 1
        WHEN DATE(@hdd1_notification_now) < DATE_ADD(DATE(invoice.due_date), INTERVAL 2 MONTH) THEN 2
        ELSE 3
    END,
    DATE_ADD(DATE(invoice.due_date), INTERVAL 1 DAY)
FROM hdbhms.invoices invoice
WHERE invoice.property_id = @hdd1_notification_property_id
  AND invoice.billing_period = '2026-07'
  AND invoice.status IN ('ISSUED', 'OVERDUE')
  AND invoice.remaining_amount > 0
  AND invoice.due_date < @hdd1_notification_now
  AND invoice.invoice_type IN ('RENT', 'UTILITY');

DELETE FROM tmp_hdd1_notification_overdue_stages
WHERE reminder_stage = 0;

-- Tenant reminder stage 1/2/3, matching BillingManagementService.
INSERT INTO hdbhms.notification_outbox
    (event_type, target_type, target_id, recipient_user_id, channel, title, body, payload, status,
     retry_count, max_retries, scheduled_at, sent_at, created_at, is_read)
SELECT
    CONCAT('INVOICE_OVERDUE_REMINDER_', overdue.reminder_stage),
    'INVOICE',
    invoice.invoice_id,
    recipient.recipient_user_id,
    channel.channel,
    CONCAT('Nhắc thanh toán hóa đơn ', invoice.invoice_code, ' - đợt ', overdue.reminder_stage),
    CONCAT(
        'Hóa đơn ', invoice.invoice_code,
        ' của phòng ', room.room_code,
        ' tại ', property.name,
        ' vẫn còn nợ ', invoice.remaining_amount,
        ' VND. Đây là lần nhắc thanh toán đợt ', overdue.reminder_stage,
        ', bắt đầu từ ngày ', overdue.reminder_date, '. Vui lòng thanh toán sớm.'
    ),
    JSON_OBJECT(
        'invoiceId', invoice.invoice_id,
        'invoiceCode', invoice.invoice_code,
        'roomCode', room.room_code,
        'propertyName', property.name,
        'remainingAmount', invoice.remaining_amount,
        'dueDate', DATE(invoice.due_date),
        'reminderStage', overdue.reminder_stage,
        'reminderDate', overdue.reminder_date,
        'targetRoute', CONCAT('/dashboard/invoices/', invoice.invoice_id)
    ),
    'SENT',
    0,
    3,
    CONCAT(overdue.reminder_date, ' 09:00:00'),
    CONCAT(overdue.reminder_date, ' 09:00:00'),
    @hdd1_notification_now,
    FALSE
FROM hdbhms.invoices invoice
JOIN hdbhms.rooms room
  ON room.room_id = invoice.room_id
JOIN hdbhms.properties property
  ON property.property_id = invoice.property_id
JOIN tmp_hdd1_notification_overdue_stages overdue
  ON overdue.invoice_id = invoice.invoice_id
JOIN tmp_hdd1_notification_tenant_recipients recipient
  ON recipient.invoice_id = invoice.invoice_id
CROSS JOIN (
    SELECT 'WEB' AS channel
    UNION ALL
    SELECT 'PUSH'
) channel
WHERE NOT EXISTS (
    SELECT 1
    FROM hdbhms.notification_outbox existing_notification
    WHERE existing_notification.event_type = CONCAT('INVOICE_OVERDUE_REMINDER_', overdue.reminder_stage)
      AND existing_notification.target_type = 'INVOICE'
      AND existing_notification.target_id = invoice.invoice_id
      AND existing_notification.recipient_user_id = recipient.recipient_user_id
      AND existing_notification.channel = channel.channel
);

DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_notification_owner_recipients;
CREATE TEMPORARY TABLE tmp_hdd1_notification_owner_recipients
(
    recipient_user_id BIGINT UNSIGNED NOT NULL PRIMARY KEY
);

-- Owner escalation includes every active owner and only active managers
-- assigned to Hai Dang 1; inactive user accounts are intentionally excluded.
INSERT INTO tmp_hdd1_notification_owner_recipients (recipient_user_id)
SELECT DISTINCT user_account.user_id
FROM hdbhms.users user_account
LEFT JOIN hdbhms.property_staff_assignments assignment
  ON assignment.staff_user_id = user_account.user_id
 AND assignment.property_id = @hdd1_notification_property_id
 AND assignment.assigned_role = 'MANAGER'
 AND assignment.assignment_status = 'ACTIVE'
 AND assignment.ended_at IS NULL
WHERE user_account.status = 'ACTIVE'
  AND user_account.deleted_at IS NULL
  AND (user_account.role = 'OWNER' OR assignment.staff_user_id IS NOT NULL);

-- Owner/manager escalation is only implemented for RENT and UTILITY.
INSERT INTO hdbhms.notification_outbox
    (event_type, target_type, target_id, recipient_user_id, channel, title, body, payload, status,
     retry_count, max_retries, scheduled_at, sent_at, created_at, is_read)
SELECT
    CONCAT('INVOICE_OVERDUE_OWNER_STAGE_', overdue.reminder_stage),
    'INVOICE',
    invoice.invoice_id,
    owner_recipient.recipient_user_id,
    channel.channel,
    CONCAT('Hóa đơn ', invoice.invoice_code, ' đã quá hạn - đợt ', overdue.reminder_stage),
    CONCAT(
        'Chủ trọ/quản lý: hóa đơn ', invoice.invoice_code,
        ' của phòng ', room.room_code,
        ' tại ', property.name,
        ' đã quá hạn sau ngày ', DATE(invoice.due_date),
        '. Số tiền còn nợ: ', invoice.remaining_amount,
        ' VND. Vui lòng kiểm tra và xử lý công nợ.'
    ),
    JSON_OBJECT(
        'invoiceId', invoice.invoice_id,
        'invoiceCode', invoice.invoice_code,
        'invoiceType', invoice.invoice_type,
        'roomCode', room.room_code,
        'propertyName', property.name,
        'remainingAmount', invoice.remaining_amount,
        'dueDate', DATE(invoice.due_date),
        'overdueStage', overdue.reminder_stage,
        'overdueStageDueDate', overdue.reminder_date,
        'recipientScope', 'OWNER_MANAGER',
        'targetRoute', '/dashboard/billing'
    ),
    'SENT',
    0,
    3,
    CONCAT(overdue.reminder_date, ' 09:00:00'),
    CONCAT(overdue.reminder_date, ' 09:00:00'),
    @hdd1_notification_now,
    FALSE
FROM hdbhms.invoices invoice
JOIN hdbhms.rooms room
  ON room.room_id = invoice.room_id
JOIN hdbhms.properties property
  ON property.property_id = invoice.property_id
JOIN tmp_hdd1_notification_overdue_stages overdue
  ON overdue.invoice_id = invoice.invoice_id
CROSS JOIN tmp_hdd1_notification_owner_recipients owner_recipient
CROSS JOIN (
    SELECT 'WEB' AS channel
    UNION ALL
    SELECT 'PUSH'
) channel
WHERE NOT EXISTS (
    SELECT 1
    FROM hdbhms.notification_outbox existing_notification
    WHERE existing_notification.event_type = CONCAT('INVOICE_OVERDUE_OWNER_STAGE_', overdue.reminder_stage)
      AND existing_notification.target_type = 'INVOICE'
      AND existing_notification.target_id = invoice.invoice_id
      AND existing_notification.recipient_user_id = owner_recipient.recipient_user_id
      AND existing_notification.channel = channel.channel
);

DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_notification_owner_recipients;
DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_notification_overdue_stages;
DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_notification_tenant_recipients;
