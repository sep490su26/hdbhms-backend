package com.sep490.hdbhms.shared.event;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationEvent {
    String eventType;
    Long userId;
    String targetType;
    Long targetId;
    Map<String, Object> data;
}
