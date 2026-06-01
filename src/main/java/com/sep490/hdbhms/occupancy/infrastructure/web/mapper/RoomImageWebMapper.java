package com.sep490.hdbhms.occupancy.infrastructure.web.mapper;

import com.sep490.hdbhms.occupancy.domain.model.RoomImage;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.RoomImageResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RoomImageWebMapper {
    @Mapping(target = "url", expression = "java(\"/api/v1/files/download/\" + roomImage.getFileId())")
    RoomImageResponse toResponse(RoomImage roomImage);
}
