package com.sep490.hdbhms.occupancy.infrastructure.web.dto.request;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CreateTransferRequestRequestTest {

    @Test
    void normalizesTransferDatesToTheFirstDayOfSelectedMonth() {
        var request = new CreateTransferRequestRequest(
                10L,
                20L,
                LocalDate.of(2026, 8, 19),
                LocalDate.of(2026, 8, 22),
                null,
                null
        );

        assertEquals(LocalDate.of(2026, 8, 1), request.requestedTransferDate());
        assertEquals(LocalDate.of(2026, 8, 1), request.expectedTransferDate());
    }

    @Test
    void defaultsMissingTransferDatesToTheFirstDayOfNextMonth() {
        var request = new CreateTransferRequestRequest(
                10L,
                20L,
                null,
                null,
                null,
                null
        );
        var expected = LocalDate.now().plusMonths(1).withDayOfMonth(1);

        assertEquals(expected, request.requestedTransferDate());
        assertEquals(expected, request.expectedTransferDate());
    }
}
