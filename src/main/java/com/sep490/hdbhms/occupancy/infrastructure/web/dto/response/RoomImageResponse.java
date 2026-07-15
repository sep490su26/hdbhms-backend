package com.sep490.hdbhms.occupancy.infrastructure.web.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomImageResponse {
    Long id;
    Long fileId;
    String url;
    Integer sortOrder;
    LocalDateTime createdAt;
    boolean fallback;
    String sourceRoomCode;
}
