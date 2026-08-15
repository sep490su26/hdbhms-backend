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
import com.sep490.hdbhms.occupancy.domain.value_objects.OccupantStatus;
import com.sep490.hdbhms.property.domain.value_objects.RoomStatus;
import com.sep490.hdbhms.shared.utils.AuthUtils;
import com.sep490.hdbhms.shared.types.dto.response.PageResponse;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import com.sep490.hdbhms.shared.utils.RandomPasswordUtils;
import com.sep490.hdbhms.shared.utils.StringUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

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

    @Transactional(readOnly = true)
    public PageResponse<TenantAccountProvisioningResponse> findProvisioningCandidates(Pageable pageable) {
        List<TenantAccountProvisioningResponse> rows = findProvisioningCandidates();
        List<TenantAccountProvisioningResponse> pageRows = rows.stream()
                .skip(pageable.getOffset())
                .limit(pageable.getPageSize())
                .toList();
        return PageResponse.fromPageToPageResponse(new PageImpl<>(pageRows, pageable, rows.size()));
    }

    public TenantAccountProvisioningResponse provisionPrimaryTenantAccount(
            Long contractId,
            boolean retryFailed
    ) {
        ContractProvisioningContext context = findContractContext(contractId);
        validateContractContext(context);

        List<TenantAccountProvisioningResponse> occupants = findContractOccupants(contractId);
        if (occupants.isEmpty()) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        List<TenantAccountProvisioningResponse> eligibleOccupants = occupants.stream()
                .filter(this::isProvisioningEligibleContext)
                .toList();
        if (eligibleOccupants.isEmpty()) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        List<Long> claimedProfileIds = transactionTemplate().execute(status -> {
            List<Long> claimed = new ArrayList<>();
            for (TenantAccountProvisioningResponse occupant : eligibleOccupants) {
                PreparationOutcome outcome =
                        prepareProvisioningAttempt(occupant, contractId, retryFailed);
                if (outcome == PreparationOutcome.CLAIMED) {
                    claimed.add(occupant.getProfileId());
                }
            }
            return claimed;
        });

        List<Long> preparedProfileIds = claimedProfileIds == null ? List.of() : claimedProfileIds;
        if (preparedProfileIds.isEmpty()) {
            List<TenantAccountProvisioningResponse> current = findContractOccupants(contractId);
            return findPrimary(current).toBuilder()
                    .message(resolveNoSendMessage(current, retryFailed))
                    .build();
        }
        try {
            Integer sentCount = transactionTemplate().execute(status ->
                    createAccountsAndSend(contractId, preparedProfileIds));
            log.info(
                    "Tenant accounts provisioned from lease contract. contractId={}, sentCount={}",
                    contractId,
                    sentCount == null ? 0 : sentCount
            );
        } catch (RuntimeException exception) {
            String failureReason = shortFailureReason(exception);
            transactionTemplate().executeWithoutResult(status ->
                    markProvisioningFailed(preparedProfileIds, contractId, failureReason));
            throw new AppException(ApiErrorCode.EXTERNAL_SERVICE_ERROR, exception);
        }

        List<TenantAccountProvisioningResponse> current = findContractOccupants(contractId);
        return findPrimary(current).toBuilder()
                .message(buildSendMessage(preparedProfileIds.size(), current))
                .build();
    }

    public TenantAccountProvisioningResponse provisionTenantAccount(
            Long contractId,
            Long tenantProfileId,
            boolean retryFailed
    ) {
        ContractProvisioningContext context = findContractContext(contractId);
        validateContractContext(context);

        TenantAccountProvisioningResponse occupant = findContractOccupant(contractId, tenantProfileId);
        if (!isProvisioningEligibleContext(occupant)) {
            return occupant.toBuilder()
                    .message("Quyền truy cập của người thuê đã bị vô hiệu hóa")
                    .build();
        }

        PreparationOutcome outcome = transactionTemplate().execute(status ->
                prepareProvisioningAttempt(occupant, contractId, retryFailed));
        if (outcome != PreparationOutcome.CLAIMED) {
            TenantAccountProvisioningResponse current = findContractOccupant(contractId, tenantProfileId);
            return current.toBuilder()
                    .message(resolveNoSendMessage(List.of(current), retryFailed))
                    .build();
        }

        try {
            Integer sentCount = transactionTemplate().execute(status ->
                    createAccountsAndSend(contractId, List.of(tenantProfileId)));
            TenantAccountProvisioningResponse current = findContractOccupant(contractId, tenantProfileId);
            return current.toBuilder()
                    .message(buildSendMessage(sentCount == null ? 0 : sentCount, List.of(current)))
                    .build();
        } catch (RuntimeException exception) {
            String failureReason = shortFailureReason(exception);
            transactionTemplate().executeWithoutResult(status ->
                    markProvisioningFailed(List.of(tenantProfileId), contractId, failureReason));
            throw new AppException(ApiErrorCode.EXTERNAL_SERVICE_ERROR, exception);
        }
    }

    public TenantAccountProvisioningResponse disableTenantContext(
            Long contractId,
            Long tenantProfileId,
            String reason
    ) {
        String normalizedReason = validateDisableReason(reason);
        Long disabledBy = AuthUtils.getCurrentAuthenticationId();
        if (disabledBy == null) {
            throw new AppException(ApiErrorCode.UNAUTHENTICATED);
        }

        ContractMembershipContext context = findContractMembershipContext(contractId, tenantProfileId);
        if (!context.hasOccupant() && !Objects.equals(context.primaryTenantProfileId(), tenantProfileId)) {
            throw new AppException(ApiErrorCode.RESOURCE_NOT_FOUND);
        }

        disableTenantContextStatus(contractId, tenantProfileId, normalizedReason, disabledBy, context);
        return findContractOccupant(contractId, tenantProfileId).toBuilder()
                .message("Vô hiệu hóa quyền truy cập của người thuê thành công")
                .build();
    }

    private String validateDisableReason(String reason) {
        String normalized = reason == null ? new String() : reason.trim();
        if (normalized.isEmpty()) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        if (normalized.length() > 1000) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        return normalized;
    }

    private boolean isProvisioningEligibleContext(TenantAccountProvisioningResponse occupant) {
        return occupant.getOccupantStatus() != OccupantStatus.DISABLED
                && occupant.getProvisioningStatus() != TenantAccountProvisioningStatus.DISABLED;
    }

    private void disableTenantContextStatus(
            Long contractId,
            Long tenantProfileId,
            String reason,
            Long disabledBy,
            ContractMembershipContext context
    ) {
        transactionTemplate().executeWithoutResult(status -> {
            if (context.hasOccupant()) {
                disableExistingTenantContext(contractId, tenantProfileId, reason, disabledBy);
            } else {
                insertDisabledPrimaryTenantContext(contractId, tenantProfileId, reason, disabledBy, context);
            }
        });
    }

    private void disableExistingTenantContext(
            Long contractId,
            Long tenantProfileId,
            String reason,
            Long disabledBy
    ) {
        jdbcTemplate.update(
                "UPDATE contract_occupants SET status = 'DISABLED', disabled_reason = ?, disabled_by = ?, disabled_at = NOW(6) WHERE contract_id = ? AND tenant_profile_id = ?",
                reason,
                disabledBy,
                contractId,
                tenantProfileId
        );
    }

    private void insertDisabledPrimaryTenantContext(
            Long contractId,
            Long tenantProfileId,
            String reason,
            Long disabledBy,
            ContractMembershipContext context
    ) {
        jdbcTemplate.update(
                "INSERT INTO contract_occupants (contract_id, tenant_id, tenant_profile_id, occupant_role, move_in_date, status, disabled_reason, disabled_by, disabled_at, created_at) VALUES (?, ?, ?, 'PRIMARY', ?, 'DISABLED', ?, ?, NOW(6), NOW(6))",
                contractId,
                resolveTenantIdForProfile(tenantProfileId, context.propertyId()),
                tenantProfileId,
                context.startDate() != null ? context.startDate() : java.time.LocalDate.now(),
                reason,
                disabledBy
        );
    }

    private PreparationOutcome prepareProvisioningAttempt(
            TenantAccountProvisioningResponse occupant,
            Long contractId,
            boolean retryFailed
    ) {
        Long profileId = occupant.getProfileId();
        if (profileId == null) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }

        if (!isProvisioningEligibleContext(occupant)) {
            return PreparationOutcome.SKIPPED;
        }

        TenantAccountProvisioningEntity provisioning =
                findOrCreateProvisioning(occupant, contractId);
        User existingUser = resolveExistingUser(occupant, profileId);
        if (existingUser != null) {
            prepareExistingUser(occupant, existingUser);
            if (isActivated(existingUser.getLastLoginAt(), existingUser.isMustChangePassword())) {
                syncExistingUserProvisioning(provisioning, existingUser, contractId);
                provisioningRepository.save(provisioning);
                return PreparationOutcome.SKIPPED;
            }
            if (provisioning.getStatus() == TenantAccountProvisioningStatus.FAILED && !retryFailed) {
                return PreparationOutcome.SKIPPED;
            }
            if (provisioning.getStatus() == TenantAccountProvisioningStatus.SENT && !retryFailed) {
                return PreparationOutcome.SKIPPED;
            }
            if (!List.of(
                    TenantAccountProvisioningStatus.NOT_PROVISIONED,
                    TenantAccountProvisioningStatus.PENDING,
                    TenantAccountProvisioningStatus.FAILED,
                    TenantAccountProvisioningStatus.SENT
            ).contains(provisioning.getStatus())) {
                return PreparationOutcome.SKIPPED;
            }
            claimProvisioningAttempt(provisioning, existingUser.getId(), contractId, occupant.getRecipientEmail());
            return PreparationOutcome.CLAIMED;
        }

        if (provisioning.getStatus() == TenantAccountProvisioningStatus.FAILED && !retryFailed) {
            return PreparationOutcome.SKIPPED;
        }
        if (!List.of(
                TenantAccountProvisioningStatus.NOT_PROVISIONED,
                TenantAccountProvisioningStatus.PENDING,
                TenantAccountProvisioningStatus.FAILED
        ).contains(provisioning.getStatus())) {
            return PreparationOutcome.SKIPPED;
        }

        claimProvisioningAttempt(provisioning, null, contractId, occupant.getRecipientEmail());
        return PreparationOutcome.CLAIMED;
    }

    private int createAccountsAndSend(
            Long contractId,
            List<Long> claimedProfileIds
    ) {
        List<TenantAccountProvisioningResponse> occupants = findContractOccupants(contractId).stream()
                .filter(item -> claimedProfileIds.contains(item.getProfileId()))
                .toList();
        List<SendPreCreatedAccountPort.AccountCredential> credentials = new ArrayList<>();
        List<ProvisionedAccount> provisionedAccounts = new ArrayList<>();

        for (TenantAccountProvisioningResponse occupant : occupants) {
            if (StringUtils.isEmpty(occupant.getPhone())) {
                throw new AppException(ApiErrorCode.INVALID_REQUEST);
            }

            User existingUser = resolveExistingUser(occupant, occupant.getProfileId());
            if (existingUser != null) {
                prepareExistingUser(occupant, existingUser);
                if (!isActivated(existingUser.getLastLoginAt(), existingUser.isMustChangePassword())) {
                    String temporaryPassword = RandomPasswordUtils.generatePassword(8, false, true);
                    existingUser.issueTemporaryPassword(passwordEncoder.encode(temporaryPassword));
                    User savedUser = userRepository.save(existingUser);
                    ensureTenantMembership(occupant.getPropertyId(), savedUser.getId());
                    linkProfileIfNeeded(occupant.getProfileId(), savedUser.getId());
                    provisionedAccounts.add(new ProvisionedAccount(
                            occupant.getProfileId(),
                            savedUser.getId()
                    ));
                    credentials.add(new SendPreCreatedAccountPort.AccountCredential(
                            occupant.getProfileId(),
                            occupant.getFullName(),
                            occupant.getPhone(),
                            temporaryPassword,
                            occupant.getRoomRole()
                    ));
                    continue;
                }
                TenantAccountProvisioningEntity provisioning =
                        getProvisioningForUpdate(occupant.getProfileId());
                syncExistingUserProvisioning(provisioning, existingUser, contractId);
                provisioningRepository.save(provisioning);
                continue;
            }

            String temporaryPassword = RandomPasswordUtils.generatePassword(8, false, true);
            User savedUser = createUser(occupant, temporaryPassword);
            ensureTenantMembership(occupant.getPropertyId(), savedUser.getId());
            linkProfileIfNeeded(occupant.getProfileId(), savedUser.getId());
            provisionedAccounts.add(new ProvisionedAccount(
                    occupant.getProfileId(),
                    savedUser.getId()
            ));
            credentials.add(new SendPreCreatedAccountPort.AccountCredential(
                    occupant.getProfileId(),
                    occupant.getFullName(),
                    occupant.getPhone(),
                    temporaryPassword,
                    occupant.getRoomRole()
            ));
        }
        if (!credentials.isEmpty()) {
            log.info(
                    "Sending tenant account notifications. contractId={}, accountCount={}",
                    contractId,
                    credentials.size()
            );
            for (SendPreCreatedAccountPort.AccountCredential credential : credentials) {
                TenantAccountProvisioningResponse recipient = findRecipient(occupants, credential.tenantProfileId());
                Long recipientUserId = provisionedAccounts.stream()
                        .filter(account -> Objects.equals(account.profileId(), recipient.getProfileId()))
                        .map(ProvisionedAccount::userId)
                        .findFirst()
                        .orElseGet(() -> {
                            User existingUser = resolveExistingUser(recipient, recipient.getProfileId());
                            return existingUser == null ? null : existingUser.getId();
                        });

                sendPreCreatedAccountPort.sendAccountInformation(
                        contractId,
                        recipient.getProfileId(),
                        recipientUserId,
                        recipient.getEmail(),
                        recipient.getFullName(),
                        recipient.getPhone(),
                        credential.randomPassword()
                );
            }
            log.info(
                    "Notification sender accepted tenant account notifications. contractId={}, accountCount={}",
                    contractId,
                    credentials.size()
            );
        }

        LocalDateTime sentAt = credentials.isEmpty() ? LocalDateTime.now() : null;
        for (ProvisionedAccount account : provisionedAccounts) {
            TenantAccountProvisioningEntity provisioning =
                    getProvisioningForUpdate(account.profileId());
            provisioning.setUserId(account.userId());
            provisioning.setLatestContractId(contractId);
            provisioning.setStatus(
                    credentials.isEmpty()
                            ? TenantAccountProvisioningStatus.SENT
                            : TenantAccountProvisioningStatus.PENDING
            );
            provisioning.setSentAt(sentAt);
            provisioning.setFailedAt(null);
            provisioning.setFailureReason(null);
            provisioningRepository.save(provisioning);
        }
        return credentials.size();
    }

    private TenantAccountProvisioningResponse findRecipient(
            List<TenantAccountProvisioningResponse> occupants,
            Long profileId
    ) {
        return occupants.stream()
                .filter(item -> Objects.equals(item.getProfileId(), profileId))
                .findFirst()
                .orElseThrow(() -> new AppException(ApiErrorCode.INVALID_REQUEST));
    }

    private void claimProvisioningAttempt(
            TenantAccountProvisioningEntity provisioning,
            Long userId,
            Long contractId,
            String recipientEmail
    ) {
        if (userId != null) {
            provisioning.setUserId(userId);
        }
        provisioning.setStatus(TenantAccountProvisioningStatus.PENDING);
        provisioning.setLatestContractId(contractId);
        provisioning.setRecipientEmail(recipientEmail);
        provisioning.setAttemptCount(Objects.requireNonNullElse(provisioning.getAttemptCount(), 0) + 1);
        provisioning.setLastAttemptAt(LocalDateTime.now());
        provisioning.setFailedAt(null);
        provisioning.setFailureReason(null);
        provisioningRepository.save(provisioning);
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
                : TenantAccountProvisioningStatus.NOT_PROVISIONED;
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

    private boolean syncExistingUserProvisioning(
            TenantAccountProvisioningEntity provisioning,
            User user,
            Long contractId
    ) {
        provisioning.setUserId(user.getId());
        provisioning.setLatestContractId(contractId);
        if (isActivated(user.getLastLoginAt(), user.isMustChangePassword())) {
            provisioning.setStatus(TenantAccountProvisioningStatus.ACTIVE);
            provisioning.setFailedAt(null);
            provisioning.setFailureReason(null);
            return true;
        }
        if (provisioning.getStatus() == TenantAccountProvisioningStatus.PENDING) {
            return true;
        }
        if (provisioning.getStatus() == TenantAccountProvisioningStatus.SENT
                && provisioning.getSentAt() != null) {
            provisioning.setFailedAt(null);
            provisioning.setFailureReason(null);
            return true;
        }
        provisioning.setStatus(TenantAccountProvisioningStatus.NOT_PROVISIONED);
        provisioning.setSentAt(null);
        provisioning.setFailedAt(null);
        provisioning.setFailureReason(null);
        return false;
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
                            WHERE person_profile_id = ?
                              AND deleted_at IS NULL
                            LIMIT 1
                            """,
                    rs -> rs.next() ? getLongOrNull(rs, "user_id") : null,
                    profileId
            );
        }
        if (linkedUserId != null) {
            return userRepository.findById(linkedUserId)
                    .orElseThrow(() -> new AppException(ApiErrorCode.RESOURCE_NOT_FOUND));
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
                .orElseThrow(() -> new AppException(ApiErrorCode.RESOURCE_NOT_FOUND));
        if (userId.equals(profile.getUserId())) {
            return;
        }
        if (profile.getUserId() != null) {
            throw new AppException(ApiErrorCode.OPERATION_CONFLICT);
        }
        PersonProfile existingUserProfile = personProfileRepository.findByUserId(userId)
                .orElse(null);
        if (existingUserProfile != null
                && !Objects.equals(existingUserProfile.getId(), profileId)) {
            log.info(
                    "Skipping duplicate user profile link during tenant account provisioning. "
                            + "profileId={}, existingProfileId={}, userId={}",
                    profileId,
                    existingUserProfile.getId(),
                    userId
            );
            return;
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
                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));
    }

    private ContractMembershipContext findContractMembershipContext(Long contractId, Long tenantProfileId) {
        List<ContractMembershipContext> contexts = jdbcTemplate.query(
                "SELECT lc.primary_tenant_profile_id, lc.start_date, r.property_id, EXISTS (SELECT 1 FROM contract_occupants co WHERE co.contract_id = lc.lease_contract_id AND co.tenant_profile_id = ?) AS has_occupant, EXISTS (SELECT 1 FROM person_profiles pp WHERE pp.person_profile_id = ? AND pp.deleted_at IS NULL) AS profile_exists FROM lease_contracts lc JOIN rooms r ON r.room_id = lc.room_id WHERE lc.lease_contract_id = ? AND lc.deleted_at IS NULL LIMIT 1",
                (rs, rowNum) -> new ContractMembershipContext(
                        getLongOrNull(rs, "primary_tenant_profile_id"),
                        toLocalDate(rs, "start_date"),
                        getLongOrNull(rs, "property_id"),
                        rs.getBoolean("has_occupant"),
                        rs.getBoolean("profile_exists")
                ),
                tenantProfileId,
                tenantProfileId,
                contractId
        );
        if (contexts.isEmpty()) {
            throw new AppException(ApiErrorCode.RESOURCE_NOT_FOUND);
        }
        ContractMembershipContext context = contexts.getFirst();
        if (!context.profileExists()) {
            throw new AppException(ApiErrorCode.RESOURCE_NOT_FOUND);
        }
        return context;
    }

    private Long resolveTenantIdForProfile(Long tenantProfileId, Long propertyId) {
        List<Long> tenantIds = jdbcTemplate.query(
                "SELECT t.tenant_id AS id FROM tenants t JOIN person_profiles pp ON pp.person_profile_id = ? LEFT JOIN tenant_account_provisionings tap ON tap.tenant_profile_id = pp.person_profile_id WHERE t.property_id = ? AND t.deleted_at IS NULL AND t.user_id = COALESCE(pp.user_id, tap.user_id) ORDER BY t.tenant_id DESC LIMIT 1",
                (rs, rowNum) -> rs.getLong("id"),
                tenantProfileId,
                propertyId
        );
        return tenantIds.isEmpty() ? null : tenantIds.getFirst();
    }

    private TenantAccountProvisioningResponse findContractOccupant(Long contractId, Long tenantProfileId) {
        List<TenantAccountProvisioningResponse> occupants = jdbcTemplate.query(
                "SELECT * FROM (" + baseCandidateSql() + ") account_candidates WHERE contract_id = ? AND profile_id = ? ORDER BY role_order ASC, full_name",
                (rs, rowNum) -> toResponse(rs),
                contractId,
                tenantProfileId
        );
        if (occupants.isEmpty()) {
            throw new AppException(ApiErrorCode.RESOURCE_NOT_FOUND);
        }
        return occupants.getFirst();
    }

    private ContractProvisioningContext findContractContext(Long contractId) {
        try {
            return jdbcTemplate.queryForObject("""
                            SELECT
                                lc.status AS contract_status,
                                lc.previous_contract_id,
                                r.current_status AS room_status,
                                (
                                    SELECT COUNT(*)
                                    FROM contract_occupants co
                                    WHERE co.contract_id = lc.lease_contract_id
                                      AND co.status = 'ACTIVE'
                                      AND co.tenant_profile_id IS NOT NULL
                                ) AS occupant_count
                            FROM lease_contracts lc
                            JOIN rooms r ON r.room_id = lc.room_id
                            WHERE lc.lease_contract_id = ?
                              AND lc.deleted_at IS NULL
                            """,
                    (rs, rowNum) -> new ContractProvisioningContext(
                            LeaseStatus.valueOf(rs.getString("contract_status")),
                            getLongOrNull(rs, "previous_contract_id"),
                            RoomStatus.valueOf(rs.getString("room_status")),
                            rs.getInt("occupant_count")
                    ),
                    contractId
            );
        } catch (EmptyResultDataAccessException exception) {
            throw new AppException(ApiErrorCode.RESOURCE_NOT_FOUND);
        }
    }

    private void validateContractContext(ContractProvisioningContext context) {
        if (!List.of(LeaseStatus.ACTIVE, LeaseStatus.EXPIRING_SOON).contains(context.contractStatus())) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        if (context.previousContractId() != null) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        if (context.roomStatus() != RoomStatus.OCCUPIED) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        if (context.occupantCount() <= 0) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
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
            throw new AppException(ApiErrorCode.RESOURCE_NOT_FOUND);
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
                    lc.lease_contract_id AS contract_id,
                    lc.contract_code,
                    lc.status AS contract_status,
                    lc.start_date,
                    lc.end_date,
                    lc.signed_at,
                    p.property_id AS property_id,
                    p.name AS property_name,
                    r.room_id AS room_id,
                    r.room_code,
                    r.current_status AS room_status,
                    co.id AS occupant_id,
                    pp.person_profile_id AS profile_id,
                    pp.full_name,
                    pp.phone,
                    pp.email,
                    COALESCE(NULLIF(pp.email, ''), primary_pp.email, primary_user.email) AS recipient_email,
                    co.occupant_role AS room_role,
                    co.status AS occupant_status,
                    GREATEST((
                        SELECT COUNT(*)
                        FROM contract_occupants co_count
                        WHERE co_count.contract_id = lc.lease_contract_id
                          AND co_count.status = 'ACTIVE'
                    ), 1) AS room_occupant_count,
                    r.max_occupants AS room_max_occupants,
                    u.user_id AS user_id,
                    u.role,
                    u.status AS account_status,
                    u.must_change_password,
                    u.last_login_at,
                    u.created_at AS account_created_at,
                    CASE
                        WHEN co.status = 'DISABLED' OR tap.status = 'DISABLED'
                            THEN 'DISABLED'
                        WHEN u.user_id IS NOT NULL
                          AND (u.last_login_at IS NOT NULL OR u.must_change_password = FALSE)
                            THEN 'ACTIVE'
                        WHEN tap.status = 'SENT' AND tap.sent_at IS NULL
                            THEN 'NOT_PROVISIONED'
                        ELSE tap.status
                    END AS provisioning_status,
                    tap.sent_at,
                    tap.failed_at,
                    tap.failure_reason,
                    co.disabled_reason,
                    co.disabled_by,
                    co.disabled_at,
                    tap.attempt_count,
                    tap.last_attempt_at,
                    CASE
                        WHEN NOT EXISTS (
                            SELECT 1
                            FROM identity_documents idoc
                            WHERE idoc.profile_id = pp.person_profile_id
                              AND idoc.status = 'ACTIVE'
                              AND idoc.doc_number IS NOT NULL
                              AND idoc.doc_number <> ''
                              AND idoc.doc_number NOT LIKE 'PENDING-%'
                        ) THEN 'MISSING_IDENTITY'
                        WHEN pp.portrait_file_id IS NULL THEN 'MISSING_PORTRAIT'
                        ELSE 'COMPLETED'
                    END AS profile_status,
                    NOT EXISTS (
                        SELECT 1
                        FROM identity_documents idoc
                        WHERE idoc.profile_id = pp.person_profile_id
                          AND idoc.status = 'ACTIVE'
                          AND idoc.doc_number IS NOT NULL
                          AND idoc.doc_number <> ''
                          AND idoc.doc_number NOT LIKE 'PENDING-%'
                    ) AS missing_identity,
                    pp.portrait_file_id IS NULL AS missing_portrait
                FROM lease_contracts lc
                JOIN rooms r ON r.room_id = lc.room_id
                JOIN properties p ON p.property_id = r.property_id
                JOIN person_profiles primary_pp ON primary_pp.person_profile_id = lc.primary_tenant_profile_id
                JOIN (
                    SELECT
                        active_occupant.contract_occupant_id AS id,
                        active_occupant.contract_id,
                        active_occupant.tenant_profile_id,
                        active_occupant.occupant_role,
                        active_occupant.status,
                        active_occupant.disabled_reason,
                        active_occupant.disabled_by,
                        active_occupant.disabled_at
                    FROM contract_occupants active_occupant
                    WHERE active_occupant.status IN ('ACTIVE','DISABLED')
                      AND active_occupant.tenant_profile_id IS NOT NULL

                    UNION ALL

                    SELECT
                        NULL AS id,
                        fallback_contract.lease_contract_id AS contract_id,
                        fallback_contract.primary_tenant_profile_id AS tenant_profile_id,
                        'PRIMARY' AS occupant_role,
                        'ACTIVE' AS status,
                        NULL AS disabled_reason,
                        NULL AS disabled_by,
                        NULL AS disabled_at
                    FROM lease_contracts fallback_contract
                    WHERE fallback_contract.deleted_at IS NULL
                      AND NOT EXISTS (
                          SELECT 1
                          FROM contract_occupants primary_occupant
                          WHERE primary_occupant.contract_id = fallback_contract.lease_contract_id
                            AND primary_occupant.tenant_profile_id =
                                fallback_contract.primary_tenant_profile_id
                      )
                ) co ON co.contract_id = lc.lease_contract_id
                JOIN person_profiles pp
                    ON pp.person_profile_id = co.tenant_profile_id
                    AND pp.deleted_at IS NULL
                LEFT JOIN users primary_user
                    ON primary_user.user_id = primary_pp.user_id
                    AND primary_user.deleted_at IS NULL
                LEFT JOIN users u ON u.user_id = pp.user_id AND u.deleted_at IS NULL
                LEFT JOIN tenant_account_provisionings tap ON tap.tenant_profile_id = pp.person_profile_id
                WHERE lc.deleted_at IS NULL
                  AND lc.status IN ('ACTIVE', 'EXPIRING_SOON', 'TERMINATION_PENDING')
                  AND lc.previous_contract_id IS NULL
                """;
    }

    private TenantAccountProvisioningResponse toResponse(ResultSet rs) throws SQLException {
        Long userId = getLongOrNull(rs, "user_id");
        Boolean mustChangePassword = getBooleanOrNull(rs, "must_change_password");
        String recipientEmail = rs.getString("recipient_email");
        TenantAccountProvisioningStatus provisioningStatus =
                parseProvisioningStatus(rs.getString("provisioning_status"));
        if (provisioningStatus == null) {
            provisioningStatus = userId != null
                    && isActivated(toLocalDateTime(rs, "last_login_at"), mustChangePassword)
                    ? TenantAccountProvisioningStatus.ACTIVE
                    : TenantAccountProvisioningStatus.NOT_PROVISIONED;
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
                .occupantStatus(parseOccupantStatus(rs.getString("occupant_status")))
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
                .disabledReason(rs.getString("disabled_reason"))
                .disabledBy(getLongOrNull(rs, "disabled_by"))
                .disabledAt(toLocalDateTime(rs, "disabled_at"))
                .attemptCount(getIntOrNull(rs, "attempt_count"))
                .lastAttemptAt(toLocalDateTime(rs, "last_attempt_at"))
                .profileStatus(rs.getString("profile_status"))
                .missingIdentity(rs.getBoolean("missing_identity"))
                .missingPortrait(rs.getBoolean("missing_portrait"))
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

    private String buildSendMessage(
            int sentCount,
            List<TenantAccountProvisioningResponse> occupants
    ) {
        long activatedCount = occupants.stream()
                .filter(item -> item.getProvisioningStatus() == TenantAccountProvisioningStatus.ACTIVE)
                .count();
        if (activatedCount > 0) {
            return "Đã gửi " + sentCount
                    + " tài khoản chưa kích hoạt; bỏ qua "
                    + activatedCount + " tài khoản đã kích hoạt.";
        }
        return "Đã gửi " + sentCount + " tài khoản cho khách thuê chưa kích hoạt.";
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
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
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

    private OccupantStatus parseOccupantStatus(String value) {
        return StringUtils.isEmpty(value) ? null : OccupantStatus.valueOf(value);
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

    private enum PreparationOutcome {
        CLAIMED,
        SKIPPED
    }

    private record ContractProvisioningContext(
            LeaseStatus contractStatus,
            Long previousContractId,
            RoomStatus roomStatus,
            int occupantCount
    ) {
    }

    private record ContractMembershipContext(
            Long primaryTenantProfileId,
            java.time.LocalDate startDate,
            Long propertyId,
            boolean hasOccupant,
            boolean profileExists
    ) {
    }
}
