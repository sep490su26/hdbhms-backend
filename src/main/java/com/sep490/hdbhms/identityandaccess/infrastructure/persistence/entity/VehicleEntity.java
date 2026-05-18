package com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity;

import com.sep490.hdbhms.file.infrastructure.persistence.entity.FileMetadataEntity;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.VehicleStatus;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.VehicleType;
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
        name = "vehicles",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_vehicle_plate_active", columnNames = {"license_plate", "active_unique_token"})
        },
        indexes = {
                @Index(name = "idx_vehicle_profile", columnList = "status")
        }
)
public class VehicleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    PersonProfileEntity profile;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false, length = 50)
    @Builder.Default
    VehicleType vehicleType = VehicleType.MOTORBIKE;

    @Column(name = "license_plate", nullable = false, length = 50)
    String licensePlate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_file_id")
    FileMetadataEntity imageFile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    VehicleStatus status = VehicleStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    LocalDateTime createdAt;

    @Column(name = "deleted_at")
    LocalDateTime deletedAt;

    @Column(name = "active_unique_token", insertable = false, updatable = false, columnDefinition = "TINYINT")
    Short activeUniqueToken;
}
