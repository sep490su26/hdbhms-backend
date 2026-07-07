package com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity;

import com.sep490.hdbhms.billingandpayment.domain.valueObjects.InvoiceLineType;
import com.sep490.hdbhms.billingandpayment.domain.valueObjects.PendingBillingChargeStatus;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.UserEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeaseContractEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.PropertyEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RoomEntity;
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
        name = "pending_billing_charges",
        indexes = {
                @Index(name = "idx_pending_billing_due", columnList = "status, scheduled_issue_at"),
                @Index(name = "idx_pending_billing_source", columnList = "source_type, source_id, status"),
                @Index(name = "idx_pending_billing_contract_period", columnList = "contract_id, billing_period, status")
        }
)
public class PendingBillingChargeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pending_billing_charge_id")
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    PropertyEntity property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    RoomEntity room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    LeaseContractEntity contract;

    @Column(name = "source_type", nullable = false, length = 100)
    String sourceType;

    @Column(name = "source_id", nullable = false)
    Long sourceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "line_type", nullable = false, length = 50)
    InvoiceLineType lineType;

    @Column(nullable = false, length = 1000)
    String description;

    @Column(nullable = false)
    Long amount;

    @Column(name = "billing_period", nullable = false, length = 7, columnDefinition = "CHAR(7)")
    String billingPeriod;

    @Column(name = "scheduled_issue_at", nullable = false)
    LocalDateTime scheduledIssueAt;

    @Column(name = "due_date", nullable = false)
    LocalDateTime dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    PendingBillingChargeStatus status = PendingBillingChargeStatus.SCHEDULED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    InvoiceEntity invoice;

    @Column(name = "failure_reason", length = 1000)
    String failureReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    UserEntity createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;
}