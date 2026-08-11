package com.sep490.hdbhms.property.infrastructure.web.dto.request;

import com.sep490.hdbhms.property.domain.value_objects.PropertyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreatePropertyRequest {
    @NotBlank(message = "Vui lòng nhập tên cơ sở.")
    String name;
    @NotNull(message = "Vui lòng chọn loại hình cơ sở.")
    PropertyType propertyType;
    @NotBlank(message = "Vui lòng nhập địa chỉ cơ sở.")
    String addressLine;
    String description;
}
