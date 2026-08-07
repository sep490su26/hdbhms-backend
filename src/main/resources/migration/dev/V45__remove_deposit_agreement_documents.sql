-- Remove PDF draft and signed file columns from deposit_agreements
ALTER TABLE deposit_agreements
    DROP FOREIGN KEY fk_dep_agreement_file,
    DROP FOREIGN KEY fk_dep_agreement_signed_file,
    DROP FOREIGN KEY fk_dep_agreement_signed_by;

ALTER TABLE deposit_agreements
    DROP COLUMN contract_file_id,
    DROP COLUMN signed_file_id,
    DROP COLUMN signed_at,
    DROP COLUMN signed_uploaded_by;
