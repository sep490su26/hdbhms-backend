package com.sep490.hdbhms.billingandpayment.infrastructure.web.controller;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MockPaymentControllerGuardTest {

    @Test
    void mockPaymentControllerRequiresDevLikeProfileAndExplicitFeatureFlag() {
        Profile profile = MockPaymentController.class.getAnnotation(Profile.class);
        ConditionalOnProperty conditional = MockPaymentController.class.getAnnotation(ConditionalOnProperty.class);

        assertNotNull(profile);
        assertArrayEquals(new String[]{"dev", "test", "local"}, profile.value());
        assertNotNull(conditional);
        assertArrayEquals(new String[]{"app.mock-payment.enabled"}, conditional.name());
        assertEquals("true", conditional.havingValue());
    }
}
