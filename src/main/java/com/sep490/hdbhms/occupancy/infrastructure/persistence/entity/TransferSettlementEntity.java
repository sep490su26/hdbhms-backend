package com.sep490.hdbhms.occupancy.infrastructure.persistence.entity;

import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.InvoiceEntity;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.PaymentTransactionEntity;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.UserEntity;
import com.sep490.hdbhms.occupancy.domain.value_objects.SettlementType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(
        name = "transfer_settlements",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_transfer_settlement", columnNames = "transfer_request_id")
        }
)
public class TransferSettlementEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transfer_request_id", nullable = false)
    RoomTransferRequestEntity transferRequest;

    @Column(name = "old_room_remaining_value", nullable = false)
    @Builder.Default
    Long oldRoomRemainingValue = 0L;

    @Column(name = "new_room_required_value", nullable = false)
    @Builder.Default
    Long newRoomRequiredValue = 0L;

    @Column(name = "difference_amount", nullable = false)
    @Builder.Default
    Long differenceAmount = 0L;

    @Enumerated(EnumType.STRING)
    @Column(name = "settlement_type", nullable = false, length = 50)
    SettlementType settlementType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "old_room_final_invoice_id", nullable = true)
    InvoiceEntity oldRoomFinalInvoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transfer_difference_invoice_id", nullable = true)
    InvoiceEntity transferDifferenceInvoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by", nullable = true)
    UserEntity confirmedBy;

    @Column(name = "confirmed_at")
    LocalDateTime confirmedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    LocalDateTime createdAt;
}