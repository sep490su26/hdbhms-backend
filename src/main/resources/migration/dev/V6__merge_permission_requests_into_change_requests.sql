ALTER TABLE hdbhms.change_requests
    MODIFY COLUMN request_type ENUM (
        'METER_READING_CORRECTION',
        'INVOICE_ADJUSTMENT',
        'RENT_PRICE_ADJUSTMENT',
        'DEPOSIT_REFUND_REQUEST',
        'ROOM_TRANSFER',
        'MOVE_OUT',
        'COMPLAINT',
        'PERMISSION_ACCESS',
        'TENANT_PROFILE_ACCESS'
    ) NOT NULL;

ALTER TABLE hdbhms.change_requests
    MODIFY COLUMN requester_role ENUM (
        'LEAD',
        'TENANT',
        'MANAGER',
        'ACCOUNTANT',
        'OWNER'
    ) NOT NULL;

ALTER TABLE hdbhms.change_requests
    MODIFY COLUMN target_type ENUM (
        'METER_READING',
        'INVOICE',
        'CONTRACT',
        'DEPOSIT',
        'TENANT_PROFILE',
        'IDENTITY_DOCUMENT',
        'REPORT',
        'FILE',
        'OTHER'
    ) NOT NULL;
