ALTER TABLE deposit_agreements
    ADD COLUMN room_hold_id BIGINT UNSIGNED NULL AFTER room_id,
    ADD INDEX idx_dep_agreement_hold (room_hold_id),
    ADD CONSTRAINT fk_dep_agreement_hold
        FOREIGN KEY (room_hold_id) REFERENCES room_holds (id);
ALTER TABLE scheduled_tasks
    MODIFY COLUMN task_type ENUM (
        'INVOICE_REMINDER','DEBT_WARNING','CONTRACT_EXPIRY',
        'ROOM_STATUS_AUTOMATION','MAINTENANCE_FOLLOWUP','OTHER',
        'ROOM_HOLD_EXPIRATION'
        ) NOT NULL;