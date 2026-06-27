-- V45: Add signed_document_id to contract_handover_records
-- and expand file_metadata.category ENUM to include new document types

-- 1. Expand the ENUM column in file_metadata to include new categories
ALTER TABLE file_metadata
    MODIFY COLUMN category ENUM(
        'ROOM_IMAGE',
        'PROPERTY_IMAGE',
        'PORTRAIT_PHOTO',
        'ID_CARD',
        'CONTRACT',
        'DEPOSIT_CONTRACT',
        'METER_PHOTO',
        'VEHICLE_PHOTO',
        'MAINTENANCE',
        'TICKET_ATTACHMENT',
        'RECEIPT',
        'OCR_INPUT',
        'LEASE_CONTRACT_DRAFT',
        'HANDOVER_DOCUMENT',
        'OTHER'
    ) NOT NULL DEFAULT 'OTHER';

-- 2. Add signed_document_id FK column to contract_handover_records
ALTER TABLE contract_handover_records
    ADD COLUMN signed_document_id BIGINT UNSIGNED NULL,
    ADD CONSTRAINT fk_chr_signed_document
        FOREIGN KEY (signed_document_id) REFERENCES file_metadata(id)
        ON DELETE SET NULL;
