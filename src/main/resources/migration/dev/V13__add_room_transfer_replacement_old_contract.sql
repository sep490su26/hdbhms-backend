ALTER TABLE hdbhms.room_transfer_requests
    ADD COLUMN replacement_old_contract_id BIGINT UNSIGNED NULL AFTER new_contract_id,
    ADD CONSTRAINT fk_tr_replacement_old_contract
        FOREIGN KEY (replacement_old_contract_id) REFERENCES hdbhms.lease_contracts (lease_contract_id);
