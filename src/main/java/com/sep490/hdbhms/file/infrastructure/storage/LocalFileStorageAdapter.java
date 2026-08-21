package com.sep490.hdbhms.file.infrastructure.storage;

import com.sep490.hdbhms.file.application.port.out.FileStoragePort;
import com.sep490.hdbhms.file.infrastructure.config.FileProperties;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@ConditionalOnProperty(
        prefix = "app.file.storage",
        name = "provider",
        havingValue = "local",
        matchIfMissing = true
)
public class LocalFileStorageAdapter implements FileStoragePort {
    FileProperties fileProperties;

    @Override
    public String put(String storageKey, InputStream content, long contentLength, String contentType) throws IOException {
        Path target = resolveNewStoragePath(storageKey);
        Path parent = target.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
        return storageKey;
    }

    @Override
    public byte[] get(String storageKey) throws IOException {
        return Files.readAllBytes(resolveExistingStoragePath(storageKey));
    }

    @Override
    public void delete(String storageKey) throws IOException {
        Files.deleteIfExists(resolveExistingStoragePath(storageKey));
    }

    private Path resolveNewStoragePath(String storageKey) throws IOException {
        Path baseDirectory = storageDirectory();
        Path relativePath = Path.of(storageKey);
        if (relativePath.isAbsolute()) {
            throw new IOException("Local storage key must be relative: " + storageKey);
        }
        Path resolvedPath = baseDirectory.resolve(relativePath).normalize();
        if (!resolvedPath.startsWith(baseDirectory)) {
            throw new IOException("Invalid local storage key: " + storageKey);
        }
        return resolvedPath;
    }

    private Path resolveExistingStoragePath(String storageKey) throws IOException {
        Path requestedPath = Path.of(storageKey);
        if (requestedPath.isAbsolute()) {
            return requestedPath.normalize();
        }
        return resolveNewStoragePath(storageKey);
    }

    private Path storageDirectory() throws IOException {
        String directory = fileProperties.getStorage().getDirectory();
        if (directory == null || directory.isBlank()) {
            throw new IOException("Local file storage directory is not configured");
        }
        return Path.of(directory).toAbsolutePath().normalize();
    }
}
