package com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity;

import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceReason;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.UtilityBillingRunStatus;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.UserEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.PropertyEntity;
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
        name = "utility_billing_runs",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_utility_billing_run_period", columnNames = {"property_id", "billing_period", "invoice_reason"})
        },
        indexes = {
                @Index(name = "idx_utility_billing_run_status", columnList = "property_id, billing_period, status")
        }
)
public class UtilityBillingRunEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "utility_billing_run_id")
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    PropertyEntity property;

    @Column(name = "billing_period", nullable = false, length = 7, columnDefinition = "CHAR(7)")
    String billingPeriod;

    @Enumerated(EnumType.STRING)
    @Column(name = "invoice_reason", nullable = false, length = 50)
    InvoiceReason invoiceReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    UtilityBillingRunStatus status = UtilityBillingRunStatus.DRAFT;

    @Column(name = "total_rooms", nullable = false)
    @Builder.Default
    Integer totalRooms = 0;

    @Column(name = "ready_count", nullable = false)
    @Builder.Default
    Integer readyCount = 0;

    @Column(name = "warning_count", nullable = false)
    @Builder.Default
    Integer warningCount = 0;

    @Column(name = "skipped_count", nullable = false)
    @Builder.Default
    Integer skippedCount = 0;

    @Column(name = "generated_invoice_count", nullable = false)
    @Builder.Default
    Integer generatedInvoiceCount = 0;

    @Column(name = "subtotal_amount", nullable = false)
    @Builder.Default
    Long subtotalAmount = 0L;

    @Column(name = "discount_amount", nullable = false)
    @Builder.Default
    Long discountAmount = 0L;

    @Column(name = "total_amount", nullable = false)
    @Builder.Default
    Long totalAmount = 0L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    UserEntity createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generated_by")
    UserEntity generatedBy;

    @Column(name = "generated_at")
    LocalDateTime generatedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;
}
