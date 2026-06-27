-- Create meter_reading_reviews domain.
-- This replaces the utility_complaints domain for meter reading disputes.

-- 1. Add review status and count to meter_readings table.
ALTER TABLE meter_readings
    ADD COLUMN review_status ENUM(
        'NONE',
        'PENDING',
        'UNDER_REVIEW',
        'APPROVED',
        'REJECTED',
        'CANCELLED'
    ) NOT NULL DEFAULT 'NONE',
    ADD COLUMN review_count INT UNSIGNED NOT NULL DEFAULT 0;

-- 2. Create meter_reading_reviews table.
CREATE TABLE meter_reading_reviews
(
    meter_reading_review_id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    meter_reading_id         BIGINT UNSIGNED                                                                         NOT NULL,
    property_id              BIGINT UNSIGNED                                                                         NOT NULL,
    room_id                  BIGINT UNSIGNED                                                                         NOT NULL,
    tenant_id                BIGINT UNSIGNED                                                                         NOT NULL,
    reported_by_user_id      BIGINT UNSIGNED                                                                         NOT NULL,
    reviewed_by_user_id      BIGINT UNSIGNED                                                                         NULL,
    review_code              VARCHAR(50)                                                                             NOT NULL,
    status ENUM(
        'PENDING',
        'UNDER_REVIEW',
        'APPROVED',
        'REJECTED',
        'CANCELLED'
    ) NOT NULL DEFAULT 'PENDING',
    review_type ENUM(
        'READING_INCORRECT',
        'IMAGE_UNCLEAR',
        'WRONG_ROOM',
        'OTHER'
    ) NOT NULL DEFAULT 'READING_INCORRECT',
    description              TEXT                                                                                    NULL,
    resolution_note          TEXT                                                                                    NULL,
    created_at               DATETIME(6)                                                                             NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    reviewed_at              DATETIME(6)                                                                             NULL,
    cancelled_at             DATETIME(6)                                                                             NULL,
    updated_at               DATETIME(6)                                                                             NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    version                  BIGINT UNSIGNED                                                                         NOT NULL DEFAULT 0,
    UNIQUE KEY uq_meter_reading_review_code (review_code),
    KEY idx_meter_reading_reviews_meter (meter_reading_id),
    KEY idx_meter_reading_reviews_status (status),
    KEY idx_meter_reading_reviews_room (room_id, status),
    KEY idx_meter_reading_reviews_property (property_id, status),
    CONSTRAINT fk_meter_reading_reviews_meter
        FOREIGN KEY (meter_reading_id)
            REFERENCES meter_readings(meter_reading_id),
    CONSTRAINT fk_meter_reading_reviews_property
        FOREIGN KEY (property_id)
            REFERENCES properties(property_id),
    CONSTRAINT fk_meter_reading_reviews_room
        FOREIGN KEY (room_id)
            REFERENCES rooms(room_id),
    CONSTRAINT fk_meter_reading_reviews_tenant
        FOREIGN KEY (tenant_id)
            REFERENCES tenants(tenant_id),
    CONSTRAINT fk_meter_reading_reviews_reporter
        FOREIGN KEY (reported_by_user_id)
            REFERENCES users(user_id),
    CONSTRAINT fk_meter_reading_reviews_reviewer
        FOREIGN KEY (reviewed_by_user_id)
            REFERENCES users(user_id)
) ENGINE = InnoDB;

-- 3. Create meter_reading_review_attachments table.
CREATE TABLE meter_reading_review_attachments
(
    meter_reading_review_attachment_id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    meter_reading_review_id BIGINT UNSIGNED NOT NULL,
    file_metadata_id BIGINT UNSIGNED NOT NULL,
    uploaded_by_user_id BIGINT UNSIGNED NULL,
    attachment_type ENUM(
        'IMAGE',
        'VIDEO',
        'DOCUMENT',
        'OTHER'
    ) NOT NULL DEFAULT 'IMAGE',
    created_at DATETIME(6)
        NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    KEY idx_meter_reading_review_attachments_review (meter_reading_review_id),
    CONSTRAINT fk_meter_reading_review_attachments_review
        FOREIGN KEY (meter_reading_review_id)
            REFERENCES meter_reading_reviews(meter_reading_review_id),
    CONSTRAINT fk_meter_reading_review_attachments_file
        FOREIGN KEY (file_metadata_id)
            REFERENCES file_metadata(file_metadata_id),
    CONSTRAINT fk_meter_reading_review_attachments_user
        FOREIGN KEY (uploaded_by_user_id)
            REFERENCES users(user_id)
) ENGINE = InnoDB;

-- 4. Create meter_reading_review_status_history table.
CREATE TABLE meter_reading_review_status_history
(
    meter_reading_review_status_history_id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    meter_reading_review_id BIGINT UNSIGNED NOT NULL,
    from_status ENUM(
        'PENDING',
        'UNDER_REVIEW',
        'APPROVED',
        'REJECTED',
        'CANCELLED'
    ) NULL,
    to_status ENUM(
        'PENDING',
        'UNDER_REVIEW',
        'APPROVED',
        'REJECTED',
        'CANCELLED'
    ) NOT NULL,
    changed_by_user_id BIGINT UNSIGNED NULL,
    note VARCHAR(1000),
    created_at DATETIME(6)
        NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    KEY idx_meter_reading_review_status_history_review (meter_reading_review_id),
    CONSTRAINT fk_meter_reading_review_status_history_review
        FOREIGN KEY (meter_reading_review_id)
            REFERENCES meter_reading_reviews(meter_reading_review_id),
    CONSTRAINT fk_meter_reading_review_status_history_user
        FOREIGN KEY (changed_by_user_id)
            REFERENCES users(user_id)
) ENGINE = InnoDB;

-- Important: If V47 and V48 migrations were already run,
-- you will need to manually reverse them or create compensating migrations
-- to drop the utility_complaints tables and data.
-- If they have not been run, you should delete/ignore V47 and V48 files.