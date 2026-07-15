package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.identityandaccess.domain.value_objects.AccountStatus;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.PromotionRole;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.RolePromotionStatus;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaRolePromotionRepository;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaUserRepository;
import com.sep490.hdbhms.notification.application.service.BusinessNotificationPublisher;
import com.sep490.hdbhms.occupancy.application.port.in.command.CreateVisitRequestCommand;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.CreateVisitRequestUseCase;
import com.sep490.hdbhms.occupancy.application.port.out.PropertyRepository;
import com.sep490.hdbhms.occupancy.application.port.out.RoomRepository;
import com.sep490.hdbhms.occupancy.application.port.out.VisitRequestRepository;
import com.sep490.hdbhms.occupancy.domain.model.Property;
import com.sep490.hdbhms.occupancy.domain.model.Room;
import com.sep490.hdbhms.occupancy.domain.model.VisitRequest;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CreateVisitRequestService implements CreateVisitRequestUseCase {
    static final String VISIT_REQUEST_CREATED_EVENT = "VISIT_REQUEST_CREATED";
    static final String VISIT_REQUEST_TARGET = "VISIT_REQUEST";

    VisitRequestRepository visitRequestRepository;
    RoomRepository roomRepository;
    PropertyRepository propertyRepository;
    JpaUserRepository userRepository;
    JpaRolePromotionRepository rolePromotionRepository;
    BusinessNotificationPublisher notificationPublisher;

    @Override
    public VisitRequest execute(CreateVisitRequestCommand command) {
        Room room = command.roomId() == null
                ? null
                : roomRepository.findById(command.roomId())
                .orElseThrow(() -> new AppException(ApiErrorCode.ROOM_NOT_FOUND));
        Long propertyId = room != null && room.getPropertyId() != null
                ? room.getPropertyId()
                : command.propertyId();
        if (propertyId == null) {
            throw new AppException(ApiErrorCode.VISIT_006);
        }

        VisitRequest visitRequest = VisitRequest.create(
                propertyId,
                command.roomId(),
                command.visitorName(),
                command.visitorPhone(),
                command.visitorEmail(),
                command.preferredStart(),
                command.notes()
        );
        VisitRequest saved = visitRequestRepository.save(visitRequest);
        publishVisitRequestCreated(saved, room);
        return saved;
    }

    private void publishVisitRequestCreated(VisitRequest visitRequest, Room room) {
        if (visitRequest == null || visitRequest.getId() == null || visitRequest.getPropertyId() == null) {
            return;
        }

        Map<String, Object> data = visitRequestData(visitRequest, room);
        for (Long recipientUserId : recipientUserIds(visitRequest.getPropertyId())) {
            notificationPublisher.publish(
                    VISIT_REQUEST_CREATED_EVENT,
                    recipientUserId,
                    VISIT_REQUEST_TARGET,
                    visitRequest.getId(),
                    data
            );
        }
    }

    private List<Long> recipientUserIds(Long propertyId) {
        LinkedHashSet<Long> userIds = new LinkedHashSet<>();
        userIds.addAll(userRepository.findIdsByRolesAndStatus(List.of(Role.OWNER), AccountStatus.ACTIVE));
        userIds.addAll(rolePromotionRepository.findActiveUserIdsByPropertyId(
                propertyId,
                PromotionRole.MANAGER,
                RolePromotionStatus.ACTIVE,
                AccountStatus.ACTIVE
        ));
        userIds.remove(null);
        return List.copyOf(userIds);
    }

    private Map<String, Object> visitRequestData(VisitRequest visitRequest, Room room) {
        Property property = propertyRepository.findById(visitRequest.getPropertyId()).orElse(null);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("visitRequestId", visitRequest.getId());
        data.put("visitorName", visitRequest.getVisitorName());
        data.put("visitorPhone", visitRequest.getVisitorPhone());
        data.put("visitorEmail", visitRequest.getVisitorEmail());
        data.put("propertyId", visitRequest.getPropertyId());
        data.put("propertyName", property == null ? "" : property.getName());
        data.put("roomId", visitRequest.getRoomId());
        data.put("roomName", roomName(room));
        data.put("preferredStart", visitRequest.getPreferredStart() == null ? "" : visitRequest.getPreferredStart().toString());
        data.put("notes", visitRequest.getNotes());
        data.put("targetRoute", "/dashboard/viewing-customers");
        return data;
    }

    private String roomName(Room room) {
        if (room == null) {
            return "Phòng chưa chọn";
        }
        if (room.getRoomCode() != null && !room.getRoomCode().isBlank()) {
            return "Phòng " + room.getRoomCode();
        }
        return room.getName() == null ? "" : room.getName();
    }
}
