package com.sep490.hdbhms.scheduling.config;

import com.sep490.hdbhms.scheduling.application.port.out.ExpireRoomHoldPort;
import com.sep490.hdbhms.scheduling.application.port.out.ScheduledTaskRepository;
import com.sep490.hdbhms.scheduling.domain.model.ScheduledTask;
import com.sep490.hdbhms.scheduling.domain.value_objects.TaskStatus;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ScheduledTaskProcessor {
    ScheduledTaskRepository scheduledTaskRepository;
    ExpireRoomHoldPort expireRoomHoldPort;

    @Transactional
    @Scheduled(fixedDelay = 10_000)
    public void processDueTasks() {
        List<ScheduledTask> dueTasks = scheduledTaskRepository
                .findByStatusAndDueAtBefore(
                        TaskStatus.PENDING,
                        LocalDateTime.now()
                );
        for (ScheduledTask scheduledTask : dueTasks) {
            try {
                executeTask(scheduledTask);
                scheduledTask.execute();
            } catch (Exception e) {
                log.error(e.getMessage());
                scheduledTask.setRetryCount(scheduledTask.getRetryCount() + 1);
                if (scheduledTask.getRetryCount() >= 3) {
                    scheduledTask.failed();
                }
            }
        }
        scheduledTaskRepository.saveAll(dueTasks);
    }

    private void executeTask(ScheduledTask scheduledTask) {
        switch (scheduledTask.getTaskType()) {
            case ROOM_HOLD_EXPIRATION -> expireRoomHoldPort.execute(scheduledTask.getTargetId());
            default -> log.error("Invalid scheduled task type");
        }
    }
}
