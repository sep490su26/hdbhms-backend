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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        request.approve(command.managerId());
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

    private Object firstNonNull(Object first, Object second) {
        return first != null ? first : second;
    }
}
