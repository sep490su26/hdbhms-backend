package com.sep490.hdbhms.identityandaccess.infrastructure.config.security;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;

class SecurityConfigPublicRoutesTest {

    @Test
    void publicPostRoutesDoNotExposeMockPaymentEndpoints() {
        boolean exposesMockPaymentRoute = Arrays.stream(SecurityConfig.PUBLIC_POST_URLS)
                .anyMatch(route -> route.contains("/api/v1/mock"));

        assertFalse(exposesMockPaymentRoute);
    }
}
