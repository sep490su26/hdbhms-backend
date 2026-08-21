SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- Repair databases that already ran the broad expiry seed updates. Contract
-- 301 was liquidated before the current July contract was seeded and must not
-- remain in the active expiry workflow.
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

UPDATE hdbhms.lease_contracts contract
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
JOIN hdbhms.contract_liquidations liquidation
  ON liquidation.contract_id = contract.lease_contract_id
 AND liquidation.status = 'CONFIRMED'
JOIN hdbhms.person_profiles historical_profile
  ON historical_profile.email = 'nguyenvanminh01@gmail.com'
 AND historical_profile.deleted_at IS NULL
SET contract.primary_tenant_profile_id = historical_profile.person_profile_id,
    contract.status = 'LIQUIDATED',
    contract.end_date = liquidation.liquidation_date,
    contract.tenant_intention = 'MOVE_OUT',
    contract.expected_vacant_date = liquidation.liquidation_date,
    contract.intention_recorded_at = '2026-07-20 08:00:00',
    contract.updated_at = @hdd1_seed_now
WHERE room.property_id = @hdd1_property_id
  AND contract.contract_code = 'HDT_P301_01_01_2026'
  AND contract.deleted_at IS NULL;

UPDATE hdbhms.contract_occupants occupant
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = occupant.contract_id
JOIN hdbhms.contract_liquidations liquidation
  ON liquidation.contract_id = contract.lease_contract_id
 AND liquidation.status = 'CONFIRMED'
SET occupant.status = 'MOVED_OUT',
    occupant.move_out_date = liquidation.liquidation_date,
    occupant.disabled_reason = 'Historical contract liquidation was already confirmed.',
    occupant.disabled_by = @hdd1_manager_id,
    occupant.disabled_at = @hdd1_seed_now
WHERE contract.contract_code = 'HDT_P301_01_01_2026'
  AND occupant.status = 'ACTIVE';

-- Remove expiry notifications and trackers that were created while the
-- historical contract had the wrong EXPIRING_SOON status.
DELETE delivery
FROM hdbhms.notification_deliveries delivery
JOIN hdbhms.notification_outbox notification
  ON notification.notification_outbox_id = delivery.outbox_id
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = notification.target_id
WHERE notification.target_type = 'CONTRACT'
  AND notification.target_id = contract.lease_contract_id
  AND contract.contract_code = 'HDT_P301_01_01_2026'
  AND notification.event_type IN (
      'LEASE_EXPIRY_REMINDER_FIRST',
      'LEASE_EXPIRY_REMINDER_SECOND',
      'LEASE_EXPIRY_REMINDER_FINAL',
      'CONTRACT_EXPIRING_SOON_REVIEW'
  );

DELETE notification
FROM hdbhms.notification_outbox notification
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = notification.target_id
WHERE notification.target_type = 'CONTRACT'
  AND contract.contract_code = 'HDT_P301_01_01_2026'
  AND notification.event_type IN (
      'LEASE_EXPIRY_REMINDER_FIRST',
      'LEASE_EXPIRY_REMINDER_SECOND',
      'LEASE_EXPIRY_REMINDER_FINAL',
      'CONTRACT_EXPIRING_SOON_REVIEW'
  );

DELETE tracker
FROM hdbhms.reminder_trackers tracker
JOIN hdbhms.lease_contracts contract
  ON contract.lease_contract_id = tracker.target_id
WHERE tracker.target_type = 'CONTRACT'
  AND contract.contract_code = 'HDT_P301_01_01_2026'
  AND tracker.reminder_key = 'LEASE_EXPIRY_INTENTION';

-- Recompute Khai's account links after the historical contract is removed
-- from his current-contract set.
UPDATE hdbhms.tenant_account_provisionings provisioning
JOIN hdbhms.person_profiles profile
  ON profile.person_profile_id = provisioning.tenant_profile_id
SET provisioning.first_contract_id = (
        SELECT MIN(contract.lease_contract_id)
        FROM hdbhms.lease_contracts contract
        JOIN hdbhms.rooms room
          ON room.room_id = contract.room_id
        WHERE contract.primary_tenant_profile_id = profile.person_profile_id
          AND room.property_id = @hdd1_property_id
          AND contract.deleted_at IS NULL
          AND contract.status IN ('ACTIVE', 'EXPIRING_SOON', 'SIGNED', 'CONFIRMED')
    ),
    provisioning.latest_contract_id = (
        SELECT MAX(contract.lease_contract_id)
        FROM hdbhms.lease_contracts contract
        JOIN hdbhms.rooms room
          ON room.room_id = contract.room_id
        WHERE contract.primary_tenant_profile_id = profile.person_profile_id
          AND room.property_id = @hdd1_property_id
          AND contract.deleted_at IS NULL
          AND contract.status IN ('ACTIVE', 'EXPIRING_SOON', 'SIGNED', 'CONFIRMED')
    ),
    provisioning.updated_at = @hdd1_seed_now
WHERE profile.email = 'nguyenvankhai95@gmail.com';

UPDATE hdbhms.rooms room
JOIN hdbhms.lease_contracts current_contract
  ON current_contract.room_id = room.room_id
 AND current_contract.contract_code = 'HDT_P301_01_07_2026'
 AND current_contract.deleted_at IS NULL
SET room.current_status = CASE
        WHEN current_contract.status = 'EXPIRING_SOON' THEN 'SOON_VACANT'
        ELSE 'OCCUPIED'
    END,
    room.updated_at = @hdd1_seed_now
WHERE room.property_id = @hdd1_property_id
  AND room.room_code = '301'
  AND room.deleted_at IS NULL;
