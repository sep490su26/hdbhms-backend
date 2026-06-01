package com.sep490.hdbhms.file.infrastructure.persistence.repository;

import com.sep490.hdbhms.file.application.port.out.FileMetadataRepository;
import com.sep490.hdbhms.file.domain.model.FileMetadata;
import com.sep490.hdbhms.file.infrastructure.persistence.jpa.JpaFileMetadataRepository;
import com.sep490.hdbhms.file.infrastructure.persistence.mapper.FileMetadataPersistenceMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SpringDataFileMetadataMetadataRepository implements FileMetadataRepository {
    FileMetadataPersistenceMapper fileMetadataPersistenceMapper;
    JpaFileMetadataRepository jpaFileMetadataRepository;

    @Override
    public FileMetadata save(FileMetadata fileMetadata) {
        return fileMetadataPersistenceMapper.toDomain(
                jpaFileMetadataRepository.save(
                        fileMetadataPersistenceMapper.toEntity(fileMetadata)
                )
        );
    }

    @Override
    public Optional<FileMetadata> findById(Long id) {
        return jpaFileMetadataRepository.findById(id)
                .map(fileMetadataPersistenceMapper::toDomain);
    }

    @Override
    public void deleteById(Long id) {
        jpaFileMetadataRepository.deleteById(id);
    }

    @Override
    public Optional<FileMetadata> findByChecksum(String sha256Checksum) {
        return jpaFileMetadataRepository.findBySha256Checksum(sha256Checksum)
                .map(fileMetadataPersistenceMapper::toDomain);
    }

    @Override
    public long countByIdInAndDeletedAtIsNull(List<Long> fileIds) {
        return jpaFileMetadataRepository.countByIdInAndDeletedAtIsNull(fileIds);
    }
}
