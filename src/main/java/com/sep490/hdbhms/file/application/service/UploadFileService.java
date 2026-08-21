package com.sep490.hdbhms.file.application.service;

import com.sep490.hdbhms.file.application.port.in.command.UploadFileCommand;
import com.sep490.hdbhms.file.application.port.in.usecase.UploadFileUseCase;
import com.sep490.hdbhms.file.application.port.out.FileMetadataRepository;
import com.sep490.hdbhms.file.application.port.out.FileStoragePort;
import com.sep490.hdbhms.file.domain.model.FileMetadata;
import com.sep490.hdbhms.file.infrastructure.config.FileProperties;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import com.sep490.hdbhms.shared.utils.HashUtils;
import com.sep490.hdbhms.shared.utils.StringUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UploadFileService implements UploadFileUseCase {
    FileProperties fileProperties;
    FileMetadataRepository fileMetadataRepository;
    FileStoragePort fileStoragePort;

    @Override
    public FileMetadata execute(UploadFileCommand query) {
        String storageKey = null;
        try {
            String sha256Checksum = HashUtils.sha256Hex(query.file().getInputStream());
            MultipartFile multipartFile = query.file();
            Long ownerId = query.ownerUserId();

            Optional<FileMetadata> duplicate = fileMetadataRepository.findByChecksum(sha256Checksum);
            if (duplicate.isPresent()) {
                log.info("Returning existing file for checksum {}", sha256Checksum);
                return duplicate.get();
            }

            log.info("Uploading file: {}", multipartFile.getOriginalFilename());
            String fileName = UUID.randomUUID().toString();
            String fileExtension = StringUtils.getFilenameExtension(multipartFile.getOriginalFilename());
            String storedFileName = fileExtension == null ? fileName : fileName + "." + fileExtension;
            storageKey = buildStorageKey(storedFileName);

            String persistedStorageKey;
            try (var inputStream = multipartFile.getInputStream()) {
                persistedStorageKey = fileStoragePort.put(
                        storageKey,
                        inputStream,
                        multipartFile.getSize(),
                        multipartFile.getContentType()
                );
            }

            FileMetadata fileMetadata = FileMetadata.of(
                    ownerId,
                    multipartFile.getOriginalFilename(),
                    multipartFile.getContentType(),
                    multipartFile.getSize(),
                    sha256Checksum,
                    query.category(),
                    query.isSensitive()
            );
            fileMetadata.setStorageKey(persistedStorageKey);

            FileMetadata savedMetadata = fileMetadataRepository.save(fileMetadata);
            log.info("File uploaded successfully: {}", multipartFile.getOriginalFilename());
            return savedMetadata;
        } catch (IOException exception) {
            cleanupStoredFile(storageKey);
            throw new AppException(ApiErrorCode.FILE_UPLOAD_FAILED, exception);
        } catch (RuntimeException exception) {
            cleanupStoredFile(storageKey);
            throw exception;
        }
    }

    private String buildStorageKey(String fileName) {
        String keyPrefix = fileProperties.getStorage().getKeyPrefix();
        if (StringUtils.isEmpty(keyPrefix)) {
            keyPrefix = "files";
        }
        keyPrefix = keyPrefix.trim().replace('\\', '/').replaceAll("^/+|/+$", "");
        return keyPrefix.isEmpty() ? fileName : keyPrefix + "/" + fileName;
    }

    private void cleanupStoredFile(String storageKey) {
        if (storageKey == null) {
            return;
        }
        try {
            fileStoragePort.delete(storageKey);
        } catch (IOException cleanupException) {
            log.warn("Unable to clean up stored file {} after upload failure", storageKey, cleanupException);
        }
    }
}
