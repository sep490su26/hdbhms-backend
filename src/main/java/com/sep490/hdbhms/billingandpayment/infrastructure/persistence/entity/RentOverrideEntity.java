package com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity;

import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.UserEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeaseContractEntity;
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
        name = "rent_overrides",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_rent_override", columnNames = {"contract_id", "billing_period"})
        }
)
public class RentOverrideEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_id", nullable = false)
    LeaseContractEntity contract;

    @Column(name = "billing_period", nullable = false, length = 7, columnDefinition = "CHAR(7)")
    String billingPeriod;

    @Column(name = "override_monthly_rent", nullable = false)
    Long overrideMonthlyRent;

    @Column(nullable = false, length = 1000)
    String reason;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "approved_by", nullable = false)
    UserEntity approvedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    LocalDateTime createdAt;
}
