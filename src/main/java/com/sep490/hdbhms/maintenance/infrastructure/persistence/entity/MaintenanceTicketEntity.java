package com.sep490.hdbhms.maintenance.infrastructure.persistence.entity;

import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.UserEntity;
import com.sep490.hdbhms.maintenance.domain.value_objects.MaintenanceTicketStatus;
import com.sep490.hdbhms.maintenance.domain.value_objects.Priority;
import com.sep490.hdbhms.maintenance.domain.value_objects.TicketScope;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeaseContractEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.PropertyEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RoomEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(
        name = "maintenance_tickets",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_ticket_code", columnNames = "ticket_code")
        },
        indexes = {
                @Index(name = "idx_ticket_status", columnList = "status, created_at"),
                @Index(name = "idx_ticket_room", columnList = "room_id"),
                @Index(name = "idx_ticket_property_status", columnList = "property_id, status, created_at")
        }
)
public class MaintenanceTicketEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "maintenance_ticket_id")
    Long id;

    @Column(name = "ticket_code", nullable = false, length = 80)
    String ticketCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    PropertyEntity property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = true)
    RoomEntity room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = true)
    LeaseContractEntity contract;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    UserEntity createdBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "ticket_scope", nullable = false, length = 50)
    TicketScope ticketScope;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    Priority priority = Priority.MEDIUM;

    @Column(nullable = false, length = 100)
    String category;

    @Column(nullable = false, length = 255)
    String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    MaintenanceTicketStatus status = MaintenanceTicketStatus.PENDING_ACCEPTANCE;

    @Column(name = "rejection_reason", length = 1000)
    String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to", nullable = true)
    UserEntity assignedTo;

    @Column(name = "worker_name", length = 255)
    String workerName;

    @Column(name = "repairman_phone", length = 30)
    String repairmanPhone;

    @Column(name = "repair_items", columnDefinition = "TEXT")
    String repairItems;

    @Column(name = "completed_at")
    LocalDateTime completedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;
}