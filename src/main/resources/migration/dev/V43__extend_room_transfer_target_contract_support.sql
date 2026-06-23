UPDATE room_transfer_requests
SET status = CASE status
    WHEN 'WAITING_NEW_CONTRACT' THEN 'WAITING_NEW_CONTRACT'
    WHEN 'WAITING_CONTRACT_CONFIRMATION' THEN 'WAITING_CONTRACT_CONFIRMATION'
    WHEN 'WAITING_SIGNING' THEN 'WAITING_SIGNING'
    WHEN 'WAITING_EXECUTION' THEN 'WAITING_EXECUTION'
    WHEN 'EXECUTED' THEN 'EXECUTED'
    ELSE status
END;

ALTER TABLE room_transfer_requests
    MODIFY COLUMN status ENUM (
        'WAITING_APPROVAL',
        'CANCELLED',
        'REJECTED',
        'WAITING_NEW_CONTRACT',
        'WAITING_TARGET_HOLDER_APPROVAL',
        'WAITING_CONTRACT_CONFIRMATION',
        'WAITING_SIGNING',
        'WAITING_EXECUTION',
        'EXECUTED'
        ) NOT NULL DEFAULT 'WAITING_APPROVAL';

ALTER TABLE room_transfer_requests
    ADD COLUMN target_contract_id BIGINT UNSIGNED NULL AFTER target_transfer_type,
    ADD COLUMN target_holder_approved_by BIGINT UNSIGNED NULL AFTER reservation_expires_at,
    ADD COLUMN target_holder_approved_at DATETIME(6) NULL AFTER target_holder_approved_by,
    ADD COLUMN target_holder_rejected_at DATETIME(6) NULL AFTER target_holder_approved_at;

ALTER TABLE room_transfer_requests
    ADD CONSTRAINT fk_tr_target_contract
        FOREIGN KEY (target_contract_id) REFERENCES lease_contracts (id),
    ADD CONSTRAINT fk_tr_target_holder_approved_by
        FOREIGN KEY (target_holder_approved_by) REFERENCES users (id);
