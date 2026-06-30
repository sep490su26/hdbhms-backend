package com.sep490.hdbhms.file.application.port.in.command;

import com.sep490.hdbhms.file.domain.valueObjects.FileCategory;
import org.springframework.web.multipart.MultipartFile;

public record UploadFileCommand(Long ownerUserId, MultipartFile file, FileCategory category, boolean isSensitive) {
}
