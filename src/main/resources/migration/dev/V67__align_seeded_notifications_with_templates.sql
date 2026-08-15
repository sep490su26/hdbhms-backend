-- Render seeded notifications with the same effective template rules as the
-- application: an active database template overrides the default; an inactive
-- or missing database template falls back to NotificationTemplateDefaults.
SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

SET @hdd1_property_id := (
    SELECT property_id
    FROM hdbhms.properties
    WHERE property_code = 'HAI_DANG_1'
      AND deleted_at IS NULL
    LIMIT 1
);

SET @tpl_contract_id := CONCAT('[[', '$', '{contractId}]]');
SET @tpl_contract_code := CONCAT('[[', '$', '{contractCode}]]');
SET @tpl_room_id := CONCAT('[[', '$', '{roomId}]]');
SET @tpl_room_name := CONCAT('[[', '$', '{roomName}]]');
SET @tpl_room_code := CONCAT('[[', '$', '{roomCode}]]');
SET @tpl_property_name := CONCAT('[[', '$', '{propertyName}]]');
SET @tpl_end_date := CONCAT('[[', '$', '{endDate}]]');
SET @tpl_days_remaining := CONCAT('[[', '$', '{daysRemaining}]]');
SET @tpl_stage := CONCAT('[[', '$', '{stage}]]');
SET @tpl_target_route := CONCAT('[[', '$', '{targetRoute}]]');
SET @tpl_tenant_intention := CONCAT('[[', '$', '{tenantIntention}]]');
SET @tpl_invoice_id := CONCAT('[[', '$', '{invoiceId}]]');
SET @tpl_invoice_code := CONCAT('[[', '$', '{invoiceCode}]]');
SET @tpl_invoice_type := CONCAT('[[', '$', '{invoiceType}]]');
SET @tpl_billing_period := CONCAT('[[', '$', '{billingPeriod}]]');
SET @tpl_amount := CONCAT('[[', '$', '{amount}]]');
SET @tpl_total_amount := CONCAT('[[', '$', '{totalAmount}]]');
SET @tpl_remaining_amount := CONCAT('[[', '$', '{remainingAmount}]]');
SET @tpl_due_date := CONCAT('[[', '$', '{dueDate}]]');

DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_notification_templates;
CREATE TEMPORARY TABLE tmp_hdd1_notification_templates
(
    event_type             VARCHAR(100) NOT NULL,
    channel                VARCHAR(10)  NOT NULL,
    default_title_template VARCHAR(255) NOT NULL,
    default_body_template  TEXT         NOT NULL,
    PRIMARY KEY (event_type, channel)
);

INSERT INTO tmp_hdd1_notification_templates
    (event_type, channel, default_title_template, default_body_template)
VALUES
    (
        'LEASE_EXPIRY_REMINDER_FIRST',
        'PUSH',
        CONCAT('Hợp đồng ', @tpl_contract_code, ' sắp hết hạn'),
        CONCAT('Phòng ', @tpl_room_name, ' tại ', @tpl_property_name, ' sẽ hết hạn vào ', @tpl_end_date, '. Bạn muốn gia hạn, chuyển phòng hay chuyển đi?')
    ),
    (
        'LEASE_EXPIRY_REMINDER_SECOND',
        'PUSH',
        CONCAT('Bạn chưa phản hồi về hợp đồng ', @tpl_contract_code),
        CONCAT('Vui lòng chọn ý định cho phòng ', @tpl_room_name, ' trước ngày hết hạn ', @tpl_end_date, ' để quản lý sắp xếp kịp thời.')
    ),
    (
        'LEASE_EXPIRY_REMINDER_FINAL',
        'PUSH',
        CONCAT('Nhắc lần cuối về hợp đồng ', @tpl_contract_code),
        CONCAT('Hợp đồng phòng ', @tpl_room_name, ' sắp hết hạn vào ', @tpl_end_date, '. Vui lòng phản hồi để tránh chậm xử lý bàn giao hoặc gia hạn.')
    ),
    (
        'INVOICE_ISSUED',
        'WEB',
        CONCAT('Có hóa đơn mới ', @tpl_invoice_code),
        CONCAT('Hóa đơn ', @tpl_invoice_code, ' của phòng ', @tpl_room_code, ' kỳ ', @tpl_billing_period, ' đã phát hành. Số tiền cần thanh toán: ', @tpl_remaining_amount, ' VND. Hạn thanh toán: ', @tpl_due_date, '.')
    ),
    (
        'INVOICE_ISSUED',
        'PUSH',
        CONCAT('Có hóa đơn mới ', @tpl_invoice_code),
        CONCAT('Hóa đơn ', @tpl_invoice_code, ' của phòng ', @tpl_room_code, ' kỳ ', @tpl_billing_period, ' đã phát hành. Số tiền cần thanh toán: ', @tpl_remaining_amount, ' VND. Hạn thanh toán: ', @tpl_due_date, '.')
    ),
    (
        'CONTRACT_EXPIRING_SOON_REVIEW',
        'WEB',
        CONCAT('Hợp đồng phòng ', @tpl_room_code, ' sắp hết hạn'),
        CONCAT('Hợp đồng ', @tpl_contract_code, ' của phòng ', @tpl_room_code, ' sẽ hết hạn vào ', @tpl_end_date, '. Khách thuê chưa ghi nhận ý định; vui lòng theo dõi và xử lý khi có phản hồi.')
    ),
    (
        'CONTRACT_EXPIRING_SOON_REVIEW',
        'PUSH',
        CONCAT('Hợp đồng phòng ', @tpl_room_code, ' sắp hết hạn'),
        CONCAT('Hợp đồng ', @tpl_contract_code, ' của phòng ', @tpl_room_code, ' sẽ hết hạn vào ', @tpl_end_date, '. Khách thuê chưa ghi nhận ý định; vui lòng theo dõi và xử lý khi có phản hồi.')
    );

-- Lease reminder templates. The replacement list includes every variable
-- allowed by the reminder definition so custom templates render too.
UPDATE hdbhms.notification_outbox notification
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = notification.target_id
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
JOIN hdbhms.properties property
  ON property.property_id = room.property_id
JOIN tmp_hdd1_notification_templates defaults
  ON defaults.event_type = notification.event_type
 AND defaults.channel = notification.channel
LEFT JOIN hdbhms.notification_templates custom_template
  ON custom_template.template_key = notification.event_type
 AND custom_template.channel = notification.channel
 AND custom_template.status = 'ACTIVE'
SET notification.title = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
        COALESCE(custom_template.title_template, defaults.default_title_template),
        @tpl_contract_id, CAST(contract.lease_contract_id AS CHAR)),
        @tpl_contract_code, contract.contract_code),
        @tpl_room_id, CAST(room.room_id AS CHAR)),
        @tpl_room_name, room.name),
        @tpl_property_name, property.name),
        @tpl_end_date, DATE_FORMAT(contract.end_date, '%Y-%m-%d')),
        @tpl_days_remaining, CAST(COALESCE(DATEDIFF(contract.end_date, DATE(notification.scheduled_at)), 0) AS CHAR)),
        @tpl_stage, CASE
            WHEN notification.event_type = 'LEASE_EXPIRY_REMINDER_FIRST' THEN 'FIRST'
            WHEN notification.event_type = 'LEASE_EXPIRY_REMINDER_SECOND' THEN 'SECOND'
            ELSE 'FINAL'
        END),
        @tpl_target_route, COALESCE(JSON_UNQUOTE(JSON_EXTRACT(notification.payload, '$.targetRoute')), '/contract')),
    notification.body = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
        COALESCE(custom_template.body_template, defaults.default_body_template),
        @tpl_contract_id, CAST(contract.lease_contract_id AS CHAR)),
        @tpl_contract_code, contract.contract_code),
        @tpl_room_id, CAST(room.room_id AS CHAR)),
        @tpl_room_name, room.name),
        @tpl_property_name, property.name),
        @tpl_end_date, DATE_FORMAT(contract.end_date, '%Y-%m-%d')),
        @tpl_days_remaining, CAST(COALESCE(DATEDIFF(contract.end_date, DATE(notification.scheduled_at)), 0) AS CHAR)),
        @tpl_stage, CASE
            WHEN notification.event_type = 'LEASE_EXPIRY_REMINDER_FIRST' THEN 'FIRST'
            WHEN notification.event_type = 'LEASE_EXPIRY_REMINDER_SECOND' THEN 'SECOND'
            ELSE 'FINAL'
        END),
        @tpl_target_route, COALESCE(JSON_UNQUOTE(JSON_EXTRACT(notification.payload, '$.targetRoute')), '/contract'))
WHERE property.property_id = @hdd1_property_id
  AND notification.target_type = 'CONTRACT'
  AND notification.channel = 'PUSH'
  AND notification.event_type IN (
      'LEASE_EXPIRY_REMINDER_FIRST',
      'LEASE_EXPIRY_REMINDER_SECOND',
      'LEASE_EXPIRY_REMINDER_FINAL'
  );

-- Issued invoice templates. Keep WEB and PUSH in sync with the effective
-- template selected by NotificationService.
UPDATE hdbhms.notification_outbox notification
JOIN hdbhms.invoices invoice
  ON invoice.invoice_id = notification.target_id
JOIN hdbhms.rooms room
  ON room.room_id = invoice.room_id
JOIN hdbhms.properties property
  ON property.property_id = invoice.property_id
JOIN tmp_hdd1_notification_templates defaults
  ON defaults.event_type = notification.event_type
 AND defaults.channel = notification.channel
LEFT JOIN hdbhms.notification_templates custom_template
  ON custom_template.template_key = notification.event_type
 AND custom_template.channel = notification.channel
 AND custom_template.status = 'ACTIVE'
SET notification.title = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
        COALESCE(custom_template.title_template, defaults.default_title_template),
        @tpl_invoice_id, CAST(invoice.invoice_id AS CHAR)),
        @tpl_invoice_code, invoice.invoice_code),
        @tpl_invoice_type, invoice.invoice_type),
        @tpl_room_code, room.room_code),
        @tpl_property_name, property.name),
        @tpl_billing_period, invoice.billing_period),
        @tpl_amount, CAST(COALESCE(invoice.total_amount, 0) AS CHAR)),
        @tpl_total_amount, CAST(COALESCE(invoice.total_amount, 0) AS CHAR)),
        @tpl_remaining_amount, CAST(COALESCE(invoice.remaining_amount, 0) AS CHAR)),
        @tpl_due_date, DATE_FORMAT(invoice.due_date, '%Y-%m-%d')),
        @tpl_target_route, COALESCE(JSON_UNQUOTE(JSON_EXTRACT(notification.payload, '$.targetRoute')), '/payment')),
    notification.body = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
        COALESCE(custom_template.body_template, defaults.default_body_template),
        @tpl_invoice_id, CAST(invoice.invoice_id AS CHAR)),
        @tpl_invoice_code, invoice.invoice_code),
        @tpl_invoice_type, invoice.invoice_type),
        @tpl_room_code, room.room_code),
        @tpl_property_name, property.name),
        @tpl_billing_period, invoice.billing_period),
        @tpl_amount, CAST(COALESCE(invoice.total_amount, 0) AS CHAR)),
        @tpl_total_amount, CAST(COALESCE(invoice.total_amount, 0) AS CHAR)),
        @tpl_remaining_amount, CAST(COALESCE(invoice.remaining_amount, 0) AS CHAR)),
        @tpl_due_date, DATE_FORMAT(invoice.due_date, '%Y-%m-%d')),
        @tpl_target_route, COALESCE(JSON_UNQUOTE(JSON_EXTRACT(notification.payload, '$.targetRoute')), '/payment'))
WHERE property.property_id = @hdd1_property_id
  AND notification.target_type = 'INVOICE'
  AND notification.event_type = 'INVOICE_ISSUED'
  AND notification.channel IN ('WEB', 'PUSH')
  AND invoice.invoice_code LIKE 'HD_P%_01_08_2026_DV';

-- Management review notifications use the contract-review template for both
-- channels and also honor an active custom template from the database.
UPDATE hdbhms.notification_outbox notification
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = notification.target_id
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
JOIN hdbhms.properties property
  ON property.property_id = room.property_id
JOIN tmp_hdd1_notification_templates defaults
  ON defaults.event_type = notification.event_type
 AND defaults.channel = notification.channel
LEFT JOIN hdbhms.notification_templates custom_template
  ON custom_template.template_key = notification.event_type
 AND custom_template.channel = notification.channel
 AND custom_template.status = 'ACTIVE'
SET notification.title = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
        COALESCE(custom_template.title_template, defaults.default_title_template),
        @tpl_contract_id, CAST(contract.lease_contract_id AS CHAR)),
        @tpl_contract_code, contract.contract_code),
        @tpl_room_id, CAST(room.room_id AS CHAR)),
        @tpl_room_name, room.name),
        @tpl_room_code, room.room_code),
        @tpl_property_name, property.name),
        @tpl_end_date, DATE_FORMAT(contract.end_date, '%Y-%m-%d')),
        @tpl_tenant_intention, COALESCE(contract.tenant_intention, '')),
        @tpl_target_route, COALESCE(JSON_UNQUOTE(JSON_EXTRACT(notification.payload, '$.targetRoute')), CONCAT('/dashboard/contracts/', contract.lease_contract_id))),
    notification.body = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
        COALESCE(custom_template.body_template, defaults.default_body_template),
        @tpl_contract_id, CAST(contract.lease_contract_id AS CHAR)),
        @tpl_contract_code, contract.contract_code),
        @tpl_room_id, CAST(room.room_id AS CHAR)),
        @tpl_room_name, room.name),
        @tpl_room_code, room.room_code),
        @tpl_property_name, property.name),
        @tpl_end_date, DATE_FORMAT(contract.end_date, '%Y-%m-%d')),
        @tpl_tenant_intention, COALESCE(contract.tenant_intention, '')),
        @tpl_target_route, COALESCE(JSON_UNQUOTE(JSON_EXTRACT(notification.payload, '$.targetRoute')), CONCAT('/dashboard/contracts/', contract.lease_contract_id)))
WHERE property.property_id = @hdd1_property_id
  AND notification.target_type = 'CONTRACT'
  AND notification.event_type = 'CONTRACT_EXPIRING_SOON_REVIEW'
  AND notification.channel IN ('WEB', 'PUSH');

DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_notification_templates;
