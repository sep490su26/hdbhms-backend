-- Dev login accounts.
-- Admin uses OWNER because the current application role enum does not define ADMIN.
-- Plain-text passwords for local development:
--   admin@hdbhms.local / Admin@123
--   manager1@hdbhms.local / Manager@123
--   manager2@hdbhms.local / Manager@123
--   manager3@hdbhms.local / Manager@123

INSERT IGNORE INTO users (
    phone,
    email,
    password_hash,
    role,
    status,
    email_verified,
    must_change_password
)
VALUES
    (
        '0901000001',
        'admin@hdbhms.local',
        '$2a$10$Ar1EoCiv/Zs.40ujaNRUPuMCsVkdvpTmu7NIkCrA27By8n1cs44qy',
        'OWNER',
        'ACTIVE',
        TRUE,
        FALSE
    ),
    (
        '0901000002',
        'manager1@hdbhms.local',
        '$2a$10$YbhXG9M5umKrvNUrGIbmCuMW.fuCRQB4lTnpsd4e22Czc8pmYPr4K',
        'MANAGER',
        'ACTIVE',
        TRUE,
        FALSE
    ),
    (
        '0901000003',
        'manager2@hdbhms.local',
        '$2a$10$YbhXG9M5umKrvNUrGIbmCuMW.fuCRQB4lTnpsd4e22Czc8pmYPr4K',
        'MANAGER',
        'ACTIVE',
        TRUE,
        FALSE
    ),
    (
        '0901000004',
        'manager3@hdbhms.local',
        '$2a$10$YbhXG9M5umKrvNUrGIbmCuMW.fuCRQB4lTnpsd4e22Czc8pmYPr4K',
        'MANAGER',
        'ACTIVE',
        TRUE,
        FALSE
    );

INSERT IGNORE INTO person_profiles (
    user_id,
    full_name,
    dob,
    gender,
    phone,
    email,
    permanent_address
)
SELECT
    u.id,
    seed.full_name,
    seed.dob,
    seed.gender,
    seed.phone,
    seed.email,
    seed.permanent_address
FROM users u
JOIN (
    SELECT
        'admin@hdbhms.local' email,
        '0901000001' phone,
        'System Administrator' full_name,
        DATE '1990-01-01' dob,
        'UNKNOWN' gender,
        'HDBHMS development admin account' permanent_address
    UNION ALL
    SELECT
        'manager1@hdbhms.local',
        '0901000002',
        'Nguyen Van Quan',
        DATE '1991-02-15',
        'MALE',
        'Hai Dang 1, Development Property'
    UNION ALL
    SELECT
        'manager2@hdbhms.local',
        '0901000003',
        'Tran Thi Ly',
        DATE '1992-06-20',
        'FEMALE',
        'Hai Dang 2, Development Property'
    UNION ALL
    SELECT
        'manager3@hdbhms.local',
        '0901000004',
        'Le Minh Khang',
        DATE '1989-11-05',
        'MALE',
        'Can ho A, Development Property'
) seed ON seed.email = u.email
WHERE u.deleted_at IS NULL;

INSERT IGNORE INTO role_promotions (
    user_id,
    role,
    status,
    property_id,
    approved_at
)
SELECT
    u.id,
    'MANAGER',
    'ACTIVE',
    p.id,
    CURRENT_TIMESTAMP(6)
FROM users u
JOIN (
    SELECT 'manager1@hdbhms.local' email, 'HD1' property_code
    UNION ALL SELECT 'manager2@hdbhms.local', 'HD2'
    UNION ALL SELECT 'manager3@hdbhms.local', 'CHA'
) seed ON seed.email = u.email
JOIN properties p ON p.property_code = seed.property_code
WHERE u.deleted_at IS NULL
  AND p.deleted_at IS NULL;
