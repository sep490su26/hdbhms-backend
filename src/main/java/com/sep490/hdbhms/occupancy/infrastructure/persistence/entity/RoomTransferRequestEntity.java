package com.sep490.hdbhms.occupancy.infrastructure.persistence.entity;

import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.DebtSnapshotEntity;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.PersonProfileEntity;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.UserEntity;
import com.sep490.hdbhms.occupancy.domain.value_objects.TransferRequestStatus;
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
        name = "room_transfer_requests",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_transfer_code", columnNames = "request_code")
        },
        indexes = {
                @Index(name = "idx_transfer_status", columnList = "status, created_at"),
                @Index(name = "idx_transfer_rooms", columnList = "old_room_id, target_room_id")
        }
)
public class RoomTransferRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "request_code", nullable = false, length = 80)
    String requestCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    TenantEntity requester;                     // FK to tenants(id)

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "old_contract_id", nullable = false)
    LeaseContractEntity oldContract;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "old_room_id", nullable = false)
    RoomEntity oldRoom;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_room_id", nullable = false)
    RoomEntity targetRoom;

    @Column(name = "requested_transfer_date", nullable = false)
    LocalDate requestedTransferDate;

    @Column(columnDefinition = "TEXT")
    String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    TransferRequestStatus status = TransferRequestStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "debt_snapshot_id", nullable = true)
    DebtSnapshotEntity debtSnapshot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by", nullable = true)
    UserEntity approvedBy;

    @Column(name = "approved_at")
    LocalDateTime approvedAt;

    @Column(name = "rejection_reason", length = 1000)
    String rejectionReason;

    @Column(name = "eligibility_checked_at")
    LocalDateTime eligibilityCheckedAt;

    @Column(name = "is_eligible_at_creation")
    Boolean isEligibleAtCreation;

    @Lob
    @Column(name = "eligibility_snapshot", columnDefinition = "BLOB")
    byte[] eligibilitySnapshot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "new_contract_id", nullable = true)
    LeaseContractEntity newContract;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;
}