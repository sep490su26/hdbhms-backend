ALTER TABLE hdbhms.change_requests
    MODIFY COLUMN request_type ENUM (
        'METER_READING_CORRECTION',
        'INVOICE_ADJUSTMENT',
        'RENT_PRICE_ADJUSTMENT',
        'DEPOSIT_REFUND_REQUEST',
        'ROOM_TRANSFER',
        'MOVE_OUT',
        'COMPLAINT',
        'TENANT_PROFILE_ACCESS'
    ) NOT NULL;

ALTER TABLE hdbhms.change_requests
    MODIFY COLUMN target_type ENUM (
        'METER_READING',
        'INVOICE',
        'CONTRACT',
        'DEPOSIT',
        'TENANT_PROFILE',
        'OTHER'
    ) NOT NULL;
