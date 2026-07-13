ALTER TABLE hdbhms.properties
    MODIFY COLUMN status ENUM ('DRAFT', 'ACTIVE', 'TEMP_CLOSED', 'CLOSED') DEFAULT 'DRAFT' NOT NULL;

ALTER TABLE hdbhms.rooms
    MODIFY COLUMN current_status ENUM (
        'DRAFT',
        'VACANT',
        'RESERVED',
        'RESERVED_FOR_TRANSFER',
        'ON_HOLD',
        'OCCUPIED',
        'SOON_VACANT',
        'MAINTENANCE',
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
        'MAINTENANCE',
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
        'MAINTENANCE',
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
        'MAINTENANCE',
        'EXPIRED'
    ) NOT NULL;
