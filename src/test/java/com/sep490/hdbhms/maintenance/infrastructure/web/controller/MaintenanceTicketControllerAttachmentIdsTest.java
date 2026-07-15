package com.sep490.hdbhms.maintenance.infrastructure.web.controller;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MaintenanceTicketControllerAttachmentIdsTest {

    @Test
    void preservesOneTwoAndThreeAttachmentOccurrencesInOrder() {
        assertEquals(
                List.of(11L),
                MaintenanceTicketController.attachmentIdsPreservingOrder(List.of(11L))
        );
        assertEquals(
                List.of(11L, 11L),
                MaintenanceTicketController.attachmentIdsPreservingOrder(List.of(11L, 11L))
        );
        assertEquals(
                List.of(11L, 12L, 11L),
                MaintenanceTicketController.attachmentIdsPreservingOrder(List.of(11L, 12L, 11L))
        );
    }

    @Test
    void removesOnlyNullValues() {
        assertEquals(
                List.of(11L, 12L),
                MaintenanceTicketController.attachmentIdsPreservingOrder(
                        java.util.Arrays.asList(11L, null, 12L)
                )
        );
    }
}
