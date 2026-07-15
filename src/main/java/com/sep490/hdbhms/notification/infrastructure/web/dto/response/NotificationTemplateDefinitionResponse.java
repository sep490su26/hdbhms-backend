package com.sep490.hdbhms.notification.infrastructure.web.dto.response;

import com.sep490.hdbhms.notification.domain.value_objects.NotificationChannel;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationTemplateDefinitionResponse {
    String eventType;
    String displayName;
    String description;
    String targetType;
    List<NotificationChannel> allowedChannels;
    List<NotificationTemplateVariableResponse> variables;
    Map<String, Object> sampleData;
}
