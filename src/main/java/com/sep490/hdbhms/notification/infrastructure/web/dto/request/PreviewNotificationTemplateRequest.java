package com.sep490.hdbhms.notification.infrastructure.web.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PreviewNotificationTemplateRequest {
    String titleTemplate;
    String bodyTemplate;
    Map<String, Object> data;
}
