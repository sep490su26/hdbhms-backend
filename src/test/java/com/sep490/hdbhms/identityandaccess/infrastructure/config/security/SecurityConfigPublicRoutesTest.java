package com.sep490.hdbhms.identityandaccess.infrastructure.config.security;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityConfigPublicRoutesTest {

    @Test
    void publicPostRoutesDoNotExposeMockPaymentEndpoints() {
        boolean exposesMockPaymentRoute = Arrays.stream(SecurityConfig.PUBLIC_POST_URLS)
                .anyMatch(route -> route.contains("/api/v1/mock"));

        assertFalse(exposesMockPaymentRoute);
    }

    @Test
    void publicRoutesDoNotExposeTheWholeApi() {
        assertFalse(Stream.concat(
                        Arrays.stream(SecurityConfig.PUBLIC_GET_URLS),
                        Arrays.stream(SecurityConfig.PUBLIC_POST_URLS)
                )
                .anyMatch("/api/v1/**"::equals));
    }

    @Test
    void publicRoutesKeepOnlyExplicitGuestAndAuthenticationEntryPoints() {
        assertTrue(Arrays.asList(SecurityConfig.PUBLIC_POST_URLS).contains("/api/v1/auth/login"));
        assertTrue(Arrays.asList(SecurityConfig.PUBLIC_POST_URLS).contains("/api/v1/deposit/checkout"));
        assertTrue(Arrays.asList(SecurityConfig.PUBLIC_GET_URLS).contains("/api/v1/rooms"));
        assertFalse(Arrays.asList(SecurityConfig.PUBLIC_GET_URLS)
                .contains("/api/v1/rooms/*/meter-readings/latest"));
    }
}
