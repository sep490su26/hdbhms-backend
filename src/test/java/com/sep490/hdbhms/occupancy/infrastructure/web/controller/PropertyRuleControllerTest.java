package com.sep490.hdbhms.occupancy.infrastructure.web.controller;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PropertyRuleControllerTest {

    @Test
    void normalizesRuleCodeForStorage() {
        assertEquals("FINE_WIFI_RESET", PropertyRuleController.normalizeRuleCode(" fine wifi reset "));
    }

    @Test
    void rejectsNegativeFineAmount() {
        assertThrows(ResponseStatusException.class, () -> PropertyRuleController.normalizeFineAmount(-1L));
    }
}
