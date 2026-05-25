package com.sep490.hdbhms.occupancy.infrastructure.persistence.entity;

import com.sep490.hdbhms.file.infrastructure.persistence.entity.FileMetadataEntity;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.PersonProfileEntity;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.UserEntity;
import com.sep490.hdbhms.occupancy.domain.value_objects.LeaseStatus;
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
        name = "lease_contracts",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_contract_code", columnNames = "contract_code")
        },
        indexes = {
                @Index(name = "idx_contract_room_status", columnList = "room_id, status"),
                @Index(name = "idx_contract_end_date", columnList = "end_date, status"),
                @Index(name = "idx_contract_primary_profile", columnList = "primary_tenant_profile_id, status")
        }
)
public class LeaseContractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "contract_code", nullable = false, length = 80)
    String contractCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    RoomEntity room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deposit_agreement_id", nullable = true)
    DepositAgreementEntity depositAgreement;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "primary_tenant_profile_id", nullable = false)
    PersonProfileEntity primaryTenantProfile;

    @Column(name = "start_date", nullable = false)
    LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    LocalDate endDate;

    @Column(name = "rent_start_date", nullable = false)
    LocalDate rentStartDate;

    @Column(name = "monthly_rent", nullable = false)
    Long monthlyRent;

    @Column(name = "payment_cycle_months", nullable = false, columnDefinition = "TINYINT UNSIGNED")
    Integer paymentCycleMonths;

    @Column(name = "deposit_amount", nullable = false)
    @Builder.Default
    Long depositAmount = 0L;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    LeaseStatus status = LeaseStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "previous_contract_id", nullable = true)
    LeaseContractEntity previousContract;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_file_id", nullable = true)
    FileMetadataEntity contractFile;

    @Column(name = "signed_at")
    LocalDateTime signedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = true)
    UserEntity createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    LocalDateTime deletedAt;

    @Version
    @Column(nullable = false)
    @Builder.Default
    Long version = 0L;
}
