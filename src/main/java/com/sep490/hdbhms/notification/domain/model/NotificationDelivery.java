package com.sep490.hdbhms.notification.domain.model;

import com.sep490.hdbhms.notification.domain.value_objects.DeliveryStatus;
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
public class NotificationDelivery {
    Long id;
    Long outboxId;
    String providerMessageId;
    DeliveryStatus deliveryStatus;
    String errorMessage;
    LocalDateTime deliveredAt;
    LocalDateTime readAt;
    LocalDateTime createdAt;
}