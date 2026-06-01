package com.sep490.hdbhms.maintenance.infrastructure.web.controller;

import com.sep490.hdbhms.maintenance.application.port.in.command.CreateMaintenanceTicketCommand;
import com.sep490.hdbhms.maintenance.application.port.in.query.GetListMaintenanceTicketsQuery;
import com.sep490.hdbhms.maintenance.application.port.in.query.GetMaintenanceTicketDetailsQuery;
import com.sep490.hdbhms.maintenance.application.port.in.usecase.CreateMaintenanceTicketUseCase;
import com.sep490.hdbhms.maintenance.application.port.in.usecase.GetListMaintenanceTicketsUseCase;
import com.sep490.hdbhms.maintenance.application.port.in.usecase.GetMaintenanceTicketDetailsUseCase;
import com.sep490.hdbhms.maintenance.infrastructure.web.dto.request.CreateMaintenanceTicketRequest;
import com.sep490.hdbhms.maintenance.infrastructure.web.dto.response.MaintenanceTicketDetailsResponse;
import com.sep490.hdbhms.maintenance.infrastructure.web.dto.response.MaintenanceTicketResponse;
import com.sep490.hdbhms.maintenance.infrastructure.web.mapper.MaintenanceTicketWebMapper;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import com.sep490.hdbhms.shared.dto.response.PageResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/maintenance/tickets")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MaintenanceTicketController {
    MaintenanceTicketWebMapper maintenanceTicketWebMapper;
    CreateMaintenanceTicketUseCase createMaintenanceTicketUseCase;
    GetListMaintenanceTicketsUseCase getListMaintenanceTicketsUseCase;
    GetMaintenanceTicketDetailsUseCase getMaintenanceTicketDetailsUseCase;

    @GetMapping
    public PageResponse<MaintenanceTicketResponse> getMaintenanceTickets(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return PageResponse.fromPageToPageResponse(
                getListMaintenanceTicketsUseCase.execute(
                                new GetListMaintenanceTicketsQuery(
                                        code, type, status, pageable
                                )
                        )
                        .map(maintenanceTicketWebMapper::toResponse)
        );
    }

    @GetMapping("/{id}")
    public ApiResponse<MaintenanceTicketDetailsResponse> getMaintenanceTicket(@PathVariable Long id) {
        return ApiResponse.<MaintenanceTicketDetailsResponse>builder()
                .data(
                        maintenanceTicketWebMapper.toDetailsResponse(
                                getMaintenanceTicketDetailsUseCase.execute(new GetMaintenanceTicketDetailsQuery(id))
                        )
                )
                .build();
    }

    @PostMapping
    public ApiResponse<MaintenanceTicketDetailsResponse> createMaintenanceTicket(@Valid @RequestBody CreateMaintenanceTicketRequest request) {
        return ApiResponse.<MaintenanceTicketDetailsResponse>builder()
                .data(
                        maintenanceTicketWebMapper.toDetailsResponse(
                                createMaintenanceTicketUseCase.execute(new CreateMaintenanceTicketCommand(
                                                request.getType(),
                                                request.getDescription(),
                                                request.getAttachmentIds()
                                        )
                                )
                        )
                )
                .build();
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<MaintenanceTicketDetailsResponse> approveMaintenanceTicket(@PathVariable Long id) {
        return ApiResponse.<MaintenanceTicketDetailsResponse>builder().build();
    }

    @PostMapping("/{id}/decline")
    public ApiResponse<MaintenanceTicketDetailsResponse> declineMaintenanceTicket(@PathVariable Long id) {
        return ApiResponse.<MaintenanceTicketDetailsResponse>builder().build();
    }

    @PostMapping("/{id}/complete")
    public ApiResponse<MaintenanceTicketDetailsResponse> completeMaintenanceTicket(@PathVariable Long id) {
        return ApiResponse.<MaintenanceTicketDetailsResponse>builder().build();
    }
}
