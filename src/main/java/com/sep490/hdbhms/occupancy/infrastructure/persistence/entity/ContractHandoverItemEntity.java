package com.sep490.hdbhms.occupancy.infrastructure.persistence.entity;

import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.InvoiceEntity;
import com.sep490.hdbhms.file.infrastructure.persistence.entity.FileMetadataEntity;
import com.sep490.hdbhms.occupancy.domain.value_objects.AssetCondition;
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
        name = "contract_handover_items",
        indexes = {
                @Index(name = "idx_chi_handover", columnList = "handover_record_id")
        }
)
public class ContractHandoverItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "handover_record_id", nullable = false)
    ContractHandoverRecordEntity handoverRecord;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_asset_id", nullable = true)
    RoomAssetEntity roomAsset;

    @Column(name = "asset_name", nullable = false, length = 255)
    String assetName;

    @Column(nullable = false)
    @Builder.Default
    Integer quantity = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_status", nullable = false, length = 50)
    @Builder.Default
    AssetCondition conditionStatus = AssetCondition.GOOD;

    @Column(name = "note", columnDefinition = "TEXT")
    String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evidence_file_id", nullable = true)
    FileMetadataEntity evidenceFile;

    @Column(name = "compensation_amount")
    Long compensationAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compensation_invoice_id", nullable = true)
    InvoiceEntity compensationInvoice;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    LocalDateTime createdAt;
}