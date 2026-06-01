package com.sep490.hdbhms.occupancy.infrastructure.web.controller;

import com.sep490.hdbhms.occupancy.application.port.in.query.GetFloorDetailsQuery;
import com.sep490.hdbhms.occupancy.application.port.in.query.GetListRoomsQuery;
import com.sep490.hdbhms.occupancy.application.port.in.query.GetPropertyDetailsQuery;
import com.sep490.hdbhms.occupancy.application.port.in.query.GetRoomDetailsQuery;
import com.sep490.hdbhms.occupancy.application.port.in.query.GetRoomImagesByRoomIdQuery;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.*;
import com.sep490.hdbhms.occupancy.domain.model.Floor;
import com.sep490.hdbhms.occupancy.domain.model.Property;
import com.sep490.hdbhms.occupancy.domain.model.Room;
import com.sep490.hdbhms.occupancy.domain.model.RoomImage;
import com.sep490.hdbhms.occupancy.domain.value_objects.RoomStatus;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.CreateRoomRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.RoomDetailsResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.RoomResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.mapper.RoomWebMapper;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import com.sep490.hdbhms.shared.dto.response.PageResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

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
    GetListRoomsUseCase getListRoomsUseCase;
    GetRoomByCodeUseCase getRoomByCodeUseCase;
    GetRoomDetailsUseCase getRoomDetailsUseCase;
    GetFloorDetailsUseCase getFloorDetailsUseCase;
    GetPropertyDetailsUseCase getPropertyDetailsUseCase;
    GetRoomImagesByRoomIdUseCase getRoomImagesByRoomIdUseCase;

    @GetMapping
    public ApiResponse<PageResponse<RoomResponse>> getRooms(
            @RequestParam(defaultValue = "1") Long propertyId,
            @RequestParam(required = false) Long floorId,
            @RequestParam(required = false) RoomStatus status,
            @RequestParam(required = false) Long minPrice,
            @RequestParam(required = false) Long maxPrice,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ApiResponse.<PageResponse<RoomResponse>>builder()
                .data(
                        PageResponse.fromPageToPageResponse(
                                getListRoomsUseCase.execute(
                                                new GetListRoomsQuery(
                                                        propertyId,
                                                        floorId,
                                                        status,
                                                        minPrice,
                                                        maxPrice,
                                                        pageable
                                                )
                                        )
                                        .map(room -> {
                                            Floor floor = getFloorDetailsUseCase.execute(
                                                    new GetFloorDetailsQuery(room.getFloorId())
                                            );
                                            Property property = getPropertyDetailsUseCase.execute(
                                                    new GetPropertyDetailsQuery(floor.getPropertyId())
                                            );
                                            return RoomResponse.builder()
                                                    .id(room.getId())
                                                    .name(room.getName())
                                                    .areaM2(room.getAreaM2())
                                                    .roomCode(room.getRoomCode())
                                                    .currentStatus(room.getCurrentStatus())
                                                    .listedPrice(room.getListedPrice())
                                                    .maxOccupants(room.getMaxOccupants())
                                                    .floorName(floor.getName())
                                                    .propertyName(property.getName())
                                                    .build();
                                        })
                        )
                )
                .build();
    }

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
        return ApiResponse.<RoomDetailsResponse>builder()
                .data(
                        roomWebMapper.toRoomDetailsResponse(
                                room,
                                floor,
                                property,
                                roomImages
                        )
                )
                .build();
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
        return ApiResponse.<RoomDetailsResponse>builder()
                .code(0)
                .data(
                        roomWebMapper.toRoomDetailsResponse(
                                room,
                                floor,
                                property,
                                roomImages
                        )
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
        return ApiResponse.<RoomDetailsResponse>builder()
                .code(0)
                .data(
                        roomWebMapper.toRoomDetailsResponse(
                                room,
                                floor,
                                property,
                                roomImages
                        )
                )
                .build();
    }
}
