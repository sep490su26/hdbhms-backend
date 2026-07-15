package com.sep490.hdbhms.notification.infrastructure.web.dto.request;

import com.sep490.hdbhms.notification.domain.value_objects.TemplateStatus;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateNotificationTemplateRequest {
    String titleTemplate;
    String bodyTemplate;
    TemplateStatus status;
}