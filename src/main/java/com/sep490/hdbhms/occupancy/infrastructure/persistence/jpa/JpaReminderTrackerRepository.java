package com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa;

import com.sep490.hdbhms.occupancy.domain.value_objects.ReminderTrackerStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.ReminderTrackerEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface JpaReminderTrackerRepository extends JpaRepository<ReminderTrackerEntity, Long> {
    @Query("""
            SELECT tracker FROM ReminderTrackerEntity tracker
            LEFT JOIN tracker.recipientUser recipient
            WHERE tracker.reminderKey = :reminderKey
              AND tracker.targetType = :targetType
              AND tracker.targetId = :targetId
              AND tracker.audience = :audience
              AND tracker.status = :status
              AND (
                    (:recipientUserId IS NULL AND recipient.id IS NULL)
                    OR recipient.id = :recipientUserId
                  )
            ORDER BY tracker.id DESC
            """)
    List<ReminderTrackerEntity> findActiveTrackers(
            @Param("reminderKey") String reminderKey,
            @Param("targetType") String targetType,
            @Param("targetId") Long targetId,
            @Param("audience") String audience,
            @Param("recipientUserId") Long recipientUserId,
            @Param("status") ReminderTrackerStatus status,
            Pageable pageable
    );

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ReminderTrackerEntity tracker
            SET tracker.status = :completedStatus,
                tracker.completedAt = :completedAt,
                tracker.nextDueAt = null
            WHERE tracker.reminderKey = :reminderKey
              AND tracker.targetType = :targetType
              AND tracker.targetId = :targetId
              AND tracker.status = :activeStatus
            """)
    int completeActiveTrackers(
            @Param("reminderKey") String reminderKey,
            @Param("targetType") String targetType,
            @Param("targetId") Long targetId,
            @Param("activeStatus") ReminderTrackerStatus activeStatus,
            @Param("completedStatus") ReminderTrackerStatus completedStatus,
            @Param("completedAt") LocalDateTime completedAt
    );
}
