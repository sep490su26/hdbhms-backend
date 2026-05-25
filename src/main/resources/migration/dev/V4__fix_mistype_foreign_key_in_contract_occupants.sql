ALTER TABLE contract_occupants DROP FOREIGN KEY fk_co_tenant;

-- Add the correct FK
ALTER TABLE contract_occupants
    ADD CONSTRAINT fk_co_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants (id);