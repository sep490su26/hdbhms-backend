package com.sep490.hdbhms.occupancy.infrastructure.adapter;

import com.sep490.hdbhms.file.application.port.in.command.UploadFileCommand;
import com.sep490.hdbhms.file.application.port.in.usecase.UploadFileUseCase;
import com.sep490.hdbhms.file.domain.model.FileMetadata;
import com.sep490.hdbhms.file.domain.value_objects.FileCategory;
import com.sep490.hdbhms.occupancy.application.port.out.UploadIdentityFilePort;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@Component
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UploadIdentityFileAdapter implements UploadIdentityFilePort {
    UploadFileUseCase uploadFileUseCase;

    @Override
    public FileMetadata execute(MultipartFile multipartFile, FileCategory fileCategory)
            throws IOException {
        return uploadFileUseCase.execute(new UploadFileCommand(
                null,
                multipartFile,
                fileCategory,
                true
        ));
    }
}
