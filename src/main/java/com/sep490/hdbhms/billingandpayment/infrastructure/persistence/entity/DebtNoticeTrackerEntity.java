package com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity;

import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeaseContractEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(
        name = "debt_notice_trackers",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_debt_notice_contract", columnNames = "lease_contract_id")
        },
        indexes = {
                @Index(name = "idx_debt_notice_last_date", columnList = "last_notice_date")
        }
)
public class DebtNoticeTrackerEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "debt_notice_tracker_id")
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lease_contract_id", nullable = false)
    LeaseContractEntity leaseContract;

    @Column(name = "unresponsive_count", nullable = false)
    @Builder.Default
    Integer unresponsiveCount = 0;

    @Column(name = "last_notice_date")
    LocalDate lastNoticeDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;
}
