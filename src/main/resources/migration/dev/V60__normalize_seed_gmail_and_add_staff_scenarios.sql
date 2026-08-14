SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

SET @hdd1_property_id := (
    SELECT property_id
    FROM hdbhms.properties
    WHERE property_code = 'HAI_DANG_1'
      AND deleted_at IS NULL
    LIMIT 1
);
SET @hdd1_seed_now := '2026-08-01 10:00:00';
SET @hdd1_password_hash := '$2a$10$2Dy4Vg1B5BKuiUMPRuTAluvk/0XzLuSgLGaABFHCoWHaUfUtDFGqm';

-- Keep every seeded account and its profile/provisioning email in sync.
-- The local part is the Vietnamese full name without accents or separators,
-- followed by the last two digits of the birth year.
DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_seed_email_map;
CREATE TEMPORARY TABLE tmp_hdd1_seed_email_map
(
    user_id   BIGINT UNSIGNED NOT NULL PRIMARY KEY,
    new_email VARCHAR(255)    NOT NULL UNIQUE
);

INSERT INTO tmp_hdd1_seed_email_map (user_id, new_email)
SELECT user_account.user_id,
       CASE user_account.role
           WHEN 'OWNER' THEN 'nguyenminhquang80@gmail.com'
           WHEN 'MANAGER' THEN 'tranthuhuong90@gmail.com'
           ELSE CONCAT(
               COALESCE(
                   NULLIF(
                       REPLACE(
                           REGEXP_REPLACE(COALESCE(SUBSTRING_INDEX(user_account.email, '@', 1), ''), '\\.[0-9]+$', ''),
                           '.',
                           ''
                       ),
                       ''
                   ),
                   CONCAT('seeduser', user_account.user_id)
               ),
               COALESCE(DATE_FORMAT(profile.dob, '%y'), '00'),
               '@gmail.com'
           )
       END
FROM hdbhms.users user_account
JOIN hdbhms.person_profiles profile
  ON profile.user_id = user_account.user_id
 AND profile.deleted_at IS NULL
WHERE user_account.deleted_at IS NULL
  AND EXISTS (
      SELECT 1
      FROM hdbhms.tenants tenant
      JOIN hdbhms.properties property
        ON property.property_id = tenant.property_id
       AND property.deleted_at IS NULL
      WHERE tenant.user_id = user_account.user_id
        AND tenant.deleted_at IS NULL
        AND property.property_code = 'HAI_DANG_1'
  );

UPDATE hdbhms.tenant_account_provisionings provisioning
JOIN hdbhms.person_profiles profile
  ON profile.person_profile_id = provisioning.tenant_profile_id
JOIN tmp_hdd1_seed_email_map email_map
  ON email_map.user_id = profile.user_id
SET provisioning.recipient_email = email_map.new_email,
    provisioning.updated_at = @hdd1_seed_now;

UPDATE hdbhms.person_profiles profile
JOIN tmp_hdd1_seed_email_map email_map
  ON email_map.user_id = profile.user_id
SET profile.email = email_map.new_email,
    profile.updated_at = @hdd1_seed_now;

UPDATE hdbhms.users user_account
JOIN tmp_hdd1_seed_email_map email_map
  ON email_map.user_id = user_account.user_id
SET user_account.email = email_map.new_email,
    user_account.updated_at = @hdd1_seed_now;

UPDATE hdbhms.visit_requests
SET visitor_email = CASE visitor_name
        WHEN 'Phạm Minh Hoàng' THEN 'phamminhhoang99@gmail.com'
        WHEN 'Vũ Thị Ngọc Anh' THEN 'vuthingocanh01@gmail.com'
        WHEN 'Hoàng Đức Long' THEN 'hoangduclong98@gmail.com'
        ELSE visitor_email
    END
WHERE property_id = @hdd1_property_id
  AND visitor_name IN ('Phạm Minh Hoàng', 'Vũ Thị Ngọc Anh', 'Hoàng Đức Long');

DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_seed_email_map;

-- Staff scenario 1: temporarily locked and still assigned to Hai Dang 1.
INSERT INTO hdbhms.users
    (phone, email, password_hash, role, status, last_login_at, email_verified,
     must_change_password, created_at, updated_at, deleted_at)
VALUES
    ('0938456721', 'vohoanglong94@gmail.com', @hdd1_password_hash, 'MANAGER', 'INACTIVE',
     '2026-07-28 17:20:00', TRUE, FALSE, '2026-03-15 08:30:00', @hdd1_seed_now, NULL),
    ('0862793415', 'nguyenngocanh96@gmail.com', @hdd1_password_hash, 'MANAGER', 'ACTIVE',
     '2026-07-30 08:45:00', TRUE, FALSE, '2026-07-20 09:15:00', @hdd1_seed_now, NULL)
ON DUPLICATE KEY UPDATE
    password_hash = VALUES(password_hash),
    role = VALUES(role),
    status = VALUES(status),
    last_login_at = VALUES(last_login_at),
    email_verified = VALUES(email_verified),
    must_change_password = VALUES(must_change_password),
    deleted_at = NULL,
    updated_at = VALUES(updated_at);

SET @hdd1_owner_id := (
    SELECT user_id
    FROM hdbhms.users
    WHERE email = 'nguyenminhquang80@gmail.com'
      AND deleted_at IS NULL
    LIMIT 1
);
SET @hdd1_locked_manager_id := (
    SELECT user_id
    FROM hdbhms.users
    WHERE email = 'vohoanglong94@gmail.com'
      AND deleted_at IS NULL
    LIMIT 1
);
SET @hdd1_unassigned_manager_id := (
    SELECT user_id
    FROM hdbhms.users
    WHERE email = 'nguyenngocanh96@gmail.com'
      AND deleted_at IS NULL
    LIMIT 1
);

INSERT INTO hdbhms.person_profiles
    (user_id, full_name, dob, gender, phone, email, permanent_address,
     portrait_file_id, created_at, updated_at, deleted_at)
VALUES
    (@hdd1_locked_manager_id, 'Võ Hoàng Long', '1994-09-16', 'MALE', '0938456721',
     'vohoanglong94@gmail.com', 'Hà Nội', NULL, '2026-03-15 08:30:00', @hdd1_seed_now, NULL),
    (@hdd1_unassigned_manager_id, 'Nguyễn Ngọc Anh', '1996-04-22', 'FEMALE', '0862793415',
     'nguyenngocanh96@gmail.com', 'Hà Nội', NULL, '2026-07-20 09:15:00', @hdd1_seed_now, NULL)
ON DUPLICATE KEY UPDATE
    full_name = VALUES(full_name),
    dob = VALUES(dob),
    gender = VALUES(gender),
    phone = VALUES(phone),
    email = VALUES(email),
    permanent_address = VALUES(permanent_address),
    deleted_at = NULL,
    updated_at = VALUES(updated_at);

INSERT INTO hdbhms.property_staff_assignments
    (property_id, staff_user_id, assigned_role, assignment_status, is_primary,
     notes, assigned_by_user_id, started_at, ended_at, created_at, updated_at)
SELECT @hdd1_property_id, @hdd1_locked_manager_id, 'MANAGER', 'ACTIVE', FALSE,
       'Nhân viên đang tạm khóa, vẫn lưu cơ sở phụ trách gần nhất.', @hdd1_owner_id,
       '2026-03-15 08:30:00', NULL, '2026-03-15 08:30:00', @hdd1_seed_now
WHERE @hdd1_property_id IS NOT NULL
  AND @hdd1_locked_manager_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM hdbhms.property_staff_assignments assignment
      WHERE assignment.property_id = @hdd1_property_id
        AND assignment.staff_user_id = @hdd1_locked_manager_id
        AND assignment.assigned_role = 'MANAGER'
        AND assignment.assignment_status = 'ACTIVE'
  );

-- Staff scenario 2 intentionally has no role promotion or staff assignment,
-- so the account management API returns an empty assignedProperties list.
