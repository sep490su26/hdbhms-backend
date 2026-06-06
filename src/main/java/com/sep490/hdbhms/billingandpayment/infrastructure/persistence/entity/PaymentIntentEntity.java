package com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity;

import com.sep490.hdbhms.billingandpayment.domain.value_objects.PaymentIntentProvider;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.PaymentIntentStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.DepositAgreementEntity;
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
        name = "payment_intents",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_payment_content", columnNames = "payment_content")
        },
        indexes = {
                @Index(name = "idx_pi_invoice", columnList = "invoice_id")
        }
)
public class PaymentIntentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = true)
    InvoiceEntity invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deposit_agreement_id", nullable = true)
    DepositAgreementEntity depositAgreement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_payment_group_id", nullable = true)
    InvoicePaymentGroupEntity invoicePaymentGroup;

    @Column(nullable = false)
    Long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    PaymentIntentProvider provider;
    @Column(name = "provider_order_code", length = 255)
    String providerOrderCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_account_id", nullable = true)
    CollectionAccountEntity collectionAccount;

    @Column(name = "payment_content", nullable = false, length = 255)
    String paymentContent;

    @Column(name = "qr_payload", columnDefinition = "TEXT")
    String qrPayload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    PaymentIntentStatus status = PaymentIntentStatus.CREATED;

    @Column(name = "expires_at")
    LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    LocalDateTime createdAt;
}