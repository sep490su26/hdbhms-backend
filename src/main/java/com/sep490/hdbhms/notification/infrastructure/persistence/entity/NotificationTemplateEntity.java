package com.sep490.hdbhms.notification.infrastructure.persistence.entity;

import com.sep490.hdbhms.notification.domain.value_objects.NotificationChannel;
import com.sep490.hdbhms.notification.domain.value_objects.TemplateStatus;
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
        name = "notification_templates",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_template", columnNames = {"template_key", "channel"})
        }
)
public class NotificationTemplateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_template_id")
    Long id;

    @Column(name = "template_key", nullable = false, length = 100)
    String templateKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    NotificationChannel channel;

    @Column(name = "title_template", nullable = false, length = 255)
    String titleTemplate;

    @Column(name = "body_template", nullable = false, columnDefinition = "TEXT")
    String bodyTemplate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    TemplateStatus status = TemplateStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    LocalDateTime createdAt;
}