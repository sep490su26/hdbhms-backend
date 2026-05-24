package com.sep490.hdbhms.occupancy.infrastructure.web.mapper;

import com.sep490.hdbhms.occupancy.application.port.in.command.CreateRoomCommand;
import com.sep490.hdbhms.occupancy.application.port.in.command.SendDepositFormCommand;
import com.sep490.hdbhms.occupancy.domain.model.Floor;
import com.sep490.hdbhms.occupancy.domain.model.Room;
import com.sep490.hdbhms.occupancy.domain.model.RoomImage;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.CreateRoomRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.SendDepositFormRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.RoomDetailsResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.RoomResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = {PropertyWebMapper.class, FloorWebMapper.class, RoomImageWebMapper.class})
public interface RoomWebMapper {
    CreateRoomCommand toCommand(CreateRoomRequest request);

    SendDepositFormCommand toCommand(SendDepositFormRequest request);

    @Mapping(target = "id", source = "room.id")
    @Mapping(target = "name", source = "room.name")
    @Mapping(target = "sortOrder", source = "room.sortOrder")
    @Mapping(target = "floor", source = "floor")
    @Mapping(target = "images", source = "images")
    RoomDetailsResponse toRoomDetailsResponse(Room room, Floor floor, List<RoomImage> images);

    RoomResponse toResponse(Room room);
}
