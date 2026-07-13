package com.sep490.hdbhms.occupancy.infrastructure.persistence.entity;

import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.UserEntity;
import com.sep490.hdbhms.occupancy.domain.value_objects.DepositContactOutcome;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(
        name = "deposit_contact_events",
        indexes = @Index(name = "idx_deposit_contact_latest", columnList = "deposit_agreement_id, contacted_at")
)
public class DepositContactEventEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "deposit_contact_event_id")
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "deposit_agreement_id", nullable = false)
    DepositAgreementEntity depositAgreement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    DepositContactOutcome outcome;

    @Column(nullable = false, columnDefinition = "TEXT")
    String note;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contacted_by", nullable = false)
    UserEntity contactedBy;

    @Column(name = "contacted_at", nullable = false)
    LocalDateTime contactedAt;
}
