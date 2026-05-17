package com.sep490.hdbhms.common;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final JdbcTemplate jdbcTemplate;

    public AuditService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void record(Long actorUserId, String action, String entityType, Long entityId) {
        jdbcTemplate.update("""
                INSERT INTO audit_logs (actor_user_id, action, entity_type, entity_id)
                VALUES (?, ?, ?, ?)
                """, actorUserId, action, entityType, entityId);
    }
}
