package com.sep490.hdbhms.scheduling.config.handler;

import com.sep490.hdbhms.occupancy.application.port.in.usecase.RoomTransferUseCase;
import com.sep490.hdbhms.occupancy.application.service.LeaseContractLifecycleService;
import com.sep490.hdbhms.occupancy.infrastructure.scheduling.VisitRequestTrashCleanupJob;
import com.sep490.hdbhms.scheduling.application.handler.ScheduledTaskHandler;
import com.sep490.hdbhms.scheduling.application.handler.ScheduledTaskPolicy;
import com.sep490.hdbhms.scheduling.application.port.out.ExpireRoomHoldPort;
import com.sep490.hdbhms.scheduling.config.ExpiredRoomHoldReconciler;
import com.sep490.hdbhms.scheduling.domain.model.ScheduledTask;
import com.sep490.hdbhms.scheduling.domain.value_objects.TaskType;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OccupancyScheduledTaskHandler implements ScheduledTaskHandler {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Set<TaskType> SUPPORTED_TASK_TYPES = Set.of(
            TaskType.ROOM_HOLD_EXPIRATION,
            TaskType.CONTRACT_EXPIRY,
            TaskType.EXPIRED_ROOM_HOLD_RECONCILE,
            TaskType.VISIT_REQUEST_TRASH_CLEANUP,
            TaskType.ROOM_TRANSFER_TIMEOUT,
            TaskType.CONTRACT_LIFECYCLE_SCAN
    );
    private static final Set<TaskType> SINGLE_INSTANCE_TASK_TYPES = Set.of(
            TaskType.EXPIRED_ROOM_HOLD_RECONCILE,
            TaskType.VISIT_REQUEST_TRASH_CLEANUP,
            TaskType.ROOM_TRANSFER_TIMEOUT,
            TaskType.CONTRACT_LIFECYCLE_SCAN
    );

    ExpireRoomHoldPort expireRoomHoldPort;
    LeaseContractLifecycleService leaseContractLifecycleService;
    ExpiredRoomHoldReconciler expiredRoomHoldReconciler;
    VisitRequestTrashCleanupJob visitRequestTrashCleanupJob;
    RoomTransferUseCase roomTransferUseCase;

    @Override
    public Set<TaskType> supportedTaskTypes() {
        return SUPPORTED_TASK_TYPES;
    }

    @Override
    public ScheduledTaskPolicy policy(TaskType taskType) {
        return SINGLE_INSTANCE_TASK_TYPES.contains(taskType)
                ? ScheduledTaskPolicy.singleInstancePolicy()
                : ScheduledTaskPolicy.standard();
    }

    @Override
    public void handle(ScheduledTask scheduledTask) {
        switch (scheduledTask.getTaskType()) {
            case ROOM_HOLD_EXPIRATION -> expireRoomHoldPort.execute(scheduledTask.getTargetId());
            case CONTRACT_EXPIRY -> leaseContractLifecycleService.processContract(
                    scheduledTask.getTargetId(),
                    LocalDate.now(BUSINESS_ZONE)
            );
            case EXPIRED_ROOM_HOLD_RECONCILE -> expiredRoomHoldReconciler.releaseStaleRoomHolds();
            case VISIT_REQUEST_TRASH_CLEANUP -> visitRequestTrashCleanupJob.purgeExpiredTrashItems();
            case ROOM_TRANSFER_TIMEOUT -> expireRoomTransferApprovals();
            case CONTRACT_LIFECYCLE_SCAN -> leaseContractLifecycleService.processAll(LocalDate.now(BUSINESS_ZONE));
            default -> throw unsupported(scheduledTask.getTaskType());
        }
    }

    private void expireRoomTransferApprovals() {
        int expiredCount = roomTransferUseCase.expireTargetHolderApprovals();
        int nominationExpiredCount = roomTransferUseCase.expireSourceHolderNominations();
        if (expiredCount > 0 || nominationExpiredCount > 0) {
            log.info("Expired room transfer approvals. targetHolder={}, sourceHolderNomination={}",
                    expiredCount,
                    nominationExpiredCount);
        }
    }

    private IllegalStateException unsupported(TaskType taskType) {
        return new IllegalStateException("Unsupported occupancy scheduled task type: " + taskType);
    }
}
