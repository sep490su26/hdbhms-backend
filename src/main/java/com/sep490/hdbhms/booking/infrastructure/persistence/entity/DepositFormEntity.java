package com.sep490.hdbhms.booking.infrastructure.persistence.entity;

import com.sep490.hdbhms.property.infrastructure.persistence.entity.RoomEntity;

import com.sep490.hdbhms.file.infrastructure.persistence.entity.FileMetadataEntity;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Gender;
import com.sep490.hdbhms.booking.domain.value_objects.DepositFormStatus;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.DepositAgreementStatus;
import com.sep490.hdbhms.booking.infrastructure.persistence.entity.LeadEntity;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.PersonProfileEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.TenantEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "deposit_forms")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DepositFormEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "deposit_form_id")
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    RoomEntity room;

    @Column(name = "id_number", length = 50, nullable = false)
    String idNumber;

    @Column(name = "permanent_address", length = 1000)
    String permanentAddress;

    @Column(name = "id_issue_date")
    LocalDate idIssueDate;

    @Column(name = "id_issue_place", length = 255)
    String idIssuePlace;

    @Column(name = "dob")
    LocalDate dob;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 50)
    @Builder.Default
    Gender gender = Gender.UNKNOWN;

    @Column(name = "full_name", length = 255, nullable = false)
    String fullName;

    @Column(name = "email", length = 255, nullable = false)
    String email;

    @Column(name = "phone", length = 30, nullable = false)
    String phone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_front_file_id", nullable = true)
     FileMetadataEntity idFrontFile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_back_file_id", nullable = true)
    FileMetadataEntity idBackFile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portrait_file_id", nullable = true)
    FileMetadataEntity portraitFile;

    @Column(name = "deposit_months", columnDefinition = "INT UNSIGNED")
    Integer depositMonths;

    @Column(name = "contract_term_months", columnDefinition = "INT UNSIGNED")
    Integer contractTermMonths;

    @Column(name = "payment_cycle_months", columnDefinition = "TINYINT UNSIGNED")
    Integer paymentCycleMonths;

    @Column(name = "occupant_count", nullable = false, columnDefinition = "TINYINT UNSIGNED")
    @Builder.Default
    Integer occupantCount = 1;

    @OneToMany(mappedBy = "depositForm", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    List<DepositFormCoOccupantEntity> coOccupants = new ArrayList<>();

    @Column(name = "expected_move_in_date", nullable = false)
    LocalDate expectedMoveInDate;

    @Column(name = "expected_lease_sign_date", nullable = false)
    LocalDate expectedLeaseSignDate;

    @Column(name = "payment_due_at")
    LocalDateTime paymentDueAt;

    @Column(name = "deposit_expires_at")
    LocalDate depositExpiresAt;

    @Column(name = "deposit_code", length = 80, unique = true)
    String depositCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_hold_id")
    RoomHoldEntity roomHold;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    TenantEntity tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id")
    LeadEntity lead;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "depositor_person_profile_id")
    PersonProfileEntity depositorPersonProfile;

    @Column(name = "amount")
    Long amount;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "deposit_status", nullable = false, length = 50)
    DepositAgreementStatus depositStatus = DepositAgreementStatus.PENDING_PAYMENT;

    @Column(name = "extension_count", nullable = false, columnDefinition = "TINYINT UNSIGNED")
    @Builder.Default
    Integer extensionCount = 0;

    @Column(name = "max_extensions", nullable = false, columnDefinition = "TINYINT UNSIGNED")
    @Builder.Default
    Integer maxExtensions = 1;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    DepositFormStatus status = DepositFormStatus.APPROVAL_PENDING;

    @Column(name = "confirmed_at")
    LocalDateTime confirmedAt;

    @Column(name = "reject_reason", columnDefinition = "TEXT")
    String rejectReason;

    @Column(name = "note", columnDefinition = "TEXT")
    String note;

    @Column(name = "forfeiture_reason", columnDefinition = "TEXT")
    String forfeitureReason;

    @Column(name = "refunded_amount")
    Long refundedAmount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;
}
