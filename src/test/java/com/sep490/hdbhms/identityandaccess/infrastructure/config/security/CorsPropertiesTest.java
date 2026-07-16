package com.sep490.hdbhms.identityandaccess.infrastructure.config.security;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CorsPropertiesTest {

    @Test
    void emptyConfigurationFailsClosedWithoutAddingAWildcard() {
        CorsProperties properties = new CorsProperties();

        assertEquals(List.of(), properties.configuredOriginPatterns());
        assertFalse(properties.configuredOriginPatterns().contains("*"));
    }

    @Test
    void configuredOriginsAreTrimmedAndBlankEntriesAreIgnored() {
        CorsProperties properties = new CorsProperties();
        properties.setAllowedOriginPatterns(List.of(" http://localhost:* ", "", "https://staging.example"));

        assertEquals(
                List.of("http://localhost:*", "https://staging.example"),
                properties.configuredOriginPatterns()
        );
    }
}
