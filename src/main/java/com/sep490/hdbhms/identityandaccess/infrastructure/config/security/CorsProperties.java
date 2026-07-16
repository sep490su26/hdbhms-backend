package com.sep490.hdbhms.identityandaccess.infrastructure.config.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {
    private List<String> allowedOriginPatterns = new ArrayList<>();

    public List<String> configuredOriginPatterns() {
        return allowedOriginPatterns.stream()
                .map(String::trim)
                .filter(pattern -> !pattern.isEmpty())
                .toList();
    }
}
