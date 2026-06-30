package com.sep490.hdbhms.occupancy.infrastructure.persistence.entity;

import com.sep490.hdbhms.occupancy.domain.valueObjects.MeterType;
import com.sep490.hdbhms.occupancy.domain.valueObjects.ValidationStatus;
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
        name = "meter_reading_import_rows",
        indexes = {
                @Index(name = "idx_mrir_batch", columnList = "batch_id")
        }
)
public class MeterReadingImportRowEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "meter_reading_import_row_id")
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    MeterReadingBatchEntity batch;

    @Column(name = "row_no", nullable = false)
    @Builder.Default
    Integer rowNo = 0;

    @Column(name = "room_code", nullable = false, length = 50)
    String roomCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "meter_type", nullable = false, length = 50)
    MeterType meterType;

    @Column(name = "previous_value", precision = 12, scale = 3)
    BigDecimal previousValue;

    @Column(name = "current_value", precision = 12, scale = 3)
    BigDecimal currentValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "validation_status", nullable = false, length = 50)
    @Builder.Default
    ValidationStatus validationStatus = ValidationStatus.VALID;

    @Column(name = "validation_message", columnDefinition = "TEXT")
    String validationMessage;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    LocalDateTime createdAt;
}