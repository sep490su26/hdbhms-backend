package com.sep490.hdbhms.file.infrastructure.web.controller;

import com.sep490.hdbhms.file.application.port.in.command.UploadBatchFileCommand;
import com.sep490.hdbhms.file.application.port.in.command.UploadFileCommand;
import com.sep490.hdbhms.file.application.port.in.query.DownloadFileQuery;
import com.sep490.hdbhms.file.application.service.DownloadFileService;
import com.sep490.hdbhms.file.application.service.UploadBatchFileService;
import com.sep490.hdbhms.file.application.service.UploadFileService;
import com.sep490.hdbhms.file.domain.valueObjects.FileCategory;
import com.sep490.hdbhms.file.infrastructure.web.dto.response.BatchFileResponse;
import com.sep490.hdbhms.file.infrastructure.web.dto.response.FileMetadataResponse;
import com.sep490.hdbhms.file.infrastructure.web.mapper.FileMetadataWebMapper;
import com.sep490.hdbhms.identityandaccess.domain.valueObjects.Role;
import com.sep490.hdbhms.identityandaccess.infrastructure.config.security.UserPrincipal;
import com.sep490.hdbhms.occupancy.application.service.LeaseContractQueryService;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/files")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FileMetadataController {
    UploadFileService uploadFileService;
    DownloadFileService downloadFileService;
    FileMetadataWebMapper fileMetadataWebMapper;
    UploadBatchFileService uploadBatchFileService;
    JdbcTemplate jdbcTemplate;
    LeaseContractQueryService leaseContractQueryService;

    @PostMapping("/upload")
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<FileMetadataResponse> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "category", defaultValue = "OTHER") FileCategory category,
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
        if (fileData == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }
        if (fileData.sensitive() && !canDownloadSensitiveFile(fileId, fileData.ownerUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to view this file");
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, fileData.contentType())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(fileData.resource());
    }

    @GetMapping("/private/{fileId}")
    @ResponseStatus(HttpStatus.OK)
    ResponseEntity<Resource> downloadPrivate(@PathVariable Long fileId) {
        assertOwnerOrManager();
        var fileData = downloadFileService.execute(new DownloadFileQuery(fileId));
        if (fileData == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, fileData.contentType())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(fileData.resource());
    }

    @PostMapping("/check")
    @ResponseStatus(HttpStatus.OK)
    ApiResponse<Object> check() {
        return ApiResponse.builder()
                .data(SecurityContextHolder.getContext().getAuthentication())
                .build();
    }

    private void assertOwnerOrManager() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please login to view this file");
        }

        Role role = principal.getRole();
        if (role != Role.OWNER && role != Role.MANAGER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to view this file");
        }
    }

    private boolean canDownloadSensitiveFile(Long fileId, Long ownerUserId) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return false;
        }

        Role role = principal.getRole();
        if (role == Role.OWNER || role == Role.MANAGER) {
            return true;
        }

        if (ownerUserId != null && ownerUserId.equals(principal.getId())) {
            return true;
        }

        if (role != Role.TENANT) {
            return false;
        }

        return canDownloadTenantLinkedFile(fileId);
    }

    private boolean canDownloadTenantLinkedFile(Long fileId) {
        return canReadAnyLinkedContract(findLinkedHandoverContractIds(fileId))
                || canReadAnyLinkedRoom(findLinkedRoomAssetRoomIds(fileId));
    }

    private List<Long> findLinkedHandoverContractIds(Long fileId) {
        return jdbcTemplate.queryForList("""
                        SELECT DISTINCT linked.contract_id
                        FROM (
                            SELECT chr.contract_id
                            FROM contract_handover_items chi
                            JOIN contract_handover_records chr
                              ON chr.contract_handover_record_id = chi.handover_record_id
                            WHERE chi.evidence_file_id = ?
                            UNION
                            SELECT chr.contract_id
                            FROM contract_handover_records chr
                            WHERE chr.signed_document_id = ?
                            UNION
                            SELECT chr.contract_id
                            FROM contract_handover_records chr
                            JOIN meter_readings mr
                              ON mr.meter_reading_id = chr.electricity_reading_id
                              OR mr.meter_reading_id = chr.water_reading_id
                            WHERE mr.photo_file_id = ?
                        ) linked
                        """,
                Long.class,
                fileId,
                fileId,
                fileId
        );
    }

    private List<Long> findLinkedRoomAssetRoomIds(Long fileId) {
        return jdbcTemplate.queryForList("""
                        SELECT DISTINCT room_id
                        FROM room_assets
                        WHERE image_file_id = ?
                          AND deleted_at IS NULL
                        """,
                Long.class,
                fileId
        );
    }

    private boolean canReadAnyLinkedContract(List<Long> contractIds) {
        for (Long contractId : contractIds) {
            if (contractId == null) {
                continue;
            }
            try {
                leaseContractQueryService.assertCurrentUserCanReadContract(contractId);
                return true;
            } catch (ResponseStatusException exception) {
                log.debug("Tenant cannot read contract {} linked to sensitive file", contractId, exception);
            }
        }
        return false;
    }

    private boolean canReadAnyLinkedRoom(List<Long> roomIds) {
        for (Long roomId : roomIds) {
            if (roomId == null) {
                continue;
            }
            try {
                leaseContractQueryService.assertCurrentUserCanReadRoom(roomId);
                return true;
            } catch (ResponseStatusException exception) {
                log.debug("Tenant cannot read room {} linked to sensitive file", roomId, exception);
            }
        }
        return false;
    }
}
