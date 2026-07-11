package com.sep490.hdbhms.occupancy.infrastructure.persistence.entity;

import com.sep490.hdbhms.occupancy.domain.value_objects.RoomStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(
        name = "room_status_display_configs",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_room_status_display", columnNames = "room_status")
        }
)
public class RoomStatusDisplayConfigEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_status_display_config_id")
    Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "room_status", nullable = false, length = 50)
    RoomStatus roomStatus;

    @Column(name = "color_hex", nullable = false, length = 20)
    String colorHex;

    @Column(nullable = false, length = 100)
    String label;
}