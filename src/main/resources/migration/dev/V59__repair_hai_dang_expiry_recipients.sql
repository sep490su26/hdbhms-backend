SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- V48/V51 could create expiry rows for the old demo profile before the
-- workbook occupants were linked to their final tenant accounts. Reconcile
-- those rows so each reminder belongs to the current primary tenant.
SET @hdd1_property_id := (
    SELECT property_id
    FROM hdbhms.properties
    WHERE property_code = 'HAI_DANG_1'
      AND deleted_at IS NULL
    LIMIT 1
);
SET @hdd1_seed_now := '2026-08-01 09:00:00';

DELETE delivery
FROM hdbhms.notification_deliveries delivery
JOIN hdbhms.notification_outbox notification
  ON notification.notification_outbox_id = delivery.outbox_id
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = notification.target_id
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
JOIN hdbhms.person_profiles profile
  ON profile.person_profile_id = contract.primary_tenant_profile_id
JOIN hdbhms.users tenant_user
  ON tenant_user.user_id = profile.user_id
WHERE notification.event_type IN (
      'LEASE_EXPIRY_REMINDER_FIRST',
      'LEASE_EXPIRY_REMINDER_SECOND',
      'LEASE_EXPIRY_REMINDER_FINAL'
  )
  AND notification.target_type = 'CONTRACT'
  AND notification.recipient_user_id <> tenant_user.user_id
  AND room.property_id = @hdd1_property_id
  AND contract.status = 'EXPIRING_SOON'
  AND contract.end_date >= DATE(@hdd1_seed_now);

DELETE notification
FROM hdbhms.notification_outbox notification
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = notification.target_id
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
JOIN hdbhms.person_profiles profile
  ON profile.person_profile_id = contract.primary_tenant_profile_id
JOIN hdbhms.users tenant_user
  ON tenant_user.user_id = profile.user_id
WHERE notification.event_type IN (
      'LEASE_EXPIRY_REMINDER_FIRST',
      'LEASE_EXPIRY_REMINDER_SECOND',
      'LEASE_EXPIRY_REMINDER_FINAL'
  )
  AND notification.target_type = 'CONTRACT'
  AND notification.recipient_user_id <> tenant_user.user_id
  AND room.property_id = @hdd1_property_id
  AND contract.status = 'EXPIRING_SOON'
  AND contract.end_date >= DATE(@hdd1_seed_now);

UPDATE hdbhms.reminder_trackers tracker
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = tracker.target_id
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
JOIN hdbhms.person_profiles profile
  ON profile.person_profile_id = contract.primary_tenant_profile_id
JOIN hdbhms.users tenant_user
  ON tenant_user.user_id = profile.user_id
SET tracker.recipient_user_id = tenant_user.user_id,
    tracker.metadata = JSON_SET(
        COALESCE(tracker.metadata, JSON_OBJECT()),
        '$.contractCode', contract.contract_code,
        '$.roomCode', room.room_code,
        '$.endDate', contract.end_date
    ),
    tracker.updated_at = @hdd1_seed_now
WHERE tracker.reminder_key = 'LEASE_EXPIRY_INTENTION'
  AND tracker.target_type = 'CONTRACT'
  AND tracker.audience = 'PRIMARY_TENANT'
  AND room.property_id = @hdd1_property_id
  AND contract.status = 'EXPIRING_SOON'
  AND contract.end_date >= DATE(@hdd1_seed_now);
