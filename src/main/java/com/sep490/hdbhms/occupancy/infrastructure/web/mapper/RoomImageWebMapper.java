package com.sep490.hdbhms.occupancy.infrastructure.web.mapper;

import com.sep490.hdbhms.occupancy.domain.model.RoomImage;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.RoomImageResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RoomImageWebMapper {
    @Mapping(target = "url", expression = "java(resolveUrl(roomImage))")
    RoomImageResponse toResponse(RoomImage roomImage);

    default String resolveUrl(RoomImage roomImage) {
        if (roomImage == null) {
            return null;
        }
        if (roomImage.getFallbackUrl() != null && !roomImage.getFallbackUrl().isBlank()) {
            return roomImage.getFallbackUrl();
        }
        if (roomImage.getFileId() == null) {
            return null;
        }
        return "/api/v1/files/download/" + roomImage.getFileId();
    }
}