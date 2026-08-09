-- Room maintenance is tracked by maintenance tickets, not by room status.

UPDATE hdbhms.rooms room
LEFT JOIN (
    SELECT DISTINCT contract.room_id
    FROM hdbhms.lease_contracts contract
    WHERE contract.deleted_at IS NULL
      AND contract.status IN ('ACTIVE', 'EXPIRING_SOON', 'TERMINATION_PENDING')
) active_contract ON active_contract.room_id = room.room_id
SET room.current_status = IF(active_contract.room_id IS NULL, 'VACANT', 'OCCUPIED')
WHERE room.current_status = 'MAINTENANCE';

UPDATE hdbhms.room_status_history
SET from_status = 'VACANT'
WHERE from_status = 'MAINTENANCE';

UPDATE hdbhms.room_status_history
SET to_status = 'VACANT'
WHERE to_status = 'MAINTENANCE';

DELETE FROM hdbhms.room_status_display_configs
WHERE room_status = 'MAINTENANCE';

ALTER TABLE hdbhms.rooms
    MODIFY COLUMN current_status ENUM (
        'DRAFT',
        'VACANT',
        'RESERVED',
        'RESERVED_FOR_TRANSFER',
        'ON_HOLD',
        'OCCUPIED',
        'SOON_VACANT',
        'EXPIRED'
    ) DEFAULT 'DRAFT' NOT NULL;

ALTER TABLE hdbhms.room_status_history
    MODIFY COLUMN from_status ENUM (
        'DRAFT',
        'VACANT',
        'RESERVED',
        'RESERVED_FOR_TRANSFER',
        'ON_HOLD',
        'OCCUPIED',
        'SOON_VACANT',
        'EXPIRED'
    ) NULL,
    MODIFY COLUMN to_status ENUM (
        'DRAFT',
        'VACANT',
        'RESERVED',
        'RESERVED_FOR_TRANSFER',
        'ON_HOLD',
        'OCCUPIED',
        'SOON_VACANT',
        'EXPIRED'
    ) NOT NULL;

ALTER TABLE hdbhms.room_status_display_configs
    MODIFY COLUMN room_status ENUM (
        'DRAFT',
        'VACANT',
        'RESERVED',
        'RESERVED_FOR_TRANSFER',
        'ON_HOLD',
        'OCCUPIED',
        'SOON_VACANT',
        'EXPIRED'
    ) NOT NULL;
