package com.sep490.hdbhms.occupancy.infrastructure.persistence.entity;

import com.sep490.hdbhms.occupancy.domain.valueObjects.DepositBatchItemStatus;
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
        name = "deposit_batch_items",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_deposit_batch_item_room", columnNames = {"batch_id", "room_id"}),
                @UniqueConstraint(name = "uq_deposit_batch_item_hold", columnNames = "room_hold_id"),
                @UniqueConstraint(name = "uq_deposit_batch_item_form", columnNames = "deposit_form_id"),
                @UniqueConstraint(name = "uq_deposit_batch_item_agreement", columnNames = "deposit_agreement_id")
        },
        indexes = {
                @Index(name = "idx_deposit_batch_item_batch_status", columnList = "batch_id, status"),
                @Index(name = "idx_deposit_batch_item_room_status", columnList = "room_id, status")
        }
)
public class DepositBatchItemEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "deposit_batch_item_id")
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    DepositBatchEntity batch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    RoomEntity room;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_hold_id")
    RoomHoldEntity roomHold;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deposit_form_id")
    DepositFormEntity depositForm;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deposit_agreement_id")
    DepositAgreementEntity depositAgreement;

    @Column(name = "deposit_amount", nullable = false)
    Long depositAmount;

    @Column(name = "occupant_count", nullable = false)
    Integer occupantCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    DepositBatchItemStatus status = DepositBatchItemStatus.PENDING_PAYMENT;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;

    @Version
    @Column(nullable = false)
    @Builder.Default
    Long version = 0L;
}