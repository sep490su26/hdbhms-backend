package com.sep490.hdbhms.occupancy.infrastructure.web.controller;

import com.sep490.hdbhms.file.application.port.in.query.DownloadFileQuery;
import com.sep490.hdbhms.file.application.port.in.usecase.DownloadFileUseCase;
import com.sep490.hdbhms.file.infrastructure.web.dto.response.FileDataResponse;
import com.sep490.hdbhms.identityandaccess.domain.valueObjects.Role;
import com.sep490.hdbhms.identityandaccess.infrastructure.config.security.UserPrincipal;
import com.sep490.hdbhms.occupancy.application.service.ManageContractHandoverService;
import com.sep490.hdbhms.occupancy.domain.valueObjects.HandoverType;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.ConfirmHandoverRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.HandoverMeterReadingsRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.SubmitHandoverRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.HandoverMeterReadingsResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.SubmitHandoverResponse;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import com.sep490.hdbhms.shared.utils.DocumentFilenameBuilder;
import com.sep490.hdbhms.shared.utils.DocumentFilenameBuilder.DocumentType;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/lease-contracts/{contractId}/handover")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ContractHandoverController {

    ManageContractHandoverService manageContractHandoverService;
    com.sep490.hdbhms.occupancy.application.service.HandoverDocumentService handoverDocumentService;
    DownloadFileUseCase downloadFileUseCase;
    JdbcTemplate jdbcTemplate;

    @PostMapping("/meter-readings")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ApiResponse<HandoverMeterReadingsResponse> createMeterReadings(
            @PathVariable Long contractId,
            @RequestParam(required = false, defaultValue = "MOVE_IN") HandoverType type,
            @Valid @RequestBody HandoverMeterReadingsRequest request) {
        return ApiResponse.<HandoverMeterReadingsResponse>builder()
                .data(manageContractHandoverService.createHandoverReadings(contractId, request, type))
                .build();
    }

    /**
     * Single-shot submit: uploads are done on the frontend first,
     * then this saves readings + assets + confirms the record atomically.
     */
    @PostMapping("/submit")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ApiResponse<SubmitHandoverResponse> submitHandover(
            @PathVariable Long contractId,
            @Valid @RequestBody SubmitHandoverRequest request) {
        return ApiResponse.<SubmitHandoverResponse>builder()
                .data(manageContractHandoverService.submitHandover(contractId, request))
                .build();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ApiResponse<com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.ContractHandoverDetailsResponse> getHandover(
            @PathVariable Long contractId,
            @RequestParam(required = false, defaultValue = "MOVE_IN") HandoverType type) {
        return ApiResponse.<com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.ContractHandoverDetailsResponse>builder()
                .data(manageContractHandoverService.getHandoverDetails(contractId, type))
                .build();
    }

    @PatchMapping("/confirm")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ApiResponse<Void> confirmHandover(
            @PathVariable Long contractId,
            @Valid @RequestBody ConfirmHandoverRequest request) {
        manageContractHandoverService.confirmHandover(contractId, request);
        return ApiResponse.<Void>builder()
                .message("Xác nhận bàn giao thành công")
                .build();
    }

    @GetMapping("/draft-pdf")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> getHandoverDraftPdf(
            @PathVariable Long contractId,
            @RequestParam(required = false, defaultValue = "MOVE_IN") HandoverType type) {
        assertOwnerOrAssignedManagerCanAccessContract(contractId);
        byte[] pdfBytes = handoverDocumentService.generateHandoverDraftPdf(contractId, type);
        org.springframework.core.io.Resource resource = new org.springframework.core.io.ByteArrayResource(pdfBytes);
        var filenameContext = handoverDocumentService.getFilenameContext(contractId, type);
        String filename = DocumentFilenameBuilder.build(
                filenameContext.roomCode(),
                filenameContext.tenantName(),
                DocumentType.BBBG,
                filenameContext.startDate()
        );
        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, DocumentFilenameBuilder.attachmentContentDisposition(filename))
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .body(resource);
    }

    @GetMapping("/signed-pdf")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> downloadSignedHandoverPdf(
            @PathVariable Long contractId,
            @RequestParam(required = false, defaultValue = "MOVE_IN") HandoverType type) {
        assertOwnerOrAssignedManagerCanAccessContract(contractId);
        var filenameContext = handoverDocumentService.getFilenameContext(contractId, type);
        if (filenameContext.signedFileId() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Chua co bien ban ban giao da ky.");
        }

        FileDataResponse fileData = downloadFileUseCase.execute(new DownloadFileQuery(filenameContext.signedFileId()));
        if (fileData == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay bien ban ban giao da ky.");
        }
        String contentType = fileData.contentType() == null
                ? org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE
                : fileData.contentType();
        String filename = DocumentFilenameBuilder.build(
                filenameContext.roomCode(),
                filenameContext.tenantName(),
                DocumentType.BBBG,
                filenameContext.handoverDate()
        );
        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, contentType)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, DocumentFilenameBuilder.attachmentContentDisposition(filename))
                .body(fileData.resource());
    }

    @PatchMapping("/document")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ApiResponse<Void> uploadHandoverDocument(
            @PathVariable Long contractId,
            @RequestParam(required = false, defaultValue = "MOVE_IN") HandoverType type,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        assertOwnerOrAssignedManagerCanAccessContract(contractId);
        handoverDocumentService.attachSignedDocument(contractId, type, file);
        return ApiResponse.<Void>builder()
                .message("Tải lên biên bản bàn giao thành công")
                .build();
    }
    private void assertOwnerOrAssignedManagerCanAccessContract(Long contractId) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chua dang nhap.");
        }
        if (principal.getRole() == Role.OWNER) {
            return;
        }
        if (principal.getRole() != Role.MANAGER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ban khong co quyen thao tac hop dong nay.");
        }

        Long propertyId = jdbcTemplate.query("""
                        SELECT r.property_id
                        FROM lease_contracts lc
                        JOIN rooms r ON r.room_id = lc.room_id
                        WHERE lc.lease_contract_id = ?
                          AND lc.deleted_at IS NULL
                        LIMIT 1
                        """,
                rs -> rs.next() ? rs.getLong("property_id") : null,
                contractId
        );
        if (propertyId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay hop dong thue.");
        }

        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM role_promotions
                        WHERE user_id = ?
                          AND property_id = ?
                          AND role = 'MANAGER'
                          AND status = 'ACTIVE'
                          AND deleted_at IS NULL
                        """,
                Integer.class,
                principal.getId(),
                propertyId
        );
        if (count == null || count == 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ban khong co quyen thao tac hop dong nay.");
        }
    }
}
