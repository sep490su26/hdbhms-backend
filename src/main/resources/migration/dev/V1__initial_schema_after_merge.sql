CREATE TABLE IF NOT EXISTS hdbhms.emergency_contacts
(
    emergency_contact_id BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    tenant_profile_id    BIGINT UNSIGNED                          NOT NULL,
    full_name            VARCHAR(255)                             NOT NULL,
    relationship         VARCHAR(100)                             NOT NULL,
    phone                VARCHAR(30)                              NOT NULL,
    created_at           DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL
);

CREATE INDEX idx_emergency_profile
    ON hdbhms.emergency_contacts (tenant_profile_id);


CREATE TABLE IF NOT EXISTS hdbhms.invalidated_tokens
(
    invalidated_token_id VARCHAR(255) NOT NULL
        PRIMARY KEY,
    expiry_time          DATETIME(6)  NOT NULL
);

CREATE TABLE IF NOT EXISTS hdbhms.ledger_entries
(
    ledger_entry_id   BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    entry_code        VARCHAR(100)                                                                         NOT NULL,
    entry_date        DATE                                                                                 NOT NULL,
    source_type       ENUM ('INVOICE', 'PAYMENT', 'REFUND', 'EXPENSE', 'DEPOSIT_SETTLEMENT', 'ADJUSTMENT') NOT NULL,
    source_id         BIGINT UNSIGNED                                                                      NOT NULL,
    account_code      VARCHAR(50)                                                                          NOT NULL,
    debit_amount      BIGINT UNSIGNED DEFAULT '0'                                                          NOT NULL,
    credit_amount     BIGINT UNSIGNED DEFAULT '0'                                                          NOT NULL,
    description       VARCHAR(1000)                                                                        null,
    posted_at         DATETIME(6)     DEFAULT CURRENT_TIMESTAMP(6)                                         NOT NULL,
    reversed_entry_id BIGINT UNSIGNED                                                                      null,
    CONSTRAINT uq_ledger_entry_code
        UNIQUE (entry_code),
    CONSTRAINT fk_le_reversal
        FOREIGN KEY (reversed_entry_id) REFERENCES hdbhms.ledger_entries (ledger_entry_id),
    CONSTRAINT chk_ledger_nonzero
        CHECK (((`debit_amount` > 0) and (`credit_amount` = 0)) or ((`credit_amount` > 0) and (`debit_amount` = 0)))
);

CREATE INDEX idx_ledger_date
    ON hdbhms.ledger_entries (entry_date);

CREATE INDEX idx_ledger_source
    ON hdbhms.ledger_entries (source_type, source_id);

CREATE TRIGGER hdbhms.trg_ledger_entries_no_delete
    BEFORE DELETE
    ON hdbhms.ledger_entries
    FOR EACH ROW
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Ledger entries are immutable; create reversal entries instead';

CREATE TRIGGER hdbhms.trg_ledger_entries_no_update
    BEFORE UPDATE
    ON hdbhms.ledger_entries
    FOR EACH ROW
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Ledger entries are immutable; create reversal entries instead';

CREATE TABLE IF NOT EXISTS hdbhms.notification_templates
(
    notification_template_id BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    template_key             VARCHAR(100)                                             NOT NULL,
    channel                  ENUM ('PUSH', 'EMAIL')                                   NOT NULL,
    title_template           VARCHAR(255)                                             NOT NULL,
    body_template            TEXT                                                     NOT NULL,
    status                   ENUM ('ACTIVE', 'INACTIVE') DEFAULT 'ACTIVE'             NOT NULL,
    created_at               DATETIME(6)                 DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    CONSTRAINT uq_template
        UNIQUE (template_key, channel)
);

CREATE TABLE IF NOT EXISTS hdbhms.properties
(
    property_id   BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    property_code VARCHAR(50)                                                                                                         NOT NULL,
    name          VARCHAR(255)                                                                                                        NOT NULL,
    property_type ENUM ('BOARDING_HOUSE', 'APARTMENT', 'WHOLE_HOUSE', 'MINI_APARTMENT', 'DORM', 'OTHER') DEFAULT 'BOARDING_HOUSE'     NOT NULL,
    address_line  VARCHAR(500)                                                                                                        NOT NULL,
    description   TEXT                                                                                                                null,
    status        ENUM ('ACTIVE', 'TEMP_CLOSED', 'CLOSED')                                               DEFAULT 'ACTIVE'             NOT NULL,
    created_at    DATETIME(6)                                                                            DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    updated_at    DATETIME(6)                                                                            DEFAULT CURRENT_TIMESTAMP(6) NOT NULL on update CURRENT_TIMESTAMP(6),
    deleted_at    DATETIME(6)                                                                                                         null,
    version       INT UNSIGNED                                                                           DEFAULT '0'                  NOT NULL,
    CONSTRAINT uq_property_code
        UNIQUE (property_code)
);

CREATE TABLE IF NOT EXISTS hdbhms.collection_accounts
(
    collection_account_id BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    property_id           BIGINT UNSIGNED                                                                null,
    account_type          ENUM ('RENT', 'UTILITY', 'DEPOSIT', 'OPERATING', 'GENERAL')                    NOT NULL,
    bank_name             VARCHAR(100)                                                                   null,
    account_number        VARCHAR(100)                                                                   null,
    account_holder        VARCHAR(255)                                                                   null,
    provider              ENUM ('BANK', 'MOMO', 'ZALOPAY', 'CASH', 'OTHER') DEFAULT 'BANK'               NOT NULL,
    status                ENUM ('ACTIVE', 'INACTIVE')                       DEFAULT 'ACTIVE'             NOT NULL,
    created_at            DATETIME(6)                                       DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    CONSTRAINT uq_collection_account
        UNIQUE (provider, account_number, account_type),
    CONSTRAINT fk_ca_property
        FOREIGN KEY (property_id) REFERENCES hdbhms.properties (property_id)
);

CREATE INDEX idx_collection_account_scope
    ON hdbhms.collection_accounts (property_id, account_type, status);

CREATE TABLE IF NOT EXISTS hdbhms.deposit_batches
(
    deposit_batch_id         BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    batch_code               VARCHAR(80)                                                                                                                                NOT NULL,
    property_id              BIGINT UNSIGNED                                                                                                                            NOT NULL,
    full_name                VARCHAR(255)                                                                                                                               NOT NULL,
    phone                    VARCHAR(30)                                                                                                                                NOT NULL,
    email                    VARCHAR(255)                                                                                                                               null,
    id_number                VARCHAR(50)                                                                                                                                null,
    expected_move_in_date    DATE                                                                                                                                       NOT NULL,
    expected_lease_sign_date DATE                                                                                                                                       NOT NULL,
    total_deposit_amount     BIGINT UNSIGNED                                                                                               DEFAULT '0'                  NOT NULL,
    invoice_id               BIGINT UNSIGNED                                                                                                                            null,
    payment_intent_id        BIGINT UNSIGNED                                                                                                                            null,
    status                   ENUM ('DRAFT', 'PENDING_PAYMENT', 'PAID', 'CONFIRMED', 'EXPIRED', 'CANCELLED', 'REFUND_REQUIRED', 'REFUNDED') DEFAULT 'DRAFT'              NOT NULL,
    created_at               DATETIME(6)                                                                                                   DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    updated_at               DATETIME(6)                                                                                                   DEFAULT CURRENT_TIMESTAMP(6) NOT NULL on update CURRENT_TIMESTAMP(6),
    version                  BIGINT UNSIGNED                                                                                               DEFAULT '0'                  NOT NULL,
    CONSTRAINT uq_deposit_batch_code
        UNIQUE (batch_code),
    CONSTRAINT uq_deposit_batch_invoice
        UNIQUE (invoice_id),
    CONSTRAINT uq_deposit_batch_payment_intent
        UNIQUE (payment_intent_id),
    CONSTRAINT fk_deposit_batch_property
        FOREIGN KEY (property_id) REFERENCES hdbhms.properties (property_id)
);

CREATE INDEX idx_deposit_batch_expected_move_in
    ON hdbhms.deposit_batches (expected_move_in_date);

CREATE INDEX idx_deposit_batch_phone
    ON hdbhms.deposit_batches (phone);

CREATE INDEX idx_deposit_batch_property
    ON hdbhms.deposit_batches (property_id);

CREATE INDEX idx_deposit_batch_status
    ON hdbhms.deposit_batches (status);

CREATE TABLE IF NOT EXISTS hdbhms.floors
(
    floor_id    BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    property_id BIGINT UNSIGNED                                          NOT NULL,
    floor_code  VARCHAR(50)                                              NOT NULL,
    name        VARCHAR(100)                                             NOT NULL,
    sort_order  INT                         DEFAULT 0                    NOT NULL,
    status      ENUM ('ACTIVE', 'INACTIVE') DEFAULT 'ACTIVE'             NOT NULL,
    created_at  DATETIME(6)                 DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    updated_at  DATETIME(6)                 DEFAULT CURRENT_TIMESTAMP(6) NOT NULL on update CURRENT_TIMESTAMP(6),
    deleted_at  DATETIME(6)                                              null,
    CONSTRAINT uq_floor_code
        UNIQUE (property_id, floor_code),
    CONSTRAINT fk_floor_property
        FOREIGN KEY (property_id) REFERENCES hdbhms.properties (property_id)
);

CREATE INDEX idx_floor_property
    ON hdbhms.floors (property_id);

CREATE INDEX idx_property_status
    ON hdbhms.properties (status);

CREATE TABLE IF NOT EXISTS hdbhms.property_rules
(
    property_rule_id    BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    property_id         BIGINT UNSIGNED                                          NOT NULL,
    rule_code           VARCHAR(50)                                              NOT NULL,
    title               VARCHAR(255)                                             NOT NULL,
    description         TEXT                                                     NOT NULL,
    default_fine_amount BIGINT UNSIGNED                                          null,
    sort_order          INT                         DEFAULT 0                    NOT NULL,
    status              ENUM ('ACTIVE', 'INACTIVE') DEFAULT 'ACTIVE'             NOT NULL,
    created_at          DATETIME(6)                 DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    updated_at          DATETIME(6)                 DEFAULT CURRENT_TIMESTAMP(6) NOT NULL on update CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_property_rule_code
        UNIQUE (property_id, rule_code),
    CONSTRAINT fk_property_rules_property
        FOREIGN KEY (property_id) REFERENCES hdbhms.properties (property_id)
);

CREATE INDEX idx_property_rules
    ON hdbhms.property_rules (property_id, status);

CREATE TABLE IF NOT EXISTS hdbhms.room_status_display_configs
(
    room_status_display_config_id BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    room_status                   ENUM ('VACANT', 'RESERVED', 'OCCUPIED', 'SOON_VACANT', 'MAINTENANCE', 'EXPIRED') NOT NULL,
    color_hex                     VARCHAR(20)                                                                      NOT NULL,
    label                         VARCHAR(100)                                                                     NOT NULL,
    CONSTRAINT uq_room_status_display
        UNIQUE (room_status)
);

CREATE TABLE IF NOT EXISTS hdbhms.rooms
(
    room_id        BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    property_id    BIGINT UNSIGNED                                                                                                                                   NOT NULL,
    floor_id       BIGINT UNSIGNED                                                                                                                                   NOT NULL,
    room_code      VARCHAR(50)                                                                                                                                       NOT NULL,
    name           VARCHAR(100)                                                                                                                                      NOT NULL,
    area_m2        DECIMAL(8, 2)                                                                                                                                     null,
    listed_price   BIGINT UNSIGNED                                                                                                      DEFAULT '0'                  NOT NULL,
    current_status ENUM ('VACANT', 'RESERVED', 'RESERVED_FOR_TRANSFER', 'ON_HOLD', 'OCCUPIED', 'SOON_VACANT', 'MAINTENANCE', 'EXPIRED') DEFAULT 'VACANT'             NOT NULL,
    max_occupants  TINYINT UNSIGNED                                                                                                     DEFAULT '3'                  NOT NULL,
    public_note    TEXT                                                                                                                                              null,
    internal_note  TEXT                                                                                                                                              null,
    position_x     INT                                                                                                                                               null,
    position_y     INT                                                                                                                                               null,
    sort_order     INT                                                                                                                  DEFAULT 0                    NOT NULL,
    created_at     DATETIME(6)                                                                                                          DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    updated_at     DATETIME(6)                                                                                                          DEFAULT CURRENT_TIMESTAMP(6) NOT NULL on update CURRENT_TIMESTAMP(6),
    deleted_at     DATETIME(6)                                                                                                                                       null,
    version        BIGINT UNSIGNED                                                                                                      DEFAULT '0'                  NOT NULL,
    CONSTRAINT uq_room_code
        UNIQUE (property_id, room_code),
    CONSTRAINT fk_room_floor
        FOREIGN KEY (floor_id) REFERENCES hdbhms.floors (floor_id),
    CONSTRAINT fk_room_property
        FOREIGN KEY (property_id) REFERENCES hdbhms.properties (property_id)
);

CREATE TABLE IF NOT EXISTS hdbhms.floor_plan_items
(
    id            BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    property_id   BIGINT UNSIGNED                             NOT NULL,
    floor_id      BIGINT UNSIGNED                             NOT NULL,
    room_id       BIGINT UNSIGNED                             null,
    item_type     VARCHAR(50)                                 NOT NULL,
    label         VARCHAR(255)                                null,
    x             DECIMAL(10, 2)                              NOT NULL,
    y             DECIMAL(10, 2)                              NOT NULL,
    width         DECIMAL(10, 2)                              NOT NULL,
    height        DECIMAL(10, 2)                              NOT NULL,
    rotation      DECIMAL(10, 2) DEFAULT 0.00                 NOT NULL,
    sort_order    INT            DEFAULT 0                    NOT NULL,
    metadata_json JSON                                        null,
    created_at    DATETIME(6)    DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    updated_at    DATETIME(6)    DEFAULT CURRENT_TIMESTAMP(6) NOT NULL on update CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_floor_plan_items_floor
        FOREIGN KEY (floor_id) REFERENCES hdbhms.floors (floor_id),
    CONSTRAINT fk_floor_plan_items_property
        FOREIGN KEY (property_id) REFERENCES hdbhms.properties (property_id),
    CONSTRAINT fk_floor_plan_items_room
        FOREIGN KEY (room_id) REFERENCES hdbhms.rooms (room_id),
    CONSTRAINT chk_floor_plan_items_size
        CHECK ((`width` > 0) and (`height` > 0))
);

CREATE INDEX idx_floor_plan_items_property_floor
    ON hdbhms.floor_plan_items (property_id, floor_id);

CREATE INDEX idx_floor_plan_items_room
    ON hdbhms.floor_plan_items (room_id);

CREATE TABLE IF NOT EXISTS hdbhms.meters
(
    meter_id         BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    room_id          BIGINT UNSIGNED                                                                NOT NULL,
    meter_type       ENUM ('ELECTRICITY', 'WATER')                                                  NOT NULL,
    meter_code       VARCHAR(100)                                                                   null,
    status           ENUM ('ACTIVE', 'REPLACED', 'BROKEN', 'INACTIVE') DEFAULT 'ACTIVE'             NOT NULL,
    installed_at     DATE                                                                           null,
    created_at       DATETIME(6)                                       DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    active_meter_key VARCHAR(255) as (if((`status` = _utf8mb4'ACTIVE'), concat(`room_id`, _utf8mb4':', `meter_type`),
                                         NULL)),
    CONSTRAINT uq_room_active_meter_type
        UNIQUE (active_meter_key),
    CONSTRAINT fk_meter_room
        FOREIGN KEY (room_id) REFERENCES hdbhms.rooms (room_id)
);

CREATE INDEX idx_meter_room
    ON hdbhms.meters (room_id);

CREATE INDEX idx_room_floor
    ON hdbhms.rooms (floor_id);

CREATE INDEX idx_room_status
    ON hdbhms.rooms (property_id, current_status);

CREATE TABLE IF NOT EXISTS hdbhms.scheduled_tasks
(
    scheduled_task_id BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    task_type         ENUM ('INVOICE_REMINDER', 'DEBT_WARNING', 'CONTRACT_EXPIRY', 'ROOM_STATUS_AUTOMATION', 'MAINTENANCE_FOLLOWUP', 'OTHER', 'ROOM_HOLD_EXPIRATION') NOT NULL,
    target_type       VARCHAR(100)                                                                                                                                    NOT NULL,
    target_id         BIGINT UNSIGNED                                                                                                                                 NOT NULL,
    due_at            DATETIME(6)                                                                                                                                     NOT NULL,
    status            ENUM ('PENDING', 'DONE', 'FAILED', 'CANCELLED') DEFAULT 'PENDING'                                                                               NOT NULL,
    retry_count       TINYINT UNSIGNED                                DEFAULT '0'                                                                                     NOT NULL,
    payload           BLOB                                                                                                                                            null,
    executed_at       DATETIME(6)                                                                                                                                     null,
    created_at        DATETIME(6)                                     DEFAULT CURRENT_TIMESTAMP(6)                                                                    NOT NULL
);

CREATE INDEX idx_tasks_due
    ON hdbhms.scheduled_tasks (status, due_at);

CREATE TABLE IF NOT EXISTS hdbhms.users
(
    user_id              BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    phone                VARCHAR(30)                                                                            NOT NULL,
    email                VARCHAR(255)                                                                           NOT NULL,
    password_hash        VARCHAR(255)                                                                           NOT NULL,
    role                 ENUM ('TENANT', 'MANAGER', 'ACCOUNTANT', 'OWNER', 'LEAD') DEFAULT 'LEAD'               NOT NULL,
    status               ENUM ('PENDING_CONTRACT', 'ACTIVE', 'DISABLED')           DEFAULT 'PENDING_CONTRACT'   NOT NULL,
    last_login_at        DATETIME(6)                                                                            null,
    email_verified       tinyint(1)                                                DEFAULT 0                    NOT NULL,
    must_change_password tinyint(1)                                                DEFAULT 0                    NOT NULL,
    created_at           DATETIME(6)                                               DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    updated_at           DATETIME(6)                                               DEFAULT CURRENT_TIMESTAMP(6) NOT NULL on update CURRENT_TIMESTAMP(6),
    deleted_at           DATETIME(6)                                                                            null,
    active_unique_token  tinyint as (if((`deleted_at` is null), 1, NULL)),
    CONSTRAINT uq_users_email_active
        UNIQUE (email, active_unique_token),
    CONSTRAINT uq_users_phone_active
        UNIQUE (phone, active_unique_token)
);

CREATE TABLE IF NOT EXISTS hdbhms.audit_logs
(
    id            BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    actor_user_id BIGINT UNSIGNED                          null,
    action        VARCHAR(100)                             NOT NULL,
    entity_type   VARCHAR(100)                             NOT NULL,
    entity_id     BIGINT UNSIGNED                          null,
    before_json   BLOB                                     null,
    after_json    BLOB                                     null,
    ip_address    VARCHAR(45)                              null,
    user_agent    VARCHAR(1000)                            null,
    created_at    DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_audit_user
        FOREIGN KEY (actor_user_id) REFERENCES hdbhms.users (user_id)
);

CREATE INDEX idx_audit_action
    ON hdbhms.audit_logs (action, created_at);

CREATE INDEX idx_audit_actor
    ON hdbhms.audit_logs (actor_user_id, created_at);

CREATE INDEX idx_audit_entity
    ON hdbhms.audit_logs (entity_type, entity_id, created_at);

CREATE TABLE IF NOT EXISTS hdbhms.file_metadata
(
    file_metadata_id BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    owner_user_id    BIGINT UNSIGNED                                                                                                                                                                                                                                                                   null,
    storage_key      VARCHAR(1000)                                                                                                                                                                                                                                                                     null,
    original_name    VARCHAR(255)                                                                                                                                                                                                                                                                      null,
    mime_type        VARCHAR(100)                                                                                                                                                                                                                                                                      null,
    size_bytes       BIGINT UNSIGNED                                                                                                                                                                                                                                                                   null,
    sha256_checksum  char(64)                                                                                                                                                                                                                                                                          null,
    category         ENUM ('ROOM_IMAGE', 'PROPERTY_IMAGE', 'PORTRAIT_PHOTO', 'ID_CARD', 'CONTRACT', 'DEPOSIT_CONTRACT', 'METER_PHOTO', 'VEHICLE_PHOTO', 'MAINTENANCE', 'TICKET_ATTACHMENT', 'RECEIPT', 'OCR_INPUT', 'LEASE_CONTRACT_DRAFT', 'HANDOVER_DOCUMENT', 'OTHER') DEFAULT 'OTHER'              NOT NULL,
    is_sensitive     tinyint(1)                                                                                                                                                                                                                                           DEFAULT 0                    NOT NULL,
    created_at       DATETIME(6)                                                                                                                                                                                                                                          DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    deleted_at       DATETIME(6)                                                                                                                                                                                                                                                                       null,
    CONSTRAINT fk_files_owner
        FOREIGN KEY (owner_user_id) REFERENCES hdbhms.users (user_id)
);

CREATE TABLE IF NOT EXISTS hdbhms.change_requests
(
    change_request_id BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    request_code      VARCHAR(80)                                                                                                                                          NOT NULL,
    request_type      ENUM ('METER_READING_CORRECTION', 'INVOICE_ADJUSTMENT', 'RENT_PRICE_ADJUSTMENT', 'DEPOSIT_REFUND_REQUEST', 'ROOM_TRANSFER', 'MOVE_OUT', 'COMPLAINT') NOT NULL,
    requester_id      BIGINT UNSIGNED                                                                                                                                      NOT NULL,
    requester_role    ENUM ('TENANT', 'MANAGER', 'ACCOUNTANT')                                                                                                             NOT NULL,
    target_type       ENUM ('METER_READING', 'INVOICE', 'CONTRACT', 'DEPOSIT', 'OTHER')                                                                                    NOT NULL,
    target_id         BIGINT UNSIGNED                                                                                                                                      null,
    title             VARCHAR(255)                                                                                                                                         NOT NULL,
    description       TEXT                                                                                                                                                 NOT NULL,
    request_payload   JSON                                                                                                                                                 null,
    evidence_file_id  BIGINT UNSIGNED                                                                                                                                      null,
    assigned_role     ENUM ('OWNER', 'MANAGER', 'ACCOUNTANT')                                                                                                              NOT NULL,
    assigned_to       BIGINT UNSIGNED                                                                                                                                      null,
    status            ENUM ('PENDING', 'UNDER_REVIEW', 'APPROVED', 'REJECTED', 'PROCESSING', 'COMPLETED', 'CANCELLED') DEFAULT 'PENDING'                                   NOT NULL,
    resolution_note   TEXT                                                                                                                                                 null,
    resolved_by       BIGINT UNSIGNED                                                                                                                                      null,
    resolved_at       DATETIME(6)                                                                                                                                          null,
    created_at        DATETIME(6)                                                                                      DEFAULT CURRENT_TIMESTAMP(6)                        NOT NULL,
    updated_at        DATETIME(6)                                                                                      DEFAULT CURRENT_TIMESTAMP(6)                        NOT NULL on update CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_request_code
        UNIQUE (request_code),
    CONSTRAINT fk_cr_assigned
        FOREIGN KEY (assigned_to) REFERENCES hdbhms.users (user_id),
    CONSTRAINT fk_cr_evidence
        FOREIGN KEY (evidence_file_id) REFERENCES hdbhms.file_metadata (file_metadata_id),
    CONSTRAINT fk_cr_requester
        FOREIGN KEY (requester_id) REFERENCES hdbhms.users (user_id),
    CONSTRAINT fk_cr_resolved
        FOREIGN KEY (resolved_by) REFERENCES hdbhms.users (user_id)
);

CREATE TABLE IF NOT EXISTS hdbhms.change_request_events
(
    change_request_event_id BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    request_id              BIGINT UNSIGNED                          NOT NULL,
    from_status             VARCHAR(50)                              null,
    to_status               VARCHAR(50)                              NOT NULL,
    note                    TEXT                                     null,
    acted_by                BIGINT UNSIGNED                          null,
    acted_at                DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_cre_request
        FOREIGN KEY (request_id) REFERENCES hdbhms.change_requests (change_request_id),
    CONSTRAINT fk_cre_user
        FOREIGN KEY (acted_by) REFERENCES hdbhms.users (user_id)
);

CREATE INDEX idx_cre_request
    ON hdbhms.change_request_events (request_id, acted_at);

CREATE INDEX idx_request_requester
    ON hdbhms.change_requests (requester_id, status);

CREATE INDEX idx_request_status
    ON hdbhms.change_requests (status, created_at);

CREATE INDEX idx_request_target
    ON hdbhms.change_requests (target_type, target_id);

CREATE INDEX idx_request_type
    ON hdbhms.change_requests (request_type, status);

CREATE TABLE IF NOT EXISTS hdbhms.deposit_forms
(
    deposit_form_id          BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    room_id                  BIGINT UNSIGNED                                                                NOT NULL,
    id_number                VARCHAR(50)                                                                    NOT NULL,
    id_issue_date            DATE                                                                           null,
    id_issue_place           VARCHAR(255)                                                                   null,
    id_front_file_id         BIGINT UNSIGNED                                                                null,
    id_back_file_id          BIGINT UNSIGNED                                                                null,
    portrait_file_id         BIGINT UNSIGNED                                                                null,
    full_name                VARCHAR(255)                                                                   NOT NULL,
    dob                      DATE                                                                           null,
    email                    VARCHAR(255)                                                                   NOT NULL,
    phone                    VARCHAR(30)                                                                    NOT NULL,
    permanent_address        VARCHAR(1000)                                                                  null,
    expected_move_in_date    DATE                                                                           NOT NULL,
    expected_lease_sign_date DATE                                                                           NOT NULL,
    payment_due_at           DATETIME(6)                                                                    null,
    deposit_expires_at       DATE                                                                           null,
    status                   ENUM ('APPROVAL_PENDING', 'APPROVED', 'REJECTED') DEFAULT 'APPROVAL_PENDING'   NOT NULL,
    confirmed_at             DATETIME(6)                                                                    null,
    reject_reason            TEXT                                                                           null,
    created_at               DATETIME(6)                                       DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    deposit_months           INT UNSIGNED                                                                   null,
    payment_cycle_months     TINYINT UNSIGNED                                                               null,
    occupant_count           TINYINT UNSIGNED                                  DEFAULT '1'                  NOT NULL,
    CONSTRAINT fk_dep_form_room
        FOREIGN KEY (room_id) REFERENCES hdbhms.rooms (room_id),
    CONSTRAINT fk_depform_id_back
        FOREIGN KEY (id_back_file_id) REFERENCES hdbhms.file_metadata (file_metadata_id),
    CONSTRAINT fk_depform_id_front
        FOREIGN KEY (id_front_file_id) REFERENCES hdbhms.file_metadata (file_metadata_id),
    CONSTRAINT fk_depform_portrait
        FOREIGN KEY (portrait_file_id) REFERENCES hdbhms.file_metadata (file_metadata_id)
);

CREATE TABLE IF NOT EXISTS hdbhms.deposit_form_co_occupants
(
    deposit_form_co_occupant_id BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    deposit_form_id             BIGINT UNSIGNED  NOT NULL,
    full_name                   VARCHAR(255)     NOT NULL,
    phone                       VARCHAR(30)      NOT NULL,
    display_order               TINYINT UNSIGNED NOT NULL,
    CONSTRAINT uq_deposit_form_co_occupant_order
        UNIQUE (deposit_form_id, display_order),
    CONSTRAINT fk_deposit_form_co_occupants_form
        FOREIGN KEY (deposit_form_id) REFERENCES hdbhms.deposit_forms (deposit_form_id)
            on delete cascade
);

CREATE INDEX idx_deposit_form_co_occupants_form
    ON hdbhms.deposit_form_co_occupants (deposit_form_id);

CREATE INDEX idx_deposit_room_status
    ON hdbhms.deposit_forms (room_id, status);

CREATE INDEX idx_files_owner
    ON hdbhms.file_metadata (owner_user_id);

CREATE INDEX idx_files_tenant_category
    ON hdbhms.file_metadata (category, created_at);

CREATE TABLE IF NOT EXISTS hdbhms.leads
(
    lead_id              BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    property_id          BIGINT UNSIGNED                          null,
    user_id              BIGINT UNSIGNED                          null,
    desired_move_in_date DATE                                     null,
    note                 TEXT                                     null,
    created_at           DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    updated_at           DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL on update CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_lead_assigned
        FOREIGN KEY (user_id) REFERENCES hdbhms.users (user_id),
    CONSTRAINT fk_lead_property
        FOREIGN KEY (property_id) REFERENCES hdbhms.properties (property_id)
);

CREATE INDEX idx_lead_assigned
    ON hdbhms.leads (user_id, created_at);

CREATE TABLE IF NOT EXISTS hdbhms.login_history
(
    login_history_id BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    user_id          BIGINT UNSIGNED                          NOT NULL,
    status           VARCHAR(50)                              NOT NULL,
    ip_address       VARCHAR(45)                              null,
    user_agent       VARCHAR(500)                             null,
    method           VARCHAR(50)                              NOT NULL,
    session_id       VARCHAR(255)                             null,
    device_id        VARCHAR(255)                             null,
    logged_in_at     DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_login_history_user
        FOREIGN KEY (user_id) REFERENCES hdbhms.users (user_id)
);

CREATE INDEX idx_login_history_user
    ON hdbhms.login_history (user_id);

CREATE TABLE IF NOT EXISTS hdbhms.meter_reading_batches
(
    meter_reading_batch_id BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    property_id            BIGINT UNSIGNED                                                                    NOT NULL,
    reading_period         char(7)                                                                            NOT NULL,
    status                 ENUM ('DRAFT', 'PREVIEWED', 'CONFIRMED', 'CANCELLED') DEFAULT 'DRAFT'              NOT NULL,
    imported_file_id       BIGINT UNSIGNED                                                                    null,
    created_by             BIGINT UNSIGNED                                                                    null,
    confirmed_by           BIGINT UNSIGNED                                                                    null,
    confirmed_at           DATETIME(6)                                                                        null,
    created_at             DATETIME(6)                                           DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    total_rooms            INT                                                   DEFAULT 0                    NOT NULL,
    completed_rooms        INT                                                   DEFAULT 0                    NOT NULL,
    anomaly_count          INT                                                   DEFAULT 0                    NOT NULL,
    CONSTRAINT fk_mrb_confirmed
        FOREIGN KEY (confirmed_by) REFERENCES hdbhms.users (user_id),
    CONSTRAINT fk_mrb_created
        FOREIGN KEY (created_by) REFERENCES hdbhms.users (user_id),
    CONSTRAINT fk_mrb_file
        FOREIGN KEY (imported_file_id) REFERENCES hdbhms.file_metadata (file_metadata_id),
    CONSTRAINT fk_mrb_property
        FOREIGN KEY (property_id) REFERENCES hdbhms.properties (property_id)
);

CREATE INDEX idx_reading_batch
    ON hdbhms.meter_reading_batches (property_id, reading_period, status);

CREATE TABLE IF NOT EXISTS hdbhms.meter_reading_import_rows
(
    meter_reading_import_row_id BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    batch_id                    BIGINT UNSIGNED                                                 NOT NULL,
    room_code                   VARCHAR(50)                                                     NOT NULL,
    meter_type                  ENUM ('ELECTRICITY', 'WATER')                                   NOT NULL,
    previous_value              DECIMAL(12, 3)                                                  null,
    current_value               DECIMAL(12, 3)                                                  null,
    validation_status           ENUM ('VALID', 'WARNING', 'ERROR') DEFAULT 'VALID'              NOT NULL,
    validation_message          TEXT                                                            null,
    created_at                  DATETIME(6)                        DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    row_no                      INT                                DEFAULT 0                    NOT NULL,
    CONSTRAINT fk_mrir_batch
        FOREIGN KEY (batch_id) REFERENCES hdbhms.meter_reading_batches (meter_reading_batch_id)
);

CREATE INDEX idx_mrir_batch
    ON hdbhms.meter_reading_import_rows (batch_id);

CREATE TABLE IF NOT EXISTS hdbhms.meter_readings
(
    meter_reading_id   BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    batch_id           BIGINT UNSIGNED                                                                                            null,
    meter_id           BIGINT UNSIGNED                                                                                            NOT NULL,
    room_id            BIGINT UNSIGNED                                                                                            NOT NULL,
    reading_period     char(7)                                                                                                    NOT NULL,
    revision_no        INT UNSIGNED                                                                  DEFAULT '1'                  NOT NULL,
    previous_value     DECIMAL(12, 3)                                                                                             NOT NULL,
    current_value      DECIMAL(12, 3)                                                                                             NOT NULL,
    usage_amount       DECIMAL(12, 3) as ((`current_value` - `previous_value`)) stored,
    reading_date       DATE                                                                                                       NOT NULL,
    photo_file_id      BIGINT UNSIGNED                                                                                            null,
    status             ENUM ('CONFIRMED', 'VOIDED')                                                  DEFAULT 'CONFIRMED'          NOT NULL,
    void_reason        VARCHAR(1000)                                                                                              null,
    created_by         BIGINT UNSIGNED                                                                                            null,
    created_at         DATETIME(6)                                                                   DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    active_reading_key VARCHAR(255) as (if((`status` <> _utf8mb4'VOIDED'),
                                           concat(`meter_id`, _utf8mb4':', `reading_period`), NULL)),
    purpose            ENUM ('MONTHLY', 'MOVE_OUT', 'TRANSFER', 'HANDOVER', 'CONTRACT_START')        DEFAULT 'MONTHLY'            NOT NULL,
    source             ENUM ('MANUAL', 'EXCEL_IMPORT', 'API')                                        DEFAULT 'MANUAL'             NOT NULL,
    review_status      ENUM ('NONE', 'PENDING', 'UNDER_REVIEW', 'APPROVED', 'REJECTED', 'CANCELLED') DEFAULT 'NONE'               NOT NULL,
    review_count       INT UNSIGNED                                                                  DEFAULT '0'                  NOT NULL,
    CONSTRAINT uq_meter_period_active
        UNIQUE (active_reading_key),
    CONSTRAINT uq_meter_period_revision
        UNIQUE (meter_id, reading_period, revision_no),
    CONSTRAINT fk_mr_batch
        FOREIGN KEY (batch_id) REFERENCES hdbhms.meter_reading_batches (meter_reading_batch_id),
    CONSTRAINT fk_mr_meter
        FOREIGN KEY (meter_id) REFERENCES hdbhms.meters (meter_id),
    CONSTRAINT fk_mr_photo
        FOREIGN KEY (photo_file_id) REFERENCES hdbhms.file_metadata (file_metadata_id),
    CONSTRAINT fk_mr_room
        FOREIGN KEY (room_id) REFERENCES hdbhms.rooms (room_id),
    CONSTRAINT fk_mr_user
        FOREIGN KEY (created_by) REFERENCES hdbhms.users (user_id)
);

CREATE TABLE IF NOT EXISTS hdbhms.meter_reading_anomalies
(
    meter_reading_anomaly_id BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    meter_reading_id         BIGINT UNSIGNED                                                   NOT NULL,
    anomaly_type             ENUM ('HIGH_USAGE', 'NEGATIVE_USAGE', 'MISSING_READING', 'OTHER') NOT NULL,
    message                  TEXT                                                              NOT NULL,
    severity                 ENUM ('LOW', 'MEDIUM', 'HIGH') DEFAULT 'MEDIUM'                   NOT NULL,
    resolved_at              DATETIME(6)                                                       null,
    resolved_by              BIGINT UNSIGNED                                                   null,
    created_at               DATETIME(6)                    DEFAULT CURRENT_TIMESTAMP(6)       NOT NULL,
    batch_id                 BIGINT UNSIGNED                                                   null,
    CONSTRAINT fk_mra_batch
        FOREIGN KEY (batch_id) REFERENCES hdbhms.meter_reading_batches (meter_reading_batch_id),
    CONSTRAINT fk_mra_reading
        FOREIGN KEY (meter_reading_id) REFERENCES hdbhms.meter_readings (meter_reading_id),
    CONSTRAINT fk_mra_resolved_by
        FOREIGN KEY (resolved_by) REFERENCES hdbhms.users (user_id)
);

CREATE INDEX idx_mra_reading
    ON hdbhms.meter_reading_anomalies (meter_reading_id);

CREATE INDEX idx_reading_room_period
    ON hdbhms.meter_readings (room_id, reading_period);

CREATE TABLE IF NOT EXISTS hdbhms.notification_outbox
(
    notification_outbox_id BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    event_type             VARCHAR(100)                                                                       NOT NULL,
    target_type            VARCHAR(100)                                                                       null,
    target_id              BIGINT UNSIGNED                                                                    null,
    recipient_user_id      BIGINT UNSIGNED                                                                    NOT NULL,
    channel                ENUM ('PUSH', 'WEB', 'IN_APP', 'EMAIL', 'SMS')                                     NOT NULL,
    title                  VARCHAR(255)                                                                       NOT NULL,
    body                   TEXT                                                                               NOT NULL,
    payload                JSON                                                                               null,
    status                 ENUM ('PENDING', 'PROCESSING', 'SENT', 'DEAD_LETTER') DEFAULT 'PENDING'            NOT NULL,
    retry_count            TINYINT UNSIGNED                                      DEFAULT '0'                  NOT NULL,
    max_retries            TINYINT UNSIGNED                                      DEFAULT '3'                  NOT NULL,
    last_error             VARCHAR(1000)                                                                      null,
    scheduled_at           DATETIME(6)                                           DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    sent_at                DATETIME(6)                                                                        null,
    created_at             DATETIME(6)                                           DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    is_read                tinyint(1)                                            DEFAULT 0                    NOT NULL,
    next_retry_at          DATETIME(6)                                                                        null,
    CONSTRAINT fk_no_user
        FOREIGN KEY (recipient_user_id) REFERENCES hdbhms.users (user_id)
);

CREATE TABLE IF NOT EXISTS hdbhms.notification_deliveries
(
    notification_delivery_id BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    outbox_id                BIGINT UNSIGNED                              NOT NULL,
    provider_message_id      VARCHAR(255)                                 null,
    delivery_status          ENUM ('SENT', 'DELIVERED', 'READ', 'FAILED') NOT NULL,
    error_message            VARCHAR(1000)                                null,
    delivered_at             DATETIME(6)                                  null,
    read_at                  DATETIME(6)                                  null,
    created_at               DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)     NOT NULL,
    CONSTRAINT fk_nd_outbox
        FOREIGN KEY (outbox_id) REFERENCES hdbhms.notification_outbox (notification_outbox_id)
);

CREATE INDEX idx_delivery_outbox
    ON hdbhms.notification_deliveries (outbox_id);

CREATE INDEX idx_delivery_read_status
    ON hdbhms.notification_deliveries (delivery_status, read_at, created_at);

CREATE INDEX idx_delivery_status
    ON hdbhms.notification_deliveries (delivery_status, created_at);

CREATE INDEX idx_outbox_pending
    ON hdbhms.notification_outbox (status, next_retry_at);

CREATE INDEX idx_outbox_recipient
    ON hdbhms.notification_outbox (recipient_user_id, created_at);

CREATE TABLE IF NOT EXISTS hdbhms.ocr_jobs
(
    id            BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    input_file_id BIGINT UNSIGNED                                                                                                 NOT NULL,
    document_type ENUM ('LEASE_CONTRACT', 'DEPOSIT_CONTRACT', 'IDENTITY_DOCUMENT', 'VEHICLE_PLATE', 'OTHER')                      NOT NULL,
    status        ENUM ('QUEUED', 'PROCESSING', 'REVIEW_REQUIRED', 'APPROVED', 'REJECTED', 'FAILED') DEFAULT 'QUEUED'             NOT NULL,
    target_type   VARCHAR(100)                                                                                                    null,
    target_id     BIGINT UNSIGNED                                                                                                 null,
    raw_result    BLOB                                                                                                            null,
    reviewed_by   BIGINT UNSIGNED                                                                                                 null,
    reviewed_at   DATETIME(6)                                                                                                     null,
    created_at    DATETIME(6)                                                                        DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_ocr_file
        FOREIGN KEY (input_file_id) REFERENCES hdbhms.file_metadata (file_metadata_id),
    CONSTRAINT fk_ocr_reviewer
        FOREIGN KEY (reviewed_by) REFERENCES hdbhms.users (user_id)
);

CREATE TABLE IF NOT EXISTS hdbhms.ocr_extracted_fields
(
    id              BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    ocr_job_id      BIGINT UNSIGNED                                                                      NOT NULL,
    field_name      VARCHAR(100)                                                                         NOT NULL,
    extracted_value TEXT                                                                                 null,
    corrected_value TEXT                                                                                 null,
    confidence      DECIMAL(5, 4)                                                                        null,
    status          ENUM ('EXTRACTED', 'CORRECTED', 'ACCEPTED', 'REJECTED') DEFAULT 'EXTRACTED'          NOT NULL,
    created_at      DATETIME(6)                                                                          DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    CONSTRAINT uq_ocr_field
        UNIQUE (ocr_job_id, field_name),
    CONSTRAINT fk_oef_job
        FOREIGN KEY (ocr_job_id) REFERENCES hdbhms.ocr_jobs (id)
);

CREATE INDEX idx_ocr_status
    ON hdbhms.ocr_jobs (status, created_at);

CREATE INDEX idx_ocr_target
    ON hdbhms.ocr_jobs (target_type, target_id);

CREATE TABLE IF NOT EXISTS hdbhms.payment_transactions
(
    payment_transaction_id  BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    provider                ENUM ('BANK', 'MOMO', 'ZALOPAY', 'CASH', 'MANUAL', 'PAYOS')                                                                                 NOT NULL,
    provider_transaction_id VARCHAR(255)                                                                                                                                null,
    collection_account_id   BIGINT UNSIGNED                                                                                                                             null,
    amount                  BIGINT UNSIGNED                                                                                                                             NOT NULL,
    transaction_time        DATETIME(6)                                                                                                                                 NOT NULL,
    payer_name              VARCHAR(255)                                                                                                                                null,
    payer_account           VARCHAR(255)                                                                                                                                null,
    content                 VARCHAR(1000)                                                                                                                               null,
    status                  ENUM ('PENDING_RECONCILE', 'MATCHED', 'PARTIALLY_ALLOCATED', 'ALLOCATED', 'DUPLICATE', 'REJECTED', 'REFUNDED') DEFAULT 'PENDING_RECONCILE'  NOT NULL,
    raw_payload             BLOB                                                                                                                                        null,
    confirmed_by            BIGINT UNSIGNED                                                                                                                             null,
    confirmed_at            DATETIME(6)                                                                                                                                 null,
    created_at              DATETIME(6)                                                                                                    DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    CONSTRAINT uq_provider_txn
        UNIQUE (provider, provider_transaction_id),
    CONSTRAINT fk_pt_account
        FOREIGN KEY (collection_account_id) REFERENCES hdbhms.collection_accounts (collection_account_id),
    CONSTRAINT fk_pt_user
        FOREIGN KEY (confirmed_by) REFERENCES hdbhms.users (user_id)
);

CREATE INDEX idx_payment_content
    ON hdbhms.payment_transactions (content(100));

CREATE INDEX idx_payment_reconcile
    ON hdbhms.payment_transactions (status, amount, transaction_time);

CREATE INDEX idx_payment_txn_status
    ON hdbhms.payment_transactions (status, transaction_time);

CREATE TRIGGER hdbhms.trg_payment_transactions_guard_update
    BEFORE UPDATE
    ON hdbhms.payment_transactions
    FOR EACH ROW
BEGIN
    IF OLD.status IN ('MATCHED', 'PARTIALLY_ALLOCATED', 'ALLOCATED', 'DUPLICATE', 'REJECTED', 'REFUNDED') THEN
        IF NOT (NEW.status = OLD.status AND NEW.confirmed_by <=> OLD.confirmed_by AND
                NEW.confirmed_at <=> OLD.confirmed_at) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Terminal payment transactions are immutable';
        END IF;
    END IF;
END;

CREATE TABLE IF NOT EXISTS hdbhms.permission_requests
(
    permission_request_id BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    requester_user_id     BIGINT UNSIGNED                                                                             NOT NULL,
    target_type           ENUM ('TENANT_PROFILE', 'IDENTITY_DOCUMENT', 'CONTRACT', 'REPORT', 'FILE')                  NOT NULL,
    target_id             BIGINT UNSIGNED                                                                             NOT NULL,
    rejected_reason       VARCHAR(1000)                                                                               NOT NULL,
    status                ENUM ('PENDING', 'APPROVED', 'REJECTED', 'EXPIRED', 'REVOKED') DEFAULT 'PENDING'            NOT NULL,
    expires_at            DATETIME(6)                                                                                 null,
    decided_at            DATETIME(6)                                                                                 null,
    created_at            DATETIME(6)                                                    DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_perm_requester
        FOREIGN KEY (requester_user_id) REFERENCES hdbhms.users (user_id)
);

CREATE INDEX idx_perm_target
    ON hdbhms.permission_requests (target_type, target_id);

CREATE INDEX idx_perm_tenant_status
    ON hdbhms.permission_requests (status, created_at);

CREATE TABLE IF NOT EXISTS hdbhms.person_profiles
(
    person_profile_id BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    user_id           BIGINT UNSIGNED                                                          null,
    full_name         VARCHAR(255)                                                             NOT NULL,
    dob               DATE                                                                     null,
    gender            ENUM ('MALE', 'FEMALE', 'OTHER', 'UNKNOWN') DEFAULT 'UNKNOWN'            NOT NULL,
    phone             VARCHAR(30)                                                              null,
    email             VARCHAR(255)                                                             null,
    permanent_address VARCHAR(1000)                                                            null,
    portrait_file_id  BIGINT UNSIGNED                                                          null,
    created_at        DATETIME(6)                                 DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    updated_at        DATETIME(6)                                 DEFAULT CURRENT_TIMESTAMP(6) NOT NULL on update CURRENT_TIMESTAMP(6),
    deleted_at        DATETIME(6)                                                              null,
    CONSTRAINT uq_person_profile_user
        UNIQUE (user_id),
    CONSTRAINT fk_pp_portrait
        FOREIGN KEY (portrait_file_id) REFERENCES hdbhms.file_metadata (file_metadata_id),
    CONSTRAINT fk_pp_user
        FOREIGN KEY (user_id) REFERENCES hdbhms.users (user_id)
);

CREATE TABLE IF NOT EXISTS hdbhms.identity_documents
(
    identity_document_id BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    profile_id           BIGINT UNSIGNED                                                                NOT NULL,
    doc_type             ENUM ('CCCD', 'CMND', 'PASSPORT', 'OTHER')        DEFAULT 'CCCD'               NOT NULL,
    doc_number           VARCHAR(50)                                                                    NOT NULL,
    issued_date          DATE                                                                           null,
    issued_place         VARCHAR(255)                                                                   null,
    expiry_date          DATE                                                                           null,
    raw_ocr_data         BLOB                                                                           null,
    front_file_id        BIGINT UNSIGNED                                                                null,
    back_file_id         BIGINT UNSIGNED                                                                null,
    status               ENUM ('ACTIVE', 'EXPIRED', 'REPLACED', 'INVALID') DEFAULT 'ACTIVE'             NOT NULL,
    created_at           DATETIME(6)                                       DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    updated_at           DATETIME(6)                                       DEFAULT CURRENT_TIMESTAMP(6) NOT NULL on update CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_doc_number_tenant
        UNIQUE (doc_type, doc_number),
    CONSTRAINT fk_doc_back
        FOREIGN KEY (back_file_id) REFERENCES hdbhms.file_metadata (file_metadata_id),
    CONSTRAINT fk_doc_front
        FOREIGN KEY (front_file_id) REFERENCES hdbhms.file_metadata (file_metadata_id),
    CONSTRAINT fk_doc_profile
        FOREIGN KEY (profile_id) REFERENCES hdbhms.person_profiles (person_profile_id)
);

CREATE INDEX idx_doc_profile
    ON hdbhms.identity_documents (profile_id);

CREATE INDEX idx_person_phone
    ON hdbhms.person_profiles (phone);

CREATE INDEX idx_pp_tenant_name_phone
    ON hdbhms.person_profiles (full_name, phone);

CREATE TABLE IF NOT EXISTS hdbhms.property_images
(
    property_image_id BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    property_id       BIGINT UNSIGNED                          NOT NULL,
    file_id           BIGINT UNSIGNED                          NOT NULL,
    sort_order        INT                         DEFAULT 0                    NOT NULL,
    created_at        DATETIME(6)                 DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_property_images_file
        FOREIGN KEY (file_id) REFERENCES hdbhms.file_metadata (file_metadata_id),
    CONSTRAINT fk_property_images_property
        FOREIGN KEY (property_id) REFERENCES hdbhms.properties (property_id)
);

CREATE INDEX idx_property_images
    ON hdbhms.property_images (property_id);

CREATE TABLE IF NOT EXISTS hdbhms.property_staff_assignments
(
    property_staff_assignment_id bigint unsigned auto_increment
        PRIMARY KEY,
    property_id                  bigint unsigned                                                                                          not null,
    staff_user_id                bigint unsigned                                                                                          not null,
    assigned_role                ENUM ('MANAGER', 'ACCOUNTANT', 'STAFF', 'MAINTENANCE', 'SECURITY', 'OTHER') DEFAULT 'STAFF'              NOT NULL,
    assignment_status            ENUM ('ACTIVE', 'INACTIVE', 'REMOVED')                                      DEFAULT 'ACTIVE'             NOT NULL,
    is_primary                   tinyint(1)                                                                  DEFAULT 0                    NOT NULL,
    notes                        VARCHAR(1000)                                                                                            null,
    assigned_by_user_id          BIGINT UNSIGNED                                                                                          null,
    started_at                   DATETIME(6)                                                                 DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    ended_at                     DATETIME(6)                                                                                              null,
    created_at                   DATETIME(6)                                                                 DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    updated_at                   DATETIME(6)                                                                 DEFAULT CURRENT_TIMESTAMP(6) NOT NULL on update CURRENT_TIMESTAMP(6),
    active_unique_token          tinyint as (if((`assignment_status` = _utf8mb4'ACTIVE'), 1, NULL)),
    primary_unique_token         tinyint as (if(((`is_primary` = 1) and (`assignment_status` = _utf8mb4'ACTIVE')), 1,
                                                NULL)),
    CONSTRAINT uq_property_staff_active_role
        UNIQUE (property_id, staff_user_id, assigned_role, active_unique_token),
    CONSTRAINT uq_property_staff_primary_role
        UNIQUE (property_id, assigned_role, primary_unique_token),
    CONSTRAINT fk_property_staff_assignment_assigned_by
        FOREIGN KEY (assigned_by_user_id) REFERENCES hdbhms.users (user_id),
    CONSTRAINT fk_property_staff_assignment_property
        FOREIGN KEY (property_id) REFERENCES hdbhms.properties (property_id),
    CONSTRAINT fk_property_staff_assignment_user
        FOREIGN KEY (staff_user_id) REFERENCES hdbhms.users (user_id),
    CONSTRAINT chk_property_staff_ended_at
        CHECK ((`ended_at` is null) or (`ended_at` >= `started_at`))
);

CREATE INDEX idx_property_staff_assigned_by
    ON hdbhms.property_staff_assignments (assigned_by_user_id);

CREATE INDEX idx_property_staff_property_status
    ON hdbhms.property_staff_assignments (property_id, assignment_status, assigned_role);

CREATE INDEX idx_property_staff_user_status
    ON hdbhms.property_staff_assignments (staff_user_id, assignment_status, started_at);

CREATE TABLE IF NOT EXISTS hdbhms.role_promotions
(
    role_promotion_id   BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    user_id             BIGINT UNSIGNED                                                                 NOT NULL,
    role                ENUM ('MANAGER', 'ACCOUNTANT', 'TENANT')                                        NOT NULL,
    status              ENUM ('PENDING', 'ACTIVE', 'DISABLED', 'REJECTED') DEFAULT 'PENDING'            NOT NULL,
    property_id         BIGINT UNSIGNED                                                                 null,
    approved_at         DATETIME(6)                                                                     null,
    created_at          DATETIME(6)                                        DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    updated_at          DATETIME(6)                                        DEFAULT CURRENT_TIMESTAMP(6) NOT NULL on update CURRENT_TIMESTAMP(6),
    deleted_at          DATETIME(6)                                                                     null,
    active_unique_token tinyint as (if((`deleted_at` is null), 1, NULL)),
    CONSTRAINT uq_membership_active
        UNIQUE (user_id, role, active_unique_token),
    CONSTRAINT fk_membership_property
        FOREIGN KEY (property_id) REFERENCES hdbhms.properties (property_id),
    CONSTRAINT fk_membership_user
        FOREIGN KEY (user_id) REFERENCES hdbhms.users (user_id)
);

CREATE INDEX idx_membership_property
    ON hdbhms.role_promotions (property_id, role);

CREATE INDEX idx_membership_tenant_role
    ON hdbhms.role_promotions (role, status);

CREATE TABLE IF NOT EXISTS hdbhms.room_assets
(
    room_asset_id     BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    room_id           BIGINT UNSIGNED                                                              NOT NULL,
    asset_name        VARCHAR(255)                                                                 NOT NULL,
    asset_category    VARCHAR(100)                                                                 null,
    quantity          INT                                             DEFAULT 1                    NOT NULL,
    current_condition ENUM ('GOOD', 'ATTENTION', 'BROKEN', 'MISSING') DEFAULT 'GOOD'               NOT NULL,
    description       TEXT                                                                         null,
    image_file_id     BIGINT UNSIGNED                                                              null,
    created_at        DATETIME(6)                                     DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    updated_at        DATETIME(6)                                     DEFAULT CURRENT_TIMESTAMP(6) NOT NULL on update CURRENT_TIMESTAMP(6),
    deleted_at        DATETIME(6)                                                                  null,
    CONSTRAINT fk_room_asset_file
        FOREIGN KEY (image_file_id) REFERENCES hdbhms.file_metadata (file_metadata_id),
    CONSTRAINT fk_room_asset_room
        FOREIGN KEY (room_id) REFERENCES hdbhms.rooms (room_id)
);

CREATE INDEX idx_room_assets_room
    ON hdbhms.room_assets (room_id);

CREATE TABLE IF NOT EXISTS hdbhms.room_images
(
    room_image_id BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    room_id       BIGINT UNSIGNED                          NOT NULL,
    file_id       BIGINT UNSIGNED                          NOT NULL,
    sort_order    INT                         DEFAULT 0                    NOT NULL,
    created_at    DATETIME(6)                 DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_room_img_file
        FOREIGN KEY (file_id) REFERENCES hdbhms.file_metadata (file_metadata_id),
    CONSTRAINT fk_room_img_room
        FOREIGN KEY (room_id) REFERENCES hdbhms.rooms (room_id)
);

CREATE INDEX idx_room_images
    ON hdbhms.room_images (room_id);

CREATE TABLE IF NOT EXISTS hdbhms.room_status_history
(
    room_status_history_id BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    room_id                BIGINT UNSIGNED                                                                  NOT NULL,
    from_status            ENUM ('VACANT', 'RESERVED', 'OCCUPIED', 'SOON_VACANT', 'MAINTENANCE', 'EXPIRED') null,
    to_status              ENUM ('VACANT', 'RESERVED', 'OCCUPIED', 'SOON_VACANT', 'MAINTENANCE', 'EXPIRED') NOT NULL,
    reason                 VARCHAR(1000)                                                                    null,
    changed_by             BIGINT UNSIGNED                                                                  null,
    changed_at             DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)                                         NOT NULL,
    CONSTRAINT fk_rsh_room
        FOREIGN KEY (room_id) REFERENCES hdbhms.rooms (room_id),
    CONSTRAINT fk_rsh_user
        FOREIGN KEY (changed_by) REFERENCES hdbhms.users (user_id)
);

CREATE INDEX idx_room_status_history
    ON hdbhms.room_status_history (room_id, changed_at);

CREATE TABLE IF NOT EXISTS hdbhms.tenants
(
    tenant_id           BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    user_id             BIGINT UNSIGNED                          NOT NULL,
    property_id         BIGINT UNSIGNED                          NOT NULL,
    created_at          DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    updated_at          DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL on update CURRENT_TIMESTAMP(6),
    deleted_at          DATETIME(6)                              null,
    active_tenant_token tinyint as (if((`deleted_at` is null), 1, NULL)),
    CONSTRAINT uq_tenant_active
        UNIQUE (user_id, property_id, active_tenant_token),
    CONSTRAINT fk_tenant_property
        FOREIGN KEY (property_id) REFERENCES hdbhms.properties (property_id),
    CONSTRAINT fk_tenant_user
        FOREIGN KEY (user_id) REFERENCES hdbhms.users (user_id)
);

CREATE TABLE IF NOT EXISTS hdbhms.room_holds
(
    room_hold_id    BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    room_id         BIGINT UNSIGNED                                                                                         NOT NULL,
    tenant_id       BIGINT UNSIGNED                                                                                         null,
    status          ENUM ('ACTIVE', 'PAYMENT_PROCESSING', 'CONFIRMED', 'EXPIRED', 'CANCELLED') DEFAULT 'ACTIVE'             NOT NULL,
    expires_at      DATETIME(6)                                                                                             NOT NULL,
    created_at      DATETIME(6)                                                                DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    released_at     DATETIME(6)                                                                                             null,
    active_room_key BIGINT UNSIGNED as (if((`status` in (_utf8mb4'ACTIVE', _utf8mb4'PAYMENT_PROCESSING')), `room_id`,
                                           NULL)),
    CONSTRAINT uq_active_room_hold
        UNIQUE (active_room_key),
    CONSTRAINT fk_hold_room
        FOREIGN KEY (room_id) REFERENCES hdbhms.rooms (room_id),
    CONSTRAINT fk_hold_tenant
        FOREIGN KEY (tenant_id) REFERENCES hdbhms.tenants (tenant_id)
);

CREATE TABLE IF NOT EXISTS hdbhms.deposit_agreements
(
    deposit_agreement_id        BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    deposit_code                VARCHAR(80)                                                                                                                                                 NOT NULL,
    room_id                     BIGINT UNSIGNED                                                                                                                                             NOT NULL,
    room_hold_id                BIGINT UNSIGNED                                                                                                                                             null,
    deposit_form_id             BIGINT UNSIGNED                                                                                                                                             null,
    tenant_id                   BIGINT UNSIGNED                                                                                                                                             null,
    lead_id                     BIGINT UNSIGNED                                                                                                                                             null,
    depositor_person_profile_id BIGINT UNSIGNED                                                                                                                                             null,
    amount                      BIGINT UNSIGNED                                                                                                                                             NOT NULL,
    expected_move_in_date       DATE                                                                                                                                                        NOT NULL,
    expected_lease_sign_date    DATE                                                                                                                                                        NOT NULL,
    payment_due_at              DATETIME(6)                                                                                                                                                 null,
    deposit_expires_at          DATE                                                                                                                                                        null,
    extension_count             TINYINT UNSIGNED                                                                                                               DEFAULT '0'                  NOT NULL,
    max_extensions              TINYINT UNSIGNED                                                                                                               DEFAULT '1'                  NOT NULL,
    status                      ENUM ('DRAFT', 'PENDING_PAYMENT', 'PAID', 'CONFIRMED', 'CONVERTED_TO_LEASE', 'EXTENDED', 'REFUNDED', 'FORFEITED', 'CANCELLED') DEFAULT 'DRAFT'              NOT NULL,
    confirmed_at                DATETIME(6)                                                                                                                                                 null,
    contract_file_id            BIGINT UNSIGNED                                                                                                                                             null,
    signed_file_id              BIGINT UNSIGNED                                                                                                                                             null,
    signed_at                   DATETIME(6)                                                                                                                                                 null,
    signed_uploaded_by          BIGINT UNSIGNED                                                                                                                                             null,
    note                        TEXT                                                                                                                                                        null,
    forfeiture_reason           TEXT                                                                                                                                                        null,
    refunded_amount             BIGINT UNSIGNED                                                                                                                                             null,
    created_at                  DATETIME(6)                                                                                                                    DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    updated_at                  DATETIME(6)                                                                                                                    DEFAULT CURRENT_TIMESTAMP(6) NOT NULL on update CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_deposit_code
        UNIQUE (deposit_code),
    CONSTRAINT fk_dep_agreement_file
        FOREIGN KEY (contract_file_id) REFERENCES hdbhms.file_metadata (file_metadata_id),
    CONSTRAINT fk_dep_agreement_form
        FOREIGN KEY (deposit_form_id) REFERENCES hdbhms.deposit_forms (deposit_form_id),
    CONSTRAINT fk_dep_agreement_hold
        FOREIGN KEY (room_hold_id) REFERENCES hdbhms.room_holds (room_hold_id),
    CONSTRAINT fk_dep_agreement_lead
        FOREIGN KEY (lead_id) REFERENCES hdbhms.leads (lead_id),
    CONSTRAINT fk_dep_agreement_person_profile
        FOREIGN KEY (depositor_person_profile_id) REFERENCES hdbhms.person_profiles (person_profile_id),
    CONSTRAINT fk_dep_agreement_room
        FOREIGN KEY (room_id) REFERENCES hdbhms.rooms (room_id),
    CONSTRAINT fk_dep_agreement_signed_by
        FOREIGN KEY (signed_uploaded_by) REFERENCES hdbhms.users (user_id),
    CONSTRAINT fk_dep_agreement_signed_file
        FOREIGN KEY (signed_file_id) REFERENCES hdbhms.file_metadata (file_metadata_id),
    CONSTRAINT fk_dep_agreement_tenant
        FOREIGN KEY (tenant_id) REFERENCES hdbhms.tenants (tenant_id)
);

CREATE INDEX idx_dep_agreement_form
    ON hdbhms.deposit_agreements (deposit_form_id);

CREATE INDEX idx_dep_agreement_hold
    ON hdbhms.deposit_agreements (room_hold_id);

CREATE INDEX idx_deposit_lead
    ON hdbhms.deposit_agreements (lead_id, status);

CREATE INDEX idx_deposit_person
    ON hdbhms.deposit_agreements (depositor_person_profile_id, status);

CREATE INDEX idx_deposit_room_status
    ON hdbhms.deposit_agreements (room_id, status);

CREATE TABLE IF NOT EXISTS hdbhms.deposit_batch_items
(
    deposit_batch_item_id BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    batch_id              BIGINT UNSIGNED                                                                                    NOT NULL,
    room_id               BIGINT UNSIGNED                                                                                    NOT NULL,
    room_hold_id          BIGINT UNSIGNED                                                                                    null,
    deposit_form_id       BIGINT UNSIGNED                                                                                    null,
    deposit_agreement_id  BIGINT UNSIGNED                                                                                    null,
    deposit_amount        BIGINT UNSIGNED                                                       DEFAULT '0'                  NOT NULL,
    occupant_count        INT UNSIGNED                                                          DEFAULT '1'                  NOT NULL,
    status                ENUM ('PENDING_PAYMENT', 'PAID', 'CONFIRMED', 'EXPIRED', 'CANCELLED') DEFAULT 'PENDING_PAYMENT'    NOT NULL,
    created_at            DATETIME(6)                                                           DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    updated_at            DATETIME(6)                                                           DEFAULT CURRENT_TIMESTAMP(6) NOT NULL on update CURRENT_TIMESTAMP(6),
    version               BIGINT UNSIGNED                                                       DEFAULT '0'                  NOT NULL,
    CONSTRAINT uq_deposit_batch_item_agreement
        UNIQUE (deposit_agreement_id),
    CONSTRAINT uq_deposit_batch_item_form
        UNIQUE (deposit_form_id),
    CONSTRAINT uq_deposit_batch_item_hold
        UNIQUE (room_hold_id),
    CONSTRAINT uq_deposit_batch_item_room
        UNIQUE (batch_id, room_id),
    CONSTRAINT fk_deposit_batch_item_agreement
        FOREIGN KEY (deposit_agreement_id) REFERENCES hdbhms.deposit_agreements (deposit_agreement_id),
    CONSTRAINT fk_deposit_batch_item_batch
        FOREIGN KEY (batch_id) REFERENCES hdbhms.deposit_batches (deposit_batch_id),
    CONSTRAINT fk_deposit_batch_item_form
        FOREIGN KEY (deposit_form_id) REFERENCES hdbhms.deposit_forms (deposit_form_id),
    CONSTRAINT fk_deposit_batch_item_hold
        FOREIGN KEY (room_hold_id) REFERENCES hdbhms.room_holds (room_hold_id),
    CONSTRAINT fk_deposit_batch_item_room
        FOREIGN KEY (room_id) REFERENCES hdbhms.rooms (room_id),
    CONSTRAINT chk_deposit_batch_item_occupant_count
        CHECK (`occupant_count` > 0)
);

CREATE INDEX idx_deposit_batch_item_batch_status
    ON hdbhms.deposit_batch_items (batch_id, status);

CREATE INDEX idx_deposit_batch_item_room_status
    ON hdbhms.deposit_batch_items (room_id, status);

CREATE TABLE IF NOT EXISTS hdbhms.deposit_extension_events
(
    deposit_extension_event_id BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    deposit_agreement_id       BIGINT UNSIGNED                          NOT NULL,
    old_expected_move_in_date  DATE                                     NOT NULL,
    new_expected_move_in_date  DATE                                     NOT NULL,
    old_expires_at             DATE                                     null,
    new_expires_at             DATE                                     NOT NULL,
    reason                     TEXT                                     null,
    approved_by                BIGINT UNSIGNED                          NOT NULL,
    approved_at                DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_dee_deposit
        FOREIGN KEY (deposit_agreement_id) REFERENCES hdbhms.deposit_agreements (deposit_agreement_id),
    CONSTRAINT fk_dee_user
        FOREIGN KEY (approved_by) REFERENCES hdbhms.users (user_id)
);

CREATE INDEX idx_deposit_extension
    ON hdbhms.deposit_extension_events (deposit_agreement_id, approved_at);

CREATE TABLE IF NOT EXISTS hdbhms.lease_contracts
(
    lease_contract_id         BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    contract_code             VARCHAR(80)                                                                                                                                                                                                                  NOT NULL,
    room_id                   BIGINT UNSIGNED                                                                                                                                                                                                              NOT NULL,
    deposit_agreement_id      BIGINT UNSIGNED                                                                                                                                                                                                              null,
    primary_tenant_profile_id BIGINT UNSIGNED                                                                                                                                                                                                              NOT NULL,
    start_date                DATE                                                                                                                                                                                                                         NOT NULL,
    end_date                  DATE                                                                                                                                                                                                                         NOT NULL,
    rent_start_date           DATE                                                                                                                                                                                                                         NOT NULL,
    monthly_rent              BIGINT UNSIGNED                                                                                                                                                                                                              NOT NULL,
    payment_cycle_months      TINYINT UNSIGNED                                                                                                                                                                                                             NOT NULL,
    deposit_amount            BIGINT UNSIGNED                                                                                                                                                                                 DEFAULT '0'                  NOT NULL,
    status                    ENUM ('DRAFT', 'CONFIRMED', 'SIGNED', 'PENDING_SIGNATURE', 'ACTIVE', 'EXPIRING_SOON', 'EXPIRED', 'TERMINATION_PENDING', 'LIQUIDATED', 'RENEWED', 'AUTO_TERMINATED', 'CANCELLED', 'TRANSFERRED') DEFAULT 'DRAFT'              NOT NULL,
    tenant_intention          VARCHAR(50)                                                                                                                                                                                                                  null,
    expected_vacant_date      DATE                                                                                                                                                                                                                         null,
    intention_recorded_at     DATETIME(6)                                                                                                                                                                                                                  null,
    previous_contract_id      BIGINT UNSIGNED                                                                                                                                                                                                              null,
    contract_file_id          BIGINT UNSIGNED                                                                                                                                                                                                              null,
    signed_at                 DATETIME(6)                                                                                                                                                                                                                  null,
    created_by                BIGINT UNSIGNED                                                                                                                                                                                                              null,
    created_at                DATETIME(6)                                                                                                                                                                                     DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    updated_at                DATETIME(6)                                                                                                                                                                                     DEFAULT CURRENT_TIMESTAMP(6) NOT NULL on update CURRENT_TIMESTAMP(6),
    deleted_at                DATETIME(6)                                                                                                                                                                                                                  null,
    version                   BIGINT UNSIGNED                                                                                                                                                                                 DEFAULT '0'                  NOT NULL,
    CONSTRAINT uq_contract_code
        UNIQUE (contract_code),
    CONSTRAINT uq_lease_contracts_deposit_agreement
        UNIQUE (deposit_agreement_id),
    CONSTRAINT fk_lc_created_by
        FOREIGN KEY (created_by) REFERENCES hdbhms.tenants (tenant_id),
    CONSTRAINT fk_lc_deposit
        FOREIGN KEY (deposit_agreement_id) REFERENCES hdbhms.deposit_agreements (deposit_agreement_id),
    CONSTRAINT fk_lc_file
        FOREIGN KEY (contract_file_id) REFERENCES hdbhms.file_metadata (file_metadata_id),
    CONSTRAINT fk_lc_prev
        FOREIGN KEY (previous_contract_id) REFERENCES hdbhms.lease_contracts (lease_contract_id),
    CONSTRAINT fk_lc_room
        FOREIGN KEY (room_id) REFERENCES hdbhms.rooms (room_id),
    CONSTRAINT chk_payment_cycle
        CHECK (`payment_cycle_months` in (1, 3))
);

CREATE TABLE IF NOT EXISTS hdbhms.contract_events
(
    contract_event_id BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    contract_id       BIGINT UNSIGNED                                                                                                                                                                                             NOT NULL,
    event_type        ENUM ('CREATED', 'SIGNED', 'RENEWED', 'NOTICE_SENT', 'INTENTION_RECORDED', 'EXPIRED', 'LIQUIDATED', 'AUTO_TERMINATED', 'PRICE_CHANGED', 'OCCUPANT_CHANGED', 'TRANSFERRED', 'RENEWAL_AFTER_MOVE_OUT_INTENT') NOT NULL,
    event_data        BLOB                                                                                                                                                                                                        null,
    created_by        BIGINT UNSIGNED                                                                                                                                                                                             null,
    created_at        DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)                                                                                                                                                                    NOT NULL,
    CONSTRAINT fk_ce_contract
        FOREIGN KEY (contract_id) REFERENCES hdbhms.lease_contracts (lease_contract_id),
    CONSTRAINT fk_ce_user
        FOREIGN KEY (created_by) REFERENCES hdbhms.users (user_id)
);

CREATE INDEX idx_contract_events
    ON hdbhms.contract_events (contract_id, created_at);

CREATE TABLE IF NOT EXISTS hdbhms.contract_handover_records
(
    contract_handover_record_id BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    contract_id                 BIGINT UNSIGNED                                                       NOT NULL,
    room_id                     BIGINT UNSIGNED                                                       NOT NULL,
    handover_type               ENUM ('MOVE_IN', 'MOVE_OUT', 'TRANSFER_OUT', 'TRANSFER_IN')           NOT NULL,
    handover_date               DATETIME(6)                                                           NOT NULL,
    electricity_reading_id      BIGINT UNSIGNED                                                       null,
    water_reading_id            BIGINT UNSIGNED                                                       null,
    note                        TEXT                                                                  null,
    status                      ENUM ('DRAFT', 'CONFIRMED', 'CANCELLED') DEFAULT 'DRAFT'              NOT NULL,
    confirmed_by                BIGINT UNSIGNED                                                       null,
    confirmed_at                DATETIME(6)                                                           null,
    created_at                  DATETIME(6)                              DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    signed_document_id          BIGINT UNSIGNED                                                       null,
    CONSTRAINT fk_chr_confirmed_by
        FOREIGN KEY (confirmed_by) REFERENCES hdbhms.users (user_id),
    CONSTRAINT fk_chr_contract
        FOREIGN KEY (contract_id) REFERENCES hdbhms.lease_contracts (lease_contract_id),
    CONSTRAINT fk_chr_electricity_reading
        FOREIGN KEY (electricity_reading_id) REFERENCES hdbhms.meter_readings (meter_reading_id),
    CONSTRAINT fk_chr_room
        FOREIGN KEY (room_id) REFERENCES hdbhms.rooms (room_id),
    CONSTRAINT fk_chr_signed_document
        FOREIGN KEY (signed_document_id) REFERENCES hdbhms.file_metadata (file_metadata_id)
            on delete set null,
    CONSTRAINT fk_chr_water_reading
        FOREIGN KEY (water_reading_id) REFERENCES hdbhms.meter_readings (meter_reading_id)
);

CREATE INDEX idx_chr_contract
    ON hdbhms.contract_handover_records (contract_id, handover_type);

CREATE INDEX idx_chr_room
    ON hdbhms.contract_handover_records (room_id, handover_date);

CREATE TABLE IF NOT EXISTS hdbhms.contract_occupants
(
    contract_occupant_id BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    contract_id          BIGINT UNSIGNED                                                       NOT NULL,
    tenant_id            BIGINT UNSIGNED                                                       null,
    tenant_profile_id    BIGINT UNSIGNED                                                       null,
    occupant_role        ENUM ('PRIMARY', 'CO_OCCUPANT')          DEFAULT 'CO_OCCUPANT'        NOT NULL,
    move_in_date         DATE                                                                  NOT NULL,
    move_out_date        DATE                                                                  null,
    status               ENUM ('ACTIVE', 'MOVED_OUT', 'DISABLED') DEFAULT 'ACTIVE'             NOT NULL,
    disabled_reason      TEXT                                                                  null,
    disabled_by          BIGINT UNSIGNED                                                       null,
    disabled_at          DATETIME(6)                                                           null,
    created_at           DATETIME(6)                              DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    CONSTRAINT uq_contract_occupant_profile
        UNIQUE (contract_id, tenant_profile_id),
    CONSTRAINT fk_co_contract
        FOREIGN KEY (contract_id) REFERENCES hdbhms.lease_contracts (lease_contract_id),
    CONSTRAINT fk_co_tenant
        FOREIGN KEY (tenant_id) REFERENCES hdbhms.tenants (tenant_id),
    CONSTRAINT fk_co_tenant_profile
        FOREIGN KEY (tenant_profile_id) REFERENCES hdbhms.person_profiles (person_profile_id),
    CONSTRAINT fk_contract_occupants_disabled_by
        FOREIGN KEY (disabled_by) REFERENCES hdbhms.users (user_id)
);

CREATE INDEX idx_contract_occupants_disabled_by
    ON hdbhms.contract_occupants (disabled_by);

CREATE INDEX idx_occupant_contract_status
    ON hdbhms.contract_occupants (contract_id, status);

CREATE INDEX idx_occupant_profile_status
    ON hdbhms.contract_occupants (tenant_profile_id, status);

CREATE TRIGGER hdbhms.trg_contract_occupants_max_three_before_insert
    before insert
    ON hdbhms.contract_occupants
    FOR EACH ROW
BEGIN
    IF NEW.status = 'ACTIVE' THEN
        IF (SELECT COUNT(*)
            FROM contract_occupants
            WHERE contract_id = NEW.contract_id
              AND status = 'ACTIVE') >= 3 THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'A room contract can have at most 3 active occupants';
        END IF;
    END IF;
END;

CREATE TABLE IF NOT EXISTS hdbhms.contract_termination_notices
(
    contract_termination_notice_id BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    contract_id                    BIGINT UNSIGNED                                                                                   NOT NULL,
    notice_by                      ENUM ('LANDLORD', 'TENANT', 'MANAGER', 'SYSTEM')                                                  NOT NULL,
    notice_user_id                 BIGINT UNSIGNED                                                                                   null,
    notice_date                    DATE                                                                                              NOT NULL,
    expected_termination_date      DATE                                                                                              NOT NULL,
    reason                         TEXT                                                                                              null,
    evidence_file_id               BIGINT UNSIGNED                                                                                   null,
    status                         ENUM ('SUBMITTED', 'ACCEPTED', 'REJECTED', 'WITHDRAWN', 'COMPLETED') DEFAULT 'SUBMITTED'          NOT NULL,
    decided_by                     BIGINT UNSIGNED                                                                                   null,
    decided_at                     DATETIME(6)                                                                                       null,
    created_at                     DATETIME(6)                                                          DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_ctn_contract
        FOREIGN KEY (contract_id) REFERENCES hdbhms.lease_contracts (lease_contract_id),
    CONSTRAINT fk_ctn_decided
        FOREIGN KEY (decided_by) REFERENCES hdbhms.users (user_id),
    CONSTRAINT fk_ctn_file
        FOREIGN KEY (evidence_file_id) REFERENCES hdbhms.file_metadata (file_metadata_id),
    CONSTRAINT fk_ctn_user
        FOREIGN KEY (notice_user_id) REFERENCES hdbhms.users (user_id)
);

CREATE INDEX idx_ctn_contract
    ON hdbhms.contract_termination_notices (contract_id, status);

CREATE TABLE IF NOT EXISTS hdbhms.debt_snapshots
(
    debt_snapshot_id    BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    room_id             BIGINT UNSIGNED                              NOT NULL,
    contract_id         BIGINT UNSIGNED                              null,
    snapshot_date       DATE                                         NOT NULL,
    rent_debt_amount    BIGINT UNSIGNED DEFAULT '0'                  NOT NULL,
    utility_debt_amount BIGINT UNSIGNED DEFAULT '0'                  NOT NULL,
    other_debt_amount   BIGINT UNSIGNED DEFAULT '0'                  NOT NULL,
    rent_debt_months    INT             DEFAULT 0                    NOT NULL,
    utility_debt_months INT             DEFAULT 0                    NOT NULL,
    mixed_debt_amount   BIGINT UNSIGNED DEFAULT '0'                  NOT NULL,
    debt_limit_amount   BIGINT UNSIGNED                              null,
    is_over_limit       tinyint(1)      DEFAULT 0                    NOT NULL,
    created_at          DATETIME(6)     DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    CONSTRAINT uq_debt_snapshot
        UNIQUE (room_id, snapshot_date),
    CONSTRAINT fk_ds_contract
        FOREIGN KEY (contract_id) REFERENCES hdbhms.lease_contracts (lease_contract_id),
    CONSTRAINT fk_ds_room
        FOREIGN KEY (room_id) REFERENCES hdbhms.rooms (room_id)
);

CREATE INDEX idx_debt_contract
    ON hdbhms.debt_snapshots (contract_id, snapshot_date);

CREATE INDEX idx_debt_over_limit
    ON hdbhms.debt_snapshots (is_over_limit, snapshot_date);

CREATE TABLE IF NOT EXISTS hdbhms.invoices
(
    invoice_id            BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    invoice_code          VARCHAR(80)                                                                                                                        NOT NULL,
    property_id           BIGINT UNSIGNED                                                                                                                    NOT NULL,
    room_id               BIGINT UNSIGNED                                                                                                                    null,
    lease_contract_id     BIGINT UNSIGNED                                                                                                                    null,
    deposit_agreement_id  BIGINT UNSIGNED                                                                                                                    null,
    deposit_batch_id      BIGINT UNSIGNED                                                                                                                    null,
    invoice_type          ENUM ('RENT', 'UTILITY', 'DEPOSIT', 'FINAL_SETTLEMENT', 'COMPENSATION', 'OPERATING_REIMBURSEMENT', 'TRANSFER_DIFFERENCE', 'OTHER') NOT NULL,
    revision_no           INT UNSIGNED                                                            DEFAULT '1'                                                NOT NULL,
    billing_period        char(7)                                                                                                                            null,
    issue_date            DATETIME(6)                                                                                                                        NOT NULL,
    due_date              DATETIME(6)                                                                                                                        NOT NULL,
    status                ENUM ('DRAFT', 'ISSUED', 'PARTIALLY_PAID', 'PAID', 'OVERDUE', 'VOIDED') DEFAULT 'DRAFT'                                            NOT NULL,
    subtotal_amount       BIGINT UNSIGNED                                                         DEFAULT '0'                                                NOT NULL,
    discount_amount       BIGINT UNSIGNED                                                         DEFAULT '0'                                                NOT NULL,
    total_amount          BIGINT UNSIGNED                                                         DEFAULT '0'                                                NOT NULL,
    paid_amount           BIGINT UNSIGNED                                                         DEFAULT '0'                                                NOT NULL,
    remaining_amount      BIGINT UNSIGNED                                                         DEFAULT '0'                                                NOT NULL,
    collection_account_id BIGINT UNSIGNED                                                                                                                    null,
    created_by            BIGINT UNSIGNED                                                                                                                    null,
    issued_at             DATETIME(6)                                                                                                                        null,
    voided_at             DATETIME(6)                                                                                                                        null,
    void_reason           VARCHAR(1000)                                                                                                                      null,
    created_at            DATETIME(6)                                                             DEFAULT CURRENT_TIMESTAMP(6)                               NOT NULL,
    updated_at            DATETIME(6)                                                             DEFAULT CURRENT_TIMESTAMP(6)                               NOT NULL on update CURRENT_TIMESTAMP(6),
    version               INT UNSIGNED                                                            DEFAULT '0'                                                NOT NULL,
    active_invoice_key    VARCHAR(255) as (if(
            ((`status` <> _utf8mb4'VOIDED') and (`lease_contract_id` is not null) and (`billing_period` is not null)),
            concat(`lease_contract_id`, _utf8mb4':', `billing_period`, _utf8mb4':', `invoice_type`), NULL)),
    CONSTRAINT uq_invoice_code
        UNIQUE (invoice_code),
    CONSTRAINT uq_invoice_contract_period_type_rev
        UNIQUE (lease_contract_id, billing_period, invoice_type, revision_no),
    CONSTRAINT fk_inv_account
        FOREIGN KEY (collection_account_id) REFERENCES hdbhms.collection_accounts (collection_account_id),
    CONSTRAINT fk_inv_deposit_agreement
        FOREIGN KEY (deposit_agreement_id) REFERENCES hdbhms.deposit_agreements (deposit_agreement_id),
    CONSTRAINT fk_inv_lease_contract
        FOREIGN KEY (lease_contract_id) REFERENCES hdbhms.lease_contracts (lease_contract_id),
    CONSTRAINT fk_inv_property
        FOREIGN KEY (property_id) REFERENCES hdbhms.properties (property_id),
    CONSTRAINT fk_inv_room
        FOREIGN KEY (room_id) REFERENCES hdbhms.rooms (room_id),
    CONSTRAINT fk_inv_user
        FOREIGN KEY (created_by) REFERENCES hdbhms.users (user_id),
    CONSTRAINT fk_invoices_deposit_batch
        FOREIGN KEY (deposit_batch_id) REFERENCES hdbhms.deposit_batches (deposit_batch_id)
);

CREATE TABLE IF NOT EXISTS hdbhms.contract_handover_items
(
    contract_handover_item_id BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    handover_record_id        BIGINT UNSIGNED                                                              NOT NULL,
    room_asset_id             BIGINT UNSIGNED                                                              null,
    asset_name                VARCHAR(255)                                                                 NOT NULL,
    quantity                  INT                                             DEFAULT 1                    NOT NULL,
    condition_status          ENUM ('GOOD', 'ATTENTION', 'BROKEN', 'MISSING') DEFAULT 'GOOD'               NOT NULL,
    note                      TEXT                                                                         null,
    evidence_file_id          BIGINT UNSIGNED                                                              null,
    compensation_amount       BIGINT UNSIGNED                                                              null,
    compensation_invoice_id   BIGINT UNSIGNED                                                              null,
    created_at                DATETIME(6)                                     DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_chi_asset
        FOREIGN KEY (room_asset_id) REFERENCES hdbhms.room_assets (room_asset_id),
    CONSTRAINT fk_chi_file
        FOREIGN KEY (evidence_file_id) REFERENCES hdbhms.file_metadata (file_metadata_id),
    CONSTRAINT fk_chi_handover
        FOREIGN KEY (handover_record_id) REFERENCES hdbhms.contract_handover_records (contract_handover_record_id),
    CONSTRAINT fk_chi_invoice
        FOREIGN KEY (compensation_invoice_id) REFERENCES hdbhms.invoices (invoice_id)
);

CREATE INDEX idx_chi_handover
    ON hdbhms.contract_handover_items (handover_record_id);

CREATE TABLE IF NOT EXISTS hdbhms.contract_liquidations
(
    contract_liquidation_id  BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    contract_id              BIGINT UNSIGNED                                                       NOT NULL,
    liquidation_date         DATE                                                                  NOT NULL,
    reason                   VARCHAR(1000)                                                         NOT NULL,
    deposit_amount           BIGINT UNSIGNED                                DEFAULT '0'                  NOT NULL,
    deposit_deduction_amount BIGINT UNSIGNED                                DEFAULT '0'                  NOT NULL,
    deposit_deduction_reason TEXT                                                                  null,
    deposit_refund_amount    BIGINT UNSIGNED                                DEFAULT '0'                  NOT NULL,
    final_invoice_id         BIGINT UNSIGNED                                                       null,
    signed_file_id           BIGINT UNSIGNED                                                       null,
    status                   ENUM ('DRAFT', 'CONFIRMED', 'CANCELLED') DEFAULT 'DRAFT'              NOT NULL,
    created_by               BIGINT UNSIGNED                                                       null,
    created_at               DATETIME(6)                                     DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    CONSTRAINT uq_liquidation_contract
        UNIQUE (contract_id),
    CONSTRAINT fk_cl_contract
        FOREIGN KEY (contract_id) REFERENCES hdbhms.lease_contracts (lease_contract_id),
    CONSTRAINT fk_cl_file
        FOREIGN KEY (signed_file_id) REFERENCES hdbhms.file_metadata (file_metadata_id),
    CONSTRAINT fk_cl_invoice
        FOREIGN KEY (final_invoice_id) REFERENCES hdbhms.invoices (invoice_id),
    CONSTRAINT fk_cl_user
        FOREIGN KEY (created_by) REFERENCES hdbhms.users (user_id)
);

alter table hdbhms.deposit_batches
    add constraint fk_deposit_batch_invoice
        FOREIGN KEY (invoice_id) REFERENCES hdbhms.invoices (invoice_id);

CREATE TABLE IF NOT EXISTS hdbhms.invoice_lines
(
    invoice_line_id       BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    invoice_id            BIGINT UNSIGNED                                                                                                                                                                   NOT NULL,
    line_type             ENUM ('ROOM_RENT', 'ELECTRICITY', 'WATER', 'SERVICE_FEE', 'MAINTENANCE_COMPENSATION', 'VIOLATION_FINE', 'TRANSFER_DIFFERENCE', 'DEPOSIT_DEDUCTION', 'MANUAL_ADJUSTMENT', 'OTHER') NOT NULL,
    description           VARCHAR(1000)                                                                                                                                                                     NOT NULL,
    quantity              INT UNSIGNED    DEFAULT '1'                                                                                                                                                       NOT NULL,
    unit_price            BIGINT UNSIGNED DEFAULT '0'                                                                                                                                                       NOT NULL,
    amount                BIGINT UNSIGNED as ((`quantity` * `unit_price`)) stored,
    meter_reading_id      BIGINT UNSIGNED                                                                                                                                                                   null,
    source_type           VARCHAR(100)                                                                                                                                                                      null,
    source_id             BIGINT UNSIGNED                                                                                                                                                                   null,
    collection_account_id BIGINT UNSIGNED                                                                                                                                                                   null,
    created_at            DATETIME(6)     DEFAULT CURRENT_TIMESTAMP(6)                                                                                                                                      NOT NULL,
    CONSTRAINT fk_il_collection_account
        FOREIGN KEY (collection_account_id) REFERENCES hdbhms.collection_accounts (collection_account_id),
    CONSTRAINT fk_il_invoice
        FOREIGN KEY (invoice_id) REFERENCES hdbhms.invoices (invoice_id),
    CONSTRAINT fk_il_meter_reading
        FOREIGN KEY (meter_reading_id) REFERENCES hdbhms.meter_readings (meter_reading_id)
);

CREATE INDEX idx_invoice_lines_invoice
    ON hdbhms.invoice_lines (invoice_id);

CREATE INDEX idx_invoice_lines_source
    ON hdbhms.invoice_lines (source_type, source_id);

CREATE TRIGGER hdbhms.trg_invoice_lines_no_delete_after_issue
    BEFORE DELETE
    ON hdbhms.invoice_lines
    FOR EACH ROW
BEGIN
    IF EXISTS (SELECT 1 FROM invoices WHERE invoices.invoice_id = OLD.invoice_id AND status <> 'DRAFT') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invoice lines cannot be deleted after invoice is issued';
    END IF;
END;

CREATE TRIGGER hdbhms.trg_invoice_lines_no_update_after_issue
    BEFORE UPDATE
    ON hdbhms.invoice_lines
    FOR EACH ROW
BEGIN
    IF EXISTS (SELECT 1 FROM invoices WHERE invoices.invoice_id = OLD.invoice_id AND status <> 'DRAFT') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invoice lines cannot be updated after invoice is issued';
    END IF;
END;

CREATE TABLE IF NOT EXISTS hdbhms.invoice_payment_groups
(
    invoice_payment_group_id BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    invoice_id               BIGINT UNSIGNED                                                                      NOT NULL,
    collection_account_id    BIGINT UNSIGNED                                                                      NOT NULL,
    group_type               ENUM ('RENT', 'UTILITY', 'DEPOSIT', 'COMPENSATION', 'OTHER')                         NOT NULL,
    amount                   BIGINT UNSIGNED                                                                      NOT NULL,
    payment_intent_id        BIGINT UNSIGNED                                                                      null,
    status                   ENUM ('PENDING', 'PARTIALLY_PAID', 'PAID', 'CANCELLED') DEFAULT 'PENDING'            NOT NULL,
    created_at               DATETIME(6)                                             DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    CONSTRAINT uq_invoice_payment_group
        UNIQUE (invoice_id, collection_account_id, group_type),
    CONSTRAINT fk_ipg_account
        FOREIGN KEY (collection_account_id) REFERENCES hdbhms.collection_accounts (collection_account_id),
    CONSTRAINT fk_ipg_invoice
        FOREIGN KEY (invoice_id) REFERENCES hdbhms.invoices (invoice_id)
);

CREATE INDEX idx_invoice_dep_agreement
    ON hdbhms.invoices (deposit_agreement_id);

CREATE INDEX idx_invoice_lease_contract
    ON hdbhms.invoices (lease_contract_id);

CREATE INDEX idx_invoice_overdue
    ON hdbhms.invoices (status, due_date);

CREATE INDEX idx_invoice_room_status
    ON hdbhms.invoices (room_id, status, due_date);

CREATE INDEX idx_invoices_deposit_batch
    ON hdbhms.invoices (deposit_batch_id);

CREATE INDEX idx_contract_end_date
    ON hdbhms.lease_contracts (end_date, status);

CREATE INDEX idx_contract_primary_profile
    ON hdbhms.lease_contracts (primary_tenant_profile_id, status);

CREATE INDEX idx_contract_room_status
    ON hdbhms.lease_contracts (room_id, status);

CREATE INDEX idx_lc_intention_recorded
    ON hdbhms.lease_contracts (tenant_intention, intention_recorded_at);

CREATE INDEX idx_lc_room_intention_vacant
    ON hdbhms.lease_contracts (room_id, tenant_intention, expected_vacant_date);

CREATE TABLE IF NOT EXISTS hdbhms.maintenance_tickets
(
    maintenance_ticket_id    BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    ticket_code              VARCHAR(80)                                                                                                                                       null,
    property_id              BIGINT UNSIGNED                                                                                                                                   NOT NULL,
    room_id                  BIGINT UNSIGNED                                                                                                                                   null,
    contract_id              BIGINT UNSIGNED                                                                                                                                   null,
    created_by               BIGINT UNSIGNED                                                                                                                                   NOT NULL,
    ticket_scope             ENUM ('TENANT_ROOM', 'COMMON_AREA', 'PROPERTY_OPERATION')                                                                                         NOT NULL,
    priority                 ENUM ('LOW', 'MEDIUM', 'HIGH', 'URGENT')                                                                             DEFAULT 'MEDIUM'             NOT NULL,
    category                 VARCHAR(100)                                                                                                                                      NOT NULL,
    title                    VARCHAR(255)                                                                                                                                      NOT NULL,
    description              TEXT                                                                                                                                              NOT NULL,
    status                   ENUM ('PENDING_ACCEPTANCE', 'ACCEPTED', 'IN_PROGRESS', 'WAITING_CONFIRMATION', 'COMPLETED', 'REJECTED', 'CANCELLED') DEFAULT 'PENDING_ACCEPTANCE' NOT NULL,
    rejection_reason         VARCHAR(1000)                                                                                                                                     null,
    assigned_to              BIGINT UNSIGNED                                                                                                                                   null,
    worker_name              VARCHAR(255)                                                                                                                                      null,
    external_repairman_name  VARCHAR(255)                                                                                                                                      null,
    external_repairman_phone VARCHAR(30)                                                                                                                                       null,
    external_repair_provider VARCHAR(255)                                                                                                                                      null,
    external_repair_note     VARCHAR(1000)                                                                                                                                     null,
    repairman_phone          VARCHAR(30)                                                                                                                                       null,
    repair_items             TEXT                                                                                                                                              null,
    completed_at             DATETIME(6)                                                                                                                                       null,
    created_at               DATETIME(6)                                                                                                          DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    updated_at               DATETIME(6)                                                                                                          DEFAULT CURRENT_TIMESTAMP(6) NOT NULL on update CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_ticket_code
        UNIQUE (ticket_code),
    CONSTRAINT fk_mt_assigned
        FOREIGN KEY (assigned_to) REFERENCES hdbhms.users (user_id),
    CONSTRAINT fk_mt_contract
        FOREIGN KEY (contract_id) REFERENCES hdbhms.lease_contracts (lease_contract_id),
    CONSTRAINT fk_mt_created
        FOREIGN KEY (created_by) REFERENCES hdbhms.users (user_id),
    CONSTRAINT fk_mt_property
        FOREIGN KEY (property_id) REFERENCES hdbhms.properties (property_id),
    CONSTRAINT fk_mt_room
        FOREIGN KEY (room_id) REFERENCES hdbhms.rooms (room_id)
);

CREATE TABLE IF NOT EXISTS hdbhms.maintenance_costs
(
    maintenance_cost_id BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    ticket_id           BIGINT UNSIGNED                                                                NOT NULL,
    cost_type           ENUM ('LABOR', 'MATERIAL', 'TENANT_COMPENSATION', 'COMMON_OPERATING', 'OTHER') NOT NULL,
    description         VARCHAR(1000)                                                                  NOT NULL,
    amount              BIGINT UNSIGNED                                                                NOT NULL,
    paid_by             ENUM ('LANDLORD', 'TENANT', 'MANAGER', 'OTHER') DEFAULT 'LANDLORD'             NOT NULL,
    cost_responsibility VARCHAR(50)                                     DEFAULT 'UNDECIDED'            NOT NULL,
    charge_invoice_id   BIGINT UNSIGNED                                                                null,
    receipt_file_id     BIGINT UNSIGNED                                                                null,
    created_by          BIGINT UNSIGNED                                                                null,
    created_at          DATETIME(6)                                     DEFAULT CURRENT_TIMESTAMP(6)   NOT NULL,
    CONSTRAINT fk_mc_invoice
        FOREIGN KEY (charge_invoice_id) REFERENCES hdbhms.invoices (invoice_id),
    CONSTRAINT fk_mc_receipt
        FOREIGN KEY (receipt_file_id) REFERENCES hdbhms.file_metadata (file_metadata_id),
    CONSTRAINT fk_mc_ticket
        FOREIGN KEY (ticket_id) REFERENCES hdbhms.maintenance_tickets (maintenance_ticket_id),
    CONSTRAINT fk_mc_user
        FOREIGN KEY (created_by) REFERENCES hdbhms.users (user_id)
);

CREATE INDEX idx_maintenance_cost_ticket
    ON hdbhms.maintenance_costs (ticket_id);

CREATE TABLE IF NOT EXISTS hdbhms.maintenance_reviews
(
    maintenance_review_id BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    ticket_id             BIGINT UNSIGNED                          NOT NULL,
    reviewer_user_id      BIGINT UNSIGNED                          NOT NULL,
    rating                TINYINT UNSIGNED                         NOT NULL,
    comment               TEXT                                     null,
    created_at            DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    CONSTRAINT uq_ticket_review_user
        UNIQUE (ticket_id, reviewer_user_id),
    CONSTRAINT fk_mr_review_ticket
        FOREIGN KEY (ticket_id) REFERENCES hdbhms.maintenance_tickets (maintenance_ticket_id),
    CONSTRAINT fk_mr_review_user
        FOREIGN KEY (reviewer_user_id) REFERENCES hdbhms.users (user_id),
    CONSTRAINT chk_rating
        CHECK (`rating` between 1 and 5)
);

CREATE TABLE IF NOT EXISTS hdbhms.maintenance_ticket_attachments
(
    maintenance_ticket_attachment_id BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    ticket_id                        BIGINT UNSIGNED                                                                     NOT NULL,
    file_id                          BIGINT UNSIGNED                                                                     NOT NULL,
    attachment_phase                 ENUM ('BEFORE', 'DURING', 'AFTER', 'RECEIPT', 'OTHER') DEFAULT 'BEFORE'             NOT NULL,
    sort_order                       INT                                                    DEFAULT 0                    NOT NULL,
    created_by                       BIGINT UNSIGNED                                                                     null,
    created_by_user_id               BIGINT UNSIGNED                                                                     null,
    created_at                       DATETIME(6)                                            DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_mta_created_by_user
        FOREIGN KEY (created_by_user_id) REFERENCES hdbhms.users (user_id),
    CONSTRAINT fk_mta_file
        FOREIGN KEY (file_id) REFERENCES hdbhms.file_metadata (file_metadata_id),
    CONSTRAINT fk_mta_ticket
        FOREIGN KEY (ticket_id) REFERENCES hdbhms.maintenance_tickets (maintenance_ticket_id),
    CONSTRAINT fk_mta_user
        FOREIGN KEY (created_by) REFERENCES hdbhms.tenants (tenant_id)
);

CREATE INDEX idx_mta_ticket
    ON hdbhms.maintenance_ticket_attachments (ticket_id, attachment_phase);

CREATE TRIGGER hdbhms.trg_ticket_attachments_max_three_before_insert
    before insert
    ON hdbhms.maintenance_ticket_attachments
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
END;

CREATE TABLE IF NOT EXISTS hdbhms.maintenance_ticket_events
(
    maintenance_ticket_event_id BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    ticket_id                   BIGINT UNSIGNED                          NOT NULL,
    from_status                 VARCHAR(50)                              null,
    to_status                   VARCHAR(50)                              NOT NULL,
    action                      VARCHAR(50)                              null,
    note                        TEXT                                     null,
    created_by                  BIGINT UNSIGNED                          null,
    created_at                  DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_mte_ticket
        FOREIGN KEY (ticket_id) REFERENCES hdbhms.maintenance_tickets (maintenance_ticket_id),
    CONSTRAINT fk_mte_user
        FOREIGN KEY (created_by) REFERENCES hdbhms.users (user_id)
);

CREATE INDEX idx_ticket_events
    ON hdbhms.maintenance_ticket_events (ticket_id, created_at);

CREATE INDEX fk_mt_assigned
    ON hdbhms.maintenance_tickets (assigned_to);

CREATE INDEX idx_maintenance_tickets_external_repairman
    ON hdbhms.maintenance_tickets (external_repairman_phone, external_repairman_name);

CREATE INDEX idx_ticket_property_status
    ON hdbhms.maintenance_tickets (property_id, status, created_at);

CREATE INDEX idx_ticket_room
    ON hdbhms.maintenance_tickets (room_id);

CREATE INDEX idx_ticket_status
    ON hdbhms.maintenance_tickets (status, created_at);

CREATE TABLE IF NOT EXISTS hdbhms.operating_expenses
(
    operating_expense_id BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    property_id          BIGINT UNSIGNED                                                                                              NOT NULL,
    room_id              BIGINT UNSIGNED                                                                                              null,
    ticket_id            BIGINT UNSIGNED                                                                                              null,
    expense_code         VARCHAR(80)                                                                                                  NOT NULL,
    expense_type         ENUM ('REPAIR', 'COMMON_UTILITY', 'SUPPLIES', 'REPLACEMENT', 'CLEANING', 'OTHER')                            NOT NULL,
    description          TEXT                                                                                                         NOT NULL,
    amount               BIGINT UNSIGNED                                                                                              NOT NULL,
    expense_date         DATE                                                                                                         NOT NULL,
    paid_by_user_id      BIGINT UNSIGNED                                                                                              null,
    receipt_file_id      BIGINT UNSIGNED                                                                                              null,
    status               ENUM ('DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'REJECTED', 'PAID', 'CANCELLED') DEFAULT 'DRAFT'              NOT NULL,
    approved_by          BIGINT UNSIGNED                                                                                              null,
    approved_at          DATETIME(6)                                                                                                  null,
    created_by           BIGINT UNSIGNED                                                                                              null,
    created_at           DATETIME(6)                                                                     DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    CONSTRAINT uq_operating_expense_code
        UNIQUE (expense_code),
    CONSTRAINT fk_oe_approved
        FOREIGN KEY (approved_by) REFERENCES hdbhms.users (user_id),
    CONSTRAINT fk_oe_created
        FOREIGN KEY (created_by) REFERENCES hdbhms.users (user_id),
    CONSTRAINT fk_oe_paid_by
        FOREIGN KEY (paid_by_user_id) REFERENCES hdbhms.users (user_id),
    CONSTRAINT fk_oe_property
        FOREIGN KEY (property_id) REFERENCES hdbhms.properties (property_id),
    CONSTRAINT fk_oe_receipt
        FOREIGN KEY (receipt_file_id) REFERENCES hdbhms.file_metadata (file_metadata_id),
    CONSTRAINT fk_oe_room
        FOREIGN KEY (room_id) REFERENCES hdbhms.rooms (room_id),
    CONSTRAINT fk_oe_ticket
        FOREIGN KEY (ticket_id) REFERENCES hdbhms.maintenance_tickets (maintenance_ticket_id)
);

CREATE INDEX idx_operating_expense_property
    ON hdbhms.operating_expenses (property_id, expense_date, status);

CREATE TABLE IF NOT EXISTS hdbhms.payment_allocations
(
    payment_allocation_id  BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    payment_transaction_id BIGINT UNSIGNED                          NOT NULL,
    invoice_id             BIGINT UNSIGNED                          NOT NULL,
    amount                 BIGINT UNSIGNED                          NOT NULL,
    allocated_by           BIGINT UNSIGNED                          null,
    allocated_at           DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    CONSTRAINT uq_payment_invoice_alloc
        UNIQUE (payment_transaction_id, invoice_id),
    CONSTRAINT fk_pa_invoice
        FOREIGN KEY (invoice_id) REFERENCES hdbhms.invoices (invoice_id),
    CONSTRAINT fk_pa_payment
        FOREIGN KEY (payment_transaction_id) REFERENCES hdbhms.payment_transactions (payment_transaction_id),
    CONSTRAINT fk_pa_user
        FOREIGN KEY (allocated_by) REFERENCES hdbhms.users (user_id)
);

CREATE INDEX idx_alloc_invoice
    ON hdbhms.payment_allocations (invoice_id);

CREATE TABLE IF NOT EXISTS hdbhms.payment_intents
(
    payment_intent_id        BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    invoice_id               BIGINT UNSIGNED                                                                                                            null,
    deposit_agreement_id     BIGINT UNSIGNED                                                                                                            null,
    deposit_batch_id         BIGINT UNSIGNED                                                                                                            null,
    invoice_payment_group_id BIGINT UNSIGNED                                                                                                            null,
    amount                   BIGINT UNSIGNED                                                                                                            NOT NULL,
    provider                 ENUM ('VIETQR', 'MOMO', 'ZALOPAY', 'CASH', 'BANK_TRANSFER', 'PAYOS')                                                       NOT NULL,
    collection_account_id    BIGINT UNSIGNED                                                                                                            null,
    payment_content          VARCHAR(255)                                                                                                               NOT NULL,
    provider_order_code      VARCHAR(255)                                                                                                               null,
    qr_payload               TEXT                                                                                                                       null,
    status                   ENUM ('CREATED', 'PENDING', 'SUCCEEDED', 'FAILED', 'EXPIRED', 'CANCELLED', 'REFUND_REQUIRED') DEFAULT 'CREATED'            NOT NULL,
    expires_at               DATETIME(6)                                                                                                                null,
    created_at               DATETIME(6)                                                                                   DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    CONSTRAINT uq_payment_intents_provider_order_code
        UNIQUE (provider_order_code),
    CONSTRAINT fk_payment_intents_deposit_batch
        FOREIGN KEY (deposit_batch_id) REFERENCES hdbhms.deposit_batches (deposit_batch_id),
    CONSTRAINT fk_pi_account
        FOREIGN KEY (collection_account_id) REFERENCES hdbhms.collection_accounts (collection_account_id),
    CONSTRAINT fk_pi_deposit
        FOREIGN KEY (deposit_agreement_id) REFERENCES hdbhms.deposit_agreements (deposit_agreement_id),
    CONSTRAINT fk_pi_invoice
        FOREIGN KEY (invoice_id) REFERENCES hdbhms.invoices (invoice_id),
    CONSTRAINT fk_pi_ipg
        FOREIGN KEY (invoice_payment_group_id) REFERENCES hdbhms.invoice_payment_groups (invoice_payment_group_id)
);

alter table hdbhms.deposit_batches
    add constraint fk_deposit_batch_payment_intent
        FOREIGN KEY (payment_intent_id) REFERENCES hdbhms.payment_intents (payment_intent_id);

alter table hdbhms.invoice_payment_groups
    add constraint fk_ipg_intent
        FOREIGN KEY (payment_intent_id) REFERENCES hdbhms.payment_intents (payment_intent_id);

CREATE INDEX idx_payment_intents_deposit_batch
    ON hdbhms.payment_intents (deposit_batch_id);

CREATE INDEX idx_pi_invoice
    ON hdbhms.payment_intents (invoice_id);

CREATE TABLE IF NOT EXISTS hdbhms.pending_billing_charges
(
    pending_billing_charge_id BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    property_id               BIGINT UNSIGNED                                                                                                                                                                   NOT NULL,
    room_id                   BIGINT UNSIGNED                                                                                                                                                                   null,
    contract_id               BIGINT UNSIGNED                                                                                                                                                                   null,
    source_type               VARCHAR(100)                                                                                                                                                                      NOT NULL,
    source_id                 BIGINT UNSIGNED                                                                                                                                                                   NOT NULL,
    line_type                 ENUM ('ROOM_RENT', 'ELECTRICITY', 'WATER', 'SERVICE_FEE', 'MAINTENANCE_COMPENSATION', 'VIOLATION_FINE', 'TRANSFER_DIFFERENCE', 'DEPOSIT_DEDUCTION', 'MANUAL_ADJUSTMENT', 'OTHER') NOT NULL,
    description               VARCHAR(1000)                                                                                                                                                                     NOT NULL,
    amount                    BIGINT UNSIGNED                                                                                                                                                                   NOT NULL,
    billing_period            char(7)                                                                                                                                                                           NOT NULL,
    scheduled_issue_at        DATETIME(6)                                                                                                                                                                       NOT NULL,
    due_date                  DATETIME(6)                                                                                                                                                                       NOT NULL,
    status                    ENUM ('SCHEDULED', 'BILLED', 'CANCELLED', 'FAILED') DEFAULT 'SCHEDULED'                                                                                                           NOT NULL,
    invoice_id                BIGINT UNSIGNED                                                                                                                                                                   null,
    failure_reason            VARCHAR(1000)                                                                                                                                                                     null,
    created_by                BIGINT UNSIGNED                                                                                                                                                                   null,
    created_at                DATETIME(6)                                         DEFAULT CURRENT_TIMESTAMP(6)                                                                                                  NOT NULL,
    updated_at                DATETIME(6)                                         DEFAULT CURRENT_TIMESTAMP(6)                                                                                                  NOT NULL on update CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_pbc_contract
        FOREIGN KEY (contract_id) REFERENCES hdbhms.lease_contracts (lease_contract_id),
    CONSTRAINT fk_pbc_invoice
        FOREIGN KEY (invoice_id) REFERENCES hdbhms.invoices (invoice_id),
    CONSTRAINT fk_pbc_property
        FOREIGN KEY (property_id) REFERENCES hdbhms.properties (property_id),
    CONSTRAINT fk_pbc_room
        FOREIGN KEY (room_id) REFERENCES hdbhms.rooms (room_id),
    CONSTRAINT fk_pbc_user
        FOREIGN KEY (created_by) REFERENCES hdbhms.users (user_id)
);

CREATE INDEX idx_pending_billing_contract_period
    ON hdbhms.pending_billing_charges (contract_id, billing_period, status);

CREATE INDEX idx_pending_billing_due
    ON hdbhms.pending_billing_charges (status, scheduled_issue_at);

CREATE INDEX idx_pending_billing_source
    ON hdbhms.pending_billing_charges (source_type, source_id, status);

CREATE TABLE IF NOT EXISTS hdbhms.rent_overrides
(
    rent_override_id      BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    contract_id           BIGINT UNSIGNED                          NOT NULL,
    billing_period        char(7)                                  NOT NULL,
    override_monthly_rent BIGINT UNSIGNED                          NOT NULL,
    reason                VARCHAR(1000)                            NOT NULL,
    approved_by           BIGINT UNSIGNED                          NOT NULL,
    created_at            DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    CONSTRAINT uq_rent_override
        UNIQUE (contract_id, billing_period),
    CONSTRAINT fk_ro_approver
        FOREIGN KEY (approved_by) REFERENCES hdbhms.users (user_id),
    CONSTRAINT fk_ro_contract
        FOREIGN KEY (contract_id) REFERENCES hdbhms.lease_contracts (lease_contract_id)
);

CREATE INDEX idx_room_hold_room
    ON hdbhms.room_holds (room_id, status, expires_at);

CREATE TABLE IF NOT EXISTS hdbhms.room_transfer_requests
(
    room_transfer_request_id        BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    request_code                    VARCHAR(80)                                                                                                                                                                                                                               NOT NULL,
    requester_id                    BIGINT UNSIGNED                                                                                                                                                                                                                           NOT NULL,
    old_contract_id                 BIGINT UNSIGNED                                                                                                                                                                                                                           NOT NULL,
    old_room_id                     BIGINT UNSIGNED                                                                                                                                                                                                                           NOT NULL,
    target_room_id                  BIGINT UNSIGNED                                                                                                                                                                                                                           NOT NULL,
    transferring_tenant_profile_ids JSON                                                                                                                                                                                                                                      null,
    nominated_holder_profile_id     BIGINT UNSIGNED                                                                                                                                                                                                                           null,
    target_transfer_type            VARCHAR(50)                                                                                                                                                                                                                               null,
    target_contract_id              BIGINT UNSIGNED                                                                                                                                                                                                                           null,
    requested_transfer_date         DATE                                                                                                                                                                                                                                      NOT NULL,
    reason                          TEXT                                                                                                                                                                                                                                      null,
    reserved_slots                  INT                                                                                                                                                                                                                                       null,
    reservation_expires_at          DATETIME(6)                                                                                                                                                                                                                               null,
    target_holder_approved_by       BIGINT UNSIGNED                                                                                                                                                                                                                           null,
    target_holder_approved_at       DATETIME(6)                                                                                                                                                                                                                               null,
    target_holder_rejected_at       DATETIME(6)                                                                                                                                                                                                                               null,
    status                          ENUM ('WAITING_APPROVAL', 'CANCELLED', 'REJECTED', 'WAITING_NEW_CONTRACT', 'WAITING_TARGET_HOLDER_APPROVAL', 'WAITING_CONTRACT_CONFIRMATION', 'WAITING_SIGNING', 'WAITING_EXECUTION', 'EXECUTED', 'EXPIRED') DEFAULT 'WAITING_APPROVAL'   NOT NULL,
    debt_snapshot_id                BIGINT UNSIGNED                                                                                                                                                                                                                           null,
    new_contract_id                 BIGINT UNSIGNED                                                                                                                                                                                                                           null,
    created_at                      DATETIME(6)                                                                                                                                                                                                  DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    updated_at                      DATETIME(6)                                                                                                                                                                                                  DEFAULT CURRENT_TIMESTAMP(6) NOT NULL on update CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_transfer_code
        UNIQUE (request_code),
    CONSTRAINT fk_tr_debt
        FOREIGN KEY (debt_snapshot_id) REFERENCES hdbhms.debt_snapshots (debt_snapshot_id),
    CONSTRAINT fk_tr_new_contract
        FOREIGN KEY (new_contract_id) REFERENCES hdbhms.lease_contracts (lease_contract_id),
    CONSTRAINT fk_tr_nominated_holder
        FOREIGN KEY (nominated_holder_profile_id) REFERENCES hdbhms.person_profiles (person_profile_id),
    CONSTRAINT fk_tr_old_contract
        FOREIGN KEY (old_contract_id) REFERENCES hdbhms.lease_contracts (lease_contract_id),
    CONSTRAINT fk_tr_old_room
        FOREIGN KEY (old_room_id) REFERENCES hdbhms.rooms (room_id),
    CONSTRAINT fk_tr_requester
        FOREIGN KEY (requester_id) REFERENCES hdbhms.tenants (tenant_id),
    CONSTRAINT fk_tr_target_contract
        FOREIGN KEY (target_contract_id) REFERENCES hdbhms.lease_contracts (lease_contract_id),
    CONSTRAINT fk_tr_target_holder_approved_by
        FOREIGN KEY (target_holder_approved_by) REFERENCES hdbhms.users (user_id),
    CONSTRAINT fk_tr_target_room
        FOREIGN KEY (target_room_id) REFERENCES hdbhms.rooms (room_id)
);

CREATE INDEX idx_transfer_rooms
    ON hdbhms.room_transfer_requests (old_room_id, target_room_id);

CREATE INDEX idx_transfer_status
    ON hdbhms.room_transfer_requests (status, created_at);

CREATE INDEX idx_transfer_target_reservation
    ON hdbhms.room_transfer_requests (target_room_id, status, reservation_expires_at);

CREATE TABLE IF NOT EXISTS hdbhms.rule_violations
(
    rule_violation_id BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    property_id       BIGINT UNSIGNED                                                                   NOT NULL,
    room_id           BIGINT UNSIGNED                                                                   null,
    contract_id       BIGINT UNSIGNED                                                                   null,
    tenant_profile_id BIGINT UNSIGNED                                                                   null,
    rule_id           BIGINT UNSIGNED                                                                   NOT NULL,
    violation_date    DATE                                                                              NOT NULL,
    description       TEXT                                                                              NOT NULL,
    fine_amount       BIGINT UNSIGNED                                      DEFAULT '0'                  NOT NULL,
    invoice_id        BIGINT UNSIGNED                                                                   null,
    evidence_file_id  BIGINT UNSIGNED                                                                   null,
    status            ENUM ('RECORDED', 'INVOICED', 'WAIVED', 'CANCELLED') DEFAULT 'RECORDED'           NOT NULL,
    created_by        BIGINT UNSIGNED                                                                   null,
    created_at        DATETIME(6)                                          DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_rv_contract
        FOREIGN KEY (contract_id) REFERENCES hdbhms.lease_contracts (lease_contract_id),
    CONSTRAINT fk_rv_file
        FOREIGN KEY (evidence_file_id) REFERENCES hdbhms.file_metadata (file_metadata_id),
    CONSTRAINT fk_rv_invoice
        FOREIGN KEY (invoice_id) REFERENCES hdbhms.invoices (invoice_id),
    CONSTRAINT fk_rv_property
        FOREIGN KEY (property_id) REFERENCES hdbhms.properties (property_id),
    CONSTRAINT fk_rv_room
        FOREIGN KEY (room_id) REFERENCES hdbhms.rooms (room_id),
    CONSTRAINT fk_rv_rule
        FOREIGN KEY (rule_id) REFERENCES hdbhms.property_rules (property_rule_id),
    CONSTRAINT fk_rv_user
        FOREIGN KEY (created_by) REFERENCES hdbhms.users (user_id)
);

CREATE INDEX idx_rule_violations_contract
    ON hdbhms.rule_violations (contract_id, status);

CREATE INDEX idx_rule_violations_room
    ON hdbhms.rule_violations (room_id, violation_date);

CREATE TABLE IF NOT EXISTS hdbhms.tenant_account_provisionings
(
    tenant_account_provisioning_id BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    tenant_profile_id              BIGINT UNSIGNED                           NOT NULL,
    user_id                        BIGINT UNSIGNED                           null,
    first_contract_id              BIGINT UNSIGNED                           null,
    latest_contract_id             BIGINT UNSIGNED                           null,
    status                         VARCHAR(50)  DEFAULT 'NOT_PROVISIONED'    NOT NULL,
    recipient_email                VARCHAR(255)                              null,
    sent_at                        DATETIME(6)                               null,
    failed_at                      DATETIME(6)                               null,
    failure_reason                 TEXT                                      null,
    attempt_count                  INT UNSIGNED DEFAULT '0'                  NOT NULL,
    last_attempt_at                DATETIME(6)                               null,
    created_at                     DATETIME(6)  DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    updated_at                     DATETIME(6)  DEFAULT CURRENT_TIMESTAMP(6) NOT NULL on update CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_tenant_account_provisionings_tenant_profile
        UNIQUE (tenant_profile_id),
    CONSTRAINT fk_tap_first_contract
        FOREIGN KEY (first_contract_id) REFERENCES hdbhms.lease_contracts (lease_contract_id),
    CONSTRAINT fk_tap_latest_contract
        FOREIGN KEY (latest_contract_id) REFERENCES hdbhms.lease_contracts (lease_contract_id),
    CONSTRAINT fk_tap_tenant_profile
        FOREIGN KEY (tenant_profile_id) REFERENCES hdbhms.person_profiles (person_profile_id),
    CONSTRAINT fk_tap_user
        FOREIGN KEY (user_id) REFERENCES hdbhms.users (user_id),
    CONSTRAINT chk_tap_status
        CHECK (`status` in ('NOT_PROVISIONED','PENDING','SENT','FAILED','ACTIVE'))
);

CREATE INDEX idx_tap_latest_contract
    ON hdbhms.tenant_account_provisionings (latest_contract_id);

CREATE INDEX idx_tap_status
    ON hdbhms.tenant_account_provisionings (status);

CREATE INDEX idx_tap_user
    ON hdbhms.tenant_account_provisionings (user_id);

CREATE INDEX idx_tenant_property
    ON hdbhms.tenants (property_id);

CREATE TABLE IF NOT EXISTS hdbhms.transfer_settlements
(
    transfer_settlement_id         BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    transfer_request_id            BIGINT UNSIGNED                                                                 NOT NULL,
    old_room_remaining_value       BIGINT UNSIGNED DEFAULT '0'                                                     NOT NULL,
    new_room_required_value        BIGINT UNSIGNED DEFAULT '0'                                                     NOT NULL,
    difference_amount              BIGINT UNSIGNED DEFAULT '0'                                                     NOT NULL,
    settlement_type                ENUM ('TENANT_PAY_MORE', 'REFUND_NOW', 'CREDIT_NEXT_CONTRACT', 'NO_DIFFERENCE') NOT NULL,
    old_room_final_invoice_id      BIGINT UNSIGNED                                                                 null,
    transfer_difference_invoice_id BIGINT UNSIGNED                                                                 null,
    confirmed_by                   BIGINT UNSIGNED                                                                 null,
    confirmed_at                   DATETIME(6)                                                                     null,
    created_at                     DATETIME(6)     DEFAULT CURRENT_TIMESTAMP(6)                                    NOT NULL,
    CONSTRAINT uq_transfer_settlement
        UNIQUE (transfer_request_id),
    CONSTRAINT fk_ts_diff_invoice
        FOREIGN KEY (transfer_difference_invoice_id) REFERENCES hdbhms.invoices (invoice_id),
    CONSTRAINT fk_ts_old_invoice
        FOREIGN KEY (old_room_final_invoice_id) REFERENCES hdbhms.invoices (invoice_id),
    CONSTRAINT fk_ts_transfer
        FOREIGN KEY (transfer_request_id) REFERENCES hdbhms.room_transfer_requests (room_transfer_request_id),
    CONSTRAINT fk_ts_user
        FOREIGN KEY (confirmed_by) REFERENCES hdbhms.users (user_id)
);

CREATE TABLE IF NOT EXISTS hdbhms.user_mobile_device_tokens
(
    user_mobile_device_token_id BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    user_id                     BIGINT UNSIGNED                                      NOT NULL,
    token                       VARCHAR(500)                                         NOT NULL,
    platform                    ENUM ('ANDROID', 'IOS') DEFAULT 'ANDROID'            NOT NULL,
    is_active                   tinyint(1)              DEFAULT 1                    NOT NULL,
    created_at                  DATETIME(6)             DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    CONSTRAINT uq_device_token
        UNIQUE (token),
    CONSTRAINT fk_token_user
        FOREIGN KEY (user_id) REFERENCES hdbhms.users (user_id)
);

CREATE TABLE IF NOT EXISTS hdbhms.user_modification_histories
(
    user_modification_history_id BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    user_id                      BIGINT UNSIGNED                          NOT NULL,
    type                         VARCHAR(50)                              NOT NULL,
    old_value                    VARCHAR(255)                             null,
    new_value                    VARCHAR(255)                             null,
    changed_at                   DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_modhist_user
        FOREIGN KEY (user_id) REFERENCES hdbhms.users (user_id)
);

CREATE INDEX idx_modhist_user
    ON hdbhms.user_modification_histories (user_modification_history_id);

CREATE TABLE IF NOT EXISTS hdbhms.user_status_logs
(
    user_status_log_id BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    user_entity_id     BIGINT UNSIGNED                          NOT NULL,
    old_status         VARCHAR(50)                              NOT NULL,
    new_status         VARCHAR(50)                              NOT NULL,
    reason             VARCHAR(255)                             null,
    changed_by         VARCHAR(255)                             null,
    changed_at         DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_status_logs_user
        FOREIGN KEY (user_entity_id) REFERENCES hdbhms.users (user_id)
);

CREATE INDEX idx_status_logs_user
    ON hdbhms.user_status_logs (user_entity_id);

CREATE INDEX idx_users_status
    ON hdbhms.users (status, created_at);

CREATE TABLE IF NOT EXISTS hdbhms.utility_complaints
(
    utility_complaint_id BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    property_id          BIGINT UNSIGNED                                                                                                          NOT NULL,
    room_id              BIGINT UNSIGNED                                                                                                          null,
    tenant_id            BIGINT UNSIGNED                                                                                                          null,
    reported_by_user_id  BIGINT UNSIGNED                                                                                                          NOT NULL,
    assigned_to_user_id  BIGINT UNSIGNED                                                                                                          null,
    complaint_code       VARCHAR(50)                                                                                                              NOT NULL,
    complaint_type       ENUM ('ELECTRICITY', 'WATER', 'METER', 'BILLING', 'OTHER')                                  DEFAULT 'OTHER'              NOT NULL,
    status               ENUM ('OPEN', 'IN_PROGRESS', 'WAITING_TENANT', 'WAITING_PROVIDER', 'RESOLVED', 'CANCELLED') DEFAULT 'OPEN'               NOT NULL,
    title                VARCHAR(255)                                                                                                             NOT NULL,
    description          TEXT                                                                                                                     null,
    resolution_note      TEXT                                                                                                                     null,
    reported_at          DATETIME(6)                                                                                 DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    acknowledged_at      DATETIME(6)                                                                                                              null,
    resolved_at          DATETIME(6)                                                                                                              null,
    cancelled_at         DATETIME(6)                                                                                                              null,
    created_at           DATETIME(6)                                                                                 DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    updated_at           DATETIME(6)                                                                                 DEFAULT CURRENT_TIMESTAMP(6) NOT NULL on update CURRENT_TIMESTAMP(6),
    deleted_at           DATETIME(6)                                                                                                              null,
    version              BIGINT UNSIGNED                                                                             DEFAULT '0'                  NOT NULL,
    CONSTRAINT uq_utility_complaint_code
        UNIQUE (complaint_code),
    CONSTRAINT fk_utility_complaints_assignee
        FOREIGN KEY (assigned_to_user_id) REFERENCES hdbhms.users (user_id),
    CONSTRAINT fk_utility_complaints_property
        FOREIGN KEY (property_id) REFERENCES hdbhms.properties (property_id),
    CONSTRAINT fk_utility_complaints_reporter
        FOREIGN KEY (reported_by_user_id) REFERENCES hdbhms.users (user_id),
    CONSTRAINT fk_utility_complaints_room
        FOREIGN KEY (room_id) REFERENCES hdbhms.rooms (room_id),
    CONSTRAINT fk_utility_complaints_tenant
        FOREIGN KEY (tenant_id) REFERENCES hdbhms.tenants (tenant_id)
);

CREATE TABLE IF NOT EXISTS hdbhms.utility_complaint_attachments
(
    utility_complaint_attachment_id BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    utility_complaint_id            BIGINT UNSIGNED                                                                      NOT NULL,
    file_metadata_id                BIGINT UNSIGNED                                                                      NOT NULL,
    attachment_type                 ENUM ('IMAGE', 'VIDEO', 'DOCUMENT', 'INVOICE', 'OTHER') DEFAULT 'IMAGE'              NOT NULL,
    uploaded_by_user_id             BIGINT UNSIGNED                                                                      null,
    created_at                      DATETIME(6)                                             DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_utility_complaint_attachments_complaint
        FOREIGN KEY (utility_complaint_id) REFERENCES hdbhms.utility_complaints (utility_complaint_id),
    CONSTRAINT fk_utility_complaint_attachments_file
        FOREIGN KEY (file_metadata_id) REFERENCES hdbhms.file_metadata (file_metadata_id),
    CONSTRAINT fk_utility_complaint_attachments_user
        FOREIGN KEY (uploaded_by_user_id) REFERENCES hdbhms.users (user_id)
);

CREATE INDEX idx_utility_complaint_attachments_complaint
    ON hdbhms.utility_complaint_attachments (utility_complaint_id, created_at);

CREATE INDEX idx_utility_complaint_attachments_file
    ON hdbhms.utility_complaint_attachments (file_metadata_id);

CREATE TABLE IF NOT EXISTS hdbhms.utility_complaint_status_history
(
    utility_complaint_status_history_id BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    utility_complaint_id                BIGINT UNSIGNED                                                                             NOT NULL,
    from_status                         ENUM ('OPEN', 'IN_PROGRESS', 'WAITING_TENANT', 'WAITING_PROVIDER', 'RESOLVED', 'CANCELLED') null,
    to_status                           ENUM ('OPEN', 'IN_PROGRESS', 'WAITING_TENANT', 'WAITING_PROVIDER', 'RESOLVED', 'CANCELLED') NOT NULL,
    changed_by_user_id                  BIGINT UNSIGNED                                                                             null,
    note                                VARCHAR(1000)                                                                               null,
    created_at                          DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)                                                    NOT NULL,
    CONSTRAINT fk_utility_complaint_status_history_complaint
        FOREIGN KEY (utility_complaint_id) REFERENCES hdbhms.utility_complaints (utility_complaint_id),
    CONSTRAINT fk_utility_complaint_status_history_user
        FOREIGN KEY (changed_by_user_id) REFERENCES hdbhms.users (user_id)
);

CREATE INDEX idx_utility_complaint_status_changed_by
    ON hdbhms.utility_complaint_status_history (changed_by_user_id, created_at);

CREATE INDEX idx_utility_complaint_status_history
    ON hdbhms.utility_complaint_status_history (utility_complaint_id, created_at);

CREATE INDEX idx_utility_complaints_assignee
    ON hdbhms.utility_complaints (assigned_to_user_id, status, created_at);

CREATE INDEX idx_utility_complaints_property_status
    ON hdbhms.utility_complaints (property_id, status, created_at);

CREATE INDEX idx_utility_complaints_reporter
    ON hdbhms.utility_complaints (reported_by_user_id, created_at);

CREATE INDEX idx_utility_complaints_room
    ON hdbhms.utility_complaints (room_id, status);

CREATE INDEX idx_utility_complaints_tenant
    ON hdbhms.utility_complaints (tenant_id, status);

CREATE INDEX idx_utility_complaints_type_priority
    ON hdbhms.utility_complaints (complaint_type, status);

CREATE TABLE IF NOT EXISTS hdbhms.utility_tariffs
(
    utility_tariff_id                       BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    property_id                             BIGINT UNSIGNED                              null,
    utility_type                            ENUM ('ELECTRICITY', 'WATER', 'SERVICE_FEE') NOT NULL,
    unit_price                              BIGINT UNSIGNED                              NOT NULL,
    free_allowance                          BIGINT UNSIGNED DEFAULT '0'                  NOT NULL,
    service_fee_waive_electricity_threshold BIGINT UNSIGNED                              null,
    effective_from                          DATE                                         NOT NULL,
    effective_to                            DATE                                         null,
    created_by                              BIGINT UNSIGNED                              null,
    created_at                              DATETIME(6)     DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_tariff_property
        FOREIGN KEY (property_id) REFERENCES hdbhms.properties (property_id),
    CONSTRAINT fk_tariff_user
        FOREIGN KEY (created_by) REFERENCES hdbhms.users (user_id)
);

CREATE INDEX idx_tariff_effective
    ON hdbhms.utility_tariffs (property_id, utility_type, effective_from);

CREATE TABLE IF NOT EXISTS hdbhms.vehicles
(
    vehicle_id          BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    profile_id          BIGINT UNSIGNED                                                                      NOT NULL,
    vehicle_type        ENUM ('MOTORBIKE', 'BICYCLE', 'CAR', 'E_BIKE', 'OTHER') DEFAULT 'MOTORBIKE'          NOT NULL,
    license_plate       VARCHAR(50)                                                                          NOT NULL,
    image_file_id       BIGINT UNSIGNED                                                                      null,
    status              ENUM ('ACTIVE', 'INACTIVE')                             DEFAULT 'ACTIVE'             NOT NULL,
    created_at          DATETIME(6)                                             DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    deleted_at          DATETIME(6)                                                                          null,
    active_unique_token tinyint as (if((`deleted_at` is null), 1, NULL)),
    CONSTRAINT uq_vehicle_plate_active
        UNIQUE (license_plate, active_unique_token),
    CONSTRAINT fk_vehicle_file
        FOREIGN KEY (image_file_id) REFERENCES hdbhms.file_metadata (file_metadata_id),
    CONSTRAINT fk_vehicle_profile
        FOREIGN KEY (profile_id) REFERENCES hdbhms.person_profiles (person_profile_id)
);

CREATE INDEX idx_vehicle_profile
    ON hdbhms.vehicles (status);

CREATE TABLE IF NOT EXISTS hdbhms.visit_requests
(
    visit_request_id BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    property_id      BIGINT UNSIGNED                                                         null,
    room_id          BIGINT UNSIGNED                                                         null,
    visitor_name     VARCHAR(255)                                                            NOT NULL,
    visitor_phone    VARCHAR(30)                                                             NOT NULL,
    visitor_email    VARCHAR(255)                                                            null,
    preferred_start  DATETIME(6)                                                             NOT NULL,
    notes            TEXT                                                                    null,
    created_at       DATETIME(6)                                DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    deleted_at       DATETIME(6)                                                             null,
    deleted_by       bigint                                                                  null,
    status           ENUM ('NOT_VIEWED', 'VIEWED', 'DISMISSED') DEFAULT 'NOT_VIEWED'         NOT NULL,
    updated_at       DATETIME(6)                                                             null,
    CONSTRAINT fk_visit_property
        FOREIGN KEY (property_id) REFERENCES hdbhms.properties (property_id),
    CONSTRAINT fk_visit_room
        FOREIGN KEY (room_id) REFERENCES hdbhms.rooms (room_id)
);

create fulltext index ft_visit_search
    on hdbhms.visit_requests (visitor_name, visitor_email, visitor_phone);

CREATE INDEX idx_visit_property
    ON hdbhms.visit_requests (property_id);

CREATE INDEX idx_visit_status
    ON hdbhms.visit_requests (preferred_start);