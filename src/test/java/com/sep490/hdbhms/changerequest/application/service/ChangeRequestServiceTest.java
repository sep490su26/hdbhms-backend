package com.sep490.hdbhms.changerequest.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep490.hdbhms.changerequest.application.port.in.command.ApproveRequestCommand;
import com.sep490.hdbhms.changerequest.application.port.out.ChangeRequestDecisionHandler;
import com.sep490.hdbhms.changerequest.application.port.out.ChangeRequestRepository;
import com.sep490.hdbhms.changerequest.domain.model.ChangeRequest;
import com.sep490.hdbhms.changerequest.domain.value_objects.RequestStatus;
import com.sep490.hdbhms.changerequest.domain.value_objects.RequestType;
import com.sep490.hdbhms.changerequest.domain.value_objects.TargetType;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.TenantAccountProvisioningStatus;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.TenantAccountProvisioningEntity;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaTenantAccountProvisioningRepository;
import com.sep490.hdbhms.permissiongrant.application.service.PermissionGrantService;
import com.sep490.hdbhms.shared.exception.AppException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChangeRequestServiceTest {

    @Test
    void legacyTenantProfileAccessRequestCannotBeApproved() {
        ChangeRequestRepository repository = mock(ChangeRequestRepository.class);
        PermissionGrantService permissionGrantService = mock(PermissionGrantService.class);
        ChangeRequest request = ChangeRequest.builder()
                .id(9L)
                .requestType(RequestType.TENANT_PROFILE_ACCESS)
                .requesterId(20L)
                .targetType(TargetType.TENANT_PROFILE)
                .targetId(30L)
                .build();
        when(repository.findById(9L)).thenReturn(Optional.of(request));

        ChangeRequestService service = new ChangeRequestService(
                repository,
                List.of(),
                permissionGrantService,
                new ObjectMapper(),
                mock(JpaTenantAccountProvisioningRepository.class)
        );

        assertThrows(AppException.class, () -> service.approveRequest(
                new ApproveRequestCommand(9L, 40L, null)
        ));
    }

    @Test
    void approvedPermissionAccessCreatesGrant() {
        ChangeRequestRepository repository = mock(ChangeRequestRepository.class);
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
                permissionGrantService,
                new ObjectMapper(),
                mock(JpaTenantAccountProvisioningRepository.class)
        );

        service.approveRequest(new ApproveRequestCommand(10L, 40L, "DAYS_7"));

        verify(permissionGrantService).grantAccess(request, 40L, "DAYS_7");
        verify(repository).save(request);
    }

    @Test
    void approvedLiquidationRequestStartsProcessingInsteadOfCompletingApproval() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        ChangeRequestRepository repository = mock(ChangeRequestRepository.class);
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
                permissionGrantService,
                objectMapper,
                mock(JpaTenantAccountProvisioningRepository.class)
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
    void tenantCanConfirmApprovedLiquidationDepositRefundWithoutManagerPaymentStep() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        ChangeRequestRepository repository = mock(ChangeRequestRepository.class);
        PermissionGrantService permissionGrantService = mock(PermissionGrantService.class);
        ChangeRequest request = ChangeRequest.builder()
                .id(12L)
                .requestType(RequestType.CONTRACT_LIQUIDATION)
                .requesterId(40L)
                .requestPayload("""
                        {
                          "primaryTenantUserId":20,
                          "liquidationStage":"WAITING_DEPOSIT_REFUND",
                          "depositRefundStatus":"APPROVED_WAITING_TENANT_CONFIRMATION",
                          "depositRefundAmount":2000000,
                          "finalInvoicePaid":true,
                          "liquidationChecklist":{"depositRefundConfirmed":false,"finalInvoicePaid":true}
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
                permissionGrantService,
                objectMapper,
                mock(JpaTenantAccountProvisioningRepository.class)
        );

        service.confirmLiquidationDepositReceipt(12L, 20L);

        Map<String, Object> payload = objectMapper.readValue(request.getRequestPayload(), Map.class);
        assertEquals("TENANT_CONFIRMED", payload.get("depositRefundStatus"));
        assertEquals(2000000, payload.get("depositRefundedAmount"));
        assertTrue(payload.containsKey("depositRefundedAt"));
        assertEquals("WAITING_SIGNED_DOCUMENT", payload.get("liquidationStage"));
        verify(repository).save(request);
    }

    @Test
    void provisionedTenantCanConfirmLiquidationDepositRefund() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        ChangeRequestRepository repository = mock(ChangeRequestRepository.class);
        PermissionGrantService permissionGrantService = mock(PermissionGrantService.class);
        JpaTenantAccountProvisioningRepository provisioningRepository =
                mock(JpaTenantAccountProvisioningRepository.class);
        ChangeRequest request = ChangeRequest.builder()
                .id(15L)
                .requestType(RequestType.CONTRACT_LIQUIDATION)
                .requesterId(40L)
                .requestPayload("""
                        {
                          "primaryTenantProfileId":77,
                          "liquidationStage":"WAITING_DEPOSIT_REFUND",
                          "depositRefundStatus":"APPROVED_WAITING_TENANT_CONFIRMATION",
                          "depositRefundAmount":2000000,
                          "finalInvoicePaid":true
                        }
                        """)
                .targetType(TargetType.CONTRACT)
                .targetId(18L)
                .build();
        TenantAccountProvisioningEntity provisioning = TenantAccountProvisioningEntity.builder()
                .tenantProfileId(77L)
                .userId(20L)
                .status(TenantAccountProvisioningStatus.ACTIVE)
                .build();
        when(repository.findById(15L)).thenReturn(Optional.of(request));
        when(repository.save(request)).thenReturn(request);
        when(provisioningRepository.findByTenantProfileId(77L))
                .thenReturn(Optional.of(provisioning));

        ChangeRequestService service = new ChangeRequestService(
                repository,
                List.of(),
                permissionGrantService,
                objectMapper,
                provisioningRepository
        );

        service.confirmLiquidationDepositReceipt(15L, 20L);

        Map<String, Object> payload = objectMapper.readValue(request.getRequestPayload(), Map.class);
        assertEquals("TENANT_CONFIRMED", payload.get("depositRefundStatus"));
        verify(repository).save(request);
    }

    @Test
    void tenantCanConfirmLiquidationDepositForfeiture() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        ChangeRequestRepository repository = mock(ChangeRequestRepository.class);
        PermissionGrantService permissionGrantService = mock(PermissionGrantService.class);
        ChangeRequest request = ChangeRequest.builder()
                .id(13L)
                .requestType(RequestType.CONTRACT_LIQUIDATION)
                .requesterId(40L)
                .requestPayload("""
                        {
                          "primaryTenantUserId":20,
                          "liquidationStage":"WAITING_DEPOSIT_FORFEITURE_CONFIRMATION",
                          "depositForfeitureStatus":"PENDING_TENANT_CONFIRMATION",
                          "depositForfeitureAmount":500000,
                          "depositForfeitureReason":"Hu hong tai san",
                          "finalInvoicePaid":true,
                          "depositRefundStatus":"NOT_REQUIRED",
                          "liquidationChecklist":{"depositForfeitureConfirmed":false,"finalInvoicePaid":true}
                        }
                        """)
                .targetType(TargetType.CONTRACT)
                .targetId(18L)
                .build();
        when(repository.findById(13L)).thenReturn(Optional.of(request));
        when(repository.save(request)).thenReturn(request);

        ChangeRequestService service = new ChangeRequestService(
                repository,
                List.of(),
                permissionGrantService,
                objectMapper,
                mock(JpaTenantAccountProvisioningRepository.class)
        );

        service.confirmLiquidationDepositForfeiture(13L, 20L);

        Map<String, Object> payload = objectMapper.readValue(request.getRequestPayload(), Map.class);
        assertEquals("TENANT_CONFIRMED", payload.get("depositForfeitureStatus"));
        assertEquals("WAITING_SIGNED_DOCUMENT", payload.get("liquidationStage"));
        assertTrue(request.getRequestPayload().contains("\"depositForfeitureConfirmed\":true"));
        verify(repository).save(request);
    }

    @Test
    void tenantCanDisputeLiquidationDepositForfeiture() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        ChangeRequestRepository repository = mock(ChangeRequestRepository.class);
        PermissionGrantService permissionGrantService = mock(PermissionGrantService.class);
        ChangeRequest request = ChangeRequest.builder()
                .id(14L)
                .requestType(RequestType.CONTRACT_LIQUIDATION)
                .requesterId(40L)
                .requestPayload("""
                        {
                          "primaryTenantUserId":20,
                          "liquidationStage":"WAITING_DEPOSIT_FORFEITURE_CONFIRMATION",
                          "depositForfeitureStatus":"PENDING_TENANT_CONFIRMATION",
                          "depositForfeitureAmount":500000,
                          "depositForfeitureReason":"Hu hong tai san"
                        }
                        """)
                .targetType(TargetType.CONTRACT)
                .targetId(18L)
                .build();
        when(repository.findById(14L)).thenReturn(Optional.of(request));
        when(repository.save(request)).thenReturn(request);

        ChangeRequestService service = new ChangeRequestService(
                repository,
                List.of(),
                permissionGrantService,
                objectMapper,
                mock(JpaTenantAccountProvisioningRepository.class)
        );

        service.disputeLiquidationDepositForfeiture(14L, 20L, "Tai san khong hu hong");

        Map<String, Object> payload = objectMapper.readValue(request.getRequestPayload(), Map.class);
        assertEquals("DISPUTED", payload.get("depositForfeitureStatus"));
        assertEquals("Tai san khong hu hong", payload.get("depositForfeitureDisputeReason"));
        assertEquals("WAITING_DEPOSIT_FORFEITURE_CONFIRMATION", payload.get("liquidationStage"));
        verify(repository).save(request);
    }
}
