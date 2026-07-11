package com.sep490.hdbhms.occupancy.infrastructure.persistence.entity;

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
@Table(name = "visit_requests")
public class VisitRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "visit_request_id")
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    PropertyEntity property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = true)
    RoomEntity room;

    @Column(name = "visitor_name", nullable = false, length = 255)
    String visitorName;

    @Column(name = "visitor_phone", nullable = false, length = 30)
    String visitorPhone;

    @Column(name = "visitor_email", length = 255)
    String visitorEmail;

    @Column(name = "preferred_start", nullable = false)
    LocalDateTime preferredStart;

    @Column(columnDefinition = "TEXT")
    String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    VisitRequestStatus status;

    @Column(name = "deleted_at")
    LocalDateTime deletedAt;

    @Column(name = "deleted_by")
    Long deletedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    LocalDateTime updatedAt;
}