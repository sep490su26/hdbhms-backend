ALTER TABLE contract_occupants
    DROP FOREIGN KEY fk_co_tenant;

ALTER TABLE contract_occupants
    MODIFY tenant_id BIGINT UNSIGNED NULL;

ALTER TABLE contract_occupants
    ADD COLUMN tenant_profile_id BIGINT UNSIGNED NULL AFTER tenant_id;

UPDATE contract_occupants co
JOIN tenants t ON t.id = co.tenant_id
JOIN person_profiles pp ON pp.user_id = t.user_id AND pp.deleted_at IS NULL
SET co.tenant_profile_id = pp.id
WHERE co.tenant_profile_id IS NULL;

ALTER TABLE contract_occupants
    DROP INDEX uq_contract_occupant,
    DROP INDEX idx_occupant_profile_status,
    ADD UNIQUE KEY uq_contract_occupant_profile (contract_id, tenant_profile_id),
    ADD KEY idx_occupant_profile_status (tenant_profile_id, status),
    ADD CONSTRAINT fk_co_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    ADD CONSTRAINT fk_co_tenant_profile
        FOREIGN KEY (tenant_profile_id) REFERENCES person_profiles (id);

DROP TRIGGER IF EXISTS trg_contract_occupants_max_three_before_insert;

DELIMITER $$
CREATE TRIGGER trg_contract_occupants_max_three_before_insert
    BEFORE INSERT
    ON contract_occupants
    FOR EACH ROW
BEGIN
    IF NEW.status = 'ACTIVE' THEN
        IF (SELECT COUNT(*)
            FROM contract_occupants
            WHERE contract_id = NEW.contract_id
              AND status = 'ACTIVE') >= 3 THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'A room contract can have at most 3 active occupants';
        END IF;
    END IF;
END$$
DELIMITER ;
