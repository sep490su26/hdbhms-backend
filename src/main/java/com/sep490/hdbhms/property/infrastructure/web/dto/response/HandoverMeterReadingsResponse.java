package com.sep490.hdbhms.property.infrastructure.web.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HandoverMeterReadingsResponse {
    Long electricityReadingId;
    Long waterReadingId;
}
