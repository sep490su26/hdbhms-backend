package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.identityandaccess.application.port.out.PersonProfileRepository;
import com.sep490.hdbhms.identityandaccess.domain.model.PersonProfile;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import com.sep490.hdbhms.identityandaccess.infrastructure.config.security.UserPrincipal;
import com.sep490.hdbhms.occupancy.application.port.out.LeaseContractRepository;
import com.sep490.hdbhms.occupancy.application.port.out.TenantRepository;
import com.sep490.hdbhms.occupancy.domain.model.RoomTransferRequest;
import com.sep490.hdbhms.occupancy.domain.model.Tenant;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RoomTransferAccessPolicyTest {
    private final TenantRepository tenantRepository = mock(TenantRepository.class);
    private final PersonProfileRepository personProfileRepository = mock(PersonProfileRepository.class);
    private final LeaseContractRepository leaseContractRepository = mock(LeaseContractRepository.class);
    private final RoomTransferAccessPolicy policy = new RoomTransferAccessPolicy(
            tenantRepository,
            personProfileRepository,
            leaseContractRepository
    );

    @Test
    void ownerAndManagerCanReadOperationalTransferRequests() {
        RoomTransferRequest request = request(10L, List.of(20L));

        assertDoesNotThrow(() -> policy.assertCanRead(principal(1L, Role.OWNER), request));
        assertDoesNotThrow(() -> policy.assertCanRead(principal(2L, Role.MANAGER), request));
    }

    @Test
    void requesterAndTransferringTenantCanReadTheirTransferRequest() {
        RoomTransferRequest request = request(10L, List.of(20L));
        when(tenantRepository.findByUserId(100L)).thenReturn(Optional.of(Tenant.builder().id(10L).build()));
        when(tenantRepository.findByUserId(200L)).thenReturn(Optional.of(Tenant.builder().id(11L).build()));
        when(personProfileRepository.findByUserId(200L))
                .thenReturn(Optional.of(PersonProfile.builder().id(20L).build()));

        assertDoesNotThrow(() -> policy.assertCanRead(principal(100L, Role.TENANT), request));
        assertDoesNotThrow(() -> policy.assertCanRead(principal(200L, Role.TENANT), request));
    }

    @Test
    void unrelatedTenantAndAccountantCannotReadTransferDetails() {
        RoomTransferRequest request = request(10L, List.of(20L));
        when(tenantRepository.findByUserId(300L)).thenReturn(Optional.of(Tenant.builder().id(12L).build()));
        when(personProfileRepository.findByUserId(300L))
                .thenReturn(Optional.of(PersonProfile.builder().id(30L).build()));

        assertForbidden(() -> policy.assertCanRead(principal(300L, Role.TENANT), request));
        assertForbidden(() -> policy.assertCanRead(principal(400L, Role.ACCOUNTANT), request));
    }

    @Test
    void onlyTheRequestingTenantCanCancel() {
        RoomTransferRequest request = request(10L, List.of(20L));
        when(tenantRepository.findByUserId(100L)).thenReturn(Optional.of(Tenant.builder().id(10L).build()));
        when(tenantRepository.findByUserId(200L)).thenReturn(Optional.of(Tenant.builder().id(11L).build()));

        assertDoesNotThrow(() -> policy.assertCanCancel(principal(100L, Role.TENANT), request));
        assertForbidden(() -> policy.assertCanCancel(principal(200L, Role.TENANT), request));
        assertForbidden(() -> policy.assertCanCancel(principal(1L, Role.OWNER), request));
    }

    private static RoomTransferRequest request(Long requesterId, List<Long> transferringProfiles) {
        return RoomTransferRequest.builder()
                .id(1L)
                .requesterId(requesterId)
                .transferringTenantProfileIds(transferringProfiles)
                .build();
    }

    private static UserPrincipal principal(Long id, Role role) {
        return UserPrincipal.builder().id(id).role(role).build();
    }

    private static void assertForbidden(Runnable action) {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, action::run);
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }
}
