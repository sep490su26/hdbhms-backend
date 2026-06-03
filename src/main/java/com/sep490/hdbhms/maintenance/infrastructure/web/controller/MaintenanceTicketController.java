package com.sep490.hdbhms.maintenance.infrastructure.web.controller;

import com.sep490.hdbhms.maintenance.application.port.in.command.CreateMaintenanceTicketCommand;
import com.sep490.hdbhms.maintenance.application.port.in.query.GetListMaintenanceTicketsQuery;
import com.sep490.hdbhms.maintenance.application.port.in.query.GetMaintenanceTicketDetailsQuery;
import com.sep490.hdbhms.maintenance.application.port.in.usecase.CreateMaintenanceTicketUseCase;
import com.sep490.hdbhms.maintenance.application.port.in.usecase.GetListMaintenanceTicketsUseCase;
import com.sep490.hdbhms.maintenance.application.port.in.usecase.GetMaintenanceTicketDetailsUseCase;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaUserRepository;
import com.sep490.hdbhms.maintenance.application.port.out.MaintenanceCostRepository;
import com.sep490.hdbhms.maintenance.application.port.out.MaintenanceTicketRepository;
import com.sep490.hdbhms.maintenance.domain.model.MaintenanceCost;
import com.sep490.hdbhms.maintenance.domain.model.MaintenanceTicket;
import com.sep490.hdbhms.maintenance.domain.value_objects.AttachmentPhase;
import com.sep490.hdbhms.maintenance.domain.value_objects.CostType;
import com.sep490.hdbhms.maintenance.domain.value_objects.MaintenanceTicketStatus;
import com.sep490.hdbhms.maintenance.domain.value_objects.PaidBy;
import com.sep490.hdbhms.maintenance.domain.value_objects.TicketScope;
import com.sep490.hdbhms.maintenance.infrastructure.persistence.entity.MaintenanceReviewEntity;
import com.sep490.hdbhms.maintenance.infrastructure.persistence.entity.MaintenanceTicketAttachmentEntity;
import com.sep490.hdbhms.maintenance.infrastructure.persistence.entity.MaintenanceTicketEventEntity;
import com.sep490.hdbhms.maintenance.infrastructure.persistence.jpa.JpaMaintenanceReviewRepository;
import com.sep490.hdbhms.maintenance.infrastructure.persistence.jpa.JpaMaintenanceTicketAttachmentRepository;
import com.sep490.hdbhms.maintenance.infrastructure.persistence.jpa.JpaMaintenanceTicketEventRepository;
import com.sep490.hdbhms.maintenance.infrastructure.persistence.jpa.JpaMaintenanceTicketRepository;
import com.sep490.hdbhms.maintenance.infrastructure.web.dto.request.*;
import com.sep490.hdbhms.maintenance.infrastructure.web.dto.response.MaintenanceTicketDetailsResponse;
import com.sep490.hdbhms.maintenance.infrastructure.web.dto.response.MaintenanceTicketResponse;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RoomEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomRepository;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import com.sep490.hdbhms.shared.dto.response.PageResponse;
import com.sep490.hdbhms.shared.utils.AuthUtils;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/maintenance/tickets")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MaintenanceTicketController {
    CreateMaintenanceTicketUseCase createMaintenanceTicketUseCase;
    GetListMaintenanceTicketsUseCase getListMaintenanceTicketsUseCase;
    GetMaintenanceTicketDetailsUseCase getMaintenanceTicketDetailsUseCase;
    MaintenanceTicketRepository maintenanceTicketRepository;
    MaintenanceCostRepository maintenanceCostRepository;
    JpaRoomRepository jpaRoomRepository;
    JpaUserRepository jpaUserRepository;
    JpaMaintenanceTicketRepository jpaMaintenanceTicketRepository;
    JpaMaintenanceTicketEventRepository jpaMaintenanceTicketEventRepository;
    JpaMaintenanceReviewRepository jpaMaintenanceReviewRepository;
    JpaMaintenanceTicketAttachmentRepository jpaMaintenanceTicketAttachmentRepository;

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
                        .map(this::toResponse)
        );
    }

    @GetMapping("/{id}")
    public ApiResponse<MaintenanceTicketDetailsResponse> getMaintenanceTicket(@PathVariable Long id) {
        return ApiResponse.<MaintenanceTicketDetailsResponse>builder()
                .data(
                        toDetailsResponse(getMaintenanceTicketDetailsUseCase.execute(new GetMaintenanceTicketDetailsQuery(id)))
                )
                .build();
    }

    @PostMapping
    @Transactional
    public ApiResponse<MaintenanceTicketDetailsResponse> createMaintenanceTicket(@Valid @RequestBody CreateMaintenanceTicketRequest request) {
        MaintenanceTicket ticket = createMaintenanceTicketUseCase.execute(new CreateMaintenanceTicketCommand(
                request.getRoomId(),
                request.getType(),
                request.getCategory(),
                request.getTitle(),
                request.getTicketScope(),
                request.getPriority(),
                request.getDescription(),
                request.getAttachmentIds()
        ));
        recordEvent(ticket.getId(), null, ticket.getStatus(), "Khách thuê tạo phiếu sự cố từ app mobile");
        return response(ticket);
    }

    @PostMapping("/{id}/approve")
    @Transactional
    public ApiResponse<MaintenanceTicketDetailsResponse> approveMaintenanceTicket(@PathVariable Long id) {
        MaintenanceTicket ticket = findTicket(id);
        MaintenanceTicketStatus fromStatus = ticket.getStatus();
        if (fromStatus != MaintenanceTicketStatus.PENDING_ACCEPTANCE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ tiếp nhận phiếu đang chờ tiếp nhận");
        }
        MaintenanceTicket saved = maintenanceTicketRepository.save(ticket.toBuilder()
                .status(MaintenanceTicketStatus.ACCEPTED)
                .assignedToId(currentUserIdOrNull())
                .build());
        recordEvent(saved.getId(), fromStatus, saved.getStatus(), "Quản lý đã tiếp nhận phiếu sự cố");
        return response(saved);
    }

    @PostMapping("/{id}/decline")
    @Transactional
    public ApiResponse<MaintenanceTicketDetailsResponse> declineMaintenanceTicket(
            @PathVariable Long id,
            @RequestBody(required = false) RejectMaintenanceTicketRequest request
    ) {
        MaintenanceTicket ticket = findTicket(id);
        MaintenanceTicketStatus fromStatus = ticket.getStatus();
        String reason = firstNonBlank(request == null ? null : request.getReason(), "Từ chối xử lý phiếu sự cố");
        MaintenanceTicket saved = maintenanceTicketRepository.save(ticket.toBuilder()
                .status(MaintenanceTicketStatus.REJECTED)
                .rejectionReason(reason)
                .assignedToId(currentUserIdOrNull())
                .build());
        recordEvent(saved.getId(), fromStatus, saved.getStatus(), reason);
        return response(saved);
    }

    @PostMapping("/{id}/progress")
    @Transactional
    public ApiResponse<MaintenanceTicketDetailsResponse> updateProgress(
            @PathVariable Long id,
            @RequestBody(required = false) UpdateMaintenanceTicketProgressRequest request
    ) {
        MaintenanceTicket ticket = findTicket(id);
        MaintenanceTicketStatus fromStatus = ticket.getStatus();
        MaintenanceTicket saved = maintenanceTicketRepository.save(ticket.toBuilder()
                .status(MaintenanceTicketStatus.IN_PROGRESS)
                .assignedToId(ticket.getAssignedToId() == null ? currentUserIdOrNull() : ticket.getAssignedToId())
                .ticketScope(request != null && request.getTicketScope() != null ? request.getTicketScope() : ticket.getTicketScope())
                .workerName(firstNonBlank(request == null ? null : request.getWorkerName(), ticket.getWorkerName()))
                .repairItems(encodeRepairItems(
                        firstNonBlank(request == null ? null : request.getRepairItems(), readRepairItems(ticket.getRepairItems())),
                        firstNonBlank(request == null ? null : request.getRootCause(), readRootCause(ticket.getRepairItems()))
                ))
                .build());
        recordEvent(saved.getId(), fromStatus, saved.getStatus(), firstNonBlank(request == null ? null : request.getNote(), "Đang xử lý phiếu sự cố"));
        return response(saved);
    }

    @PostMapping("/{id}/complete")
    @Transactional
    public ApiResponse<MaintenanceTicketDetailsResponse> completeMaintenanceTicket(
            @PathVariable Long id,
            @RequestBody(required = false) CompleteMaintenanceTicketRequest request
    ) {
        MaintenanceTicket ticket = findTicket(id);
        MaintenanceTicketStatus fromStatus = ticket.getStatus();
        String rootCause = firstNonBlank(request == null ? null : request.getRootCause(), readRootCause(ticket.getRepairItems()));
        String repairItems = firstNonBlank(request == null ? null : request.getRepairItems(), readRepairItems(ticket.getRepairItems()));
        MaintenanceTicket saved = maintenanceTicketRepository.save(ticket.toBuilder()
                .status(MaintenanceTicketStatus.WAITING_CONFIRMATION)
                .assignedToId(ticket.getAssignedToId() == null ? currentUserIdOrNull() : ticket.getAssignedToId())
                .ticketScope(request != null && request.getTicketScope() != null ? request.getTicketScope() : ticket.getTicketScope())
                .workerName(firstNonBlank(request == null ? null : request.getWorkerName(), ticket.getWorkerName()))
                .repairItems(encodeRepairItems(repairItems, rootCause))
                .completedAt(LocalDateTime.now())
                .build());

        Long amount = request == null ? null : request.getAmount();
        if (amount != null && amount > 0) {
            String costDescription = firstNonBlank(
                    request.getCostDescription(),
                    repairItems,
                    "Chi phí xử lý sự cố"
            );
            if (!rootCause.isBlank()) {
                costDescription = costDescription + " | Nguyên nhân: " + rootCause;
            }
            maintenanceCostRepository.save(MaintenanceCost.builder()
                    .ticketId(saved.getId())
                    .costType(request.getCostType() == null ? CostType.OTHER : request.getCostType())
                    .description(costDescription)
                    .amount(amount)
                    .paidBy(request.getPaidBy() == null ? PaidBy.LANDLORD : request.getPaidBy())
                    .createdById(currentUserIdOrNull())
                    .build());
        }

        recordEvent(saved.getId(), fromStatus, saved.getStatus(), firstNonBlank(
                request == null ? null : request.getCompletionNote(),
                "Đã xử lý xong, chờ khách thuê xác nhận"
        ));
        return response(saved);
    }

    @PostMapping("/{id}/confirm")
    @Transactional
    public ApiResponse<MaintenanceTicketDetailsResponse> confirmMaintenanceTicket(@PathVariable Long id) {
        MaintenanceTicket ticket = findTicket(id);
        MaintenanceTicketStatus fromStatus = ticket.getStatus();
        if (fromStatus != MaintenanceTicketStatus.WAITING_CONFIRMATION) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ xác nhận phiếu đang chờ khách xác nhận");
        }
        MaintenanceTicket saved = maintenanceTicketRepository.save(ticket.toBuilder()
                .status(MaintenanceTicketStatus.COMPLETED)
                .build());
        recordEvent(saved.getId(), fromStatus, saved.getStatus(), "Khách thuê xác nhận sự cố đã xử lý xong");
        return response(saved);
    }

    @PostMapping("/{id}/review")
    @Transactional
    public ApiResponse<MaintenanceTicketDetailsResponse> reviewMaintenanceTicket(
            @PathVariable Long id,
            @RequestBody ReviewMaintenanceTicketRequest request
    ) {
        MaintenanceTicket ticket = findTicket(id);
        Long currentUserId = currentUserIdOrNull();
        if (currentUserId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Phiên đăng nhập không hợp lệ");
        }
        int rating = request == null || request.getRating() == null ? 5 : request.getRating();
        if (rating < 1 || rating > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đánh giá phải từ 1 đến 5 sao");
        }
        MaintenanceReviewEntity review = jpaMaintenanceReviewRepository
                .findByTicket_IdAndReviewerUser_Id(id, currentUserId)
                .orElseGet(() -> MaintenanceReviewEntity.builder()
                        .ticket(jpaMaintenanceTicketRepository.getReferenceById(id))
                        .reviewerUser(jpaUserRepository.getReferenceById(currentUserId))
                        .build());
        review.setRating(rating);
        review.setComment(request == null ? null : request.getComment());
        jpaMaintenanceReviewRepository.save(review);

        MaintenanceTicket saved = ticket;
        if (ticket.getStatus() == MaintenanceTicketStatus.WAITING_CONFIRMATION) {
            saved = maintenanceTicketRepository.save(ticket.toBuilder()
                    .status(MaintenanceTicketStatus.COMPLETED)
                    .build());
            recordEvent(saved.getId(), ticket.getStatus(), saved.getStatus(), "Khách thuê xác nhận và đánh giá phiếu sự cố");
        }
        return response(saved);
    }

    private ApiResponse<MaintenanceTicketDetailsResponse> response(MaintenanceTicket ticket) {
        return ApiResponse.<MaintenanceTicketDetailsResponse>builder()
                .data(toDetailsResponse(ticket))
                .build();
    }

    private MaintenanceTicket findTicket(Long id) {
        return maintenanceTicketRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy phiếu sự cố"));
    }

    private MaintenanceTicketResponse toResponse(MaintenanceTicket ticket) {
        RoomInfo room = findRoomInfo(ticket.getRoomId());
        CostInfo cost = summarizeCosts(ticket.getId());
        return MaintenanceTicketResponse.builder()
                .id(ticket.getId())
                .ticketCode(ticket.getTicketCode())
                .propertyId(ticket.getPropertyId())
                .roomId(ticket.getRoomId())
                .roomCode(room.roomCode())
                .roomName(room.roomName())
                .ticketScope(ticket.getTicketScope())
                .priority(ticket.getPriority())
                .category(ticket.getCategory())
                .title(ticket.getTitle())
                .description(ticket.getDescription())
                .status(ticket.getStatus())
                .workerName(ticket.getWorkerName())
                .repairItems(readRepairItems(ticket.getRepairItems()))
                .rootCause(readRootCause(ticket.getRepairItems()))
                .costAmount(cost.amount())
                .costDescription(cost.description())
                .paidBy(cost.paidBy())
                .completedAt(ticket.getCompletedAt())
                .updatedAt(ticket.getUpdatedAt())
                .createdAt(ticket.getCreatedAt())
                .build();
    }

    private MaintenanceTicketDetailsResponse toDetailsResponse(MaintenanceTicket ticket) {
        RoomInfo room = findRoomInfo(ticket.getRoomId());
        CostInfo cost = summarizeCosts(ticket.getId());
        List<MaintenanceTicketDetailsResponse.EventResponse> events = jpaMaintenanceTicketEventRepository
                .findAllByTicket_IdOrderByCreatedAtAsc(ticket.getId())
                .stream()
                .map(event -> MaintenanceTicketDetailsResponse.EventResponse.builder()
                        .id(event.getId())
                        .fromStatus(event.getFromStatus())
                        .toStatus(event.getToStatus())
                        .note(event.getNote())
                        .createdAt(event.getCreatedAt())
                        .build())
                .toList();
        List<MaintenanceTicketDetailsResponse.AttachmentResponse> attachments = jpaMaintenanceTicketAttachmentRepository
                .findAllByTicket_IdOrderBySortOrderAsc(ticket.getId())
                .stream()
                .map(this::toAttachmentResponse)
                .toList();
        return MaintenanceTicketDetailsResponse.builder()
                .id(ticket.getId())
                .ticketCode(ticket.getTicketCode())
                .propertyId(ticket.getPropertyId())
                .roomId(ticket.getRoomId())
                .roomCode(room.roomCode())
                .roomName(room.roomName())
                .ticketScope(ticket.getTicketScope())
                .priority(ticket.getPriority())
                .category(ticket.getCategory())
                .title(ticket.getTitle())
                .description(ticket.getDescription())
                .status(ticket.getStatus())
                .workerName(ticket.getWorkerName())
                .repairItems(readRepairItems(ticket.getRepairItems()))
                .rootCause(readRootCause(ticket.getRepairItems()))
                .rejectionReason(ticket.getRejectionReason())
                .costAmount(cost.amount())
                .costDescription(cost.description())
                .paidBy(cost.paidBy())
                .completedAt(ticket.getCompletedAt())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .beforeAttachments(filterAttachments(attachments, AttachmentPhase.BEFORE))
                .afterAttachments(filterAttachments(attachments, AttachmentPhase.AFTER))
                .events(events)
                .build();
    }

    private MaintenanceTicketDetailsResponse.AttachmentResponse toAttachmentResponse(MaintenanceTicketAttachmentEntity attachment) {
        Long fileId = attachment.getFile() == null ? null : attachment.getFile().getId();
        return MaintenanceTicketDetailsResponse.AttachmentResponse.builder()
                .id(attachment.getId())
                .fileId(fileId)
                .url(fileId == null ? null : "/api/v1/files/download/" + fileId)
                .mimeType(attachment.getFile() == null ? null : attachment.getFile().getMimeType())
                .name(attachment.getFile() == null ? null : attachment.getFile().getOriginalName())
                .phase(attachment.getAttachmentPhase() == null ? null : attachment.getAttachmentPhase().name())
                .sortOrder(attachment.getSortOrder())
                .build();
    }

    private List<MaintenanceTicketDetailsResponse.AttachmentResponse> filterAttachments(
            List<MaintenanceTicketDetailsResponse.AttachmentResponse> attachments,
            AttachmentPhase phase
    ) {
        return attachments.stream()
                .filter(attachment -> phase.name().equals(attachment.getPhase()))
                .toList();
    }

    private RoomInfo findRoomInfo(Long roomId) {
        if (roomId == null) {
            return new RoomInfo(null, null);
        }
        Optional<RoomEntity> room = jpaRoomRepository.findById(roomId);
        return room.map(value -> new RoomInfo(value.getRoomCode(), value.getName()))
                .orElseGet(() -> new RoomInfo(null, null));
    }

    private CostInfo summarizeCosts(Long ticketId) {
        List<MaintenanceCost> costs = maintenanceCostRepository.findAllByTicketId(ticketId);
        if (costs.isEmpty()) {
            return new CostInfo(0L, null, null);
        }
        long total = costs.stream()
                .map(MaintenanceCost::getAmount)
                .filter(amount -> amount != null)
                .mapToLong(Long::longValue)
                .sum();
        MaintenanceCost first = costs.getFirst();
        return new CostInfo(total, first.getPaidBy(), first.getDescription());
    }

    private void recordEvent(Long ticketId, MaintenanceTicketStatus fromStatus, MaintenanceTicketStatus toStatus, String note) {
        if (ticketId == null || toStatus == null) {
            return;
        }
        Long currentUserId = currentUserIdOrNull();
        jpaMaintenanceTicketEventRepository.save(MaintenanceTicketEventEntity.builder()
                .ticket(jpaMaintenanceTicketRepository.getReferenceById(ticketId))
                .fromStatus(fromStatus == null ? null : fromStatus.name())
                .toStatus(toStatus.name())
                .note(note)
                .createdBy(currentUserId == null ? null : jpaUserRepository.getReferenceById(currentUserId))
                .build());
    }

    private Long currentUserIdOrNull() {
        try {
            return AuthUtils.getCurrentAuthenticationId();
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private String encodeRepairItems(String repairItems, String rootCause) {
        String cause = firstNonBlank(rootCause);
        String items = firstNonBlank(repairItems);
        if (cause.isBlank()) {
            return items;
        }
        return "[ROOT_CAUSE]" + cause + "\n[REPAIR_ITEMS]" + items;
    }

    private String readRootCause(String storedRepairItems) {
        if (storedRepairItems == null || storedRepairItems.isBlank()) {
            return "";
        }
        for (String line : storedRepairItems.split("\\R")) {
            if (line.startsWith("[ROOT_CAUSE]")) {
                return line.substring("[ROOT_CAUSE]".length()).trim();
            }
        }
        return "";
    }

    private String readRepairItems(String storedRepairItems) {
        if (storedRepairItems == null || storedRepairItems.isBlank()) {
            return "";
        }
        int markerIndex = storedRepairItems.indexOf("[REPAIR_ITEMS]");
        if (markerIndex >= 0) {
            return storedRepairItems.substring(markerIndex + "[REPAIR_ITEMS]".length()).trim();
        }
        if (storedRepairItems.startsWith("[ROOT_CAUSE]")) {
            return "";
        }
        return storedRepairItems;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.trim().isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private record RoomInfo(String roomCode, String roomName) {
    }

    private record CostInfo(Long amount, PaidBy paidBy, String description) {
    }
}
