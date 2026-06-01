package com.sep490.hdbhms.file.application.service;

import com.sep490.hdbhms.file.application.port.in.query.DownloadFileQuery;
import com.sep490.hdbhms.file.application.port.in.usecase.DownloadFileUseCase;
import com.sep490.hdbhms.file.application.port.out.FileMetadataRepository;
import com.sep490.hdbhms.file.infrastructure.web.dto.response.FileDataResponse;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DownloadFileService implements DownloadFileUseCase {
    FileMetadataRepository fileMetadataRepository;

    @Override
    public FileDataResponse execute(DownloadFileQuery command) {
        var file = fileMetadataRepository.findById(command.fileId()).orElse(null);
        if (file == null) {
            return null;
        }
        try {
            var data = Files.readAllBytes(Path.of(file.getStorageKey()));
            var resource = new ByteArrayResource(data);
            return new FileDataResponse(file.getMimeType(), resource);
        } catch (IOException e) {
            throw new AppException(ApiErrorCode.UNDEFINED);
        }
    }
}
