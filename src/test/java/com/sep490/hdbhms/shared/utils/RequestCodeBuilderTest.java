package com.sep490.hdbhms.shared.utils;

import com.sep490.hdbhms.changerequest.domain.value_objects.RequestType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RequestCodeBuilderTest {

    @Test
    void buildsTypeRoomDateCode() {
        assertEquals(
                "GHHD_P101_29_07_2026",
                RequestCodeBuilder.build(RequestType.CONTRACT_RENEWAL, "101", LocalDate.of(2026, 7, 29))
        );
    }

    @Test
    void keepsExistingRoomPrefixAndSanitizesRoomCode() {
        assertEquals(
                "TLHD_PA101_29_07_2026",
                RequestCodeBuilder.build(RequestType.CONTRACT_LIQUIDATION, "P/A:101", LocalDate.of(2026, 7, 29))
        );
    }

    @Test
    void fallsBackForMissingRoomAndDate() {
        assertEquals(
                "CP_PX_Chua-Ro-Ngay",
                RequestCodeBuilder.build(RequestType.ROOM_TRANSFER, null, null)
        );
    }

    @Test
    void addsSequenceOnlyWhenNeeded() {
        assertEquals("DYC_P101_29_07_2026", RequestCodeBuilder.withSequence("DYC_P101_29_07_2026", 1));
        assertEquals("DYC_P101_29_07_2026_2", RequestCodeBuilder.withSequence("DYC_P101_29_07_2026", 2));
    }

    @Test
    void findsNextAvailableCode() {
        Set<String> existingCodes = Set.of(
                "GHHD_P101_29_07_2026",
                "GHHD_P101_29_07_2026_2"
        );

        assertEquals(
                "GHHD_P101_29_07_2026_3",
                RequestCodeBuilder.nextAvailable(
                        RequestType.CONTRACT_RENEWAL,
                        "101",
                        LocalDate.of(2026, 7, 29),
                        existingCodes::contains
                )
        );
    }
}
