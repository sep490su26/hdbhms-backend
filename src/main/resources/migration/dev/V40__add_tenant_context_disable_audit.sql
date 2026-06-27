ALTER TABLE contract_occupants
    MODIFY status ENUM ('ACTIVE','MOVED_OUT','DISABLED') NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN disabled_reason TEXT NULL AFTER status,
    ADD COLUMN disabled_by BIGINT UNSIGNED NULL AFTER disabled_reason,
    ADD COLUMN disabled_at DATETIME(6) NULL AFTER disabled_by,
    ADD KEY idx_contract_occupants_disabled_by (disabled_by),
    ADD CONSTRAINT fk_contract_occupants_disabled_by
        FOREIGN KEY (disabled_by) REFERENCES users (id);
