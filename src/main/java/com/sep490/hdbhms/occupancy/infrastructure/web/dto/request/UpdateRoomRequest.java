package com.sep490.hdbhms.occupancy.infrastructure.web.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.sep490.hdbhms.occupancy.domain.value_objects.RoomStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateRoomRequest {
    @NotNull(message = "floorId is required")
    Long floorId;

    @NotBlank(message = "roomCode is required")
    String roomCode;

    @NotBlank(message = "name is required")
    String name;

    BigDecimal areaM2;

    @Min(value = 0, message = "listedPrice must not be negative")
    Long listedPrice;

    @Min(value = 1, message = "maxOccupants must be greater than zero")
    Integer maxOccupants;

    @Min(value = 0, message = "sortOrder must not be negative")
    Integer sortOrder;

    @JsonAlias("status")
    RoomStatus currentStatus;

    String publicNote;
}
