-- Property-level staff assignments.
-- Purpose:
--   - map staff users to properties
--   - keep assignment role/scope separate from role_promotions approval flow
--   - support active assignment lookup and reassignment history

CREATE TABLE property_staff_assignments
(
    property_staff_assignment_id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    property_id                  BIGINT UNSIGNED                                                          NOT NULL,
    staff_user_id                BIGINT UNSIGNED                                                          NOT NULL,
    assigned_role                ENUM ('MANAGER','ACCOUNTANT','STAFF','MAINTENANCE','SECURITY','OTHER') NOT NULL DEFAULT 'STAFF',
    assignment_status            ENUM ('ACTIVE','INACTIVE','REMOVED')                                    NOT NULL DEFAULT 'ACTIVE',
    is_primary                   TINYINT(1)                                                              NOT NULL DEFAULT 0,
    notes                        VARCHAR(1000)                                                           NULL,
    assigned_by_user_id          BIGINT UNSIGNED                                                          NULL,
    started_at                   DATETIME(6)                                                             NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    ended_at                     DATETIME(6)                                                             NULL,
    created_at                   DATETIME(6)                                                             NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at                   DATETIME(6)                                                             NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    active_unique_token          TINYINT GENERATED ALWAYS AS (IF(assignment_status = 'ACTIVE', 1, NULL)) VIRTUAL,
    primary_unique_token         TINYINT GENERATED ALWAYS AS (IF(is_primary = 1 AND assignment_status = 'ACTIVE', 1, NULL)) VIRTUAL,

    UNIQUE KEY uq_property_staff_active_role (property_id, staff_user_id, assigned_role, active_unique_token),
    UNIQUE KEY uq_property_staff_primary_role (property_id, assigned_role, primary_unique_token),
    KEY idx_property_staff_property_status (property_id, assignment_status, assigned_role),
    KEY idx_property_staff_user_status (staff_user_id, assignment_status, started_at),
    KEY idx_property_staff_assigned_by (assigned_by_user_id),
    CONSTRAINT fk_property_staff_assignment_property FOREIGN KEY (property_id) REFERENCES properties (property_id),
    CONSTRAINT fk_property_staff_assignment_user FOREIGN KEY (staff_user_id) REFERENCES users (user_id),
    CONSTRAINT fk_property_staff_assignment_assigned_by FOREIGN KEY (assigned_by_user_id) REFERENCES users (user_id),
    CONSTRAINT chk_property_staff_ended_at CHECK (ended_at IS NULL OR ended_at >= started_at)
) ENGINE = InnoDB;