package com.sep490.hdbhms.occupancy.infrastructure.web.controller;

import com.sep490.hdbhms.identityandaccess.infrastructure.config.security.UserPrincipal;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import com.sep490.hdbhms.occupancy.application.port.in.query.GetFloorDetailsQuery;
import com.sep490.hdbhms.occupancy.application.port.in.query.GetPropertyDetailsQuery;
import com.sep490.hdbhms.occupancy.application.port.in.query.GetRoomDetailsQuery;
import com.sep490.hdbhms.occupancy.application.port.in.query.GetRoomImagesByRoomIdQuery;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.*;
import com.sep490.hdbhms.occupancy.domain.model.Floor;
import com.sep490.hdbhms.occupancy.domain.model.Property;
import com.sep490.hdbhms.occupancy.domain.model.Room;
import com.sep490.hdbhms.occupancy.domain.model.RoomImage;
import com.sep490.hdbhms.occupancy.domain.value_objects.PropertyStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.RoomStatus;
import com.sep490.hdbhms.occupancy.application.service.GetLatestMeterReadingsService;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.LatestMeterReadingsResponse;
import com.sep490.hdbhms.occupancy.application.service.RoomCommitmentChecker;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.CreateRoomRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.UpdateRoomRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.RoomDetailsResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.mapper.RoomWebMapper;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.FloorEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RoomEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaFloorRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaFloorPlanItemRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomRepository;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/rooms")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoomController {
    RoomWebMapper roomWebMapper;
    BookRoomUseCase bookRoomUseCase;
    CreateRoomUseCase createRoomUseCase;
    GetLatestMeterReadingsService getLatestMeterReadingsService;
    GetRoomByCodeUseCase getRoomByCodeUseCase;
    GetRoomDetailsUseCase getRoomDetailsUseCase;
    GetFloorDetailsUseCase getFloorDetailsUseCase;
    GetPropertyDetailsUseCase getPropertyDetailsUseCase;
    GetRoomImagesByRoomIdUseCase getRoomImagesByRoomIdUseCase;
    RoomCommitmentChecker roomCommitmentChecker;
    JpaRoomRepository roomRepository;
    JpaFloorRepository floorRepository;
    JpaFloorPlanItemRepository floorPlanItemRepository;

    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<RoomDetailsResponse> createRoom(
            @Valid @RequestBody CreateRoomRequest request
    ) {
        Room room = createRoomUseCase.execute(roomWebMapper.toCommand(request));
        Floor floor = getFloorDetailsUseCase.execute(
                new GetFloorDetailsQuery(room.getFloorId())
        );
        Property property = getPropertyDetailsUseCase.execute(
                new GetPropertyDetailsQuery(floor.getPropertyId())
        );
        List<RoomImage> roomImages = getRoomImagesByRoomIdUseCase.execute(
                new GetRoomImagesByRoomIdQuery(room.getId())
        );
        RoomDetailsResponse response = roomWebMapper.toRoomDetailsResponse(
                room,
                floor,
                property,
                roomImages
        );
        response.setExpectedVacantDate(expectedVacantDate(room));
        return ApiResponse.<RoomDetailsResponse>builder()
                .data(
                        response
                )
                .build();
    }

    @PutMapping("/{roomId}")
    @Transactional
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<RoomDetailsResponse> updateRoom(
            @PathVariable Long roomId,
            @Valid @RequestBody UpdateRoomRequest request
    ) {
        RoomEntity room = roomRepository.findById(roomId)
                .filter(item -> item.getDeletedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
        FloorEntity floor = floorRepository.findById(request.getFloorId())
                .filter(item -> item.getDeletedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Floor not found"));

        Long propertyId = room.getProperty().getId();
        if (!floor.getProperty().getId().equals(propertyId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Floor does not belong to room property");
        }

        String nextRoomCode = request.getRoomCode().trim();
        if (!nextRoomCode.equals(room.getRoomCode())
                && roomRepository.existsByProperty_IdAndRoomCodeAndDeletedAtIsNull(propertyId, nextRoomCode)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Room code already exists");
        }

        boolean movedFloor = !floor.getId().equals(room.getFloor().getId());
        room.setFloor(floor);
        room.setRoomCode(nextRoomCode);
        room.setName(request.getName().trim());
        room.setAreaM2(request.getAreaM2());
        room.setListedPrice(request.getListedPrice() == null ? 0L : request.getListedPrice());
        room.setMaxOccupants(request.getMaxOccupants() == null ? 3 : request.getMaxOccupants());
        if (request.getSortOrder() != null) {
            room.setSortOrder(request.getSortOrder());
        }
        if (request.getCurrentStatus() != null) {
            room.setCurrentStatus(request.getCurrentStatus());
        }
        room.setPublicNote(request.getPublicNote() == null ? null : request.getPublicNote().trim());

        if (movedFloor) {
            floorPlanItemRepository.findAllByProperty_IdAndRoom_Id(propertyId, roomId)
                    .forEach(item -> item.setFloor(floor));
        }

        roomRepository.saveAndFlush(room);

        Room updatedRoom = getRoomDetailsUseCase.execute(new GetRoomDetailsQuery(roomId));
        Floor updatedFloor = getFloorDetailsUseCase.execute(
                new GetFloorDetailsQuery(updatedRoom.getFloorId())
        );
        Property property = getPropertyDetailsUseCase.execute(
                new GetPropertyDetailsQuery(updatedFloor.getPropertyId())
        );
        List<RoomImage> roomImages = getRoomImagesByRoomIdUseCase.execute(
                new GetRoomImagesByRoomIdQuery(updatedRoom.getId())
        );
        RoomDetailsResponse response = roomWebMapper.toRoomDetailsResponse(
                updatedRoom,
                updatedFloor,
                property,
                roomImages
        );
        response.setExpectedVacantDate(expectedVacantDate(updatedRoom));
        return ApiResponse.<RoomDetailsResponse>builder()
                .code(0)
                .data(response)
                .build();
    }

    @DeleteMapping("/{roomId}")
    @Transactional
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<Void> deleteRoom(@PathVariable Long roomId) {
        RoomEntity room = roomRepository.findById(roomId).orElseThrow();
        floorPlanItemRepository.deleteByProperty_IdAndRoom_Id(room.getProperty().getId(), roomId);
        room.setDeletedAt(LocalDateTime.now());
        return ApiResponse.<Void>builder().build();
    }

    @GetMapping("/id/{roomId}")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ApiResponse<RoomDetailsResponse> getRoomById(@PathVariable Long roomId) {
        Room room = getRoomDetailsUseCase.execute(new GetRoomDetailsQuery(roomId));
        Floor floor = getFloorDetailsUseCase.execute(
                new GetFloorDetailsQuery(room.getFloorId())
        );
        Property property = getPropertyDetailsUseCase.execute(
                new GetPropertyDetailsQuery(floor.getPropertyId())
        );
        List<RoomImage> roomImages = getRoomImagesByRoomIdUseCase.execute(
                new GetRoomImagesByRoomIdQuery(room.getId())
        );
        RoomDetailsResponse response = roomWebMapper.toRoomDetailsResponse(
                room,
                floor,
                property,
                roomImages
        );
        response.setExpectedVacantDate(expectedVacantDate(room));
        return ApiResponse.<RoomDetailsResponse>builder()
                .code(0)
                .data(
                        response
                )
                .build();
    }

    @GetMapping("/{roomCode}")
    public ApiResponse<RoomDetailsResponse> getRoomByCode(@PathVariable String roomCode) {
        Room room = getRoomByCodeUseCase.getRoomByCode(roomCode);
        Floor floor = getFloorDetailsUseCase.execute(
                new GetFloorDetailsQuery(room.getFloorId())
        );
        Property property = getPropertyDetailsUseCase.execute(
                new GetPropertyDetailsQuery(floor.getPropertyId())
        );
        assertPubliclyVisible(room, property);
        List<RoomImage> roomImages = getRoomImagesByRoomIdUseCase.execute(
                new GetRoomImagesByRoomIdQuery(room.getId())
        );
        RoomDetailsResponse response = roomWebMapper.toRoomDetailsResponse(
                room,
                floor,
                property,
                roomImages
        );
        response.setExpectedVacantDate(expectedVacantDate(room));
        return ApiResponse.<RoomDetailsResponse>builder()
                .code(0)
                .data(
                        response
                )
                .build();
    }

    private LocalDate expectedVacantDate(Room room) {
        if (room.getCurrentStatus() != RoomStatus.SOON_VACANT) {
            return null;
        }
        return roomCommitmentChecker.findExpectedVacantDateForBooking(room.getId()).orElse(null);
    }

    private void assertPubliclyVisible(Room room, Property property) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            if (principal.getRole() == Role.OWNER || principal.getRole() == Role.MANAGER) {
                return;
            }
        }
        if (property.getStatus() != PropertyStatus.ACTIVE
                || !RoomCatalogController.isPublicRoomStatus(room.getCurrentStatus())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found");
        }
    }

    @GetMapping("/{roomId}/meter-readings/latest")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ApiResponse<LatestMeterReadingsResponse> getLatestMeterReadings(@PathVariable Long roomId) {
        return ApiResponse.<LatestMeterReadingsResponse>builder()
                .data(getLatestMeterReadingsService.getLatestReadings(roomId))
                .build();
    }
}
