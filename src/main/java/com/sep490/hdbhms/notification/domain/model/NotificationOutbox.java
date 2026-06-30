package com.sep490.hdbhms.notification.domain.model;

import com.sep490.hdbhms.notification.domain.valueObjects.NotificationChannel;
import com.sep490.hdbhms.notification.domain.valueObjects.OutboxStatus;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Builder
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationOutbox {
    Long id;
    String eventType;
    String targetType;
    Long targetId;
    Long recipientUserId;
    String recipientEmail;
    String recipientPhone;
    NotificationChannel channel;
    String title;
    String body;
    String payload;
    OutboxStatus status;
    @Builder.Default
    Integer retryCount = 0;
    Integer maxRetries;
    String lastError;
    Boolean isRead;
    LocalDateTime scheduledAt;
    LocalDateTime nextRetryAt;
    LocalDateTime sentAt;
    LocalDateTime createdAt;

    public void setSent() {
        this.status = OutboxStatus.SENT;
        this.sentAt = LocalDateTime.now();
        this.lastError = null;
    }

    public void markRetry(String errorMessage) {
        this.retryCount++;
        this.lastError = errorMessage;

        if (this.maxRetries != null && this.retryCount >= this.maxRetries) {
            this.status = OutboxStatus.DEAD_LETTER;
            this.nextRetryAt = null;
        } else {
            this.status = OutboxStatus.PENDING;
            long delayMinutes = (long) Math.pow(5, this.retryCount);
            this.nextRetryAt = LocalDateTime.now().plusMinutes(delayMinutes);
        }
    }

    public void markDeadLetter(String reason) {
        this.status = OutboxStatus.DEAD_LETTER;
        this.lastError = reason;
        this.nextRetryAt = null;
    }

    public void markAsRead() {
        this.isRead = true;
    }
}