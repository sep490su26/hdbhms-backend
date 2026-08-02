INSERT INTO hdbhms.role_promotions
    (user_id, role, status, property_id, approved_at, created_at, updated_at, deleted_at)
SELECT seed.staff_user_id,
       'MANAGER',
       'ACTIVE',
       seed.property_id,
       COALESCE(seed.started_at, NOW(6)),
       COALESCE(seed.created_at, NOW(6)),
       NOW(6),
       NULL
FROM (
    SELECT psa.staff_user_id,
           psa.property_id,
           psa.started_at,
           psa.created_at,
           ROW_NUMBER() OVER (
               PARTITION BY psa.staff_user_id
               ORDER BY psa.is_primary DESC, psa.started_at DESC, psa.property_staff_assignment_id DESC
           ) AS assignment_rank
    FROM hdbhms.property_staff_assignments psa
    JOIN hdbhms.users u
      ON u.user_id = psa.staff_user_id
     AND u.role = 'MANAGER'
     AND u.deleted_at IS NULL
    JOIN hdbhms.properties p
      ON p.property_id = psa.property_id
     AND p.deleted_at IS NULL
    WHERE psa.assigned_role = 'MANAGER'
      AND psa.assignment_status = 'ACTIVE'
) seed
WHERE seed.assignment_rank = 1
  AND NOT EXISTS (
      SELECT 1
      FROM hdbhms.role_promotions rp
      WHERE rp.user_id = seed.staff_user_id
        AND rp.role = 'MANAGER'
        AND rp.deleted_at IS NULL
  );
