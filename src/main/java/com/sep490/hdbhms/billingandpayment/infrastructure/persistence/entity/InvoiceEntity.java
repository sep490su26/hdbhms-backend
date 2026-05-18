package com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity;

import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceStatus;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceType;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.UserEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeaseContractEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.PropertyEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RoomEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(
        name = "invoices",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_invoice_code", columnNames = "invoice_code"),
                @UniqueConstraint(name = "uq_invoice_contract_period_type_rev",
                        columnNames = {"contract_id", "billing_period", "invoice_type", "revision_no"}),
                @UniqueConstraint(name = "uq_invoice_active_key", columnNames = "active_invoice_key")
        },
        indexes = {
                @Index(name = "idx_invoice_room_status", columnList = "room_id, status, due_date"),
                @Index(name = "idx_invoice_contract", columnList = "contract_id"),
                @Index(name = "idx_invoice_overdue", columnList = "status, due_date")
        }
)
public class InvoiceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "invoice_code", nullable = false, length = 80)
    String invoiceCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    PropertyEntity property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = true)
    RoomEntity room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = true)
    LeaseContractEntity contract;

    @Enumerated(EnumType.STRING)
    @Column(name = "invoice_type", nullable = false, length = 50)
    InvoiceType invoiceType;

    @Column(name = "revision_no", nullable = false)
    @Builder.Default
    Integer revisionNo = 1;

    @Column(name = "billing_period", length = 7, columnDefinition = "CHAR(7)")
    String billingPeriod;

    @Column(name = "issue_date", nullable = false)
    LocalDate issueDate;

    @Column(name = "due_date", nullable = false)
    LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    InvoiceStatus status = InvoiceStatus.DRAFT;

    // currency column removed – VND only, amounts in đồng (BIGINT UNSIGNED)

    @Column(name = "subtotal_amount", nullable = false)
    @Builder.Default
    Long subtotalAmount = 0L;

    @Column(name = "discount_amount", nullable = false)
    @Builder.Default
    Long discountAmount = 0L;

    @Column(name = "total_amount", nullable = false)
    @Builder.Default
    Long totalAmount = 0L;

    @Column(name = "paid_amount", nullable = false)
    @Builder.Default
    Long paidAmount = 0L;

    @Column(name = "remaining_amount", nullable = false)
    @Builder.Default
    Long remainingAmount = 0L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_account_id", nullable = true)
    CollectionAccountEntity collectionAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = true)
    UserEntity createdBy;

    @Column(name = "issued_at")
    Instant issuedAt;

    @Column(name = "voided_at")
    Instant voidedAt;

    @Column(name = "void_reason", length = 1000)
    String voidReason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    @Version
    @Column(nullable = false)
    @Builder.Default
    Integer version = 0;

    @Column(name = "active_invoice_key", insertable = false, updatable = false, length = 255)
    String activeInvoiceKey;
}