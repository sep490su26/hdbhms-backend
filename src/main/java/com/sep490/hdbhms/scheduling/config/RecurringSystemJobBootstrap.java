package com.sep490.hdbhms.scheduling.config;

import com.sep490.hdbhms.scheduling.application.port.out.ScheduledTaskRepository;
import com.sep490.hdbhms.scheduling.domain.value_objects.TaskType;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RecurringSystemJobBootstrap implements ApplicationRunner {
    ScheduledTaskRepository scheduledTaskRepository;

    @Override
    public void run(ApplicationArguments args) {
        LocalDateTime now = LocalDateTime.now();
        for (Map.Entry<TaskType, String> entry : RecurringSystemJobSchedule.expressions().entrySet()) {
            TaskType taskType = entry.getKey();
            scheduledTaskRepository.ensureRecurringSystemTask(
                    taskType,
                    RecurringSystemJobSchedule.nextDueAt(taskType, now),
                    entry.getValue(),
                    RecurringSystemJobSchedule.idempotencyKey(taskType)
            );
        }
    }
}
