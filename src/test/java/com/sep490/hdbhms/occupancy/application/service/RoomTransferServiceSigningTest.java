package com.sep490.hdbhms.occupancy.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep490.hdbhms.billingandpayment.application.port.out.InvoiceLineRepository;
import com.sep490.hdbhms.billingandpayment.application.port.out.InvoiceRepository;
import com.sep490.hdbhms.billingandpayment.application.service.IssuedInvoiceChargeService;
import com.sep490.hdbhms.billingandpayment.application.service.UtilityBillingRunService;
import com.sep490.hdbhms.changerequest.application.port.out.ChangeRequestRepository;
import com.sep490.hdbhms.identityandaccess.application.port.out.PersonProfileRepository;
import com.sep490.hdbhms.identityandaccess.application.port.out.UserRepository;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import com.sep490.hdbhms.identityandaccess.infrastructure.config.security.UserPrincipal;
import com.sep490.hdbhms.occupancy.application.port.out.ContractOccupantRepository;
import com.sep490.hdbhms.occupancy.application.port.out.LeaseContractRepository;
import com.sep490.hdbhms.property.application.port.out.RoomRepository;
import com.sep490.hdbhms.occupancy.application.port.out.RoomTransferRepository;
import com.sep490.hdbhms.occupancy.application.port.out.RoomTransferRequestRepository;
import com.sep490.hdbhms.occupancy.application.port.out.TenantRepository;
import com.sep490.hdbhms.occupancy.application.port.out.TransferSettlementRepository;
import com.sep490.hdbhms.occupancy.domain.model.LeaseContract;
import com.sep490.hdbhms.occupancy.domain.model.RoomTransferRequest;
import com.sep490.hdbhms.occupancy.domain.value_objects.LeaseStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.TransferRequestStatus;
import com.sep490.hdbhms.shared.utils.id.SnowflakeIdGenerator;
import com.sep490.hdbhms.shared.exception.AppException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoomTransferServiceSigningTest {
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void signTransferContractDocumentSignsOnlySelectedContract() {
        setOwner();
        LeaseContractRepository leaseContractRepository = mock(LeaseContractRepository.class);
        RoomTransferRepository roomTransferRepository = mock(RoomTransferRepository.class);
        RoomTransferRequest request = transferRequest();
        LeaseContract selected = transferContract(101L, LeaseStatus.CONFIRMED, 201L);
        LeaseContract other = transferContract(102L, LeaseStatus.CONFIRMED, 202L);

        when(roomTransferRepository.findById(10L)).thenReturn(Optional.of(request));
        when(leaseContractRepository.findById(101L)).thenReturn(Optional.of(selected));
        when(leaseContractRepository.findById(102L)).thenReturn(Optional.of(other));

        newService(leaseContractRepository, roomTransferRepository)
                .signTransferContractDocument(10L, 101L, 1L);

        assertEquals(LeaseStatus.SIGNED, selected.getStatus());
        assertEquals(LeaseStatus.CONFIRMED, other.getStatus());
        verify(leaseContractRepository).save(selected);
        verify(roomTransferRepository, never()).save(any());
    }

    @Test
    void signTransferContractRequiresEveryContractAlreadySigned() {
        setOwner();
        LeaseContractRepository leaseContractRepository = mock(LeaseContractRepository.class);
        RoomTransferRepository roomTransferRepository = mock(RoomTransferRepository.class);
        RoomTransferRequest request = transferRequest();

        when(roomTransferRepository.findById(10L)).thenReturn(Optional.of(request));
        when(leaseContractRepository.findById(101L))
                .thenReturn(Optional.of(transferContract(101L, LeaseStatus.SIGNED, 201L)));
        when(leaseContractRepository.findById(102L))
                .thenReturn(Optional.of(transferContract(102L, LeaseStatus.CONFIRMED, 202L)));

        AppException exception = assertThrows(
                AppException.class,
                () -> newService(leaseContractRepository, roomTransferRepository).signTransferContract(10L, 1L)
        );

        assertEquals("TRANSFER_CONTRACTS_MUST_BE_CONFIRMED_INDIVIDUALLY", exception.getApiErrorCode().name());
        assertTrue(exception.getMessage().contains("xác nhận từng hợp đồng"));
        assertEquals(TransferRequestStatus.WAITING_SIGNING, request.getStatus());
        verify(roomTransferRepository, never()).save(any());
    }

    private static RoomTransferRequest transferRequest() {
        return RoomTransferRequest.builder()
                .id(10L)
                .status(TransferRequestStatus.WAITING_SIGNING)
                .newContractId(101L)
                .replacementOldContractId(102L)
                .build();
    }

    private static LeaseContract transferContract(Long id, LeaseStatus status, Long signedFileId) {
        return LeaseContract.builder()
                .id(id)
                .status(status)
                .signedFileId(signedFileId)
                .build();
    }

    private static RoomTransferService newService(
            LeaseContractRepository leaseContractRepository,
            RoomTransferRepository roomTransferRepository
    ) {
        return new RoomTransferService(
                mock(RoomRepository.class),
                mock(TenantRepository.class),
                mock(PersonProfileRepository.class),
                mock(UserRepository.class),
                leaseContractRepository,
                roomTransferRepository,
                mock(ChangeRequestRepository.class),
                mock(ContractOccupantRepository.class),
                mock(RoomTransferRequestRepository.class),
                mock(TransferSettlementRepository.class),
                mock(InvoiceRepository.class),
                mock(InvoiceLineRepository.class),
                mock(IssuedInvoiceChargeService.class),
                mock(UtilityBillingRunService.class),
                mock(ManageContractHandoverService.class),
                mock(RoomTransferCreateBypassRegistry.class),
                mock(SnowflakeIdGenerator.class),
                mock(ApplicationEventPublisher.class),
                mock(JdbcTemplate.class),
                mock(ObjectMapper.class)
        );
    }

    private static void setOwner() {
        var authority = new SimpleGrantedAuthority("ROLE_OWNER");
        var principal = UserPrincipal.builder()
                .id(1L)
                .role(Role.OWNER)
                .authorities(Set.of(authority))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }
}
