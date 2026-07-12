package com.sep490.hdbhms.notification.infrastructure.web.dto.request;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SendNotificationBroadcastRequest {
    String scopeType;
    List<Long> scopeIds;
    List<String> roles;
    List<String> channels;
    String title;
    String body;
}
