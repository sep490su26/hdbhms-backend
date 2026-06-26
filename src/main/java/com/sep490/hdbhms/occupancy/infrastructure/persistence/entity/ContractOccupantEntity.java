package com.sep490.hdbhms.occupancy.infrastructure.persistence.entity;

import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.PersonProfileEntity;
import com.sep490.hdbhms.occupancy.domain.value_objects.OccupantRole;
import com.sep490.hdbhms.occupancy.domain.value_objects.OccupantStatus;
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
        name = "contract_occupants",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_contract_occupant_profile", columnNames = {"contract_id", "tenant_profile_id"})
        },
        indexes = {
                @Index(name = "idx_occupant_contract_status", columnList = "contract_id, status"),
                @Index(name = "idx_occupant_profile_status", columnList = "tenant_profile_id, status")
        }
)
public class ContractOccupantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "contract_occupant_id")
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_id", nullable = false)
    LeaseContractEntity contract;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    TenantEntity tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_profile_id")
    PersonProfileEntity tenantProfile;

    @Enumerated(EnumType.STRING)
    @Column(name = "occupant_role", nullable = false, length = 50)
    @Builder.Default
    OccupantRole occupantRole = OccupantRole.CO_OCCUPANT;

    @Column(name = "move_in_date", nullable = false)
    LocalDate moveInDate;

    @Column(name = "move_out_date")
    LocalDate moveOutDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    OccupantStatus status = OccupantStatus.ACTIVE;

    @Column(name = "disabled_reason", columnDefinition = "TEXT")
    String disabledReason;

    @Column(name = "disabled_by")
    Long disabledBy;

    @Column(name = "disabled_at")
    LocalDateTime disabledAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    LocalDateTime createdAt;
}