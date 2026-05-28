-- Thêm file metadata cho file PDF mẫu có sẵn trên đĩa
INSERT INTO file_metadata (owner_user_id, storage_key, original_name, mime_type, size_bytes, sha256_checksum, category, is_sensitive)
VALUES
(NULL, 'local/deposit/Hợp đồng TNT.pdf', 'Hợp đồng TNT.pdf', 'application/pdf', 456427, '6fa72d32571216d6c3ef6d1a1b4b5c6d3216858e9f5e3e2d1c0b0a9f8e7d6c5b', 'DEPOSIT_CONTRACT', FALSE);

SET @contract_file_id := LAST_INSERT_ID();

-- Gán tài liệu mẫu này cho toàn bộ hợp đồng cọc cũ chưa có tài liệu
UPDATE deposit_agreements
SET contract_file_id = @contract_file_id
WHERE contract_file_id IS NULL;

-- Gán tài liệu mẫu này cho toàn bộ hợp đồng thuê cũ chưa có tài liệu
UPDATE lease_contracts
SET contract_file_id = @contract_file_id
WHERE contract_file_id IS NULL;
