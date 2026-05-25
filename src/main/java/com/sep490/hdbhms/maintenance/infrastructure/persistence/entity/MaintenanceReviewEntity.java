package com.sep490.hdbhms.maintenance.infrastructure.persistence.entity;

import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.UserEntity;
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
        name = "maintenance_reviews",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_ticket_review_user", columnNames = {"ticket_id", "reviewer_user_id"})
        }
)
public class MaintenanceReviewEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    MaintenanceTicketEntity ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reviewer_user_id", nullable = false)
    UserEntity reviewerUser;

    @Column(nullable = false, columnDefinition = "TINYINT UNSIGNED")
    @Builder.Default
    Integer rating = 5;

    @Column(columnDefinition = "TEXT")
    String comment;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    LocalDateTime createdAt;
}