package com.sep490.hdbhms.occupancy.infrastructure.web.controller;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MockOccupancyFlowControllerGuardTest {

    @Test
    void mockOccupancyFlowControllerRequiresDevLikeProfileAndExplicitFeatureFlag() {
        Profile profile = MockOccupancyFlowController.class.getAnnotation(Profile.class);
        ConditionalOnProperty conditional = MockOccupancyFlowController.class.getAnnotation(ConditionalOnProperty.class);

        assertNotNull(profile);
        assertArrayEquals(new String[]{"dev", "test", "local"}, profile.value());
        assertNotNull(conditional);
        assertArrayEquals(new String[]{"app.mock-occupancy-flow.enabled"}, conditional.name());
        assertEquals("true", conditional.havingValue());
    }
}
