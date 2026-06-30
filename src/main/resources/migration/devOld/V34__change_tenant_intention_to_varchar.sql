-- Fix Hibernate schema validation: lease_contracts.tenant_intention
-- Hibernate expects VARCHAR(50) for @Enumerated(EnumType.STRING) + @Column(length=50)
-- but the column was created as ENUM by V33.
ALTER TABLE lease_contracts
    MODIFY COLUMN tenant_intention VARCHAR(50) NULL;
