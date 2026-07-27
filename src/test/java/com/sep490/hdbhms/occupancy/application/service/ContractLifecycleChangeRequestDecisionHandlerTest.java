package com.sep490.hdbhms.occupancy.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep490.hdbhms.changerequest.domain.model.ChangeRequest;
import com.sep490.hdbhms.changerequest.domain.value_objects.RequestType;
import com.sep490.hdbhms.changerequest.domain.value_objects.TargetType;
import com.sep490.hdbhms.identityandaccess.application.service.TenantAccountProvisioningService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ContractLifecycleChangeRequestDecisionHandlerTest {

    @Test
    void approvedRenewalUpdatesCurrentContractTermsInsteadOfCreatingNewContract() {
        LeaseContractManagementService managementService = mock(LeaseContractManagementService.class);
        TenantAccountProvisioningService provisioningService = mock(TenantAccountProvisioningService.class);
        ContractLifecycleChangeRequestDecisionHandler handler = new ContractLifecycleChangeRequestDecisionHandler(
                managementService,
                provisioningService,
                new ObjectMapper()
        );
        ChangeRequest request = ChangeRequest.builder()
                .requestType(RequestType.CONTRACT_RENEWAL)
                .targetType(TargetType.CONTRACT)
                .targetId(18L)
                .requestPayload("""
                        {
                          "startDate":"2024-01-01",
                          "newEndDate":"2027-07-31",
                          "monthlyRent":2200000,
                          "paymentCycleMonths":1,
                          "depositAmount":2200000
                        }
                        """)
                .build();

        handler.onApproved(request, 40L);

        verify(managementService).updateTerms(
                18L,
                LocalDate.parse("2024-01-01"),
                LocalDate.parse("2027-07-31"),
                1,
                2200000L,
                2200000L
        );
        verify(managementService, never()).renew(
                anyLong(),
                any(LocalDate.class),
                any(LocalDate.class),
                anyLong(),
                anyInt(),
                anyLong(),
                any(),
                any()
        );
    }

    @Test
    void approvedAddCoOccupantProvisionsAddedTenantAccount() {
        LeaseContractManagementService managementService = mock(LeaseContractManagementService.class);
        TenantAccountProvisioningService provisioningService = mock(TenantAccountProvisioningService.class);
        ContractLifecycleChangeRequestDecisionHandler handler = new ContractLifecycleChangeRequestDecisionHandler(
                managementService,
                provisioningService,
                new ObjectMapper()
        );
        ChangeRequest request = ChangeRequest.builder()
                .requestType(RequestType.ADD_CO_OCCUPANT)
                .targetType(TargetType.CONTRACT)
                .targetId(18L)
                .requestPayload("""
                        {
                          "tenantProfileId":77,
                          "moveInDate":"2026-07-27"
                        }
                        """)
                .build();

        handler.onApproved(request, 40L);

        verify(managementService).addCoOccupantFromChangeRequest(
                18L,
                77L,
                LocalDate.parse("2026-07-27"),
                40L
        );
        verify(provisioningService).provisionTenantAccount(18L, 77L, false);
    }
}
