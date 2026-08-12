package com.sep490.hdbhms.property.infrastructure.web.controller;

import com.sep490.hdbhms.property.application.port.in.command.AttachRoomImageCommand;
import com.sep490.hdbhms.property.application.port.in.command.DeleteRoomImageCommand;
import com.sep490.hdbhms.property.application.port.in.query.GetFloorDetailsQuery;
import com.sep490.hdbhms.property.application.port.in.query.GetPropertyDetailsQuery;
import com.sep490.hdbhms.property.application.port.in.query.GetRoomDetailsQuery;
import com.sep490.hdbhms.property.application.port.in.query.GetRoomImagesByRoomIdQuery;
import com.sep490.hdbhms.property.application.port.in.usecase.AttachRoomImageUseCase;
import com.sep490.hdbhms.property.application.port.in.usecase.CreateRoomUseCase;
import com.sep490.hdbhms.property.application.port.in.usecase.DeleteRoomImageUseCase;
import com.sep490.hdbhms.property.application.port.in.usecase.GetFloorDetailsUseCase;
import com.sep490.hdbhms.property.application.port.in.usecase.GetPropertyDetailsUseCase;
import com.sep490.hdbhms.property.application.port.in.usecase.GetRoomByCodeUseCase;
import com.sep490.hdbhms.property.application.port.in.usecase.GetRoomDetailsUseCase;
import com.sep490.hdbhms.property.application.port.in.usecase.GetRoomImagesByRoomIdUseCase;
import com.sep490.hdbhms.property.domain.model.Floor;
import com.sep490.hdbhms.property.domain.model.Property;
import com.sep490.hdbhms.property.domain.model.Room;
import com.sep490.hdbhms.property.domain.model.RoomImage;
import com.sep490.hdbhms.property.domain.value_objects.RoomStatus;
import com.sep490.hdbhms.property.application.service.GetLatestMeterReadingsService;
import com.sep490.hdbhms.property.infrastructure.web.dto.response.LatestMeterReadingsResponse;
import com.sep490.hdbhms.property.application.service.RoomCommitmentChecker;
import com.sep490.hdbhms.property.infrastructure.web.dto.request.CreateRoomRequest;
import com.sep490.hdbhms.property.infrastructure.web.dto.request.AttachImageRequest;
import com.sep490.hdbhms.property.infrastructure.web.dto.request.UpdateRoomRequest;
import com.sep490.hdbhms.property.infrastructure.web.dto.response.RoomDetailsResponse;
import com.sep490.hdbhms.property.infrastructure.web.dto.response.RoomImageResponse;
import com.sep490.hdbhms.property.infrastructure.web.mapper.RoomImageWebMapper;
import com.sep490.hdbhms.property.infrastructure.web.mapper.RoomWebMapper;
import com.sep490.hdbhms.property.infrastructure.persistence.entity.FloorEntity;
import com.sep490.hdbhms.property.infrastructure.persistence.entity.RoomEntity;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaFloorRepository;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaFloorPlanItemRepository;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaRoomRepository;
import com.sep490.hdbhms.shared.types.dto.response.ApiResponse;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

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
    RoomImageWebMapper roomImageWebMapper;
    CreateRoomUseCase createRoomUseCase;
    AttachRoomImageUseCase attachRoomImageUseCase;
    DeleteRoomImageUseCase deleteRoomImageUseCase;
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
                .message("Thêm phòng mới thành công")
                .data(
                        response
                )
                .build();
    }

    @PutMapping("/{roomId}")
    @Transactional
    public ApiResponse<RoomDetailsResponse> updateRoom(
            @PathVariable Long roomId,
            @Valid @RequestBody UpdateRoomRequest request
    ) {
        RoomEntity room = roomRepository.findById(roomId)
                .filter(item -> item.getDeletedAt() == null)
                .orElseThrow(() -> new AppException(ApiErrorCode.ROOM_NOT_FOUND));
        FloorEntity floor = floorRepository.findById(request.getFloorId())
                .filter(item -> item.getDeletedAt() == null)
                .orElseThrow(() -> new AppException(ApiErrorCode.FLOOR_NOT_FOUND));

        Long propertyId = room.getProperty().getId();
        if (!floor.getProperty().getId().equals(propertyId)) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }

        String nextRoomCode = request.getRoomCode().trim();
        if (!nextRoomCode.equals(room.getRoomCode())
                && roomRepository.existsByProperty_IdAndRoomCodeAndDeletedAtIsNull(propertyId, nextRoomCode)) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
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
                .message("Cập nhật phòng thành công")
                .data(response)
                .build();
    }

    @DeleteMapping("/{roomId}")
    @Transactional
    public ApiResponse<Void> deleteRoom(@PathVariable Long roomId) {
        RoomEntity room = roomRepository.findById(roomId).orElseThrow();
        floorPlanItemRepository.deleteByProperty_IdAndRoom_Id(room.getProperty().getId(), roomId);
        room.setDeletedAt(LocalDateTime.now());
        return ApiResponse.<Void>builder().build();
    }

    @GetMapping("/{roomId}/images")
    public ApiResponse<List<RoomImageResponse>> getRoomImages(@PathVariable Long roomId) {
        return ApiResponse.<List<RoomImageResponse>>builder()
                .data(getRoomImagesByRoomIdUseCase.execute(new GetRoomImagesByRoomIdQuery(roomId)).stream()
                        .map(roomImageWebMapper::toResponse)
                        .toList())
                .build();
    }

    @PostMapping("/{roomId}/images")
    public ApiResponse<RoomImageResponse> attachRoomImage(
            @PathVariable Long roomId,
            @Valid @RequestBody AttachImageRequest request
    ) {
        RoomImage image = attachRoomImageUseCase.execute(new AttachRoomImageCommand(
                roomId,
                request.getFileId(),
                request.getSortOrder()
        ));
        return ApiResponse.<RoomImageResponse>builder()
                .data(roomImageWebMapper.toResponse(image))
                .build();
    }

    @DeleteMapping("/{roomId}/images/{imageId}")
    public ApiResponse<Void> deleteRoomImage(
            @PathVariable Long roomId,
            @PathVariable Long imageId
    ) {
        deleteRoomImageUseCase.execute(new DeleteRoomImageCommand(roomId, imageId));
        return ApiResponse.<Void>builder().build();
    }

    @GetMapping("/id/{roomId}")
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

    @GetMapping("/{roomId}/meter-readings/latest")
//    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ApiResponse<LatestMeterReadingsResponse> getLatestMeterReadings(@PathVariable Long roomId) {
        return ApiResponse.<LatestMeterReadingsResponse>builder()
                .data(getLatestMeterReadingsService.getLatestReadings(roomId))
                .build();
    }
}
