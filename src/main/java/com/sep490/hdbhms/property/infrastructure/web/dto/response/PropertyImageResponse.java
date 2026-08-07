package com.sep490.hdbhms.property.infrastructure.web.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PropertyImageResponse {
    Long id;
    Long fileId;
    String url;
    Integer sortOrder;
    LocalDateTime createdAt;
}
