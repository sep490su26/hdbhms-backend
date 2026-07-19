CREATE TABLE IF NOT EXISTS hdbhms.deposit_transfer_records
(
    deposit_transfer_record_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    transfer_request_id        BIGINT UNSIGNED                          NOT NULL,
    old_contract_id            BIGINT UNSIGNED                          NOT NULL,
    new_contract_id            BIGINT UNSIGNED                          NOT NULL,
    old_deposit_agreement_id   BIGINT UNSIGNED                          NULL,
    from_room_id               BIGINT UNSIGNED                          NOT NULL,
    to_room_id                 BIGINT UNSIGNED                          NOT NULL,
    amount                     BIGINT                                   NOT NULL DEFAULT 0,
    status                     ENUM ('DRAFT', 'EFFECTIVE', 'CANCELLED') NOT NULL DEFAULT 'DRAFT',
    effective_date             DATE                                     NULL,
    cancelled_at               DATETIME(6)                              NULL,
    note                       TEXT                                     NULL,
    created_at                 DATETIME(6)                              NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at                 DATETIME(6)                              NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_deposit_transfer_request
        UNIQUE (transfer_request_id),
    CONSTRAINT uq_deposit_transfer_new_contract
        UNIQUE (new_contract_id),
    CONSTRAINT fk_deposit_transfer_request
        FOREIGN KEY (transfer_request_id) REFERENCES hdbhms.room_transfer_requests (room_transfer_request_id),
    CONSTRAINT fk_deposit_transfer_old_contract
        FOREIGN KEY (old_contract_id) REFERENCES hdbhms.lease_contracts (lease_contract_id),
    CONSTRAINT fk_deposit_transfer_new_contract
        FOREIGN KEY (new_contract_id) REFERENCES hdbhms.lease_contracts (lease_contract_id),
    CONSTRAINT fk_deposit_transfer_old_deposit
        FOREIGN KEY (old_deposit_agreement_id) REFERENCES hdbhms.deposit_agreements (deposit_agreement_id),
    CONSTRAINT fk_deposit_transfer_from_room
        FOREIGN KEY (from_room_id) REFERENCES hdbhms.rooms (room_id),
    CONSTRAINT fk_deposit_transfer_to_room
        FOREIGN KEY (to_room_id) REFERENCES hdbhms.rooms (room_id)
);

CREATE INDEX idx_deposit_transfer_old_contract
    ON hdbhms.deposit_transfer_records (old_contract_id);

CREATE INDEX idx_deposit_transfer_status
    ON hdbhms.deposit_transfer_records (status);
