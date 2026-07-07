package com.sep490.hdbhms.file.infrastructure.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sep490.hdbhms.file.domain.valueObjects.FileCategory;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FileMetadataResponse {
    Long fileId;
    String originalFileName;
    String url;
    boolean uploaded;
    String message;
}