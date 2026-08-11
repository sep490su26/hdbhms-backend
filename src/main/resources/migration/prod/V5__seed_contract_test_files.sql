-- Seed a reusable lease-contract file for test data.
INSERT INTO file_metadata (owner_user_id, storage_key, original_name, mime_type, size_bytes, sha256_checksum, category, is_sensitive)
VALUES
(NULL, 'local/lease/lease-contract-template.pdf', 'lease-contract-template.pdf', 'application/pdf', 456427, '6fa72d32571216d6c3ef6d1a1b4b5c6d3216858e9f5e3e2d1c0b0a9f8e7d6c5b', 'CONTRACT', FALSE);

SET @contract_file_id := LAST_INSERT_ID();

UPDATE lease_contracts
SET contract_file_id = @contract_file_id
WHERE contract_file_id IS NULL;
