package com.sep490.hdbhms.occupancy.infrastructure.web.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateFloorRequest {
    @NotNull(message = "propertyId is required")
    Long propertyId;

    @NotBlank(message = "floorCode is required")
    String floorCode;

    @NotBlank(message = "name is required")
    String name;

    @Min(value = 0, message = "sortOrder must not be negative")
    Integer sortOrder;
}
