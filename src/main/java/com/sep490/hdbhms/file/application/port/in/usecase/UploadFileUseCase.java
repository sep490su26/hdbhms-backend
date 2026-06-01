package com.sep490.hdbhms.file.application.port.in.usecase;

import com.sep490.hdbhms.file.application.port.in.command.UploadFileCommand;
import com.sep490.hdbhms.file.domain.model.FileMetadata;
import com.sep490.hdbhms.file.infrastructure.web.dto.response.FileMetadataResponse;

import java.io.IOException;

public interface UploadFileUseCase {
    FileMetadata execute(UploadFileCommand query) throws IOException;
}
