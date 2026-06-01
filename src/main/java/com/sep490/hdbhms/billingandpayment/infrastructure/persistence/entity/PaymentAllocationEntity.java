package com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity;

import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.UserEntity;
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
        name = "payment_allocations",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_payment_invoice_alloc", columnNames = {"payment_transaction_id", "invoice_id"})
        },
        indexes = {
                @Index(name = "idx_alloc_invoice", columnList = "invoice_id")
        }
)
public class PaymentAllocationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_transaction_id", nullable = false)
    PaymentTransactionEntity paymentTransaction;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    InvoiceEntity invoice;

    @Column(nullable = false)
    Long amount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "allocated_by")
    UserEntity allocatedBy;

    @CreationTimestamp
    @Column(name = "allocated_at", updatable = false, nullable = false)
    LocalDateTime allocatedAt;
}
