package com.sep490.hdbhms.occupancy.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "floor_plan_items",
        indexes = {
                @Index(name = "idx_floor_plan_items_property_floor", columnList = "property_id,floor_id"),
                @Index(name = "idx_floor_plan_items_room", columnList = "room_id")
        }
)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FloorPlanItemEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    PropertyEntity property;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "floor_id", nullable = false)
    FloorEntity floor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    RoomEntity room;

    @Column(name = "type", nullable = false, length = 50)
    String type;

    @Column(name = "position_x", nullable = false)
    Integer positionX;

    @Column(name = "position_y", nullable = false)
    Integer positionY;

    @Column(name = "width", nullable = false)
    Integer width;

    @Column(name = "height", nullable = false)
    Integer height;

    @Column(name = "metadata", columnDefinition = "json")
    String metadata;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;
}
