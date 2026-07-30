package com.sep490.hdbhms.occupancy.application.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> LeaseContractManagementService.validateHolderReplacementProfileIds(
                        1L,
                        Set.of(1L, 2L),
                        List.of(1L, 2L),
                        List.of(2L),
                        2L
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }
}
