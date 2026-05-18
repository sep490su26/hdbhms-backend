package com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity;

import com.sep490.hdbhms.billingandpayment.domain.value_objects.TransactionProvider;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.TransactionStatus;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(
        name = "payment_transactions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_provider_txn", columnNames = {"provider", "provider_transaction_id"})
        },
        indexes = {
                @Index(name = "idx_payment_txn_status", columnList = "status, transaction_time"),
                @Index(name = "idx_payment_reconcile", columnList = "status, amount, transaction_time"),
                @Index(name = "idx_payment_content", columnList = "content(100)")
        }
)
public class PaymentTransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    TransactionProvider provider;

    @Column(name = "provider_transaction_id", length = 255)
    String providerTransactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_account_id", nullable = true)
    CollectionAccountEntity collectionAccount;

    @Column(nullable = false)
    Long amount;

    @Column(name = "transaction_time", nullable = false)
    Instant transactionTime;

    @Column(name = "payer_name", length = 255)
    String payerName;

    @Column(name = "payer_account", length = 255)
    String payerAccount;

    @Column(length = 1000)
    String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    TransactionStatus status = TransactionStatus.PENDING_RECONCILE;

    @Lob
    @Column(name = "raw_payload", columnDefinition = "BLOB")
    byte[] rawPayload;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by", nullable = true)
    UserEntity confirmedBy;

    @Column(name = "confirmed_at")
    Instant confirmedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    LocalDateTime createdAt;
}