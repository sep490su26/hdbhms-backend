SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- Remove duplicate management reminders left by earlier seed overlays and
-- keep one deterministic history row per contract and recipient.
SET @hdd1_property_id := (
    SELECT property_id
    FROM hdbhms.properties
    WHERE property_code = 'HAI_DANG_1'
      AND deleted_at IS NULL
    LIMIT 1
);

DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_duplicate_review_notifications;
CREATE TEMPORARY TABLE tmp_hdd1_duplicate_review_notifications
(
    notification_outbox_id BIGINT UNSIGNED NOT NULL PRIMARY KEY
);

INSERT INTO tmp_hdd1_duplicate_review_notifications (notification_outbox_id)
SELECT notification.notification_outbox_id
FROM hdbhms.notification_outbox notification
JOIN hdbhms.lease_contracts contract
  ON notification.target_type = 'CONTRACT'
 AND notification.target_id = contract.lease_contract_id
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
JOIN (
    SELECT target_id, recipient_user_id, MIN(notification_outbox_id) AS keep_id
    FROM hdbhms.notification_outbox
    WHERE event_type = 'CONTRACT_EXPIRING_SOON_REVIEW'
      AND target_type = 'CONTRACT'
    GROUP BY target_id, recipient_user_id
    HAVING COUNT(*) > 1
) duplicate_group
  ON duplicate_group.target_id = notification.target_id
 AND duplicate_group.recipient_user_id = notification.recipient_user_id
WHERE notification.event_type = 'CONTRACT_EXPIRING_SOON_REVIEW'
  AND room.property_id = @hdd1_property_id
  AND notification.notification_outbox_id <> duplicate_group.keep_id;

DELETE delivery
FROM hdbhms.notification_deliveries delivery
JOIN tmp_hdd1_duplicate_review_notifications duplicate_notification
  ON duplicate_notification.notification_outbox_id = delivery.outbox_id;

DELETE notification
FROM hdbhms.notification_outbox notification
JOIN tmp_hdd1_duplicate_review_notifications duplicate_notification
  ON duplicate_notification.notification_outbox_id = notification.notification_outbox_id;

-- A delivery row represents the time the recipient received the notification.
-- Align its audit creation time when older seed data recorded it a few
-- microseconds later than delivered_at.
UPDATE hdbhms.notification_deliveries delivery
JOIN hdbhms.notification_outbox notification
  ON notification.notification_outbox_id = delivery.outbox_id
LEFT JOIN hdbhms.lease_contracts contract
  ON notification.target_type = 'CONTRACT'
 AND notification.target_id = contract.lease_contract_id
LEFT JOIN hdbhms.rooms contract_room
  ON contract_room.room_id = contract.room_id
LEFT JOIN hdbhms.invoices invoice
  ON notification.target_type = 'INVOICE'
 AND notification.target_id = invoice.invoice_id
LEFT JOIN hdbhms.change_requests request
  ON notification.target_type = 'CHANGE_REQUEST'
 AND notification.target_id = request.change_request_id
LEFT JOIN hdbhms.lease_contracts request_contract
  ON request.target_type = 'CONTRACT'
 AND request.target_id = request_contract.lease_contract_id
LEFT JOIN hdbhms.rooms request_contract_room
  ON request_contract_room.room_id = request_contract.room_id
LEFT JOIN hdbhms.invoices request_invoice
  ON request.target_type = 'INVOICE'
 AND request.target_id = request_invoice.invoice_id
SET delivery.created_at = delivery.delivered_at
WHERE delivery.delivered_at IS NOT NULL
  AND delivery.created_at > delivery.delivered_at
  AND (
         contract_room.property_id = @hdd1_property_id
      OR invoice.property_id = @hdd1_property_id
      OR request_contract_room.property_id = @hdd1_property_id
      OR request_invoice.property_id = @hdd1_property_id
  );

DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_duplicate_review_notifications;
