package com.sep490.hdbhms.maintenance.infrastructure.persistence.entity;

import com.sep490.hdbhms.file.infrastructure.persistence.entity.FileMetadataEntity;
import com.sep490.hdbhms.maintenance.domain.value_objects.AttachmentPhase;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.TenantEntity;
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
        name = "maintenance_ticket_attachments",
        indexes = {
                @Index(name = "idx_mta_ticket", columnList = "ticket_id, attachment_phase")
        }
)
public class MaintenanceTicketAttachmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    MaintenanceTicketEntity ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "file_id", nullable = false)
    FileMetadataEntity file;

    @Enumerated(EnumType.STRING)
    @Column(name = "attachment_phase", nullable = false, length = 50)
    @Builder.Default
    AttachmentPhase attachmentPhase = AttachmentPhase.BEFORE;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    Integer sortOrder = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = true)
    TenantEntity createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    LocalDateTime createdAt;
}
