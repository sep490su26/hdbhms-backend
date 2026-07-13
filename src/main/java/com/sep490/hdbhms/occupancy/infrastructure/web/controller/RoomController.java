package com.sep490.hdbhms.occupancy.infrastructure.web.controller;

import com.sep490.hdbhms.occupancy.application.port.in.query.GetFloorDetailsQuery;
import com.sep490.hdbhms.occupancy.application.port.in.query.GetPropertyDetailsQuery;
import com.sep490.hdbhms.occupancy.application.port.in.query.GetRoomDetailsQuery;
import com.sep490.hdbhms.occupancy.application.port.in.query.GetRoomImagesByRoomIdQuery;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.*;
import com.sep490.hdbhms.occupancy.domain.model.Floor;
import com.sep490.hdbhms.occupancy.domain.model.Property;
import com.sep490.hdbhms.occupancy.domain.model.Room;
import com.sep490.hdbhms.occupancy.domain.model.RoomImage;
import com.sep490.hdbhms.occupancy.domain.value_objects.RoomStatus;
import com.sep490.hdbhms.occupancy.application.service.GetLatestMeterReadingsService;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.LatestMeterReadingsResponse;
import com.sep490.hdbhms.occupancy.application.service.RoomCommitmentChecker;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.CreateRoomRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.RoomDetailsResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.mapper.RoomWebMapper;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RoomEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaFloorPlanItemRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomRepository;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
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
                .data(
                        response
                )
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
