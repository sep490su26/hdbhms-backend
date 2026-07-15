package com.sep490.hdbhms.billingandpayment.infrastructure.web.controller;

import com.sep490.hdbhms.billingandpayment.application.service.TransactionHistoryService;
import com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.request.TransactionExportRequest;
import com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.response.TransactionHistoryResponse;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import com.sep490.hdbhms.shared.dto.response.PageResponse;
import com.sep490.hdbhms.shared.utils.DocumentFilenameBuilder;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/api/v1/admin/transactions")
@PreAuthorize("hasAnyRole('OWNER','MANAGER')")
public class TransactionHistoryController {
    TransactionHistoryService transactionHistoryService;

    @GetMapping
    public ApiResponse<PageResponse<TransactionHistoryResponse>> getTransactions(
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) String tenantName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return ApiResponse.<PageResponse<TransactionHistoryResponse>>builder()
                .data(transactionHistoryService.getTransactions(
                        new TransactionExportRequest(
                                roomId,
                                tenantName,
                                fromDate,
                                toDate,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null
                        ),
                        pageable
                ))
                .build();
    }

    @GetMapping("/export")
    public ResponseEntity<Resource> exportTransactions(
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) String tenantName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "ALL") String periodType,
            @RequestParam(required = false) String billingPeriod,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate issueFromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate issueToDate,
            @RequestParam(defaultValue = "excel") String format
    ) {
        var file = transactionHistoryService.exportTransactions(
                new TransactionExportRequest(
                        roomId,
                        tenantName,
                        fromDate,
                        toDate,
                        periodType,
                        billingPeriod,
                        year,
                        issueFromDate,
                        issueToDate,
                        format
                )
        );
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, DocumentFilenameBuilder.attachmentContentDisposition(file.filename()))
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(new ByteArrayResource(file.bytes()));
    }
}
