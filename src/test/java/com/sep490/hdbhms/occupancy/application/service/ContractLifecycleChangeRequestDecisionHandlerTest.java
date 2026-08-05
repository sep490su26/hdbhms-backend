package com.sep490.hdbhms.occupancy.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep490.hdbhms.changerequest.domain.model.ChangeRequest;
import com.sep490.hdbhms.changerequest.domain.value_objects.RequestType;
import com.sep490.hdbhms.changerequest.domain.value_objects.TargetType;
import com.sep490.hdbhms.identityandaccess.application.service.TenantAccountProvisioningService;
import com.sep490.hdbhms.occupancy.application.port.in.command.AddCoOccupantToContractCommand;
import com.sep490.hdbhms.occupancy.application.port.in.command.UpdateLeaseContractTermsCommand;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.AddCoOccupantToContractUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.StartLeaseLiquidationProcessingUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.UpdateLeaseContractTermsUseCase;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;

import static org.mockito.Mockito.mock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ContractLifecycleChangeRequestDecisionHandlerTest {

    @Test
    void approvedRenewalUpdatesCurrentContractTermsInsteadOfCreatingNewContract() {
        StartLeaseLiquidationProcessingUseCase liquidationUseCase = mock(StartLeaseLiquidationProcessingUseCase.class);
        AddCoOccupantToContractUseCase addCoOccupantUseCase = mock(AddCoOccupantToContractUseCase.class);
        UpdateLeaseContractTermsUseCase updateTermsUseCase = mock(UpdateLeaseContractTermsUseCase.class);
        TenantAccountProvisioningService provisioningService = mock(TenantAccountProvisioningService.class);
        ContractLifecycleChangeRequestDecisionHandler handler = new ContractLifecycleChangeRequestDecisionHandler(
                liquidationUseCase,
                addCoOccupantUseCase,
                updateTermsUseCase,
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

        ArgumentCaptor<UpdateLeaseContractTermsCommand> captor =
                ArgumentCaptor.forClass(UpdateLeaseContractTermsCommand.class);
        verify(updateTermsUseCase).execute(captor.capture());
        assertEquals(18L, captor.getValue().leaseContractId());
        assertEquals(LocalDate.parse("2024-01-01"), captor.getValue().startDate());
        assertEquals(1, captor.getValue().paymentCycleMonths());
        assertEquals(2200000L, captor.getValue().monthlyRent());
        assertEquals(2200000L, captor.getValue().depositAmount());
    }

    @Test
    void approvedAddCoOccupantProvisionsAddedTenantAccount() {
        StartLeaseLiquidationProcessingUseCase liquidationUseCase = mock(StartLeaseLiquidationProcessingUseCase.class);
        AddCoOccupantToContractUseCase addCoOccupantUseCase = mock(AddCoOccupantToContractUseCase.class);
        UpdateLeaseContractTermsUseCase updateTermsUseCase = mock(UpdateLeaseContractTermsUseCase.class);
        TenantAccountProvisioningService provisioningService = mock(TenantAccountProvisioningService.class);
        ContractLifecycleChangeRequestDecisionHandler handler = new ContractLifecycleChangeRequestDecisionHandler(
                liquidationUseCase,
                addCoOccupantUseCase,
                updateTermsUseCase,
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

        ArgumentCaptor<AddCoOccupantToContractCommand> captor =
                ArgumentCaptor.forClass(AddCoOccupantToContractCommand.class);
        verify(addCoOccupantUseCase).execute(captor.capture());
        assertEquals(18L, captor.getValue().leaseContractId());
        assertEquals(77L, captor.getValue().tenantProfileId());
        assertEquals(LocalDate.parse("2026-07-27"), captor.getValue().moveInDate());
        assertEquals(40L, captor.getValue().approvedBy());
        verify(provisioningService).provisionTenantAccount(18L, 77L, false);
    }
}
