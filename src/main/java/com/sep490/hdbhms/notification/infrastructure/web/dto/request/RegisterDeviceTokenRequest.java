package com.sep490.hdbhms.notification.infrastructure.web.dto.request;

import com.sep490.hdbhms.notification.domain.value_objects.Platform;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RegisterDeviceTokenRequest {
    String token;
    Platform platform;
}
