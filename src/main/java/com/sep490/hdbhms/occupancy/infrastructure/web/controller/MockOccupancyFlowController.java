package com.sep490.hdbhms.occupancy.infrastructure.web.controller;

import com.sep490.hdbhms.occupancy.application.port.in.usecase.RoomTransferUseCase;
import com.sep490.hdbhms.notification.infrastructure.dispatcher.NotificationOutboxDispatcher;
import com.sep490.hdbhms.occupancy.application.service.LeaseContractLifecycleService;
import com.sep490.hdbhms.occupancy.application.service.LeaseContractManagementService;
import com.sep490.hdbhms.occupancy.domain.model.RoomTransferRequest;
import com.sep490.hdbhms.occupancy.domain.value_objects.LeaseStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeaseContractEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaLeaseContractRepository;
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

    private List<Map<String, Object>> safeQueryRows(String sql) {
        try {
            return jdbcTemplate.queryForList(sql).stream()
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
}
