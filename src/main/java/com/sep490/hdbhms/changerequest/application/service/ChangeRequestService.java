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
import com.sep490.hdbhms.notification.application.service.BusinessNotificationPublisher;
import com.sep490.hdbhms.permissiongrant.application.service.PermissionGrantService;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChangeRequestService implements ChangeRequestUseCase {
    ChangeRequestRepository repository;
    List<ChangeRequestDecisionHandler> decisionHandlers;
    BusinessNotificationPublisher notificationPublisher;
    PermissionGrantService permissionGrantService;
    ObjectMapper objectMapper;

    @Override
    @Transactional
    public void approveRequest(ApproveRequestCommand command) {
        ChangeRequest request = repository.findById(command.requestId())
                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));
        if (request.getRequestType() == RequestType.CONTRACT_LIQUIDATION) {
            request.startProcessing(command.managerId());
            request.updateRequestPayload(withLiquidationStage(request.getRequestPayload(), "WAITING_HANDOVER"));
        } else {
            request.approve(command.managerId());
        }
        if (request.getRequestType() == RequestType.TENANT_PROFILE_ACCESS
                || request.getRequestType() == RequestType.PERMISSION_ACCESS) {
            permissionGrantService.grantAccess(request, command.managerId(), command.durationCode());
        }
        repository.save(request);
        dispatchApproved(request, command.managerId());
        notifyTenantProfileAccessResolved(request, true);
    }

    @Override
    @Transactional
    public void rejectRequest(RejectRequestCommand command) {
        ChangeRequest request = repository.findById(command.requestId())
                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));
        request.reject(command.managerId(), command.resolutionNote());
        repository.save(request);
        dispatchRejected(request, command.managerId(), command.resolutionNote());
        notifyTenantProfileAccessResolved(request, false);
    }

    @Override
    @Transactional
    public ChangeRequest confirmLiquidationDepositReceipt(Long requestId, Long tenantId) {
        ChangeRequest request = liquidationRequestForTenant(requestId, tenantId);
        Map<String, Object> payload = payloadMap(request.getRequestPayload());
        assertRefundRecordedByManager(payload);
        payload.put("depositRefundStatus", "TENANT_CONFIRMED");
        payload.put("depositRefundConfirmedBy", tenantId);
        payload.put("depositRefundConfirmedAt", LocalDateTime.now().toString());
        if ("WAITING_DEPOSIT_REFUND".equals(payload.get("liquidationStage"))) {
            payload.put("liquidationStage", "WAITING_SIGNED_DOCUMENT");
        }
        markChecklist(payload, "depositRefundConfirmed", true);
        request.updateRequestPayload(writePayload(payload));
        return repository.save(request);
    }

    @Override
    @Transactional
    public ChangeRequest disputeLiquidationDepositRefund(Long requestId, Long tenantId, String reason) {
        String finalReason = reason == null ? "" : reason.trim();
        if (finalReason.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng nhập lý do chưa nhận hoặc sai số tiền cọc.");
        }
        ChangeRequest request = liquidationRequestForTenant(requestId, tenantId);
        Map<String, Object> payload = payloadMap(request.getRequestPayload());
        assertRefundRecordedByManager(payload);
        payload.put("depositRefundStatus", "DISPUTED");
        payload.put("depositRefundDisputedBy", tenantId);
        payload.put("depositRefundDisputedAt", LocalDateTime.now().toString());
        payload.put("depositRefundDisputeReason", finalReason);
        markChecklist(payload, "depositRefundConfirmed", false);
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

    private void notifyTenantProfileAccessResolved(ChangeRequest request, boolean approved) {
        if (request.getRequestType() != RequestType.TENANT_PROFILE_ACCESS || request.getRequesterId() == null) {
            return;
        }
        Map<String, Object> data = notificationData(request);
        notificationPublisher.publish(
                approved ? "TENANT_PROFILE_ACCESS_APPROVED" : "TENANT_PROFILE_ACCESS_REJECTED",
                request.getRequesterId(),
                "TENANT_PROFILE",
                request.getTargetId(),
                data
        );
    }

    private Map<String, Object> notificationData(ChangeRequest request) {
        Map<String, Object> data = new LinkedHashMap<>();
        if (request.getRequestPayload() != null && !request.getRequestPayload().isBlank()) {
            try {
                data.putAll(objectMapper.readValue(
                        request.getRequestPayload(),
                        new TypeReference<Map<String, Object>>() {
                        }
                ));
            } catch (Exception ignored) {
            }
        }
        data.put("requestId", request.getId());
        data.put("changeRequestId", request.getId());
        data.put("requestCode", request.getRequestCode());
        data.put("requestType", request.getRequestType() == null ? null : request.getRequestType().name());
        data.put("profileId", request.getTargetId());
        data.put("tenantProfileId", request.getTargetId());
        data.put("managerId", request.getRequesterId());
        data.put("resolutionNote", request.getResolutionNote());
        data.put("status", request.getStatus() == null ? null : request.getStatus().name());
        data.put("targetRoute", "/dashboard/tenant-profiles?profileId=" + request.getTargetId());
        data.putIfAbsent("tenantName", firstNonNull(data.get("fullName"), data.get("tenantName")));
        data.putIfAbsent("roomName", firstNonNull(data.get("roomCode"), data.get("roomName")));
        return data;
    }

    private String withLiquidationStage(String payloadJson, String stage) {
        Map<String, Object> data = payloadMap(payloadJson);
        data.put("liquidationStage", stage);
        data.put("depositRefundStatus", "PENDING");
        data.put("liquidationChecklist", Map.of(
                "handoverConfirmed", false,
                "finalInvoicePaid", false,
                "depositRefundConfirmed", false,
                "signedDocumentUploaded", false,
                "canConfirm", false
        ));
        return writePayload(data);
    }

    private ChangeRequest liquidationRequestForTenant(Long requestId, Long tenantId) {
        ChangeRequest request = repository.findById(requestId)
                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));
        if (request.getRequestType() != RequestType.CONTRACT_LIQUIDATION) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Yêu cầu không phải thanh lý hợp đồng.");
        }
        if (tenantId == null || !tenantId.equals(request.getRequesterId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền xác nhận hoàn cọc cho yêu cầu này.");
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
            throw new IllegalStateException("Invalid change request payload.", e);
        }
        return data;
    }

    private void assertRefundRecordedByManager(Map<String, Object> payload) {
        if (!"RECORDED_BY_MANAGER".equals(payload.get("depositRefundStatus"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quản lý chưa ghi nhận hoàn cọc để khách thuê xác nhận.");
        }
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

    private String writePayload(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot update change request payload.", e);
        }
    }

    private Object firstNonNull(Object first, Object second) {
        return first != null ? first : second;
    }
}
