package com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity;

import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoicePaymentGroupStatus;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoicePaymentGroupType;
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
        name = "invoice_payment_groups",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_invoice_payment_group",
                        columnNames = {"invoice_id", "collection_account_id", "group_type"})
        }
)
public class InvoicePaymentGroupEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    InvoiceEntity invoice;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "collection_account_id", nullable = false)
    CollectionAccountEntity collectionAccount;

    @Enumerated(EnumType.STRING)
    @Column(name = "group_type", nullable = false, length = 50)
    InvoicePaymentGroupType groupType;

    @Column(nullable = false)
    Long amount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_intent_id", nullable = true)
    PaymentIntentEntity paymentIntent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    InvoicePaymentGroupStatus status = InvoicePaymentGroupStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    LocalDateTime createdAt;
}