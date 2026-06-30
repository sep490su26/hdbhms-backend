package com.sep490.hdbhms.occupancy.infrastructure.persistence.entity;

import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.DebtSnapshotEntity;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.PersonProfileEntity;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.UserEntity;
import com.sep490.hdbhms.occupancy.domain.valueObjects.TransferRequestStatus;
import com.sep490.hdbhms.occupancy.domain.valueObjects.TargetTransferType;
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
    @Column(name = "room_transfer_request_id")
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

    @Column(name = "transferring_tenant_profile_ids", columnDefinition = "JSON")
    String transferringTenantProfileIds;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nominated_holder_profile_id")
    PersonProfileEntity nominatedHolderProfile;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_transfer_type", length = 50)
    TargetTransferType targetTransferType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_contract_id")
    LeaseContractEntity targetContract;


    @Column(name = "requested_transfer_date", nullable = false)
    LocalDate requestedTransferDate;

    @Column(columnDefinition = "TEXT")
    String reason;

    @Column(name = "reserved_slots")
    Integer reservedSlots;

    @Column(name = "reservation_expires_at")
    LocalDateTime reservationExpiresAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_holder_approved_by")
    UserEntity targetHolderApprovedBy;

    @Column(name = "target_holder_approved_at")
    LocalDateTime targetHolderApprovedAt;

    @Column(name = "target_holder_rejected_at")
    LocalDateTime targetHolderRejectedAt;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    TransferRequestStatus status = TransferRequestStatus.WAITING_APPROVAL;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "debt_snapshot_id", nullable = true)
    DebtSnapshotEntity debtSnapshot;

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