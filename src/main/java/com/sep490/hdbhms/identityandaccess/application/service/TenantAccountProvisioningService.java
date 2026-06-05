package com.sep490.hdbhms.identityandaccess.application.service;

import com.sep490.hdbhms.identityandaccess.application.port.out.PersonProfileRepository;
import com.sep490.hdbhms.identityandaccess.application.port.out.SendPreCreatedAccountPort;
import com.sep490.hdbhms.identityandaccess.application.port.out.UserRepository;
import com.sep490.hdbhms.identityandaccess.domain.model.PersonProfile;
import com.sep490.hdbhms.identityandaccess.domain.model.User;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.AccountStatus;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.response.TenantAccountProvisioningResponse;
import com.sep490.hdbhms.occupancy.application.port.out.TenantRepository;
import com.sep490.hdbhms.occupancy.domain.model.Tenant;
import com.sep490.hdbhms.occupancy.domain.value_objects.LeaseStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.RoomStatus;
import com.sep490.hdbhms.shared.utils.RandomPasswordUtils;
import com.sep490.hdbhms.shared.utils.StringUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TenantAccountProvisioningService {
    static final String SYNTHETIC_TENANT_EMAIL_DOMAIN = "tenant.hdbhms.local";

    JdbcTemplate jdbcTemplate;
    UserRepository userRepository;
    TenantRepository tenantRepository;
    PasswordEncoder passwordEncoder;
    PersonProfileRepository personProfileRepository;
    SendPreCreatedAccountPort sendPreCreatedAccountPort;

    @Transactional(readOnly = true)
    public List<TenantAccountProvisioningResponse> findProvisioningCandidates() {
        return jdbcTemplate.query("""
                        SELECT *
                        FROM (
                            %s
                        ) account_candidates
                        ORDER BY signed_at DESC, contract_id DESC, role_order ASC, full_name
                        """.formatted(baseCandidateSql()),
                (rs, rowNum) -> toResponse(rs)
        );
    }

    public TenantAccountProvisioningResponse provisionPrimaryTenantAccount(Long contractId) {
        List<TenantAccountProvisioningResponse> occupants = findContractOccupants(contractId);
        if (occupants.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hợp đồng thuê.");
        }

        TenantAccountProvisioningResponse primary = occupants.stream()
                .filter(item -> "PRIMARY".equalsIgnoreCase(item.getRoomRole()))
                .findFirst()
                .orElse(occupants.getFirst());

        if (primary.getContractStatus() != LeaseStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hợp đồng phải ở trạng thái ACTIVE trước khi gửi tài khoản.");
        }
        if (StringUtils.isEmpty(primary.getRecipientEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hợp đồng chưa có email người ký chính để nhận thông tin tài khoản.");
        }

        List<SendPreCreatedAccountPort.AccountCredential> credentials = new ArrayList<>();
        for (TenantAccountProvisioningResponse occupant : occupants) {
            ProvisionedAccount account = provisionOccupantAccount(occupant);
            if (!StringUtils.isEmpty(account.temporaryPassword())) {
                credentials.add(new SendPreCreatedAccountPort.AccountCredential(
                        occupant.getFullName(),
                        occupant.getPhone(),
                        account.temporaryPassword(),
                        occupant.getRoomRole()
                ));
            }
        }

        if (!credentials.isEmpty()) {
            sendPreCreatedAccountPort.sendAccountInformationBatch(
                    primary.getRecipientEmail(),
                    primary.getFullName(),
                    credentials
            );
        }

        log.info("Provisioned tenant accounts from lease contract. contractId={}, createdOrIssued={}",
                contractId,
                credentials.size());

        return findPrimaryContractOccupant(contractId).toBuilder()
                .message(credentials.isEmpty()
                        ? "Các tài khoản trong hợp đồng đã được kích hoạt, không gửi lại mật khẩu tạm."
                        : "Đã gửi thông tin tài khoản cho các khách thuê trong hợp đồng.")
                .build();
    }

    private ProvisionedAccount provisionOccupantAccount(TenantAccountProvisioningResponse occupant) {
        if (StringUtils.isEmpty(occupant.getPhone())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Hồ sơ " + safeName(occupant.getFullName()) + " chưa có số điện thoại.");
        }

        Long profileId = resolveOrCreatePersonProfile(occupant);
        User user = resolveExistingUser(occupant, profileId);
        if (user != null
                && (user.getLastLoginAt() != null || !user.isMustChangePassword())) {
            ensureTenantMembership(occupant.getPropertyId(), user.getId());
            linkProfileIfNeeded(profileId, user.getId());
            return new ProvisionedAccount(user.getId(), null);
        }

        String temporaryPassword = RandomPasswordUtils.generatePassword(6, true, true);
        User savedUser = resolveOrCreateUser(occupant, user, temporaryPassword);
        ensureTenantMembership(occupant.getPropertyId(), savedUser.getId());
        linkProfileIfNeeded(profileId, savedUser.getId());
        return new ProvisionedAccount(savedUser.getId(), temporaryPassword);
    }

    private User resolveExistingUser(TenantAccountProvisioningResponse occupant, Long profileId) {
        Long linkedUserId = occupant.getUserId();
        if (linkedUserId == null && profileId != null && !profileId.equals(occupant.getProfileId())) {
            linkedUserId = jdbcTemplate.query("""
                            SELECT user_id
                            FROM person_profiles
                            WHERE id = ?
                              AND deleted_at IS NULL
                            LIMIT 1
                            """,
                    rs -> rs.next() ? getLongOrNull(rs, "user_id") : null,
                    profileId);
        }
        if (linkedUserId != null) {
            return userRepository.findById(linkedUserId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản đã liên kết với hồ sơ."));
        }
        return userRepository.findByPhoneOrEmailAndDeletedAtIsNull(
                        occupant.getPhone(),
                        resolveUserEmail(occupant)
                )
                .orElse(null);
    }

    private User resolveOrCreateUser(
            TenantAccountProvisioningResponse occupant,
            User existingUser,
            String temporaryPassword
    ) {
        String temporaryPasswordHash = passwordEncoder.encode(temporaryPassword);
        User user = existingUser;
        if (user == null) {
            user = User.newUser(
                    occupant.getPhone(),
                    resolveUserEmail(occupant),
                    temporaryPasswordHash,
                    Role.TENANT
            );
            user.activeAccount();
        } else {
            if (user.getRole() != Role.TENANT) {
                user.assignRole(Role.TENANT);
            }
            user.issueTemporaryPassword(temporaryPasswordHash);
        }
        return userRepository.save(user);
    }

    private Long resolveOrCreatePersonProfile(TenantAccountProvisioningResponse occupant) {
        if (occupant.getProfileId() != null) {
            return occupant.getProfileId();
        }

        Long existingProfileId = jdbcTemplate.query("""
                        SELECT id
                        FROM person_profiles
                        WHERE phone = ?
                          AND deleted_at IS NULL
                        ORDER BY user_id IS NULL, id DESC
                        LIMIT 1
                        """,
                rs -> rs.next() ? rs.getLong("id") : null,
                occupant.getPhone()
        );
        if (existingProfileId != null) {
            return existingProfileId;
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement("""
                            INSERT INTO person_profiles (
                                full_name,
                                phone,
                                email,
                                created_at,
                                updated_at
                            )
                            VALUES (?, ?, ?, NOW(6), NOW(6))
                            """,
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, safeName(occupant.getFullName()));
            statement.setString(2, occupant.getPhone());
            statement.setString(3, null);
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không tạo được hồ sơ người ở cùng.");
        }
        return key.longValue();
    }

    private void ensureTenantMembership(Long propertyId, Long userId) {
        tenantRepository.findByUserIdAndPropertyId(userId, propertyId)
                .orElseGet(() -> tenantRepository.save(Tenant.newTenant(propertyId, userId)));
    }

    private void linkProfileIfNeeded(Long profileId, Long userId) {
        PersonProfile profile = personProfileRepository.findById(profileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hồ sơ khách thuê."));
        if (userId.equals(profile.getUserId())) {
            return;
        }
        profile.linkUser(userId);
        personProfileRepository.save(profile);
    }

    private List<TenantAccountProvisioningResponse> findContractOccupants(Long contractId) {
        return jdbcTemplate.query("""
                        SELECT *
                        FROM (
                            %s
                        ) account_candidates
                        WHERE contract_id = ?
                        ORDER BY role_order ASC, full_name
                        """.formatted(baseCandidateSql()),
                (rs, rowNum) -> toResponse(rs),
                contractId
        );
    }

    private TenantAccountProvisioningResponse findPrimaryContractOccupant(Long contractId) {
        try {
            return jdbcTemplate.queryForObject("""
                            SELECT *
                            FROM (
                                %s
                            ) account_candidates
                            WHERE contract_id = ?
                            ORDER BY role_order ASC, full_name
                            LIMIT 1
                            """.formatted(baseCandidateSql()),
                    (rs, rowNum) -> toResponse(rs),
                    contractId
            );
        } catch (EmptyResultDataAccessException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hợp đồng thuê.");
        }
    }

    private String baseCandidateSql() {
        return """
                SELECT
                    0 AS role_order,
                    lc.id AS contract_id,
                    lc.contract_code,
                    lc.status AS contract_status,
                    lc.start_date,
                    lc.end_date,
                    lc.signed_at,
                    p.id AS property_id,
                    p.name AS property_name,
                    r.id AS room_id,
                    r.room_code,
                    r.current_status AS room_status,
                    pp.id AS profile_id,
                    pp.user_id AS profile_user_id,
                    pp.full_name,
                    pp.phone,
                    pp.email,
                    pp.email AS recipient_email,
                    'PRIMARY' AS room_role,
                    GREATEST(
                        COALESCE(df.occupant_count, 1),
                        1 + COALESCE((
                            SELECT COUNT(*)
                            FROM deposit_form_co_occupants dco_count
                            WHERE dco_count.deposit_form_id = df.id
                        ), 0)
                    ) AS room_occupant_count,
                    r.max_occupants AS room_max_occupants,
                    u.id AS user_id,
                    u.role,
                    u.status AS account_status,
                    u.must_change_password,
                    u.last_login_at,
                    u.created_at AS account_created_at
                FROM lease_contracts lc
                JOIN rooms r ON r.id = lc.room_id
                JOIN properties p ON p.id = r.property_id
                JOIN person_profiles pp ON pp.id = lc.primary_tenant_profile_id
                LEFT JOIN deposit_agreements da ON da.id = lc.deposit_agreement_id
                LEFT JOIN deposit_forms df ON df.id = da.deposit_form_id
                LEFT JOIN users u ON u.id = pp.user_id AND u.deleted_at IS NULL
                WHERE lc.deleted_at IS NULL
                  AND lc.status = 'ACTIVE'
                  AND pp.deleted_at IS NULL

                UNION ALL

                SELECT
                    dco.display_order AS role_order,
                    lc.id AS contract_id,
                    lc.contract_code,
                    lc.status AS contract_status,
                    lc.start_date,
                    lc.end_date,
                    lc.signed_at,
                    p.id AS property_id,
                    p.name AS property_name,
                    r.id AS room_id,
                    r.room_code,
                    r.current_status AS room_status,
                    pp.id AS profile_id,
                    pp.user_id AS profile_user_id,
                    COALESCE(pp.full_name, dco.full_name) AS full_name,
                    COALESCE(pp.phone, dco.phone) AS phone,
                    pp.email,
                    primary_pp.email AS recipient_email,
                    'CO_OCCUPANT' AS room_role,
                    GREATEST(
                        COALESCE(df.occupant_count, 1),
                        1 + COALESCE((
                            SELECT COUNT(*)
                            FROM deposit_form_co_occupants dco_count
                            WHERE dco_count.deposit_form_id = df.id
                        ), 0)
                    ) AS room_occupant_count,
                    r.max_occupants AS room_max_occupants,
                    u.id AS user_id,
                    u.role,
                    u.status AS account_status,
                    u.must_change_password,
                    u.last_login_at,
                    u.created_at AS account_created_at
                FROM lease_contracts lc
                JOIN rooms r ON r.id = lc.room_id
                JOIN properties p ON p.id = r.property_id
                JOIN person_profiles primary_pp ON primary_pp.id = lc.primary_tenant_profile_id
                JOIN deposit_agreements da ON da.id = lc.deposit_agreement_id
                JOIN deposit_forms df ON df.id = da.deposit_form_id
                JOIN deposit_form_co_occupants dco ON dco.deposit_form_id = df.id
                LEFT JOIN person_profiles pp ON pp.id = (
                    SELECT pp2.id
                    FROM person_profiles pp2
                    WHERE pp2.deleted_at IS NULL
                      AND pp2.phone = dco.phone
                    ORDER BY pp2.user_id IS NULL, pp2.id DESC
                    LIMIT 1
                )
                LEFT JOIN users u ON u.id = pp.user_id AND u.deleted_at IS NULL
                WHERE lc.deleted_at IS NULL
                  AND lc.status = 'ACTIVE'
                  AND primary_pp.deleted_at IS NULL
                  AND dco.phone <> primary_pp.phone
                """;
    }

    private TenantAccountProvisioningResponse toResponse(ResultSet rs) throws SQLException {
        Long userId = getLongOrNull(rs, "user_id");
        Boolean mustChangePassword = getBooleanOrNull(rs, "must_change_password");
        String recipientEmail = rs.getString("recipient_email");
        return TenantAccountProvisioningResponse.builder()
                .contractId(rs.getLong("contract_id"))
                .contractCode(rs.getString("contract_code"))
                .contractStatus(LeaseStatus.valueOf(rs.getString("contract_status")))
                .startDate(rs.getDate("start_date") != null ? rs.getDate("start_date").toLocalDate() : null)
                .endDate(rs.getDate("end_date") != null ? rs.getDate("end_date").toLocalDate() : null)
                .signedAt(rs.getTimestamp("signed_at") != null ? rs.getTimestamp("signed_at").toLocalDateTime() : null)
                .propertyId(rs.getLong("property_id"))
                .propertyName(rs.getString("property_name"))
                .roomId(rs.getLong("room_id"))
                .roomCode(rs.getString("room_code"))
                .roomStatus(RoomStatus.valueOf(rs.getString("room_status")))
                .profileId(getLongOrNull(rs, "profile_id"))
                .roomRole(rs.getString("room_role"))
                .roomOccupantCount(getIntOrNull(rs, "room_occupant_count"))
                .roomMaxOccupants(getIntOrNull(rs, "room_max_occupants"))
                .userId(userId)
                .fullName(rs.getString("full_name"))
                .phone(rs.getString("phone"))
                .email(rs.getString("email"))
                .recipientEmail(recipientEmail)
                .role(parseRole(rs.getString("role")))
                .accountStatus(parseAccountStatus(rs.getString("account_status")))
                .mustChangePassword(mustChangePassword)
                .lastLoginAt(toLocalDateTime(rs, "last_login_at"))
                .accountCreatedAt(toLocalDateTime(rs, "account_created_at"))
                .accountProvisioned(userId != null)
                .emailAvailable(!StringUtils.isEmpty(recipientEmail))
                .build();
    }

    private String resolveUserEmail(TenantAccountProvisioningResponse occupant) {
        if (!StringUtils.isEmpty(occupant.getEmail())) {
            return StringUtils.normalize(occupant.getEmail());
        }
        return "tenant-" + normalizePhone(occupant.getPhone()) + "@" + SYNTHETIC_TENANT_EMAIL_DOMAIN;
    }

    private String normalizePhone(String phone) {
        String normalized = phone == null ? "" : phone.replaceAll("\\D+", "");
        if (StringUtils.isEmpty(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số điện thoại khách thuê không hợp lệ.");
        }
        return normalized;
    }

    private String safeName(String value) {
        return StringUtils.isEmpty(value) ? "Khách thuê" : value.trim();
    }

    private Role parseRole(String value) {
        return StringUtils.isEmpty(value) ? null : Role.valueOf(value);
    }

    private AccountStatus parseAccountStatus(String value) {
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        if ("DISABLED".equals(value)) {
            return AccountStatus.INACTIVE;
        }
        return AccountStatus.valueOf(value);
    }

    private Long getLongOrNull(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private Integer getIntOrNull(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private Boolean getBooleanOrNull(ResultSet rs, String column) throws SQLException {
        boolean value = rs.getBoolean(column);
        return rs.wasNull() ? null : value;
    }

    private LocalDateTime toLocalDateTime(ResultSet rs, String column) throws SQLException {
        var timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private record ProvisionedAccount(Long userId, String temporaryPassword) {
    }
}
