package com.sep490.hdbhms.advisor.infrastructure.config;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@ConfigurationProperties(prefix = "app.advisor")
public class AdvisorProperties {
    String baseUrl = "http://localhost:8000/api/v1/advisor";
    int timeoutMs = 120000;
}
