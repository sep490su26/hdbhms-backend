package com.sep490.hdbhms.occupancy.infrastructure.persistence.entity;

import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.UserEntity;
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
        name = "deposit_extension_events",
        indexes = {
                @Index(name = "idx_deposit_extension", columnList = "deposit_agreement_id, approved_at")
        }
)
public class DepositExtensionEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "deposit_extension_event_id")
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "deposit_agreement_id", nullable = false)
    DepositAgreementEntity depositAgreement;

    @Column(name = "old_expected_move_in_date", nullable = false)
    LocalDate oldExpectedMoveInDate;

    @Column(name = "new_expected_move_in_date", nullable = false)
    LocalDate newExpectedMoveInDate;

    @Column(name = "old_expires_at")
    LocalDate oldExpiresAt;

    @Column(name = "new_expires_at", nullable = false)
    LocalDate newExpiresAt;

    @Column(columnDefinition = "TEXT")
    String reason;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "approved_by", nullable = false)
    UserEntity approvedBy;

    @Column(name = "approved_at", nullable = false, columnDefinition = "DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)")
    LocalDateTime approvedAt;
}