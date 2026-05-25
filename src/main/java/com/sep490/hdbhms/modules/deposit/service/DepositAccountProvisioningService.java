package com.sep490.hdbhms.modules.deposit.service;

import com.sep490.hdbhms.common.AuditService;
import com.sep490.hdbhms.common.email.EmailService;
import com.sep490.hdbhms.common.exception.ApiException;
import java.security.SecureRandom;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class DepositAccountProvisioningService {

    private static final String TEMP_PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AuditService auditService;

    public DepositAccountProvisioningService(
            JdbcTemplate jdbcTemplate,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            AuditService auditService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.auditService = auditService;
    }

    @Transactional
    public Long provisionAfterDepositPaid(Long depositAgreementId) {
        Map<String, Object> deposit = jdbcTemplate.queryForMap("""
                SELECT da.id,
                       da.room_id,
                       da.depositor_person_profile_id,
                       pp.full_name,
                       pp.phone,
                       pp.email,
                       r.property_id
                FROM deposit_agreements da
                JOIN person_profiles pp ON pp.id = da.depositor_person_profile_id
                JOIN rooms r ON r.id = da.room_id
                WHERE da.id = ?
                  AND da.status IN ('PAID', 'CONFIRMED', 'CONVERTED_TO_LEASE')
                """, depositAgreementId);

        String phone = value(deposit.get("phone"));
        String email = value(deposit.get("email"));
        if (!StringUtils.hasText(phone) && !StringUtils.hasText(email)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Thông tin đặt cọc thiếu SĐT/email để tạo tài khoản");
        }

        Long userId = findUserId(phone, email);
        String temporaryPassword = null;
        if (userId == null) {
            temporaryPassword = generateTemporaryPassword();
            userId = createUser(phone, email, passwordEncoder.encode(temporaryPassword));
            auditService.record(userId, "TEMP_ACCOUNT_CREATED", "USER", userId);
        }

        Long propertyId = ((Number) deposit.get("property_id")).longValue();
        Long tenantId = upsertTenant(userId, propertyId);
        upsertTenantRole(userId, propertyId);

        jdbcTemplate.update("""
                UPDATE deposit_agreements
                SET tenant_id = ?, updated_at = NOW(6)
                WHERE id = ?
                """, tenantId, depositAgreementId);

        if (temporaryPassword != null && StringUtils.hasText(email)) {
            String loginId = StringUtils.hasText(phone) ? phone : email;
            emailService.sendTemporaryAccount(email, loginId, temporaryPassword);
        }

        return userId;
    }

    private Long findUserId(String phone, String email) {
        List<Long> ids = jdbcTemplate.queryForList("""
                SELECT id
                FROM users
                WHERE deleted_at IS NULL
                  AND (
                    (? <> '' AND phone = ?)
                    OR (? <> '' AND LOWER(email) = LOWER(?))
                  )
                LIMIT 1
                """, Long.class, nullToEmpty(phone), nullToEmpty(phone), nullToEmpty(email), nullToEmpty(email));
        return ids.isEmpty() ? null : ids.get(0);
    }

    private Long createUser(String phone, String email, String passwordHash) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO users (
                        phone,
                        email,
                        password_hash,
                        role,
                        status,
                        email_verified,
                        must_change_password
                    )
                    VALUES (?, ?, ?, 'TENANT', 'ACTIVE', FALSE, TRUE)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, nullToEmpty(phone));
            statement.setString(2, nullToEmpty(email));
            statement.setString(3, passwordHash);
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Cannot create tenant user");
        }
        return key.longValue();
    }

    private Long upsertTenant(Long userId, Long propertyId) {
        Long existingTenantId = jdbcTemplate.query("""
                SELECT id
                FROM tenants
                WHERE user_id = ?
                  AND property_id = ?
                  AND deleted_at IS NULL
                LIMIT 1
                """, rs -> rs.next() ? rs.getLong("id") : null, userId, propertyId);
        if (existingTenantId != null) {
            return existingTenantId;
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO tenants (user_id, property_id)
                    VALUES (?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, userId);
            statement.setLong(2, propertyId);
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Cannot create tenant");
        }
        return key.longValue();
    }

    private void upsertTenantRole(Long userId, Long propertyId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM role_promotions
                WHERE user_id = ?
                  AND role = 'TENANT'
                  AND deleted_at IS NULL
                """, Integer.class, userId);
        if (count != null && count > 0) {
            jdbcTemplate.update("""
                    UPDATE role_promotions
                    SET status = 'ACTIVE',
                        property_id = ?,
                        approved_at = NOW(6),
                        updated_at = NOW(6)
                    WHERE user_id = ?
                      AND role = 'TENANT'
                      AND deleted_at IS NULL
                    """, propertyId, userId);
            return;
        }

        jdbcTemplate.update("""
                INSERT INTO role_promotions (user_id, role, status, property_id, approved_at)
                VALUES (?, 'TENANT', 'ACTIVE', ?, NOW(6))
                """, userId, propertyId);
    }

    private String generateTemporaryPassword() {
        StringBuilder password = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            password.append(TEMP_PASSWORD_CHARS.charAt(SECURE_RANDOM.nextInt(TEMP_PASSWORD_CHARS.length())));
        }
        return password.toString();
    }

    private String value(Object value) {
        return value == null ? "" : value.toString();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
