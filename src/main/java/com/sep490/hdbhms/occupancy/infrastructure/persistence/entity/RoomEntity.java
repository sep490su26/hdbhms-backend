package com.sep490.hdbhms.occupancy.infrastructure.persistence.entity;

import com.sep490.hdbhms.occupancy.domain.valueObjects.RoomStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "rooms")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    PropertyEntity property;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "floor_id", nullable = false)
    FloorEntity floor;

    @Column(name = "room_code", length = 50, nullable = false)
    String roomCode;

    @Column(name = "name", length = 100, nullable = false)
    String name;

    @Column(name = "area_m2", precision = 8, scale = 2)
    BigDecimal areaM2;

    @Builder.Default
    @Column(name = "listed_price", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    Long listedPrice = 0L;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "current_status", nullable = false, length = 50)
    RoomStatus currentStatus = RoomStatus.VACANT;

    @Builder.Default
    @Column(name = "max_occupants", nullable = false, columnDefinition = "TINYINT UNSIGNED")
    Integer maxOccupants = 3;

    @Column(name = "public_note", columnDefinition = "TEXT")
    String publicNote;

    @Column(name = "internal_note", columnDefinition = "TEXT")
    String internalNote;

    @Column(name = "position_x")
    Integer positionX;

    @Column(name = "position_y")
    Integer positionY;

    @Builder.Default
    @Column(name = "sort_order", nullable = false)
    Integer sortOrder = 0;

    @OneToMany(mappedBy = "room", fetch = FetchType.LAZY)
    List<RoomImageEntity> images = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    LocalDateTime deletedAt;

    @Version
    @Builder.Default
    @Column(name = "version", nullable = false)
    Long version = 0L;
}