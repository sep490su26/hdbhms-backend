package com.sep490.hdbhms.scheduling.infrastructure.persistence.mapper;

import com.sep490.hdbhms.scheduling.domain.model.ScheduledTask;
import com.sep490.hdbhms.scheduling.infrastructure.persistence.entity.ScheduledTaskEntity;
import org.springframework.stereotype.Component;

@Component
public class ScheduledTaskPersistenceMapper {

    public ScheduledTask toDomain(ScheduledTaskEntity entity) {
        if (entity == null) return null;
        return ScheduledTask.builder()
                .id(entity.getId())
                .taskType(entity.getTaskType())
                .targetType(entity.getTargetType())
                .targetId(entity.getTargetId())
                .dueAt(entity.getDueAt())
                .status(entity.getStatus())
                .retryCount(entity.getRetryCount())
                .payload(entity.getPayload())
                .executedAt(entity.getExecutedAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public ScheduledTaskEntity toEntity(ScheduledTask domain) {
        if (domain == null) return null;
        return ScheduledTaskEntity.builder()
                .id(domain.getId())
                .taskType(domain.getTaskType())
                .targetType(domain.getTargetType())
                .targetId(domain.getTargetId())
                .dueAt(domain.getDueAt())
                .status(domain.getStatus())
                .retryCount(domain.getRetryCount())
                .payload(domain.getPayload())
                .executedAt(domain.getExecutedAt())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}