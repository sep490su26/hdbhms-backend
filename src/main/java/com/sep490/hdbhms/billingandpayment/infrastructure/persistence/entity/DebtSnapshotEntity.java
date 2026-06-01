package com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity;

import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeaseContractEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RoomEntity;
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
        name = "debt_snapshots",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_debt_snapshot", columnNames = {"room_id", "snapshot_date"})
        },
        indexes = {
                @Index(name = "idx_debt_over_limit", columnList = "is_over_limit, snapshot_date"),
                @Index(name = "idx_debt_contract", columnList = "contract_id, snapshot_date")
        }
)
public class DebtSnapshotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    RoomEntity room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    LeaseContractEntity contract;

    @Column(name = "snapshot_date", nullable = false)
    LocalDate snapshotDate;

    @Column(name = "rent_debt_amount", nullable = false)
    @Builder.Default
    Long rentDebtAmount = 0L;

    @Column(name = "utility_debt_amount", nullable = false)
    @Builder.Default
    Long utilityDebtAmount = 0L;

    @Column(name = "other_debt_amount", nullable = false)
    @Builder.Default
    Long otherDebtAmount = 0L;

    @Column(name = "rent_debt_months", nullable = false)
    @Builder.Default
    Integer rentDebtMonths = 0;

    @Column(name = "utility_debt_months", nullable = false)
    @Builder.Default
    Integer utilityDebtMonths = 0;

    @Column(name = "mixed_debt_amount", nullable = false)
    @Builder.Default
    Long mixedDebtAmount = 0L;

    @Column(name = "debt_limit_amount")
    Long debtLimitAmount;

    @Column(name = "is_over_limit", nullable = false)
    @Builder.Default
    Boolean isOverLimit = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    LocalDateTime createdAt;
}
