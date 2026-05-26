package com.sep490.hdbhms.file.application.port.in.usecase;

import com.sep490.hdbhms.file.application.port.in.query.GetFileMetadataFromIdQuery;
import com.sep490.hdbhms.file.domain.model.FileMetadata;

public interface GetFileMetadataFromIdUseCase {
    FileMetadata execute(GetFileMetadataFromIdQuery query);
}
