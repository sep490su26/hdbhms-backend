UPDATE room_transfer_requests
SET status = CASE status
    WHEN 'PENDING' THEN 'WAITING_APPROVAL'
    WHEN 'APPROVED' THEN 'WAITING_NEW_CONTRACT'
    WHEN 'OLD_ROOM_HANDOVER' THEN 'WAITING_EXECUTION'
    WHEN 'SETTLEMENT_PENDING' THEN 'WAITING_EXECUTION'
    WHEN 'NEW_CONTRACT_CREATED' THEN 'WAITING_CONTRACT_CONFIRMATION'
    WHEN 'COMPLETED' THEN 'EXECUTED'
    ELSE status
END;

ALTER TABLE room_transfer_requests
    MODIFY COLUMN status ENUM (
        'WAITING_APPROVAL',
        'CANCELLED',
        'REJECTED',
        'WAITING_NEW_CONTRACT',
        'WAITING_CONTRACT_CONFIRMATION',
        'WAITING_SIGNING',
        'WAITING_EXECUTION',
        'EXECUTED'
        ) NOT NULL DEFAULT 'WAITING_APPROVAL';

ALTER TABLE room_transfer_requests
    ADD COLUMN transferring_tenant_profile_ids JSON NULL AFTER target_room_id,
    ADD COLUMN nominated_holder_profile_id BIGINT UNSIGNED NULL AFTER transferring_tenant_profile_ids,
    ADD COLUMN target_transfer_type VARCHAR(50) NULL AFTER nominated_holder_profile_id;

ALTER TABLE room_transfer_requests
    ADD CONSTRAINT fk_tr_nominated_holder
        FOREIGN KEY (nominated_holder_profile_id) REFERENCES person_profiles (id);

ALTER TABLE lease_contracts
    MODIFY COLUMN status ENUM (
        'DRAFT',
        'CONFIRMED',
        'SIGNED',
        'PENDING_SIGNATURE',
        'ACTIVE',
        'EXPIRING_SOON',
        'EXPIRED',
        'TERMINATION_PENDING',
        'LIQUIDATED',
        'RENEWED',
        'AUTO_TERMINATED',
        'CANCELLED',
        'TRANSFERRED'
        ) NOT NULL DEFAULT 'DRAFT';
