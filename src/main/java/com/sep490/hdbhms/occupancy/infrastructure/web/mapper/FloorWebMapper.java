package com.sep490.hdbhms.occupancy.infrastructure.web.mapper;

import com.sep490.hdbhms.occupancy.domain.model.Floor;
import com.sep490.hdbhms.occupancy.domain.model.Property;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.FloorResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = FloorWebMapper.class)
public interface FloorWebMapper {
    @Mapping(target = "id", source = "floor.id")
    @Mapping(target = "name", source = "floor.name")
    @Mapping(target = "status", source = "floor.status")
    @Mapping(target = "createdAt", source = "floor.createdAt")
    @Mapping(target = "updatedAt", source = "floor.updatedAt")
    @Mapping(target = "deletedAt", source = "floor.deletedAt")
    @Mapping(target = "property", source = "property")
    FloorResponse toFloorResponse(Floor floor, Property property);
}
