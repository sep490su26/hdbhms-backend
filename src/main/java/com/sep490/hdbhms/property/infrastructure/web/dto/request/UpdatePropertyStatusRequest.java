package com.sep490.hdbhms.property.infrastructure.web.dto.request;

import com.sep490.hdbhms.property.domain.value_objects.PropertyStatus;
import jakarta.validation.constraints.NotNull;

public record UpdatePropertyStatusRequest(
        @NotNull(message = "Vui lòng chọn trạng thái cơ sở") PropertyStatus status
) {
}
