-- Separate external repairman details from system staff assignment.
-- Repairmen are external service providers, not users/staff in the system.
-- Keep assigned_to as the internal system user managing/handling the ticket.
-- Store external repairman information in dedicated nullable columns.

ALTER TABLE maintenance_tickets
    ADD COLUMN external_repairman_name VARCHAR(255) NULL AFTER worker_name,
    ADD COLUMN external_repairman_phone VARCHAR(30) NULL AFTER external_repairman_name,
    ADD COLUMN external_repair_provider VARCHAR(255) NULL AFTER external_repairman_phone,
    ADD COLUMN external_repair_note VARCHAR(1000) NULL AFTER external_repair_provider;

UPDATE maintenance_tickets
SET external_repairman_name = worker_name,
    external_repairman_phone = repairman_phone
WHERE worker_name IS NOT NULL
   OR repairman_phone IS NOT NULL;

CREATE INDEX idx_maintenance_tickets_external_repairman
    ON maintenance_tickets (external_repairman_phone, external_repairman_name);