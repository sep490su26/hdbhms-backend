-- Move deposit lifecycle and payment data onto deposit_forms before removing the legacy table.
ALTER TABLE hdbhms.deposit_forms
    ADD COLUMN deposit_code VARCHAR(80) NULL AFTER deposit_expires_at,
    ADD COLUMN room_hold_id BIGINT UNSIGNED NULL AFTER deposit_code,
    ADD COLUMN tenant_id BIGINT UNSIGNED NULL AFTER room_hold_id,
    ADD COLUMN lead_id BIGINT UNSIGNED NULL AFTER tenant_id,
    ADD COLUMN depositor_person_profile_id BIGINT UNSIGNED NULL AFTER lead_id,
    ADD COLUMN amount BIGINT UNSIGNED NULL AFTER depositor_person_profile_id,
    ADD COLUMN deposit_status ENUM ('PENDING_PAYMENT', 'PAID', 'CONFIRMED', 'CONVERTED_TO_LEASE', 'EXTENDED', 'REFUNDED', 'FORFEITED', 'CANCELLED') NOT NULL DEFAULT 'PENDING_PAYMENT' AFTER amount,
    ADD COLUMN extension_count TINYINT UNSIGNED NOT NULL DEFAULT 0 AFTER deposit_status,
    ADD COLUMN max_extensions TINYINT UNSIGNED NOT NULL DEFAULT 1 AFTER extension_count,
    ADD COLUMN note TEXT NULL AFTER reject_reason,
    ADD COLUMN forfeiture_reason TEXT NULL AFTER note,
    ADD COLUMN refunded_amount BIGINT UNSIGNED NULL AFTER forfeiture_reason,
    ADD COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) AFTER created_at,
    ADD UNIQUE KEY uq_deposit_code (deposit_code),
    ADD KEY idx_deposit_form_hold (room_hold_id),
    ADD KEY idx_deposit_form_lead (lead_id, deposit_status),
    ADD KEY idx_deposit_form_person (depositor_person_profile_id, deposit_status),
    ADD KEY idx_deposit_form_room_deposit_status (room_id, deposit_status),
    ADD CONSTRAINT fk_dep_form_hold FOREIGN KEY (room_hold_id) REFERENCES hdbhms.room_holds (room_hold_id),
    ADD CONSTRAINT fk_dep_form_tenant FOREIGN KEY (tenant_id) REFERENCES hdbhms.tenants (tenant_id),
    ADD CONSTRAINT fk_dep_form_lead FOREIGN KEY (lead_id) REFERENCES hdbhms.leads (lead_id),
    ADD CONSTRAINT fk_dep_form_person_profile FOREIGN KEY (depositor_person_profile_id) REFERENCES hdbhms.person_profiles (person_profile_id);

DROP TEMPORARY TABLE IF EXISTS tmp_deposit_agreement_form_map;
CREATE TEMPORARY TABLE tmp_deposit_agreement_form_map
(
    deposit_agreement_id BIGINT UNSIGNED NOT NULL PRIMARY KEY,
    deposit_form_id      BIGINT UNSIGNED NOT NULL
);

INSERT INTO tmp_deposit_agreement_form_map (deposit_agreement_id, deposit_form_id)
SELECT deposit_agreement_id, deposit_form_id
FROM hdbhms.deposit_agreements
WHERE deposit_form_id IS NOT NULL;

UPDATE hdbhms.deposit_forms form
JOIN tmp_deposit_agreement_form_map map ON map.deposit_form_id = form.deposit_form_id
JOIN hdbhms.deposit_agreements agreement ON agreement.deposit_agreement_id = map.deposit_agreement_id
SET form.deposit_code = agreement.deposit_code,
    form.room_hold_id = agreement.room_hold_id,
    form.tenant_id = agreement.tenant_id,
    form.lead_id = agreement.lead_id,
    form.depositor_person_profile_id = agreement.depositor_person_profile_id,
    form.amount = agreement.amount,
    form.deposit_status = CASE agreement.status
        WHEN 'DRAFT' THEN 'PENDING_PAYMENT'
        ELSE agreement.status
    END,
    form.expected_move_in_date = agreement.expected_move_in_date,
    form.expected_lease_sign_date = agreement.expected_lease_sign_date,
    form.payment_due_at = agreement.payment_due_at,
    form.deposit_expires_at = agreement.deposit_expires_at,
    form.extension_count = agreement.extension_count,
    form.max_extensions = agreement.max_extensions,
    form.confirmed_at = agreement.confirmed_at,
    form.note = agreement.note,
    form.forfeiture_reason = agreement.forfeiture_reason,
    form.refunded_amount = agreement.refunded_amount,
    form.updated_at = agreement.updated_at;

-- Agreements without a form are retained through a minimal legacy form row.
INSERT INTO hdbhms.deposit_forms
    (room_id, id_number, full_name, email, phone, expected_move_in_date, expected_lease_sign_date,
     payment_due_at, deposit_expires_at, deposit_code, room_hold_id, tenant_id, lead_id,
     depositor_person_profile_id, amount, deposit_status, extension_count, max_extensions,
     status, confirmed_at, note, forfeiture_reason, refunded_amount, created_at, updated_at)
SELECT agreement.room_id,
       CONCAT('LEGACY-', agreement.deposit_agreement_id),
       COALESCE(NULLIF(profile.full_name, ''), CONCAT('Legacy deposit ', agreement.deposit_code)),
       COALESCE(NULLIF(profile.email, ''), CONCAT('legacy-deposit-', agreement.deposit_agreement_id, '@invalid.local')),
       COALESCE(NULLIF(profile.phone, ''), CONCAT('legacy-', agreement.deposit_agreement_id)),
       agreement.expected_move_in_date,
       agreement.expected_lease_sign_date,
       agreement.payment_due_at,
       agreement.deposit_expires_at,
       agreement.deposit_code,
       agreement.room_hold_id,
       agreement.tenant_id,
       agreement.lead_id,
       agreement.depositor_person_profile_id,
       agreement.amount,
       CASE agreement.status
           WHEN 'DRAFT' THEN 'PENDING_PAYMENT'
           ELSE agreement.status
       END,
       agreement.extension_count,
       agreement.max_extensions,
       'APPROVED',
       agreement.confirmed_at,
       agreement.note,
       agreement.forfeiture_reason,
       agreement.refunded_amount,
       agreement.created_at,
       agreement.updated_at
FROM hdbhms.deposit_agreements agreement
LEFT JOIN hdbhms.person_profiles profile
    ON profile.person_profile_id = agreement.depositor_person_profile_id
WHERE agreement.deposit_form_id IS NULL;

INSERT INTO tmp_deposit_agreement_form_map (deposit_agreement_id, deposit_form_id)
SELECT agreement.deposit_agreement_id, form.deposit_form_id
FROM hdbhms.deposit_agreements agreement
JOIN hdbhms.deposit_forms form ON form.deposit_code = agreement.deposit_code
WHERE agreement.deposit_form_id IS NULL;

ALTER TABLE hdbhms.deposit_batch_items
    DROP FOREIGN KEY fk_deposit_batch_item_agreement,
    DROP INDEX uq_deposit_batch_item_agreement;

UPDATE hdbhms.deposit_batch_items item
JOIN tmp_deposit_agreement_form_map map ON map.deposit_agreement_id = item.deposit_agreement_id
SET item.deposit_form_id = COALESCE(item.deposit_form_id, map.deposit_form_id);

ALTER TABLE hdbhms.deposit_batch_items
    DROP COLUMN deposit_agreement_id;

ALTER TABLE hdbhms.invoices
    DROP FOREIGN KEY fk_inv_deposit_agreement,
    ADD COLUMN deposit_form_id BIGINT UNSIGNED NULL AFTER lease_contract_id;

UPDATE hdbhms.invoices invoice
JOIN tmp_deposit_agreement_form_map map ON map.deposit_agreement_id = invoice.deposit_agreement_id
SET invoice.deposit_form_id = map.deposit_form_id;

ALTER TABLE hdbhms.invoices
    DROP COLUMN deposit_agreement_id,
    ADD KEY idx_invoices_deposit_form (deposit_form_id),
    ADD CONSTRAINT fk_inv_deposit_form FOREIGN KEY (deposit_form_id) REFERENCES hdbhms.deposit_forms (deposit_form_id);

ALTER TABLE hdbhms.payment_intents
    DROP FOREIGN KEY fk_pi_deposit,
    ADD COLUMN deposit_form_id BIGINT UNSIGNED NULL AFTER invoice_id;

UPDATE hdbhms.payment_intents intent
JOIN tmp_deposit_agreement_form_map map ON map.deposit_agreement_id = intent.deposit_agreement_id
SET intent.deposit_form_id = map.deposit_form_id;

ALTER TABLE hdbhms.payment_intents
    DROP COLUMN deposit_agreement_id,
    ADD KEY idx_payment_intents_deposit_form (deposit_form_id),
    ADD CONSTRAINT fk_pi_deposit_form FOREIGN KEY (deposit_form_id) REFERENCES hdbhms.deposit_forms (deposit_form_id);

ALTER TABLE hdbhms.deposit_contact_events
    DROP FOREIGN KEY fk_dce_deposit,
    DROP INDEX idx_deposit_contact_latest,
    CHANGE COLUMN deposit_agreement_id deposit_form_id BIGINT UNSIGNED NOT NULL;

UPDATE hdbhms.deposit_contact_events contact_event
JOIN tmp_deposit_agreement_form_map map ON map.deposit_agreement_id = contact_event.deposit_form_id
SET contact_event.deposit_form_id = map.deposit_form_id;

ALTER TABLE hdbhms.deposit_contact_events
    ADD CONSTRAINT fk_dce_deposit_form FOREIGN KEY (deposit_form_id) REFERENCES hdbhms.deposit_forms (deposit_form_id),
    ADD INDEX idx_deposit_contact_latest (deposit_form_id, contacted_at);

ALTER TABLE hdbhms.deposit_extension_events
    DROP FOREIGN KEY fk_dee_deposit,
    DROP INDEX idx_deposit_extension,
    CHANGE COLUMN deposit_agreement_id deposit_form_id BIGINT UNSIGNED NOT NULL;

UPDATE hdbhms.deposit_extension_events extension_event
JOIN tmp_deposit_agreement_form_map map ON map.deposit_agreement_id = extension_event.deposit_form_id
SET extension_event.deposit_form_id = map.deposit_form_id;

ALTER TABLE hdbhms.deposit_extension_events
    ADD CONSTRAINT fk_dee_deposit_form FOREIGN KEY (deposit_form_id) REFERENCES hdbhms.deposit_forms (deposit_form_id),
    ADD INDEX idx_deposit_extension (deposit_form_id, approved_at);

ALTER TABLE hdbhms.deposit_transfer_records
    DROP FOREIGN KEY fk_deposit_transfer_old_deposit,
    CHANGE COLUMN old_deposit_agreement_id old_deposit_form_id BIGINT UNSIGNED NULL;

UPDATE hdbhms.deposit_transfer_records transfer_record
JOIN tmp_deposit_agreement_form_map map ON map.deposit_agreement_id = transfer_record.old_deposit_form_id
SET transfer_record.old_deposit_form_id = map.deposit_form_id;

ALTER TABLE hdbhms.deposit_transfer_records
    ADD CONSTRAINT fk_deposit_transfer_old_form
        FOREIGN KEY (old_deposit_form_id) REFERENCES hdbhms.deposit_forms (deposit_form_id);

ALTER TABLE hdbhms.lease_contracts
    DROP FOREIGN KEY fk_lc_deposit,
    DROP INDEX uq_lease_contracts_deposit_agreement,
    CHANGE COLUMN deposit_agreement_id deposit_form_id BIGINT UNSIGNED NULL;

UPDATE hdbhms.lease_contracts lease_contract
JOIN tmp_deposit_agreement_form_map map ON map.deposit_agreement_id = lease_contract.deposit_form_id
SET lease_contract.deposit_form_id = map.deposit_form_id;

ALTER TABLE hdbhms.lease_contracts
    ADD UNIQUE KEY uq_lease_contracts_deposit_form (deposit_form_id),
    ADD CONSTRAINT fk_lc_deposit_form
        FOREIGN KEY (deposit_form_id) REFERENCES hdbhms.deposit_forms (deposit_form_id);

DROP TEMPORARY TABLE tmp_deposit_agreement_form_map;
DROP TABLE hdbhms.deposit_agreements;
