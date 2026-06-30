-- Update maintenance_tickets schema for external repair providers.
-- Remove foreign key constraint for assigned_to column, as repairmen are external.

-- 1. Drop existing foreign key constraint from assigned_to to users.
ALTER TABLE maintenance_tickets
    DROP CONSTRAINT fk_mt_assigned;

-- Note: The 'assigned_to' column remains as BIGINT UNSIGNED NULL.
-- It can be repurposed for an external provider ID or left for future definition.
-- The worker_name and repairman_phone columns are already present for external contact details.