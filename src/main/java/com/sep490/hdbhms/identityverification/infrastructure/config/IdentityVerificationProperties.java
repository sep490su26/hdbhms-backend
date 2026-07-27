package com.sep490.hdbhms.identityverification.infrastructure.config;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@ConfigurationProperties(prefix = "app.identity-verification")
public class IdentityVerificationProperties {
    String visionBaseUrl = "http://localhost:8001/api/v1";
    int visionTimeoutMs = 180000;
}
