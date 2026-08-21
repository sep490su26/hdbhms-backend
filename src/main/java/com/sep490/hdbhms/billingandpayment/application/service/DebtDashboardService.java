package com.sep490.hdbhms.billingandpayment.application.service;

import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceStatus;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceType;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.ManagerTaskStatus;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.DebtNoticeTrackerEntity;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.InvoiceEntity;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.ManagerTaskEntity;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaDebtNoticeTrackerRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaInvoiceRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaManagerTaskRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.response.DebtSummaryResponse;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.UserEntity;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaUserRepository;
import com.sep490.hdbhms.notification.application.port.in.usecase.SendNotificationUseCase;
import com.sep490.hdbhms.shared.event.NotificationEvent;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeaseContractEntity;
import com.sep490.hdbhms.property.infrastructure.persistence.entity.RoomEntity;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DebtDashboardService {
    static final int WARNING_MONTH_LIMIT = 3;
    static final int DIRECT_VISIT_NOTICE_LIMIT = 3;
    static final int TASK_REMINDER_DAYS = 3;
    static final String DIRECT_VISIT_EVENT = "DEBT_DIRECT_VISIT_REQUIRED";

    JpaInvoiceRepository invoiceRepository;
    JpaDebtNoticeTrackerRepository debtNoticeTrackerRepository;
    JpaManagerTaskRepository managerTaskRepository;
    JpaUserRepository userRepository;
    SendNotificationUseCase notificationUseCase;

    @Transactional(readOnly = true)
    public List<DebtSummaryResponse> getDebtSummary(Long propertyId) {
        return buildDebtSummaries(propertyId);
    }

    @Transactional
    public void processOverdueDebts() {
        List<DebtSummary> debts = buildDebtSummaryModels(null);
        if (debts.isEmpty()) {
            return;
        }

        LocalDate today = LocalDate.now();
        UserEntity assignee = resolveTaskAssignee().orElse(null);
        int taskCount = 0;
        for (DebtSummary debt : debts) {
            if (debt.contract == null || debt.contract.getId() == null) {
                continue;
            }
            DebtNoticeTrackerEntity tracker = debtNoticeTrackerRepository
                    .findByLeaseContract_Id(debt.contract.getId())
                    .orElseGet(() -> DebtNoticeTrackerEntity.builder()
                            .leaseContract(debt.contract)
                            .unresponsiveCount(0)
                            .build());

            int reminderStage = overdueReminderStage(debt, today);
            if (reminderStage == 0) {
                continue;
            }

            // The tracker follows the three monthly reminders, not the daily scheduler runs.
            if (safe(tracker.getUnresponsiveCount()) < reminderStage) {
                tracker.setUnresponsiveCount(reminderStage);
                tracker.setLastNoticeDate(reminderDate(debt, reminderStage));
                tracker = debtNoticeTrackerRepository.save(tracker);
            }

            if (reminderStage >= DIRECT_VISIT_NOTICE_LIMIT) {
                taskCount += createOrRemindDirectVisitTask(debt, assignee, today);
            }
        }
        if (taskCount > 0) {
            log.info("The debt scheduler created or reminded managers about {} tasks", taskCount);
        }
    }

    private int createOrRemindDirectVisitTask(DebtSummary debt, UserEntity assignee, LocalDate today) {
        Optional<ManagerTaskEntity> existingTask = managerTaskRepository
                .findFirstByLeaseContract_IdAndStatusOrderByIdDesc(debt.contract.getId(), ManagerTaskStatus.PENDING);

        if (existingTask.isPresent()) {
            ManagerTaskEntity task = existingTask.get();
            if (task.getDueDate() != null && task.getDueDate().isAfter(today)) {
                return 0;
            }
            task.setDueDate(today.plusDays(TASK_REMINDER_DAYS));
            managerTaskRepository.save(task);
            queueDirectVisitNotification(task, debt, assignee);
            return 1;
        }

        ManagerTaskEntity task = managerTaskRepository.save(ManagerTaskEntity.builder()
                .title("Khách phòng " + debt.roomName + " nợ quá hạn 3 lần, cần gặp trực tiếp")
                .description("Tổng nợ: " + debt.totalDebt + " VND. Loại nợ: " + debt.debtType + ".")
                .assignee(assignee)
                .room(debt.room)
                .leaseContract(debt.contract)
                .status(ManagerTaskStatus.PENDING)
                .dueDate(today.plusDays(TASK_REMINDER_DAYS))
                .build());
        queueDirectVisitNotification(task, debt, assignee);
        return 1;
    }

    private void queueDirectVisitNotification(ManagerTaskEntity task, DebtSummary debt, UserEntity assignee) {
        if (assignee == null || assignee.getId() == null) {
            return;
        }
        notificationUseCase.queueNotification(NotificationEvent.builder()
                .eventType(DIRECT_VISIT_EVENT)
                .userId(assignee.getId())
                .targetType("MANAGER_TASK")
                .targetId(task.getId())
                .data(Map.of(
                        "roomName", debt.roomName,
                        "propertyName", debt.propertyName,
                        "totalDebt", debt.totalDebt,
                        "dueDate", String.valueOf(task.getDueDate())
                ))
                .build());
    }

    private Optional<UserEntity> resolveTaskAssignee() {
        return userRepository.findFirstByRoleAndDeletedAtIsNullOrderByIdAsc(Role.OWNER)
                .or(() -> userRepository.findFirstByRoleAndDeletedAtIsNullOrderByIdAsc(Role.MANAGER));
    }

    private List<DebtSummaryResponse> buildDebtSummaries(Long propertyId) {
        return buildDebtSummaryModels(propertyId)
                .stream()
                .map(DebtSummary::toResponse)
                .toList();
    }

    private List<DebtSummary> buildDebtSummaryModels(Long propertyId) {
        Map<Long, DebtSummary> summaries = new LinkedHashMap<>();
        for (InvoiceEntity invoice : findDebtInvoices(propertyId)) {
            RoomEntity room = invoice.getRoom();
            if (room == null || room.getId() == null) {
                continue;
            }
            DebtSummary summary = summaries.computeIfAbsent(room.getId(), ignored -> DebtSummary.from(invoice));
            summary.addInvoice(invoice);
        }
        return summaries.values()
                .stream()
                .filter((summary) -> summary.totalDebt > 0)
                .toList();
    }

    private List<InvoiceEntity> findDebtInvoices(Long propertyId) {
        return invoiceRepository.findDebtDashboardInvoices(
                propertyId,
                List.of(InvoiceStatus.ISSUED, InvoiceStatus.OVERDUE),
                List.of(InvoiceType.RENT, InvoiceType.UTILITY)
        );
    }

    private int safe(Integer value) {
        return value == null ? 0 : value;
    }

    private static long safe(Long value) {
        return value == null ? 0L : value;
    }

    private int overdueReminderStage(DebtSummary debt, LocalDate today) {
        if (debt == null || debt.oldestDueDate == null || today == null) {
            return 0;
        }

        LocalDate firstReminderDate = debt.oldestDueDate.plusDays(1);
        if (today.isBefore(firstReminderDate)) {
            return 0;
        }
        if (today.isBefore(firstReminderDate.plusMonths(1))) {
            return 1;
        }
        if (today.isBefore(firstReminderDate.plusMonths(2))) {
            return 2;
        }
        return DIRECT_VISIT_NOTICE_LIMIT;
    }

    private LocalDate reminderDate(DebtSummary debt, int stage) {
        return debt.oldestDueDate.plusDays(1).plusMonths(stage - 1L);
    }

    private static class DebtSummary {
        Long propertyId;
        String propertyName;
        Long roomId;
        String roomName;
        String tenantName;
        RoomEntity room;
        LeaseContractEntity contract;
        long rentDebt;
        long utilityDebt;
        Set<String> rentPeriods = new HashSet<>();
        Set<String> utilityPeriods = new HashSet<>();
        long totalDebt;
        String debtType = "OTHER";
        LocalDate oldestDueDate;

        static DebtSummary from(InvoiceEntity invoice) {
            RoomEntity room = invoice.getRoom();
            DebtSummary summary = new DebtSummary();
            summary.room = room;
            summary.contract = invoice.getLeastContract();
            summary.roomId = room.getId();
            summary.roomName = room.getRoomCode() != null && !room.getRoomCode().isBlank()
                    ? room.getRoomCode()
                    : room.getName();
            summary.propertyId = invoice.getProperty() == null ? null : invoice.getProperty().getId();
            summary.propertyName = invoice.getProperty() == null ? "" : invoice.getProperty().getName();
            summary.tenantName = invoice.getLeastContract() == null
                    || invoice.getLeastContract().getPrimaryTenantProfile() == null
                    ? ""
                    : invoice.getLeastContract().getPrimaryTenantProfile().getFullName();
            summary.oldestDueDate = invoice.getDueDate() == null ? null : invoice.getDueDate().toLocalDate();
            return summary;
        }

        void addInvoice(InvoiceEntity invoice) {
            long amount = safe(invoice.getRemainingAmount());
            if (invoice.getInvoiceType() == InvoiceType.RENT) {
                rentDebt += amount;
                addPeriod(rentPeriods, invoice.getBillingPeriod());
            } else if (invoice.getInvoiceType() == InvoiceType.UTILITY) {
                utilityDebt += amount;
                addPeriod(utilityPeriods, invoice.getBillingPeriod());
            }
            totalDebt = rentDebt + utilityDebt;
            debtType = resolveDebtType();
            LocalDate dueDate = invoice.getDueDate() == null ? null : invoice.getDueDate().toLocalDate();
            if (dueDate != null && (oldestDueDate == null || dueDate.isBefore(oldestDueDate))) {
                oldestDueDate = dueDate;
            }
            if (contract == null && invoice.getLeastContract() != null) {
                contract = invoice.getLeastContract();
            }
        }

        DebtSummaryResponse toResponse() {
            int rawMonths = Math.max(rentPeriods.size(), utilityPeriods.size());
            return new DebtSummaryResponse(
                    propertyId,
                    propertyName,
                    roomId,
                    roomName,
                    tenantName,
                    rentDebt,
                    utilityDebt,
                    totalDebt,
                    Math.min(rawMonths, WARNING_MONTH_LIMIT),
                    debtType,
                    rawMonths >= WARNING_MONTH_LIMIT
            );
        }

        private String resolveDebtType() {
            if (rentDebt > 0 && utilityDebt > 0) {
                return "MIXED";
            }
            if (rentDebt > 0) {
                return "RENT";
            }
            if (utilityDebt > 0) {
                return "UTILITY";
            }
            return "OTHER";
        }

        private void addPeriod(Set<String> periods, String value) {
            if (value != null && !value.isBlank()) {
                periods.add(value);
            }
        }
    }
}
