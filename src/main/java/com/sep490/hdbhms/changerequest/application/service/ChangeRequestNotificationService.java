package com.sep490.hdbhms.changerequest.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep490.hdbhms.changerequest.domain.model.ChangeRequest;
import com.sep490.hdbhms.changerequest.domain.value_objects.AssignedRole;
import com.sep490.hdbhms.changerequest.domain.value_objects.RequestStatus;
import com.sep490.hdbhms.changerequest.domain.value_objects.RequestType;
import com.sep490.hdbhms.changerequest.domain.value_objects.TargetType;
import com.sep490.hdbhms.identityandaccess.application.port.out.UserRepository;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.AccountStatus;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import com.sep490.hdbhms.notification.application.service.BusinessNotificationPublisher;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChangeRequestNotificationService {
    private static final String CHANGE_REQUEST_CREATED = "CHANGE_REQUEST_CREATED";
    private static final String CHANGE_REQUEST_TARGET = "CHANGE_REQUEST";

    BusinessNotificationPublisher notificationPublisher;
    UserRepository userRepository;
    JdbcTemplate jdbcTemplate;
    ObjectMapper objectMapper;

    public void notifyCreated(ChangeRequest request) {
        if (request == null || request.getId() == null || request.getStatus() != RequestStatus.PENDING) {
            return;
        }

        Map<String, Object> data = notificationData(request);
        for (Long recipientId : recipientIds(request, data)) {
            notificationPublisher.publish(
                    CHANGE_REQUEST_CREATED,
                    recipientId,
                    CHANGE_REQUEST_TARGET,
                    request.getId(),
                    data
            );
        }
    }

    private Set<Long> recipientIds(ChangeRequest request, Map<String, Object> data) {
        Set<Long> recipients = new LinkedHashSet<>();
        if (request.getAssignedTo() != null) {
            recipients.add(request.getAssignedTo());
        } else if (shouldNotifyOwnerAndPropertyManagers(request)) {
            recipients.addAll(userRepository.findIdsByRolesAndStatus(List.of(Role.OWNER), AccountStatus.ACTIVE));
            recipients.addAll(managerRecipientIds(request, data));
        } else if (request.getAssignedRole() == AssignedRole.MANAGER) {
            recipients.addAll(managerRecipientIds(request, data));
        } else if (request.getAssignedRole() == AssignedRole.ACCOUNTANT) {
            recipients.addAll(userRepository.findIdsByRolesAndStatus(List.of(Role.ACCOUNTANT), AccountStatus.ACTIVE));
        } else {
            recipients.addAll(userRepository.findIdsByRolesAndStatus(List.of(Role.OWNER), AccountStatus.ACTIVE));
        }
        recipients.remove(request.getRequesterId());
        return recipients;
    }

    private boolean shouldNotifyOwnerAndPropertyManagers(ChangeRequest request) {
        return request.getRequestType() == RequestType.CONTRACT_RENEWAL
                || request.getRequestType() == RequestType.ADD_CO_OCCUPANT;
    }

    private List<Long> managerRecipientIds(ChangeRequest request, Map<String, Object> data) {
        Long propertyId = resolvePropertyId(request, data);
        if (propertyId != null) {
            List<Long> managerIds = jdbcTemplate.queryForList("""
                            SELECT psa.staff_user_id
                            FROM property_staff_assignments psa
                            JOIN users u ON u.user_id = psa.staff_user_id
                            WHERE psa.property_id = ?
                              AND psa.assignment_status = 'ACTIVE'
                              AND psa.assigned_role = 'MANAGER'
                              AND u.status = 'ACTIVE'
                              AND u.deleted_at IS NULL
                            ORDER BY psa.is_primary DESC, psa.property_staff_assignment_id ASC
                            """,
                    Long.class,
                    propertyId
            );
            if (!managerIds.isEmpty()) {
                return managerIds;
            }
        }
        return userRepository.findIdsByRolesAndStatus(List.of(Role.OWNER), AccountStatus.ACTIVE);
    }

    private Long resolvePropertyId(ChangeRequest request, Map<String, Object> data) {
        Long propertyId = toLong(data.get("propertyId"));
        if (propertyId != null) {
            return propertyId;
        }

        Long roomId = toLong(data.get("roomId"));
        if (roomId != null) {
            propertyId = querySingleLong("SELECT property_id FROM rooms WHERE room_id = ?", roomId);
            if (propertyId != null) {
                return propertyId;
            }
        }

        Long contractId = request.getTargetType() == TargetType.CONTRACT
                ? request.getTargetId()
                : toLong(data.get("contractId"));
        if (contractId != null) {
            propertyId = querySingleLong("""
                            SELECT r.property_id
                            FROM lease_contracts lc
                            JOIN rooms r ON r.room_id = lc.room_id
                            WHERE lc.lease_contract_id = ?
                            """,
                    contractId
            );
            if (propertyId != null) {
                return propertyId;
            }
        }

        if (request.getTargetType() == TargetType.METER_READING && request.getTargetId() != null) {
            return querySingleLong("""
                            SELECT r.property_id
                            FROM meter_readings mr
                            JOIN rooms r ON r.room_id = mr.room_id
                            WHERE mr.meter_reading_id = ?
                            """,
                    request.getTargetId()
            );
        }
        return null;
    }

    private Long querySingleLong(String sql, Long id) {
        List<Long> values = jdbcTemplate.queryForList(sql, Long.class, id);
        return values.isEmpty() ? null : values.get(0);
    }

    private Map<String, Object> notificationData(ChangeRequest request) {
        Map<String, Object> data = new LinkedHashMap<>(parsePayload(request.getRequestPayload()));
        data.put("requestId", request.getId());
        data.put("requestCode", blankToFallback(request.getRequestCode(), "CR-" + request.getId()));
        data.put("requestType", request.getRequestType() == null ? "" : request.getRequestType().name());
        data.put("requestTypeLabel", requestTypeLabel(request));
        data.put("title", blankToFallback(request.getTitle(), requestTypeLabel(request)));
        data.put("description", blankToFallback(request.getDescription(), ""));
        data.put("requesterId", request.getRequesterId());
        data.put("requesterRole", request.getRequesterRole() == null ? "" : request.getRequesterRole().name());
        data.put("assignedRole", request.getAssignedRole() == null ? "" : request.getAssignedRole().name());
        data.put("targetType", request.getTargetType() == null ? "" : request.getTargetType().name());
        data.put("targetId", request.getTargetId());
        data.put("targetRoute", "/dashboard/requests?requestId=" + request.getId());
        return data;
    }

    private Map<String, Object> parsePayload(String payload) {
        if (payload == null || payload.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(payload, new TypeReference<>() {
            });
        } catch (Exception exception) {
            log.warn("Could not parse change request payload for notification: {}", exception.getMessage());
            return Map.of();
        }
    }

    private String requestTypeLabel(ChangeRequest request) {
        if (request.getRequestType() == null) {
            return "Yêu cầu";
        }
        return switch (request.getRequestType()) {
            case CONTRACT_RENEWAL -> "Yêu cầu tái ký hợp đồng";
            case CONTRACT_LIQUIDATION -> "Yêu cầu thanh lý hợp đồng";
            case ADD_CO_OCCUPANT -> "Yêu cầu thêm người ở cùng";
            case METER_READING_CORRECTION -> "Khiếu nại chỉ số điện nước";
            case PERMISSION_ACCESS -> "Yêu cầu cấp quyền";
            case INVOICE_ADJUSTMENT -> "Yêu cầu điều chỉnh hóa đơn";
            case RENT_PRICE_ADJUSTMENT -> "Yêu cầu điều chỉnh giá thuê";
            case DEPOSIT_REFUND_REQUEST -> "Yêu cầu xử lý tiền cọc";
            case MOVE_OUT -> "Yêu cầu chuyển đi";
            case COMPLAINT -> "Khiếu nại";
            default -> "Yêu cầu " + request.getRequestType().name();
        };
    }

    private String blankToFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
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
}
