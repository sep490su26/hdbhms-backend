package com.sep490.hdbhms.changerequest.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep490.hdbhms.changerequest.application.port.in.command.ApproveRequestCommand;
import com.sep490.hdbhms.changerequest.application.port.out.ChangeRequestDecisionHandler;
import com.sep490.hdbhms.changerequest.application.port.out.ChangeRequestRepository;
import com.sep490.hdbhms.changerequest.domain.model.ChangeRequest;
import com.sep490.hdbhms.changerequest.domain.value_objects.RequestStatus;
import com.sep490.hdbhms.changerequest.domain.value_objects.RequestType;
import com.sep490.hdbhms.changerequest.domain.value_objects.TargetType;
import com.sep490.hdbhms.notification.application.service.BusinessNotificationPublisher;
import com.sep490.hdbhms.permissiongrant.application.service.PermissionGrantService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChangeRequestServiceTest {

    @Test
    void approvedPermissionAccessCreatesGrant() {
        ChangeRequestRepository repository = mock(ChangeRequestRepository.class);
        BusinessNotificationPublisher notificationPublisher = mock(BusinessNotificationPublisher.class);
        PermissionGrantService permissionGrantService = mock(PermissionGrantService.class);
        ChangeRequest request = ChangeRequest.builder()
                .id(10L)
                .requestType(RequestType.PERMISSION_ACCESS)
                .requesterId(20L)
                .targetType(TargetType.FILE)
                .targetId(30L)
                .build();
        when(repository.findById(10L)).thenReturn(Optional.of(request));

        ChangeRequestService service = new ChangeRequestService(
                repository,
                List.of(),
                notificationPublisher,
                permissionGrantService,
                new ObjectMapper()
        );

        service.approveRequest(new ApproveRequestCommand(10L, 40L, "DAYS_7"));

        verify(permissionGrantService).grantAccess(request, 40L, "DAYS_7");
        verify(repository).save(request);
    }

    @Test
    void approvedLiquidationRequestStartsProcessingInsteadOfCompletingApproval() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        ChangeRequestRepository repository = mock(ChangeRequestRepository.class);
        BusinessNotificationPublisher notificationPublisher = mock(BusinessNotificationPublisher.class);
        PermissionGrantService permissionGrantService = mock(PermissionGrantService.class);
        ChangeRequestDecisionHandler handler = mock(ChangeRequestDecisionHandler.class);
        ChangeRequest request = ChangeRequest.builder()
                .id(11L)
                .requestType(RequestType.CONTRACT_LIQUIDATION)
                .requestPayload("{\"contractId\":18,\"liquidationDate\":\"2026-07-22\"}")
                .targetType(TargetType.CONTRACT)
                .targetId(18L)
                .build();
        when(repository.findById(11L)).thenReturn(Optional.of(request));
        when(handler.supports(RequestType.CONTRACT_LIQUIDATION)).thenReturn(true);

        ChangeRequestService service = new ChangeRequestService(
                repository,
                List.of(handler),
                notificationPublisher,
                permissionGrantService,
                objectMapper
        );

        service.approveRequest(new ApproveRequestCommand(11L, 40L, null));

        assertEquals(RequestStatus.PROCESSING, request.getStatus());
        Map<String, Object> payload = objectMapper.readValue(request.getRequestPayload(), Map.class);
        assertEquals("WAITING_HANDOVER", payload.get("liquidationStage"));
        assertTrue(payload.containsKey("liquidationChecklist"));
        verify(repository).save(request);
        verify(handler).onApproved(request, 40L);
    }

    @Test
    void tenantCanConfirmRecordedLiquidationDepositRefund() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        ChangeRequestRepository repository = mock(ChangeRequestRepository.class);
        BusinessNotificationPublisher notificationPublisher = mock(BusinessNotificationPublisher.class);
        PermissionGrantService permissionGrantService = mock(PermissionGrantService.class);
        ChangeRequest request = ChangeRequest.builder()
                .id(12L)
                .requestType(RequestType.CONTRACT_LIQUIDATION)
                .requesterId(20L)
                .requestPayload("""
                        {
                          "liquidationStage":"WAITING_DEPOSIT_REFUND",
                          "depositRefundStatus":"RECORDED_BY_MANAGER",
                          "liquidationChecklist":{"depositRefundConfirmed":false}
                        }
                        """)
                .targetType(TargetType.CONTRACT)
                .targetId(18L)
                .build();
        when(repository.findById(12L)).thenReturn(Optional.of(request));
        when(repository.save(request)).thenReturn(request);

        ChangeRequestService service = new ChangeRequestService(
                repository,
                List.of(),
                notificationPublisher,
                permissionGrantService,
                objectMapper
        );

        service.confirmLiquidationDepositReceipt(12L, 20L);

        Map<String, Object> payload = objectMapper.readValue(request.getRequestPayload(), Map.class);
        assertEquals("TENANT_CONFIRMED", payload.get("depositRefundStatus"));
        assertEquals("WAITING_SIGNED_DOCUMENT", payload.get("liquidationStage"));
        verify(repository).save(request);
    }
}
