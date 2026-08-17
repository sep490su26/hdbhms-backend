package com.sep490.hdbhms.changerequest.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep490.hdbhms.changerequest.application.port.in.command.ApproveRequestCommand;
import com.sep490.hdbhms.changerequest.application.port.in.command.RejectRequestCommand;
import com.sep490.hdbhms.changerequest.application.port.in.usecase.ChangeRequestUseCase;
import com.sep490.hdbhms.changerequest.application.port.out.ChangeRequestDecisionHandler;
import com.sep490.hdbhms.changerequest.application.port.out.ChangeRequestRepository;
import com.sep490.hdbhms.changerequest.domain.model.ChangeRequest;
import com.sep490.hdbhms.changerequest.domain.value_objects.RequestType;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.TenantAccountProvisioningStatus;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaTenantAccountProvisioningRepository;
import com.sep490.hdbhms.permissiongrant.application.service.PermissionGrantService;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChangeRequestService implements ChangeRequestUseCase {
    static final String LIQUIDATION_MODE_PRIMARY_LEAVES_CO_OCCUPANT_STAYS = "PRIMARY_LEAVES_CO_OCCUPANT_STAYS";

    ChangeRequestRepository repository;
    List<ChangeRequestDecisionHandler> decisionHandlers;
    PermissionGrantService permissionGrantService;
    ObjectMapper objectMapper;
    JpaTenantAccountProvisioningRepository provisioningRepository;

    @Override
    @Transactional
    public void approveRequest(ApproveRequestCommand command) {
        ChangeRequest request = repository.findById(command.requestId())
                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));
        if (request.getRequestType() == RequestType.TENANT_PROFILE_ACCESS) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        if (request.getRequestType() == RequestType.CONTRACT_LIQUIDATION) {
            request.startProcessing(command.managerId());
            request.updateRequestPayload(withLiquidationStage(request.getRequestPayload()));
        } else {
            request.approve(command.managerId());
        }
        if (request.getRequestType() == RequestType.PERMISSION_ACCESS) {
            permissionGrantService.grantAccess(request, command.managerId(), command.durationCode());
        }
        repository.save(request);
        dispatchApproved(request, command.managerId());
    }

    @Override
    @Transactional
    public void rejectRequest(RejectRequestCommand command) {
        ChangeRequest request = repository.findById(command.requestId())
                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));
        if (request.getRequestType() == RequestType.TENANT_PROFILE_ACCESS) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        request.reject(command.managerId(), command.resolutionNote());
        repository.save(request);
        dispatchRejected(request, command.managerId(), command.resolutionNote());
    }

    @Override
    @Transactional
    public ChangeRequest confirmLiquidationDepositReceipt(Long requestId, Long tenantId) {
        ChangeRequest request = liquidationRequestForTenant(requestId, tenantId);
        Map<String, Object> payload = payloadMap(request.getRequestPayload());
        if ("TENANT_CONFIRMED".equals(payload.get("depositRefundStatus"))) {
            return request;
        }
        assertRefundAwaitingTenantConfirmation(payload);
        LocalDateTime confirmedAt = LocalDateTime.now();
        payload.put("depositRefundStatus", "TENANT_CONFIRMED");
        payload.put("depositRefundConfirmedBy", tenantId);
        payload.put("depositRefundConfirmedAt", confirmedAt.toString());
        payload.put("depositRefundedAmount", payload.get("depositRefundAmount"));
        payload.put("depositRefundedAt", confirmedAt.toString());
        markChecklist(payload, "depositRefundConfirmed", true);
        payload.put("liquidationStage", liquidationStageAfterTenantSettlement(payload));
        request.updateRequestPayload(writePayload(payload));
        return repository.save(request);
    }

    @Override
    @Transactional
    public ChangeRequest disputeLiquidationDepositRefund(Long requestId, Long tenantId, String reason) {
        String finalReason = reason == null ? "" : reason.trim();
        if (finalReason.isBlank()) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        ChangeRequest request = liquidationRequestForTenant(requestId, tenantId);
        Map<String, Object> payload = payloadMap(request.getRequestPayload());
        assertRefundAwaitingTenantConfirmation(payload);
        payload.put("depositRefundStatus", "DISPUTED");
        payload.put("depositRefundDisputedBy", tenantId);
        payload.put("depositRefundDisputedAt", LocalDateTime.now().toString());
        payload.put("depositRefundDisputeReason", finalReason);
        markChecklist(payload, "depositRefundConfirmed", false);
        request.updateRequestPayload(writePayload(payload));
        return repository.save(request);
    }

    @Override
    @Transactional
    public ChangeRequest confirmLiquidationDepositForfeiture(Long requestId, Long tenantId) {
        ChangeRequest request = liquidationRequestForTenant(requestId, tenantId);
        Map<String, Object> payload = payloadMap(request.getRequestPayload());
        if ("TENANT_CONFIRMED".equals(payload.get("depositForfeitureStatus"))) {
            return request;
        }
        assertForfeiturePending(payload);
        payload.put("depositForfeitureStatus", "TENANT_CONFIRMED");
        payload.put("depositForfeitureConfirmedBy", tenantId);
        payload.put("depositForfeitureConfirmedAt", LocalDateTime.now().toString());
        markChecklist(payload, "depositForfeitureConfirmed", true);
        payload.put("liquidationStage", liquidationStageAfterTenantSettlement(payload));
        request.updateRequestPayload(writePayload(payload));
        return repository.save(request);
    }

    @Override
    @Transactional
    public ChangeRequest disputeLiquidationDepositForfeiture(Long requestId, Long tenantId, String reason) {
        String finalReason = reason == null ? "" : reason.trim();
        if (finalReason.isBlank()) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        ChangeRequest request = liquidationRequestForTenant(requestId, tenantId);
        Map<String, Object> payload = payloadMap(request.getRequestPayload());
        assertForfeiturePending(payload);
        payload.put("depositForfeitureStatus", "DISPUTED");
        payload.put("depositForfeitureDisputedBy", tenantId);
        payload.put("depositForfeitureDisputedAt", LocalDateTime.now().toString());
        payload.put("depositForfeitureDisputeReason", finalReason);
        markChecklist(payload, "depositForfeitureConfirmed", false);
        payload.put("liquidationStage", "WAITING_DEPOSIT_FORFEITURE_CONFIRMATION");
        request.updateRequestPayload(writePayload(payload));
        return repository.save(request);
    }

    private void dispatchApproved(ChangeRequest request, Long managerId) {
        decisionHandlers.stream()
                .filter(handler -> handler.supports(request.getRequestType()))
                .forEach(handler -> handler.onApproved(request, managerId));
    }

    private void dispatchRejected(ChangeRequest request, Long managerId, String resolutionNote) {
        decisionHandlers.stream()
                .filter(handler -> handler.supports(request.getRequestType()))
                .forEach(handler -> handler.onRejected(request, managerId, resolutionNote));
    }

    private String withLiquidationStage(String payloadJson) {
        Map<String, Object> data = payloadMap(payloadJson);
        Object liquidationMode = data.get("liquidationMode");
        boolean holderReplacement = liquidationMode != null
                && LIQUIDATION_MODE_PRIMARY_LEAVES_CO_OCCUPANT_STAYS
                .equalsIgnoreCase(liquidationMode.toString().trim());
        String stage = holderReplacement ? "WAITING_REPLACEMENT_CONTRACT" : "WAITING_HANDOVER";
        data.put("liquidationStage", stage);
        data.put("depositRefundStatus", holderReplacement ? "NOT_REQUIRED" : "PENDING");
        data.put("depositForfeitureStatus", "NOT_REQUIRED");
        Map<String, Object> checklist = new LinkedHashMap<>();
        checklist.put("handoverConfirmed", holderReplacement);
        checklist.put("finalInvoicePaid", false);
        checklist.put("depositRefundConfirmed", holderReplacement);
        checklist.put("depositForfeitureConfirmed", holderReplacement);
        if (holderReplacement) {
            checklist.put("replacementContractSigned", false);
        }
        checklist.put("canConfirm", false);
        data.put("liquidationChecklist", checklist);
        return writePayload(data);
    }

    private ChangeRequest liquidationRequestForTenant(Long requestId, Long tenantId) {
        ChangeRequest request = repository.findById(requestId)
                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));
        if (request.getRequestType() != RequestType.CONTRACT_LIQUIDATION) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        Map<String, Object> payload = payloadMap(request.getRequestPayload());
        Long contractTenantUserId = toLong(payload.get("primaryTenantUserId"));
        Long expectedTenantUserId = contractTenantUserId == null
                ? request.getRequesterId()
                : contractTenantUserId;
        Long primaryTenantProfileId = toLong(payload.get("primaryTenantProfileId"));
        boolean provisionedTenant = tenantId != null
                && primaryTenantProfileId != null
                && provisioningRepository.findByTenantProfileId(primaryTenantProfileId)
                .filter(provisioning -> provisioning.getStatus() != TenantAccountProvisioningStatus.DISABLED)
                .map(provisioning -> tenantId.equals(provisioning.getUserId()))
                .orElse(false);
        if (tenantId == null || (!tenantId.equals(expectedTenantUserId) && !provisionedTenant)) {
            throw new AppException(ApiErrorCode.FORBIDDEN_OPERATION);
        }
        return request;
    }

    private Map<String, Object> payloadMap(String payloadJson) {
        Map<String, Object> data = new LinkedHashMap<>();
        if (payloadJson == null || payloadJson.isBlank()) {
            return data;
        }
        try {
            data.putAll(objectMapper.readValue(
                    payloadJson,
                    new TypeReference<Map<String, Object>>() {
                    }
            ));
        } catch (Exception e) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST, e);
        }
        return data;
    }

    private void assertRefundAwaitingTenantConfirmation(Map<String, Object> payload) {
        Object status = payload.get("depositRefundStatus");
        if (!"APPROVED_WAITING_TENANT_CONFIRMATION".equals(status)
                && !"RECORDED_BY_MANAGER".equals(status)) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
    }

    private void assertForfeiturePending(Map<String, Object> payload) {
        Object status = payload.get("depositForfeitureStatus");
        if (!"PENDING_TENANT_CONFIRMATION".equals(status)
                && !"DISPUTED".equals(status)) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
    }

    private String liquidationStageAfterTenantSettlement(Map<String, Object> payload) {
        if (!isLiquidationHandoverConfirmed(payload)) {
            return "WAITING_HANDOVER";
        }
        Object forfeitureStatus = payload.get("depositForfeitureStatus");
        if (forfeitureStatus != null
                && !"NOT_REQUIRED".equals(forfeitureStatus)
                && !"TENANT_CONFIRMED".equals(forfeitureStatus)
                && !"AUTOMATICALLY_FORFEITED".equals(forfeitureStatus)) {
            return "WAITING_DEPOSIT_FORFEITURE_CONFIRMATION";
        }
        if (!isFinalInvoicePaid(payload)) {
            return "WAITING_PAYMENT";
        }
        Object refundStatus = payload.get("depositRefundStatus");
        if (refundStatus != null
                && !"TENANT_CONFIRMED".equals(refundStatus)
                && !"NOT_REQUIRED".equals(refundStatus)) {
            return "WAITING_DEPOSIT_REFUND";
        }
        return "READY_TO_COMPLETE";
    }

    @SuppressWarnings("unchecked")
    private boolean isLiquidationHandoverConfirmed(Map<String, Object> payload) {
        if (Boolean.TRUE.equals(payload.get("handoverConfirmed"))) {
            return true;
        }
        Object rawChecklist = payload.get("liquidationChecklist");
        if (rawChecklist instanceof Map<?, ?> raw) {
            Object value = ((Map<String, Object>) raw).get("handoverConfirmed");
            return Boolean.TRUE.equals(value);
        }
        return false;
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Long.parseLong(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private void markChecklist(Map<String, Object> payload, String key, boolean value) {
        Object rawChecklist = payload.get("liquidationChecklist");
        Map<String, Object> checklist = rawChecklist instanceof Map<?, ?> raw
                ? new LinkedHashMap<>((Map<String, Object>) raw)
                : new LinkedHashMap<>();
        checklist.put(key, value);
        payload.put("liquidationChecklist", checklist);
    }

    @SuppressWarnings("unchecked")
    private boolean isFinalInvoicePaid(Map<String, Object> payload) {
        Object direct = payload.get("finalInvoicePaid");
        if (direct instanceof Boolean value) {
            return value;
        }
        Object rawChecklist = payload.get("liquidationChecklist");
        if (rawChecklist instanceof Map<?, ?> raw) {
            Object value = ((Map<String, Object>) raw).get("finalInvoicePaid");
            return value instanceof Boolean paid && paid;
        }
        return false;
    }

    private String writePayload(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new AppException(ApiErrorCode.UNDEFINED, e);
        }
    }

    private Object firstNonNull(Object first, Object second) {
        return first != null ? first : second;
    }
}
