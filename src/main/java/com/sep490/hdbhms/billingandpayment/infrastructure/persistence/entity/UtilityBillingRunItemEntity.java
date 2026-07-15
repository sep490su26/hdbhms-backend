package com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity;

import com.sep490.hdbhms.billingandpayment.domain.value_objects.UtilityBillingRunItemStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeaseContractEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.MeterReadingEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RoomEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(
        name = "utility_billing_run_items",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_utility_billing_run_item_room", columnNames = {"run_id", "room_id"})
        },
        indexes = {
                @Index(name = "idx_utility_billing_run_item_status", columnList = "run_id, status")
        }
)
public class UtilityBillingRunItemEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "utility_billing_run_item_id")
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "run_id", nullable = false)
    UtilityBillingRunEntity run;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    RoomEntity room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lease_contract_id")
    LeaseContractEntity leaseContract;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "electricity_reading_id")
    MeterReadingEntity electricityReading;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "water_reading_id")
    MeterReadingEntity waterReading;

    @Column(name = "electricity_previous", precision = 12, scale = 3)
    BigDecimal electricityPrevious;

    @Column(name = "electricity_current", precision = 12, scale = 3)
    BigDecimal electricityCurrent;

    @Column(name = "electricity_usage", precision = 12, scale = 3)
    BigDecimal electricityUsage;

    @Column(name = "electricity_quantity", nullable = false)
    @Builder.Default
    Integer electricityQuantity = 0;

    @Column(name = "electricity_unit_price", nullable = false)
    @Builder.Default
    Long electricityUnitPrice = 0L;

    @Column(name = "electricity_amount", nullable = false)
    @Builder.Default
    Long electricityAmount = 0L;

    @Column(name = "water_previous", precision = 12, scale = 3)
    BigDecimal waterPrevious;

    @Column(name = "water_current", precision = 12, scale = 3)
    BigDecimal waterCurrent;

    @Column(name = "water_usage", precision = 12, scale = 3)
    BigDecimal waterUsage;

    @Column(name = "water_quantity", nullable = false)
    @Builder.Default
    Integer waterQuantity = 0;

    @Column(name = "water_unit_price", nullable = false)
    @Builder.Default
    Long waterUnitPrice = 0L;

    @Column(name = "water_amount", nullable = false)
    @Builder.Default
    Long waterAmount = 0L;

    @Column(name = "subtotal_amount", nullable = false)
    @Builder.Default
    Long subtotalAmount = 0L;

    @Column(name = "discount_amount", nullable = false)
    @Builder.Default
    Long discountAmount = 0L;

    @Column(name = "total_amount", nullable = false)
    @Builder.Default
    Long totalAmount = 0L;

    @Column(name = "warning_message", columnDefinition = "TEXT")
    String warningMessage;

    @Column(name = "adjustment_reason", length = 500)
    String adjustmentReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    UtilityBillingRunItemStatus status = UtilityBillingRunItemStatus.READY;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    InvoiceEntity invoice;
}
