package com.sep490.hdbhms.scheduling.infrastructure.persistence.jpa;

import com.sep490.hdbhms.scheduling.domain.valueObjects.TaskStatus;
import com.sep490.hdbhms.scheduling.infrastructure.persistence.entity.ScheduledTaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface JpaScheduledTaskRepository extends JpaRepository<ScheduledTaskEntity, Long> {
    List<ScheduledTaskEntity> findByStatusAndDueAtBefore(TaskStatus status, LocalDateTime dueAt);

    @Modifying
    @Query("UPDATE ScheduledTaskEntity t SET t.status = :newStatus WHERE t.targetType = :type AND t.targetId = :id AND t.status = :oldStatus")
    void changeScheduleTaskStatus(@Param("type") String type, @Param("id") Long id,
                                  @Param("oldStatus") TaskStatus old, @Param("newStatus") TaskStatus newStatus);
}
