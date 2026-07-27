CREATE TABLE IF NOT EXISTS hdbhms.ai_chat_history
(
    ai_chat_history_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    owner_user_id      BIGINT UNSIGNED NOT NULL,
    user_id            BIGINT UNSIGNED NULL,
    session_id         VARCHAR(255) NULL,
    question           TEXT NOT NULL,
    sql_query          TEXT NULL,
    sql_result         JSON NULL,
    ai_response        TEXT NULL,
    visualization      JSON NULL,
    is_successful      TINYINT(1) DEFAULT 1 NOT NULL,
    execution_time_ms  INT NULL,
    created_at         DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    INDEX idx_aich_owner (owner_user_id),
    INDEX idx_aich_session (session_id),
    INDEX idx_aich_time (created_at)
);

CREATE TABLE IF NOT EXISTS hdbhms.ai_audit_logs
(
    log_id                 BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    session_id             VARCHAR(100) NULL,
    owner_user_id          BIGINT UNSIGNED NOT NULL,
    period                 VARCHAR(20) NOT NULL,
    question               TEXT NOT NULL,
    system_instruction_len INT NULL,
    skills_loaded          TEXT NULL,
    method                 VARCHAR(100) NULL,
    tools_called           JSON NULL,
    reply                  TEXT NULL,
    latency_ms             DOUBLE NULL,
    created_at             DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    INDEX idx_audit_owner (owner_user_id, period),
    INDEX idx_audit_created (created_at)
);
