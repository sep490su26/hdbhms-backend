package com.sep490.hdbhms.notification.domain.model;

import com.sep490.hdbhms.notification.domain.valueObjects.NotificationChannel;
import com.sep490.hdbhms.notification.domain.valueObjects.TemplateStatus;
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
public class NotificationTemplate {
    Long id;
    String templateKey;
    NotificationChannel channel;
    String titleTemplate;
    String bodyTemplate;
    TemplateStatus status;
    LocalDateTime createdAt;
}