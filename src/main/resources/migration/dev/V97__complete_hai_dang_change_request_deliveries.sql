SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- Change-request notifications are part of the same Hai Dang demo scope as
-- contract and invoice notifications. Backfill their receive timestamps so
-- SENT rows do not appear to have disappeared before reaching the recipient.
SET @hdd1_property_id := (
    SELECT property_id
    FROM hdbhms.properties
    WHERE property_code = 'HAI_DANG_1'
      AND deleted_at IS NULL
    LIMIT 1
);

INSERT INTO hdbhms.notification_deliveries
    (outbox_id, provider_message_id, delivery_status, delivered_at, read_at, created_at)
SELECT notification.notification_outbox_id,
       CONCAT('seed-delivery-', notification.notification_outbox_id),
       'SENT',
       DATE_ADD(notification.sent_at, INTERVAL 2 MINUTE),
       CASE
           WHEN notification.is_read = TRUE
               THEN COALESCE(notification.read_at, DATE_ADD(notification.sent_at, INTERVAL 7 MINUTE))
           ELSE NULL
       END,
       DATE_ADD(notification.sent_at, INTERVAL 2 MINUTE)
FROM hdbhms.notification_outbox notification
JOIN hdbhms.change_requests request
  ON notification.target_type = 'CHANGE_REQUEST'
 AND notification.target_id = request.change_request_id
LEFT JOIN hdbhms.lease_contracts contract
  ON request.target_type = 'CONTRACT'
 AND request.target_id = contract.lease_contract_id
LEFT JOIN hdbhms.rooms contract_room
  ON contract_room.room_id = contract.room_id
LEFT JOIN hdbhms.invoices invoice
  ON request.target_type = 'INVOICE'
 AND request.target_id = invoice.invoice_id
LEFT JOIN hdbhms.notification_deliveries existing_delivery
  ON existing_delivery.outbox_id = notification.notification_outbox_id
WHERE existing_delivery.notification_delivery_id IS NULL
  AND notification.status = 'SENT'
  AND notification.sent_at IS NOT NULL
  AND (
         contract_room.property_id = @hdd1_property_id
      OR invoice.property_id = @hdd1_property_id
  );

UPDATE hdbhms.notification_deliveries delivery
JOIN hdbhms.notification_outbox notification
  ON notification.notification_outbox_id = delivery.outbox_id
JOIN hdbhms.change_requests request
  ON notification.target_type = 'CHANGE_REQUEST'
 AND notification.target_id = request.change_request_id
LEFT JOIN hdbhms.lease_contracts contract
  ON request.target_type = 'CONTRACT'
 AND request.target_id = contract.lease_contract_id
LEFT JOIN hdbhms.rooms contract_room
  ON contract_room.room_id = contract.room_id
LEFT JOIN hdbhms.invoices invoice
  ON request.target_type = 'INVOICE'
 AND request.target_id = invoice.invoice_id
SET delivery.delivered_at = CASE
        WHEN delivery.delivered_at IS NULL OR delivery.delivered_at < notification.sent_at
            THEN DATE_ADD(notification.sent_at, INTERVAL 2 MINUTE)
        ELSE delivery.delivered_at
    END,
    delivery.created_at = CASE
        WHEN delivery.created_at < notification.sent_at
            THEN DATE_ADD(notification.sent_at, INTERVAL 2 MINUTE)
        ELSE delivery.created_at
    END,
    delivery.read_at = CASE
        WHEN delivery.read_at IS NOT NULL AND delivery.read_at < notification.sent_at
            THEN DATE_ADD(notification.sent_at, INTERVAL 7 MINUTE)
        ELSE delivery.read_at
    END
WHERE notification.status = 'SENT'
  AND notification.sent_at IS NOT NULL
  AND (
         contract_room.property_id = @hdd1_property_id
      OR invoice.property_id = @hdd1_property_id
  );
