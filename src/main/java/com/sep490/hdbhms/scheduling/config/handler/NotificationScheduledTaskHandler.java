package com.sep490.hdbhms.scheduling.config.handler;

import com.sep490.hdbhms.notification.infrastructure.dispatcher.NotificationOutboxDispatcher;
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
public class NotificationScheduledTaskHandler implements ScheduledTaskHandler {
    private static final Set<TaskType> SUPPORTED_TASK_TYPES = Set.of(TaskType.NOTIFICATION_OUTBOX_DISPATCH);

    NotificationOutboxDispatcher notificationOutboxDispatcher;

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
        if (scheduledTask.getTaskType() != TaskType.NOTIFICATION_OUTBOX_DISPATCH) {
            throw new IllegalStateException("Unsupported notification scheduled task type: " + scheduledTask.getTaskType());
        }
        notificationOutboxDispatcher.dispatch();
    }
}
