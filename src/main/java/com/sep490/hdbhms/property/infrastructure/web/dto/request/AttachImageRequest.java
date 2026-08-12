package com.sep490.hdbhms.property.infrastructure.web.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AttachImageRequest {
    @NotNull(message = "Mã tệp là bắt buộc")
    Long fileId;

    @Min(value = 0, message = "Thứ tự sắp xếp không được âm")
    Integer sortOrder;
}
