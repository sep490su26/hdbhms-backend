package com.sep490.hdbhms.occupancy.infrastructure.persistence.entity;

import com.sep490.hdbhms.occupancy.domain.valueObjects.MeterStatus;
import com.sep490.hdbhms.occupancy.domain.valueObjects.MeterType;
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
        name = "meters",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_room_active_meter_type", columnNames = "active_meter_key")
        },
        indexes = {
                @Index(name = "idx_meter_room", columnList = "room_id")
        }
)
public class MeterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "meter_id")
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    RoomEntity room;

    @Enumerated(EnumType.STRING)
    @Column(name = "meter_type", nullable = false, length = 50)
    MeterType meterType;

    @Column(name = "meter_code", length = 100)
    String meterCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    MeterStatus status = MeterStatus.ACTIVE;

    @Column(name = "installed_at")
    LocalDate installedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    LocalDateTime createdAt;

    @Column(name = "active_meter_key", insertable = false, updatable = false, length = 255)
    String activeMeterKey;
}