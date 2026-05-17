package com.sep490.hdbhms.modules.file.controller;

import com.sep490.hdbhms.modules.file.service.FileDownloadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/files")
@Tag(name = "Files", description = "Public non-sensitive file download APIs")
public class FileController {

    private final FileDownloadService fileDownloadService;

    public FileController(FileDownloadService fileDownloadService) {
        this.fileDownloadService = fileDownloadService;
    }

    @GetMapping("/{fileId}")
    @Operation(summary = "Download a non-sensitive file by id")
    public ResponseEntity<Resource> downloadPublicFile(@PathVariable Long fileId) {
        FileDownloadService.PublicFile file = fileDownloadService.getPublicFile(fileId);
        MediaType mediaType = file.mimeType() == null || file.mimeType().isBlank()
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(file.mimeType());

        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(file.resource());
    }
}
