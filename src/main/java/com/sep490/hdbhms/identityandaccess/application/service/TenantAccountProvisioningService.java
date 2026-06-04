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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TenantAccountProvisioningService {
    JdbcTemplate jdbcTemplate;
    UserRepository userRepository;
    TenantRepository tenantRepository;
    PasswordEncoder passwordEncoder;
    PersonProfileRepository personProfileRepository;
    SendPreCreatedAccountPort sendPreCreatedAccountPort;

    public List<TenantAccountProvisioningResponse> findProvisioningCandidates() {
        return jdbcTemplate.query("""
                        SELECT
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
                        LEFT JOIN users u ON u.id = pp.user_id AND u.deleted_at IS NULL
                        WHERE lc.deleted_at IS NULL
                          AND lc.status = 'ACTIVE'
                        ORDER BY lc.signed_at DESC, lc.id DESC
                        """,
                (rs, rowNum) -> toResponse(rs)
        );
    }

    public TenantAccountProvisioningResponse provisionPrimaryTenantAccount(Long contractId) {
        TenantAccountProvisioningResponse contract = findContract(contractId);
        if (contract.getContractStatus() != LeaseStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hop dong phai o trang thai ACTIVE truoc khi gui tai khoan.");
        }
        if (StringUtils.isEmpty(contract.getPhone())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ho so nguoi ky chinh chua co so dien thoai.");
        }
        if (StringUtils.isEmpty(contract.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ho so nguoi ky chinh chua co email de gui tai khoan.");
        }
        if (contract.getUserId() != null
                && (contract.getLastLoginAt() != null || Boolean.FALSE.equals(contract.getMustChangePassword()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tai khoan khach thue da kich hoat, khong gui lai mat khau tam.");
        }

        String temporaryPassword = RandomPasswordUtils.generatePassword(6, true, true);
        User user = resolveOrCreateUser(contract, temporaryPassword);
        ensureTenantMembership(contract.getPropertyId(), user.getId());
        linkProfileIfNeeded(contract.getProfileId(), user.getId());

        sendPreCreatedAccountPort.sendAccountInformation(
                contract.getEmail(),
                contract.getFullName(),
                contract.getPhone(),
                temporaryPassword
        );

        log.info("Provisioned tenant account from lease contract. contractId={}, userId={}", contractId, user.getId());
        return findContract(contractId).toBuilder()
                .message("Da gui tai khoan va mat khau tam thoi den email khach thue.")
                .build();
    }

    private User resolveOrCreateUser(TenantAccountProvisioningResponse contract, String temporaryPassword) {
        User user = null;
        if (contract.getUserId() != null) {
            user = userRepository.findById(contract.getUserId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay tai khoan da lien ket voi ho so."));
        }
        if (user == null) {
            user = userRepository.findByPhoneOrEmailAndDeletedAtIsNull(contract.getPhone(), contract.getEmail())
                    .orElse(null);
        }

        String temporaryPasswordHash = passwordEncoder.encode(temporaryPassword);
        if (user == null) {
            user = User.newUser(contract.getPhone(), contract.getEmail(), temporaryPasswordHash, Role.TENANT);
            user.activeAccount();
        } else {
            if (user.getRole() != Role.TENANT) {
                user.assignRole(Role.TENANT);
            }
            user.issueTemporaryPassword(temporaryPasswordHash);
        }
        return userRepository.save(user);
    }

    private void ensureTenantMembership(Long propertyId, Long userId) {
        tenantRepository.findByUserIdAndPropertyId(userId, propertyId)
                .orElseGet(() -> tenantRepository.save(Tenant.newTenant(propertyId, userId)));
    }

    private void linkProfileIfNeeded(Long profileId, Long userId) {
        PersonProfile profile = personProfileRepository.findById(profileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay ho so nguoi ky chinh."));
        if (userId.equals(profile.getUserId())) {
            return;
        }
        profile.linkUser(userId);
        personProfileRepository.save(profile);
    }

    private TenantAccountProvisioningResponse findContract(Long contractId) {
        try {
            return jdbcTemplate.queryForObject("""
                            SELECT
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
                            LEFT JOIN users u ON u.id = pp.user_id AND u.deleted_at IS NULL
                            WHERE lc.deleted_at IS NULL
                              AND lc.id = ?
                            """,
                    (rs, rowNum) -> toResponse(rs),
                    contractId
            );
        } catch (EmptyResultDataAccessException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay hop dong thue.");
        }
    }

    private TenantAccountProvisioningResponse toResponse(ResultSet rs) throws SQLException {
        Long userId = getLongOrNull(rs, "user_id");
        Boolean mustChangePassword = getBooleanOrNull(rs, "must_change_password");
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
                .profileId(rs.getLong("profile_id"))
                .userId(userId)
                .fullName(rs.getString("full_name"))
                .phone(rs.getString("phone"))
                .email(rs.getString("email"))
                .role(rs.getString("role") != null ? Role.valueOf(rs.getString("role")) : null)
                .accountStatus(rs.getString("account_status") != null ? AccountStatus.valueOf(rs.getString("account_status")) : null)
                .mustChangePassword(mustChangePassword)
                .lastLoginAt(rs.getTimestamp("last_login_at") != null ? rs.getTimestamp("last_login_at").toLocalDateTime() : null)
                .accountCreatedAt(rs.getTimestamp("account_created_at") != null ? rs.getTimestamp("account_created_at").toLocalDateTime() : null)
                .accountProvisioned(userId != null)
                .emailAvailable(!StringUtils.isEmpty(rs.getString("email")))
                .build();
    }

    private Long getLongOrNull(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private Boolean getBooleanOrNull(ResultSet rs, String column) throws SQLException {
        boolean value = rs.getBoolean(column);
        return rs.wasNull() ? null : value;
    }
}
