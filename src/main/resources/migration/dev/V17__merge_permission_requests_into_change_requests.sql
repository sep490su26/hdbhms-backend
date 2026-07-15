ALTER TABLE hdbhms.change_requests
    MODIFY COLUMN request_type ENUM (
        'METER_READING_CORRECTION',
        'INVOICE_ADJUSTMENT',
        'RENT_PRICE_ADJUSTMENT',
        'DEPOSIT_REFUND_REQUEST',
        'ROOM_TRANSFER',
        'MOVE_OUT',
        'COMPLAINT',
        'PERMISSION_ACCESS',
        'TENANT_PROFILE_ACCESS',
        'ADD_CO_OCCUPANT'
    ) NOT NULL;

ALTER TABLE hdbhms.change_requests
    MODIFY COLUMN requester_role ENUM (
        'LEAD',
        'TENANT',
        'MANAGER',
        'ACCOUNTANT',
        'OWNER'
    ) NOT NULL;

ALTER TABLE hdbhms.change_requests
    MODIFY COLUMN target_type ENUM (
        'METER_READING',
        'INVOICE',
        'CONTRACT',
        'DEPOSIT',
        'TENANT_PROFILE',
        'IDENTITY_DOCUMENT',
        'REPORT',
        'FILE',
        'OTHER'
    ) NOT NULL;

INSERT INTO hdbhms.change_requests (
    request_code,
    request_type,
    requester_id,
    requester_role,
    target_type,
    target_id,
    title,
    description,
    request_payload,
    assigned_role,
    status,
    resolution_note,
    resolved_at,
    created_at,
    updated_at
)
SELECT CONCAT('PR-LEGACY-', pr.permission_request_id),
       'PERMISSION_ACCESS',
       pr.requester_user_id,
       CASE u.role
           WHEN 'LEAD' THEN 'LEAD'
           WHEN 'MANAGER' THEN 'MANAGER'
           WHEN 'ACCOUNTANT' THEN 'ACCOUNTANT'
           WHEN 'OWNER' THEN 'OWNER'
           ELSE 'TENANT'
       END,
       pr.target_type,
       pr.target_id,
       CONCAT('Legacy permission request #', pr.permission_request_id),
       CONCAT('Migrated permission request for ', pr.target_type, ' #', pr.target_id),
       JSON_OBJECT(
           'legacyPermissionRequestId', pr.permission_request_id,
           'legacyStatus', pr.status,
           'legacyExpiresAt', pr.expires_at
       ),
       'OWNER',
       CASE pr.status
           WHEN 'APPROVED' THEN 'APPROVED'
           WHEN 'REJECTED' THEN 'REJECTED'
           WHEN 'PENDING' THEN 'PENDING'
           ELSE 'CANCELLED'
       END,
       NULLIF(pr.rejected_reason, ''),
       pr.decided_at,
       pr.created_at,
       COALESCE(pr.decided_at, pr.created_at)
FROM hdbhms.permission_requests pr
JOIN hdbhms.users u ON u.user_id = pr.requester_user_id
WHERE NOT EXISTS (
    SELECT 1
    FROM hdbhms.change_requests cr
    WHERE cr.request_code = CONCAT('PR-LEGACY-', pr.permission_request_id)
);
