package com.sep490.hdbhms.notification.infrastructure.persistence.mapper;

import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaUserRepository;
import com.sep490.hdbhms.notification.domain.model.NotificationOutbox;
import com.sep490.hdbhms.notification.infrastructure.persistence.entity.NotificationOutboxEntity;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationOutboxPersistenceMapper {

    JpaUserRepository jpaUserRepository;

    public NotificationOutbox toDomain(NotificationOutboxEntity entity) {
        if (entity == null) return null;
        return NotificationOutbox.builder()
                .id(entity.getId())
                .eventType(entity.getEventType())
                .targetType(entity.getTargetType())
                .targetId(entity.getTargetId())
                .recipientUserId(entity.getRecipientUser() != null ? entity.getRecipientUser().getId() : null)
                .recipientEmail(entity.getRecipientEmail())
                .recipientPhone(entity.getRecipientPhone())
                .channel(entity.getChannel())
                .title(entity.getTitle())
                .body(entity.getBody())
                .payload(entity.getPayload())
                .status(entity.getStatus())
                .retryCount(entity.getRetryCount())
                .maxRetries(entity.getMaxRetries())
                .lastError(entity.getLastError())
                .isRead(entity.getIsRead())
                .readAt(entity.getReadAt())
                .scheduledAt(entity.getScheduledAt())
                .nextRetryAt(entity.getNextRetryAt())
                .sentAt(entity.getSentAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public NotificationOutboxEntity toEntity(NotificationOutbox domain) {
        if (domain == null) return null;
        return NotificationOutboxEntity.builder()
                .id(domain.getId())
                .eventType(domain.getEventType())
                .targetType(domain.getTargetType())
                .targetId(domain.getTargetId())
                .recipientUser(domain.getRecipientUserId() != null
                        ? jpaUserRepository.getReferenceById(domain.getRecipientUserId())
                        : null)
                .recipientEmail(domain.getRecipientEmail())
                .recipientPhone(domain.getRecipientPhone())
                .channel(domain.getChannel())
                .title(domain.getTitle())
                .body(domain.getBody())
                .payload(domain.getPayload())
                .status(domain.getStatus())
                .retryCount(domain.getRetryCount())
                .maxRetries(domain.getMaxRetries())
                .lastError(domain.getLastError())
                .isRead(domain.getIsRead())
                .readAt(domain.getReadAt())
                .scheduledAt(domain.getScheduledAt())
                .nextRetryAt(domain.getNextRetryAt())
                .sentAt(domain.getSentAt())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
