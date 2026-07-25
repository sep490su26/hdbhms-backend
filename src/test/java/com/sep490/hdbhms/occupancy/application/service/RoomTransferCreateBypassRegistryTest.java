package com.sep490.hdbhms.occupancy.application.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoomTransferCreateBypassRegistryTest {
    @Test
    void consumesMatchingGrantOnce() {
        RoomTransferCreateBypassRegistry registry = new RoomTransferCreateBypassRegistry();

        registry.enable(1L, 10L, 20L, 1, 15);

        assertTrue(registry.consumeIfAllowed(1L, 10L, 20L));
        assertFalse(registry.consumeIfAllowed(1L, 10L, 20L));
    }

    @Test
    void nullGrantFieldsMatchAnyRequesterOrTargetRoom() {
        RoomTransferCreateBypassRegistry registry = new RoomTransferCreateBypassRegistry();

        registry.enable(null, 10L, null, 1, 15);

        assertTrue(registry.consumeIfAllowed(99L, 10L, 88L));
    }
}
