package com.sep490.hdbhms.file.infrastructure.web.mapper;

import com.sep490.hdbhms.file.domain.model.FileMetadata;
import com.sep490.hdbhms.file.infrastructure.config.FileProperties;
import com.sep490.hdbhms.file.infrastructure.web.dto.response.FileMetadataResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.WARN)
public abstract class FileMetadataWebMapper {
    @Autowired
    private FileProperties fileProperties;

    public FileMetadataResponse toSuccessResponse(FileMetadata fileMetadata) {
        return FileMetadataResponse.builder()
                .fileId(fileMetadata.getId())
                .originalFileName(fileMetadata.getOriginalName())
                .url(fileProperties.getDownload().getPrefix() + "/" + fileMetadata.getId())
                .uploaded(true)
                .build();
    }

    public FileMetadataResponse toFailedResponse(MultipartFile file, String message) {
        return FileMetadataResponse.builder()
                .originalFileName(file.getOriginalFilename())
                .uploaded(false)
                .message(message)
                .build();
    }
}
