package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.property.application.service.RoomCommitmentChecker;

import com.sep490.hdbhms.occupancy.application.port.in.command.RecordTenantIntentionCommand;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetLeaseContractManagementUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.RecordTenantIntentionUseCase;
import com.sep490.hdbhms.occupancy.domain.value_objects.LeaseStatus;
import com.sep490.hdbhms.property.domain.value_objects.RoomStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeaseContractEntity;
import com.sep490.hdbhms.property.infrastructure.persistence.entity.RoomEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaLeaseContractRepository;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaRoomRepository;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.LeaseContractManagementResponse;
import com.sep490.hdbhms.shared.utils.AuthUtils;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RecordTenantIntentionService implements RecordTenantIntentionUseCase {
    static final Set<String> TENANT_INTENTIONS = Set.of(
            "RENEW",
            "MOVE_OUT",
            "TRANSFER",
            "UNDECIDED"
    );

    JpaLeaseContractRepository leaseContractRepository;
    JpaRoomRepository roomRepository;
    JdbcTemplate jdbcTemplate;
    RoomCommitmentChecker roomCommitmentChecker;
    LeaseExpiryReminderService leaseExpiryReminderService;
    LeaseContractWorkflowSupport workflowSupport;
    GetLeaseContractManagementUseCase getLeaseContractManagementUseCase;

    @Override
    public LeaseContractManagementResponse executeForManagement(RecordTenantIntentionCommand command) {
        return recordTenantIntention(
                command.leaseContractId(),
                command.intention(),
                command.expectedMoveOutDate(),
                command.note(),
                "MANAGEMENT_WEB"
        );
    }

    @Override
    public LeaseContractManagementResponse executeForCurrentUser(RecordTenantIntentionCommand command) {
        if (currentUserHasRole("ROLE_TENANT")) {
            return executeForCurrentTenant(command);
        }
        return executeForManagement(command);
    }

    @Override
    public LeaseContractManagementResponse executeForCurrentTenant(RecordTenantIntentionCommand command) {
        Long userId = AuthUtils.getCurrentAuthenticationId();
        if (userId == null) {
            throw new AppException(ApiErrorCode.UNAUTHENTICATED);
        }
        Integer contractExists = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM lease_contracts lc
                        WHERE lc.lease_contract_id = ?
                          AND lc.deleted_at IS NULL
                        """,
                Integer.class,
                command.leaseContractId()
        );
        if (contractExists == null || contractExists == 0) {
            throw new AppException(ApiErrorCode.RESOURCE_NOT_FOUND);
        }

        Integer isPrimarySigner = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM lease_contracts lc
                        JOIN person_profiles pp ON pp.person_profile_id = lc.primary_tenant_profile_id
                        LEFT JOIN tenant_account_provisionings tap
                               ON tap.tenant_profile_id = pp.person_profile_id
                              AND tap.user_id = ?
                        WHERE lc.lease_contract_id = ?
                          AND lc.deleted_at IS NULL
                          AND pp.deleted_at IS NULL
                          AND (pp.user_id = ? OR (tap.user_id = ? AND tap.status <> 'DISABLED'))
                          AND NOT EXISTS (
                              SELECT 1
                              FROM contract_occupants disabled_primary
                              WHERE disabled_primary.contract_id = lc.lease_contract_id
                                AND disabled_primary.tenant_profile_id = pp.person_profile_id
                                AND disabled_primary.status = 'DISABLED'
                          )
                        """,
                Integer.class,
                userId,
                command.leaseContractId(),
                userId,
                userId
        );
        if (isPrimarySigner == null || isPrimarySigner == 0) {
            throw new AppException(ApiErrorCode.FORBIDDEN_OPERATION);
        }
        return recordTenantIntention(
                command.leaseContractId(),
                command.intention(),
                command.expectedMoveOutDate(),
                command.note(),
                "TENANT_MOBILE"
        );
    }

    private LeaseContractManagementResponse recordTenantIntention(
            Long leaseContractId,
            String intention,
            LocalDate expectedMoveOutDate,
            String note,
            String source
    ) {
        lockContractAndRoom(leaseContractId);
        LeaseContractEntity contract = leaseContractRepository.findById(leaseContractId)
                .orElseThrow(() -> new AppException(ApiErrorCode.RESOURCE_NOT_FOUND));
        if (contract.getDeletedAt() != null) {
            throw new AppException(ApiErrorCode.RESOURCE_NOT_FOUND);
        }
        if (!List.of(LeaseStatus.ACTIVE, LeaseStatus.EXPIRING_SOON).contains(contract.getStatus())) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }

        String normalizedIntention = normalizeTenantIntention(intention);
        log.info(normalizedIntention);
        if (!TENANT_INTENTIONS.contains(normalizedIntention)) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        LocalDate today = LocalDate.now();
        boolean withinThreeMonths = isWithinThreeMonths(contract, today);
        if (List.of("MOVE_OUT", "TRANSFER").contains(normalizedIntention)) {
            if (!withinThreeMonths) {
                throw new AppException(ApiErrorCode.INVALID_REQUEST_STATE);
            }
            if (expectedMoveOutDate == null) {
                throw new AppException(ApiErrorCode.INVALID_REQUEST_STATE);
            }
            if (expectedMoveOutDate.isBefore(today)) {
                throw new AppException(ApiErrorCode.INVALID_REQUEST_STATE);
            }
            if (contract.getEndDate() != null && expectedMoveOutDate.isAfter(contract.getEndDate())) {
                throw new AppException(ApiErrorCode.INVALID_REQUEST_STATE);
            }
        }

        RoomEntity room = contract.getRoom();
        if (room == null) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        contract.setTenantIntention(normalizedIntention);
        contract.setIntentionRecordedAt(LocalDateTime.now());

        if (List.of("MOVE_OUT", "TRANSFER").contains(normalizedIntention)) {
            contract.setExpectedVacantDate(expectedMoveOutDate);
            leaseContractRepository.saveAndFlush(contract);
            RoomStatus fromStatus = room.getCurrentStatus();
            if (fromStatus != RoomStatus.SOON_VACANT) {
                room.setCurrentStatus(RoomStatus.SOON_VACANT);
                roomRepository.saveAndFlush(room);
                workflowSupport.appendRoomStatusHistory(
                        room.getId(),
                        fromStatus,
                        RoomStatus.SOON_VACANT,
                        "Khách dự kiến chuyển đi theo hợp đồng " + contract.getContractCode()
                );
            }
        } else {
            contract.setExpectedVacantDate(null);
            if ("RENEW".equals(normalizedIntention)
                    && (room.getCurrentStatus() == RoomStatus.SOON_VACANT
                    || room.getCurrentStatus() == RoomStatus.RESERVED)) {
                RoomCommitmentChecker.Blocker blocker =
                        roomCommitmentChecker.checkRenewBlockers(room.getId(), contract.getId());
                if (blocker != RoomCommitmentChecker.Blocker.NONE) {
                    throwRenewBlocked(blocker);
                }
            }
            if (room.getCurrentStatus() == RoomStatus.SOON_VACANT) {
                RoomStatus fromStatus = room.getCurrentStatus();
                room.setCurrentStatus(RoomStatus.OCCUPIED);
                roomRepository.saveAndFlush(room);
                workflowSupport.appendRoomStatusHistory(
                        room.getId(),
                        fromStatus,
                        RoomStatus.OCCUPIED,
                        "Khách đổi ý tiếp tục thuê hợp đồng " + contract.getContractCode()
                );
                if ("RENEW".equals(normalizedIntention)) {
                    workflowSupport.appendContractEvent(
                            contract.getId(),
                            "RENEWAL_AFTER_MOVE_OUT_INTENT",
                            "Khách đổi ý tiếp tục thuê sau khi đã báo chuyển đi"
                    );
                }
            }
            leaseContractRepository.saveAndFlush(contract);
        }
        String eventData = "intention=" + normalizedIntention
                + "; expectedVacantDate=" + (contract.getExpectedVacantDate() != null ? contract.getExpectedVacantDate() : "")
                + "; source=" + (source == null ? "" : source)
                + "; note=" + (note == null ? "" : note.trim());
        workflowSupport.appendContractEvent(contract.getId(), "INTENTION_RECORDED", eventData);
        leaseExpiryReminderService.onTenantIntentionRecorded(contract.getId(), LocalDate.now());
        return getLeaseContractManagementUseCase.findOne(contract.getId());
    }

    private void lockContractAndRoom(Long leaseContractId) {
        List<Long> locked = jdbcTemplate.query("""
                        SELECT lc.lease_contract_id AS id
                        FROM lease_contracts lc
                        JOIN rooms r ON r.room_id = lc.room_id
                        WHERE lc.lease_contract_id = ?
                        FOR UPDATE
                        """,
                (rs, rowNum) -> rs.getLong("id"),
                leaseContractId
        );
        if (locked.isEmpty()) {
            throw new AppException(ApiErrorCode.RESOURCE_NOT_FOUND);
        }
    }

    private boolean currentUserHasRole(String role) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.getAuthorities() != null
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> role.equals(authority.getAuthority()));
    }

    private String normalizeTenantIntention(String intention) {
        String normalized = intention == null ? "" : intention.trim().toUpperCase();
        return "TRANSFER_ROOM".equals(normalized) ? "TRANSFER" : normalized;
    }

    private boolean isWithinThreeMonths(LeaseContractEntity contract, LocalDate today) {
        return contract.getEndDate() != null && !today.isBefore(contract.getEndDate().minusMonths(3));
    }

    private void throwRenewBlocked(RoomCommitmentChecker.Blocker blocker) {
        if (blocker == RoomCommitmentChecker.Blocker.ROOM_ALREADY_RESERVED_BY_NEW_TENANT) {
            throw new AppException(ApiErrorCode.OPERATION_CONFLICT);
        }
        throw new AppException(ApiErrorCode.OPERATION_CONFLICT);
    }
}
