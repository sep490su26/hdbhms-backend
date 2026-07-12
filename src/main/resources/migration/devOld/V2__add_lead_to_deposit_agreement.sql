ALTER TABLE deposit_agreements
    ADD COLUMN lead_id BIGINT UNSIGNED NULL AFTER tenant_id;

ALTER TABLE deposit_agreements
    ADD CONSTRAINT fk_dep_agreement_lead
        FOREIGN KEY (lead_id) REFERENCES leads (id);

ALTER TABLE deposit_agreements
    ADD INDEX idx_deposit_lead (lead_id, status);

ALTER TABLE deposit_agreements
    ADD CONSTRAINT chk_deposit_source
        CHECK (
            (tenant_id IS NOT NULL AND lead_id IS NULL) OR
            (tenant_id IS NULL AND lead_id IS NOT NULL)
            );