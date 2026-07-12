-- V37: Add signed file support for deposit agreements (offline signing flow)
-- deposit_agreements currently only has contract_file_id for the system-generated draft PDF.
-- This migration adds columns to store the scanned/uploaded signed contract file,
-- matching the pattern used by lease_contracts (contract_file_id + signed_at).

ALTER TABLE deposit_agreements
    ADD COLUMN signed_file_id       BIGINT UNSIGNED NULL AFTER contract_file_id,
    ADD COLUMN signed_at            DATETIME(6)     NULL AFTER signed_file_id,
    ADD COLUMN signed_uploaded_by   BIGINT UNSIGNED NULL AFTER signed_at,
    ADD CONSTRAINT fk_dep_agreement_signed_file FOREIGN KEY (signed_file_id) REFERENCES file_metadata (id),
    ADD CONSTRAINT fk_dep_agreement_signed_by   FOREIGN KEY (signed_uploaded_by) REFERENCES users (id);
