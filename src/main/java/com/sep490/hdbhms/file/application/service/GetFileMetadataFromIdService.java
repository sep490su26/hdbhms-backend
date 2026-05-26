package com.sep490.hdbhms.file.application.service;

import com.sep490.hdbhms.file.application.port.in.query.GetFileMetadataFromIdQuery;
import com.sep490.hdbhms.file.application.port.in.usecase.GetFileMetadataFromIdUseCase;
import com.sep490.hdbhms.file.application.port.out.FileMetadataRepository;
import com.sep490.hdbhms.file.domain.model.FileMetadata;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GetFileMetadataFromIdService implements GetFileMetadataFromIdUseCase {
    FileMetadataRepository fileMetadataRepository;

    @Override
    public FileMetadata execute(GetFileMetadataFromIdQuery query) {
        return fileMetadataRepository.findById(query.fileMetadataId())
                .orElseThrow();
    }
}
