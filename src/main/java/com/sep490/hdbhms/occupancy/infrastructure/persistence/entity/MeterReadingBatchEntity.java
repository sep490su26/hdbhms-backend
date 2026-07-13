package com.sep490.hdbhms.occupancy.infrastructure.persistence.entity;

import com.sep490.hdbhms.file.infrastructure.persistence.entity.FileMetadataEntity;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.UserEntity;
import com.sep490.hdbhms.occupancy.domain.value_objects.BatchSource;
import com.sep490.hdbhms.occupancy.domain.value_objects.BatchStatus;
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
        name = "meter_reading_batches",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_mrb_property_period", columnNames = {"property_id", "reading_period"})
        },
        indexes = {
                @Index(name = "idx_reading_batch", columnList = "property_id, reading_period, status")
        }
)
public class MeterReadingBatchEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "meter_reading_batch_id")
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    PropertyEntity property;

    @Column(name = "reading_period", nullable = false, length = 7, columnDefinition = "CHAR(7)")
    String readingPeriod;  // YYYY-MM

    @Column(name = "total_rooms", nullable = false)
    @Builder.Default
    Integer totalRooms = 0;

    @Column(name = "completed_rooms", nullable = false)
    @Builder.Default
    Integer completedRooms = 0;

    @Column(name = "anomaly_count", nullable = false)
    @Builder.Default
    Integer anomalyCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    BatchStatus status = BatchStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "imported_file_id", nullable = true)
    FileMetadataEntity importedFile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = true)
    UserEntity createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by", nullable = true)
    UserEntity confirmedBy;

    @Column(name = "confirmed_at")
    LocalDateTime confirmedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    LocalDateTime createdAt;
}
