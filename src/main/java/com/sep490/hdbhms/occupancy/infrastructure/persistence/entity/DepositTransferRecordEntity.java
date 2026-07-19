package com.sep490.hdbhms.occupancy.infrastructure.persistence.entity;

import com.sep490.hdbhms.occupancy.domain.value_objects.DepositTransferStatus;
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
        name = "deposit_transfer_records",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_deposit_transfer_request", columnNames = "transfer_request_id"),
                @UniqueConstraint(name = "uq_deposit_transfer_new_contract", columnNames = "new_contract_id")
        },
        indexes = {
                @Index(name = "idx_deposit_transfer_old_contract", columnList = "old_contract_id"),
                @Index(name = "idx_deposit_transfer_status", columnList = "status")
        }
)
public class DepositTransferRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "deposit_transfer_record_id")
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transfer_request_id", nullable = false)
    RoomTransferRequestEntity transferRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "old_contract_id", nullable = false)
    LeaseContractEntity oldContract;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "new_contract_id", nullable = false)
    LeaseContractEntity newContract;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "old_deposit_agreement_id")
    DepositAgreementEntity oldDepositAgreement;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_room_id", nullable = false)
    RoomEntity fromRoom;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_room_id", nullable = false)
    RoomEntity toRoom;

    @Column(name = "amount", nullable = false)
    @Builder.Default
    Long amount = 0L;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    DepositTransferStatus status = DepositTransferStatus.DRAFT;

    @Column(name = "effective_date")
    LocalDate effectiveDate;

    @Column(name = "cancelled_at")
    LocalDateTime cancelledAt;

    @Column(name = "note", columnDefinition = "TEXT")
    String note;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;
}
