package com.sep490.hdbhms.occupancy.infrastructure.web.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record SaveFloorPlanItemRequest(
        Long id,
        @NotBlank @JsonAlias("itemType") String type,
        Long roomId,
        @NotNull @JsonAlias("x") Integer positionX,
        @NotNull @JsonAlias("y") Integer positionY,
        @NotNull @Min(1) Integer width,
        @NotNull @Min(1) Integer height,
        Map<String, Object> metadata
) {
}
