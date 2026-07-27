ALTER TABLE hdbhms.users
    MODIFY COLUMN status ENUM (
        'PENDING_CONTRACT',
        'ACTIVE',
        'INACTIVE',
        'DISABLED',
        'REJECTED',
        'CLOSED',
        'ARCHIVED'
    ) DEFAULT 'PENDING_CONTRACT' NOT NULL;

UPDATE hdbhms.users
SET status = 'INACTIVE'
WHERE status = 'DISABLED';

ALTER TABLE hdbhms.users
    MODIFY COLUMN status ENUM (
        'PENDING_CONTRACT',
        'ACTIVE',
        'INACTIVE',
        'REJECTED',
        'CLOSED',
        'ARCHIVED'
    ) DEFAULT 'PENDING_CONTRACT' NOT NULL;
