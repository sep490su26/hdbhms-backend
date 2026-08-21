ALTER TABLE hdbhms.users
    MODIFY COLUMN status ENUM (
        'PENDING_CONTRACT',
        'ACTIVE',
        'DORMANT',
        'INACTIVE',
        'REJECTED',
        'CLOSED',
        'ARCHIVED'
    ) DEFAULT 'PENDING_CONTRACT' NOT NULL;
