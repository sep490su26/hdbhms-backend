package com.sep490.hdbhms.scheduling.application.port.out;

import com.sep490.hdbhms.scheduling.domain.model.ScheduledTask;
import com.sep490.hdbhms.scheduling.domain.valueObjects.TaskStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ScheduledTaskRepository {
    ScheduledTask save(ScheduledTask scheduledTask);

    Optional<ScheduledTask> findById(Long id);

    List<ScheduledTask> findByStatusAndDueAtBefore(TaskStatus taskStatus, LocalDateTime now);

    void saveAll(List<ScheduledTask> dueTasks);

    void cancelForTarget(String targetType, Long targetId);
}
