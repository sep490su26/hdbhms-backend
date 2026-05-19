package com.sep490.hdbhms.modules.maintenance.controller;

import com.sep490.hdbhms.modules.maintenance.dto.MaintenanceTicketListResponse;
import com.sep490.hdbhms.modules.maintenance.dto.ConfirmAndReviewResponse;
import com.sep490.hdbhms.modules.maintenance.dto.MaintenanceTicketActionResponse;
import com.sep490.hdbhms.modules.maintenance.dto.MaintenanceTicketDetailResponse;
import com.sep490.hdbhms.modules.maintenance.dto.MaintenanceTicketRequests;
import com.sep490.hdbhms.modules.maintenance.dto.MaintenanceTicketResponse;
import com.sep490.hdbhms.modules.maintenance.dto.MaintenanceTicketReviewResponse;
import com.sep490.hdbhms.modules.maintenance.service.MaintenanceTicketQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/tickets")
@Tag(name = "Maintenance Tickets", description = "Maintenance ticket APIs for tenant mobile app")
public class MaintenanceTicketController {

    private final MaintenanceTicketQueryService maintenanceTicketQueryService;

    public MaintenanceTicketController(MaintenanceTicketQueryService maintenanceTicketQueryService) {
        this.maintenanceTicketQueryService = maintenanceTicketQueryService;
    }

    @PostMapping
    @Operation(
            summary = "Create a maintenance ticket",
            description = "TENANT users can create TENANT_ROOM tickets only for rooms they are actively renting.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<MaintenanceTicketResponse> createTicket(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long tenantId,
            @RequestBody MaintenanceTicketRequests.CreateTicketRequest request
    ) {
        return ResponseEntity.status(201).body(
                maintenanceTicketQueryService.createTicket(Long.parseLong(jwt.getSubject()), tenantId, request)
        );
    }

    @GetMapping
    @Operation(
            summary = "List maintenance tickets",
            description = """
                    Returns maintenance tickets in the requested tenant/property scope.
                    TENANT users only see TENANT_ROOM tickets for rooms they are actively renting.
                    OWNER and MANAGER users can see all tickets in the property scope.
                    Supports status, category/ticket_type, created date range, keyword, and pagination filters.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<MaintenanceTicketListResponse> listTickets(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long tenantId,
            @Parameter(description = "Ticket status, e.g. PENDING_ACCEPTANCE, IN_PROGRESS, COMPLETED")
            @RequestParam(required = false) String status,
            @Parameter(description = "Issue category alias, e.g. ELECTRIC, WATER, EQUIPMENT, OTHER")
            @RequestParam(required = false) String category,
            @Parameter(description = "Alias of category for backward compatibility")
            @RequestParam(name = "ticket_type", required = false) String ticketType,
            @Parameter(description = "Created-at start date in yyyy-MM-dd")
            @RequestParam(name = "from_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,
            @Parameter(description = "Created-at end date in yyyy-MM-dd")
            @RequestParam(name = "to_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,
            @Parameter(description = "Search by ticket code. Accepts SC-2026-0001 or #SC-2026-0001")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "1-based page number. Default 1")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "Page size. Default 20, max 100")
            @RequestParam(required = false) Integer size
    ) {
        MaintenanceTicketQueryService.Query query = new MaintenanceTicketQueryService.Query(
                tenantId,
                status,
                category,
                ticketType,
                fromDate,
                toDate,
                keyword,
                page,
                size
        );
        return ResponseEntity.ok(
                maintenanceTicketQueryService.listTickets(Long.parseLong(jwt.getSubject()), tenantId, query)
        );
    }

    @GetMapping("/{ticketId}")
    @Operation(
            summary = "Get maintenance ticket detail",
            description = "Returns ticket detail, attachments, repair info, review and timeline. TENANT users only see permitted room tickets.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<MaintenanceTicketDetailResponse> getTicketDetail(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long tenantId,
            @PathVariable Long ticketId
    ) {
        return ResponseEntity.ok(
                maintenanceTicketQueryService.getTicketDetail(Long.parseLong(jwt.getSubject()), tenantId, ticketId)
        );
    }

    @PostMapping("/{ticketId}/accept")
    @Operation(summary = "Accept a pending maintenance ticket", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<MaintenanceTicketActionResponse> acceptTicket(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long tenantId,
            @PathVariable Long ticketId,
            @RequestBody(required = false) MaintenanceTicketRequests.AcceptTicketRequest request
    ) {
        return ResponseEntity.ok(
                maintenanceTicketQueryService.acceptTicket(Long.parseLong(jwt.getSubject()), tenantId, ticketId, request)
        );
    }

    @PostMapping("/{ticketId}/reject")
    @Operation(summary = "Reject a maintenance ticket", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<MaintenanceTicketActionResponse> rejectTicket(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long tenantId,
            @PathVariable Long ticketId,
            @RequestBody MaintenanceTicketRequests.RejectTicketRequest request
    ) {
        return ResponseEntity.ok(
                maintenanceTicketQueryService.rejectTicket(Long.parseLong(jwt.getSubject()), tenantId, ticketId, request)
        );
    }

    @PostMapping("/{ticketId}/update-progress")
    @Operation(summary = "Update maintenance ticket progress", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<MaintenanceTicketActionResponse> updateProgress(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long tenantId,
            @PathVariable Long ticketId,
            @RequestBody(required = false) MaintenanceTicketRequests.UpdateTicketProgressRequest request
    ) {
        return ResponseEntity.ok(
                maintenanceTicketQueryService.updateProgress(Long.parseLong(jwt.getSubject()), tenantId, ticketId, request)
        );
    }

    @PostMapping("/{ticketId}/complete")
    @Operation(summary = "Mark maintenance ticket as repaired", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<MaintenanceTicketActionResponse> completeTicket(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long tenantId,
            @PathVariable Long ticketId,
            @RequestBody(required = false) MaintenanceTicketRequests.CompleteTicketRequest request
    ) {
        return ResponseEntity.ok(
                maintenanceTicketQueryService.completeTicket(Long.parseLong(jwt.getSubject()), tenantId, ticketId, request)
        );
    }

    @PostMapping("/{ticketId}/confirm")
    @Operation(summary = "Tenant confirms repaired ticket completion", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<MaintenanceTicketActionResponse> confirmTicket(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long tenantId,
            @PathVariable Long ticketId,
            @RequestBody(required = false) MaintenanceTicketRequests.ConfirmTicketRequest request
    ) {
        return ResponseEntity.ok(
                maintenanceTicketQueryService.confirmTicket(Long.parseLong(jwt.getSubject()), tenantId, ticketId, request)
        );
    }

    @PostMapping("/{ticketId}/review")
    @Operation(summary = "Tenant reviews a completed maintenance ticket", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<MaintenanceTicketReviewResponse> reviewTicket(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long tenantId,
            @PathVariable Long ticketId,
            @RequestBody MaintenanceTicketRequests.ReviewTicketRequest request
    ) {
        return ResponseEntity.ok(
                maintenanceTicketQueryService.reviewTicket(Long.parseLong(jwt.getSubject()), tenantId, ticketId, request)
        );
    }

    @PostMapping("/{ticketId}/confirm-and-review")
    @Operation(summary = "Tenant confirms completion and reviews in one transaction", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ConfirmAndReviewResponse> confirmAndReview(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long tenantId,
            @PathVariable Long ticketId,
            @RequestBody MaintenanceTicketRequests.ConfirmAndReviewRequest request
    ) {
        return ResponseEntity.ok(
                maintenanceTicketQueryService.confirmAndReview(Long.parseLong(jwt.getSubject()), tenantId, ticketId, request)
        );
    }
}
