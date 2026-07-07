package com.sep490.hdbhms.occupancy.infrastructure.persistence.entity;

import com.sep490.hdbhms.billingandpayment.domain.valueObjects.DepositAgreementStatus;
import com.sep490.hdbhms.file.infrastructure.persistence.entity.FileMetadataEntity;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.PersonProfileEntity;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.UserEntity;
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
        name = "deposit_agreements",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_deposit_code", columnNames = "deposit_code")
        },
        indexes = {
                @Index(name = "idx_deposit_room_status", columnList = "room_id, status"),
                @Index(name = "idx_deposit_person", columnList = "depositor_person_profile_id, status")
        }
)
public class DepositAgreementEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "deposit_agreement_id")
    Long id;

    @Column(name = "deposit_code", nullable = false, length = 80)
    String depositCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    RoomEntity room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deposit_form_id", nullable = true)
    DepositFormEntity depositForm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = true)
    TenantEntity tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id", nullable = true)
    LeadEntity lead;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "depositor_person_profile_id", nullable = true)
    PersonProfileEntity depositorPersonProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_hold_id", nullable = true)
    RoomHoldEntity roomHold;

    @Column(nullable = false)
    Long amount;  // VND đồng

    @Column(name = "expected_move_in_date", nullable = false)
    LocalDate expectedMoveInDate;

    @Column(name = "expected_lease_sign_date", nullable = false)
    LocalDate expectedLeaseSignDate;

    @Column(name = "payment_due_at")
    LocalDateTime paymentDueAt;

    @Column(name = "deposit_expires_at")
    LocalDate depositExpiresAt;

    @Column(name = "extension_count", nullable = false, columnDefinition = "TINYINT UNSIGNED")
    @Builder.Default
    Integer extensionCount = 0;

    @Column(name = "max_extensions", nullable = false, columnDefinition = "TINYINT UNSIGNED")
    @Builder.Default
    Integer maxExtensions = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    DepositAgreementStatus status = DepositAgreementStatus.PENDING_PAYMENT;

    @Column(name = "confirmed_at")
    LocalDateTime confirmedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_file_id", nullable = true)
    FileMetadataEntity contractFile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "signed_file_id", nullable = true)
    FileMetadataEntity signedFile;

    @Column(name = "signed_at")
    LocalDateTime signedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "signed_uploaded_by", nullable = true)
    UserEntity signedUploadedBy;

    @Column(columnDefinition = "TEXT")
    String note;

    @Column(name = "forfeiture_reason", columnDefinition = "TEXT")
    String forfeitureReason;

    @Column(name = "refunded_amount")
    Long refundedAmount;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;
}