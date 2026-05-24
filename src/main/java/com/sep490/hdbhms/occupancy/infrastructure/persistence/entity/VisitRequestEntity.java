package com.sep490.hdbhms.occupancy.infrastructure.persistence.entity;

import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.UserEntity;
import com.sep490.hdbhms.occupancy.domain.value_objects.VisitRequestStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(
        name = "visit_requests",
        indexes = {
                @Index(name = "idx_visit_status", columnList = "status, preferred_start"),
                @Index(name = "idx_visit_property", columnList = "property_id")
        }
)
public class VisitRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = true)
    PropertyEntity property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = true)
    RoomEntity room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id", nullable = true)
    LeadEntity lead;

    @Column(name = "visitor_name", nullable = false, length = 255)
    String visitorName;

    @Column(name = "visitor_phone", nullable = false, length = 30)
    String visitorPhone;

    @Column(name = "visitor_email", length = 255)
    String visitorEmail;

    @Column(name = "preferred_start", nullable = false)
    LocalDateTime preferredStart;

    @Column(name = "preferred_end")
    LocalDateTime preferredEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    VisitRequestStatus status = VisitRequestStatus.REQUESTED;

    @Column(columnDefinition = "TEXT")
    String notes;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    UserEntity createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;
}