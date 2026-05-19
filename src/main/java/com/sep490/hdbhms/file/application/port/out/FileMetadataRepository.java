package com.sep490.hdbhms.file.application.port.out;

import com.sep490.hdbhms.file.domain.model.FileMetadata;

import java.util.List;
import java.util.Optional;

public interface FileMetadataRepository {
    FileMetadata save(FileMetadata fileMetadata);

    Optional<FileMetadata> findById(Long id);

    void deleteById(Long id);

    Optional<FileMetadata> findByChecksum(String sha256Checksum);

    long countByIdInAndDeletedAtIsNull(List<Long> fileIds);
}
