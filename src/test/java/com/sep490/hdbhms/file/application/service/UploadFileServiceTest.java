package com.sep490.hdbhms.file.application.service;

import com.sep490.hdbhms.file.application.port.in.command.UploadFileCommand;
import com.sep490.hdbhms.file.application.port.out.FileMetadataRepository;
import com.sep490.hdbhms.file.domain.model.FileMetadata;
import com.sep490.hdbhms.file.domain.value_objects.FileCategory;
import com.sep490.hdbhms.file.infrastructure.config.FileProperties;
import com.sep490.hdbhms.shared.utils.ServerInfoUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class UploadFileServiceTest {

    @TempDir
    Path tempRoot;

    @Test
    void uploadPersistsMetadataOnlyAfterStorageKeyIsAvailable() throws Exception {
        FileProperties fileProperties = new FileProperties();
        fileProperties.getTemp().setDirectory(tempRoot.resolve("temp").toString());
        fileProperties.getStorage().setDirectory(tempRoot.resolve("storage").toString());

        AtomicInteger saves = new AtomicInteger();
        FileMetadataRepository repository = new FileMetadataRepository() {
            @Override
            public FileMetadata save(FileMetadata fileMetadata) {
                saves.incrementAndGet();
                assertNotNull(fileMetadata.getStorageKey());
                assertTrue(Files.exists(Path.of(fileMetadata.getStorageKey())));
                return FileMetadata.builder()
                        .id(99L)
                        .ownerUserId(fileMetadata.getOwnerUserId())
                        .storageKey(fileMetadata.getStorageKey())
                        .originalName(fileMetadata.getOriginalName())
                        .mimeType(fileMetadata.getMimeType())
                        .sizeBytes(fileMetadata.getSizeBytes())
                        .sha256Checksum(fileMetadata.getSha256Checksum())
                        .category(fileMetadata.getCategory())
                        .isSensitive(fileMetadata.isSensitive())
                        .build();
            }

            @Override
            public Optional<FileMetadata> findById(Long id) {
                return Optional.empty();
            }

            @Override
            public void deleteById(Long id) {
            }

            @Override
            public Optional<FileMetadata> findByChecksum(String sha256Checksum) {
                return Optional.empty();
            }

            @Override
            public long countByIdInAndDeletedAtIsNull(List<Long> fileIds) {
                return 0;
            }
        };

        UploadFileService service = new UploadFileService(
                fileProperties,
                mock(ServerInfoUtils.class),
                repository
        );

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "maintenance.png",
                "image/png",
                new byte[]{1, 2, 3, 4}
        );

        FileMetadata uploaded = service.execute(
                new UploadFileCommand(7L, file, FileCategory.MAINTENANCE, false)
        );

        assertEquals(1, saves.get());
        assertEquals(99L, uploaded.getId());
        assertEquals(FileCategory.MAINTENANCE, uploaded.getCategory());
        assertTrue(Files.exists(Path.of(uploaded.getStorageKey())));
    }
}
