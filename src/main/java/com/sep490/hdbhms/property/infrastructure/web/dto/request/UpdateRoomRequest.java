package com.sep490.hdbhms.property.infrastructure.web.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.sep490.hdbhms.property.domain.value_objects.RoomStatus;
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
    @NotNull(message = "Vui lòng chọn tầng")
    Long floorId;

    @NotBlank(message = "Vui lòng nhập mã phòng")
    String roomCode;

    @NotBlank(message = "Vui lòng nhập tên phòng")
    String name;

    BigDecimal areaM2;

    @Min(value = 0, message = "Giá phòng không được âm")
    Long listedPrice;

    @Min(value = 1, message = "Số người tối đa phải lớn hơn 0")
    Integer maxOccupants;

    @Min(value = 0, message = "Thứ tự phòng không được âm")
    Integer sortOrder;

    @JsonAlias("status")
    RoomStatus currentStatus;

    String publicNote;
}
