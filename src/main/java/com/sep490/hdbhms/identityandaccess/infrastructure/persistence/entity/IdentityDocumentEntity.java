package com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity;

import com.sep490.hdbhms.file.infrastructure.persistence.entity.FileMetadataEntity;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.DocumentStatus;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.DocumentType;
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
        name = "identity_documents",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_doc_number_tenant", columnNames = {"doc_type", "doc_number"})
        },
        indexes = {
                @Index(name = "idx_doc_profile", columnList = "profile_id")
        }
)
public class IdentityDocumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "identity_document_id")
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    PersonProfileEntity profile;

    @Enumerated(EnumType.STRING)
    @Column(name = "doc_type", nullable = false, length = 50)
    @Builder.Default
    DocumentType docType = DocumentType.CCCD;

    @Column(name = "doc_number", nullable = false, length = 50)
    String docNumber;

    @Column(name = "issued_date")
    LocalDate issuedDate;

    @Column(name = "issued_place", length = 255)
    String issuedPlace;

    @Column(name = "expiry_date")
    LocalDate expiryDate;

    @Column(name = "raw_ocr_data", columnDefinition = "BLOB")
    byte[] rawOcrData;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "front_file_id")
    FileMetadataEntity frontFile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "back_file_id")
    FileMetadataEntity backFile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    DocumentStatus status = DocumentStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;
}