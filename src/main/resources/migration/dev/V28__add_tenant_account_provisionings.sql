CREATE TABLE tenant_account_provisionings
(
    id                 BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,

    tenant_profile_id  BIGINT UNSIGNED NOT NULL,
    user_id            BIGINT UNSIGNED NULL,

    first_contract_id  BIGINT UNSIGNED NULL,
    latest_contract_id BIGINT UNSIGNED NULL,

    status             VARCHAR(50)     NOT NULL DEFAULT 'NOT_PROVISIONED',
    recipient_email    VARCHAR(255)    NULL,

    sent_at            DATETIME(6)     NULL,
    failed_at          DATETIME(6)     NULL,
    failure_reason     TEXT            NULL,

    attempt_count      INT UNSIGNED    NOT NULL DEFAULT 0,
    last_attempt_at    DATETIME(6)     NULL,

    created_at         DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at         DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT uq_tenant_account_provisionings_tenant_profile
        UNIQUE (tenant_profile_id),

    KEY idx_tap_user (user_id),
    KEY idx_tap_status (status),
    KEY idx_tap_latest_contract (latest_contract_id),

    CONSTRAINT fk_tap_tenant_profile
        FOREIGN KEY (tenant_profile_id) REFERENCES person_profiles (id),

    CONSTRAINT fk_tap_user
        FOREIGN KEY (user_id) REFERENCES users (id),

    CONSTRAINT fk_tap_first_contract
        FOREIGN KEY (first_contract_id) REFERENCES lease_contracts (id),

    CONSTRAINT fk_tap_latest_contract
        FOREIGN KEY (latest_contract_id) REFERENCES lease_contracts (id),

    CONSTRAINT chk_tap_status
        CHECK (status IN (
            'NOT_PROVISIONED',
            'PENDING',
            'SENT',
            'FAILED',
            'ACTIVE'
        ))
) ENGINE = InnoDB;

INSERT INTO tenant_account_provisionings
(
    tenant_profile_id,
    user_id,
    first_contract_id,
    latest_contract_id,
    status,
    recipient_email,
    created_at,
    updated_at
)
SELECT
    co.tenant_profile_id,
    u.id AS user_id,
    MIN(lc.id) AS first_contract_id,
    MAX(lc.id) AS latest_contract_id,
    CASE
        WHEN u.id IS NULL THEN 'NOT_PROVISIONED'
        WHEN u.last_login_at IS NOT NULL OR u.must_change_password = FALSE THEN 'ACTIVE'
        ELSE 'SENT'
    END AS status,
    COALESCE(pp.email, u.email) AS recipient_email,
    CURRENT_TIMESTAMP(6) AS created_at,
    CURRENT_TIMESTAMP(6) AS updated_at
FROM contract_occupants co
JOIN lease_contracts lc
    ON lc.id = co.contract_id
    AND lc.deleted_at IS NULL
JOIN person_profiles pp
    ON pp.id = co.tenant_profile_id
    AND pp.deleted_at IS NULL
LEFT JOIN users u
    ON u.id = pp.user_id
    AND u.deleted_at IS NULL
WHERE co.tenant_profile_id IS NOT NULL
GROUP BY
    co.tenant_profile_id,
    u.id,
    u.last_login_at,
    u.must_change_password,
    pp.email,
    u.email;
