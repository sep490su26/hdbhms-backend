package com.sep490.hdbhms.file.infrastructure.persistence.jpa;

import com.sep490.hdbhms.file.infrastructure.persistence.entity.FileMetadataEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JpaFileMetadataRepository extends JpaRepository<FileMetadataEntity, Long> {
    Optional<FileMetadataEntity> findBySha256Checksum(String checksum);

    long countByIdInAndDeletedAtIsNull(List<Long> fileIds);
}
