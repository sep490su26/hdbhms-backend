package com.sep490.hdbhms.scheduling.application.handler;

import com.sep490.hdbhms.scheduling.domain.model.ScheduledTask;
import com.sep490.hdbhms.scheduling.domain.value_objects.TaskType;

import java.util.Set;

public interface ScheduledTaskHandler {
    Set<TaskType> supportedTaskTypes();

    void handle(ScheduledTask scheduledTask);

    default ScheduledTaskPolicy policy(TaskType taskType) {
        return ScheduledTaskPolicy.standard();
    }
}
