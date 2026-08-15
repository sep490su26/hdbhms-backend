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
import com.sep490.hdbhms.property.domain.model.Room;
import com.sep490.hdbhms.occupancy.application.port.out.RoomTransferRepository;
import com.sep490.hdbhms.occupancy.application.port.out.RoomTransferRequestRepository;
import com.sep490.hdbhms.occupancy.application.port.out.TenantRepository;
import com.sep490.hdbhms.occupancy.application.port.out.TransferSettlementRepository;
import com.sep490.hdbhms.occupancy.domain.model.LeaseContract;
import com.sep490.hdbhms.occupancy.domain.model.RoomTransferRequest;
import com.sep490.hdbhms.occupancy.domain.value_objects.LeaseStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.HandoverStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.HandoverType;
import com.sep490.hdbhms.occupancy.domain.value_objects.TransferRequestStatus;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.ContractHandoverDetailsResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.SubmitHandoverResponse;
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
import java.lang.reflect.Method;

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

    @Test
    void signingDocumentIsIdempotentAfterRequestIsReadyForHandover() {
        setOwner();
        LeaseContractRepository leaseContractRepository = mock(LeaseContractRepository.class);
        RoomTransferRepository roomTransferRepository = mock(RoomTransferRepository.class);
        RoomTransferRequest request = transferRequest();
        request.setStatus(TransferRequestStatus.READY_FOR_HANDOVER);

        when(roomTransferRepository.findById(10L)).thenReturn(Optional.of(request));

        newService(leaseContractRepository, roomTransferRepository)
                .signTransferContractDocument(10L, 101L, 1L);

        verify(leaseContractRepository, never()).findById(any());
        verify(roomTransferRepository, never()).save(any());
    }

    @Test
    void signingDocumentAcceptsAlreadyActivatedTransferContracts() {
        setOwner();
        LeaseContractRepository leaseContractRepository = mock(LeaseContractRepository.class);
        RoomTransferRepository roomTransferRepository = mock(RoomTransferRepository.class);
        RoomRepository roomRepository = mock(RoomRepository.class);
        RoomTransferRequest request = transferRequest();
        request.setOldRoomId(1L);
        request.setTargetRoomId(2L);
        LeaseContract selected = transferContract(101L, LeaseStatus.ACTIVE, 201L);
        LeaseContract other = transferContract(102L, LeaseStatus.ACTIVE, 202L);

        when(roomTransferRepository.findById(10L)).thenReturn(Optional.of(request));
        when(roomTransferRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(leaseContractRepository.findById(101L)).thenReturn(Optional.of(selected));
        when(leaseContractRepository.findById(102L)).thenReturn(Optional.of(other));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(Room.builder().id(1L).propertyId(1L).name("Old").build()));
        when(roomRepository.findById(2L)).thenReturn(Optional.of(Room.builder().id(2L).propertyId(1L).name("New").build()));

        newService(leaseContractRepository, roomRepository, roomTransferRepository)
                .signTransferContractDocument(10L, 101L, 1L);

        assertEquals(TransferRequestStatus.READY_FOR_HANDOVER, request.getStatus());
        verify(roomTransferRepository).save(request);
        verify(leaseContractRepository, never()).save(any());
    }

    @Test
    void transferRequestSyncPromotesActivatedContractsFromContractConfirmation() {
        setOwner();
        LeaseContractRepository leaseContractRepository = mock(LeaseContractRepository.class);
        RoomTransferRepository roomTransferRepository = mock(RoomTransferRepository.class);
        RoomRepository roomRepository = mock(RoomRepository.class);
        RoomTransferRequest request = transferRequest();
        request.setStatus(TransferRequestStatus.WAITING_CONTRACT_CONFIRMATION);
        request.setOldRoomId(1L);
        request.setTargetRoomId(2L);

        when(roomTransferRepository.findById(10L)).thenReturn(Optional.of(request));
        when(roomTransferRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(leaseContractRepository.findById(101L))
                .thenReturn(Optional.of(transferContract(101L, LeaseStatus.ACTIVE, 201L)));
        when(leaseContractRepository.findById(102L))
                .thenReturn(Optional.of(transferContract(102L, LeaseStatus.ACTIVE, 202L)));
        when(roomRepository.findById(1L))
                .thenReturn(Optional.of(Room.builder().id(1L).propertyId(1L).name("Old").build()));
        when(roomRepository.findById(2L))
                .thenReturn(Optional.of(Room.builder().id(2L).propertyId(1L).name("New").build()));

        RoomTransferRequest result = newService(leaseContractRepository, roomRepository, roomTransferRepository)
                .getTransferRequestById(10L);

        assertEquals(TransferRequestStatus.READY_FOR_HANDOVER, result.getStatus());
        verify(roomTransferRepository).save(request);
    }

    @Test
    void transferCompletionUsesMoveInFallbackWithoutCallingThrowingHandoverLookup() throws Exception {
        ManageContractHandoverService handoverService = mock(ManageContractHandoverService.class);
        ContractHandoverDetailsResponse moveIn = ContractHandoverDetailsResponse.builder()
                .handoverRecordId(77L)
                .handoverType(HandoverType.MOVE_IN)
                .status(HandoverStatus.CONFIRMED)
                .signedDocumentId(88L)
                .build();
        when(handoverService.findHandoverDetails(46L, HandoverType.TRANSFER_IN))
                .thenReturn(Optional.empty());
        when(handoverService.findHandoverDetails(46L, HandoverType.MOVE_IN))
                .thenReturn(Optional.of(moveIn));

        RoomTransferService service = newService(
                mock(LeaseContractRepository.class),
                mock(RoomRepository.class),
                mock(RoomTransferRepository.class),
                handoverService
        );
        Method lookup = RoomTransferService.class.getDeclaredMethod(
                "requireConfirmedHandover", Long.class, HandoverType.class, String.class
        );
        lookup.setAccessible(true);

        SubmitHandoverResponse result = (SubmitHandoverResponse) lookup.invoke(
                service, 46L, HandoverType.TRANSFER_IN, "missing"
        );

        assertEquals(HandoverType.MOVE_IN, result.getHandoverType());
        assertEquals(HandoverStatus.CONFIRMED, result.getStatus());
        verify(handoverService, never()).getHandoverDetails(any(), any());
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
        return newService(leaseContractRepository, mock(RoomRepository.class), roomTransferRepository);
    }

    private static RoomTransferService newService(
            LeaseContractRepository leaseContractRepository,
            RoomRepository roomRepository,
            RoomTransferRepository roomTransferRepository
    ) {
        return newService(
                leaseContractRepository,
                roomRepository,
                roomTransferRepository,
                mock(ManageContractHandoverService.class)
        );
    }

    private static RoomTransferService newService(
            LeaseContractRepository leaseContractRepository,
            RoomRepository roomRepository,
            RoomTransferRepository roomTransferRepository,
            ManageContractHandoverService handoverService
    ) {
        return new RoomTransferService(
                roomRepository,
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
                handoverService,
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
