package com.sep490.hdbhms.occupancy.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DepositAgreementTest {

    @Test
    void buildDepositCodeUsesDepositContractFilenameWithoutExtension() {
        assertEquals(
                "HDC_P101_20_07_2026",
                DepositAgreement.buildDepositCode("101", LocalDate.of(2026, 7, 20))
        );
        assertEquals(
                "HDC_P101_20_07_2026",
                DepositAgreement.buildDepositCode("P101", LocalDate.of(2026, 7, 20))
        );
    }
}
