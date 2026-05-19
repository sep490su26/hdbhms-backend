package com.sep490.hdbhms.file.application.port.in.usecase;

import com.sep490.hdbhms.file.application.port.in.query.DownloadFileQuery;
import com.sep490.hdbhms.file.infrastructure.web.dto.response.FileDataResponse;

public interface DownloadFileUseCase {
    FileDataResponse execute(DownloadFileQuery command);
}
