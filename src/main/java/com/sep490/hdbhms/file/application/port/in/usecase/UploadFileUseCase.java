package com.sep490.hdbhms.file.application.port.in.usecase;

import com.sep490.hdbhms.file.application.port.in.command.UploadFileCommand;
import com.sep490.hdbhms.file.infrastructure.web.dto.response.FileResponse;

import java.io.IOException;

public interface UploadFileUseCase {
    FileResponse execute(UploadFileCommand query) throws IOException;
}
