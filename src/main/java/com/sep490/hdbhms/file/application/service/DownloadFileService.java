package com.sep490.hdbhms.file.application.service;

import com.sep490.hdbhms.file.application.port.in.query.DownloadFileQuery;
import com.sep490.hdbhms.file.application.port.in.usecase.DownloadFileUseCase;
import com.sep490.hdbhms.file.application.port.out.FileMetadataRepository;
import com.sep490.hdbhms.file.application.port.out.FileStoragePort;
import com.sep490.hdbhms.file.infrastructure.web.dto.response.FileDataResponse;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DownloadFileService implements DownloadFileUseCase {
    FileMetadataRepository fileMetadataRepository;
    FileStoragePort fileStoragePort;

    @Override
    public FileDataResponse execute(DownloadFileQuery command) {
        var file = fileMetadataRepository.findById(command.fileId()).orElse(null);
        if (file == null) {
            return null;
        }
        try {
            var data = readFileBytes(file.getStorageKey());
            var resource = new ByteArrayResource(data);
            return new FileDataResponse(file.getMimeType(), resource, file.isSensitive(), file.getOwnerUserId());
        } catch (IOException exception) {
            throw new AppException(ApiErrorCode.FILE_DOWNLOAD_FAILED, exception);
        }
    }

    private byte[] readFileBytes(String storageKey) throws IOException {
        try {
            return fileStoragePort.get(storageKey);
        } catch (IOException storageException) {
            if (!storageKey.startsWith("room-samples/") && !storageKey.startsWith("identity-samples/")) {
                throw storageException;
            }

            var classpathResource = new ClassPathResource("static/" + storageKey);
            if (classpathResource.exists()) {
                try (var inputStream = classpathResource.getInputStream()) {
                    return inputStream.readAllBytes();
                }
            }
            throw storageException;
        }
    }
}
