package com.sep490.hdbhms.file.infrastructure.utils;

import com.sep490.hdbhms.file.infrastructure.web.dto.response.FileResponse;
import com.sep490.hdbhms.shared.utils.ServerInfoUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FileUtils {

    @SneakyThrows
    public static void cleanupTempFile(Path tempFilePath) {
        if (tempFilePath != null && Files.exists(tempFilePath)) {
            Files.deleteIfExists(tempFilePath);
        }
    }

    public static String buildFileUrl(String fileId, String urlPrefix, ServerInfoUtils serverInfoUtils) {
        return serverInfoUtils.getBaseUrl() + urlPrefix + fileId;
    }

    public static FileResponse failedResponse(MultipartFile file, String message) {
        return FileResponse.builder()
                .originalFileName(file.getOriginalFilename())
                .uploaded(false)
                .message(message)
                .build();
    }
}
