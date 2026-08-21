SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- Backfill the two additional high-value rooms for databases that already
-- applied V88. V48 and V88 own the same final state for fresh databases.
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

DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_high_value_release_rooms;
CREATE TEMPORARY TABLE tmp_hdd1_high_value_release_rooms
(
    room_code VARCHAR(10) NOT NULL PRIMARY KEY
);

INSERT INTO tmp_hdd1_high_value_release_rooms (room_code)
VALUES ('101'), ('102');

UPDATE hdbhms.lease_contracts contract
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
JOIN tmp_hdd1_high_value_release_rooms selected_room
  ON selected_room.room_code = room.room_code
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
JOIN tmp_hdd1_high_value_release_rooms selected_room
  ON selected_room.room_code = room.room_code
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
           'source', 'V89_SEED_AUGUST_HIGH_VALUE_ROOM_RELEASE',
           'releasedAt', DATE_FORMAT(@hdd1_seed_now, '%Y-%m-%dT%H:%i:%s'),
           'roomCode', room.room_code
       ) AS BINARY),
       @hdd1_manager_id,
       @hdd1_seed_now
FROM hdbhms.lease_contracts contract
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
JOIN tmp_hdd1_high_value_release_rooms selected_room
  ON selected_room.room_code = room.room_code
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
JOIN tmp_hdd1_high_value_release_rooms selected_room
  ON selected_room.room_code = room.room_code
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
JOIN tmp_hdd1_high_value_release_rooms selected_room
  ON selected_room.room_code = room.room_code
SET room.current_status = 'VACANT',
    room.public_note = 'Vacant from August 2026 seed release.',
    room.internal_note = 'Seed V89: high-value room released after contract auto-termination.',
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

DELETE delivery
FROM hdbhms.notification_deliveries delivery
JOIN hdbhms.notification_outbox notification
  ON notification.notification_outbox_id = delivery.outbox_id
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = notification.target_id
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
JOIN tmp_hdd1_high_value_release_rooms selected_room
  ON selected_room.room_code = room.room_code
WHERE notification.target_type = 'CONTRACT'
  AND (
      notification.event_type LIKE 'LEASE_EXPIRY%'
      OR notification.event_type = 'CONTRACT_EXPIRING_SOON_REVIEW'
  );

DELETE notification
FROM hdbhms.notification_outbox notification
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = notification.target_id
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
JOIN tmp_hdd1_high_value_release_rooms selected_room
  ON selected_room.room_code = room.room_code
WHERE notification.target_type = 'CONTRACT'
  AND (
      notification.event_type LIKE 'LEASE_EXPIRY%'
      OR notification.event_type = 'CONTRACT_EXPIRING_SOON_REVIEW'
  );

DELETE tracker
FROM hdbhms.reminder_trackers tracker
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = tracker.target_id
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
JOIN tmp_hdd1_high_value_release_rooms selected_room
  ON selected_room.room_code = room.room_code
WHERE tracker.target_type = 'CONTRACT'
  AND tracker.reminder_key LIKE 'LEASE_EXPIRY%';

DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_high_value_release_rooms;
