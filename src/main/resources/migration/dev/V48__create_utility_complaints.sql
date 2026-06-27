-- Utility complaints domain
-- Covers complaint master record, attached files, and status history.

CREATE TABLE utility_complaints
(
    utility_complaint_id  BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    property_id           BIGINT UNSIGNED                                                                           NOT NULL,
    room_id               BIGINT UNSIGNED                                                                           NULL,
    tenant_id             BIGINT UNSIGNED                                                                           NULL,
    reported_by_user_id   BIGINT UNSIGNED                                                                           NOT NULL,
    assigned_to_user_id   BIGINT UNSIGNED                                                                           NULL,
    complaint_code        VARCHAR(50)                                                                               NOT NULL,
    complaint_type        ENUM ('ELECTRICITY','WATER', 'METER','BILLING','OTHER')               NOT NULL DEFAULT 'OTHER',
    status                ENUM ('OPEN','IN_PROGRESS','WAITING_TENANT','WAITING_PROVIDER','RESOLVED','CANCELLED')   NOT NULL DEFAULT 'OPEN',
    title                 VARCHAR(255)                                                                              NOT NULL,
    description           TEXT                                                                                      NULL,
    resolution_note       TEXT                                                                                      NULL,
    reported_at           DATETIME(6)                                                                               NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    acknowledged_at       DATETIME(6)                                                                               NULL,
    resolved_at           DATETIME(6)                                                                               NULL,
    cancelled_at          DATETIME(6)                                                                               NULL,
    created_at            DATETIME(6)                                                                               NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at            DATETIME(6)                                                                               NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at            DATETIME(6)                                                                               NULL,
    version               BIGINT UNSIGNED                                                                           NOT NULL DEFAULT 0,
    UNIQUE KEY uq_utility_complaint_code (complaint_code),
    KEY idx_utility_complaints_property_status (property_id, status, created_at),
    KEY idx_utility_complaints_room (room_id, status),
    KEY idx_utility_complaints_tenant (tenant_id, status),
    KEY idx_utility_complaints_reporter (reported_by_user_id, created_at),
    KEY idx_utility_complaints_assignee (assigned_to_user_id, status, created_at),
    KEY idx_utility_complaints_type_priority (complaint_type, status),
    CONSTRAINT fk_utility_complaints_property FOREIGN KEY (property_id) REFERENCES properties (property_id),
    CONSTRAINT fk_utility_complaints_room FOREIGN KEY (room_id) REFERENCES rooms (room_id),
    CONSTRAINT fk_utility_complaints_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (tenant_id),
    CONSTRAINT fk_utility_complaints_reporter FOREIGN KEY (reported_by_user_id) REFERENCES users (user_id),
    CONSTRAINT fk_utility_complaints_assignee FOREIGN KEY (assigned_to_user_id) REFERENCES users (user_id)
) ENGINE = InnoDB;

CREATE TABLE utility_complaint_attachments
(
    utility_complaint_attachment_id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    utility_complaint_id            BIGINT UNSIGNED NOT NULL,
    file_metadata_id                BIGINT UNSIGNED NOT NULL,
    attachment_type                 ENUM ('IMAGE','VIDEO','DOCUMENT','INVOICE','OTHER') NOT NULL DEFAULT 'IMAGE',
    uploaded_by_user_id             BIGINT UNSIGNED NULL,
    created_at                      DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    KEY idx_utility_complaint_attachments_complaint (utility_complaint_id, created_at),
    KEY idx_utility_complaint_attachments_file (file_metadata_id),
    CONSTRAINT fk_utility_complaint_attachments_complaint FOREIGN KEY (utility_complaint_id)
        REFERENCES utility_complaints (utility_complaint_id),
    CONSTRAINT fk_utility_complaint_attachments_file FOREIGN KEY (file_metadata_id)
        REFERENCES file_metadata (file_metadata_id),
    CONSTRAINT fk_utility_complaint_attachments_user FOREIGN KEY (uploaded_by_user_id)
        REFERENCES users (user_id)
) ENGINE = InnoDB;

CREATE TABLE utility_complaint_status_history
(
    utility_complaint_status_history_id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    utility_complaint_id                BIGINT UNSIGNED                                                                         NOT NULL,
    from_status                         ENUM ('OPEN','IN_PROGRESS','WAITING_TENANT','WAITING_PROVIDER','RESOLVED','CANCELLED') NULL,
    to_status                           ENUM ('OPEN','IN_PROGRESS','WAITING_TENANT','WAITING_PROVIDER','RESOLVED','CANCELLED') NOT NULL,
    changed_by_user_id                  BIGINT UNSIGNED                                                                         NULL,
    note                                VARCHAR(1000)                                                                           NULL,
    created_at                          DATETIME(6)                                                                             NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    KEY idx_utility_complaint_status_history (utility_complaint_id, created_at),
    KEY idx_utility_complaint_status_changed_by (changed_by_user_id, created_at),
    CONSTRAINT fk_utility_complaint_status_history_complaint FOREIGN KEY (utility_complaint_id)
        REFERENCES utility_complaints (utility_complaint_id),
    CONSTRAINT fk_utility_complaint_status_history_user FOREIGN KEY (changed_by_user_id)
        REFERENCES users (user_id)
) ENGINE = InnoDB;