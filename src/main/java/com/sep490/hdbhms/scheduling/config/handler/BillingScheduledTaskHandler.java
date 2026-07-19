package com.sep490.hdbhms.scheduling.config.handler;

import com.sep490.hdbhms.billingandpayment.application.service.BillingManagementService;
import com.sep490.hdbhms.billingandpayment.application.service.DebtDashboardService;
import com.sep490.hdbhms.billingandpayment.application.service.ScheduledBillingChargeService;
import com.sep490.hdbhms.billingandpayment.application.service.UtilityBillingRunService;
import com.sep490.hdbhms.scheduling.application.handler.ScheduledTaskHandler;
import com.sep490.hdbhms.scheduling.application.handler.ScheduledTaskPolicy;
import com.sep490.hdbhms.scheduling.domain.model.ScheduledTask;
import com.sep490.hdbhms.scheduling.domain.value_objects.TaskType;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BillingScheduledTaskHandler implements ScheduledTaskHandler {
    private static final Set<TaskType> SUPPORTED_TASK_TYPES = Set.of(
            TaskType.UTILITY_MONTHLY_RUN,
            TaskType.SCHEDULED_BILLING_CHARGES,
            TaskType.DEBT_OVERDUE_SCAN,
            TaskType.INVOICE_OVERDUE_WARNINGS
    );

    UtilityBillingRunService utilityBillingRunService;
    ScheduledBillingChargeService scheduledBillingChargeService;
    DebtDashboardService debtDashboardService;
    BillingManagementService billingManagementService;

    @Override
    public Set<TaskType> supportedTaskTypes() {
        return SUPPORTED_TASK_TYPES;
    }

    @Override
    public ScheduledTaskPolicy policy(TaskType taskType) {
        return ScheduledTaskPolicy.singleInstancePolicy();
    }

    @Override
    public void handle(ScheduledTask scheduledTask) {
        switch (scheduledTask.getTaskType()) {
            case UTILITY_MONTHLY_RUN -> utilityBillingRunService.createMonthlyRunsOnBillingDay();
            case SCHEDULED_BILLING_CHARGES -> scheduledBillingChargeService.createMonthlyDraftInvoices();
            case DEBT_OVERDUE_SCAN -> debtDashboardService.processOverdueDebts();
            case INVOICE_OVERDUE_WARNINGS -> billingManagementService.sendAutomaticOverdueWarnings();
            default -> throw unsupported(scheduledTask.getTaskType());
        }
    }

    private IllegalStateException unsupported(TaskType taskType) {
        return new IllegalStateException("Unsupported billing scheduled task type: " + taskType);
    }
}
