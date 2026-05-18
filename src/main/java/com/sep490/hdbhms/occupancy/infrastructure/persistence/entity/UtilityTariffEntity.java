package com.sep490.hdbhms.occupancy.infrastructure.persistence.entity;

import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.UserEntity;
import com.sep490.hdbhms.occupancy.domain.value_objects.RuleStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.UtilityType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(
        name = "utility_tariffs",
        indexes = {
                @Index(name = "idx_tariff_effective", columnList = "property_id, utility_type, effective_from")
        }
)
public class UtilityTariffEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = true)
    PropertyEntity property;

    @Enumerated(EnumType.STRING)
    @Column(name = "utility_type", nullable = false, length = 50)
    UtilityType utilityType;   // ELECTRICITY, WATER, SERVICE_FEE

    @Column(name = "unit_price", nullable = false)
    Long unitPrice;             // VND đồng per unit (BIGINT UNSIGNED)

    @Column(name = "free_allowance", nullable = false)
    @Builder.Default
    Long freeAllowance = 0L;

    @Column(name = "service_fee_waive_electricity_threshold")
    Long serviceFeeWaiveElectricityThreshold;

    @Column(name = "effective_from", nullable = false)
    LocalDate effectiveFrom;

    @Column(name = "effective_to")
    LocalDate effectiveTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = true)
    UserEntity createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    LocalDateTime createdAt;
}