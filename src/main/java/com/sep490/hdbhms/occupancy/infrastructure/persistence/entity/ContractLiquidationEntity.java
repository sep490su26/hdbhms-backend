package com.sep490.hdbhms.occupancy.infrastructure.persistence.entity;

import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.InvoiceEntity;
import com.sep490.hdbhms.file.infrastructure.persistence.entity.FileMetadataEntity;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.UserEntity;
import com.sep490.hdbhms.occupancy.domain.value_objects.LiquidationStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(
        name = "contract_liquidations",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_liquidation_contract", columnNames = "contract_id")
        }
)
public class ContractLiquidationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_id", nullable = false)
    LeaseContractEntity contract;

    @Column(name = "liquidation_date", nullable = false)
    LocalDate liquidationDate;

    @Column(name = "reason", nullable = false, length = 1000)
    String reason;

    @Column(name = "deposit_amount", nullable = false)
    @Builder.Default
    Long depositAmount = 0L;

    @Column(name = "deposit_deduction_amount", nullable = false)
    @Builder.Default
    Long depositDeductionAmount = 0L;

    @Column(name = "deposit_deduction_reason", columnDefinition = "TEXT")
    String depositDeductionReason;

    @Column(name = "deposit_refund_amount", nullable = false)
    @Builder.Default
    Long depositRefundAmount = 0L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "final_invoice_id", nullable = true)
    InvoiceEntity finalInvoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "signed_file_id", nullable = true)
    FileMetadataEntity signedFile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    LiquidationStatus status = LiquidationStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = true)
    UserEntity createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    LocalDateTime createdAt;
}