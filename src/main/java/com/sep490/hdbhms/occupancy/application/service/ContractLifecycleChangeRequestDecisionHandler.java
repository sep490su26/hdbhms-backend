package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.shared.exception.AppException;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep490.hdbhms.changerequest.application.port.out.ChangeRequestDecisionHandler;
import com.sep490.hdbhms.changerequest.domain.model.ChangeRequest;
import com.sep490.hdbhms.changerequest.domain.value_objects.RequestType;
import com.sep490.hdbhms.changerequest.domain.value_objects.TargetType;
import com.sep490.hdbhms.identityandaccess.application.service.TenantAccountProvisioningService;
import com.sep490.hdbhms.occupancy.application.port.in.command.AddCoOccupantToContractCommand;
import com.sep490.hdbhms.occupancy.application.port.in.command.StartLeaseLiquidationProcessingCommand;
import com.sep490.hdbhms.occupancy.application.port.in.command.UpdateLeaseContractTermsCommand;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.AddCoOccupantToContractUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.StartLeaseLiquidationProcessingUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.UpdateLeaseContractTermsUseCase;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ContractLifecycleChangeRequestDecisionHandler implements ChangeRequestDecisionHandler {
    StartLeaseLiquidationProcessingUseCase startLeaseLiquidationProcessingUseCase;
    AddCoOccupantToContractUseCase addCoOccupantToContractUseCase;
    UpdateLeaseContractTermsUseCase updateLeaseContractTermsUseCase;
    TenantAccountProvisioningService tenantAccountProvisioningService;
    ObjectMapper objectMapper;

    @Override
    public boolean supports(RequestType requestType) {
        return requestType == RequestType.CONTRACT_LIQUIDATION
                || requestType == RequestType.CONTRACT_RENEWAL
                || requestType == RequestType.ADD_CO_OCCUPANT
                || requestType == RequestType.RENT_PRICE_ADJUSTMENT;
    }

    @Override
    public void onApproved(ChangeRequest request, Long managerId) {
        Map<String, Object> payload = payload(request);
        if (request.getRequestType() == RequestType.CONTRACT_LIQUIDATION) {
            startLeaseLiquidationProcessingUseCase.execute(new StartLeaseLiquidationProcessingCommand(
                    request.getTargetId(),
                    localDate(payload.get("liquidationDate")),
                    string(payload.get("reason"))
            ));
            return;
        }
        if (request.getRequestType() == RequestType.ADD_CO_OCCUPANT) {
            Long tenantProfileId = longValue(payload.get("tenantProfileId"));
            addCoOccupantToContractUseCase.execute(new AddCoOccupantToContractCommand(
                    request.getTargetId(),
                    tenantProfileId,
                    localDate(payload.get("moveInDate")),
                    managerId
            ));
            tenantAccountProvisioningService.provisionTenantAccount(request.getTargetId(), tenantProfileId, false);
            return;
        }
        if (request.getRequestType() == RequestType.RENT_PRICE_ADJUSTMENT
                && request.getTargetType() != TargetType.CONTRACT) {
            return;
        }
        boolean renewal = request.getRequestType() == RequestType.CONTRACT_RENEWAL;
        updateLeaseContractTermsUseCase.execute(new UpdateLeaseContractTermsCommand(
                request.getTargetId(),
                localDate(firstValue(payload, "newStartDate", "startDate")),
                localDate(firstValue(payload, "newEndDate", "endDate")),
                intValue(payload.get("paymentCycleMonths")),
                longValue(payload.get("monthlyRent")),
                longValue(payload.get("depositAmount")),
                true,
                renewal
        ));
    }

    @Override
    public void onRejected(ChangeRequest request, Long managerId, String resolutionNote) {
        // ChangeRequest status already stores the rejection decision.
    }

    private Map<String, Object> payload(ChangeRequest request) {
        if (request.getRequestPayload() == null || request.getRequestPayload().isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(request.getRequestPayload(), new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
    }

    private LocalDate localDate(Object value) {
        return value == null ? null : LocalDate.parse(value.toString());
    }

    private Object firstValue(Map<String, Object> payload, String... keys) {
        for (String key : keys) {
            if (payload.containsKey(key) && payload.get(key) != null) {
                return payload.get(key);
            }
        }
        return null;
    }

    private String string(Object value) {
        return value == null ? null : value.toString();
    }

    private Long longValue(Object value) {
        if (value instanceof Number number) return number.longValue();
        return value == null ? null : Long.parseLong(value.toString());
    }

    private Integer intValue(Object value) {
        if (value instanceof Number number) return number.intValue();
        return value == null ? null : Integer.parseInt(value.toString());
    }
}
