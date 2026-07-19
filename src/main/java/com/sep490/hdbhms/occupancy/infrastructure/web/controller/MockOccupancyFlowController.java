package com.sep490.hdbhms.occupancy.infrastructure.web.controller;

import com.sep490.hdbhms.occupancy.application.port.in.usecase.RoomTransferUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.command.AcceptHolderNominationCommand;
import com.sep490.hdbhms.occupancy.application.port.in.command.ApproveTransferCommand;
import com.sep490.hdbhms.occupancy.application.port.in.command.CompleteTransferCommand;
import com.sep490.hdbhms.occupancy.application.port.in.command.ConfirmTenantTransferCommand;
import com.sep490.hdbhms.occupancy.application.port.in.command.CreateTransferRequestCommand;
import com.sep490.hdbhms.occupancy.application.port.in.command.ExecuteTransferCommand;
import com.sep490.hdbhms.occupancy.application.port.in.command.NominateHolderCommand;
import com.sep490.hdbhms.notification.infrastructure.dispatcher.NotificationOutboxDispatcher;
import com.sep490.hdbhms.occupancy.application.service.LeaseContractLifecycleService;
import com.sep490.hdbhms.occupancy.application.service.LeaseContractManagementService;
import com.sep490.hdbhms.occupancy.domain.model.RoomTransferRequest;
import com.sep490.hdbhms.occupancy.domain.value_objects.LeaseStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.SettlementType;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeaseContractEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaLeaseContractRepository;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.ExecuteTransferRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.RoomTransferResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.mapper.RoomTransferWebMapper;
import com.sep490.hdbhms.scheduling.config.ScheduledTaskProcessor;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@Profile({"dev", "test", "local"})
@ConditionalOnProperty(name = "app.mock-occupancy-flow.enabled", havingValue = "true")
@RequiredArgsConstructor
@RequestMapping("/api/v1/mock")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MockOccupancyFlowController {
    private static final List<String> RENEWAL_TASK_TYPES = List.of(
            "LEASE_RENEWAL_TERMS_CONFIRMATION",
            "LEASE_HANDOVER_CONFIRMATION",
            "LEASE_EXPIRY_MANAGER_VISIT"
    );

    private static final List<String> RENEWAL_NOTIFICATION_EVENTS = List.of(
            "LEASE_EXPIRY_REMINDER_FIRST",
            "LEASE_EXPIRY_REMINDER_SECOND",
            "LEASE_EXPIRY_REMINDER_FINAL",
            "LEASE_EXPIRY_MANAGER_VISIT_REQUIRED",
            "LEASE_RENEWAL_TERMS_CONFIRMATION_DUE",
            "LEASE_HANDOVER_CONFIRMATION_DUE"
    );
    private static final List<String> ROOM_TRANSFER_NOTIFICATION_EVENTS = List.of(
            "ROOM_TRANSFER_MANAGER_ACTION_REQUIRED",
            "ROOM_TRANSFER_HOLDER_NOMINATION_REQUESTED",
            "ROOM_TRANSFER_TARGET_HOLDER_APPROVAL_REQUESTED",
            "CHANGE_REQUEST_CREATED"
    );
    private static final List<LeaseStatus> EXPIRY_CANDIDATE_STATUSES = List.of(
            LeaseStatus.ACTIVE,
            LeaseStatus.EXPIRING_SOON
    );

    LeaseContractLifecycleService leaseContractLifecycleService;
    LeaseContractManagementService leaseContractManagementService;
    JpaLeaseContractRepository leaseContractRepository;
    RoomTransferUseCase roomTransferUseCase;
    RoomTransferWebMapper roomTransferWebMapper;
    NotificationOutboxDispatcher notificationOutboxDispatcher;
    ScheduledTaskProcessor scheduledTaskProcessor;
    JdbcTemplate jdbcTemplate;

    @PostMapping("/lease-renewal/contracts/{contractId}/prepare-expiring")
    public ApiResponse<MockLeaseRenewalStateResponse> prepareExpiringContract(
            @PathVariable Long contractId,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(defaultValue = "true") boolean clearIntention,
            @RequestParam(defaultValue = "true") boolean resetReminderState,
            @RequestParam(defaultValue = "false") boolean clearNotifications,
            @RequestParam(defaultValue = "false") boolean triggerScheduler
    ) {
        LeaseContractEntity contract = requireContract(contractId);
        contract.setEndDate(endDate == null ? LocalDate.now().plusDays(75) : endDate);
        if (!EXPIRY_CANDIDATE_STATUSES.contains(contract.getStatus())) {
            contract.setStatus(LeaseStatus.ACTIVE);
        }
        if (clearIntention) {
            contract.setTenantIntention(null);
            contract.setExpectedVacantDate(null);
            contract.setIntentionRecordedAt(null);
        }
        leaseContractRepository.saveAndFlush(contract);
        if (resetReminderState) {
            resetRenewalState(contractId, clearNotifications);
        }
        if (triggerScheduler) {
            markRecurringSchedulerDue(
                    "CONTRACT_LIFECYCLE_SCAN",
                    "SYSTEM_JOB:CONTRACT_LIFECYCLE_SCAN",
                    "DAILY:00:05",
                    schedulerNow()
            );
            markRecurringSchedulerDue(
                    "NOTIFICATION_OUTBOX_DISPATCH",
                    "SYSTEM_JOB:NOTIFICATION_OUTBOX_DISPATCH",
                    "FIXED_DELAY:PT60S",
                    schedulerNow().plusSeconds(15)
            );
        }
        return ApiResponse.<MockLeaseRenewalStateResponse>builder()
                .data(buildLeaseRenewalState(contractId))
                .build();
    }

    @PostMapping("/lease-renewal/contracts/{contractId}/reminders/run")
    public ApiResponse<MockLeaseRenewalStateResponse> runLeaseRenewalReminder(
            @PathVariable Long contractId,
            @RequestParam(required = false) LocalDate today,
            @RequestParam(defaultValue = "true") boolean dispatchOutbox
    ) {
        requireContract(contractId);
        leaseContractLifecycleService.processContract(contractId, today == null ? LocalDate.now() : today);
        if (dispatchOutbox) {
            notificationOutboxDispatcher.dispatch();
        }
        return ApiResponse.<MockLeaseRenewalStateResponse>builder()
                .data(buildLeaseRenewalState(contractId))
                .build();
    }

    @PostMapping("/lease-renewal/contracts/{contractId}/intention")
    public ApiResponse<MockLeaseRenewalStateResponse> recordLeaseRenewalIntention(
            @PathVariable Long contractId,
            @RequestParam String intention,
            @RequestParam(required = false) LocalDate expectedMoveOutDate,
            @RequestParam(required = false) String note
    ) {
        leaseContractManagementService.recordTenantIntention(
                contractId,
                intention,
                expectedMoveOutDate,
                note == null || note.isBlank() ? "Mock renewal intention" : note
        );
        return ApiResponse.<MockLeaseRenewalStateResponse>builder()
                .data(buildLeaseRenewalState(contractId))
                .build();
    }

    @PostMapping("/lease-renewal/contracts/{contractId}/reset")
    public ApiResponse<MockLeaseRenewalStateResponse> resetLeaseRenewalReminderState(
            @PathVariable Long contractId,
            @RequestParam(defaultValue = "false") boolean clearNotifications
    ) {
        requireContract(contractId);
        resetRenewalState(contractId, clearNotifications);
        return ApiResponse.<MockLeaseRenewalStateResponse>builder()
                .data(buildLeaseRenewalState(contractId))
                .build();
    }

    @GetMapping("/lease-renewal/contracts/{contractId}/state")
    public ApiResponse<MockLeaseRenewalStateResponse> getLeaseRenewalState(@PathVariable Long contractId) {
        requireContract(contractId);
        return ApiResponse.<MockLeaseRenewalStateResponse>builder()
                .data(buildLeaseRenewalState(contractId))
                .build();
    }

    @PostMapping("/scheduler/contract-lifecycle/due-now")
    public ApiResponse<Map<String, Object>> markContractLifecycleSchedulerDueNow() {
        markRecurringSchedulerDue(
                "CONTRACT_LIFECYCLE_SCAN",
                "SYSTEM_JOB:CONTRACT_LIFECYCLE_SCAN",
                "DAILY:00:05",
                schedulerNow()
        );
        return ApiResponse.<Map<String, Object>>builder()
                .data(buildSchedulerState("SYSTEM_JOB:CONTRACT_LIFECYCLE_SCAN"))
                .build();
    }

    @PostMapping("/scheduler/notification-outbox/due-now")
    public ApiResponse<Map<String, Object>> markNotificationOutboxSchedulerDueNow() {
        markRecurringSchedulerDue(
                "NOTIFICATION_OUTBOX_DISPATCH",
                "SYSTEM_JOB:NOTIFICATION_OUTBOX_DISPATCH",
                "FIXED_DELAY:PT60S",
                schedulerNow()
        );
        return ApiResponse.<Map<String, Object>>builder()
                .data(buildSchedulerState("SYSTEM_JOB:NOTIFICATION_OUTBOX_DISPATCH"))
                .build();
    }

    @PostMapping("/scheduler/notification-outbox/dispatch-now")
    public ApiResponse<Map<String, Object>> dispatchNotificationOutboxNow() {
        notificationOutboxDispatcher.dispatch();
        return ApiResponse.<Map<String, Object>>builder()
                .data(Map.of("dispatchedAt", LocalDateTime.now()))
                .build();
    }

    @GetMapping("/scheduler/processor-state")
    public ApiResponse<Map<String, Object>> getSchedulerProcessorState() {
        ScheduledTaskProcessor.ProcessorSnapshot snapshot = scheduledTaskProcessor.snapshot();
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("serverNow", LocalDateTime.now());
        state.put("workerId", snapshot.workerId());
        state.put("lastPollAt", snapshot.lastPollAt());
        state.put("lastPollFinishedAt", snapshot.lastPollFinishedAt());
        state.put("lastClaimableTaskCount", snapshot.lastClaimableTaskCount());
        state.put("lastSubmittedTaskCount", snapshot.lastSubmittedTaskCount());
        state.put("lastPollError", snapshot.lastPollError());
        state.put("activeWorkerCount", snapshot.activeWorkerCount());
        state.put("queuedWorkerCount", snapshot.queuedWorkerCount());
        state.put("claimableTasks", safeQueryRows("""
                        SELECT
                            scheduled_task_id AS id,
                            task_type AS taskType,
                            target_type AS targetType,
                            target_id AS targetId,
                            due_at AS dueAt,
                            status,
                            retry_count AS retryCount,
                            idempotency_key AS idempotencyKey,
                            recurring,
                            schedule_expression AS scheduleExpression,
                            last_error AS lastError,
                            claimed_at AS claimedAt,
                            claimed_by AS claimedBy,
                            lock_until AS lockUntil,
                            executed_at AS executedAt
                        FROM scheduled_tasks
                        WHERE (status = 'PENDING' AND due_at <= NOW(6))
                           OR (status = 'PROCESSING' AND lock_until <= NOW(6))
                        ORDER BY due_at ASC
                        LIMIT 20
                        """));
        state.put("taskTypeLocks", safeQueryRows("""
                        SELECT
                            task_type AS taskType,
                            claimed_by AS claimedBy,
                            lock_until AS lockUntil,
                            updated_at AS updatedAt,
                            lock_until > NOW(6) AS active
                        FROM scheduled_task_type_locks
                        ORDER BY task_type ASC
                        """));
        return ApiResponse.<Map<String, Object>>builder()
                .data(state)
                .build();
    }

    @PostMapping("/room-transfers/{requestId}/eligibility/refresh")
    public ApiResponse<RoomTransferResponse> refreshRoomTransferEligibility(@PathVariable Long requestId) {
        RoomTransferRequest request = roomTransferUseCase.refreshTransferEligibilitySnapshot(requestId);
        return ApiResponse.<RoomTransferResponse>builder()
                .data(roomTransferWebMapper.toResponse(request))
                .build();
    }

    @PostMapping("/room-transfers/code/{requestCode}/eligibility/refresh")
    public ApiResponse<RoomTransferResponse> refreshRoomTransferEligibilityByCode(@PathVariable String requestCode) {
        RoomTransferRequest current = roomTransferUseCase.getTransferRequestByCode(requestCode);
        RoomTransferRequest request = roomTransferUseCase.refreshTransferEligibilitySnapshot(current.getId());
        return ApiResponse.<RoomTransferResponse>builder()
                .data(roomTransferWebMapper.toResponse(request))
                .build();
    }

    @PostMapping("/room-transfers/contracts/{contractId}/prepare-eligible")
    public ApiResponse<Map<String, Object>> prepareRoomTransferEligibleContract(
            @PathVariable Long contractId,
            @RequestParam(defaultValue = "13") int monthsStayed,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate
    ) {
        prepareRoomTransferContractEligibility(contractId, monthsStayed, startDate, endDate);
        return ApiResponse.<Map<String, Object>>builder()
                .data(buildRoomTransferContractEligibilityState(contractId))
                .build();
    }

    @PostMapping("/room-transfers")
    public ApiResponse<MockRoomTransferStateResponse> createRoomTransfer(
            @RequestParam Long requesterUserId,
            @RequestParam Long sourceContractId,
            @RequestParam Long targetRoomId,
            @RequestParam(required = false) LocalDate requestedTransferDate,
            @RequestParam(required = false) List<Long> transferredTenantProfileIds,
            @RequestParam(required = false) String reason,
            @RequestParam(defaultValue = "true") boolean prepareEligible,
            @RequestParam(defaultValue = "13") int monthsStayed
    ) {
        if (prepareEligible) {
            prepareRoomTransferContractEligibility(sourceContractId, monthsStayed, null, null);
        }
        Long requestId = roomTransferUseCase.createTransferRequest(new CreateTransferRequestCommand(
                requesterUserId,
                sourceContractId,
                targetRoomId,
                requestedTransferDate == null ? nextAllowedRoomTransferDate() : requestedTransferDate,
                transferredTenantProfileIds == null ? List.of() : transferredTenantProfileIds,
                reason == null || reason.isBlank() ? "Mock room transfer" : reason
        ));
        return ApiResponse.<MockRoomTransferStateResponse>builder()
                .data(buildRoomTransferState(requestId))
                .build();
    }

    @PostMapping("/room-transfers/contracts/{sourceContractId}")
    public ApiResponse<MockRoomTransferStateResponse> createRoomTransferFromContract(
            @PathVariable Long sourceContractId,
            @RequestParam(required = false) Long targetRoomId,
            @RequestParam(required = false) LocalDate requestedTransferDate,
            @RequestParam(required = false) String reason,
            @RequestParam(defaultValue = "true") boolean prepareEligible,
            @RequestParam(defaultValue = "13") int monthsStayed
    ) {
        Long resolvedTargetRoomId = targetRoomId == null
                ? resolveFirstMockTransferTargetRoomId(sourceContractId)
                : targetRoomId;
        if (prepareEligible) {
            prepareRoomTransferContractEligibility(sourceContractId, monthsStayed, null, null);
        }
        Long requestId = roomTransferUseCase.createTransferRequest(new CreateTransferRequestCommand(
                resolvePrimaryTenantUserId(sourceContractId),
                sourceContractId,
                resolvedTargetRoomId,
                requestedTransferDate == null ? nextAllowedRoomTransferDate() : requestedTransferDate,
                List.of(),
                reason == null || reason.isBlank() ? "Mock room transfer" : reason
        ));
        return ApiResponse.<MockRoomTransferStateResponse>builder()
                .data(buildRoomTransferState(requestId))
                .build();
    }

    @PostMapping("/room-transfers/{requestId}/approve")
    public ApiResponse<MockRoomTransferStateResponse> approveRoomTransfer(
            @PathVariable Long requestId,
            @RequestParam Long managerUserId,
            @RequestParam(required = false) SettlementType settlementType
    ) {
        roomTransferUseCase.approveTransfer(new ApproveTransferCommand(requestId, managerUserId, settlementType));
        return ApiResponse.<MockRoomTransferStateResponse>builder()
                .data(buildRoomTransferState(requestId))
                .build();
    }

    @PostMapping("/room-transfers/{requestId}/reject")
    public ApiResponse<MockRoomTransferStateResponse> rejectRoomTransfer(
            @PathVariable Long requestId,
            @RequestParam Long managerUserId,
            @RequestParam(required = false) String note
    ) {
        roomTransferUseCase.rejectTransferRequest(requestId, managerUserId, note == null ? "Mock reject" : note);
        return ApiResponse.<MockRoomTransferStateResponse>builder()
                .data(buildRoomTransferState(requestId))
                .build();
    }

    @PostMapping("/room-transfers/{requestId}/holder-replacement")
    public ApiResponse<MockRoomTransferStateResponse> nominateRoomTransferHolder(
            @PathVariable Long requestId,
            @RequestParam Long requesterUserId,
            @RequestParam Long nominatedHolderProfileId
    ) {
        roomTransferUseCase.nominateHolder(new NominateHolderCommand(
                requestId,
                requesterUserId,
                nominatedHolderProfileId
        ));
        return ApiResponse.<MockRoomTransferStateResponse>builder()
                .data(buildRoomTransferState(requestId))
                .build();
    }

    @PostMapping("/room-transfers/{requestId}/holder-replacement/accept")
    public ApiResponse<MockRoomTransferStateResponse> acceptRoomTransferHolderNomination(
            @PathVariable Long requestId,
            @RequestParam Long tenantUserId
    ) {
        roomTransferUseCase.acceptHolderNomination(new AcceptHolderNominationCommand(requestId, tenantUserId));
        return ApiResponse.<MockRoomTransferStateResponse>builder()
                .data(buildRoomTransferState(requestId))
                .build();
    }

    @PostMapping("/room-transfers/{requestId}/holder-replacement/reject")
    public ApiResponse<MockRoomTransferStateResponse> rejectRoomTransferHolderNomination(
            @PathVariable Long requestId,
            @RequestParam Long tenantUserId
    ) {
        roomTransferUseCase.rejectHolderNomination(requestId, tenantUserId);
        return ApiResponse.<MockRoomTransferStateResponse>builder()
                .data(buildRoomTransferState(requestId))
                .build();
    }

    @PostMapping("/room-transfers/{requestId}/confirm")
    public ApiResponse<MockRoomTransferStateResponse> confirmRoomTransfer(
            @PathVariable Long requestId,
            @RequestParam Long tenantUserId,
            @RequestParam(required = false) SettlementType settlementType,
            @RequestParam(required = false) Long nominatedHolderProfileId
    ) {
        roomTransferUseCase.confirmTenantTransfer(new ConfirmTenantTransferCommand(
                requestId,
                tenantUserId,
                settlementType,
                nominatedHolderProfileId
        ));
        return ApiResponse.<MockRoomTransferStateResponse>builder()
                .data(buildRoomTransferState(requestId))
                .build();
    }

    @PostMapping("/room-transfers/{requestId}/target-holder/approve")
    public ApiResponse<MockRoomTransferStateResponse> approveRoomTransferTargetHolder(
            @PathVariable Long requestId,
            @RequestParam Long holderUserId
    ) {
        roomTransferUseCase.approveTargetHolderTransfer(requestId, holderUserId);
        return ApiResponse.<MockRoomTransferStateResponse>builder()
                .data(buildRoomTransferState(requestId))
                .build();
    }

    @PostMapping("/room-transfers/{requestId}/target-holder/reject")
    public ApiResponse<MockRoomTransferStateResponse> rejectRoomTransferTargetHolder(
            @PathVariable Long requestId,
            @RequestParam Long holderUserId
    ) {
        roomTransferUseCase.rejectTargetHolderTransfer(requestId, holderUserId);
        return ApiResponse.<MockRoomTransferStateResponse>builder()
                .data(buildRoomTransferState(requestId))
                .build();
    }

    @PostMapping("/room-transfers/{requestId}/contract/confirm")
    public ApiResponse<MockRoomTransferStateResponse> confirmRoomTransferContract(
            @PathVariable Long requestId,
            @RequestParam Long tenantUserId
    ) {
        roomTransferUseCase.confirmTransferContract(requestId, tenantUserId);
        return ApiResponse.<MockRoomTransferStateResponse>builder()
                .data(buildRoomTransferState(requestId))
                .build();
    }

    @PostMapping("/room-transfers/{requestId}/contract/sign")
    public ApiResponse<MockRoomTransferStateResponse> signRoomTransferContract(
            @PathVariable Long requestId,
            @RequestParam Long tenantUserId
    ) {
        roomTransferUseCase.signTransferContract(requestId, tenantUserId);
        return ApiResponse.<MockRoomTransferStateResponse>builder()
                .data(buildRoomTransferState(requestId))
                .build();
    }

    @PostMapping("/room-transfers/{requestId}/contract/reject")
    public ApiResponse<MockRoomTransferStateResponse> rejectRoomTransferContract(
            @PathVariable Long requestId,
            @RequestParam Long tenantUserId
    ) {
        roomTransferUseCase.rejectTransferContract(requestId, tenantUserId);
        return ApiResponse.<MockRoomTransferStateResponse>builder()
                .data(buildRoomTransferState(requestId))
                .build();
    }

    @PostMapping("/room-transfers/{requestId}/execute")
    public ApiResponse<MockRoomTransferStateResponse> executeRoomTransfer(
            @PathVariable Long requestId,
            @RequestParam Long managerUserId,
            @RequestParam(required = false) SettlementType settlementType,
            @RequestBody(required = false) ExecuteTransferRequest request
    ) {
        roomTransferUseCase.executeTransfer(new ExecuteTransferCommand(
                requestId,
                managerUserId,
                toTransferHandoverData(request == null ? null : request.transferOutHandover()),
                toTransferHandoverData(request == null ? null : request.transferInHandover()),
                settlementType != null ? settlementType : request == null ? null : request.positiveDifferenceSettlementType()
        ));
        return ApiResponse.<MockRoomTransferStateResponse>builder()
                .data(buildRoomTransferState(requestId))
                .build();
    }

    @PostMapping("/room-transfers/{requestId}/complete")
    public ApiResponse<MockRoomTransferStateResponse> completeRoomTransfer(
            @PathVariable Long requestId,
            @RequestParam Long managerUserId,
            @RequestParam(required = false) SettlementType settlementType,
            @RequestBody(required = false) ExecuteTransferRequest request
    ) {
        roomTransferUseCase.completeTransfer(new CompleteTransferCommand(
                requestId,
                managerUserId,
                toTransferHandoverData(request == null ? null : request.transferInHandover()),
                settlementType != null ? settlementType : request == null ? null : request.positiveDifferenceSettlementType()
        ));
        return ApiResponse.<MockRoomTransferStateResponse>builder()
                .data(buildRoomTransferState(requestId))
                .build();
    }

    @PostMapping("/room-transfers/{requestId}/cancel")
    public ApiResponse<MockRoomTransferStateResponse> cancelRoomTransfer(@PathVariable Long requestId) {
        roomTransferUseCase.cancelTransferRequest(requestId);
        return ApiResponse.<MockRoomTransferStateResponse>builder()
                .data(buildRoomTransferState(requestId))
                .build();
    }

    @PostMapping("/room-transfers/timeouts/run")
    public ApiResponse<Map<String, Object>> runRoomTransferTimeouts() {
        int targetHolderExpired = roomTransferUseCase.expireTargetHolderApprovals();
        int sourceHolderExpired = roomTransferUseCase.expireSourceHolderNominations();
        return ApiResponse.<Map<String, Object>>builder()
                .data(Map.of(
                        "targetHolderExpired", targetHolderExpired,
                        "sourceHolderExpired", sourceHolderExpired,
                        "ranAt", LocalDateTime.now()
                ))
                .build();
    }

    @PostMapping("/scheduler/room-transfer-timeout/due-now")
    public ApiResponse<Map<String, Object>> markRoomTransferTimeoutSchedulerDueNow() {
        markRecurringSchedulerDue(
                "ROOM_TRANSFER_TIMEOUT",
                "SYSTEM_JOB:ROOM_TRANSFER_TIMEOUT",
                "HOURLY:00",
                schedulerNow()
        );
        return ApiResponse.<Map<String, Object>>builder()
                .data(buildSchedulerState("SYSTEM_JOB:ROOM_TRANSFER_TIMEOUT"))
                .build();
    }

    @PostMapping("/room-transfers/{requestId}/notifications/clear")
    public ApiResponse<MockRoomTransferStateResponse> clearRoomTransferNotifications(@PathVariable Long requestId) {
        deleteRoomTransferNotifications(requestId);
        return ApiResponse.<MockRoomTransferStateResponse>builder()
                .data(buildRoomTransferState(requestId))
                .build();
    }

    @GetMapping("/room-transfers/{requestId}/state")
    public ApiResponse<MockRoomTransferStateResponse> getRoomTransferState(@PathVariable Long requestId) {
        return ApiResponse.<MockRoomTransferStateResponse>builder()
                .data(buildRoomTransferState(requestId))
                .build();
    }

    private ExecuteTransferCommand.TransferHandoverData toTransferHandoverData(
            ExecuteTransferRequest.TransferHandoverPayload payload
    ) {
        if (payload == null) {
            return null;
        }
        return new ExecuteTransferCommand.TransferHandoverData(
                payload.handoverDate(),
                payload.note(),
                toTransferMeterData(payload.electricity()),
                toTransferMeterData(payload.water()),
                payload.assets() == null
                        ? null
                        : payload.assets().stream()
                        .map(this::toTransferAssetData)
                        .toList(),
                payload.incidentalChargeAmount(),
                payload.incidentalChargeNote()
        );
    }

    private ExecuteTransferCommand.MeterReadingData toTransferMeterData(ExecuteTransferRequest.MeterReadingPayload payload) {
        if (payload == null) {
            return null;
        }
        return new ExecuteTransferCommand.MeterReadingData(
                payload.currentValue(),
                payload.photoFileId(),
                payload.readingDate()
        );
    }

    private ExecuteTransferCommand.AssetData toTransferAssetData(ExecuteTransferRequest.AssetPayload payload) {
        return new ExecuteTransferCommand.AssetData(
                payload.id(),
                payload.assetName(),
                payload.assetCategory(),
                payload.quantity(),
                payload.currentCondition(),
                payload.description(),
                payload.fileImageId()
        );
    }

    private LocalDate nextAllowedRoomTransferDate() {
        LocalDate date = LocalDate.now().plusDays(7);
        while (date.getDayOfMonth() >= 1 && date.getDayOfMonth() <= 5) {
            date = date.plusDays(1);
        }
        return date;
    }

    private Long resolvePrimaryTenantUserId(Long contractId) {
        List<Long> userIds = jdbcTemplate.queryForList("""
                        SELECT pp.user_id
                        FROM lease_contracts lc
                        JOIN person_profiles pp
                          ON pp.person_profile_id = lc.primary_tenant_profile_id
                        WHERE lc.lease_contract_id = ?
                          AND pp.user_id IS NOT NULL
                        LIMIT 1
                        """,
                Long.class,
                contractId
        );
        if (userIds.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Khong tim thay user cua nguoi thue chinh cho hop dong: " + contractId
            );
        }
        return userIds.get(0);
    }

    private Long resolveFirstMockTransferTargetRoomId(Long sourceContractId) {
        List<Long> roomIds = jdbcTemplate.queryForList("""
                        SELECT target.room_id
                        FROM lease_contracts lc
                        JOIN rooms old_room
                          ON old_room.room_id = lc.room_id
                        JOIN rooms target
                          ON target.property_id = old_room.property_id
                        WHERE lc.lease_contract_id = ?
                          AND target.room_id <> old_room.room_id
                          AND target.current_status = 'VACANT'
                          AND target.deleted_at IS NULL
                          AND COALESCE(target.max_occupants, 0) >= 1
                        ORDER BY target.sort_order ASC, target.room_code ASC, target.room_id ASC
                        LIMIT 1
                        """,
                Long.class,
                sourceContractId
        );
        if (roomIds.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Khong tim thay phong trong cung co so de mock chuyen phong. Truyen targetRoomId neu muon chon phong cu the."
            );
        }
        return roomIds.get(0);
    }

    private LeaseContractEntity requireContract(Long contractId) {
        return leaseContractRepository.findByIdAndDeletedAtIsNull(contractId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Khong tim thay hop dong thue: " + contractId
                ));
    }

    private void markRecurringSchedulerDue(
            String taskType,
            String idempotencyKey,
            String scheduleExpression,
            LocalDateTime dueAt
    ) {
        jdbcTemplate.update("""
                        INSERT INTO scheduled_tasks (
                            task_type,
                            target_type,
                            target_id,
                            due_at,
                            status,
                            retry_count,
                            idempotency_key,
                            recurring,
                            schedule_expression,
                            created_at
                        )
                        VALUES (
                            ?,
                            'SYSTEM_JOB',
                            0,
                            ?,
                            'PENDING',
                            0,
                            ?,
                            1,
                            ?,
                            NOW(6)
                        )
                        ON DUPLICATE KEY UPDATE
                            due_at = VALUES(due_at),
                            status = IF(status = 'PROCESSING', status, 'PENDING'),
                            retry_count = 0,
                            last_error = NULL,
                            claimed_at = IF(status = 'PROCESSING', claimed_at, NULL),
                            claimed_by = IF(status = 'PROCESSING', claimed_by, NULL),
                            lock_until = IF(status = 'PROCESSING', lock_until, NULL)
                        """,
                taskType,
                dueAt,
                idempotencyKey,
                scheduleExpression
        );
    }

    private LocalDateTime schedulerNow() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    private Map<String, Object> buildSchedulerState(String idempotencyKey) {
        return normalizeDateValues(jdbcTemplate.queryForMap("""
                        SELECT
                            scheduled_task_id AS id,
                            task_type AS taskType,
                            target_type AS targetType,
                            target_id AS targetId,
                            due_at AS dueAt,
                            status,
                            retry_count AS retryCount,
                            idempotency_key AS idempotencyKey,
                            recurring,
                            schedule_expression AS scheduleExpression,
                            last_error AS lastError,
                            claimed_at AS claimedAt,
                            claimed_by AS claimedBy,
                            lock_until AS lockUntil,
                            executed_at AS executedAt,
                            created_at AS createdAt
                        FROM scheduled_tasks
                        WHERE idempotency_key = ?
                        LIMIT 1
                        """,
                idempotencyKey
        ));
    }

    private void resetRenewalState(Long contractId, boolean clearNotifications) {
        jdbcTemplate.update(
                "DELETE FROM reminder_trackers WHERE target_type = 'CONTRACT' AND target_id = ?",
                contractId
        );
        jdbcTemplate.update("""
                        DELETE FROM manager_tasks
                        WHERE lease_contract_id = ?
                          AND task_type IN (?, ?, ?)
                        """,
                contractId,
                RENEWAL_TASK_TYPES.get(0),
                RENEWAL_TASK_TYPES.get(1),
                RENEWAL_TASK_TYPES.get(2)
        );
        if (clearNotifications) {
            deleteRenewalNotifications(contractId);
        }
    }

    private void deleteRenewalNotifications(Long contractId) {
        jdbcTemplate.update("""
                        DELETE FROM notification_deliveries
                        WHERE outbox_id IN (
                            SELECT notification_outbox_id
                            FROM notification_outbox
                            WHERE event_type IN (?, ?, ?, ?, ?, ?)
                              AND (
                                    (target_type = 'CONTRACT' AND target_id = ?)
                                    OR JSON_UNQUOTE(JSON_EXTRACT(payload, '$.contractId')) = ?
                                  )
                        )
                        """,
                RENEWAL_NOTIFICATION_EVENTS.get(0),
                RENEWAL_NOTIFICATION_EVENTS.get(1),
                RENEWAL_NOTIFICATION_EVENTS.get(2),
                RENEWAL_NOTIFICATION_EVENTS.get(3),
                RENEWAL_NOTIFICATION_EVENTS.get(4),
                RENEWAL_NOTIFICATION_EVENTS.get(5),
                contractId,
                String.valueOf(contractId)
        );
        jdbcTemplate.update("""
                        DELETE FROM notification_outbox
                        WHERE event_type IN (?, ?, ?, ?, ?, ?)
                          AND (
                                (target_type = 'CONTRACT' AND target_id = ?)
                                OR JSON_UNQUOTE(JSON_EXTRACT(payload, '$.contractId')) = ?
                              )
                        """,
                RENEWAL_NOTIFICATION_EVENTS.get(0),
                RENEWAL_NOTIFICATION_EVENTS.get(1),
                RENEWAL_NOTIFICATION_EVENTS.get(2),
                RENEWAL_NOTIFICATION_EVENTS.get(3),
                RENEWAL_NOTIFICATION_EVENTS.get(4),
                RENEWAL_NOTIFICATION_EVENTS.get(5),
                contractId,
                String.valueOf(contractId)
        );
    }

    private void deleteRoomTransferNotifications(Long requestId) {
        jdbcTemplate.update("""
                        DELETE FROM notification_deliveries
                        WHERE outbox_id IN (
                            SELECT notification_outbox_id
                            FROM notification_outbox
                            WHERE event_type IN (?, ?, ?, ?)
                              AND (
                                    (target_type = 'ROOM_TRANSFER' AND target_id = ?)
                                    OR JSON_UNQUOTE(JSON_EXTRACT(payload, '$.requestId')) = ?
                                  )
                        )
                        """,
                ROOM_TRANSFER_NOTIFICATION_EVENTS.get(0),
                ROOM_TRANSFER_NOTIFICATION_EVENTS.get(1),
                ROOM_TRANSFER_NOTIFICATION_EVENTS.get(2),
                ROOM_TRANSFER_NOTIFICATION_EVENTS.get(3),
                requestId,
                String.valueOf(requestId)
        );
        jdbcTemplate.update("""
                        DELETE FROM notification_outbox
                        WHERE event_type IN (?, ?, ?, ?)
                          AND (
                                (target_type = 'ROOM_TRANSFER' AND target_id = ?)
                                OR JSON_UNQUOTE(JSON_EXTRACT(payload, '$.requestId')) = ?
                              )
                        """,
                ROOM_TRANSFER_NOTIFICATION_EVENTS.get(0),
                ROOM_TRANSFER_NOTIFICATION_EVENTS.get(1),
                ROOM_TRANSFER_NOTIFICATION_EVENTS.get(2),
                ROOM_TRANSFER_NOTIFICATION_EVENTS.get(3),
                requestId,
                String.valueOf(requestId)
        );
    }

    private void prepareRoomTransferContractEligibility(
            Long contractId,
            int monthsStayed,
            LocalDate startDate,
            LocalDate endDate
    ) {
        requireContract(contractId);
        LocalDate resolvedStartDate = startDate == null
                ? LocalDate.now().minusMonths(Math.max(monthsStayed, 13))
                : startDate;
        LocalDate resolvedEndDate = endDate == null
                ? LocalDate.now().plusMonths(2)
                : endDate;
        jdbcTemplate.update("""
                        UPDATE lease_contracts
                        SET start_date = ?,
                            rent_start_date = ?,
                            end_date = ?,
                            status = 'ACTIVE',
                            updated_at = NOW(6)
                        WHERE lease_contract_id = ?
                        """,
                resolvedStartDate,
                resolvedStartDate,
                resolvedEndDate,
                contractId
        );
        jdbcTemplate.update("""
                        UPDATE contract_occupants
                        SET move_in_date = ?
                        WHERE contract_id = ?
                          AND status = 'ACTIVE'
                        """,
                resolvedStartDate,
                contractId
        );
    }

    private MockRoomTransferStateResponse buildRoomTransferState(Long requestId) {
        RoomTransferRequest request = roomTransferUseCase.getTransferRequestById(requestId);
        Map<String, Object> transfer = safeQueryOne("""
                        SELECT
                            r.room_transfer_request_id AS id,
                            r.request_code AS requestCode,
                            r.status,
                            r.requester_id AS requesterTenantId,
                            tenant.user_id AS requesterUserId,
                            r.old_contract_id AS oldContractId,
                            old_contract.contract_code AS oldContractCode,
                            r.old_room_id AS oldRoomId,
                            old_room.room_code AS oldRoomCode,
                            old_room.name AS oldRoomName,
                            r.target_room_id AS targetRoomId,
                            target_room.room_code AS targetRoomCode,
                            target_room.name AS targetRoomName,
                            r.transferring_tenant_profile_ids AS transferringTenantProfileIds,
                            r.nominated_holder_profile_id AS nominatedHolderProfileId,
                            r.target_transfer_type AS targetTransferType,
                            r.target_contract_id AS targetContractId,
                            r.new_contract_id AS newContractId,
                            r.replacement_old_contract_id AS replacementOldContractId,
                            r.requested_transfer_date AS requestedTransferDate,
                            r.reserved_slots AS reservedSlots,
                            r.reservation_expires_at AS reservationExpiresAt,
                            r.target_holder_approved_by AS targetHolderApprovedBy,
                            r.target_holder_approved_at AS targetHolderApprovedAt,
                            r.target_holder_rejected_at AS targetHolderRejectedAt,
                            r.approved_by AS approvedBy,
                            r.approved_at AS approvedAt,
                            r.executed_at AS executedAt,
                            r.completed_at AS completedAt,
                            r.positive_difference_settlement_type AS positiveDifferenceSettlementType,
                            r.eligibility_checked_at AS eligibilityCheckedAt,
                            r.is_eligible_at_creation AS eligibleAtCreation,
                            r.eligibility_snapshot AS eligibilitySnapshot,
                            r.created_at AS createdAt,
                            r.updated_at AS updatedAt
                        FROM room_transfer_requests r
                        JOIN tenants tenant ON tenant.tenant_id = r.requester_id
                        JOIN lease_contracts old_contract ON old_contract.lease_contract_id = r.old_contract_id
                        JOIN rooms old_room ON old_room.room_id = r.old_room_id
                        JOIN rooms target_room ON target_room.room_id = r.target_room_id
                        WHERE r.room_transfer_request_id = ?
                        """,
                requestId
        );
        return new MockRoomTransferStateResponse(
                roomTransferWebMapper.toResponse(request),
                transfer,
                safeQueryRows("""
                                SELECT
                                    change_request_id AS id,
                                    request_code AS requestCode,
                                    request_type AS requestType,
                                    requester_id AS requesterId,
                                    requester_role AS requesterRole,
                                    target_type AS targetType,
                                    target_id AS targetId,
                                    title,
                                    assigned_role AS assignedRole,
                                    assigned_to AS assignedTo,
                                    status,
                                    resolution_note AS resolutionNote,
                                    resolved_by AS resolvedBy,
                                    resolved_at AS resolvedAt,
                                    created_at AS createdAt,
                                    updated_at AS updatedAt
                                FROM change_requests
                                WHERE request_type = 'ROOM_TRANSFER'
                                  AND target_id = ?
                                ORDER BY change_request_id DESC
                                """,
                        requestId
                ),
                safeQueryRows("""
                                SELECT
                                    transfer_settlement_id AS id,
                                    transfer_request_id AS transferRequestId,
                                    old_room_remaining_value AS oldRoomRemainingValue,
                                    new_room_required_value AS newRoomRequiredValue,
                                    difference_amount AS differenceAmount,
                                    settlement_type AS settlementType,
                                    old_room_final_invoice_id AS oldRoomFinalInvoiceId,
                                    transfer_difference_invoice_id AS transferDifferenceInvoiceId,
                                    confirmed_by AS confirmedBy,
                                    confirmed_at AS confirmedAt,
                                    created_at AS createdAt
                                FROM transfer_settlements
                                WHERE transfer_request_id = ?
                                ORDER BY transfer_settlement_id DESC
                                """,
                        requestId
                ),
                safeQueryRows("""
                                SELECT
                                    d.deposit_transfer_record_id AS id,
                                    d.transfer_request_id AS transferRequestId,
                                    d.old_contract_id AS oldContractId,
                                    d.new_contract_id AS newContractId,
                                    d.old_deposit_agreement_id AS oldDepositAgreementId,
                                    d.from_room_id AS fromRoomId,
                                    d.to_room_id AS toRoomId,
                                    d.amount,
                                    d.status,
                                    d.effective_date AS effectiveDate,
                                    d.cancelled_at AS cancelledAt,
                                    d.note,
                                    d.created_at AS createdAt,
                                    d.updated_at AS updatedAt
                                FROM deposit_transfer_records d
                                WHERE d.transfer_request_id = ?
                                ORDER BY d.deposit_transfer_record_id DESC
                                """,
                        requestId
                ),
                safeQueryRows("""
                                SELECT DISTINCT
                                    i.invoice_id AS id,
                                    i.invoice_code AS invoiceCode,
                                    i.invoice_type AS invoiceType,
                                    i.invoice_reason AS invoiceReason,
                                    i.lease_contract_id AS contractId,
                                    i.room_id AS roomId,
                                    i.billing_period AS billingPeriod,
                                    i.status,
                                    i.subtotal_amount AS subtotalAmount,
                                    i.total_amount AS totalAmount,
                                    i.paid_amount AS paidAmount,
                                    i.remaining_amount AS remainingAmount,
                                    i.issue_date AS issueDate,
                                    i.due_date AS dueDate,
                                    i.issued_at AS issuedAt
                                FROM invoices i
                                JOIN transfer_settlements ts
                                  ON ts.old_room_final_invoice_id = i.invoice_id
                                  OR ts.transfer_difference_invoice_id = i.invoice_id
                                WHERE ts.transfer_request_id = ?
                                ORDER BY i.invoice_id DESC
                                """,
                        requestId
                ),
                safeQueryRows("""
                                SELECT
                                    notification_outbox_id AS id,
                                    event_type AS eventType,
                                    target_type AS targetType,
                                    target_id AS targetId,
                                    recipient_user_id AS recipientUserId,
                                    channel,
                                    title,
                                    body,
                                    status,
                                    retry_count AS retryCount,
                                    next_retry_at AS nextRetryAt,
                                    last_error AS lastError,
                                    is_read AS isRead,
                                    created_at AS createdAt,
                                    sent_at AS sentAt,
                                    payload
                                FROM notification_outbox
                                WHERE event_type IN (?, ?, ?, ?)
                                  AND (
                                        (target_type = 'ROOM_TRANSFER' AND target_id = ?)
                                        OR JSON_UNQUOTE(JSON_EXTRACT(payload, '$.requestId')) = ?
                                      )
                                ORDER BY notification_outbox_id DESC
                                LIMIT 50
                                """,
                        ROOM_TRANSFER_NOTIFICATION_EVENTS.get(0),
                        ROOM_TRANSFER_NOTIFICATION_EVENTS.get(1),
                        ROOM_TRANSFER_NOTIFICATION_EVENTS.get(2),
                        ROOM_TRANSFER_NOTIFICATION_EVENTS.get(3),
                        requestId,
                        String.valueOf(requestId)
                )
        );
    }

    private Map<String, Object> buildRoomTransferContractEligibilityState(Long contractId) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("contract", safeQueryOne("""
                        SELECT
                            lc.lease_contract_id AS id,
                            lc.contract_code AS contractCode,
                            lc.status,
                            lc.room_id AS roomId,
                            lc.primary_tenant_profile_id AS primaryTenantProfileId,
                            lc.start_date AS startDate,
                            lc.rent_start_date AS rentStartDate,
                            lc.end_date AS endDate,
                            DATE_ADD(lc.start_date, INTERVAL 12 MONTH) AS twelveMonthEligibleAt,
                            CURRENT_DATE >= DATE_ADD(lc.start_date, INTERVAL 12 MONTH) AS twelveMonthSatisfied,
                            DATEDIFF(CURRENT_DATE, lc.start_date) + 1 AS stayedDays,
                            DATEDIFF(lc.end_date, lc.start_date) + 1 AS totalContractDays,
                            CEIL((DATEDIFF(lc.end_date, lc.start_date) + 1) * 2 / 3) AS twoThirdsRequiredDays,
                            DATEDIFF(CURRENT_DATE, lc.start_date) + 1 >= CEIL((DATEDIFF(lc.end_date, lc.start_date) + 1) * 2 / 3) AS twoThirdsSatisfied
                        FROM lease_contracts lc
                        WHERE lc.lease_contract_id = ?
                        """,
                contractId
        ));
        state.put("activeOccupants", safeQueryRows("""
                        SELECT
                            co.contract_occupant_id AS id,
                            co.contract_id AS contractId,
                            co.tenant_profile_id AS tenantProfileId,
                            pp.user_id AS userId,
                            pp.full_name AS fullName,
                            co.role,
                            co.status,
                            co.move_in_date AS moveInDate,
                            DATE_ADD(co.move_in_date, INTERVAL 12 MONTH) AS twelveMonthEligibleAt
                        FROM contract_occupants co
                        JOIN person_profiles pp ON pp.person_profile_id = co.tenant_profile_id
                        WHERE co.contract_id = ?
                          AND co.status = 'ACTIVE'
                        ORDER BY co.contract_occupant_id ASC
                        """,
                contractId
        ));
        return state;
    }

    private MockLeaseRenewalStateResponse buildLeaseRenewalState(Long contractId) {
        Map<String, Object> contract = jdbcTemplate.queryForMap("""
                        SELECT
                            lease_contract_id AS id,
                            contract_code AS contractCode,
                            status,
                            room_id AS roomId,
                            primary_tenant_profile_id AS primaryTenantProfileId,
                            (
                                SELECT pp.user_id
                                FROM person_profiles pp
                                WHERE pp.person_profile_id = lease_contracts.primary_tenant_profile_id
                            ) AS primaryTenantUserId,
                            end_date AS endDate,
                            tenant_intention AS tenantIntention,
                            expected_vacant_date AS expectedVacantDate,
                            intention_recorded_at AS intentionRecordedAt
                        FROM lease_contracts
                        WHERE lease_contract_id = ?
                        """,
                contractId
        );
        List<Map<String, Object>> reminderTrackers = jdbcTemplate.queryForList("""
                        SELECT
                            reminder_tracker_id AS id,
                            reminder_key AS reminderKey,
                            audience,
                            recipient_user_id AS recipientUserId,
                            status,
                            sent_count AS sentCount,
                            last_sent_at AS lastSentAt,
                            next_due_at AS nextDueAt,
                            completed_at AS completedAt,
                            related_task_id AS relatedTaskId,
                            metadata,
                            created_at AS createdAt,
                            updated_at AS updatedAt
                        FROM reminder_trackers
                        WHERE target_type = 'CONTRACT'
                          AND target_id = ?
                        ORDER BY reminder_tracker_id DESC
                        """,
                contractId
        );
        List<Map<String, Object>> managerTasks = jdbcTemplate.queryForList("""
                        SELECT
                            manager_task_id AS id,
                            title,
                            task_type AS taskType,
                            idempotency_key AS idempotencyKey,
                            assignee_id AS assigneeId,
                            status,
                            due_date AS dueDate,
                            created_at AS createdAt,
                            updated_at AS updatedAt
                        FROM manager_tasks
                        WHERE lease_contract_id = ?
                          AND task_type IN (?, ?, ?)
                        ORDER BY manager_task_id DESC
                        """,
                contractId,
                RENEWAL_TASK_TYPES.get(0),
                RENEWAL_TASK_TYPES.get(1),
                RENEWAL_TASK_TYPES.get(2)
        );
        List<Map<String, Object>> notifications = jdbcTemplate.queryForList("""
                        SELECT
                            notification_outbox_id AS id,
                            event_type AS eventType,
                            target_type AS targetType,
                            target_id AS targetId,
                            recipient_user_id AS recipientUserId,
                            channel,
                            title,
                            status,
                            retry_count AS retryCount,
                            next_retry_at AS nextRetryAt,
                            last_error AS lastError,
                            is_read AS isRead,
                            created_at AS createdAt,
                            sent_at AS sentAt,
                            payload
                        FROM notification_outbox
                        WHERE event_type IN (?, ?, ?, ?, ?, ?)
                          AND (
                                (target_type = 'CONTRACT' AND target_id = ?)
                                OR JSON_UNQUOTE(JSON_EXTRACT(payload, '$.contractId')) = ?
                              )
                        ORDER BY notification_outbox_id DESC
                        LIMIT 30
                        """,
                RENEWAL_NOTIFICATION_EVENTS.get(0),
                RENEWAL_NOTIFICATION_EVENTS.get(1),
                RENEWAL_NOTIFICATION_EVENTS.get(2),
                RENEWAL_NOTIFICATION_EVENTS.get(3),
                RENEWAL_NOTIFICATION_EVENTS.get(4),
                RENEWAL_NOTIFICATION_EVENTS.get(5),
                contractId,
                String.valueOf(contractId)
        );
        return new MockLeaseRenewalStateResponse(
                normalizeDateValues(contract),
                reminderTrackers.stream().map(this::normalizeDateValues).toList(),
                managerTasks.stream().map(this::normalizeDateValues).toList(),
                notifications.stream().map(this::normalizeDateValues).toList(),
                List.of(
                        buildSchedulerStateIfExists("SYSTEM_JOB:CONTRACT_LIFECYCLE_SCAN"),
                        buildSchedulerStateIfExists("SYSTEM_JOB:NOTIFICATION_OUTBOX_DISPATCH")
                ).stream().filter(row -> !row.isEmpty()).toList()
        );
    }

    private Map<String, Object> buildSchedulerStateIfExists(String idempotencyKey) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                        SELECT
                            scheduled_task_id AS id,
                            task_type AS taskType,
                            target_type AS targetType,
                            target_id AS targetId,
                            due_at AS dueAt,
                            status,
                            retry_count AS retryCount,
                            idempotency_key AS idempotencyKey,
                            recurring,
                            schedule_expression AS scheduleExpression,
                            last_error AS lastError,
                            claimed_at AS claimedAt,
                            claimed_by AS claimedBy,
                            lock_until AS lockUntil,
                            executed_at AS executedAt,
                            created_at AS createdAt
                        FROM scheduled_tasks
                        WHERE idempotency_key = ?
                        LIMIT 1
                        """,
                idempotencyKey
        );
        if (rows.isEmpty()) {
            return Map.of();
        }
        return normalizeDateValues(rows.get(0));
    }

    private Map<String, Object> normalizeDateValues(Map<String, Object> row) {
        row.replaceAll((key, value) -> value instanceof Date date ? date.toLocalDate() : value);
        return row;
    }

    private Map<String, Object> safeQueryOne(String sql, Object... args) {
        List<Map<String, Object>> rows = safeQueryRows(sql, args);
        if (rows.isEmpty()) {
            return Map.of();
        }
        return rows.get(0);
    }

    private List<Map<String, Object>> safeQueryRows(String sql, Object... args) {
        try {
            return jdbcTemplate.queryForList(sql, args).stream()
                    .map(this::normalizeDateValues)
                    .toList();
        } catch (RuntimeException exception) {
            return List.of(Map.of("error", exception.getClass().getSimpleName() + ": " + exception.getMessage()));
        }
    }

    public record MockLeaseRenewalStateResponse(
            Map<String, Object> contract,
            List<Map<String, Object>> reminderTrackers,
            List<Map<String, Object>> managerTasks,
            List<Map<String, Object>> notifications,
            List<Map<String, Object>> schedulerTasks
    ) {
    }

    public record MockRoomTransferStateResponse(
            RoomTransferResponse request,
            Map<String, Object> transfer,
            List<Map<String, Object>> changeRequests,
            List<Map<String, Object>> settlements,
            List<Map<String, Object>> depositTransfers,
            List<Map<String, Object>> invoices,
            List<Map<String, Object>> notifications
    ) {
    }
}
