package com.sep490.hdbhms.notification.infrastructure.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sep490.hdbhms.notification.domain.value_objects.NotificationChannel;
import com.sep490.hdbhms.notification.domain.value_objects.TemplateStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationTemplateResponse {
    String eventType;
    String displayName;
    String targetType;
    NotificationChannel channel;
    String source;
    TemplateStatus status;
    String titleTemplate;
    String bodyTemplate;
    List<NotificationTemplateVariableResponse> variables;
    Long updatedBy;
    LocalDateTime updatedAt;
}
