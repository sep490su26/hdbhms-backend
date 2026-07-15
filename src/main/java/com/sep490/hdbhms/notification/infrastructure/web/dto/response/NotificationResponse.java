package com.sep490.hdbhms.notification.infrastructure.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationResponse {
    Long id;
    String title;
    String body;

    String eventType;
    String targetType;
    Long targetId;
    Map<String, String> data;
    LocalDateTime createdAt;
    LocalDateTime readAt;
    boolean isRead;
}
