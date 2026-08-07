package com.sep490.hdbhms.occupancy.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.ManagerTaskStatus;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.ManagerTaskEntity;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaManagerTaskRepository;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.UserEntity;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaUserRepository;
import com.sep490.hdbhms.notification.application.service.BusinessNotificationPublisher;
import com.sep490.hdbhms.occupancy.application.port.out.LeaseContractRepository;
import com.sep490.hdbhms.occupancy.domain.model.LeaseContract;
import com.sep490.hdbhms.occupancy.domain.value_objects.ReminderTrackerStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeaseContractEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.ReminderTrackerEntity;
import com.sep490.hdbhms.property.infrastructure.persistence.entity.RoomEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaReminderTrackerRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LeaseExpiryReminderService {
    private static final String CONTRACT_TARGET = "CONTRACT";
    private static final String MANAGER_TASK_TARGET = "MANAGER_TASK";
    private static final String PRIMARY_TENANT_AUDIENCE = "PRIMARY_TENANT";
    private static final String PROPERTY_MANAGER_AUDIENCE = "PROPERTY_MANAGER";

    private static final String LEASE_EXPIRY_INTENTION = "LEASE_EXPIRY_INTENTION";
    private static final String LEASE_HANDOVER_CONFIRMATION = "LEASE_HANDOVER_CONFIRMATION";

    private static final String RENEWAL_TERMS_TASK = "LEASE_RENEWAL_TERMS_CONFIRMATION";
    private static final String HANDOVER_TASK = "LEASE_HANDOVER_CONFIRMATION";
    private static final String MANAGER_VISIT_TASK = "LEASE_EXPIRY_MANAGER_VISIT";

    private static final int REMINDER_SPACING_DAYS = 30;
    private static final int HANDOVER_WINDOW_DAYS = 14;

    JpaReminderTrackerRepository reminderTrackerRepository;
    LeaseContractRepository leaseContractRepository;
    JpaManagerTaskRepository managerTaskRepository;
    JpaUserRepository userRepository;
    BusinessNotificationPublisher notificationPublisher;
    JdbcTemplate jdbcTemplate;
    ObjectMapper objectMapper;

    @Transactional
    public void processContract(LeaseContract contract, LocalDate today, boolean hasActivatedRenewal) {
        if (contract == null || contract.getId() == null || contract.getEndDate() == null) {
            return;
        }
        contract = loadReminderContract(contract);
        if (hasActivatedRenewal) {
            completeLeaseExpiryIntention(contract.getId());
            return;
        }

        String intention = normalize(contract.getTenantIntention());
        if (intention == null) {
            processIntentionReminder(contract, today);
            ensureHandoverTaskIfDue(contract, today, "Khách chưa phản hồi ý định khi hợp đồng sắp hết hạn.");
            return;
        }

        completeLeaseExpiryIntention(contract.getId());
        if ("RENEW".equals(intention)) {
            ensureRenewalTermsTask(contract, today);
        } else if ("MOVE_OUT".equals(intention) || "TRANSFER".equals(intention)) {
            ensureHandoverTaskIfDue(contract, today, "Khách đã chọn " + intentionLabel(intention) + ".");
        }
    }

    @Transactional
    public void onTenantIntentionRecorded(Long contractId, LocalDate today) {
        if (contractId == null) {
            return;
        }
        LeaseContract contract = leaseContractRepository.findById(contractId).orElse(null);
        if (contract == null) {
            return;
        }
        completeLeaseExpiryIntention(contract.getId());
        String intention = normalize(contract.getTenantIntention());
        if ("RENEW".equals(intention)) {
            ensureRenewalTermsTask(contract, today);
        } else if ("MOVE_OUT".equals(intention) || "TRANSFER".equals(intention)) {
            ensureHandoverTaskIfDue(contract, today, "Khách đã chọn " + intentionLabel(intention) + ".");
        }
    }

    private void processIntentionReminder(LeaseContract contract, LocalDate today) {
        LocalDate firstReminderDate = contract.getEndDate().minusMonths(3);
        if (today.isBefore(firstReminderDate)) {
            return;
        }

        Long recipientUserId = primaryTenantUserId(contract);
        if (recipientUserId == null) {
            log.warn("Skip lease expiry reminder because primary tenant has no user. contractId={}", contract.getId());
            return;
        }

        ReminderTrackerEntity tracker = findActiveTracker(
                LEASE_EXPIRY_INTENTION,
                contract.getId(),
                PRIMARY_TENANT_AUDIENCE,
                recipientUserId
        );
        if (tracker == null) {
            tracker = reminderTrackerRepository.save(ReminderTrackerEntity.builder()
                    .reminderKey(LEASE_EXPIRY_INTENTION)
                    .targetType(CONTRACT_TARGET)
                    .targetId(contract.getId())
                    .audience(PRIMARY_TENANT_AUDIENCE)
                    .recipientUser(UserEntity.builder().id(recipientUserId).build())
                    .status(ReminderTrackerStatus.ACTIVE)
                    .sentCount(0)
                    .nextDueAt(firstReminderDate.atStartOfDay())
                    .metadata(metadata(contract, "PENDING"))
                    .build());
        }

        if (!isReminderDue(tracker, today)) {
            return;
        }

        int sentCount = tracker.getSentCount() == null ? 0 : tracker.getSentCount();
        if (sentCount >= 3) {
            return;
        }

        ReminderStage stage = ReminderStage.fromSentCount(sentCount);
        notificationPublisher.publish(
                stage.eventType(),
                recipientUserId,
                CONTRACT_TARGET,
                contract.getId(),
                notificationData(contract, stage.name(), today)
        );

        LocalDateTime now = LocalDateTime.now();
        tracker.setSentCount(sentCount + 1);
        tracker.setLastSentAt(now);
        tracker.setMetadata(metadata(contract, stage.name()));

        if (stage == ReminderStage.FINAL) {
            TaskCreation taskCreation = ensureManagerTask(
                    MANAGER_VISIT_TASK,
                    "Cần gặp trực tiếp khách về hợp đồng sắp hết hạn",
                    "Khách chưa phản hồi sau 3 lần nhắc về ý định hợp đồng. Cần gặp trực tiếp để chốt gia hạn, chuyển phòng hoặc chuyển đi.",
                    contract,
                    today.plusDays(1)
            );
            tracker.setRelatedTask(taskCreation.task());
            tracker.setNextDueAt(null);
            publishManagerNotification(
                    "LEASE_EXPIRY_MANAGER_VISIT_REQUIRED",
                    taskCreation.task(),
                    contract,
                    "Khách chưa phản hồi sau 3 lần nhắc.",
                    today
            );

            //TODO: Add release room logic when reached final reminder
//            contract.getRoom().setCurrentStatus(RoomStatus.SOON_VACANT);
        } else {
            tracker.setNextDueAt(today.plusDays(REMINDER_SPACING_DAYS).atStartOfDay());
        }
        reminderTrackerRepository.save(tracker);
    }

    private void ensureRenewalTermsTask(LeaseContract contract, LocalDate today) {
        TaskCreation taskCreation = ensureManagerTask(
                RENEWAL_TERMS_TASK,
                "Chốt điều khoản gia hạn hợp đồng",
                "Khách đã chọn gia hạn hợp đồng. Cần chốt giá, thời hạn, tiền cọc và lịch ký.",
                contract,
                today.plusDays(7)
        );
        if (taskCreation.created()) {
            publishManagerNotification(
                    "LEASE_RENEWAL_TERMS_CONFIRMATION_DUE",
                    taskCreation.task(),
                    contract,
                    "Khách đã chọn gia hạn hợp đồng.",
                    today
            );
        }
    }

    private void ensureHandoverTaskIfDue(LeaseContract contract, LocalDate today, String reason) {
        LocalDate handoverDate = contract.getExpectedVacantDate() != null
                ? contract.getExpectedVacantDate()
                : contract.getEndDate();
        if (handoverDate == null || ChronoUnit.DAYS.between(today, handoverDate) > HANDOVER_WINDOW_DAYS) {
            return;
        }

        ReminderTrackerEntity tracker = findActiveTracker(
                LEASE_HANDOVER_CONFIRMATION,
                contract.getId(),
                PROPERTY_MANAGER_AUDIENCE,
                null
        );
        if (tracker != null && tracker.getRelatedTask() != null) {
            return;
        }

        TaskCreation taskCreation = ensureManagerTask(
                HANDOVER_TASK,
                "Chốt lịch bàn giao phòng",
                "Cần chốt ngày giờ bàn giao và người xuống kiểm tra phòng. " + reason,
                contract,
                today.plusDays(1)
        );
        if (tracker == null) {
            tracker = ReminderTrackerEntity.builder()
                    .reminderKey(LEASE_HANDOVER_CONFIRMATION)
                    .targetType(CONTRACT_TARGET)
                    .targetId(contract.getId())
                    .audience(PROPERTY_MANAGER_AUDIENCE)
                    .status(ReminderTrackerStatus.ACTIVE)
                    .sentCount(0)
                    .metadata(metadata(contract, "HANDOVER"))
                    .build();
        }
        tracker.setRelatedTask(taskCreation.task());
        tracker.setNextDueAt(null);
        reminderTrackerRepository.save(tracker);

        if (taskCreation.created()) {
            publishManagerNotification("LEASE_HANDOVER_CONFIRMATION_DUE", taskCreation.task(), contract, reason, today);
        }
    }

    private TaskCreation ensureManagerTask(
            String taskType,
            String title,
            String description,
            LeaseContract contract,
            LocalDate dueDate
    ) {
        String idempotencyKey = taskType + ":CONTRACT:" + contract.getId();
        return managerTaskRepository.findFirstByIdempotencyKey(idempotencyKey)
                .map(task -> new TaskCreation(task, false))
                .orElseGet(() -> new TaskCreation(managerTaskRepository.save(ManagerTaskEntity.builder()
                                .title(title)
                                .description(description)
                                .taskType(taskType)
                                .idempotencyKey(idempotencyKey)
                                .assignee(resolveTaskAssignee(contract))
                                .room(contract.getRoomId() == null ? null : RoomEntity.builder().id(contract.getRoomId()).build())
                                .leaseContract(LeaseContractEntity.builder().id(contract.getId()).build())
                                .status(ManagerTaskStatus.PENDING)
                                .dueDate(dueDate)
                                .build()),
                        true));
    }

    private void publishManagerNotification(
            String eventType,
            ManagerTaskEntity task,
            LeaseContract contract,
            String reason,
            LocalDate today
    ) {
        for (Long recipientId : managerRecipientIds(contract)) {
            notificationPublisher.publish(
                    eventType,
                    recipientId,
                    MANAGER_TASK_TARGET,
                    task.getId(),
                    managerNotificationData(contract, task, reason, today)
            );
        }
    }

    private List<Long> managerRecipientIds(LeaseContract contract) {
        Long propertyId = propertyId(contract);
        if (propertyId != null) {
            List<Long> managerIds = jdbcTemplate.queryForList("""
                            SELECT staff_user_id
                            FROM property_staff_assignments
                            WHERE property_id = ?
                              AND assignment_status = 'ACTIVE'
                              AND assigned_role = 'MANAGER'
                            ORDER BY is_primary DESC, property_staff_assignment_id
                            """,
                    Long.class,
                    propertyId
            );
            if (!managerIds.isEmpty()) {
                return managerIds;
            }
        }
        return userRepository.findFirstByRoleAndDeletedAtIsNullOrderByIdAsc(Role.OWNER)
                .map(user -> List.of(user.getId()))
                .orElseGet(List::of);
    }

    private UserEntity resolveTaskAssignee(LeaseContract contract) {
        List<Long> recipientIds = managerRecipientIds(contract);
        if (recipientIds.isEmpty()) {
            return null;
        }
        return userRepository.findById(recipientIds.getFirst()).orElse(null);
    }

    private ReminderTrackerEntity findActiveTracker(
            String reminderKey,
            Long targetId,
            String audience,
            Long recipientUserId
    ) {
        return reminderTrackerRepository.findActiveTrackers(
                        reminderKey,
                        LeaseExpiryReminderService.CONTRACT_TARGET,
                        targetId,
                        audience,
                        recipientUserId,
                        ReminderTrackerStatus.ACTIVE,
                        PageRequest.of(0, 1)
                )
                .stream()
                .findFirst()
                .orElse(null);
    }

    private boolean isReminderDue(ReminderTrackerEntity tracker, LocalDate today) {
        if (tracker.getNextDueAt() == null) {
            return true;
        }
        return !today.isBefore(tracker.getNextDueAt().toLocalDate());
    }

    private LeaseContract loadReminderContract(LeaseContract contract) {
        return leaseContractRepository.findById(contract.getId()).orElse(contract);
    }

    private void completeLeaseExpiryIntention(Long contractId) {
        reminderTrackerRepository.completeActiveTrackers(
                LEASE_EXPIRY_INTENTION,
                CONTRACT_TARGET,
                contractId,
                ReminderTrackerStatus.ACTIVE,
                ReminderTrackerStatus.COMPLETED,
                LocalDateTime.now()
        );
    }

    private Long primaryTenantUserId(LeaseContract contract) {
        return reminderContext(contract).primaryTenantUserId();
    }

    private Long propertyId(LeaseContract contract) {
        return reminderContext(contract).propertyId();
    }

    private Map<String, Object> notificationData(LeaseContract contract, String stage, LocalDate today) {
        Map<String, Object> data = baseNotificationData(contract, today);
        data.put("stage", stage);
        data.put("targetRoute", "/contract");
        return data;
    }

    private Map<String, Object> managerNotificationData(
            LeaseContract contract,
            ManagerTaskEntity task,
            String reason,
            LocalDate today
    ) {
        Map<String, Object> data = baseNotificationData(contract, today);
        data.put("taskId", task.getId());
        data.put("dueDate", String.valueOf(task.getDueDate()));
        data.put("reason", reason);
        data.put("targetRoute", "/dashboard/contracts/" + contract.getId());
        return data;
    }

    private Map<String, Object> baseNotificationData(LeaseContract contract, LocalDate today) {
        LeaseReminderContext context = reminderContext(contract);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("contractId", contract.getId());
        data.put("contractCode", contract.getContractCode());
        data.put("roomId", context.roomId());
        data.put("roomName", context.roomName());
        data.put("roomCode", context.roomCode());
        data.put("propertyName", context.propertyName());
        data.put("endDate", String.valueOf(contract.getEndDate()));
        data.put("daysRemaining", ChronoUnit.DAYS.between(today, contract.getEndDate()));
        return data;
    }

    private String metadata(LeaseContract contract, String stage) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "endDate", String.valueOf(contract.getEndDate()),
                    "firstReminderDate", String.valueOf(contract.getEndDate().minusMonths(3)),
                    "lastReminderStage", stage
            ));
        } catch (JsonProcessingException exception) {
            return null;
        }
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase();
    }

    private String intentionLabel(String intention) {
        return switch (intention) {
            case "RENEW" -> "gia hạn hợp đồng";
            case "TRANSFER" -> "chuyển phòng";
            case "MOVE_OUT" -> "chuyển đi";
            default -> intention;
        };
    }

    private LeaseReminderContext reminderContext(LeaseContract contract) {
        if (contract == null || contract.getId() == null) {
            return LeaseReminderContext.empty();
        }
        return jdbcTemplate.query("""
                        SELECT
                            lc.room_id,
                            room.room_code,
                            room.name AS room_name,
                            property.property_id,
                            property.name AS property_name,
                            user_account.user_id AS primary_tenant_user_id
                        FROM lease_contracts lc
                        LEFT JOIN rooms room ON room.room_id = lc.room_id
                        LEFT JOIN properties property ON property.property_id = room.property_id
                        LEFT JOIN person_profiles profile ON profile.person_profile_id = lc.primary_tenant_profile_id
                        LEFT JOIN users user_account ON user_account.user_id = profile.user_id
                        WHERE lc.lease_contract_id = ?
                        LIMIT 1
                        """,
                rs -> {
                    if (!rs.next()) {
                        return LeaseReminderContext.empty();
                    }
                    return new LeaseReminderContext(
                            rs.getObject("room_id", Long.class),
                            rs.getString("room_code"),
                            rs.getString("room_name"),
                            rs.getObject("property_id", Long.class),
                            rs.getString("property_name"),
                            rs.getObject("primary_tenant_user_id", Long.class)
                    );
                },
                contract.getId()
        );
    }

    private record LeaseReminderContext(
            Long roomId,
            String roomCode,
            String roomName,
            Long propertyId,
            String propertyName,
            Long primaryTenantUserId
    ) {
        private static LeaseReminderContext empty() {
            return new LeaseReminderContext(null, "", "", null, "", null);
        }
    }

    private enum ReminderStage {
        FIRST("LEASE_EXPIRY_REMINDER_FIRST"),
        SECOND("LEASE_EXPIRY_REMINDER_SECOND"),
        FINAL("LEASE_EXPIRY_REMINDER_FINAL");

        private final String eventType;

        ReminderStage(String eventType) {
            this.eventType = eventType;
        }

        static ReminderStage fromSentCount(int sentCount) {
            if (sentCount <= 0) {
                return FIRST;
            }
            if (sentCount == 1) {
                return SECOND;
            }
            return FINAL;
        }

        String eventType() {
            return eventType;
        }
    }

    private record TaskCreation(ManagerTaskEntity task, boolean created) {
    }
}
