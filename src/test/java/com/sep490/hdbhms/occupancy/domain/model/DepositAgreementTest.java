package com.sep490.hdbhms.occupancy.domain.model;

import com.sep490.hdbhms.booking.domain.model.DepositAgreement;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DepositAgreementTest {

    @Test
    void buildDepositCodeUsesDepositPrefix() {
        assertEquals(
                "COC_101_2026-07-20",
                DepositAgreement.buildDepositCode("101", LocalDate.of(2026, 7, 20))
        );
        assertEquals(
                "COC_P101_2026-07-20",
                DepositAgreement.buildDepositCode("P101", LocalDate.of(2026, 7, 20))
        );
    }
}
