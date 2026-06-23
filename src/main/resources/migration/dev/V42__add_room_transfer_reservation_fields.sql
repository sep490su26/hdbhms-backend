ALTER TABLE room_transfer_requests
    ADD COLUMN reserved_slots INT NULL AFTER reason,
    ADD COLUMN reservation_expires_at DATETIME(6) NULL AFTER reserved_slots;

CREATE INDEX idx_transfer_target_reservation
    ON room_transfer_requests (target_room_id, status, reservation_expires_at);

ALTER TABLE rooms
    MODIFY COLUMN current_status ENUM(
        'VACANT',
        'RESERVED',
        'RESERVED_FOR_TRANSFER',
        'ON_HOLD',
        'OCCUPIED',
        'SOON_VACANT',
        'MAINTENANCE',
        'EXPIRED'
    ) NOT NULL DEFAULT 'VACANT';

# ALTER TABLE room_status_snapshots
#     MODIFY COLUMN room_status ENUM(
#         'VACANT',
#         'RESERVED',
#         'RESERVED_FOR_TRANSFER',
#         'ON_HOLD',
#         'OCCUPIED',
#         'SOON_VACANT',
#         'MAINTENANCE',
#         'EXPIRED'
#     ) NOT NULL;

# ALTER TABLE room_status_history
#     MODIFY COLUMN from_status ENUM(
#         'VACANT',
#         'RESERVED',
#         'RESERVED_FOR_TRANSFER',
#         'ON_HOLD',
#         'OCCUPIED',
#         'SOON_VACANT',
#         'MAINTENANCE',
#         'EXPIRED'
#     ) NULL,
#     MODIFY COLUMN to_status ENUM(
#         'VACANT',
#         'RESERVED',
#         'RESERVED_FOR_TRANSFER',
#         'ON_HOLD',
#         'OCCUPIED',
#         'SOON_VACANT',
#         'MAINTENANCE',
#         'EXPIRED'
#     ) NOT NULL;
