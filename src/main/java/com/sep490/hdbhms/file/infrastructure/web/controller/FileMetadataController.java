package com.sep490.hdbhms.file.infrastructure.web.controller;

import com.sep490.hdbhms.file.application.port.in.command.UploadBatchFileCommand;
import com.sep490.hdbhms.file.application.port.in.command.UploadFileCommand;
import com.sep490.hdbhms.file.application.port.in.query.DownloadFileQuery;
import com.sep490.hdbhms.file.application.service.DownloadFileService;
import com.sep490.hdbhms.file.application.service.UploadBatchFileService;
import com.sep490.hdbhms.file.application.service.UploadFileService;
import com.sep490.hdbhms.file.domain.value_objects.FileCategory;
import com.sep490.hdbhms.file.infrastructure.web.dto.response.BatchFileResponse;
import com.sep490.hdbhms.file.infrastructure.web.dto.response.FileMetadataResponse;
import com.sep490.hdbhms.file.infrastructure.web.mapper.FileMetadataWebMapper;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import com.sep490.hdbhms.shared.utils.AuthUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/resources")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FileMetadataController {
    UploadFileService uploadFileService;
    DownloadFileService downloadFileService;
    FileMetadataWebMapper fileMetadataWebMapper;
    UploadBatchFileService uploadBatchFileService;

    @PostMapping("/upload")
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<FileMetadataResponse> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam("category") FileCategory category,
            @RequestParam(value = "isSensitive", defaultValue = "false") boolean isSensitive
    ) {
        return ApiResponse.<FileMetadataResponse>builder()
                .data(
                        fileMetadataWebMapper.toSuccessResponse(
                                uploadFileService.execute(
                                        new UploadFileCommand(
                                                AuthUtils.getCurrentAuthenticationId(),
                                                file,
                                                category,
                                                isSensitive
                                        )
                                )
                        )
                )
                .build();
    }

    @PostMapping("/upload/batch")
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<BatchFileResponse> upload(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam("category") FileCategory category,
            @RequestParam("isSensitive") boolean isSensitive
    ) {
        return ApiResponse.<BatchFileResponse>builder()
                .data(uploadBatchFileService.execute(new UploadBatchFileCommand(
                        AuthUtils.getCurrentAuthenticationId(),
                        files,
                        category,
                        isSensitive
                )))
                .build();
    }

    @GetMapping("/download/{fileId}")
    @ResponseStatus(HttpStatus.OK)
    ResponseEntity<Resource> download(@PathVariable Long fileId) {
        var fileData = downloadFileService.execute(new DownloadFileQuery(fileId));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, fileData.contentType())
                .body(fileData.resource());
    }

    @PostMapping("/check")
    @ResponseStatus(HttpStatus.OK)
    ApiResponse<Object> check() {
        return ApiResponse.builder()
                .data(SecurityContextHolder.getContext().getAuthentication())
                .build();
    }
}
