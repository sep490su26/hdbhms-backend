package com.sep490.hdbhms.modules.rule.repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PropertyRuleRepository {

    private final JdbcTemplate jdbcTemplate;

    public PropertyRuleRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long findCurrentPropertyId(Long tenantId) {
        try {
            return jdbcTemplate.queryForObject("""
                    SELECT r.property_id
                    FROM contract_occupants co
                    JOIN lease_contracts lc ON lc.id = co.contract_id
                    JOIN rooms r ON r.id = lc.room_id
                    WHERE co.tenant_id = ?
                      AND co.status = 'ACTIVE'
                      AND lc.status IN ('ACTIVE', 'EXPIRING_SOON')
                      AND lc.deleted_at IS NULL
                      AND r.deleted_at IS NULL
                    ORDER BY
                      CASE lc.status
                        WHEN 'ACTIVE' THEN 0
                        WHEN 'EXPIRING_SOON' THEN 1
                        ELSE 2
                      END,
                      lc.start_date DESC,
                      lc.id DESC
                    LIMIT 1
                    """, Long.class, tenantId);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    public List<PropertyRuleRow> findActiveRulesByPropertyId(Long propertyId) {
        return jdbcTemplate.query("""
                SELECT id,
                       rule_code,
                       rule_category,
                       icon_key,
                       title,
                       description,
                       default_fine_amount,
                       fine_unit,
                       is_highlight,
                       display_note,
                       sort_order,
                       status,
                       updated_at
                FROM property_rules
                WHERE property_id = ?
                  AND status = 'ACTIVE'
                ORDER BY sort_order ASC, id ASC
                """, (rs, rowNum) -> mapRule(rs), propertyId);
    }

    private PropertyRuleRow mapRule(ResultSet rs) throws java.sql.SQLException {
        return new PropertyRuleRow(
                rs.getLong("id"),
                rs.getString("rule_code"),
                rs.getString("rule_category"),
                rs.getString("icon_key"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getBigDecimal("default_fine_amount"),
                rs.getString("fine_unit"),
                rs.getBoolean("is_highlight"),
                rs.getString("display_note"),
                rs.getInt("sort_order"),
                rs.getString("status"),
                rs.getTimestamp("updated_at").toLocalDateTime()
        );
    }

    public record PropertyRuleRow(
            Long id,
            String ruleCode,
            String ruleCategory,
            String iconKey,
            String title,
            String description,
            BigDecimal defaultFineAmount,
            String fineUnit,
            Boolean isHighlight,
            String displayNote,
            Integer sortOrder,
            String status,
            LocalDateTime updatedAt
    ) {
    }
}
