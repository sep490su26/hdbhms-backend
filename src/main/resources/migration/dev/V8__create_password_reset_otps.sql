CREATE TABLE IF NOT EXISTS password_reset_otps
(
    id                    BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    user_id               BIGINT UNSIGNED NOT NULL,
    email                 VARCHAR(255)    NOT NULL,
    otp_hash              VARCHAR(255)    NOT NULL,
    expires_at            DATETIME(6)     NOT NULL,
    used_at               DATETIME(6)     NULL,
    attempt_count         INT             NOT NULL DEFAULT 0,
    locked_at             DATETIME(6)     NULL,
    reset_token_hash      VARCHAR(255)    NULL,
    reset_token_expires_at DATETIME(6)    NULL,
    reset_token_used_at   DATETIME(6)     NULL,
    created_at            DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    KEY idx_password_reset_user_created (user_id, created_at),
    KEY idx_password_reset_email (email),
    CONSTRAINT fk_password_reset_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB;
