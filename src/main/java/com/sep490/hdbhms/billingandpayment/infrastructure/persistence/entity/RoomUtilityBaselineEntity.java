package com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity;

import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.MeterEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RoomEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(
        name = "room_utility_baselines",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_room_utility_baseline_meter", columnNames = "meter_id")
        },
        indexes = {
                @Index(name = "idx_room_utility_baseline_room", columnList = "room_id")
        }
)
public class RoomUtilityBaselineEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_utility_baseline_id")
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    RoomEntity room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meter_id", nullable = false)
    MeterEntity meter;

    @Column(name = "last_billed_reading", nullable = false, precision = 12, scale = 3)
    BigDecimal lastBilledReading;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_invoice_id")
    InvoiceEntity lastInvoice;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;
}
