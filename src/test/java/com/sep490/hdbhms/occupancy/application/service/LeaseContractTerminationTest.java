package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.file.application.service.UploadFileService;
import com.sep490.hdbhms.file.infrastructure.persistence.jpa.JpaFileMetadataRepository;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import com.sep490.hdbhms.identityandaccess.infrastructure.config.security.UserPrincipal;
import com.sep490.hdbhms.occupancy.domain.value_objects.LeaseStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.RoomStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeaseContractEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RoomEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaContractLiquidationRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaDepositAgreementRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaLeaseContractRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomRepository;
import com.sep490.hdbhms.occupancy.infrastructure.web.controller.TenantLeaseContractController;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.LeaseContractManagementResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LeaseContractTerminationTest {
    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final JpaRoomRepository roomRepository = mock(JpaRoomRepository.class);
    private final JpaLeaseContractRepository contractRepository = mock(JpaLeaseContractRepository.class);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void tenantMoveOutRecordsExpectedDateAndReasonWithoutTerminatingContract() {
        setUser(1L, Role.OWNER);
        LeaseContractEntity contract = activeContract();
        LeaseContractManagementService service = serviceReturning(contract);

        service.recordTenantIntention(
                91L,
                "MOVE_OUT",
                LocalDate.now().plusDays(10),
                "Relocating for work"
        );

        assertEquals("MOVE_OUT", contract.getTenantIntention());
        assertEquals(LocalDate.now().plusDays(10), contract.getExpectedVacantDate());
        assertEquals(LeaseStatus.ACTIVE, contract.getStatus());
        assertEquals(RoomStatus.SOON_VACANT, contract.getRoom().getCurrentStatus());
        verify(contractRepository).saveAndFlush(contract);
        verify(roomRepository).saveAndFlush(contract.getRoom());
    }

    @Test
    void moveOutRejectsContractWithoutAnActiveLeaseStatus() {
        LeaseContractEntity contract = activeContract();
        contract.setStatus(LeaseStatus.EXPIRED);
        LeaseContractManagementService service = serviceReturning(contract);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.recordTenantIntention(
                        91L,
                        "MOVE_OUT",
                        LocalDate.now().plusDays(10),
                        "Relocating"
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(contractRepository, never()).saveAndFlush(any());
    }

    @Test
    void duplicateMoveOutRequestReturnsConflict() {
        LeaseContractEntity contract = activeContract();
        contract.setTenantIntention("MOVE_OUT");
        contract.setExpectedVacantDate(LocalDate.now().plusDays(10));
        LeaseContractManagementService service = serviceReturning(contract);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.recordTenantIntention(
                        91L,
                        "MOVE_OUT",
                        LocalDate.now().plusDays(11),
                        "Duplicate"
                )
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertTrue(exception.getReason().contains("MOVE_OUT_INTENTION_ALREADY_RECORDED"));
    }

    @Test
    void invalidMoveOutDateAndBlankReasonAreRejected() {
        LeaseContractManagementService dateService = serviceReturning(activeContract());
        ResponseStatusException invalidDate = assertThrows(
                ResponseStatusException.class,
                () -> dateService.recordTenantIntention(
                        91L,
                        "MOVE_OUT",
                        LocalDate.now().minusDays(1),
                        "Relocating"
                )
        );
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, invalidDate.getStatusCode());

        LeaseContractManagementService reasonService = serviceReturning(activeContract());
        ResponseStatusException blankReason = assertThrows(
                ResponseStatusException.class,
                () -> reasonService.recordTenantIntention(
                        91L,
                        "MOVE_OUT",
                        LocalDate.now().plusDays(10),
                        "  "
                )
        );
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, blankReason.getStatusCode());
        assertTrue(blankReason.getReason().contains("MOVE_OUT_REASON_REQUIRED"));
    }

    @Test
    void anonymousAndUnrelatedTenantCannotSubmit() {
        LeaseContractManagementService service = serviceReturning(activeContract());

        ResponseStatusException anonymous = assertThrows(
                ResponseStatusException.class,
                () -> service.recordTenantIntentionForCurrentTenant(
                        91L,
                        "MOVE_OUT",
                        LocalDate.now().plusDays(10),
                        "Relocating"
                )
        );
        assertEquals(HttpStatus.UNAUTHORIZED, anonymous.getStatusCode());

        setUser(7L, Role.TENANT);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(Object[].class)))
                .thenReturn(1, 0);
        ResponseStatusException unrelated = assertThrows(
                ResponseStatusException.class,
                () -> service.recordTenantIntentionForCurrentTenant(
                        91L,
                        "MOVE_OUT",
                        LocalDate.now().plusDays(10),
                        "Relocating"
                )
        );
        assertEquals(HttpStatus.FORBIDDEN, unrelated.getStatusCode());
    }

    @Test
    void endpointIsTenantOnlySoOwnerAndManagerAreRejectedByMethodSecurity() throws NoSuchMethodException {
        var method = TenantLeaseContractController.class.getMethod(
                "recordTenantIntention",
                Long.class,
                TenantLeaseContractController.TenantIntentionRequest.class
        );
        PreAuthorize authorization = method.getAnnotation(PreAuthorize.class);

        assertEquals("hasRole('TENANT')", authorization.value());
    }

    private LeaseContractManagementService serviceReturning(LeaseContractEntity contract) {
        when(jdbcTemplate.query(anyString(), any(org.springframework.jdbc.core.RowMapper.class), eq(91L)))
                .thenReturn(List.of(91L));
        when(contractRepository.findById(91L)).thenReturn(Optional.of(contract));
        var service = spy(new LeaseContractManagementService(
                jdbcTemplate,
                mock(UploadFileService.class),
                roomRepository,
                mock(JpaFileMetadataRepository.class),
                contractRepository,
                mock(JpaDepositAgreementRepository.class),
                mock(JpaContractLiquidationRepository.class),
                mock(RoomCommitmentChecker.class)
        ));
        doReturn(LeaseContractManagementResponse.builder().leaseContractId(91L).build())
                .when(service).findOne(91L);
        return service;
    }

    private static LeaseContractEntity activeContract() {
        return LeaseContractEntity.builder()
                .id(91L)
                .contractCode("LC-91")
                .status(LeaseStatus.ACTIVE)
                .endDate(LocalDate.now().plusDays(30))
                .room(RoomEntity.builder().id(11L).currentStatus(RoomStatus.OCCUPIED).build())
                .build();
    }

    private static void setUser(Long userId, Role role) {
        var authority = new SimpleGrantedAuthority("ROLE_" + role.name());
        var principal = UserPrincipal.builder()
                .id(userId)
                .role(role)
                .authorities(Set.of(authority))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }
}
