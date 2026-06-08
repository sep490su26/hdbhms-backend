package com.sep490.hdbhms.occupancy.infrastructure.persistence.entity;

import com.sep490.hdbhms.occupancy.domain.value_objects.DepositBatchStatus;
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
        name = "deposit_batches",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_deposit_batch_code", columnNames = "batch_code"),
                @UniqueConstraint(name = "uq_deposit_batch_invoice", columnNames = "invoice_id"),
                @UniqueConstraint(name = "uq_deposit_batch_payment_intent", columnNames = "payment_intent_id")
        },
        indexes = {
                @Index(name = "idx_deposit_batch_property", columnList = "property_id"),
                @Index(name = "idx_deposit_batch_status", columnList = "status")
        }
)
public class DepositBatchEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "batch_code", nullable = false, length = 80)
    String batchCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    PropertyEntity property;

    @Column(name = "full_name", nullable = false, length = 255)
    String fullName;

    @Column(nullable = false, length = 30)
    String phone;

    @Column(length = 255)
    String email;

    @Column(name = "id_number", length = 50)
    String idNumber;

    @Column(name = "expected_move_in_date", nullable = false)
    LocalDate expectedMoveInDate;

    @Column(name = "expected_lease_sign_date", nullable = false)
    LocalDate expectedLeaseSignDate;

    @Column(name = "total_deposit_amount", nullable = false)
    Long totalDepositAmount;

    @Column(name = "invoice_id")
    Long invoiceId;

    @Column(name = "payment_intent_id")
    Long paymentIntentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    DepositBatchStatus status = DepositBatchStatus.DRAFT;

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
