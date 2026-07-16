package com.sep490.hdbhms.occupancy.infrastructure.web.controller;

import com.sep490.hdbhms.billingandpayment.domain.value_objects.DepositAgreementStatus;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaRolePromotionRepository;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.CreatePropertyUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetListPropertiesUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetPropertyDetailsUseCase;
import com.sep490.hdbhms.occupancy.domain.value_objects.LeaseStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.PropertyStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.RoomStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.PropertyEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaFloorPlanItemRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaPropertyRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaUtilityTariffRepository;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.UpdatePropertyStatusRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.mapper.PropertyWebMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PropertyActivationRoomStatusTest {
    private final JpaPropertyRepository propertyRepository = mock(JpaPropertyRepository.class);
    private final JpaRoomRepository roomRepository = mock(JpaRoomRepository.class);
    private final JpaFloorPlanItemRepository floorPlanItemRepository = mock(JpaFloorPlanItemRepository.class);
    private final PropertyController controller = new PropertyController(
            mock(PropertyWebMapper.class),
            mock(CreatePropertyUseCase.class),
            mock(GetListPropertiesUseCase.class),
            mock(GetPropertyDetailsUseCase.class),
            propertyRepository,
            roomRepository,
            floorPlanItemRepository,
            mock(JpaUtilityTariffRepository.class),
            mock(JpaRolePromotionRepository.class)
    );

    @Test
    void activationMovesOnlyUncommittedDraftRoomsToVacant() {
        PropertyEntity property = property(PropertyStatus.DRAFT);
        givenPropertyCanBeActivated(property);

        controller.updatePropertyStatus(1L, new UpdatePropertyStatusRequest(PropertyStatus.ACTIVE));

        verify(roomRepository).updateDraftRoomsWithoutActiveCommitmentsToStatus(
                1L,
                RoomStatus.DRAFT,
                List.of(LeaseStatus.ACTIVE, LeaseStatus.EXPIRING_SOON, LeaseStatus.TERMINATION_PENDING),
                List.of(
                        DepositAgreementStatus.PENDING_PAYMENT,
                        DepositAgreementStatus.PAID,
                        DepositAgreementStatus.CONFIRMED,
                        DepositAgreementStatus.EXTENDED,
                        DepositAgreementStatus.CONVERTED_TO_LEASE
                ),
                RoomStatus.VACANT
        );
        assertEquals(PropertyStatus.ACTIVE, property.getStatus());
    }

    @Test
    void activationPreservesEveryNonDraftBusinessStatus() {
        List<RoomStatus> preserved = Arrays.stream(RoomStatus.values())
                .filter(status -> status != RoomStatus.DRAFT)
                .toList();

        assertEquals(
                List.of(
                        RoomStatus.VACANT,
                        RoomStatus.ON_HOLD,
                        RoomStatus.RESERVED,
                        RoomStatus.RESERVED_FOR_TRANSFER,
                        RoomStatus.OCCUPIED,
                        RoomStatus.SOON_VACANT,
                        RoomStatus.MAINTENANCE,
                        RoomStatus.EXPIRED
                ),
                preserved
        );
    }

    @Test
    void activationQueryProtectsDepositsAndActiveLeases() throws NoSuchMethodException {
        var method = JpaRoomRepository.class.getMethod(
                "updateDraftRoomsWithoutActiveCommitmentsToStatus",
                Long.class,
                RoomStatus.class,
                List.class,
                List.class,
                RoomStatus.class
        );
        String query = method.getAnnotation(Query.class).value();

        assertTrue(query.contains("room.currentStatus = :expectedStatus"));
        assertTrue(query.contains("contract.status IN :activeStatuses"));
        assertTrue(query.contains("deposit.status IN :activeDepositStatuses"));
    }

    @Test
    void activatingAnAlreadyActivePropertyIsIdempotent() {
        PropertyEntity property = property(PropertyStatus.ACTIVE);
        when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));
        when(propertyRepository.save(property)).thenReturn(property);

        controller.updatePropertyStatus(1L, new UpdatePropertyStatusRequest(PropertyStatus.ACTIVE));

        verify(roomRepository, never()).updateDraftRoomsWithoutActiveCommitmentsToStatus(
                any(), any(), any(), any(), any()
        );
        assertEquals(PropertyStatus.ACTIVE, property.getStatus());
    }

    private void givenPropertyCanBeActivated(PropertyEntity property) {
        when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));
        when(propertyRepository.save(property)).thenReturn(property);
        when(roomRepository.existsByProperty_IdAndDeletedAtIsNull(1L)).thenReturn(true);
        when(floorPlanItemRepository.existsByProperty_Id(1L)).thenReturn(true);
    }

    private static PropertyEntity property(PropertyStatus status) {
        return PropertyEntity.builder()
                .id(1L)
                .propertyCode("PROP-1")
                .name("Property 1")
                .addressLine("Address")
                .status(status)
                .build();
    }
}
