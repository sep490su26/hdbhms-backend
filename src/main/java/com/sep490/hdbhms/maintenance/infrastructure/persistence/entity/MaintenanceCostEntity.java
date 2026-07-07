package com.sep490.hdbhms.maintenance.infrastructure.persistence.entity;

import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.InvoiceEntity;
import com.sep490.hdbhms.file.infrastructure.persistence.entity.FileMetadataEntity;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.UserEntity;
import com.sep490.hdbhms.maintenance.domain.valueObjects.CostResponsibility;
import com.sep490.hdbhms.maintenance.domain.valueObjects.CostType;
import com.sep490.hdbhms.maintenance.domain.valueObjects.PaidBy;
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
        name = "maintenance_costs",
        indexes = {
                @Index(name = "idx_maintenance_cost_ticket", columnList = "ticket_id")
        }
)
public class MaintenanceCostEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "maintenance_cost_id")
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    MaintenanceTicketEntity ticket;

    @Enumerated(EnumType.STRING)
    @Column(name = "cost_type", nullable = false, length = 50)
    CostType costType;

    @Column(nullable = false, length = 1000)
    String description;

    @Column(nullable = false)
    Long amount;                     // BIGINT UNSIGNED

    @Enumerated(EnumType.STRING)
    @Column(name = "paid_by", nullable = false, length = 50)
    @Builder.Default
    PaidBy paidBy = PaidBy.LANDLORD;

    @Enumerated(EnumType.STRING)
    @Column(name = "cost_responsibility", nullable = false, length = 50)
    @Builder.Default
    CostResponsibility costResponsibility = CostResponsibility.UNDECIDED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "charge_invoice_id", nullable = true)
    InvoiceEntity chargeInvoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_file_id", nullable = true)
    FileMetadataEntity receiptFile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = true)
    UserEntity createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    LocalDateTime createdAt;
}