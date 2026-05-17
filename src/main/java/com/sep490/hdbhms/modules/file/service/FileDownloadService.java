package com.sep490.hdbhms.modules.file.service;

import com.sep490.hdbhms.common.exception.ApiException;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FileDownloadService {

    private final JdbcTemplate jdbcTemplate;
    private final Path storageDirectory;

    public FileDownloadService(
            JdbcTemplate jdbcTemplate,
            @Value("${app.file.storage.directory:${java.io.tmpdir}/hdbhms-files}") String storageDirectory
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.storageDirectory = StringUtils.hasText(storageDirectory)
                ? Path.of(storageDirectory)
                : Path.of(System.getProperty("java.io.tmpdir"), "hdbhms-files");
    }

    public PublicFile getPublicFile(Long fileId) {
        FileRow file = jdbcTemplate.query("""
                SELECT storage_key, mime_type, is_sensitive
                FROM file_metadata
                WHERE id = ?
                  AND deleted_at IS NULL
                """, rs -> rs.next()
                ? new FileRow(
                rs.getString("storage_key"),
                rs.getString("mime_type"),
                rs.getBoolean("is_sensitive")
        )
                : null, fileId);

        if (file == null || file.isSensitive()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy file");
        }

        Path path = storageDirectory.resolve(file.storageKey()).normalize();
        if (!path.startsWith(storageDirectory.normalize())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Đường dẫn file không hợp lệ");
        }

        Resource resource = new FileSystemResource(path);
        if (!resource.exists() || !resource.isReadable()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy file");
        }

        return new PublicFile(resource, file.mimeType());
    }

    public record PublicFile(Resource resource, String mimeType) {
    }

    private record FileRow(String storageKey, String mimeType, boolean isSensitive) {
    }
}
