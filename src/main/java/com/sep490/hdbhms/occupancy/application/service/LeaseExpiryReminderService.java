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
import com.sep490.hdbhms.occupancy.domain.value_objects.ReminderTrackerStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeaseContractEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.ReminderTrackerEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RoomEntity;
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

    private static final int REMINDER_SPACING_DAYS = 14;
    private static final int HANDOVER_WINDOW_DAYS = 14;

    JpaReminderTrackerRepository reminderTrackerRepository;
    JpaManagerTaskRepository managerTaskRepository;
    JpaUserRepository userRepository;
    BusinessNotificationPublisher notificationPublisher;
    JdbcTemplate jdbcTemplate;
    ObjectMapper objectMapper;

    @Transactional
    public void processContract(LeaseContractEntity contract, LocalDate today, boolean hasActivatedRenewal) {
        if (contract == null || contract.getId() == null || contract.getEndDate() == null) {
            return;
        }
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
    public void onTenantIntentionRecorded(LeaseContractEntity contract, LocalDate today) {
        if (contract == null || contract.getId() == null) {
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

    private void processIntentionReminder(LeaseContractEntity contract, LocalDate today) {
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
                CONTRACT_TARGET,
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
                    "Khách chưa phản hồi sau 3 lần nhắc về ý định hợp đồng. Cần gặp trực tiếp để chốt tái ký, chuyển phòng hoặc chuyển đi.",
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
        } else {
            tracker.setNextDueAt(today.plusDays(REMINDER_SPACING_DAYS).atStartOfDay());
        }
        reminderTrackerRepository.save(tracker);
    }

    private void ensureRenewalTermsTask(LeaseContractEntity contract, LocalDate today) {
        TaskCreation taskCreation = ensureManagerTask(
                RENEWAL_TERMS_TASK,
                "Chốt điều khoản tái ký hợp đồng",
                "Khách đã chọn ký hợp đồng mới. Cần chốt giá, thời hạn, tiền cọc và lịch ký.",
                contract,
                today.plusDays(7)
        );
        if (taskCreation.created()) {
            publishManagerNotification(
                    "LEASE_RENEWAL_TERMS_CONFIRMATION_DUE",
                    taskCreation.task(),
                    contract,
                    "Khách đã chọn tái ký hợp đồng.",
                    today
            );
        }
    }

    private void ensureHandoverTaskIfDue(LeaseContractEntity contract, LocalDate today, String reason) {
        LocalDate handoverDate = contract.getExpectedVacantDate() != null
                ? contract.getExpectedVacantDate()
                : contract.getEndDate();
        if (handoverDate == null || ChronoUnit.DAYS.between(today, handoverDate) > HANDOVER_WINDOW_DAYS) {
            return;
        }

        ReminderTrackerEntity tracker = findActiveTracker(
                LEASE_HANDOVER_CONFIRMATION,
                CONTRACT_TARGET,
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
            LeaseContractEntity contract,
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
                                .room(contract.getRoom())
                                .leaseContract(contract)
                                .status(ManagerTaskStatus.PENDING)
                                .dueDate(dueDate)
                                .build()),
                        true));
    }

    private void publishManagerNotification(
            String eventType,
            ManagerTaskEntity task,
            LeaseContractEntity contract,
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

    private List<Long> managerRecipientIds(LeaseContractEntity contract) {
        Long propertyId = propertyId(contract);
        if (propertyId != null) {
            List<Long> managerIds = jdbcTemplate.queryForList("""
                            SELECT staff_user_id
                            FROM property_staff_assignments
                            WHERE property_id = ?
                              AND assignment_status = 'ACTIVE'
                              AND assigned_role = 'MANAGER'
                            ORDER BY is_primary DESC, property_staff_assignment_id ASC
                            """,
                    Long.class,
                    propertyId
            );
            if (!managerIds.isEmpty()) {
                return managerIds;
            }
        }
        return userRepository.findByRole(Role.OWNER)
                .map(user -> List.of(user.getId()))
                .orElseGet(List::of);
    }

    private UserEntity resolveTaskAssignee(LeaseContractEntity contract) {
        List<Long> recipientIds = managerRecipientIds(contract);
        if (recipientIds.isEmpty()) {
            return null;
        }
        return userRepository.findById(recipientIds.get(0)).orElse(null);
    }

    private ReminderTrackerEntity findActiveTracker(
            String reminderKey,
            String targetType,
            Long targetId,
            String audience,
            Long recipientUserId
    ) {
        return reminderTrackerRepository.findActiveTrackers(
                        reminderKey,
                        targetType,
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

    private Long primaryTenantUserId(LeaseContractEntity contract) {
        if (contract.getPrimaryTenantProfile() == null || contract.getPrimaryTenantProfile().getUser() == null) {
            return null;
        }
        return contract.getPrimaryTenantProfile().getUser().getId();
    }

    private Long propertyId(LeaseContractEntity contract) {
        RoomEntity room = contract.getRoom();
        if (room == null || room.getProperty() == null) {
            return null;
        }
        return room.getProperty().getId();
    }

    private Map<String, Object> notificationData(LeaseContractEntity contract, String stage, LocalDate today) {
        Map<String, Object> data = baseNotificationData(contract, today);
        data.put("stage", stage);
        data.put("targetRoute", "/contract");
        return data;
    }

    private Map<String, Object> managerNotificationData(
            LeaseContractEntity contract,
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

    private Map<String, Object> baseNotificationData(LeaseContractEntity contract, LocalDate today) {
        RoomEntity room = contract.getRoom();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("contractId", contract.getId());
        data.put("contractCode", contract.getContractCode());
        data.put("roomId", room == null ? null : room.getId());
        data.put("roomName", room == null ? "" : room.getName());
        data.put("roomCode", room == null ? "" : room.getRoomCode());
        data.put("propertyName", room == null || room.getProperty() == null ? "" : room.getProperty().getName());
        data.put("endDate", String.valueOf(contract.getEndDate()));
        data.put("daysRemaining", ChronoUnit.DAYS.between(today, contract.getEndDate()));
        return data;
    }

    private String metadata(LeaseContractEntity contract, String stage) {
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
            case "RENEW" -> "tái ký hợp đồng";
            case "TRANSFER" -> "chuyển phòng";
            case "MOVE_OUT" -> "chuyển đi";
            default -> intention;
        };
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
