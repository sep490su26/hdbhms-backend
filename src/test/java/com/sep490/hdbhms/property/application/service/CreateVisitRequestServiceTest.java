package com.sep490.hdbhms.property.application.service;

import com.sep490.hdbhms.identityandaccess.domain.value_objects.AccountStatus;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.PromotionRole;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.RolePromotionStatus;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaRolePromotionRepository;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaUserRepository;
import com.sep490.hdbhms.notification.application.service.BusinessNotificationPublisher;
import com.sep490.hdbhms.property.application.port.in.command.CreateVisitRequestCommand;
import com.sep490.hdbhms.property.application.port.out.PropertyRepository;
import com.sep490.hdbhms.property.application.port.out.RoomRepository;
import com.sep490.hdbhms.property.application.port.out.VisitRequestRepository;
import com.sep490.hdbhms.property.domain.model.Property;
import com.sep490.hdbhms.property.domain.model.Room;
import com.sep490.hdbhms.property.domain.model.VisitRequest;
import com.sep490.hdbhms.property.domain.value_objects.RoomStatus;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreateVisitRequestServiceTest {

    @Test
    void rejectsRoomThatIsNotAvailableForViewing() {
        VisitRequestRepository visitRequestRepository = mock(VisitRequestRepository.class);
        RoomRepository roomRepository = mock(RoomRepository.class);
        PropertyRepository propertyRepository = mock(PropertyRepository.class);
        JpaUserRepository userRepository = mock(JpaUserRepository.class);
        JpaRolePromotionRepository rolePromotionRepository = mock(JpaRolePromotionRepository.class);
        BusinessNotificationPublisher notificationPublisher = mock(BusinessNotificationPublisher.class);
        RoomCommitmentChecker roomCommitmentChecker = mock(RoomCommitmentChecker.class);
        CreateVisitRequestService service = new CreateVisitRequestService(
                visitRequestRepository,
                roomRepository,
                propertyRepository,
                userRepository,
                rolePromotionRepository,
                notificationPublisher,
                roomCommitmentChecker
        );

        when(roomRepository.findById(20L)).thenReturn(Optional.of(
                Room.builder()
                        .id(20L)
                        .propertyId(2L)
                        .currentStatus(RoomStatus.OCCUPIED)
                        .build()
        ));

        AppException exception = assertThrows(AppException.class, () -> service.execute(
                new CreateVisitRequestCommand(
                        2L,
                        20L,
                        "Nguyen Van A",
                        "0912345678",
                        "",
                        LocalDateTime.now().plusDays(1),
                        "Muon xem phong"
                )
        ));

        assertEquals(ApiErrorCode.VISIT_008, exception.getApiErrorCode());
    }

    @Test
    void rejectsSoonVacantViewingBeforeMoveOutDate() {
        VisitRequestRepository visitRequestRepository = mock(VisitRequestRepository.class);
        RoomRepository roomRepository = mock(RoomRepository.class);
        PropertyRepository propertyRepository = mock(PropertyRepository.class);
        JpaUserRepository userRepository = mock(JpaUserRepository.class);
        JpaRolePromotionRepository rolePromotionRepository = mock(JpaRolePromotionRepository.class);
        BusinessNotificationPublisher notificationPublisher = mock(BusinessNotificationPublisher.class);
        RoomCommitmentChecker roomCommitmentChecker = mock(RoomCommitmentChecker.class);
        CreateVisitRequestService service = new CreateVisitRequestService(
                visitRequestRepository,
                roomRepository,
                propertyRepository,
                userRepository,
                rolePromotionRepository,
                notificationPublisher,
                roomCommitmentChecker
        );
        LocalDate moveOutDate = LocalDate.now().plusDays(5);

        when(roomRepository.findById(20L)).thenReturn(Optional.of(
                Room.builder()
                        .id(20L)
                        .propertyId(2L)
                        .currentStatus(RoomStatus.SOON_VACANT)
                        .build()
        ));
        when(roomCommitmentChecker.findExpectedVacantDateForBooking(20L)).thenReturn(Optional.of(moveOutDate));

        AppException exception = assertThrows(AppException.class, () -> service.execute(
                new CreateVisitRequestCommand(
                        2L,
                        20L,
                        "Nguyen Van A",
                        "0912345678",
                        "",
                        moveOutDate.atStartOfDay().plusHours(12),
                        "Muon xem phong"
                )
        ));

        assertEquals(ApiErrorCode.VISIT_009, exception.getApiErrorCode());
    }

    @Test
    void usesRoomPropertyWhenRoomIsSelected() {
        VisitRequestRepository visitRequestRepository = mock(VisitRequestRepository.class);
        RoomRepository roomRepository = mock(RoomRepository.class);
        PropertyRepository propertyRepository = mock(PropertyRepository.class);
        JpaUserRepository userRepository = mock(JpaUserRepository.class);
        JpaRolePromotionRepository rolePromotionRepository = mock(JpaRolePromotionRepository.class);
        BusinessNotificationPublisher notificationPublisher = mock(BusinessNotificationPublisher.class);
        RoomCommitmentChecker roomCommitmentChecker = mock(RoomCommitmentChecker.class);
        CreateVisitRequestService service = new CreateVisitRequestService(
                visitRequestRepository,
                roomRepository,
                propertyRepository,
                userRepository,
                rolePromotionRepository,
                notificationPublisher,
                roomCommitmentChecker
        );

        when(roomRepository.findById(20L)).thenReturn(Optional.of(
                Room.builder()
                        .id(20L)
                        .propertyId(2L)
                        .currentStatus(RoomStatus.VACANT)
                        .build()
        ));
        when(visitRequestRepository.save(any(VisitRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.execute(new CreateVisitRequestCommand(
                1L,
                20L,
                "Nguyen Van A",
                "0912345678",
                "",
                LocalDateTime.now().plusDays(1),
                "Muon xem phong"
        ));

        ArgumentCaptor<VisitRequest> captor = ArgumentCaptor.forClass(VisitRequest.class);
        verify(visitRequestRepository).save(captor.capture());
        assertEquals(2L, captor.getValue().getPropertyId());
        assertEquals(20L, captor.getValue().getRoomId());
    }

    @Test
    void publishesNotificationToOwnerAndAssignedManager() {
        VisitRequestRepository visitRequestRepository = mock(VisitRequestRepository.class);
        RoomRepository roomRepository = mock(RoomRepository.class);
        PropertyRepository propertyRepository = mock(PropertyRepository.class);
        JpaUserRepository userRepository = mock(JpaUserRepository.class);
        JpaRolePromotionRepository rolePromotionRepository = mock(JpaRolePromotionRepository.class);
        BusinessNotificationPublisher notificationPublisher = mock(BusinessNotificationPublisher.class);
        RoomCommitmentChecker roomCommitmentChecker = mock(RoomCommitmentChecker.class);
        CreateVisitRequestService service = new CreateVisitRequestService(
                visitRequestRepository,
                roomRepository,
                propertyRepository,
                userRepository,
                rolePromotionRepository,
                notificationPublisher,
                roomCommitmentChecker
        );
        LocalDateTime preferredStart = LocalDateTime.now().plusDays(1);

        when(roomRepository.findById(20L)).thenReturn(Optional.of(
                Room.builder()
                        .id(20L)
                        .propertyId(2L)
                        .roomCode("101")
                        .currentStatus(RoomStatus.SOON_VACANT)
                        .build()
        ));
        when(propertyRepository.findById(2L)).thenReturn(Optional.of(
                Property.builder()
                        .id(2L)
                        .name("Cơ sở A")
                        .build()
        ));
        when(userRepository.findIdsByRolesAndStatus(List.of(Role.OWNER), AccountStatus.ACTIVE))
                .thenReturn(List.of(1L));
        when(rolePromotionRepository.findActiveUserIdsByPropertyId(
                2L,
                PromotionRole.MANAGER,
                RolePromotionStatus.ACTIVE,
                AccountStatus.ACTIVE
        )).thenReturn(List.of(9L));
        when(visitRequestRepository.save(any(VisitRequest.class)))
                .thenAnswer(invocation -> {
                    VisitRequest request = invocation.getArgument(0);
                    return VisitRequest.builder()
                            .id(77L)
                            .propertyId(request.getPropertyId())
                            .roomId(request.getRoomId())
                            .visitorName(request.getVisitorName())
                            .visitorPhone(request.getVisitorPhone())
                            .visitorEmail(request.getVisitorEmail())
                            .preferredStart(request.getPreferredStart())
                            .status(request.getStatus())
                            .notes(request.getNotes())
                            .createdAt(request.getCreatedAt())
                            .updatedAt(request.getUpdatedAt())
                            .deletedAt(request.getDeletedAt())
                            .deletedByUserId(request.getDeletedByUserId())
                            .build();
                });

        service.execute(new CreateVisitRequestCommand(
                2L,
                20L,
                "Nguyen Van A",
                "0912345678",
                "a@example.com",
                preferredStart,
                "Muon xem phong"
        ));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(notificationPublisher).publish(
                eq("VISIT_REQUEST_CREATED"),
                eq(1L),
                eq("VISIT_REQUEST"),
                eq(77L),
                payloadCaptor.capture()
        );
        verify(notificationPublisher).publish(
                eq("VISIT_REQUEST_CREATED"),
                eq(9L),
                eq("VISIT_REQUEST"),
                eq(77L),
                any()
        );

        Map<String, Object> payload = payloadCaptor.getValue();
        assertEquals(77L, payload.get("visitRequestId"));
        assertEquals("Nguyen Van A", payload.get("visitorName"));
        assertEquals("Phòng 101", payload.get("roomName"));
        assertEquals("Cơ sở A", payload.get("propertyName"));
        assertTrue(String.valueOf(payload.get("targetRoute")).contains("/dashboard/viewing-customers"));
    }
}
