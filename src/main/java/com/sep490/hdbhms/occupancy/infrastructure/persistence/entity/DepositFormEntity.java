package com.sep490.hdbhms.occupancy.infrastructure.persistence.entity;

import com.sep490.hdbhms.occupancy.domain.value_objects.DepositFormStatus;
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
@Table(name = "deposit_forms")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DepositFormEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    RoomEntity room;

    @Column(name = "id_number", length = 50, nullable = false)
    String idNumber;

    @Column(name = "full_name", length = 255, nullable = false)
    String fullName;

    @Column(name = "email", length = 255, nullable = false)
    String email;

    @Column(name = "phone", length = 30, nullable = false)
    String phone;

    @Column(name = "expected_move_in_date", nullable = false)
    LocalDate expectedMoveInDate;

    @Column(name = "expected_lease_sign_date", nullable = false)
    LocalDate expectedLeaseSignDate;

    @Column(name = "payment_due_at")
    LocalDateTime paymentDueAt;

    @Column(name = "deposit_expires_at")
    LocalDate depositExpiresAt;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    DepositFormStatus status = DepositFormStatus.APPROVAL_PENDING;

    @Column(name = "confirmed_at")
    LocalDateTime confirmedAt;

    @Column(name = "reject_reason", columnDefinition = "TEXT")
    String rejectReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;
}