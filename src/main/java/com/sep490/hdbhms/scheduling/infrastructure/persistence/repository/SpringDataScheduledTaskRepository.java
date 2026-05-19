package com.sep490.hdbhms.scheduling.infrastructure.persistence.repository;

import com.sep490.hdbhms.scheduling.application.port.out.ScheduledTaskRepository;
import com.sep490.hdbhms.scheduling.domain.model.ScheduledTask;
import com.sep490.hdbhms.scheduling.domain.value_objects.TaskStatus;
import com.sep490.hdbhms.scheduling.infrastructure.persistence.entity.ScheduledTaskEntity;
import com.sep490.hdbhms.scheduling.infrastructure.persistence.jpa.JpaScheduledTaskRepository;
import com.sep490.hdbhms.scheduling.infrastructure.persistence.mapper.ScheduledTaskPersistenceMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SpringDataScheduledTaskRepository implements ScheduledTaskRepository {
    JpaScheduledTaskRepository jpaScheduledTaskRepository;
    ScheduledTaskPersistenceMapper scheduledTaskPersistenceMapper;

    @Override
    public ScheduledTask save(ScheduledTask scheduledTask) {
        return scheduledTaskPersistenceMapper.toDomain(
                jpaScheduledTaskRepository.save(
                        scheduledTaskPersistenceMapper.toEntity(
                                scheduledTask
                        )
                )
        );
    }

    @Override
    public Optional<ScheduledTask> findById(Long id) {
        return jpaScheduledTaskRepository.findById(id)
                .map(scheduledTaskPersistenceMapper::toDomain);
    }

    @Override
    public List<ScheduledTask> findByStatusAndDueAtBefore(TaskStatus taskStatus, LocalDateTime dateTime) {
        return jpaScheduledTaskRepository
                .findByStatusAndDueAtBefore(taskStatus, dateTime).stream()
                .map(scheduledTaskPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public void saveAll(List<ScheduledTask> dueTasks) {
        List<ScheduledTaskEntity> dueTaskEntities = dueTasks.stream()
                .map(scheduledTaskPersistenceMapper::toEntity)
                .toList();
        jpaScheduledTaskRepository.saveAll(dueTaskEntities);
    }

    @Override
    public void cancelForTarget(String targetType, Long targetId) {
        jpaScheduledTaskRepository.changeScheduleTaskStatus(
                targetType,
                targetId,
                TaskStatus.PENDING,
                TaskStatus.CANCELLED
        );
    }
}
