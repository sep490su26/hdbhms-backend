package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.shared.exception.AppException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LeaseContractManagementServiceHolderReplacementTest {

    @Test
    void validatesPrimaryLeavesAndCoOccupantStaysProfilePlan() {
        LeaseContractManagementService.HolderReplacementProfilePlan plan =
                LeaseContractManagementService.validateHolderReplacementProfileIds(
                        1L,
                        Set.of(1L, 2L),
                        List.of(1L),
                        List.of(2L),
                        2L
                );

        assertEquals(Set.of(1L), plan.leavingProfileIds());
        assertEquals(Set.of(2L), plan.stayingProfileIds());
        assertEquals(2L, plan.replacementPrimaryTenantProfileId());
    }

    @Test
    void rejectsOverlappingLeavingAndStayingOccupants() {
        AppException exception = assertThrows(
                AppException.class,
                () -> LeaseContractManagementService.validateHolderReplacementProfileIds(
                        1L,
                        Set.of(1L, 2L),
                        List.of(1L, 2L),
                        List.of(2L),
                        2L
                )
        );

        assertEquals("HOLDER_REPLACEMENT_OCCUPANTS_OVERLAP", exception.getApiErrorCode().name());
    }
}
