package com.sep490.hdbhms.identityandaccess.application.service;

import com.sep490.hdbhms.identityandaccess.application.port.out.PersonProfileRepository;
import com.sep490.hdbhms.identityandaccess.application.port.out.SendPreCreatedAccountPort;
import com.sep490.hdbhms.identityandaccess.application.port.out.UserRepository;
import com.sep490.hdbhms.identityandaccess.domain.model.PersonProfile;
import com.sep490.hdbhms.identityandaccess.domain.model.User;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.AccountStatus;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.TenantAccountProvisioningStatus;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.TenantAccountProvisioningEntity;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaTenantAccountProvisioningRepository;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
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
    JpaTenantAccountProvisioningRepository provisioningRepository;
    PlatformTransactionManager transactionManager;

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

    public TenantAccountProvisioningResponse provisionPrimaryTenantAccount(
            Long contractId,
            boolean retryFailed
    ) {
        ContractProvisioningContext context = findContractContext(contractId);
        validateContractContext(context);

        List<TenantAccountProvisioningResponse> occupants = findContractOccupants(contractId);
        if (occupants.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Hợp đồng chưa có người thuê trong contract_occupants."
            );
        }
        TenantAccountProvisioningResponse primary = findPrimary(occupants);
        if (StringUtils.isEmpty(primary.getRecipientEmail())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "MISSING_EMAIL: Thiếu email người ký chính để nhận tài khoản."
            );
        }

        List<Long> claimedProfileIds = transactionTemplate().execute(status -> {
            List<Long> claimed = new ArrayList<>();
            for (TenantAccountProvisioningResponse occupant : occupants) {
                if (prepareProvisioningAttempt(occupant, contractId, retryFailed)) {
                    claimed.add(occupant.getProfileId());
                }
            }
            return claimed;
        });

        if (claimedProfileIds == null || claimedProfileIds.isEmpty()) {
            List<TenantAccountProvisioningResponse> current = findContractOccupants(contractId);
            return findPrimary(current).toBuilder()
                    .message(resolveNoSendMessage(current, retryFailed))
                    .build();
        }

        try {
            Integer sentCount = transactionTemplate().execute(status ->
                    createAccountsAndSend(contractId, claimedProfileIds, primary));
            log.info(
                    "Provisioned tenant accounts from lease contract. contractId={}, sentCount={}",
                    contractId,
                    sentCount == null ? 0 : sentCount
            );
        } catch (RuntimeException exception) {
            String failureReason = shortFailureReason(exception);
            transactionTemplate().executeWithoutResult(status ->
                    markProvisioningFailed(claimedProfileIds, contractId, failureReason));
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Gửi tài khoản thất bại. Có thể thử gửi lại sau khi xác nhận.",
                    exception
            );
        }

        return findPrimaryContractOccupant(contractId).toBuilder()
                .message("Đã gửi tài khoản cho các người thuê chưa được cấp.")
                .build();
    }

    private boolean prepareProvisioningAttempt(
            TenantAccountProvisioningResponse occupant,
            Long contractId,
            boolean retryFailed
    ) {
        Long profileId = occupant.getProfileId();
        if (profileId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Người thuê chưa có tenant_profile_id."
            );
        }

        TenantAccountProvisioningEntity provisioning =
                findOrCreateProvisioning(occupant, contractId);
        User existingUser = resolveExistingUser(occupant, profileId);
        if (existingUser != null) {
            prepareExistingUser(occupant, existingUser);
            syncExistingUserProvisioning(provisioning, existingUser, contractId);
            provisioningRepository.save(provisioning);
            return false;
        }

        if (provisioning.getStatus() == TenantAccountProvisioningStatus.FAILED && !retryFailed) {
            return false;
        }
        if (!List.of(
                TenantAccountProvisioningStatus.NOT_PROVISIONED,
                TenantAccountProvisioningStatus.FAILED
        ).contains(provisioning.getStatus())) {
            return false;
        }

        provisioning.setStatus(TenantAccountProvisioningStatus.PENDING);
        provisioning.setLatestContractId(contractId);
        provisioning.setRecipientEmail(occupant.getRecipientEmail());
        provisioning.setAttemptCount(Objects.requireNonNullElse(provisioning.getAttemptCount(), 0) + 1);
        provisioning.setLastAttemptAt(LocalDateTime.now());
        provisioning.setFailedAt(null);
        provisioning.setFailureReason(null);
        provisioningRepository.save(provisioning);
        return true;
    }

    private int createAccountsAndSend(
            Long contractId,
            List<Long> claimedProfileIds,
            TenantAccountProvisioningResponse primary
    ) {
        List<TenantAccountProvisioningResponse> occupants = findContractOccupants(contractId).stream()
                .filter(item -> claimedProfileIds.contains(item.getProfileId()))
                .toList();
        List<SendPreCreatedAccountPort.AccountCredential> credentials = new ArrayList<>();
        List<ProvisionedAccount> provisionedAccounts = new ArrayList<>();

        for (TenantAccountProvisioningResponse occupant : occupants) {
            if (StringUtils.isEmpty(occupant.getPhone())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Hồ sơ " + safeName(occupant.getFullName()) + " chưa có số điện thoại."
                );
            }

            User existingUser = resolveExistingUser(occupant, occupant.getProfileId());
            if (existingUser != null) {
                prepareExistingUser(occupant, existingUser);
                TenantAccountProvisioningEntity provisioning =
                        getProvisioningForUpdate(occupant.getProfileId());
                syncExistingUserProvisioning(provisioning, existingUser, contractId);
                provisioningRepository.save(provisioning);
                continue;
            }

            String temporaryPassword = RandomPasswordUtils.generatePassword(6, true, true);
            User savedUser = createUser(occupant, temporaryPassword);
            ensureTenantMembership(occupant.getPropertyId(), savedUser.getId());
            linkProfileIfNeeded(occupant.getProfileId(), savedUser.getId());
            provisionedAccounts.add(new ProvisionedAccount(
                    occupant.getProfileId(),
                    savedUser.getId()
            ));
            credentials.add(new SendPreCreatedAccountPort.AccountCredential(
                    occupant.getFullName(),
                    occupant.getPhone(),
                    temporaryPassword,
                    occupant.getRoomRole()
            ));
        }

        if (!credentials.isEmpty()) {
            sendPreCreatedAccountPort.sendAccountInformationBatch(
                    primary.getRecipientEmail(),
                    primary.getFullName(),
                    credentials
            );
        }

        LocalDateTime sentAt = LocalDateTime.now();
        for (ProvisionedAccount account : provisionedAccounts) {
            TenantAccountProvisioningEntity provisioning =
                    getProvisioningForUpdate(account.profileId());
            provisioning.setUserId(account.userId());
            provisioning.setLatestContractId(contractId);
            provisioning.setStatus(TenantAccountProvisioningStatus.SENT);
            provisioning.setSentAt(sentAt);
            provisioning.setFailedAt(null);
            provisioning.setFailureReason(null);
            provisioningRepository.save(provisioning);
        }
        return credentials.size();
    }

    private TenantAccountProvisioningEntity findOrCreateProvisioning(
            TenantAccountProvisioningResponse occupant,
            Long contractId
    ) {
        jdbcTemplate.update("""
                        INSERT IGNORE INTO tenant_account_provisionings (
                            tenant_profile_id,
                            user_id,
                            first_contract_id,
                            latest_contract_id,
                            status,
                            recipient_email,
                            attempt_count,
                            created_at,
                            updated_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, 0, NOW(6), NOW(6))
                        """,
                occupant.getProfileId(),
                occupant.getUserId(),
                contractId,
                contractId,
                initialStatus(occupant).name(),
                occupant.getRecipientEmail()
        );
        TenantAccountProvisioningEntity provisioning =
                getProvisioningForUpdate(occupant.getProfileId());
        if (provisioning.getFirstContractId() == null) {
            provisioning.setFirstContractId(contractId);
        }
        provisioning.setLatestContractId(contractId);
        if (StringUtils.isEmpty(provisioning.getRecipientEmail())) {
            provisioning.setRecipientEmail(occupant.getRecipientEmail());
        }
        return provisioning;
    }

    private TenantAccountProvisioningStatus initialStatus(
            TenantAccountProvisioningResponse occupant
    ) {
        if (occupant.getUserId() == null) {
            return TenantAccountProvisioningStatus.NOT_PROVISIONED;
        }
        return isActivated(occupant.getLastLoginAt(), occupant.getMustChangePassword())
                ? TenantAccountProvisioningStatus.ACTIVE
                : TenantAccountProvisioningStatus.SENT;
    }

    private void prepareExistingUser(
            TenantAccountProvisioningResponse occupant,
            User user
    ) {
        if (user.getRole() != Role.TENANT) {
            user.assignRole(Role.TENANT);
            userRepository.save(user);
        }
        ensureTenantMembership(occupant.getPropertyId(), user.getId());
        linkProfileIfNeeded(occupant.getProfileId(), user.getId());
    }

    private void syncExistingUserProvisioning(
            TenantAccountProvisioningEntity provisioning,
            User user,
            Long contractId
    ) {
        provisioning.setUserId(user.getId());
        provisioning.setLatestContractId(contractId);
        provisioning.setStatus(
                isActivated(user.getLastLoginAt(), user.isMustChangePassword())
                        ? TenantAccountProvisioningStatus.ACTIVE
                        : TenantAccountProvisioningStatus.SENT
        );
        provisioning.setFailedAt(null);
        provisioning.setFailureReason(null);
    }

    private User createUser(
            TenantAccountProvisioningResponse occupant,
            String temporaryPassword
    ) {
        String temporaryPasswordHash = passwordEncoder.encode(temporaryPassword);
        User user = User.newUser(
                occupant.getPhone(),
                resolveUserEmail(occupant),
                temporaryPasswordHash,
                Role.TENANT
        );
        user.activeAccount();
        return userRepository.save(user);
    }

    private User resolveExistingUser(
            TenantAccountProvisioningResponse occupant,
            Long profileId
    ) {
        Long linkedUserId = occupant.getUserId();
        if (linkedUserId == null && profileId != null) {
            linkedUserId = jdbcTemplate.query("""
                            SELECT user_id
                            FROM person_profiles
                            WHERE id = ?
                              AND deleted_at IS NULL
                            LIMIT 1
                            """,
                    rs -> rs.next() ? getLongOrNull(rs, "user_id") : null,
                    profileId
            );
        }
        if (linkedUserId != null) {
            return userRepository.findById(linkedUserId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Không tìm thấy tài khoản đã liên kết với hồ sơ."
                    ));
        }
        return userRepository.findByPhoneOrEmailAndDeletedAtIsNull(
                        occupant.getPhone(),
                        resolveUserEmail(occupant)
                )
                .orElse(null);
    }

    private void ensureTenantMembership(Long propertyId, Long userId) {
        tenantRepository.findByUserIdAndPropertyId(userId, propertyId)
                .orElseGet(() -> tenantRepository.save(Tenant.newTenant(propertyId, userId)));
    }

    private void linkProfileIfNeeded(Long profileId, Long userId) {
        PersonProfile profile = personProfileRepository.findById(profileId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy hồ sơ khách thuê."
                ));
        if (userId.equals(profile.getUserId())) {
            return;
        }
        if (profile.getUserId() != null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Hồ sơ khách thuê đã liên kết với tài khoản khác."
            );
        }
        profile.linkUser(userId);
        personProfileRepository.save(profile);
    }

    private void markProvisioningFailed(
            List<Long> profileIds,
            Long contractId,
            String failureReason
    ) {
        LocalDateTime failedAt = LocalDateTime.now();
        for (Long profileId : profileIds) {
            TenantAccountProvisioningEntity provisioning =
                    getProvisioningForUpdate(profileId);
            if (provisioning.getStatus() != TenantAccountProvisioningStatus.PENDING) {
                continue;
            }
            provisioning.setLatestContractId(contractId);
            provisioning.setStatus(TenantAccountProvisioningStatus.FAILED);
            provisioning.setFailedAt(failedAt);
            provisioning.setFailureReason(failureReason);
            provisioningRepository.save(provisioning);
        }
    }

    private TenantAccountProvisioningEntity getProvisioningForUpdate(Long profileId) {
        return provisioningRepository.findByTenantProfileIdForUpdate(profileId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Không tạo được trạng thái cấp tài khoản."
                ));
    }

    private ContractProvisioningContext findContractContext(Long contractId) {
        try {
            return jdbcTemplate.queryForObject("""
                            SELECT
                                lc.status AS contract_status,
                                r.current_status AS room_status,
                                (
                                    SELECT COUNT(*)
                                    FROM contract_occupants co
                                    WHERE co.contract_id = lc.id
                                      AND co.status = 'ACTIVE'
                                      AND co.tenant_profile_id IS NOT NULL
                                ) AS occupant_count
                            FROM lease_contracts lc
                            JOIN rooms r ON r.id = lc.room_id
                            WHERE lc.id = ?
                              AND lc.deleted_at IS NULL
                            """,
                    (rs, rowNum) -> new ContractProvisioningContext(
                            LeaseStatus.valueOf(rs.getString("contract_status")),
                            RoomStatus.valueOf(rs.getString("room_status")),
                            rs.getInt("occupant_count")
                    ),
                    contractId
            );
        } catch (EmptyResultDataAccessException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hợp đồng thuê.");
        }
    }

    private void validateContractContext(ContractProvisioningContext context) {
        if (context.contractStatus() != LeaseStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Chỉ được gửi tài khoản khi hợp đồng đang ACTIVE."
            );
        }
        if (context.roomStatus() != RoomStatus.OCCUPIED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Phòng phải ở trạng thái OCCUPIED trước khi gửi tài khoản."
            );
        }
        if (context.occupantCount() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Hợp đồng chưa có người thuê trong contract_occupants."
            );
        }
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
        List<TenantAccountProvisioningResponse> occupants = findContractOccupants(contractId);
        if (occupants.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hợp đồng thuê.");
        }
        return findPrimary(occupants);
    }

    private TenantAccountProvisioningResponse findPrimary(
            List<TenantAccountProvisioningResponse> occupants
    ) {
        return occupants.stream()
                .filter(item -> "PRIMARY".equalsIgnoreCase(item.getRoomRole()))
                .findFirst()
                .orElse(occupants.getFirst());
    }

    private String baseCandidateSql() {
        return """
                SELECT
                    CASE WHEN co.occupant_role = 'PRIMARY' THEN 0 ELSE 1 END AS role_order,
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
                    co.id AS occupant_id,
                    pp.id AS profile_id,
                    pp.full_name,
                    pp.phone,
                    pp.email,
                    COALESCE(primary_pp.email, primary_user.email) AS recipient_email,
                    co.occupant_role AS room_role,
                    GREATEST((
                        SELECT COUNT(*)
                        FROM contract_occupants co_count
                        WHERE co_count.contract_id = lc.id
                          AND co_count.status = 'ACTIVE'
                    ), 1) AS room_occupant_count,
                    r.max_occupants AS room_max_occupants,
                    u.id AS user_id,
                    u.role,
                    u.status AS account_status,
                    u.must_change_password,
                    u.last_login_at,
                    u.created_at AS account_created_at,
                    CASE
                        WHEN u.id IS NOT NULL
                          AND (u.last_login_at IS NOT NULL OR u.must_change_password = FALSE)
                            THEN 'ACTIVE'
                        ELSE tap.status
                    END AS provisioning_status,
                    tap.sent_at,
                    tap.failed_at,
                    tap.failure_reason,
                    tap.attempt_count,
                    tap.last_attempt_at,
                    CASE
                        WHEN NOT EXISTS (
                            SELECT 1
                            FROM identity_documents idoc
                            WHERE idoc.profile_id = pp.id
                              AND idoc.status = 'ACTIVE'
                              AND idoc.doc_number IS NOT NULL
                              AND idoc.doc_number <> ''
                              AND idoc.doc_number NOT LIKE 'PENDING-%'
                        ) THEN 'MISSING_IDENTITY'
                        WHEN pp.portrait_file_id IS NULL THEN 'MISSING_PORTRAIT'
                        WHEN NOT EXISTS (
                            SELECT 1
                            FROM emergency_contacts ec
                            WHERE ec.tenant_profile_id = pp.id
                        ) THEN 'MISSING_EMERGENCY_CONTACT'
                        ELSE 'COMPLETED'
                    END AS profile_status,
                    NOT EXISTS (
                        SELECT 1
                        FROM identity_documents idoc
                        WHERE idoc.profile_id = pp.id
                          AND idoc.status = 'ACTIVE'
                          AND idoc.doc_number IS NOT NULL
                          AND idoc.doc_number <> ''
                          AND idoc.doc_number NOT LIKE 'PENDING-%'
                    ) AS missing_identity,
                    pp.portrait_file_id IS NULL AS missing_portrait,
                    NOT EXISTS (
                        SELECT 1
                        FROM emergency_contacts ec
                        WHERE ec.tenant_profile_id = pp.id
                    ) AS missing_emergency_contact
                FROM lease_contracts lc
                JOIN rooms r ON r.id = lc.room_id
                JOIN properties p ON p.id = r.property_id
                JOIN person_profiles primary_pp ON primary_pp.id = lc.primary_tenant_profile_id
                JOIN (
                    SELECT
                        active_occupant.id,
                        active_occupant.contract_id,
                        active_occupant.tenant_profile_id,
                        active_occupant.occupant_role
                    FROM contract_occupants active_occupant
                    WHERE active_occupant.status = 'ACTIVE'
                      AND active_occupant.tenant_profile_id IS NOT NULL

                    UNION ALL

                    SELECT
                        NULL AS id,
                        fallback_contract.id AS contract_id,
                        fallback_contract.primary_tenant_profile_id AS tenant_profile_id,
                        'PRIMARY' AS occupant_role
                    FROM lease_contracts fallback_contract
                    WHERE fallback_contract.deleted_at IS NULL
                      AND NOT EXISTS (
                          SELECT 1
                          FROM contract_occupants primary_occupant
                          WHERE primary_occupant.contract_id = fallback_contract.id
                            AND primary_occupant.tenant_profile_id =
                                fallback_contract.primary_tenant_profile_id
                            AND primary_occupant.status = 'ACTIVE'
                      )
                ) co ON co.contract_id = lc.id
                JOIN person_profiles pp
                    ON pp.id = co.tenant_profile_id
                    AND pp.deleted_at IS NULL
                LEFT JOIN users primary_user
                    ON primary_user.id = primary_pp.user_id
                    AND primary_user.deleted_at IS NULL
                LEFT JOIN users u ON u.id = pp.user_id AND u.deleted_at IS NULL
                LEFT JOIN tenant_account_provisionings tap ON tap.tenant_profile_id = pp.id
                WHERE lc.deleted_at IS NULL
                  AND lc.status IN ('ACTIVE', 'EXPIRING_SOON', 'TERMINATION_PENDING')
                """;
    }

    private TenantAccountProvisioningResponse toResponse(ResultSet rs) throws SQLException {
        Long userId = getLongOrNull(rs, "user_id");
        Boolean mustChangePassword = getBooleanOrNull(rs, "must_change_password");
        String recipientEmail = rs.getString("recipient_email");
        TenantAccountProvisioningStatus provisioningStatus =
                parseProvisioningStatus(rs.getString("provisioning_status"));
        if (provisioningStatus == null) {
            provisioningStatus = userId == null
                    ? TenantAccountProvisioningStatus.NOT_PROVISIONED
                    : isActivated(toLocalDateTime(rs, "last_login_at"), mustChangePassword)
                    ? TenantAccountProvisioningStatus.ACTIVE
                    : TenantAccountProvisioningStatus.SENT;
        }

        return TenantAccountProvisioningResponse.builder()
                .contractId(rs.getLong("contract_id"))
                .contractCode(rs.getString("contract_code"))
                .contractStatus(LeaseStatus.valueOf(rs.getString("contract_status")))
                .startDate(toLocalDate(rs, "start_date"))
                .endDate(toLocalDate(rs, "end_date"))
                .signedAt(toLocalDateTime(rs, "signed_at"))
                .propertyId(rs.getLong("property_id"))
                .propertyName(rs.getString("property_name"))
                .roomId(rs.getLong("room_id"))
                .roomCode(rs.getString("room_code"))
                .roomStatus(RoomStatus.valueOf(rs.getString("room_status")))
                .occupantId(getLongOrNull(rs, "occupant_id"))
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
                .accountProvisioned(List.of(
                        TenantAccountProvisioningStatus.SENT,
                        TenantAccountProvisioningStatus.ACTIVE
                ).contains(provisioningStatus))
                .emailAvailable(!StringUtils.isEmpty(recipientEmail))
                .provisioningStatus(provisioningStatus)
                .sentAt(toLocalDateTime(rs, "sent_at"))
                .failedAt(toLocalDateTime(rs, "failed_at"))
                .failureReason(rs.getString("failure_reason"))
                .attemptCount(getIntOrNull(rs, "attempt_count"))
                .lastAttemptAt(toLocalDateTime(rs, "last_attempt_at"))
                .profileStatus(rs.getString("profile_status"))
                .missingIdentity(rs.getBoolean("missing_identity"))
                .missingPortrait(rs.getBoolean("missing_portrait"))
                .missingEmergencyContact(rs.getBoolean("missing_emergency_contact"))
                .build();
    }

    private String resolveNoSendMessage(
            List<TenantAccountProvisioningResponse> occupants,
            boolean retryFailed
    ) {
        if (occupants.stream().anyMatch(item ->
                item.getProvisioningStatus() == TenantAccountProvisioningStatus.FAILED)
                && !retryFailed) {
            return "Có người thuê gửi tài khoản thất bại. Vui lòng xác nhận thử gửi lại.";
        }
        if (occupants.stream().anyMatch(item ->
                item.getProvisioningStatus() == TenantAccountProvisioningStatus.PENDING)) {
            return "Tài khoản đang được gửi, không gửi lặp lại.";
        }
        return "Tất cả người thuê đã được cấp tài khoản, không gửi lại.";
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
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Số điện thoại khách thuê không hợp lệ."
            );
        }
        return normalized;
    }

    private String safeName(String value) {
        return StringUtils.isEmpty(value) ? "Khách thuê" : value.trim();
    }

    private String shortFailureReason(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        String normalized = message.replaceAll("[\\r\\n\\t]+", " ").trim();
        return normalized.substring(0, Math.min(normalized.length(), 500));
    }

    private boolean isActivated(LocalDateTime lastLoginAt, Boolean mustChangePassword) {
        return lastLoginAt != null || Boolean.FALSE.equals(mustChangePassword);
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

    private TenantAccountProvisioningStatus parseProvisioningStatus(String value) {
        return StringUtils.isEmpty(value)
                ? null
                : TenantAccountProvisioningStatus.valueOf(value);
    }

    private TransactionTemplate transactionTemplate() {
        return new TransactionTemplate(transactionManager);
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

    private java.time.LocalDate toLocalDate(ResultSet rs, String column) throws SQLException {
        var date = rs.getDate(column);
        return date == null ? null : date.toLocalDate();
    }

    private LocalDateTime toLocalDateTime(ResultSet rs, String column) throws SQLException {
        var timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private record ProvisionedAccount(Long profileId, Long userId) {
    }

    private record ContractProvisioningContext(
            LeaseStatus contractStatus,
            RoomStatus roomStatus,
            int occupantCount
    ) {
    }
}
