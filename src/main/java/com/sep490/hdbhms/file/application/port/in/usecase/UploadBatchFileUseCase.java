package com.sep490.hdbhms.file.application.port.in.usecase;

import com.sep490.hdbhms.file.application.port.in.command.UploadBatchFileCommand;
import com.sep490.hdbhms.file.infrastructure.web.dto.response.BatchFileResponse;

public interface UploadBatchFileUseCase {
    BatchFileResponse execute(UploadBatchFileCommand query);
}
