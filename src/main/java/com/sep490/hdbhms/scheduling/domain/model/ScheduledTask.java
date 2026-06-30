package com.sep490.hdbhms.scheduling.domain.model;

import com.sep490.hdbhms.scheduling.domain.valueObjects.TaskStatus;
import com.sep490.hdbhms.scheduling.domain.valueObjects.TaskType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Builder
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ScheduledTask {
    Long id;
    TaskType taskType;
    String targetType;
    Long targetId;
    LocalDateTime dueAt;
    TaskStatus status;
    @Setter
    Integer retryCount;
    byte[] payload;
    LocalDateTime executedAt;
    LocalDateTime createdAt;

    public static ScheduledTask create(TaskType taskType,
                                       String targetType,
                                       Long targetId,
                                       LocalDateTime dueAt) {
        return ScheduledTask.builder()
                .taskType(taskType)
                .targetType(targetType)
                .targetId(targetId)
                .dueAt(dueAt)
                .status(TaskStatus.PENDING)
                .retryCount(0)
                .build();
    }

    public void execute() {
        status = TaskStatus.DONE;
        executedAt = LocalDateTime.now();
    }

    public void failed() {
        status = TaskStatus.FAILED;
    }
}
