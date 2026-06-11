ALTER TABLE maintenance_tickets
    ADD COLUMN repairman_phone VARCHAR(30) NULL AFTER worker_name;

ALTER TABLE maintenance_ticket_events
    ADD COLUMN action VARCHAR(50) NULL AFTER to_status;

ALTER TABLE maintenance_ticket_attachments
    ADD COLUMN created_by_user_id BIGINT UNSIGNED NULL AFTER created_by,
    ADD CONSTRAINT fk_mta_created_by_user FOREIGN KEY (created_by_user_id) REFERENCES users (id);

ALTER TABLE maintenance_costs
    ADD COLUMN cost_responsibility VARCHAR(50) NOT NULL DEFAULT 'UNDECIDED' AFTER paid_by;
