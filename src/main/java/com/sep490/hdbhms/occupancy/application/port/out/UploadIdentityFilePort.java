package com.sep490.hdbhms.occupancy.application.port.out;

import com.sep490.hdbhms.file.domain.model.FileMetadata;
import com.sep490.hdbhms.file.domain.valueObjects.FileCategory;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface UploadIdentityFilePort {

    FileMetadata execute(MultipartFile multipartFile, FileCategory fileCategory) throws IOException;
}
