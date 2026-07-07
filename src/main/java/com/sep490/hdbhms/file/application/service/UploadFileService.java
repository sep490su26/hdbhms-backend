package com.sep490.hdbhms.file.application.service;

import com.sep490.hdbhms.file.application.port.in.command.UploadFileCommand;
import com.sep490.hdbhms.file.application.port.in.usecase.UploadFileUseCase;
import com.sep490.hdbhms.file.application.port.out.FileMetadataRepository;
import com.sep490.hdbhms.file.domain.model.FileMetadata;
import com.sep490.hdbhms.file.infrastructure.config.FileProperties;
import com.sep490.hdbhms.file.infrastructure.utils.FileUtils;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import com.sep490.hdbhms.shared.utils.HashUtils;
import com.sep490.hdbhms.shared.utils.ServerInfoUtils;
import com.sep490.hdbhms.shared.utils.StringUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UploadFileService implements UploadFileUseCase {
    FileProperties fileProperties;
    ServerInfoUtils serverInfoUtils;
    FileMetadataRepository fileMetadataRepository;

    @Override
    public FileMetadata execute(UploadFileCommand query) {
        Path tempFilePath = null;
        Path finalPath = null;
        FileMetadata fileMetadata = null;
        try {
            String sha256Checksum = HashUtils.sha256Hex(query.file().getInputStream());

            MultipartFile multipartFile = query.file();
            Long ownerId = query.ownerUserId();
//            if (ownerId == null) {
//                throw new AppException(ApiErrorCode.UNAUTHENTICATED);
//            }
            // Check for duplicate using the unique database constraint
            Optional<FileMetadata> duplicate = fileMetadataRepository.findByChecksum(sha256Checksum);
            if (duplicate.isPresent()) {
                log.info("{}", duplicate.get());
                return duplicate.get();
            }

            log.info("Uploading file: {}", multipartFile.getOriginalFilename());

            String fileName = UUID.randomUUID().toString();
            // Prepare temporary directory
            Path tempDirectory = Path.of(fileProperties.getTemp().getDirectory());
            Files.createDirectories(tempDirectory);

            String fileExtension = StringUtils.getFilenameExtension(multipartFile.getOriginalFilename());
            String localFilename = (fileExtension == null) ? fileName : fileName + "." + fileExtension;
            String tempFilename = "temp_" + localFilename;
            tempFilePath = tempDirectory.resolve(tempFilename).normalize().toAbsolutePath();

            // Copy to temp file
            try (var is = multipartFile.getInputStream()) {
                Files.copy(is, tempFilePath, StandardCopyOption.REPLACE_EXISTING);
            }
            log.info("Getting user id: {} who uploaded file name: {}", ownerId, multipartFile.getOriginalFilename());

            Path fileDirectory = Path.of(fileProperties.getStorage().getDirectory());
            Files.createDirectories(fileDirectory);
            finalPath = fileDirectory.resolve(localFilename).normalize().toAbsolutePath();
            Files.move(tempFilePath, finalPath, StandardCopyOption.REPLACE_EXISTING);
            tempFilePath = null;

            // Build metadata only after the file has a durable storage path.
            fileMetadata = FileMetadata.of(
                    ownerId,
                    multipartFile.getOriginalFilename(),
                    multipartFile.getContentType(),
                    multipartFile.getSize(),
                    sha256Checksum,
                    query.category(),
                    query.isSensitive()
            );
            fileMetadata.setStorageKey(finalPath.toString());
            log.info(fileMetadata.toString());

            fileMetadata = fileMetadataRepository.save(fileMetadata);
            log.info("{}", fileMetadata);
            log.info("Successfully uploaded file: {}", multipartFile.getOriginalFilename());
            return fileMetadata;

        } catch (IOException | AppException | NumberFormatException ex) {
            FileUtils.cleanupTempFile(tempFilePath);
            FileUtils.cleanupTempFile(finalPath);
            // Only delete metadata if it was created but the file wasn't moved
            if (fileMetadata != null && StringUtils.isEmpty(fileMetadata.getStorageKey()) && fileMetadata.getId() != null) {
                fileMetadataRepository.deleteById(fileMetadata.getId());
            }

            throw new AppException(ApiErrorCode.UNDEFINED);
        } catch (RuntimeException ex) {
            FileUtils.cleanupTempFile(tempFilePath);
            FileUtils.cleanupTempFile(finalPath);
            throw ex;
        }
    }
}
