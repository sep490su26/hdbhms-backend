-- Migrate electricity/water maintenance tickets into utility complaints domain.
-- Safe strategy:
--   1) copy maintenance tickets to utility_complaints
--   2) copy attachments and status events
--   3) mark old maintenance tickets as migrated/cancelled instead of hard delete
-- Hard delete is avoided because maintenance_tickets may still have linked costs/reviews.

INSERT INTO utility_complaints
(
    property_id,
    room_id,
    tenant_id,
    reported_by_user_id,
    assigned_to_user_id,
    complaint_code,
    complaint_type,
    status,
    title,
    description,
    resolution_note,
    reported_at,
    acknowledged_at,
    resolved_at,
    cancelled_at,
    created_at,
    updated_at,
    deleted_at,
    version
)
SELECT
    mt.property_id,
    mt.room_id,
    NULL AS tenant_id,
    mt.created_by AS reported_by_user_id,
    mt.assigned_to AS assigned_to_user_id,
    mt.ticket_code AS complaint_code,
    CASE
        WHEN UPPER(TRIM(mt.category)) IN ('ELECTRICITY', 'ELECTRIC', 'ELECTRICAL', 'POWER') THEN 'ELECTRICITY'
        WHEN UPPER(TRIM(mt.category)) IN ('WATER', 'PLUMBING', 'WATER_LEAK') THEN 'WATER'
        ELSE 'OTHER'
    END AS complaint_type,
    CASE
        WHEN mt.status = 'PENDING_ACCEPTANCE' THEN 'OPEN'
        WHEN mt.status IN ('ACCEPTED', 'IN_PROGRESS') THEN 'IN_PROGRESS'
        WHEN mt.status = 'WAITING_CONFIRMATION' THEN 'WAITING_TENANT'
        WHEN mt.status = 'COMPLETED' THEN 'RESOLVED'
        WHEN mt.status IN ('REJECTED', 'CANCELLED') THEN 'CANCELLED'
        ELSE 'OPEN'
    END AS status,
    mt.title,
    mt.description,
    CASE
        WHEN mt.repair_items IS NOT NULL AND mt.rejection_reason IS NOT NULL
            THEN CONCAT('Repair items: ', mt.repair_items, '\nRejected reason: ', mt.rejection_reason)
        WHEN mt.repair_items IS NOT NULL
            THEN CONCAT('Repair items: ', mt.repair_items)
        WHEN mt.rejection_reason IS NOT NULL
            THEN CONCAT('Rejected reason: ', mt.rejection_reason)
        ELSE NULL
    END AS resolution_note,
    mt.created_at AS reported_at,
    CASE
        WHEN mt.status IN ('ACCEPTED', 'IN_PROGRESS', 'WAITING_CONFIRMATION', 'COMPLETED')
            THEN mt.updated_at
        ELSE NULL
    END AS acknowledged_at,
    CASE
        WHEN mt.status = 'COMPLETED' THEN COALESCE(mt.completed_at, mt.updated_at)
        ELSE NULL
    END AS resolved_at,
    CASE
        WHEN mt.status IN ('REJECTED', 'CANCELLED') THEN mt.updated_at
        ELSE NULL
    END AS cancelled_at,
    mt.created_at,
    mt.updated_at,
    NULL AS deleted_at,
    0 AS version
FROM maintenance_tickets mt
WHERE UPPER(TRIM(mt.category)) IN (
    'ELECTRICITY', 'ELECTRIC', 'ELECTRICAL', 'POWER',
    'WATER', 'PLUMBING', 'WATER_LEAK'
)
AND NOT EXISTS (
    SELECT 1
    FROM utility_complaints uc
    WHERE uc.complaint_code = mt.ticket_code
);

INSERT INTO utility_complaint_attachments
(
    utility_complaint_id,
    file_metadata_id,
    attachment_type,
    uploaded_by_user_id,
    created_at
)
SELECT
    uc.utility_complaint_id,
    mta.file_id AS file_metadata_id,
    CASE
        WHEN mta.attachment_phase = 'RECEIPT' THEN 'INVOICE'
        ELSE 'IMAGE'
    END AS attachment_type,
    COALESCE(t.user_id, mt.created_by) AS uploaded_by_user_id,
    mta.created_at
FROM maintenance_ticket_attachments mta
JOIN maintenance_tickets mt
    ON mt.maintenance_ticket_id = mta.ticket_id
JOIN utility_complaints uc
    ON uc.complaint_code = mt.ticket_code
LEFT JOIN tenants t
    ON t.tenant_id = mta.created_by
WHERE UPPER(TRIM(mt.category)) IN (
    'ELECTRICITY', 'ELECTRIC', 'ELECTRICAL', 'POWER',
    'WATER', 'PLUMBING', 'WATER_LEAK'
)
AND NOT EXISTS (
    SELECT 1
    FROM utility_complaint_attachments uca
    WHERE uca.utility_complaint_id = uc.utility_complaint_id
      AND uca.file_metadata_id = mta.file_id
);

INSERT INTO utility_complaint_status_history
(
    utility_complaint_id,
    from_status,
    to_status,
    changed_by_user_id,
    note,
    created_at
)
SELECT
    uc.utility_complaint_id,
    CASE
        WHEN mte.from_status = 'PENDING_ACCEPTANCE' THEN 'OPEN'
        WHEN mte.from_status IN ('ACCEPTED', 'IN_PROGRESS') THEN 'IN_PROGRESS'
        WHEN mte.from_status = 'WAITING_CONFIRMATION' THEN 'WAITING_TENANT'
        WHEN mte.from_status = 'COMPLETED' THEN 'RESOLVED'
        WHEN mte.from_status IN ('REJECTED', 'CANCELLED') THEN 'CANCELLED'
        WHEN mte.from_status IS NULL THEN NULL
        ELSE 'OPEN'
    END AS from_status,
    CASE
        WHEN mte.to_status = 'PENDING_ACCEPTANCE' THEN 'OPEN'
        WHEN mte.to_status IN ('ACCEPTED', 'IN_PROGRESS') THEN 'IN_PROGRESS'
        WHEN mte.to_status = 'WAITING_CONFIRMATION' THEN 'WAITING_TENANT'
        WHEN mte.to_status = 'COMPLETED' THEN 'RESOLVED'
        WHEN mte.to_status IN ('REJECTED', 'CANCELLED') THEN 'CANCELLED'
        ELSE 'OPEN'
    END AS to_status,
    mte.created_by AS changed_by_user_id,
    mte.note,
    mte.created_at
FROM maintenance_ticket_events mte
JOIN maintenance_tickets mt
    ON mt.maintenance_ticket_id = mte.ticket_id
JOIN utility_complaints uc
    ON uc.complaint_code = mt.ticket_code
WHERE UPPER(TRIM(mt.category)) IN (
    'ELECTRICITY', 'ELECTRIC', 'ELECTRICAL', 'POWER',
    'WATER', 'PLUMBING', 'WATER_LEAK'
)
AND NOT EXISTS (
    SELECT 1
    FROM utility_complaint_status_history ucsh
    WHERE ucsh.utility_complaint_id = uc.utility_complaint_id
      AND ucsh.created_at = mte.created_at
      AND ucsh.to_status = CASE
          WHEN mte.to_status = 'PENDING_ACCEPTANCE' THEN 'OPEN'
          WHEN mte.to_status IN ('ACCEPTED', 'IN_PROGRESS') THEN 'IN_PROGRESS'
          WHEN mte.to_status = 'WAITING_CONFIRMATION' THEN 'WAITING_TENANT'
          WHEN mte.to_status = 'COMPLETED' THEN 'RESOLVED'
          WHEN mte.to_status IN ('REJECTED', 'CANCELLED') THEN 'CANCELLED'
          ELSE 'OPEN'
      END
);

UPDATE maintenance_tickets mt
SET
    mt.category = 'UTILITY_MIGRATED',
    mt.status = 'CANCELLED',
    mt.rejection_reason = CASE
        WHEN mt.rejection_reason IS NULL OR TRIM(mt.rejection_reason) = ''
            THEN 'Migrated to utility_complaints by V48 migration'
        ELSE CONCAT(mt.rejection_reason, ' | Migrated to utility_complaints by V48 migration')
    END,
    mt.updated_at = CURRENT_TIMESTAMP(6)
WHERE UPPER(TRIM(mt.category)) IN (
    'ELECTRICITY', 'ELECTRIC', 'ELECTRICAL', 'POWER',
    'WATER', 'PLUMBING', 'WATER_LEAK'
);