CREATE TABLE IF NOT EXISTS hdbhms.contract_occupant_intentions
(
    contract_occupant_intention_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    contract_id                    BIGINT UNSIGNED NOT NULL,
    contract_occupant_id           BIGINT UNSIGNED NOT NULL,
    tenant_profile_id              BIGINT UNSIGNED NOT NULL,
    intention                      ENUM ('FOLLOW_PRIMARY_MOVE_OUT', 'JOIN_RENEWAL') NOT NULL,
    note                           TEXT NULL,
    recorded_at                    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_at                     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at                     DATETIME(6) NULL ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_contract_occupant_intention UNIQUE (contract_id, contract_occupant_id),
    CONSTRAINT fk_coi_contract FOREIGN KEY (contract_id)
        REFERENCES hdbhms.lease_contracts (lease_contract_id),
    CONSTRAINT fk_coi_occupant FOREIGN KEY (contract_occupant_id)
        REFERENCES hdbhms.contract_occupants (contract_occupant_id),
    CONSTRAINT fk_coi_profile FOREIGN KEY (tenant_profile_id)
        REFERENCES hdbhms.person_profiles (person_profile_id),
    INDEX idx_coi_contract_intention (contract_id, intention),
    INDEX idx_coi_profile (tenant_profile_id)
);
