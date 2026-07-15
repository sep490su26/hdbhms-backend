CREATE TABLE IF NOT EXISTS debt_notice_trackers
(
    debt_notice_tracker_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    lease_contract_id      BIGINT UNSIGNED NOT NULL,
    unresponsive_count     INT             NOT NULL DEFAULT 0,
    last_notice_date       DATE            NULL,
    created_at             DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at             DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_debt_notice_contract UNIQUE (lease_contract_id),
    CONSTRAINT fk_debt_notice_contract
        FOREIGN KEY (lease_contract_id) REFERENCES lease_contracts (lease_contract_id)
);

CREATE INDEX idx_debt_notice_last_date
    ON debt_notice_trackers (last_notice_date);

CREATE TABLE IF NOT EXISTS manager_tasks
(
    manager_task_id  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    title            VARCHAR(255) NOT NULL,
    description      TEXT         NULL,
    assignee_id      BIGINT UNSIGNED NULL,
    room_id          BIGINT UNSIGNED NULL,
    lease_contract_id BIGINT UNSIGNED NULL,
    status           VARCHAR(30)  NOT NULL DEFAULT 'PENDING',
    due_date         DATE         NOT NULL,
    completed_at     DATETIME(6)  NULL,
    created_at       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_manager_task_assignee
        FOREIGN KEY (assignee_id) REFERENCES users (user_id),
    CONSTRAINT fk_manager_task_room
        FOREIGN KEY (room_id) REFERENCES rooms (room_id),
    CONSTRAINT fk_manager_task_contract
        FOREIGN KEY (lease_contract_id) REFERENCES lease_contracts (lease_contract_id)
);

CREATE INDEX idx_manager_task_status_due
    ON manager_tasks (status, due_date);

CREATE INDEX idx_manager_task_contract
    ON manager_tasks (lease_contract_id, status);

INSERT IGNORE INTO notification_templates (
    template_key,
    channel,
    title_template,
    body_template,
    status
) VALUES (
    'DEBT_DIRECT_VISIT_REQUIRED',
    'PUSH',
    CONCAT('Cần gặp trực tiếp khách phòng [[', '$', '{roomName}]]'),
    CONCAT(
        'Phòng [[', '$', '{roomName}]] tại [[', '$', '{propertyName}]] còn nợ [[',
        '$', '{totalDebt}]] VND. Hạn xử lý: [[', '$', '{dueDate}]].'
    ),
    'ACTIVE'
);
