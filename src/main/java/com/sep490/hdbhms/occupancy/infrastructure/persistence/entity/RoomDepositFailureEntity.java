package com.sep490.hdbhms.occupancy.infrastructure.persistence.entity;

import com.sep490.hdbhms.occupancy.domain.value_objects.RoomDepositFailureReason;
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
        name = "room_deposit_failures",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_room_deposit_failure_hold", columnNames = "room_hold_id")
        },
        indexes = {
                @Index(name = "idx_room_deposit_failures_room_time", columnList = "room_id, occurred_at"),
                @Index(name = "idx_room_deposit_failures_payment_intent", columnList = "payment_intent_id")
        }
)
public class RoomDepositFailureEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_deposit_failure_id")
    Long id;

    @Column(name = "room_id", nullable = false)
    Long roomId;

    @Column(name = "room_hold_id")
    Long roomHoldId;

    @Column(name = "payment_intent_id")
    Long paymentIntentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    RoomDepositFailureReason reason;

    @Column(name = "occurred_at", nullable = false)
    LocalDateTime occurredAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    LocalDateTime createdAt;
}
