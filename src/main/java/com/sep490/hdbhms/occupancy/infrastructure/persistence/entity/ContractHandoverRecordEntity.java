package com.sep490.hdbhms.occupancy.infrastructure.persistence.entity;

import com.sep490.hdbhms.file.infrastructure.persistence.entity.FileMetadataEntity;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.UserEntity;
import com.sep490.hdbhms.occupancy.domain.value_objects.HandoverStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.HandoverType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(
        name = "contract_handover_records",
        indexes = {
                @Index(name = "idx_chr_contract", columnList = "contract_id, handover_type"),
                @Index(name = "idx_chr_room", columnList = "room_id, handover_date")
        }
)
public class ContractHandoverRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_id", nullable = false)
    LeaseContractEntity contract;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    RoomEntity room;

    @Enumerated(EnumType.STRING)
    @Column(name = "handover_type", nullable = false, length = 50)
    HandoverType handoverType;

    @Column(name = "handover_date", nullable = false)
    LocalDateTime handoverDate;               // DATETIME(6)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "electricity_reading_id", nullable = true)
    MeterReadingEntity electricityReading;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "water_reading_id", nullable = true)
    MeterReadingEntity waterReading;

    @Column(name = "note", columnDefinition = "TEXT")
    String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "signed_document_id", nullable = true)
    FileMetadataEntity signedDocument;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    HandoverStatus status = HandoverStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by", nullable = true)
    UserEntity confirmedBy;

    @Column(name = "confirmed_at")
    LocalDateTime confirmedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    LocalDateTime createdAt;
}

