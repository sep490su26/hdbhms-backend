package com.sep490.hdbhms.occupancy.infrastructure.web.mapper;

import com.sep490.hdbhms.occupancy.application.port.in.command.CreateRoomCommand;
import com.sep490.hdbhms.occupancy.application.port.in.command.SendDepositFormCommand;
import com.sep490.hdbhms.occupancy.domain.model.Floor;
import com.sep490.hdbhms.occupancy.domain.model.Property;
import com.sep490.hdbhms.occupancy.domain.model.Room;
import com.sep490.hdbhms.occupancy.domain.model.RoomImage;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.CreateRoomRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.SendDepositFormRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.RoomDetailsResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.RoomResponse;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Mapper(
        componentModel = "spring",
        uses = {PropertyWebMapper.class, FloorWebMapper.class, RoomImageWebMapper.class},
        injectionStrategy = InjectionStrategy.FIELD
)
public abstract class RoomWebMapper {
    @Autowired
    protected FloorWebMapper floorWebMapper;

    public abstract CreateRoomCommand toCommand(CreateRoomRequest request);

    public abstract SendDepositFormCommand toCommand(SendDepositFormRequest request, MultipartFile idFrontFile, MultipartFile idBackFile, MultipartFile portraitFile);

    @Mapping(target = "id", source = "room.id")
    @Mapping(target = "name", source = "room.name")
    @Mapping(target = "sortOrder", source = "room.sortOrder")
    @Mapping(target = "floor", expression = "java( floorWebMapper.toFloorResponse(floor, property) )")
    @Mapping(target = "images", source = "images")
    public abstract RoomDetailsResponse toRoomDetailsResponse(Room room, Floor floor, Property property, List<RoomImage> images);

    public abstract RoomResponse toResponse(Room room);
}
