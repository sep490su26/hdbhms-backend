ALTER TABLE deposit_agreements
    ADD COLUMN deposit_form_id BIGINT UNSIGNED NULL AFTER room_id,
    ADD INDEX idx_dep_agreement_form (deposit_form_id),
    ADD CONSTRAINT fk_dep_agreement_form
        FOREIGN KEY (deposit_form_id) REFERENCES deposit_forms (id);