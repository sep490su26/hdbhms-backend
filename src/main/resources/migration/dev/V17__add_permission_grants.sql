CREATE TABLE IF NOT EXISTS hdbhms.permission_grants
(
    permission_grant_id     BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    grantee_user_id         BIGINT UNSIGNED                          NOT NULL,
    target_type             VARCHAR(100)                             NOT NULL,
    target_id               BIGINT UNSIGNED                          NOT NULL,
    source_change_request_id BIGINT UNSIGNED                         null,
    granted_by              BIGINT UNSIGNED                          NOT NULL,
    reason                  TEXT                                     null,
    duration_code           VARCHAR(50)                              NOT NULL,
    granted_at              DATETIME(6)                              NOT NULL,
    expires_at              DATETIME(6)                              NOT NULL,
    revoked_at              DATETIME(6)                              null,
    revoked_by              BIGINT UNSIGNED                          null,
    revoke_reason           TEXT                                     null,
    created_at              DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    updated_at              DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL on update CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_permission_grant_grantee
        FOREIGN KEY (grantee_user_id) REFERENCES hdbhms.users (user_id),
    CONSTRAINT fk_permission_grant_source_request
        FOREIGN KEY (source_change_request_id) REFERENCES hdbhms.change_requests (change_request_id),
    CONSTRAINT fk_permission_grant_granted_by
        FOREIGN KEY (granted_by) REFERENCES hdbhms.users (user_id),
    CONSTRAINT fk_permission_grant_revoked_by
        FOREIGN KEY (revoked_by) REFERENCES hdbhms.users (user_id)
);

CREATE INDEX idx_permission_grant_lookup
    ON hdbhms.permission_grants (grantee_user_id, target_type, target_id, expires_at, revoked_at);

CREATE INDEX idx_permission_grant_source_request
    ON hdbhms.permission_grants (source_change_request_id);

CREATE INDEX idx_permission_grant_owner_review
    ON hdbhms.permission_grants (target_type, target_id, revoked_at, expires_at);

CREATE TABLE IF NOT EXISTS hdbhms.permission_access_audit_logs
(
    permission_access_audit_log_id BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    permission_grant_id            BIGINT UNSIGNED                          NOT NULL,
    viewer_user_id                 BIGINT UNSIGNED                          NOT NULL,
    target_type                    VARCHAR(100)                             NOT NULL,
    target_id                      BIGINT UNSIGNED                          NOT NULL,
    action                         VARCHAR(100)                             NOT NULL,
    reason                         TEXT                                     null,
    ip_address                     VARCHAR(100)                             null,
    user_agent                     VARCHAR(1000)                            null,
    viewed_at                      DATETIME(6)                              NOT NULL,
    created_at                     DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_permission_audit_grant
        FOREIGN KEY (permission_grant_id) REFERENCES hdbhms.permission_grants (permission_grant_id),
    CONSTRAINT fk_permission_audit_viewer
        FOREIGN KEY (viewer_user_id) REFERENCES hdbhms.users (user_id)
);

CREATE INDEX idx_permission_audit_target
    ON hdbhms.permission_access_audit_logs (target_type, target_id, viewed_at);

CREATE INDEX idx_permission_audit_viewer
    ON hdbhms.permission_access_audit_logs (viewer_user_id, viewed_at);

CREATE INDEX idx_permission_audit_grant
    ON hdbhms.permission_access_audit_logs (permission_grant_id);

INSERT INTO hdbhms.permission_grants (
    grantee_user_id,
    target_type,
    target_id,
    source_change_request_id,
    granted_by,
    reason,
    duration_code,
    granted_at,
    expires_at,
    created_at,
    updated_at
)
SELECT cr.requester_id,
       cr.target_type,
       cr.target_id,
       cr.change_request_id,
       COALESCE(cr.resolved_by, cr.requester_id),
       cr.description,
       'DAYS_30',
       COALESCE(cr.resolved_at, NOW(6)),
       DATE_ADD(COALESCE(cr.resolved_at, NOW(6)), INTERVAL 30 DAY),
       NOW(6),
       NOW(6)
FROM hdbhms.change_requests cr
WHERE cr.request_type = 'TENANT_PROFILE_ACCESS'
  AND cr.status = 'APPROVED'
  AND cr.target_type = 'TENANT_PROFILE'
  AND cr.target_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM hdbhms.permission_grants pg
      WHERE pg.source_change_request_id = cr.change_request_id
  );
