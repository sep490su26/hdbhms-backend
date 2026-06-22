package com.sep490.hdbhms.notification.infrastructure.persistence.entity;

import com.sep490.hdbhms.notification.domain.value_objects.DeliveryStatus;
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
        name = "notification_deliveries",
        indexes = {
                @Index(name = "idx_delivery_outbox", columnList = "outbox_id"),
                @Index(name = "idx_delivery_status", columnList = "delivery_status, created_at"),
                @Index(name = "idx_delivery_read_status", columnList = "delivery_status, read_at, created_at")
        }
)
public class NotificationDeliveryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "outbox_id", nullable = false)
    NotificationOutboxEntity outbox;

    @Column(name = "provider_message_id", length = 255)
    String providerMessageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false, length = 50)
    DeliveryStatus deliveryStatus;

    @Column(name = "error_message", length = 1000)
    String errorMessage;

    @Column(name = "delivered_at")
    LocalDateTime deliveredAt;

    @Column(name = "read_at")
    LocalDateTime readAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    LocalDateTime createdAt;
}