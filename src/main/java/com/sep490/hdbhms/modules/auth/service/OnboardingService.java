package com.sep490.hdbhms.modules.auth.service;

import com.sep490.hdbhms.modules.auth.dto.OnboardingStateResponse;
import com.sep490.hdbhms.modules.user.entity.User;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Locale;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class OnboardingService {

    public static final String CHANGE_PASSWORD = "CHANGE_PASSWORD";
    public static final String IDENTITY_VERIFICATION = "IDENTITY_VERIFICATION";
    public static final String HOME = "HOME";
    private static final boolean MOBILE_IDENTITY_VERIFICATION_REQUIRED = false;

    private final JdbcTemplate jdbcTemplate;

    public OnboardingService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public OnboardingStateResponse resolve(User user) {
        boolean identityCompleted = isIdentityCompleted(user);
        return new OnboardingStateResponse(
                user.getId(),
                user.isMustChangePassword(),
                identityCompleted,
                MOBILE_IDENTITY_VERIFICATION_REQUIRED,
                nextStep(user.isMustChangePassword())
        );
    }

    public boolean isIdentityCompleted(User user) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM person_profiles pp
                WHERE pp.deleted_at IS NULL
                  AND pp.portrait_file_id IS NOT NULL
                  AND (
                    pp.phone = ?
                    OR LOWER(pp.email) = LOWER(?)
                    OR pp.id IN (
                        SELECT da.depositor_person_profile_id
                        FROM deposit_agreements da
                        JOIN tenants t ON t.id = da.tenant_id
                        WHERE t.user_id = ?
                          AND da.depositor_person_profile_id IS NOT NULL
                    )
                    OR pp.id IN (
                        SELECT lc.primary_tenant_profile_id
                        FROM lease_contracts lc
                        JOIN contract_occupants co ON co.contract_id = lc.id
                        JOIN tenants t ON t.id = co.tenant_id
                        WHERE t.user_id = ?
                    )
                  )
                  AND EXISTS (
                    SELECT 1
                    FROM identity_documents idoc
                    WHERE idoc.profile_id = pp.id
                      AND idoc.status = 'ACTIVE'
                      AND idoc.front_file_id IS NOT NULL
                      AND idoc.back_file_id IS NOT NULL
                  )
                """, Integer.class, user.getPhone(), user.getEmail(), user.getId(), user.getId());
        return count != null && count > 0;
    }

    @Transactional
    public Long resolveOrCreatePersonProfile(User user) {
        Long existingId = findPersonProfileId(user);
        if (existingId != null) {
            return existingId;
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO person_profiles (full_name, phone, email)
                    VALUES (?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, resolveFullName(user));
            statement.setString(2, user.getPhone());
            statement.setString(3, user.getEmail());
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Cannot create person profile");
        }
        return key.longValue();
    }

    public String resolveFullName(User user) {
        String profileName = queryNullableString("""
                SELECT pp.full_name
                FROM person_profiles pp
                WHERE pp.deleted_at IS NULL
                  AND (
                    pp.phone = ?
                    OR LOWER(pp.email) = LOWER(?)
                  )
                ORDER BY pp.id DESC
                LIMIT 1
                """, user.getPhone(), user.getEmail());
        if (StringUtils.hasText(profileName)) {
            return profileName;
        }

        if (StringUtils.hasText(user.getEmail()) && user.getEmail().contains("@")) {
            return user.getEmail().substring(0, user.getEmail().indexOf('@'));
        }
        return user.getPhone();
    }

    public boolean hasActiveTenant(User user, Long tenantId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM tenants t
                WHERE t.id = ?
                  AND t.user_id = ?
                  AND t.deleted_at IS NULL
                """, Integer.class, tenantId, user.getId());
        return count != null && count > 0;
    }

    public List<Long> findActiveTenantIds(Long userId) {
        return jdbcTemplate.queryForList("""
                SELECT t.id
                FROM tenants t
                WHERE t.user_id = ?
                  AND t.deleted_at IS NULL
                """, Long.class, userId);
    }

    private String nextStep(boolean mustChangePassword) {
        if (mustChangePassword) {
            return CHANGE_PASSWORD;
        }
        return HOME;
    }

    private Long findPersonProfileId(User user) {
        Long profileId = queryNullableLong("""
                SELECT pp.id
                FROM person_profiles pp
                WHERE pp.deleted_at IS NULL
                  AND (
                    pp.phone = ?
                    OR LOWER(pp.email) = LOWER(?)
                  )
                ORDER BY pp.id DESC
                LIMIT 1
                """, user.getPhone(), user.getEmail());
        if (profileId != null) {
            return profileId;
        }

        return queryNullableLong("""
                SELECT pp.id
                FROM person_profiles pp
                WHERE pp.deleted_at IS NULL
                  AND pp.id IN (
                    SELECT da.depositor_person_profile_id
                    FROM deposit_agreements da
                    JOIN tenants t ON t.id = da.tenant_id
                    WHERE t.user_id = ?
                      AND da.depositor_person_profile_id IS NOT NULL
                  )
                ORDER BY pp.id DESC
                LIMIT 1
                """, user.getId());
    }

    private Long queryNullableLong(String sql, Object... args) {
        try {
            return jdbcTemplate.queryForObject(sql, Long.class, args);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    private String queryNullableString(String sql, Object... args) {
        try {
            return jdbcTemplate.queryForObject(sql, String.class, args);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }
}
