package com.sep490.hdbhms.occupancy.infrastructure.persistence.entity;

import com.sep490.hdbhms.file.infrastructure.persistence.entity.FileMetadataEntity;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.UserEntity;
import com.sep490.hdbhms.occupancy.domain.value_objects.ReadingPurpose;
import com.sep490.hdbhms.occupancy.domain.value_objects.ReadingSource;
import com.sep490.hdbhms.occupancy.domain.value_objects.ReadingStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.MeterReadingReviewStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(
        name = "meter_readings",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_meter_period_active", columnNames = "active_reading_key"),
                @UniqueConstraint(name = "uq_meter_period_revision", columnNames = {"meter_id", "reading_period", "revision_no"})
        },
        indexes = {
                @Index(name = "idx_reading_room_period", columnList = "room_id, reading_period")
        }
)
public class MeterReadingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "meter_reading_id")
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = true)
    MeterReadingBatchEntity batch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meter_id", nullable = false)
    MeterEntity meter;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    RoomEntity room;

    @Column(name = "reading_period", nullable = false, length = 7, columnDefinition = "CHAR(7)")
    String readingPeriod;

    @Column(name = "revision_no", nullable = false)
    @Builder.Default
    Integer revisionNo = 1;

    @Column(name = "previous_value", nullable = false, precision = 12, scale = 3)
    BigDecimal previousValue;

    @Column(name = "current_value", nullable = false, precision = 12, scale = 3)
    BigDecimal currentValue;

    @Column(name = "usage_amount", insertable = false, updatable = false, precision = 12, scale = 3)
    BigDecimal usageAmount;  // generated column

    @Column(name = "reading_date", nullable = false)
    LocalDate readingDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "photo_file_id", nullable = true)
    FileMetadataEntity photoFile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    ReadingPurpose purpose = ReadingPurpose.MONTHLY;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    ReadingSource source = ReadingSource.MANUAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    ReadingStatus status = ReadingStatus.CONFIRMED;

    @Column(name = "void_reason", length = 1000)
    String voidReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = true)
    UserEntity createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    LocalDateTime createdAt;

    @Column(name = "active_reading_key", insertable = false, updatable = false, length = 255)
    String activeReadingKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 50)
    @Builder.Default
    MeterReadingReviewStatus reviewStatus = MeterReadingReviewStatus.NONE;

    @Column(name = "review_count", nullable = false)
    @Builder.Default
    Integer reviewCount = 0;
}
