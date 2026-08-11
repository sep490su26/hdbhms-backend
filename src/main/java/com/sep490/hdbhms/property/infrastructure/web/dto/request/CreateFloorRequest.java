package com.sep490.hdbhms.property.infrastructure.web.dto.request;

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
    @NotNull(message = "Vui lòng chọn cơ sở")
    Long propertyId;

    @NotBlank(message = "Vui lòng nhập mã tầng")
    String floorCode;

    @NotBlank(message = "Vui lòng nhập tên tầng")
    String name;

    @Min(value = 0, message = "Thứ tự tầng không được nhỏ hơn 0")
    Integer sortOrder;
}
