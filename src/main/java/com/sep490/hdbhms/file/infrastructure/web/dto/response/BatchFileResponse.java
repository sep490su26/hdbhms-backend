package com.sep490.hdbhms.file.infrastructure.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BatchFileResponse {
    int totalFiles;
    int successfulUploads;
    int failedUploads;
    @Builder.Default
    List<FileMetadataResponse> fileMetadataResponse = new ArrayList<>();
    String message;
}