package com.sep490.hdbhms.file.infrastructure.persistence.entity;

import com.sep490.hdbhms.file.domain.value_objects.FileCategory;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.Where;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "file_metadata", indexes = {
        @Index(name = "idx_files_tenant_category", columnList = "category, created_at"),
        @Index(name = "idx_files_owner", columnList = "owner_user_id")
})
@SQLDelete(sql = "UPDATE file_metadata SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class FileMetadataEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id", foreignKey = @ForeignKey(name = "fk_files_owner"))
    UserEntity owner;

    @Column(name = "storage_key", length = 1000, nullable = false)
    String storageKey;

    @Column(name = "original_name")
    String originalName;

    @Column(name = "mime_type", length = 100)
    String mimeType;

    @Column(name = "size_bytes")
    Long sizeBytes;

    @Column(name = "sha256_checksum", length = 64, columnDefinition = "CHAR(64)")
    String sha256Checksum;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "ENUM('ROOM_IMAGE','PROPERTY_IMAGE','PORTRAIT_PHOTO','ID_CARD','CONTRACT','DEPOSIT_CONTRACT','METER_PHOTO','VEHICLE_PHOTO','MAINTENANCE','TICKET_ATTACHMENT','RECEIPT','OCR_INPUT','OTHER') DEFAULT 'OTHER'")
    FileCategory category = FileCategory.OTHER;

    @Builder.Default
    @Column(name = "is_sensitive", nullable = false)
    boolean isSensitive = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @Column(name = "deleted_at")
    LocalDateTime deletedAt;
}
