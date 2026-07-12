package com.sep490.hdbhms.notification.infrastructure.web.dto.response;

import com.sep490.hdbhms.notification.domain.value_objects.NotificationChannel;
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
public class NotificationBroadcastResponse {
    String scopeType;
    List<String> roles;
    List<NotificationChannel> channels;
    int recipientCount;
    int outboxCount;
}
