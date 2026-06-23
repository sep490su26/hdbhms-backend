package com.sep490.hdbhms.occupancy.infrastructure.web.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("is_fallback")
    boolean fallback;
    @JsonProperty("source_room_code")
    String sourceRoomCode;
}
