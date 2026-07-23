package com.sep490.hdbhms.occupancy.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep490.hdbhms.changerequest.application.port.out.ChangeRequestDecisionHandler;
import com.sep490.hdbhms.changerequest.domain.model.ChangeRequest;
import com.sep490.hdbhms.changerequest.domain.value_objects.RequestType;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.Map;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ContractLifecycleChangeRequestDecisionHandler implements ChangeRequestDecisionHandler {
    LeaseContractManagementService leaseContractManagementService;
    ObjectMapper objectMapper;

    @Override
    public boolean supports(RequestType requestType) {
        return requestType == RequestType.CONTRACT_LIQUIDATION
                || requestType == RequestType.CONTRACT_RENEWAL
                || requestType == RequestType.ADD_CO_OCCUPANT;
    }

    @Override
    public void onApproved(ChangeRequest request, Long managerId) {
        Map<String, Object> payload = payload(request);
        if (request.getRequestType() == RequestType.CONTRACT_LIQUIDATION) {
            leaseContractManagementService.startLiquidationProcessing(
                    request.getTargetId(),
                    localDate(payload.get("liquidationDate")),
                    string(payload.get("reason"))
            );
            return;
        }
        if (request.getRequestType() == RequestType.ADD_CO_OCCUPANT) {
            leaseContractManagementService.addCoOccupantFromChangeRequest(
                    request.getTargetId(),
                    longValue(payload.get("tenantProfileId")),
                    localDate(payload.get("moveInDate")),
                    managerId
            );
            return;
        }
        leaseContractManagementService.renew(
                request.getTargetId(),
                localDate(payload.get("newStartDate")),
                localDate(payload.get("newEndDate")),
                longValue(payload.get("monthlyRent")),
                intValue(payload.get("paymentCycleMonths")),
                longValue(payload.get("depositAmount")),
                null,
                string(payload.get("note"))
        );
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Noi dung yeu cau khong hop le.");
        }
    }

    private LocalDate localDate(Object value) {
        return value == null ? null : LocalDate.parse(value.toString());
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
