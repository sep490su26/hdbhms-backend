package com.sep490.hdbhms.occupancy.infrastructure.web.controller;

import com.sep490.hdbhms.occupancy.application.service.ManageContractHandoverService;
import com.sep490.hdbhms.occupancy.domain.valueObjects.HandoverType;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.ConfirmHandoverRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.HandoverMeterReadingsRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.SubmitHandoverRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.HandoverMeterReadingsResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.SubmitHandoverResponse;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/lease-contracts/{contractId}/handover")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ContractHandoverController {

    ManageContractHandoverService manageContractHandoverService;
    com.sep490.hdbhms.occupancy.application.service.HandoverDocumentService handoverDocumentService;

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
        byte[] pdfBytes = handoverDocumentService.generateHandoverDraftPdf(contractId, type);
        org.springframework.core.io.Resource resource = new org.springframework.core.io.ByteArrayResource(pdfBytes);
        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"handover-document.pdf\"")
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .body(resource);
    }

    @PatchMapping("/document")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ApiResponse<Void> uploadHandoverDocument(
            @PathVariable Long contractId,
            @RequestParam(required = false, defaultValue = "MOVE_IN") HandoverType type,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        handoverDocumentService.attachSignedDocument(contractId, type, file);
        return ApiResponse.<Void>builder()
                .message("Tải lên biên bản bàn giao thành công")
                .build();
    }
}
