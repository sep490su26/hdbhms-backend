DROP DATABASE IF EXISTS hdbhms;
CREATE DATABASE hdbhms
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;
USE hdbhms;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =========================================================
-- 1. TENANCY / IDENTITY
-- =========================================================
CREATE TABLE invalidated_tokens
(
    id          VARCHAR(255) NOT NULL,
    expiry_time DATETIME(6)  NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB;

CREATE TABLE login_history
(
    login_history_id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    user_id          BIGINT UNSIGNED NOT NULL,
    status           VARCHAR(50)     NOT NULL,
    ip_address       VARCHAR(45)     NULL,
    user_agent       VARCHAR(500)    NULL,
    method           VARCHAR(50)     NOT NULL,
    session_id       VARCHAR(255)    NULL,
    device_id        VARCHAR(255)    NULL,
    logged_in_at     DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_login_history_user (user_id),
    CONSTRAINT fk_login_history_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB;

CREATE TABLE user_modification_histories
(
    id         BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    user_id    BIGINT UNSIGNED NOT NULL,
    type       VARCHAR(50)     NOT NULL,
    old_value  VARCHAR(255)    NULL,
    new_value  VARCHAR(255)    NULL,
    changed_at DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_modhist_user (id),
    CONSTRAINT fk_modhist_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB;

CREATE TABLE user_status_logs
(
    id             BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    user_entity_id BIGINT UNSIGNED NOT NULL,
    old_status     VARCHAR(50)     NOT NULL,
    new_status     VARCHAR(50)     NOT NULL,
    reason         VARCHAR(255)    NULL,
    changed_by     VARCHAR(255)    NULL,
    changed_at     DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_status_logs_user (user_entity_id),
    CONSTRAINT fk_status_logs_user FOREIGN KEY (user_entity_id) REFERENCES users (id)
) ENGINE = InnoDB;

CREATE TABLE users
(
    id                  BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    phone               VARCHAR(30)                                             NOT NULL,
    email               VARCHAR(255)                                            NOT NULL,
    password_hash       VARCHAR(255)                                            NOT NULL,
    role                ENUM ('TENANT','MANAGER','ACCOUNTANT', 'OWNER', 'LEAD') NOT NULL DEFAULT 'LEAD',
    status              ENUM ('PENDING_CONTRACT','ACTIVE','DISABLED')           NOT NULL DEFAULT 'PENDING_CONTRACT',
    last_login_at       DATETIME(6)                                             NULL,
    email_verified      BOOLEAN                                                 NOT NULL DEFAULT FALSE,
    created_at          DATETIME(6)                                             NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)                                             NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at          DATETIME(6)                                             NULL,
    active_unique_token TINYINT GENERATED ALWAYS AS (IF(deleted_at IS NULL, 1, NULL)) VIRTUAL,
    UNIQUE KEY uq_users_phone_active (phone, active_unique_token),
    UNIQUE KEY uq_users_email_active (email, active_unique_token),
    KEY idx_users_status (status, created_at)
) ENGINE = InnoDB;

CREATE TABLE tenants
(
    id                  BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    user_id             BIGINT UNSIGNED NOT NULL,
    property_id         BIGINT UNSIGNED NOT NULL,
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at          DATETIME(6)     NULL,
    active_tenant_token TINYINT GENERATED ALWAYS AS (
        IF(deleted_at IS NULL, 1, NULL)
        ) VIRTUAL,
    UNIQUE KEY uq_tenant_active (user_id, property_id, active_tenant_token),
    KEY idx_tenant_property (property_id),
    CONSTRAINT fk_tenant_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_tenant_property FOREIGN KEY (property_id) REFERENCES properties (id)
) ENGINE = InnoDB;

CREATE TABLE file_metadata
(
    id              BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    owner_user_id   BIGINT UNSIGNED NULL,
    storage_key     VARCHAR(1000)   NOT NULL,
    original_name   VARCHAR(255)    NULL,
    mime_type       VARCHAR(100)    NULL,
    size_bytes      BIGINT UNSIGNED NULL,
    sha256_checksum CHAR(64)        NULL,
    category        ENUM (
        'ROOM_IMAGE','PROPERTY_IMAGE','PORTRAIT_PHOTO','ID_CARD','CONTRACT','DEPOSIT_CONTRACT',
        'METER_PHOTO','VEHICLE_PHOTO','MAINTENANCE','TICKET_ATTACHMENT','RECEIPT','OCR_INPUT','OTHER'
        )                           NOT NULL DEFAULT 'OTHER',
    is_sensitive    BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted_at      DATETIME(6)     NULL,
    KEY idx_files_tenant_category (category, created_at),
    KEY idx_files_owner (owner_user_id),
    CONSTRAINT fk_files_owner FOREIGN KEY (owner_user_id) REFERENCES users (id)
) ENGINE = InnoDB;

-- =========================================================
-- 2. PROPERTY / ROOM
-- =========================================================

CREATE TABLE properties
(
    id            BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    property_code VARCHAR(50)                                                                       NOT NULL,
    name          VARCHAR(255)                                                                      NOT NULL,
    property_type ENUM ('BOARDING_HOUSE','APARTMENT','WHOLE_HOUSE','MINI_APARTMENT','DORM','OTHER') NOT NULL DEFAULT 'BOARDING_HOUSE',
    address_line  VARCHAR(500)                                                                      NOT NULL,
    description   TEXT                                                                              NULL,
    status        ENUM ('ACTIVE','TEMP_CLOSED','CLOSED')                                            NOT NULL DEFAULT 'ACTIVE',
    created_at    DATETIME(6)                                                                       NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    DATETIME(6)                                                                       NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at    DATETIME(6)                                                                       NULL,
    version       INT UNSIGNED                                                                      NOT NULL DEFAULT 0,
    UNIQUE KEY uq_property_code (property_code),
    KEY idx_property_status (status)
) ENGINE = InnoDB;

CREATE TABLE property_images
(
    id          BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    property_id BIGINT UNSIGNED NOT NULL,
    file_id     BIGINT UNSIGNED NOT NULL,
    sort_order  INT             NOT NULL DEFAULT 0,
    created_at  DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    KEY idx_property_images (property_id),
    CONSTRAINT fk_property_images_property FOREIGN KEY (property_id) REFERENCES properties (id),
    CONSTRAINT fk_property_images_file FOREIGN KEY (file_id) REFERENCES file_metadata (id)
) ENGINE = InnoDB;

CREATE TABLE floors
(
    id          BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    property_id BIGINT UNSIGNED            NOT NULL,
    floor_code  VARCHAR(50)                NOT NULL,
    name        VARCHAR(100)               NOT NULL,
    sort_order  INT                        NOT NULL DEFAULT 0,
    status      ENUM ('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    created_at  DATETIME(6)                NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6)                NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at  DATETIME(6)                NULL,
    UNIQUE KEY uq_floor_code (property_id, floor_code),
    KEY idx_floor_property (property_id),
    CONSTRAINT fk_floor_property FOREIGN KEY (property_id) REFERENCES properties (id)
) ENGINE = InnoDB;

CREATE TABLE rooms
(
    id             BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    property_id    BIGINT UNSIGNED                                                             NOT NULL,
    floor_id       BIGINT UNSIGNED                                                             NOT NULL,
    room_code      VARCHAR(50)                                                                 NOT NULL,
    name           VARCHAR(100)                                                                NOT NULL,
    area_m2        DECIMAL(8, 2)                                                               NULL,
    listed_price   BIGINT UNSIGNED                                                             NOT NULL DEFAULT 0,
    current_status ENUM ('VACANT','RESERVED','OCCUPIED','SOON_VACANT','MAINTENANCE','EXPIRED') NOT NULL DEFAULT 'VACANT',
    max_occupants  TINYINT UNSIGNED                                                            NOT NULL DEFAULT 3,
    public_note    TEXT                                                                        NULL,
    internal_note  TEXT                                                                        NULL,
    position_x     INT                                                                         NULL,
    position_y     INT                                                                         NULL,
    sort_order     INT                                                                         NOT NULL DEFAULT 0,
    created_at     DATETIME(6)                                                                 NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at     DATETIME(6)                                                                 NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at     DATETIME(6)                                                                 NULL,
    version        BIGINT UNSIGNED                                                             NOT NULL DEFAULT 0,
    UNIQUE KEY uq_room_code (property_id, room_code),
    KEY idx_room_status (property_id, current_status),
    KEY idx_room_floor (floor_id),
    CONSTRAINT fk_room_property FOREIGN KEY (property_id) REFERENCES properties (id),
    CONSTRAINT fk_room_floor FOREIGN KEY (floor_id) REFERENCES floors (id)
) ENGINE = InnoDB;

CREATE TABLE room_status_display_configs
(
    id          BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    room_status ENUM ('VACANT','RESERVED','OCCUPIED','SOON_VACANT','MAINTENANCE','EXPIRED') NOT NULL,
    color_hex   VARCHAR(20)                                                                 NOT NULL,
    label       VARCHAR(100)                                                                NOT NULL,
    UNIQUE KEY uq_room_status_display (room_status)
) ENGINE = InnoDB;

CREATE TABLE room_status_history
(
    id          BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    room_id     BIGINT UNSIGNED                                                             NOT NULL,
    from_status ENUM ('VACANT','RESERVED','OCCUPIED','SOON_VACANT','MAINTENANCE','EXPIRED') NULL,
    to_status   ENUM ('VACANT','RESERVED','OCCUPIED','SOON_VACANT','MAINTENANCE','EXPIRED') NOT NULL,
    reason      VARCHAR(1000)                                                               NULL,
    changed_by  BIGINT UNSIGNED                                                             NULL,
    changed_at  DATETIME(6)                                                                 NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    KEY idx_room_status_history (room_id, changed_at),
    CONSTRAINT fk_rsh_room FOREIGN KEY (room_id) REFERENCES rooms (id),
    CONSTRAINT fk_rsh_user FOREIGN KEY (changed_by) REFERENCES users (id)
) ENGINE = InnoDB;

CREATE TABLE room_images
(
    id         BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    room_id    BIGINT UNSIGNED NOT NULL,
    file_id    BIGINT UNSIGNED NOT NULL,
    sort_order INT             NOT NULL DEFAULT 0,
    created_at DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    KEY idx_room_images (room_id),
    CONSTRAINT fk_room_img_room FOREIGN KEY (room_id) REFERENCES rooms (id),
    CONSTRAINT fk_room_img_file FOREIGN KEY (file_id) REFERENCES file_metadata (id)
) ENGINE = InnoDB;

CREATE TABLE room_assets
(
    id                BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    room_id           BIGINT UNSIGNED                              NOT NULL,
    asset_name        VARCHAR(255)                                 NOT NULL,
    asset_category    VARCHAR(100)                                 NULL,
    quantity          INT                                          NOT NULL DEFAULT 1,
    current_condition ENUM ('GOOD','ATTENTION','BROKEN','MISSING') NOT NULL DEFAULT 'GOOD',
    description       TEXT                                         NULL,
    image_file_id     BIGINT UNSIGNED                              NULL,
    created_at        DATETIME(6)                                  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at        DATETIME(6)                                  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at        DATETIME(6)                                  NULL,
    KEY idx_room_assets_room (room_id),
    CONSTRAINT fk_room_asset_room FOREIGN KEY (room_id) REFERENCES rooms (id),
    CONSTRAINT fk_room_asset_file FOREIGN KEY (image_file_id) REFERENCES file_metadata (id)
) ENGINE = InnoDB;

-- =========================================================
-- 3. MEMBERSHIP / PERMISSIONS
-- =========================================================

CREATE TABLE role_promotions
(
    id                  BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    user_id             BIGINT UNSIGNED                                 NOT NULL,
    role                ENUM ('MANAGER','ACCOUNTANT','TENANT')          NOT NULL,
    status              ENUM ('PENDING','ACTIVE','DISABLED','REJECTED') NOT NULL DEFAULT 'PENDING',
    property_id         BIGINT UNSIGNED                                 NULL,
    approved_at         DATETIME(6)                                     NULL,
    created_at          DATETIME(6)                                     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)                                     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at          DATETIME(6)                                     NULL,
    active_unique_token TINYINT GENERATED ALWAYS AS (IF(deleted_at IS NULL, 1, NULL)) VIRTUAL,
    UNIQUE KEY uq_membership_active (user_id, role, active_unique_token),
    KEY idx_membership_tenant_role (role, status),
    KEY idx_membership_property (property_id, role),
    CONSTRAINT fk_membership_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_membership_property FOREIGN KEY (property_id) REFERENCES properties (id)
) ENGINE = InnoDB;

CREATE TABLE permission_requests
(
    id                BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    requester_user_id BIGINT UNSIGNED                                                        NOT NULL,
    target_type       ENUM ('TENANT_PROFILE','IDENTITY_DOCUMENT','CONTRACT','REPORT','FILE') NOT NULL,
    target_id         BIGINT UNSIGNED                                                        NOT NULL,
    rejected_reason   VARCHAR(1000)                                                          NOT NULL,
    status            ENUM ('PENDING','APPROVED','REJECTED','EXPIRED','REVOKED')             NOT NULL DEFAULT 'PENDING',
    expires_at        DATETIME(6)                                                            NULL,
    decided_at        DATETIME(6)                                                            NULL,
    created_at        DATETIME(6)                                                            NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    KEY idx_perm_tenant_status (status, created_at),
    KEY idx_perm_target (target_type, target_id),
    CONSTRAINT fk_perm_requester FOREIGN KEY (requester_user_id) REFERENCES users (id)
) ENGINE = InnoDB;

-- =========================================================
-- 4. PERSON / TENANT PROFILE / DOCUMENT / VEHICLE
-- =========================================================

CREATE TABLE person_profiles
(
    id                BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    full_name         VARCHAR(255)                             NOT NULL,
    dob               DATE                                     NULL,
    gender            ENUM ('MALE','FEMALE','OTHER','UNKNOWN') NOT NULL DEFAULT 'UNKNOWN',
    phone             VARCHAR(30)                              NULL,
    email             VARCHAR(255)                             NULL,
    permanent_address VARCHAR(1000)                            NULL,
    portrait_file_id  BIGINT UNSIGNED                          NULL,
    created_at        DATETIME(6)                              NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at        DATETIME(6)                              NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at        DATETIME(6)                              NULL,
    KEY idx_pp_tenant_name_phone (full_name, phone),
    KEY idx_person_phone (phone),
    CONSTRAINT fk_pp_portrait FOREIGN KEY (portrait_file_id) REFERENCES file_metadata (id)
) ENGINE = InnoDB;

CREATE TABLE identity_documents
(
    id            BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    profile_id    BIGINT UNSIGNED                                NOT NULL,
    doc_type      ENUM ('CCCD','CMND','PASSPORT','OTHER')        NOT NULL DEFAULT 'CCCD',
    doc_number    VARCHAR(50)                                    NOT NULL,
    issued_date   DATE                                           NULL,
    issued_place  VARCHAR(255)                                   NULL,
    expiry_date   DATE                                           NULL,
    raw_ocr_data  BLOB                                           NULL,
    front_file_id BIGINT UNSIGNED                                NULL,
    back_file_id  BIGINT UNSIGNED                                NULL,
    status        ENUM ('ACTIVE','EXPIRED','REPLACED','INVALID') NOT NULL DEFAULT 'ACTIVE',
    created_at    DATETIME(6)                                    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    DATETIME(6)                                    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    UNIQUE KEY uq_doc_number_tenant (doc_type, doc_number),
    KEY idx_doc_profile (profile_id),
    CONSTRAINT fk_doc_profile FOREIGN KEY (profile_id) REFERENCES person_profiles (id),
    CONSTRAINT fk_doc_front FOREIGN KEY (front_file_id) REFERENCES file_metadata (id),
    CONSTRAINT fk_doc_back FOREIGN KEY (back_file_id) REFERENCES file_metadata (id)
) ENGINE = InnoDB;

CREATE TABLE vehicles
(
    id                  BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    profile_id          BIGINT UNSIGNED                                     NOT NULL,
    vehicle_type        ENUM ('MOTORBIKE','BICYCLE','CAR','E_BIKE','OTHER') NOT NULL DEFAULT 'MOTORBIKE',
    license_plate       VARCHAR(50)                                         NOT NULL,
    image_file_id       BIGINT UNSIGNED                                     NULL,
    status              ENUM ('ACTIVE','INACTIVE')                          NOT NULL DEFAULT 'ACTIVE',
    created_at          DATETIME(6)                                         NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted_at          DATETIME(6)                                         NULL,
    active_unique_token TINYINT GENERATED ALWAYS AS (IF(deleted_at IS NULL, 1, NULL)) VIRTUAL,
    UNIQUE KEY uq_vehicle_plate_active (license_plate, active_unique_token),
    KEY idx_vehicle_profile (status),
    CONSTRAINT fk_vehicle_file FOREIGN KEY (image_file_id) REFERENCES file_metadata (id),
    CONSTRAINT fk_vehicle_profile FOREIGN KEY (profile_id) REFERENCES person_profiles (id)
) ENGINE = InnoDB;

CREATE TABLE emergency_contacts
(
    id                BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    tenant_profile_id BIGINT UNSIGNED NOT NULL,
    full_name         VARCHAR(255)    NOT NULL,
    relationship      VARCHAR(100)    NOT NULL,
    phone             VARCHAR(30)     NOT NULL,
    created_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    KEY idx_emergency_profile (tenant_profile_id)
) ENGINE = InnoDB;

-- =========================================================
-- 5. CRM / LEADS / VIEWING
-- =========================================================

CREATE TABLE leads
(
    id                   BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    property_id          BIGINT UNSIGNED                                                                      NULL,
    assigned_user_id     BIGINT UNSIGNED                                                                      NULL,
    source               ENUM ('PHONE','WALK_IN','REFERRAL','OTHER')                                          NOT NULL DEFAULT 'OTHER',
    status               ENUM ('NEW','CONTACTED','VIEWING_SCHEDULED','VIEWED','DEPOSITED','LOST','CONVERTED') NOT NULL DEFAULT 'NEW',
    desired_move_in_date DATE                                                                                 NULL,
    note                 TEXT                                                                                 NULL,
    created_at           DATETIME(6)                                                                          NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at           DATETIME(6)                                                                          NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    KEY idx_lead_status (status, created_at),
    KEY idx_lead_assigned (assigned_user_id, status, created_at),
    CONSTRAINT fk_lead_property FOREIGN KEY (property_id) REFERENCES properties (id),
    CONSTRAINT fk_lead_assigned FOREIGN KEY (assigned_user_id) REFERENCES users (id)
) ENGINE = InnoDB;

-- CREATE TABLE lead_room_interests (
--   id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
--   lead_id BIGINT UNSIGNED NOT NULL,
--   room_id BIGINT UNSIGNED NOT NULL,
--   priority TINYINT UNSIGNED NOT NULL DEFAULT 1,
--   created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
--   UNIQUE KEY uq_lead_room (lead_id, room_id),
--   CONSTRAINT fk_lri_lead FOREIGN KEY (lead_id) REFERENCES leads(id),
--   CONSTRAINT fk_lri_room FOREIGN KEY (room_id) REFERENCES rooms(id)
-- ) ENGINE=InnoDB;

-- CREATE TABLE viewing_appointments (
--   id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
--   lead_id BIGINT UNSIGNED NOT NULL,
--   room_id BIGINT UNSIGNED NULL,
--   scheduled_at DATETIME(6) NOT NULL,
--   status ENUM('SCHEDULED','DONE','NO_SHOW','CANCELLED','RESCHEDULED') NOT NULL DEFAULT 'SCHEDULED',
--   handled_by BIGINT UNSIGNED NULL,
--   note TEXT NULL,
--   created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
--   KEY idx_viewing_schedule (scheduled_at, status),
--   CONSTRAINT fk_viewing_lead FOREIGN KEY (lead_id) REFERENCES leads(id),
--   CONSTRAINT fk_viewing_room FOREIGN KEY (room_id) REFERENCES rooms(id),
--   CONSTRAINT fk_viewing_handler FOREIGN KEY (handled_by) REFERENCES users(id)
-- ) ENGINE=InnoDB;

-- CREATE TABLE lead_activities (
--   id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
--   lead_id BIGINT UNSIGNED NOT NULL,
--   activity_type ENUM('CALL','ZALO','MESSAGE','VIEWING','NOTE','STATUS_CHANGE','OTHER') NOT NULL,
--   content TEXT NULL,
--   created_by BIGINT UNSIGNED NULL,
--   created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
--   KEY idx_lead_activities (lead_id, created_at),
--   CONSTRAINT fk_la_lead FOREIGN KEY (lead_id) REFERENCES leads(id),
--   CONSTRAINT fk_la_user FOREIGN KEY (created_by) REFERENCES users(id)
-- ) ENGINE=InnoDB;

-- =========================================================
-- 6. PROPERTY RULES / VIOLATIONS
-- =========================================================

CREATE TABLE property_rules
(
    id                  BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    property_id         BIGINT UNSIGNED            NOT NULL,
    rule_code           VARCHAR(50)                NOT NULL,
    title               VARCHAR(255)               NOT NULL,
    description         TEXT                       NOT NULL,
    default_fine_amount BIGINT UNSIGNED            NULL,
    sort_order          INT                        NOT NULL DEFAULT 0,
    status              ENUM ('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    created_at          DATETIME(6)                NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)                NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    UNIQUE KEY uq_property_rule_code (property_id, rule_code),
    KEY idx_property_rules (property_id, status),
    CONSTRAINT fk_property_rules_property FOREIGN KEY (property_id) REFERENCES properties (id)
) ENGINE = InnoDB;

-- =========================================================
-- 7. DEPOSIT / CONTRACT
-- =========================================================

CREATE TABLE room_holds
(
    id              BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    room_id         BIGINT UNSIGNED                                                        NOT NULL,
    tenant_id       BIGINT UNSIGNED                                                        NULL,
    status          ENUM ('ACTIVE','PAYMENT_PROCESSING','CONFIRMED','EXPIRED','CANCELLED') NOT NULL DEFAULT 'ACTIVE',
    expires_at      DATETIME(6)                                                            NOT NULL,
    created_at      DATETIME(6)                                                            NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    released_at     DATETIME(6)                                                            NULL,
    active_room_key BIGINT UNSIGNED GENERATED ALWAYS AS (IF(status IN ('ACTIVE', 'PAYMENT_PROCESSING'), room_id, NULL)) VIRTUAL,
    UNIQUE KEY uq_active_room_hold (active_room_key),
    KEY idx_room_hold_room (room_id, status, expires_at),
    CONSTRAINT fk_hold_room FOREIGN KEY (room_id) REFERENCES rooms (id),
    CONSTRAINT fk_hold_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id)
) ENGINE = InnoDB;

CREATE TABLE deposit_forms
(
    id                       BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    room_id                  BIGINT UNSIGNED                                 NOT NULL,
    id_number                VARCHAR(50)                                     NOT NULL,
    full_name                VARCHAR(255)                                    NOT NULL,
    email                    VARCHAR(255)                                    NOT NULL,
    phone                    VARCHAR(30)                                     NOT NULL,
    expected_move_in_date    DATE                                            NOT NULL,
    expected_lease_sign_date DATE                                            NOT NULL,
    payment_due_at           DATETIME(6)                                     NULL,
    deposit_expires_at       DATE                                            NULL,
    status                   ENUM ('APPROVAL_PENDING','APPROVED','REJECTED') NOT NULL DEFAULT 'APPROVAL_PENDING',
    confirmed_at             DATETIME(6)                                     NULL,
    reject_reason            TEXT                                            NULL,
    created_at               DATETIME(6)                                     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    KEY idx_deposit_room_status (room_id, status),
    CONSTRAINT fk_dep_form_room FOREIGN KEY (room_id) REFERENCES rooms (id)
) ENGINE = InnoDB;

CREATE TABLE deposit_agreements
(
    id                          BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    deposit_code                VARCHAR(80)                                                                                                            NOT NULL,
    room_id                     BIGINT UNSIGNED                                                                                                        NOT NULL,
    tenant_id                   BIGINT UNSIGNED                                                                                                        NULL,
    depositor_person_profile_id BIGINT UNSIGNED                                                                                                        NULL,
    amount                      BIGINT UNSIGNED                                                                                                        NOT NULL,
    expected_move_in_date       DATE                                                                                                                   NOT NULL,
    expected_lease_sign_date    DATE                                                                                                                   NOT NULL,
    payment_due_at              DATETIME(6)                                                                                                            NULL,
    deposit_expires_at          DATE                                                                                                                   NULL,
    extension_count             TINYINT UNSIGNED                                                                                                       NOT NULL DEFAULT 0,
    max_extensions              TINYINT UNSIGNED                                                                                                       NOT NULL DEFAULT 1,
    status                      ENUM ('DRAFT','PENDING_PAYMENT','PAID','CONFIRMED','CONVERTED_TO_LEASE','EXTENDED','REFUNDED','FORFEITED','CANCELLED') NOT NULL DEFAULT 'DRAFT',
    confirmed_at                DATETIME(6)                                                                                                            NULL,
    contract_file_id            BIGINT UNSIGNED                                                                                                        NULL,
    note                        TEXT                                                                                                                   NULL,
    forfeiture_reason           TEXT                                                                                                                   NULL,
    refunded_amount             BIGINT UNSIGNED                                                                                                        NULL,
    created_at                  DATETIME(6)                                                                                                            NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at                  DATETIME(6)                                                                                                            NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    UNIQUE KEY uq_deposit_code (deposit_code),
    KEY idx_deposit_room_status (room_id, status),
    KEY idx_deposit_person (depositor_person_profile_id, status),
    CONSTRAINT fk_dep_agreement_room FOREIGN KEY (room_id) REFERENCES rooms (id),
    CONSTRAINT fk_dep_agreement_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_dep_agreement_person_profile FOREIGN KEY (depositor_person_profile_id) REFERENCES person_profiles (id),
    CONSTRAINT fk_dep_agreement_file FOREIGN KEY (contract_file_id) REFERENCES file_metadata (id)
) ENGINE = InnoDB;

CREATE TABLE deposit_extension_events
(
    id                        BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    deposit_agreement_id      BIGINT UNSIGNED NOT NULL,
    old_expected_move_in_date DATE            NOT NULL,
    new_expected_move_in_date DATE            NOT NULL,
    old_expires_at            DATE            NULL,
    new_expires_at            DATE            NOT NULL,
    reason                    TEXT            NULL,
    approved_by               BIGINT UNSIGNED NOT NULL,
    approved_at               DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    KEY idx_deposit_extension (deposit_agreement_id, approved_at),
    CONSTRAINT fk_dee_deposit FOREIGN KEY (deposit_agreement_id) REFERENCES deposit_agreements (id),
    CONSTRAINT fk_dee_user FOREIGN KEY (approved_by) REFERENCES users (id)
) ENGINE = InnoDB;

CREATE TABLE lease_contracts
(
    id                        BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    contract_code             VARCHAR(80)                                                                                                                                      NOT NULL,
    room_id                   BIGINT UNSIGNED                                                                                                                                  NOT NULL,
    deposit_agreement_id      BIGINT UNSIGNED                                                                                                                                  NULL,
    primary_tenant_profile_id BIGINT UNSIGNED                                                                                                                                  NOT NULL,
    start_date                DATE                                                                                                                                             NOT NULL,
    end_date                  DATE                                                                                                                                             NOT NULL,
    rent_start_date           DATE                                                                                                                                             NOT NULL,
    monthly_rent              BIGINT UNSIGNED                                                                                                                                  NOT NULL,
    payment_cycle_months      TINYINT UNSIGNED                                                                                                                                 NOT NULL,
    deposit_amount            BIGINT UNSIGNED                                                                                                                                  NOT NULL DEFAULT 0,
    status                    ENUM ('DRAFT','PENDING_SIGNATURE','ACTIVE','EXPIRING_SOON','EXPIRED','TERMINATION_PENDING','LIQUIDATED','RENEWED','AUTO_TERMINATED','CANCELLED') NOT NULL DEFAULT 'DRAFT',
    previous_contract_id      BIGINT UNSIGNED                                                                                                                                  NULL,
    contract_file_id          BIGINT UNSIGNED                                                                                                                                  NULL,
    signed_at                 DATETIME(6)                                                                                                                                      NULL,
    created_by                BIGINT UNSIGNED                                                                                                                                  NULL,
    created_at                DATETIME(6)                                                                                                                                      NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at                DATETIME(6)                                                                                                                                      NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at                DATETIME(6)                                                                                                                                      NULL,
    version                   BIGINT UNSIGNED                                                                                                                                           DEFAULT 0 NOT NULL DEFAULT 0,
    UNIQUE KEY uq_contract_code (contract_code),
    KEY idx_contract_room_status (room_id, status),
    KEY idx_contract_end_date (end_date, status),
    KEY idx_contract_primary_profile (primary_tenant_profile_id, status),
    CONSTRAINT chk_payment_cycle CHECK (payment_cycle_months IN (1, 3)),
    CONSTRAINT fk_lc_room FOREIGN KEY (room_id) REFERENCES rooms (id),
    CONSTRAINT fk_lc_deposit FOREIGN KEY (deposit_agreement_id) REFERENCES deposit_agreements (id),
    CONSTRAINT fk_lc_prev FOREIGN KEY (previous_contract_id) REFERENCES lease_contracts (id),
    CONSTRAINT fk_lc_file FOREIGN KEY (contract_file_id) REFERENCES file_metadata (id),
    CONSTRAINT fk_lc_created_by FOREIGN KEY (created_by) REFERENCES tenants (id)
) ENGINE = InnoDB;

CREATE TABLE contract_occupants
(
    id            BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    contract_id   BIGINT UNSIGNED                NOT NULL,
    tenant_id     BIGINT UNSIGNED                NOT NULL,
    occupant_role ENUM ('PRIMARY','CO_OCCUPANT') NOT NULL DEFAULT 'CO_OCCUPANT',
    move_in_date  DATE                           NOT NULL,
    move_out_date DATE                           NULL,
    status        ENUM ('ACTIVE','MOVED_OUT')    NOT NULL DEFAULT 'ACTIVE',
    created_at    DATETIME(6)                    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uq_contract_occupant (contract_id),
    KEY idx_occupant_contract_status (contract_id, status),
    KEY idx_occupant_profile_status (status),
    CONSTRAINT fk_co_contract FOREIGN KEY (contract_id) REFERENCES lease_contracts (id),
    CONSTRAINT fk_co_tenant FOREIGN KEY (contract_id) REFERENCES tenants (id)
) ENGINE = InnoDB;

CREATE TABLE contract_events
(
    id          BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    contract_id BIGINT UNSIGNED                                                                                                                                                  NOT NULL,
    event_type  ENUM ('CREATED','SIGNED','RENEWED','NOTICE_SENT','INTENTION_RECORDED','EXPIRED','LIQUIDATED','AUTO_TERMINATED','PRICE_CHANGED','OCCUPANT_CHANGED','TRANSFERRED') NOT NULL,
    event_data  BLOB                                                                                                                                                             NULL,
    created_by  BIGINT UNSIGNED                                                                                                                                                  NULL,
    created_at  DATETIME(6)                                                                                                                                                      NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    KEY idx_contract_events (contract_id, created_at),
    CONSTRAINT fk_ce_contract FOREIGN KEY (contract_id) REFERENCES lease_contracts (id),
    CONSTRAINT fk_ce_user FOREIGN KEY (created_by) REFERENCES users (id)
) ENGINE = InnoDB;

CREATE TABLE contract_termination_notices
(
    id                        BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    contract_id               BIGINT UNSIGNED                                                  NOT NULL,
    notice_by                 ENUM ('LANDLORD','TENANT','MANAGER','SYSTEM')                    NOT NULL,
    notice_user_id            BIGINT UNSIGNED                                                  NULL,
    notice_date               DATE                                                             NOT NULL,
    expected_termination_date DATE                                                             NOT NULL,
    reason                    TEXT                                                             NULL,
    evidence_file_id          BIGINT UNSIGNED                                                  NULL,
    status                    ENUM ('SUBMITTED','ACCEPTED','REJECTED','WITHDRAWN','COMPLETED') NOT NULL DEFAULT 'SUBMITTED',
    decided_by                BIGINT UNSIGNED                                                  NULL,
    decided_at                DATETIME(6)                                                      NULL,
    created_at                DATETIME(6)                                                      NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    KEY idx_ctn_contract (contract_id, status),
    CONSTRAINT fk_ctn_contract FOREIGN KEY (contract_id) REFERENCES lease_contracts (id),
    CONSTRAINT fk_ctn_user FOREIGN KEY (notice_user_id) REFERENCES users (id),
    CONSTRAINT fk_ctn_file FOREIGN KEY (evidence_file_id) REFERENCES file_metadata (id),
    CONSTRAINT fk_ctn_decided FOREIGN KEY (decided_by) REFERENCES users (id)
) ENGINE = InnoDB;

CREATE TABLE rent_overrides
(
    id                    BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    contract_id           BIGINT UNSIGNED NOT NULL,
    billing_period        CHAR(7)         NOT NULL,
    override_monthly_rent BIGINT UNSIGNED NOT NULL,
    reason                VARCHAR(1000)   NOT NULL,
    approved_by           BIGINT UNSIGNED NOT NULL,
    created_at            DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uq_rent_override (contract_id, billing_period),
    CONSTRAINT fk_ro_contract FOREIGN KEY (contract_id) REFERENCES lease_contracts (id),
    CONSTRAINT fk_ro_approver FOREIGN KEY (approved_by) REFERENCES users (id)
) ENGINE = InnoDB;

-- =========================================================
-- 8. UTILITY METERING
-- =========================================================

CREATE TABLE utility_tariffs
(
    id                                      BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    property_id                             BIGINT UNSIGNED                            NULL,
    utility_type                            ENUM ('ELECTRICITY','WATER','SERVICE_FEE') NOT NULL,
    unit_price                              BIGINT UNSIGNED                            NOT NULL,
    free_allowance                          BIGINT UNSIGNED                            NOT NULL DEFAULT 0,
    service_fee_waive_electricity_threshold BIGINT UNSIGNED                            NULL,
    effective_from                          DATE                                       NOT NULL,
    effective_to                            DATE                                       NULL,
    created_by                              BIGINT UNSIGNED                            NULL,
    created_at                              DATETIME(6)                                NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    KEY idx_tariff_effective (property_id, utility_type, effective_from),
    CONSTRAINT fk_tariff_property FOREIGN KEY (property_id) REFERENCES properties (id),
    CONSTRAINT fk_tariff_user FOREIGN KEY (created_by) REFERENCES users (id)
) ENGINE = InnoDB;

CREATE TABLE meters
(
    id               BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    room_id          BIGINT UNSIGNED                                NOT NULL,
    meter_type       ENUM ('ELECTRICITY','WATER')                   NOT NULL,
    meter_code       VARCHAR(100)                                   NULL,
    status           ENUM ('ACTIVE','REPLACED','BROKEN','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    installed_at     DATE                                           NULL,
    created_at       DATETIME(6)                                    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    active_meter_key VARCHAR(255) GENERATED ALWAYS AS (IF(status = 'ACTIVE', CONCAT(room_id, ':', meter_type), NULL)) VIRTUAL,
    UNIQUE KEY uq_room_active_meter_type (active_meter_key),
    KEY idx_meter_room (room_id),
    CONSTRAINT fk_meter_room FOREIGN KEY (room_id) REFERENCES rooms (id)
) ENGINE = InnoDB;

CREATE TABLE meter_reading_batches
(
    id               BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    property_id      BIGINT UNSIGNED                                                    NOT NULL,
    reading_period   CHAR(7)                                                            NOT NULL,
    source           ENUM ('EXCEL_IMPORT','MANUAL','HANDOVER','TRANSFER','LIQUIDATION') NOT NULL,
    status           ENUM ('DRAFT','PREVIEWED','CONFIRMED','CANCELLED')                 NOT NULL DEFAULT 'DRAFT',
    imported_file_id BIGINT UNSIGNED                                                    NULL,
    created_by       BIGINT UNSIGNED                                                    NULL,
    confirmed_by     BIGINT UNSIGNED                                                    NULL,
    confirmed_at     DATETIME(6)                                                        NULL,
    created_at       DATETIME(6)                                                        NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    KEY idx_reading_batch (property_id, reading_period, status),
    CONSTRAINT fk_mrb_property FOREIGN KEY (property_id) REFERENCES properties (id),
    CONSTRAINT fk_mrb_file FOREIGN KEY (imported_file_id) REFERENCES file_metadata (id),
    CONSTRAINT fk_mrb_created FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_mrb_confirmed FOREIGN KEY (confirmed_by) REFERENCES users (id)
) ENGINE = InnoDB;

CREATE TABLE meter_reading_import_rows
(
    id                 BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    batch_id           BIGINT UNSIGNED                  NOT NULL,
    room_code          VARCHAR(50)                      NOT NULL,
    meter_type         ENUM ('ELECTRICITY','WATER')     NOT NULL,
    previous_value     DECIMAL(12, 3)                   NULL,
    current_value      DECIMAL(12, 3)                   NULL,
    validation_status  ENUM ('VALID','WARNING','ERROR') NOT NULL DEFAULT 'VALID',
    validation_message TEXT                             NULL,
    created_at         DATETIME(6)                      NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    KEY idx_mrir_batch (batch_id),
    CONSTRAINT fk_mrir_batch FOREIGN KEY (batch_id) REFERENCES meter_reading_batches (id)
) ENGINE = InnoDB;

CREATE TABLE meter_readings
(
    id                 BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    batch_id           BIGINT UNSIGNED                                                    NULL,
    meter_id           BIGINT UNSIGNED                                                    NOT NULL,
    room_id            BIGINT UNSIGNED                                                    NOT NULL,
    reading_period     CHAR(7)                                                            NOT NULL,
    revision_no        INT UNSIGNED                                                       NOT NULL DEFAULT 1,
    previous_value     DECIMAL(12, 3)                                                     NOT NULL,
    current_value      DECIMAL(12, 3)                                                     NOT NULL,
    usage_amount       DECIMAL(12, 3) GENERATED ALWAYS AS (current_value - previous_value) STORED,
    reading_date       DATE                                                               NOT NULL,
    photo_file_id      BIGINT UNSIGNED                                                    NULL,
    source             ENUM ('EXCEL_IMPORT','MANUAL','HANDOVER','TRANSFER','LIQUIDATION') NOT NULL,
    status             ENUM ('CONFIRMED','VOIDED')                                        NOT NULL DEFAULT 'CONFIRMED',
    void_reason        VARCHAR(1000)                                                      NULL,
    created_by         BIGINT UNSIGNED                                                    NULL,
    created_at         DATETIME(6)                                                        NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    active_reading_key VARCHAR(255) GENERATED ALWAYS AS (IF(status <> 'VOIDED', CONCAT(meter_id, ':', reading_period), NULL)) VIRTUAL,
    UNIQUE KEY uq_meter_period_active (active_reading_key),
    UNIQUE KEY uq_meter_period_revision (meter_id, reading_period, revision_no),
    KEY idx_reading_room_period (room_id, reading_period),
    CONSTRAINT chk_meter_index CHECK (current_value >= previous_value),
    CONSTRAINT fk_mr_batch FOREIGN KEY (batch_id) REFERENCES meter_reading_batches (id),
    CONSTRAINT fk_mr_meter FOREIGN KEY (meter_id) REFERENCES meters (id),
    CONSTRAINT fk_mr_room FOREIGN KEY (room_id) REFERENCES rooms (id),
    CONSTRAINT fk_mr_photo FOREIGN KEY (photo_file_id) REFERENCES file_metadata (id),
    CONSTRAINT fk_mr_user FOREIGN KEY (created_by) REFERENCES users (id)
) ENGINE = InnoDB;

CREATE TABLE meter_reading_anomalies
(
    id               BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    meter_reading_id BIGINT UNSIGNED                                                NOT NULL,
    anomaly_type     ENUM ('HIGH_USAGE','NEGATIVE_USAGE','MISSING_READING','OTHER') NOT NULL,
    message          TEXT                                                           NOT NULL,
    severity         ENUM ('LOW','MEDIUM','HIGH')                                   NOT NULL DEFAULT 'MEDIUM',
    resolved_at      DATETIME(6)                                                    NULL,
    resolved_by      BIGINT UNSIGNED                                                NULL,
    created_at       DATETIME(6)                                                    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    KEY idx_mra_reading (meter_reading_id),
    CONSTRAINT fk_mra_reading FOREIGN KEY (meter_reading_id) REFERENCES meter_readings (id),
    CONSTRAINT fk_mra_resolved_by FOREIGN KEY (resolved_by) REFERENCES users (id)
) ENGINE = InnoDB;

-- =========================================================
-- 9. BILLING / PAYMENT / ACCOUNTING
-- =========================================================

CREATE TABLE collection_accounts
(
    id             BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    property_id    BIGINT UNSIGNED                                         NULL,
    account_type   ENUM ('RENT','UTILITY','DEPOSIT','OPERATING','GENERAL') NOT NULL,
    bank_name      VARCHAR(100)                                            NULL,
    account_number VARCHAR(100)                                            NULL,
    account_holder VARCHAR(255)                                            NULL,
    provider       ENUM ('BANK','MOMO','ZALOPAY','CASH','OTHER')           NOT NULL DEFAULT 'BANK',
    status         ENUM ('ACTIVE','INACTIVE')                              NOT NULL DEFAULT 'ACTIVE',
    created_at     DATETIME(6)                                             NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uq_collection_account (provider, account_number, account_type),
    KEY idx_collection_account_scope (property_id, account_type, status),
    CONSTRAINT fk_ca_property FOREIGN KEY (property_id) REFERENCES properties (id)
) ENGINE = InnoDB;

CREATE TABLE invoices
(
    id                    BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    invoice_code          VARCHAR(80)                                                                                                                 NOT NULL,
    property_id           BIGINT UNSIGNED                                                                                                             NOT NULL,
    room_id               BIGINT UNSIGNED                                                                                                             NULL,
    contract_id           BIGINT UNSIGNED                                                                                                             NULL,
    invoice_type          ENUM ('RENT','UTILITY','DEPOSIT','FINAL_SETTLEMENT','COMPENSATION','OPERATING_REIMBURSEMENT','TRANSFER_DIFFERENCE','OTHER') NOT NULL,
    revision_no           INT UNSIGNED                                                                                                                NOT NULL DEFAULT 1,
    billing_period        CHAR(7)                                                                                                                     NULL,
    issue_date            DATE                                                                                                                        NOT NULL,
    due_date              DATE                                                                                                                        NOT NULL,
    status                ENUM ('DRAFT','ISSUED','PARTIALLY_PAID','PAID','OVERDUE','VOIDED')                                                          NOT NULL DEFAULT 'DRAFT',
    currency              CHAR(3)                                                                                                                     NOT NULL DEFAULT 'VND',
    subtotal_amount       DECIMAL(15, 2)                                                                                                              NOT NULL DEFAULT 0,
    discount_amount       DECIMAL(15, 2)                                                                                                              NOT NULL DEFAULT 0,
    total_amount          DECIMAL(15, 2)                                                                                                              NOT NULL DEFAULT 0,
    paid_amount           DECIMAL(15, 2)                                                                                                              NOT NULL DEFAULT 0,
    remaining_amount      DECIMAL(15, 2)                                                                                                              NOT NULL DEFAULT 0,
    collection_account_id BIGINT UNSIGNED                                                                                                             NULL,
    created_by            BIGINT UNSIGNED                                                                                                             NULL,
    issued_at             DATETIME(6)                                                                                                                 NULL,
    voided_at             DATETIME(6)                                                                                                                 NULL,
    void_reason           VARCHAR(1000)                                                                                                               NULL,
    created_at            DATETIME(6)                                                                                                                 NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at            DATETIME(6)                                                                                                                 NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    version               INT UNSIGNED                                                                                                                NOT NULL DEFAULT 0,
    active_invoice_key    VARCHAR(255) GENERATED ALWAYS AS (
        IF(status <> 'VOIDED' AND contract_id IS NOT NULL AND billing_period IS NOT NULL,
           CONCAT(contract_id, ':', billing_period, ':', invoice_type), NULL)
        ) VIRTUAL,
    UNIQUE KEY uq_invoice_code (invoice_code),
    UNIQUE KEY uq_invoice_contract_period_type_rev (contract_id, billing_period, invoice_type, revision_no),
    UNIQUE KEY uq_invoice_active_key (active_invoice_key),
    KEY idx_invoice_room_status (room_id, status, due_date),
    KEY idx_invoice_contract (contract_id),
    KEY idx_invoice_overdue (status, due_date),
    CONSTRAINT fk_inv_property FOREIGN KEY (property_id) REFERENCES properties (id),
    CONSTRAINT fk_inv_room FOREIGN KEY (room_id) REFERENCES rooms (id),
    CONSTRAINT fk_inv_contract FOREIGN KEY (contract_id) REFERENCES lease_contracts (id),
    CONSTRAINT fk_inv_account FOREIGN KEY (collection_account_id) REFERENCES collection_accounts (id),
    CONSTRAINT fk_inv_user FOREIGN KEY (created_by) REFERENCES users (id)
) ENGINE = InnoDB;

CREATE TABLE invoice_lines
(
    id                    BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    invoice_id            BIGINT UNSIGNED                                                                                                                                                          NOT NULL,
    line_type             ENUM ('ROOM_RENT','ELECTRICITY','WATER','SERVICE_FEE','MAINTENANCE_COMPENSATION','VIOLATION_FINE','TRANSFER_DIFFERENCE','DEPOSIT_DEDUCTION','MANUAL_ADJUSTMENT','OTHER') NOT NULL,
    description           VARCHAR(1000)                                                                                                                                                            NOT NULL,
    quantity              INT UNSIGNED                                                                                                                                                             NOT NULL DEFAULT 1,
    unit_price            BIGINT UNSIGNED                                                                                                                                                          NOT NULL DEFAULT 0,
    amount                BIGINT UNSIGNED GENERATED ALWAYS AS (quantity * unit_price) STORED,
    meter_reading_id      BIGINT UNSIGNED                                                                                                                                                          NULL,
    source_type           VARCHAR(100)                                                                                                                                                             NULL,
    source_id             BIGINT UNSIGNED                                                                                                                                                          NULL,
    collection_account_id BIGINT UNSIGNED                                                                                                                                                          NULL,
    created_at            DATETIME(6)                                                                                                                                                              NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    KEY idx_invoice_lines_invoice (invoice_id),
    KEY idx_invoice_lines_source (source_type, source_id),
    CONSTRAINT fk_il_invoice FOREIGN KEY (invoice_id) REFERENCES invoices (id),
    CONSTRAINT fk_il_meter_reading FOREIGN KEY (meter_reading_id) REFERENCES meter_readings (id),
    CONSTRAINT fk_il_collection_account FOREIGN KEY (collection_account_id) REFERENCES collection_accounts (id)
) ENGINE = InnoDB;

CREATE TABLE invoice_payment_groups
(
    id                    BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    invoice_id            BIGINT UNSIGNED                                          NOT NULL,
    collection_account_id BIGINT UNSIGNED                                          NOT NULL,
    group_type            ENUM ('RENT','UTILITY','DEPOSIT','COMPENSATION','OTHER') NOT NULL,
    amount                BIGINT UNSIGNED                                          NOT NULL,
    payment_intent_id     BIGINT UNSIGNED                                          NULL,
    status                ENUM ('PENDING','PARTIALLY_PAID','PAID','CANCELLED')     NOT NULL DEFAULT 'PENDING',
    created_at            DATETIME(6)                                              NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uq_invoice_payment_group (invoice_id, collection_account_id, group_type),
    CONSTRAINT fk_ipg_invoice FOREIGN KEY (invoice_id) REFERENCES invoices (id),
    CONSTRAINT fk_ipg_account FOREIGN KEY (collection_account_id) REFERENCES collection_accounts (id)
) ENGINE = InnoDB;

CREATE TABLE payment_intents
(
    id                       BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    invoice_id               BIGINT UNSIGNED                                                       NULL,
    deposit_agreement_id     BIGINT UNSIGNED                                                       NULL,
    invoice_payment_group_id BIGINT UNSIGNED                                                       NULL,
    amount                   BIGINT UNSIGNED                                                       NOT NULL,
    provider                 ENUM ('VIETQR','MOMO','ZALOPAY','CASH','BANK_TRANSFER')               NOT NULL,
    collection_account_id    BIGINT UNSIGNED                                                       NULL,
    payment_content          VARCHAR(255)                                                          NOT NULL,
    qr_payload               TEXT                                                                  NULL,
    status                   ENUM ('CREATED','PENDING','SUCCEEDED','FAILED','EXPIRED','CANCELLED') NOT NULL DEFAULT 'CREATED',
    expires_at               DATETIME(6)                                                           NULL,
    created_at               DATETIME(6)                                                           NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uq_payment_content (payment_content),
    KEY idx_pi_invoice (invoice_id),
    CONSTRAINT fk_pi_invoice FOREIGN KEY (invoice_id) REFERENCES invoices (id),
    CONSTRAINT fk_pi_deposit FOREIGN KEY (deposit_agreement_id) REFERENCES deposit_agreements (id),
    CONSTRAINT fk_pi_ipg FOREIGN KEY (invoice_payment_group_id) REFERENCES invoice_payment_groups (id),
    CONSTRAINT fk_pi_account FOREIGN KEY (collection_account_id) REFERENCES collection_accounts (id)
) ENGINE = InnoDB;

ALTER TABLE invoice_payment_groups
    ADD CONSTRAINT fk_ipg_intent FOREIGN KEY (payment_intent_id) REFERENCES payment_intents (id);

CREATE TABLE payment_transactions
(
    id                      BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    provider                ENUM ('BANK','MOMO','ZALOPAY','CASH','MANUAL')                                                           NOT NULL,
    provider_transaction_id VARCHAR(255)                                                                                             NULL,
    collection_account_id   BIGINT UNSIGNED                                                                                          NULL,
    amount                  BIGINT UNSIGNED                                                                                          NOT NULL,
    transaction_time        DATETIME(6)                                                                                              NOT NULL,
    payer_name              VARCHAR(255)                                                                                             NULL,
    payer_account           VARCHAR(255)                                                                                             NULL,
    content                 VARCHAR(1000)                                                                                            NULL,
    status                  ENUM ('PENDING_RECONCILE','MATCHED','PARTIALLY_ALLOCATED','ALLOCATED','DUPLICATE','REJECTED','REFUNDED') NOT NULL DEFAULT 'PENDING_RECONCILE',
    raw_payload             BLOB                                                                                                     NULL,
    confirmed_by            BIGINT UNSIGNED                                                                                          NULL,
    confirmed_at            DATETIME(6)                                                                                              NULL,
    created_at              DATETIME(6)                                                                                              NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uq_provider_txn (provider, provider_transaction_id),
    KEY idx_payment_txn_status (status, transaction_time),
    KEY idx_payment_reconcile (status, amount, transaction_time),
    KEY idx_payment_content (content(100)),
    CONSTRAINT fk_pt_account FOREIGN KEY (collection_account_id) REFERENCES collection_accounts (id),
    CONSTRAINT fk_pt_user FOREIGN KEY (confirmed_by) REFERENCES users (id)
) ENGINE = InnoDB;

CREATE TABLE payment_allocations
(
    id                     BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    payment_transaction_id BIGINT UNSIGNED NOT NULL,
    invoice_id             BIGINT UNSIGNED NOT NULL,
    amount                 BIGINT UNSIGNED NOT NULL,
    allocated_by           BIGINT UNSIGNED NULL,
    allocated_at           DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uq_payment_invoice_alloc (payment_transaction_id, invoice_id),
    KEY idx_alloc_invoice (invoice_id),
    CONSTRAINT fk_pa_payment FOREIGN KEY (payment_transaction_id) REFERENCES payment_transactions (id),
    CONSTRAINT fk_pa_invoice FOREIGN KEY (invoice_id) REFERENCES invoices (id),
    CONSTRAINT fk_pa_user FOREIGN KEY (allocated_by) REFERENCES users (id)
) ENGINE = InnoDB;

CREATE TABLE debt_snapshots
(
    id                  BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    room_id             BIGINT UNSIGNED NOT NULL,
    contract_id         BIGINT UNSIGNED NULL,
    snapshot_date       DATE            NOT NULL,
    rent_debt_amount    BIGINT UNSIGNED NOT NULL DEFAULT 0,
    utility_debt_amount BIGINT UNSIGNED NOT NULL DEFAULT 0,
    other_debt_amount   BIGINT UNSIGNED NOT NULL DEFAULT 0,
    rent_debt_months    INT             NOT NULL DEFAULT 0,
    utility_debt_months INT             NOT NULL DEFAULT 0,
    mixed_debt_amount   BIGINT UNSIGNED NOT NULL DEFAULT 0,
    debt_limit_amount   BIGINT UNSIGNED NULL,
    is_over_limit       BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uq_debt_snapshot (room_id, snapshot_date),
    KEY idx_debt_over_limit (is_over_limit, snapshot_date),
    KEY idx_debt_contract (contract_id, snapshot_date),
    CONSTRAINT fk_ds_room FOREIGN KEY (room_id) REFERENCES rooms (id),
    CONSTRAINT fk_ds_contract FOREIGN KEY (contract_id) REFERENCES lease_contracts (id)
) ENGINE = InnoDB;

CREATE TABLE ledger_entries
(
    id                BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    entry_code        VARCHAR(100)                                                                    NOT NULL,
    entry_date        DATE                                                                            NOT NULL,
    source_type       ENUM ('INVOICE','PAYMENT','REFUND','EXPENSE','DEPOSIT_SETTLEMENT','ADJUSTMENT') NOT NULL,
    source_id         BIGINT UNSIGNED                                                                 NOT NULL,
    account_code      VARCHAR(50)                                                                     NOT NULL,
    debit_amount      BIGINT UNSIGNED                                                                 NOT NULL DEFAULT 0,
    credit_amount     BIGINT UNSIGNED                                                                 NOT NULL DEFAULT 0,
    description       VARCHAR(1000)                                                                   NULL,
    posted_at         DATETIME(6)                                                                     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    reversed_entry_id BIGINT UNSIGNED                                                                 NULL,
    UNIQUE KEY uq_ledger_entry_code (entry_code),
    KEY idx_ledger_source (source_type, source_id),
    KEY idx_ledger_date (entry_date),
    CONSTRAINT chk_ledger_nonzero CHECK ((debit_amount > 0 AND credit_amount = 0) OR
                                         (credit_amount > 0 AND debit_amount = 0)),
    CONSTRAINT fk_le_reversal FOREIGN KEY (reversed_entry_id) REFERENCES ledger_entries (id)
) ENGINE = InnoDB;

-- =========================================================
-- 10. LIQUIDATION / HANDOVER / RULE VIOLATION
-- =========================================================

CREATE TABLE contract_liquidations
(
    id                       BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    contract_id              BIGINT UNSIGNED                        NOT NULL,
    liquidation_date         DATE                                   NOT NULL,
    reason                   VARCHAR(1000)                          NOT NULL,
    deposit_amount           BIGINT UNSIGNED                        NOT NULL DEFAULT 0,
    deposit_deduction_amount BIGINT UNSIGNED                        NOT NULL DEFAULT 0,
    deposit_deduction_reason TEXT                                   NULL,
    deposit_refund_amount    BIGINT UNSIGNED                        NOT NULL DEFAULT 0,
    final_invoice_id         BIGINT UNSIGNED                        NULL,
    signed_file_id           BIGINT UNSIGNED                        NULL,
    status                   ENUM ('DRAFT','CONFIRMED','CANCELLED') NOT NULL DEFAULT 'DRAFT',
    created_by               BIGINT UNSIGNED                        NULL,
    created_at               DATETIME(6)                            NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uq_liquidation_contract (contract_id),
    CONSTRAINT fk_cl_contract FOREIGN KEY (contract_id) REFERENCES lease_contracts (id),
    CONSTRAINT fk_cl_invoice FOREIGN KEY (final_invoice_id) REFERENCES invoices (id),
    CONSTRAINT fk_cl_file FOREIGN KEY (signed_file_id) REFERENCES file_metadata (id),
    CONSTRAINT fk_cl_user FOREIGN KEY (created_by) REFERENCES users (id)
) ENGINE = InnoDB;

CREATE TABLE contract_handover_records
(
    id                     BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    contract_id            BIGINT UNSIGNED                                          NOT NULL,
    room_id                BIGINT UNSIGNED                                          NOT NULL,
    handover_type          ENUM ('MOVE_IN','MOVE_OUT','TRANSFER_OUT','TRANSFER_IN') NOT NULL,
    handover_date          DATETIME(6)                                              NOT NULL,
    electricity_reading_id BIGINT UNSIGNED                                          NULL,
    water_reading_id       BIGINT UNSIGNED                                          NULL,
    note                   TEXT                                                     NULL,
    status                 ENUM ('DRAFT','CONFIRMED','CANCELLED')                   NOT NULL DEFAULT 'DRAFT',
    confirmed_by           BIGINT UNSIGNED                                          NULL,
    confirmed_at           DATETIME(6)                                              NULL,
    created_at             DATETIME(6)                                              NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    KEY idx_chr_contract (contract_id, handover_type),
    KEY idx_chr_room (room_id, handover_date),
    CONSTRAINT fk_chr_contract FOREIGN KEY (contract_id) REFERENCES lease_contracts (id),
    CONSTRAINT fk_chr_room FOREIGN KEY (room_id) REFERENCES rooms (id),
    CONSTRAINT fk_chr_electricity_reading FOREIGN KEY (electricity_reading_id) REFERENCES meter_readings (id),
    CONSTRAINT fk_chr_water_reading FOREIGN KEY (water_reading_id) REFERENCES meter_readings (id),
    CONSTRAINT fk_chr_confirmed_by FOREIGN KEY (confirmed_by) REFERENCES users (id)
) ENGINE = InnoDB;

CREATE TABLE contract_handover_items
(
    id                      BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    handover_record_id      BIGINT UNSIGNED                              NOT NULL,
    room_asset_id           BIGINT UNSIGNED                              NULL,
    asset_name              VARCHAR(255)                                 NOT NULL,
    quantity                INT                                          NOT NULL DEFAULT 1,
    condition_status        ENUM ('GOOD','ATTENTION','BROKEN','MISSING') NOT NULL DEFAULT 'GOOD',
    note                    TEXT                                         NULL,
    evidence_file_id        BIGINT UNSIGNED                              NULL,
    compensation_amount     BIGINT UNSIGNED                              NULL,
    compensation_invoice_id BIGINT UNSIGNED                              NULL,
    created_at              DATETIME(6)                                  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    KEY idx_chi_handover (handover_record_id),
    CONSTRAINT fk_chi_handover FOREIGN KEY (handover_record_id) REFERENCES contract_handover_records (id),
    CONSTRAINT fk_chi_asset FOREIGN KEY (room_asset_id) REFERENCES room_assets (id),
    CONSTRAINT fk_chi_file FOREIGN KEY (evidence_file_id) REFERENCES file_metadata (id),
    CONSTRAINT fk_chi_invoice FOREIGN KEY (compensation_invoice_id) REFERENCES invoices (id)
) ENGINE = InnoDB;

CREATE TABLE rule_violations
(
    id                BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    property_id       BIGINT UNSIGNED                                   NOT NULL,
    room_id           BIGINT UNSIGNED                                   NULL,
    contract_id       BIGINT UNSIGNED                                   NULL,
    tenant_profile_id BIGINT UNSIGNED                                   NULL,
    rule_id           BIGINT UNSIGNED                                   NOT NULL,
    violation_date    DATE                                              NOT NULL,
    description       TEXT                                              NOT NULL,
    fine_amount       BIGINT UNSIGNED                                   NOT NULL DEFAULT 0,
    invoice_id        BIGINT UNSIGNED                                   NULL,
    evidence_file_id  BIGINT UNSIGNED                                   NULL,
    status            ENUM ('RECORDED','INVOICED','WAIVED','CANCELLED') NOT NULL DEFAULT 'RECORDED',
    created_by        BIGINT UNSIGNED                                   NULL,
    created_at        DATETIME(6)                                       NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    KEY idx_rule_violations_room (room_id, violation_date),
    KEY idx_rule_violations_contract (contract_id, status),
    CONSTRAINT fk_rv_property FOREIGN KEY (property_id) REFERENCES properties (id),
    CONSTRAINT fk_rv_room FOREIGN KEY (room_id) REFERENCES rooms (id),
    CONSTRAINT fk_rv_contract FOREIGN KEY (contract_id) REFERENCES lease_contracts (id),
    CONSTRAINT fk_rv_rule FOREIGN KEY (rule_id) REFERENCES property_rules (id),
    CONSTRAINT fk_rv_invoice FOREIGN KEY (invoice_id) REFERENCES invoices (id),
    CONSTRAINT fk_rv_file FOREIGN KEY (evidence_file_id) REFERENCES file_metadata (id),
    CONSTRAINT fk_rv_user FOREIGN KEY (created_by) REFERENCES users (id)
) ENGINE = InnoDB;

-- =========================================================
-- 11. TRANSFER
-- =========================================================

CREATE TABLE room_transfer_requests
(
    id                      BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    request_code            VARCHAR(80)                                                                                                                    NOT NULL,
    requester_id            BIGINT UNSIGNED                                                                                                                NOT NULL,
    old_contract_id         BIGINT UNSIGNED                                                                                                                NOT NULL,
    old_room_id             BIGINT UNSIGNED                                                                                                                NOT NULL,
    target_room_id          BIGINT UNSIGNED                                                                                                                NOT NULL,
    requested_transfer_date DATE                                                                                                                           NOT NULL,
    reason                  TEXT                                                                                                                           NULL,
    status                  ENUM ('PENDING','APPROVED','REJECTED','CANCELLED','OLD_ROOM_HANDOVER','SETTLEMENT_PENDING','NEW_CONTRACT_CREATED','COMPLETED') NOT NULL DEFAULT 'PENDING',
    debt_snapshot_id        BIGINT UNSIGNED                                                                                                                NULL,
    approved_by             BIGINT UNSIGNED                                                                                                                NULL,
    approved_at             DATETIME(6)                                                                                                                    NULL,
    rejection_reason        VARCHAR(1000)                                                                                                                  NULL,
    eligibility_checked_at  DATETIME(6)                                                                                                                    NULL,
    is_eligible_at_creation BOOLEAN                                                                                                                        NULL,
    eligibility_snapshot    BLOB                                                                                                                           NULL,
    new_contract_id         BIGINT UNSIGNED                                                                                                                NULL,
    created_at              DATETIME(6)                                                                                                                    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at              DATETIME(6)                                                                                                                    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    UNIQUE KEY uq_transfer_code (request_code),
    KEY idx_transfer_status (status, created_at),
    KEY idx_transfer_rooms (old_room_id, target_room_id),
    CONSTRAINT fk_tr_requester FOREIGN KEY (requester_id) REFERENCES tenants (id),
    CONSTRAINT fk_tr_old_contract FOREIGN KEY (old_contract_id) REFERENCES lease_contracts (id),
    CONSTRAINT fk_tr_old_room FOREIGN KEY (old_room_id) REFERENCES rooms (id),
    CONSTRAINT fk_tr_target_room FOREIGN KEY (target_room_id) REFERENCES rooms (id),
    CONSTRAINT fk_tr_debt FOREIGN KEY (debt_snapshot_id) REFERENCES debt_snapshots (id),
    CONSTRAINT fk_tr_approved FOREIGN KEY (approved_by) REFERENCES users (id),
    CONSTRAINT fk_tr_new_contract FOREIGN KEY (new_contract_id) REFERENCES lease_contracts (id)
) ENGINE = InnoDB;

CREATE TABLE transfer_settlements
(
    id                             BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    transfer_request_id            BIGINT UNSIGNED                                                              NOT NULL,
    old_room_remaining_value       BIGINT UNSIGNED                                                              NOT NULL DEFAULT 0,
    new_room_required_value        BIGINT UNSIGNED                                                              NOT NULL DEFAULT 0,
    difference_amount              BIGINT UNSIGNED                                                              NOT NULL DEFAULT 0,
    settlement_type                ENUM ('TENANT_PAY_MORE','REFUND_NOW','CREDIT_NEXT_CONTRACT','NO_DIFFERENCE') NOT NULL,
    old_room_final_invoice_id      BIGINT UNSIGNED                                                              NULL,
    transfer_difference_invoice_id BIGINT UNSIGNED                                                              NULL,
    confirmed_by                   BIGINT UNSIGNED                                                              NULL,
    confirmed_at                   DATETIME(6)                                                                  NULL,
    created_at                     DATETIME(6)                                                                  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uq_transfer_settlement (transfer_request_id),
    CONSTRAINT fk_ts_transfer FOREIGN KEY (transfer_request_id) REFERENCES room_transfer_requests (id),
    CONSTRAINT fk_ts_old_invoice FOREIGN KEY (old_room_final_invoice_id) REFERENCES invoices (id),
    CONSTRAINT fk_ts_diff_invoice FOREIGN KEY (transfer_difference_invoice_id) REFERENCES invoices (id),
    CONSTRAINT fk_ts_user FOREIGN KEY (confirmed_by) REFERENCES users (id)
) ENGINE = InnoDB;

-- =========================================================
-- 12. MAINTENANCE / OPERATING EXPENSE
-- =========================================================

CREATE TABLE maintenance_tickets
(
    id               BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    ticket_code      VARCHAR(80)                                                                                                    NOT NULL,
    property_id      BIGINT UNSIGNED                                                                                                NOT NULL,
    room_id          BIGINT UNSIGNED                                                                                                NULL,
    contract_id      BIGINT UNSIGNED                                                                                                NULL,
    created_by       BIGINT UNSIGNED                                                                                                NOT NULL,
    ticket_scope     ENUM ('TENANT_ROOM','COMMON_AREA','PROPERTY_OPERATION')                                                        NOT NULL,
    priority         ENUM ('LOW','MEDIUM','HIGH','URGENT')                                                                          NOT NULL DEFAULT 'MEDIUM',
    category         VARCHAR(100)                                                                                                   NOT NULL,
    title            VARCHAR(255)                                                                                                   NOT NULL,
    description      TEXT                                                                                                           NOT NULL,
    status           ENUM ('PENDING_ACCEPTANCE','ACCEPTED','IN_PROGRESS','WAITING_CONFIRMATION','COMPLETED','REJECTED','CANCELLED') NOT NULL DEFAULT 'PENDING_ACCEPTANCE',
    rejection_reason VARCHAR(1000)                                                                                                  NULL,
    assigned_to      BIGINT UNSIGNED                                                                                                NULL,
    worker_name      VARCHAR(255)                                                                                                   NULL,
    repair_items     TEXT                                                                                                           NULL,
    completed_at     DATETIME(6)                                                                                                    NULL,
    created_at       DATETIME(6)                                                                                                    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at       DATETIME(6)                                                                                                    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    UNIQUE KEY uq_ticket_code (ticket_code),
    KEY idx_ticket_status (status, created_at),
    KEY idx_ticket_room (room_id),
    KEY idx_ticket_property_status (property_id, status, created_at),
    CONSTRAINT fk_mt_property FOREIGN KEY (property_id) REFERENCES properties (id),
    CONSTRAINT fk_mt_room FOREIGN KEY (room_id) REFERENCES rooms (id),
    CONSTRAINT fk_mt_contract FOREIGN KEY (contract_id) REFERENCES lease_contracts (id),
    CONSTRAINT fk_mt_created FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_mt_assigned FOREIGN KEY (assigned_to) REFERENCES users (id)
) ENGINE = InnoDB;

CREATE TABLE maintenance_ticket_attachments
(
    id               BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    ticket_id        BIGINT UNSIGNED                                    NOT NULL,
    file_id          BIGINT UNSIGNED                                    NOT NULL,
    attachment_phase ENUM ('BEFORE','DURING','AFTER','RECEIPT','OTHER') NOT NULL DEFAULT 'BEFORE',
    sort_order       INT                                                NOT NULL DEFAULT 0,
    created_by       BIGINT UNSIGNED                                    NULL,
    created_at       DATETIME(6)                                        NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    KEY idx_mta_ticket (ticket_id, attachment_phase),
    CONSTRAINT fk_mta_ticket FOREIGN KEY (ticket_id) REFERENCES maintenance_tickets (id),
    CONSTRAINT fk_mta_file FOREIGN KEY (file_id) REFERENCES file_metadata (id),
    CONSTRAINT fk_mta_user FOREIGN KEY (created_by) REFERENCES tenants (id)
) ENGINE = InnoDB;

CREATE TABLE maintenance_ticket_events
(
    id          BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    ticket_id   BIGINT UNSIGNED NOT NULL,
    from_status VARCHAR(50)     NULL,
    to_status   VARCHAR(50)     NOT NULL,
    note        TEXT            NULL,
    created_by  BIGINT UNSIGNED NULL,
    created_at  DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    KEY idx_ticket_events (ticket_id, created_at),
    CONSTRAINT fk_mte_ticket FOREIGN KEY (ticket_id) REFERENCES maintenance_tickets (id),
    CONSTRAINT fk_mte_user FOREIGN KEY (created_by) REFERENCES users (id)
) ENGINE = InnoDB;

CREATE TABLE maintenance_costs
(
    id                BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    ticket_id         BIGINT UNSIGNED                                                            NOT NULL,
    cost_type         ENUM ('LABOR','MATERIAL','TENANT_COMPENSATION','COMMON_OPERATING','OTHER') NOT NULL,
    description       VARCHAR(1000)                                                              NOT NULL,
    amount            BIGINT UNSIGNED                                                            NOT NULL,
    paid_by           ENUM ('LANDLORD','TENANT','MANAGER','OTHER')                               NOT NULL DEFAULT 'LANDLORD',
    charge_invoice_id BIGINT UNSIGNED                                                            NULL,
    receipt_file_id   BIGINT UNSIGNED                                                            NULL,
    created_by        BIGINT UNSIGNED                                                            NULL,
    created_at        DATETIME(6)                                                                NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    KEY idx_maintenance_cost_ticket (ticket_id),
    CONSTRAINT fk_mc_ticket FOREIGN KEY (ticket_id) REFERENCES maintenance_tickets (id),
    CONSTRAINT fk_mc_invoice FOREIGN KEY (charge_invoice_id) REFERENCES invoices (id),
    CONSTRAINT fk_mc_receipt FOREIGN KEY (receipt_file_id) REFERENCES file_metadata (id),
    CONSTRAINT fk_mc_user FOREIGN KEY (created_by) REFERENCES users (id)
) ENGINE = InnoDB;

CREATE TABLE maintenance_reviews
(
    id               BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    ticket_id        BIGINT UNSIGNED  NOT NULL,
    reviewer_user_id BIGINT UNSIGNED  NOT NULL,
    rating           TINYINT UNSIGNED NOT NULL,
    comment          TEXT             NULL,
    created_at       DATETIME(6)      NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uq_ticket_review_user (ticket_id, reviewer_user_id),
    CONSTRAINT chk_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT fk_mr_review_ticket FOREIGN KEY (ticket_id) REFERENCES maintenance_tickets (id),
    CONSTRAINT fk_mr_review_user FOREIGN KEY (reviewer_user_id) REFERENCES users (id)
) ENGINE = InnoDB;

CREATE TABLE operating_expenses
(
    id              BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    property_id     BIGINT UNSIGNED                                                              NOT NULL,
    room_id         BIGINT UNSIGNED                                                              NULL,
    ticket_id       BIGINT UNSIGNED                                                              NULL,
    expense_code    VARCHAR(80)                                                                  NOT NULL,
    expense_type    ENUM ('REPAIR','COMMON_UTILITY','SUPPLIES','REPLACEMENT','CLEANING','OTHER') NOT NULL,
    description     TEXT                                                                         NOT NULL,
    amount          BIGINT UNSIGNED                                                              NOT NULL,
    expense_date    DATE                                                                         NOT NULL,
    paid_by_user_id BIGINT UNSIGNED                                                              NULL,
    receipt_file_id BIGINT UNSIGNED                                                              NULL,
    status          ENUM ('DRAFT','PENDING_APPROVAL','APPROVED','REJECTED','PAID','CANCELLED')   NOT NULL DEFAULT 'DRAFT',
    approved_by     BIGINT UNSIGNED                                                              NULL,
    approved_at     DATETIME(6)                                                                  NULL,
    created_by      BIGINT UNSIGNED                                                              NULL,
    created_at      DATETIME(6)                                                                  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uq_operating_expense_code (expense_code),
    KEY idx_operating_expense_property (property_id, expense_date, status),
    CONSTRAINT fk_oe_property FOREIGN KEY (property_id) REFERENCES properties (id),
    CONSTRAINT fk_oe_room FOREIGN KEY (room_id) REFERENCES rooms (id),
    CONSTRAINT fk_oe_ticket FOREIGN KEY (ticket_id) REFERENCES maintenance_tickets (id),
    CONSTRAINT fk_oe_paid_by FOREIGN KEY (paid_by_user_id) REFERENCES users (id),
    CONSTRAINT fk_oe_receipt FOREIGN KEY (receipt_file_id) REFERENCES file_metadata (id),
    CONSTRAINT fk_oe_approved FOREIGN KEY (approved_by) REFERENCES users (id),
    CONSTRAINT fk_oe_created FOREIGN KEY (created_by) REFERENCES users (id)
) ENGINE = InnoDB;

-- =========================================================
-- 13. OCR
-- =========================================================

CREATE TABLE ocr_jobs
(
    id            BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    input_file_id BIGINT UNSIGNED                                                                        NOT NULL,
    document_type ENUM ('LEASE_CONTRACT','DEPOSIT_CONTRACT','IDENTITY_DOCUMENT','VEHICLE_PLATE','OTHER') NOT NULL,
    status        ENUM ('QUEUED','PROCESSING','REVIEW_REQUIRED','APPROVED','REJECTED','FAILED')          NOT NULL DEFAULT 'QUEUED',
    target_type   VARCHAR(100)                                                                           NULL,
    target_id     BIGINT UNSIGNED                                                                        NULL,
    raw_result    BLOB                                                                                   NULL,
    reviewed_by   BIGINT UNSIGNED                                                                        NULL,
    reviewed_at   DATETIME(6)                                                                            NULL,
    created_at    DATETIME(6)                                                                            NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    KEY idx_ocr_status (status, created_at),
    KEY idx_ocr_target (target_type, target_id),
    CONSTRAINT fk_ocr_file FOREIGN KEY (input_file_id) REFERENCES file_metadata (id),
    CONSTRAINT fk_ocr_reviewer FOREIGN KEY (reviewed_by) REFERENCES users (id)
) ENGINE = InnoDB;

CREATE TABLE ocr_extracted_fields
(
    id              BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    ocr_job_id      BIGINT UNSIGNED                                      NOT NULL,
    field_name      VARCHAR(100)                                         NOT NULL,
    extracted_value TEXT                                                 NULL,
    corrected_value TEXT                                                 NULL,
    confidence      DECIMAL(5, 4)                                        NULL,
    status          ENUM ('EXTRACTED','CORRECTED','ACCEPTED','REJECTED') NOT NULL DEFAULT 'EXTRACTED',
    created_at      DATETIME(6)                                          NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uq_ocr_field (ocr_job_id, field_name),
    CONSTRAINT fk_oef_job FOREIGN KEY (ocr_job_id) REFERENCES ocr_jobs (id)
) ENGINE = InnoDB;

-- =========================================================
-- 14. NOTIFICATION / SCHEDULED TASKS
-- =========================================================

CREATE TABLE notification_templates
(
    id             BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    template_key   VARCHAR(100)               NOT NULL,
    channel        ENUM ('PUSH', 'EMAIL')     NOT NULL,
    title_template VARCHAR(255)               NOT NULL,
    body_template  TEXT                       NOT NULL,
    status         ENUM ('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    created_at     DATETIME(6)                NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uq_template (template_key, channel)
) ENGINE = InnoDB;

CREATE TABLE notification_outbox
(
    id                BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    event_type        VARCHAR(100)                                 NOT NULL,
    target_type       VARCHAR(100)                                 NULL,
    target_id         BIGINT UNSIGNED                              NULL,
    recipient_user_id BIGINT UNSIGNED                              NOT NULL,
    channel           ENUM ('PUSH','EMAIL')                        NOT NULL,
    title             VARCHAR(255)                                 NOT NULL,
    body              TEXT                                         NOT NULL,
    payload           JSON                                         NULL,
    status            ENUM ('PENDING','SENT','FAILED','CANCELLED') NOT NULL DEFAULT 'PENDING',
    retry_count       TINYINT UNSIGNED                             NOT NULL DEFAULT 0,
    max_retries       TINYINT UNSIGNED                             NOT NULL DEFAULT 3,
    last_error        VARCHAR(1000)                                NULL,
    scheduled_at      DATETIME(6)                                  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    sent_at           DATETIME(6)                                  NULL,
    created_at        DATETIME(6)                                  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    KEY idx_outbox_pending (status, scheduled_at),
    KEY idx_outbox_recipient (recipient_user_id, created_at),
    CONSTRAINT fk_no_user FOREIGN KEY (recipient_user_id) REFERENCES users (id)
) ENGINE = InnoDB;

CREATE TABLE notification_deliveries
(
    id                  BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    outbox_id           BIGINT UNSIGNED                           NOT NULL,
    provider_message_id VARCHAR(255)                              NULL,
    delivery_status     ENUM ('SENT','DELIVERED','READ','FAILED') NOT NULL,
    error_message       VARCHAR(1000)                             NULL,
    delivered_at        DATETIME(6)                               NULL,
    read_at             DATETIME(6)                               NULL,
    created_at          DATETIME(6)                               NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    KEY idx_delivery_outbox (outbox_id),
    KEY idx_delivery_status (delivery_status, created_at),
    KEY idx_delivery_read_status (delivery_status, read_at, created_at),
    CONSTRAINT fk_nd_outbox FOREIGN KEY (outbox_id) REFERENCES notification_outbox (id)
) ENGINE = InnoDB;

CREATE TABLE scheduled_tasks
(
    id          BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    task_type   ENUM ('INVOICE_REMINDER','DEBT_WARNING','CONTRACT_EXPIRY','ROOM_STATUS_AUTOMATION','MAINTENANCE_FOLLOWUP','OTHER') NOT NULL,
    target_type VARCHAR(100)                                                                                                       NOT NULL,
    target_id   BIGINT UNSIGNED                                                                                                    NOT NULL,
    due_at      DATETIME(6)                                                                                                        NOT NULL,
    status      ENUM ('PENDING','DONE','FAILED','CANCELLED')                                                                       NOT NULL DEFAULT 'PENDING',
    retry_count TINYINT UNSIGNED                                                                                                   NOT NULL DEFAULT 0,
    payload     BLOB                                                                                                               NULL,
    executed_at DATETIME(6)                                                                                                        NULL,
    created_at  DATETIME(6)                                                                                                        NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    KEY idx_tasks_due (status, due_at)
) ENGINE = InnoDB;

-- =========================================================
-- 15. AUDIT
-- =========================================================

CREATE TABLE audit_logs
(
    id            BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    actor_user_id BIGINT UNSIGNED NULL,
    action        VARCHAR(100)    NOT NULL,
    entity_type   VARCHAR(100)    NOT NULL,
    entity_id     BIGINT UNSIGNED NULL,
    before_json   BLOB            NULL,
    after_json    BLOB            NULL,
    ip_address    VARCHAR(45)     NULL,
    user_agent    VARCHAR(1000)   NULL,
    created_at    DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    KEY idx_audit_entity (entity_type, entity_id, created_at),
    KEY idx_audit_actor (actor_user_id, created_at),
    KEY idx_audit_action (action, created_at),
    CONSTRAINT fk_audit_user FOREIGN KEY (actor_user_id) REFERENCES users (id)
) ENGINE = InnoDB;

SET FOREIGN_KEY_CHECKS = 1;

-- =========================================================
-- 16. BUSINESS GUARD TRIGGERS
-- These are guard rails only. Complex business rules still belong in domain services.
-- =========================================================

DELIMITER $$

# CREATE TRIGGER trg_vehicles_max_two_active_before_insert
#     BEFORE INSERT ON vehicles
#     FOR EACH ROW
# BEGIN
#     IF NEW.status = 'ACTIVE' AND NEW.deleted_at IS NULL THEN
#         IF (SELECT COUNT(*) FROM vehicles
#             WHERE tenant_id = NEW.tenant_id
#               AND tenant_profile_id = NEW.tenant_profile_id
#               AND status = 'ACTIVE'
#               AND deleted_at IS NULL) >= 2 THEN
#             SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'A tenant profile can register at most 2 active vehicles';
#         END IF;
#     END IF;
# END
$$

CREATE TRIGGER trg_contract_occupants_max_three_before_insert
    BEFORE INSERT
    ON contract_occupants
    FOR EACH ROW
BEGIN
    IF NEW.status = 'ACTIVE' THEN
        IF (SELECT COUNT(*)
            FROM contract_occupants
            WHERE tenant_id = NEW.tenant_id
              AND contract_id = NEW.contract_id
              AND status = 'ACTIVE') >= 3 THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'A room contract can have at most 3 active occupants';
        END IF;
    END IF;
END$$

CREATE TRIGGER trg_ticket_attachments_max_three_before_insert
    BEFORE INSERT
    ON maintenance_ticket_attachments
    FOR EACH ROW
BEGIN
    IF NEW.attachment_phase IN ('BEFORE', 'DURING', 'AFTER') THEN
        IF (SELECT COUNT(*)
            FROM maintenance_ticket_attachments
            WHERE created_by = NEW.created_by
              AND ticket_id = NEW.ticket_id
              AND attachment_phase = NEW.attachment_phase) >= 3 THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT =
                    'A maintenance ticket can have at most 3 media attachments per phase';
        END IF;
    END IF;
END$$

CREATE TRIGGER trg_ledger_entries_no_update
    BEFORE UPDATE
    ON ledger_entries
    FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Ledger entries are immutable; create reversal entries instead';
END$$

CREATE TRIGGER trg_ledger_entries_no_delete
    BEFORE DELETE
    ON ledger_entries
    FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Ledger entries are immutable; create reversal entries instead';
END$$

CREATE TRIGGER trg_invoice_lines_no_update_after_issue
    BEFORE UPDATE
    ON invoice_lines
    FOR EACH ROW
BEGIN
    IF EXISTS (SELECT 1 FROM invoices WHERE id = OLD.invoice_id AND status <> 'DRAFT') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invoice lines cannot be updated after invoice is issued';
    END IF;
END$$

CREATE TRIGGER trg_invoice_lines_no_delete_after_issue
    BEFORE DELETE
    ON invoice_lines
    FOR EACH ROW
BEGIN
    IF EXISTS (SELECT 1 FROM invoices WHERE id = OLD.invoice_id AND status <> 'DRAFT') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invoice lines cannot be deleted after invoice is issued';
    END IF;
END$$

CREATE TRIGGER trg_payment_transactions_guard_update
    BEFORE UPDATE
    ON payment_transactions
    FOR EACH ROW
BEGIN
    IF OLD.status IN ('MATCHED', 'PARTIALLY_ALLOCATED', 'ALLOCATED', 'DUPLICATE', 'REJECTED', 'REFUNDED') THEN
        IF NOT (NEW.status = OLD.status AND NEW.confirmed_by <=> OLD.confirmed_by AND
                NEW.confirmed_at <=> OLD.confirmed_at) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Terminal payment transactions are immutable';
        END IF;
    END IF;
END$$

DELIMITER ;

-- =========================================================
-- 17. SEED DEFAULT ROOM STATUS COLORS
-- Insert per tenant after tenant creation in application code.
-- Example omitted b ecause tenant_id is dynamic.
-- =========================================================

-- End of schema
