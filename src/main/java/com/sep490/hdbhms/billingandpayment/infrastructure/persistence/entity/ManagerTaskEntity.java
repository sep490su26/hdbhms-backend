package com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity;

import com.sep490.hdbhms.billingandpayment.domain.value_objects.ManagerTaskStatus;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.UserEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeaseContractEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RoomEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(
        name = "manager_tasks",
        indexes = {
                @Index(name = "idx_manager_task_status_due", columnList = "status, due_date"),
                @Index(name = "idx_manager_task_contract", columnList = "lease_contract_id, status"),
                @Index(name = "idx_manager_task_type_contract", columnList = "task_type, lease_contract_id, status")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_manager_task_idempotency_key", columnNames = "idempotency_key")
        }
)
public class ManagerTaskEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "manager_task_id")
    Long id;

    @Column(nullable = false, length = 255)
    String title;

    @Column(columnDefinition = "TEXT")
    String description;

    @Column(name = "task_type", length = 80)
    String taskType;

    @Column(name = "idempotency_key", length = 180)
    String idempotencyKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    UserEntity assignee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    RoomEntity room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lease_contract_id")
    LeaseContractEntity leaseContract;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    ManagerTaskStatus status = ManagerTaskStatus.PENDING;

    @Column(name = "due_date", nullable = false)
    LocalDate dueDate;

    @Column(name = "completed_at")
    LocalDateTime completedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;
}
