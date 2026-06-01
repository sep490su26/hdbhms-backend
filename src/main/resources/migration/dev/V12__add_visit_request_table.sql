CREATE TABLE visit_requests
(
    id              BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    property_id     BIGINT UNSIGNED                                                  NULL,
    room_id         BIGINT UNSIGNED                                                  NULL,
    lead_id         BIGINT UNSIGNED                                                  NULL,
    visitor_name    VARCHAR(255)                                                     NOT NULL,
    visitor_phone   VARCHAR(30)                                                      NOT NULL,
    visitor_email   VARCHAR(255)                                                     NULL,
    preferred_start DATETIME(6)                                                      NOT NULL,
    preferred_end   DATETIME(6)                                                      NULL,
    status          ENUM ('REQUESTED','SCHEDULED','COMPLETED','CANCELLED','NO_SHOW') NOT NULL DEFAULT 'REQUESTED',
    notes           TEXT                                                             NULL,
    created_by      BIGINT UNSIGNED                                                  NOT NULL,
    created_at      DATETIME(6)                                                      NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)                                                      NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    KEY idx_visit_status (status, preferred_start),
    KEY idx_visit_property (property_id),
    CONSTRAINT fk_visit_property FOREIGN KEY (property_id) REFERENCES properties (id),
    CONSTRAINT fk_visit_room FOREIGN KEY (room_id) REFERENCES rooms (id),
    CONSTRAINT fk_visit_lead FOREIGN KEY (lead_id) REFERENCES leads (id),
    CONSTRAINT fk_visit_creator FOREIGN KEY (created_by) REFERENCES users (id)
) ENGINE = InnoDB;