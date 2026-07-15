package com.sep490.hdbhms.changerequest.infrastructure.persistence.entity;

import com.sep490.hdbhms.changerequest.domain.value_objects.AssignedRole;
import com.sep490.hdbhms.changerequest.domain.value_objects.RequestStatus;
import com.sep490.hdbhms.changerequest.domain.value_objects.RequestType;
import com.sep490.hdbhms.changerequest.domain.value_objects.RequesterRole;
import com.sep490.hdbhms.changerequest.domain.value_objects.TargetType;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.UserEntity;
import com.sep490.hdbhms.file.infrastructure.persistence.entity.FileMetadataEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "change_requests")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChangeRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "change_request_id")
    Long id;

    @Column(name = "request_code", nullable = false, unique = true, length = 80)
    String requestCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false)
    RequestType requestType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    UserEntity requester;

    @Enumerated(EnumType.STRING)
    @Column(name = "requester_role", nullable = false)
    RequesterRole requesterRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    TargetType targetType;

    @Column(name = "target_id")
    Long targetId;

    @Column(name = "title", nullable = false)
    String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    String description;

    @Column(name = "request_payload", columnDefinition = "JSON")
    String requestPayload;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evidence_file_id")
    FileMetadataEntity evidenceFile;

    @Enumerated(EnumType.STRING)
    @Column(name = "assigned_role", nullable = false)
    AssignedRole assignedRole;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    UserEntity assignedTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    RequestStatus status = RequestStatus.PENDING;

    @Column(name = "resolution_note", columnDefinition = "TEXT")
    String resolutionNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    UserEntity resolvedBy;

    @Column(name = "resolved_at")
    LocalDateTime resolvedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;
}