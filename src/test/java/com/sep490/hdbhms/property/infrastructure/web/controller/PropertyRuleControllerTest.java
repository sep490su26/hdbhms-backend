package com.sep490.hdbhms.property.infrastructure.web.controller;

import com.sep490.hdbhms.shared.exception.AppException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PropertyRuleControllerTest {

    @Test
    void normalizesRuleCodeForStorage() {
        assertEquals("FINE_WIFI_RESET", PropertyRuleController.normalizeRuleCode(" fine wifi reset "));
    }

    @Test
    void rejectsNegativeFineAmount() {
        assertThrows(AppException.class, () -> PropertyRuleController.normalizeFineAmount(-1L));
    }
}
