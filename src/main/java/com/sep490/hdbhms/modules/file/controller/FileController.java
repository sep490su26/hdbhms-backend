//package com.sep490.hdbhms.modules.file.controller;
//
//import com.sep490.hdbhms.modules.file.service.FileDownloadService;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import org.springframework.http.MediaType;
//import org.springframework.http.ResponseEntity;
//import org.springframework.core.io.Resource;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.security.oauth2.jwt.Jwt;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.RestController;
//
//@RestController
//@Tag(name = "Files", description = "Public non-sensitive file download APIs")
//public class FileController {
//
//    private final FileDownloadService fileDownloadService;
//
//    public FileController(FileDownloadService fileDownloadService) {
//        this.fileDownloadService = fileDownloadService;
//    }
//
//    @GetMapping("/api/v1/files/{fileId}")
//    @Operation(summary = "Download a non-sensitive file by id")
//    public ResponseEntity<Resource> downloadPublicFile(@PathVariable Long fileId) {
//        FileDownloadService.PublicFile file = fileDownloadService.getPublicFile(fileId);
//        return fileResponse(file);
//    }
//
//    @GetMapping("/api/v1/tenants/{tenantId}/me/files/{fileId}")
//    @Operation(summary = "Download a tenant profile file by id")
//    public ResponseEntity<Resource> downloadTenantFile(
//            @AuthenticationPrincipal Jwt jwt,
//            @PathVariable Long tenantId,
//            @PathVariable Long fileId
//    ) {
//        FileDownloadService.PublicFile file = fileDownloadService.getTenantFile(
//                Long.parseLong(jwt.getSubject()),
//                tenantId,
//                fileId
//        );
//        return fileResponse(file);
//    }
//
//    private ResponseEntity<Resource> fileResponse(FileDownloadService.PublicFile file) {
//        MediaType mediaType = file.mimeType() == null || file.mimeType().isBlank()
//                ? MediaType.APPLICATION_OCTET_STREAM
//                : MediaType.parseMediaType(file.mimeType());
//
//        return ResponseEntity.ok()
//                .contentType(mediaType)
//                .body(file.resource());
//    }
//}
