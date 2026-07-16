package com.sep490.hdbhms.occupancy.infrastructure.web.controller;

import com.sep490.hdbhms.occupancy.domain.value_objects.RoomStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoomCatalogVisibilityTest {

    @Test
    void publicCatalogContainsOnlyBookableRoomStates() {
        assertTrue(RoomCatalogController.isPublicRoomStatus(RoomStatus.VACANT));
        assertTrue(RoomCatalogController.isPublicRoomStatus(RoomStatus.SOON_VACANT));

        assertFalse(RoomCatalogController.isPublicRoomStatus(RoomStatus.DRAFT));
        assertFalse(RoomCatalogController.isPublicRoomStatus(RoomStatus.ON_HOLD));
        assertFalse(RoomCatalogController.isPublicRoomStatus(RoomStatus.RESERVED));
        assertFalse(RoomCatalogController.isPublicRoomStatus(RoomStatus.RESERVED_FOR_TRANSFER));
        assertFalse(RoomCatalogController.isPublicRoomStatus(RoomStatus.OCCUPIED));
        assertFalse(RoomCatalogController.isPublicRoomStatus(RoomStatus.MAINTENANCE));
        assertFalse(RoomCatalogController.isPublicRoomStatus(RoomStatus.EXPIRED));
    }
}
