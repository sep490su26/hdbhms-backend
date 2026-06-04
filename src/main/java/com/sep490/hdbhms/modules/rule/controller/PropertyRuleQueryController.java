package com.sep490.hdbhms.modules.rule.controller;

import com.sep490.hdbhms.modules.rule.dto.PropertyRuleItemResponse;
import com.sep490.hdbhms.modules.rule.dto.PropertyRuleResponse;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import com.sep490.hdbhms.shared.utils.AuthUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/property-rules")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PropertyRuleQueryController {
    JdbcTemplate jdbcTemplate;

    @GetMapping("/me")
    public ApiResponse<PropertyRuleResponse> getMyPropertyRules(
            @RequestParam(required = false) Long tenantId
    ) {
        Long userId = AuthUtils.getCurrentAuthenticationId();
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session expired");
        }

        Long propertyId = resolvePropertyId(userId, tenantId);
        if (propertyId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant property not found");
        }

        List<PropertyRuleItemResponse> items = jdbcTemplate.query("""
                        SELECT *
                        FROM property_rules
                        WHERE property_id = ? AND status = 'ACTIVE'
                        ORDER BY sort_order ASC, id ASC
                        """,
                (rs, rowNum) -> mapRule(rs),
                propertyId
        );
        LocalDateTime updatedAt = jdbcTemplate.queryForObject("""
                        SELECT MAX(updated_at)
                        FROM property_rules
                        WHERE property_id = ? AND status = 'ACTIVE'
                        """,
                (rs, rowNum) -> toLocalDateTime(rs.getTimestamp(1)),
                propertyId
        );

        return ApiResponse.<PropertyRuleResponse>builder()
                .data(new PropertyRuleResponse(updatedAt, items))
                .build();
    }

    private Long resolvePropertyId(Long userId, Long tenantId) {
        List<Long> propertyIds;
        if (tenantId != null) {
            propertyIds = jdbcTemplate.query("""
                            SELECT property_id
                            FROM tenants
                            WHERE id = ? AND user_id = ? AND deleted_at IS NULL
                            LIMIT 1
                            """,
                    (rs, rowNum) -> rs.getLong("property_id"),
                    tenantId,
                    userId
            );
        } else {
            propertyIds = jdbcTemplate.query("""
                            SELECT property_id
                            FROM tenants
                            WHERE user_id = ? AND deleted_at IS NULL
                            ORDER BY id DESC
                            LIMIT 1
                            """,
                    (rs, rowNum) -> rs.getLong("property_id"),
                    userId
            );
        }
        return propertyIds.isEmpty() ? null : propertyIds.getFirst();
    }

    private PropertyRuleItemResponse mapRule(ResultSet rs) throws SQLException {
        String ruleCode = rs.getString("rule_code");
        return new PropertyRuleItemResponse(
                rs.getLong("id"),
                ruleCode,
                stringOrFallback(rs, "rule_category", inferCategory(ruleCode)),
                nullableString(rs, "icon_key"),
                rs.getString("title"),
                rs.getString("description"),
                nullableBigDecimal(rs, "default_fine_amount"),
                nullableString(rs, "fine_unit"),
                nullableBoolean(rs, "is_highlight"),
                nullableString(rs, "display_note"),
                rs.getInt("sort_order"),
                rs.getString("status")
        );
    }

    private String inferCategory(String ruleCode) {
        String code = ruleCode == null ? "" : ruleCode.trim().toUpperCase();
        if (code.startsWith("SECURITY_")) {
            return "SECURITY";
        }
        if (code.startsWith("HYGIENE_")) {
            return "HYGIENE";
        }
        if (code.startsWith("UTILITY_")) {
            return "UTILITY";
        }
        if (code.startsWith("FINE_")
                || code.contains("WIFI_RESET")
                || code.contains("UNAUTHORIZED_REPAIR")) {
            return "FINE";
        }
        return "GENERAL";
    }

    private String stringOrFallback(ResultSet rs, String column, String fallback) throws SQLException {
        String value = nullableString(rs, column);
        return value == null || value.isBlank() ? fallback : value;
    }

    private String nullableString(ResultSet rs, String column) throws SQLException {
        if (!hasColumn(rs, column)) {
            return null;
        }
        return rs.getString(column);
    }

    private BigDecimal nullableBigDecimal(ResultSet rs, String column) throws SQLException {
        if (!hasColumn(rs, column)) {
            return null;
        }
        return rs.getBigDecimal(column);
    }

    private Boolean nullableBoolean(ResultSet rs, String column) throws SQLException {
        if (!hasColumn(rs, column)) {
            return null;
        }
        boolean value = rs.getBoolean(column);
        return rs.wasNull() ? null : value;
    }

    private boolean hasColumn(ResultSet rs, String column) throws SQLException {
        try {
            rs.findColumn(column);
            return true;
        } catch (SQLException ignored) {
            return false;
        }
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
