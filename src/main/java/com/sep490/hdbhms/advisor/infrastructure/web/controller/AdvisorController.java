package com.sep490.hdbhms.advisor.infrastructure.web.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.sep490.hdbhms.advisor.application.port.out.AdvisorFile;
import com.sep490.hdbhms.advisor.application.service.AdvisorService;
import com.sep490.hdbhms.advisor.infrastructure.web.dto.request.AdvisorAskRequest;
import com.sep490.hdbhms.identityandaccess.infrastructure.config.security.UserPrincipal;
import com.sep490.hdbhms.shared.types.dto.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/advisor")
@PreAuthorize("hasRole('OWNER')")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdvisorController {
    AdvisorService advisorService;

    @GetMapping("/kpi/overview")
    public ApiResponse<JsonNode> getKpiOverview(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String period
    ) {
        return ok(advisorService.getKpiOverview(principal.getId(), period));
    }

    @GetMapping("/copilot/suggestions")
    public ApiResponse<JsonNode> getSuggestions(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String period
    ) {
        return ok(advisorService.getSuggestions(principal.getId(), period));
    }

    @GetMapping("/copilot/analysis")
    public ApiResponse<JsonNode> getAnalysis(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String period
    ) {
        return ok(advisorService.getAnalysis(principal.getId(), period));
    }

    @PostMapping("/copilot/session")
    public ApiResponse<JsonNode> createSession(@AuthenticationPrincipal UserPrincipal principal) {
        return ok(advisorService.createSession(principal.getId()));
    }

    @GetMapping("/copilot/session/{sessionId}")
    public ApiResponse<JsonNode> getSessionHistory(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String sessionId
    ) {
        return ok(advisorService.getSessionHistory(principal.getId(), sessionId));
    }

    @PostMapping("/copilot/ask")
    public ApiResponse<JsonNode> ask(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String period,
            @Valid @RequestBody AdvisorAskRequest request
    ) {
        return ApiResponse.<JsonNode>builder()
                .message("Nhận câu trả lời từ trợ lý AI thành công")
                .data(advisorService.ask(principal.getId(), period, request.toCommand()))
                .build();
    }

    @PostMapping("/copilot/report")
    public ApiResponse<JsonNode> generateReport(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String period
    ) {
        return ok(advisorService.generateReport(principal.getId(), period));
    }

    @PostMapping("/copilot/report/refresh")
    public ApiResponse<JsonNode> refreshReport(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String period
    ) {
        return ok(advisorService.refreshReport(principal.getId(), period));
    }

    @GetMapping("/copilot/report/export-docx")
    public ResponseEntity<byte[]> exportReportDocx(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String period
    ) {
        AdvisorFile file = advisorService.exportReportDocx(principal.getId(), period);
        return ResponseEntity.ok()
                .headers(downloadHeaders(file))
                .body(file.body());
    }

    private ApiResponse<JsonNode> ok(JsonNode data) {
        return ApiResponse.<JsonNode>builder().data(data).build();
    }

    private HttpHeaders downloadHeaders(AdvisorFile file) {
        HttpHeaders headers = new HttpHeaders();
        if (file.contentType() != null && !file.contentType().isBlank()) {
            headers.setContentType(MediaType.parseMediaType(file.contentType()));
        }
        if (file.contentDisposition() != null && !file.contentDisposition().isBlank()) {
            headers.add(HttpHeaders.CONTENT_DISPOSITION, file.contentDisposition());
        }
        return headers;
    }
}
