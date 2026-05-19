package com.sep490.hdbhms.file.application.port.in.command;


import com.sep490.hdbhms.file.domain.value_objects.FileCategory;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public record UploadBatchFileCommand(Long userId, List<MultipartFile> files, FileCategory category,
                                     boolean isSensitive) {
}
