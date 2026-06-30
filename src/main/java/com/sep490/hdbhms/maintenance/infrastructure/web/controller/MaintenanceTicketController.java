package com.sep490.hdbhms.maintenance.infrastructure.web.controller;

import com.sep490.hdbhms.billingandpayment.application.service.IssuedInvoiceChargeService;
import com.sep490.hdbhms.billingandpayment.application.service.ScheduledBillingChargeService;
import com.sep490.hdbhms.billingandpayment.domain.valueObjects.InvoiceLineType;
import com.sep490.hdbhms.billingandpayment.domain.valueObjects.InvoiceStatus;
import com.sep490.hdbhms.billingandpayment.domain.valueObjects.PendingBillingChargeStatus;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.InvoiceEntity;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.InvoiceLineEntity;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.PendingBillingChargeEntity;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaInvoiceLineRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaPendingBillingChargeRepository;
import com.sep490.hdbhms.file.infrastructure.persistence.entity.FileMetadataEntity;
import com.sep490.hdbhms.file.infrastructure.persistence.jpa.JpaFileMetadataRepository;
import com.sep490.hdbhms.identityandaccess.domain.valueObjects.PromotionRole;
import com.sep490.hdbhms.identityandaccess.domain.valueObjects.Role;
import com.sep490.hdbhms.identityandaccess.domain.valueObjects.RolePromotionStatus;
import com.sep490.hdbhms.identityandaccess.infrastructure.config.security.UserPrincipal;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.UserEntity;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaRolePromotionRepository;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaUserRepository;
import com.sep490.hdbhms.maintenance.application.port.out.MaintenanceCostRepository;
import com.sep490.hdbhms.maintenance.application.port.out.MaintenanceTicketRepository;
import com.sep490.hdbhms.maintenance.domain.model.MaintenanceCost;
import com.sep490.hdbhms.maintenance.domain.model.MaintenanceTicket;
import com.sep490.hdbhms.maintenance.domain.valueObjects.AttachmentPhase;
import com.sep490.hdbhms.maintenance.domain.valueObjects.CostResponsibility;
import com.sep490.hdbhms.maintenance.domain.valueObjects.CostType;
import com.sep490.hdbhms.maintenance.domain.valueObjects.MaintenanceTicketAction;
import com.sep490.hdbhms.maintenance.domain.valueObjects.MaintenanceTicketStatus;
import com.sep490.hdbhms.maintenance.domain.valueObjects.PaidBy;
import com.sep490.hdbhms.maintenance.domain.valueObjects.Priority;
import com.sep490.hdbhms.maintenance.domain.valueObjects.TicketScope;
import com.sep490.hdbhms.maintenance.infrastructure.persistence.entity.MaintenanceCostEntity;
import com.sep490.hdbhms.maintenance.infrastructure.persistence.entity.MaintenanceReviewEntity;
import com.sep490.hdbhms.maintenance.infrastructure.persistence.entity.MaintenanceTicketAttachmentEntity;
import com.sep490.hdbhms.maintenance.infrastructure.persistence.entity.MaintenanceTicketEntity;
import com.sep490.hdbhms.maintenance.infrastructure.persistence.entity.MaintenanceTicketEventEntity;
import com.sep490.hdbhms.maintenance.infrastructure.persistence.jpa.JpaMaintenanceCostRepository;
import com.sep490.hdbhms.maintenance.infrastructure.persistence.jpa.JpaMaintenanceReviewRepository;
import com.sep490.hdbhms.maintenance.infrastructure.persistence.jpa.JpaMaintenanceTicketAttachmentRepository;
import com.sep490.hdbhms.maintenance.infrastructure.persistence.jpa.JpaMaintenanceTicketEventRepository;
import com.sep490.hdbhms.maintenance.infrastructure.persistence.jpa.JpaMaintenanceTicketRepository;
import com.sep490.hdbhms.maintenance.infrastructure.persistence.mapper.MaintenanceTicketPersistenceMapper;
import com.sep490.hdbhms.maintenance.infrastructure.web.dto.request.AttachMaintenanceTicketFilesRequest;
import com.sep490.hdbhms.maintenance.infrastructure.web.dto.request.CompleteMaintenanceTicketRequest;
import com.sep490.hdbhms.maintenance.infrastructure.web.dto.request.CreateMaintenanceTicketRequest;
import com.sep490.hdbhms.maintenance.infrastructure.web.dto.request.RejectMaintenanceTicketRequest;
import com.sep490.hdbhms.maintenance.infrastructure.web.dto.request.ReportMaintenanceNotFixedRequest;
import com.sep490.hdbhms.maintenance.infrastructure.web.dto.request.ReviewMaintenanceTicketRequest;
import com.sep490.hdbhms.maintenance.infrastructure.web.dto.request.UpdateMaintenanceTicketProgressRequest;
import com.sep490.hdbhms.maintenance.infrastructure.web.dto.response.MaintenanceTicketDetailsResponse;
import com.sep490.hdbhms.maintenance.infrastructure.web.dto.response.MaintenanceTicketResponse;
import com.sep490.hdbhms.maintenance.infrastructure.web.dto.response.InternalMaintenanceCostResponse;
import com.sep490.hdbhms.occupancy.application.service.LeaseContractQueryService;
import com.sep490.hdbhms.occupancy.domain.valueObjects.LeaseStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.PropertyEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeaseContractEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RoomEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaLeaseContractRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaPropertyRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomRepository;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import com.sep490.hdbhms.shared.dto.response.PageResponse;
import com.sep490.hdbhms.shared.utils.AuthUtils;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/maintenance/tickets")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MaintenanceTicketController {
    private static final int MAX_BEFORE_ATTACHMENTS = 3;
    private static final int MIN_DESCRIPTION_LENGTH = 10;
    private static final Set<String> ALLOWED_IMAGE_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp"
    );
    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> ACTIVE_TENANT_CONTRACT_STATUSES = Set.of(
            "ACTIVE",
            "EXPIRING_SOON",
            "TERMINATION_PENDING"
    );
    private static final List<LeaseStatus> INVOICEABLE_CONTRACT_STATUSES = List.of(
            LeaseStatus.ACTIVE,
            LeaseStatus.EXPIRING_SOON,
            LeaseStatus.TERMINATION_PENDING
    );

    MaintenanceTicketRepository maintenanceTicketRepository;
    MaintenanceCostRepository maintenanceCostRepository;
    JpaRolePromotionRepository jpaRolePromotionRepository;
    JpaRoomRepository jpaRoomRepository;
    JpaPropertyRepository jpaPropertyRepository;
    JpaLeaseContractRepository jpaLeaseContractRepository;
    JpaUserRepository jpaUserRepository;
    JpaInvoiceLineRepository jpaInvoiceLineRepository;
    JpaFileMetadataRepository jpaFileMetadataRepository;
    JpaMaintenanceTicketRepository jpaMaintenanceTicketRepository;
    JpaMaintenanceTicketEventRepository jpaMaintenanceTicketEventRepository;
    JpaMaintenanceReviewRepository jpaMaintenanceReviewRepository;
    JpaMaintenanceTicketAttachmentRepository jpaMaintenanceTicketAttachmentRepository;
    JpaMaintenanceCostRepository jpaMaintenanceCostRepository;
    JpaPendingBillingChargeRepository jpaPendingBillingChargeRepository;
    MaintenanceTicketPersistenceMapper maintenanceTicketPersistenceMapper;
    LeaseContractQueryService leaseContractQueryService;
    IssuedInvoiceChargeService issuedInvoiceChargeService;
    ScheduledBillingChargeService scheduledBillingChargeService;

    @GetMapping
    @Transactional(readOnly = true)
    public PageResponse<MaintenanceTicketResponse> getMaintenanceTickets(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) Long propertyId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String scope,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Role role = requireRole();
        if (role == Role.TENANT) {
            return searchTicketsForTenant(code, status, roomId, category, severity, priority, scope, fromDate, toDate, pageable);
        }
        assertManagerOrOwner(role);
        List<Long> restrictedPropertyIds = restrictedPropertyIdsForCurrentManager(role);
        if (restrictedPropertyIds != null && restrictedPropertyIds.isEmpty()) {
            return emptyTicketPage(pageable);
        }
        if (propertyId != null && restrictedPropertyIds != null && !restrictedPropertyIds.contains(propertyId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền xem phiếu sự cố của cơ sở này.");
        }
        return searchTickets(code, status, roomId, propertyId, firstNonBlank(category, type), severity, priority, scope, fromDate, toDate, pageable, null, restrictedPropertyIds);
    }

    @GetMapping("/my")
    @Transactional(readOnly = true)
    public PageResponse<MaintenanceTicketResponse> getMyMaintenanceTickets(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String scope,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        if (requireRole() != Role.TENANT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only tenants can use this endpoint.");
        }
        return searchTicketsForTenant(code, status, roomId, category, severity, priority, scope, fromDate, toDate, pageable);
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ApiResponse<MaintenanceTicketDetailsResponse> getMaintenanceTicket(@PathVariable Long id) {
        MaintenanceTicket ticket = findTicket(id);
        assertCanRead(ticket);
        return response(ticket);
    }

    @PostMapping
    @Transactional
    public ApiResponse<MaintenanceTicketDetailsResponse> createMaintenanceTicket(
            @Valid @RequestBody CreateMaintenanceTicketRequest request
    ) {
        Role role = requireRole();
        if (role == Role.TENANT) {
            return response(createTenantRoomTicket(request));
        }
        assertManagerOrOwner(role);
        return response(createManagementTicket(request));
    }

    @GetMapping("/internal-costs")
    @Transactional(readOnly = true)
    public List<InternalMaintenanceCostResponse> getInternalMaintenanceCosts() {
        Role role = requireRole();
        if (role != Role.OWNER && role != Role.MANAGER && role != Role.ACCOUNTANT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền xem báo cáo chi phí nội bộ.");
        }
        List<Long> restrictedPropertyIds = role == Role.MANAGER
                ? restrictedPropertyIdsForCurrentManager(role)
                : null;
        return jpaMaintenanceCostRepository.findAllByPaidByOrderByCreatedAtDesc(PaidBy.LANDLORD)
                .stream()
                .filter(cost -> cost.getTicket() != null
                        && cost.getTicket().getTicketScope() == TicketScope.PROPERTY_OPERATION)
                .filter(cost -> restrictedPropertyIds == null
                        || restrictedPropertyIds.contains(cost.getTicket().getProperty().getId()))
                .map(this::toInternalCostResponse)
                .toList();
    }

    @PostMapping("/internal")
    @Transactional
    public ApiResponse<MaintenanceTicketDetailsResponse> createInternalMaintenanceTicket(
            @Valid @RequestBody CreateMaintenanceTicketRequest request
    ) {
        assertManagerOrOwner(requireRole());
        validateCreatePayload(request);

        Long propertyId = request.getPropertyId();
        Long roomId = request.getRoomId();
        if (roomId != null) {
            RoomEntity room = jpaRoomRepository.findById(roomId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không tìm thấy phòng."));
            propertyId = room.getProperty().getId();
        }
        if (propertyId == null || !jpaPropertyRepository.existsById(propertyId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn cơ sở hợp lệ.");
        }
        assertManagerCanAccessProperty(propertyId);

        List<Long> attachmentIds = attachmentIdsPreservingOrder(request.getAttachmentIds());
        validateImageFileIds(attachmentIds);
        if (attachmentIds.size() > MAX_BEFORE_ATTACHMENTS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ được upload tối đa 3 ảnh hiện trạng.");
        }

        MaintenanceTicket ticket = saveNewTicket(
                propertyId,
                roomId,
                null,
                TicketScope.PROPERTY_OPERATION,
                request
        );
        attachFiles(ticket, attachmentIds, AttachmentPhase.BEFORE, "Ảnh hiện trạng bảo trì nội bộ");
        recordEvent(ticket.getId(), null, ticket.getStatus(), MaintenanceTicketAction.CREATE,
                roomId == null
                        ? "Tạo phiếu bảo trì nội bộ khu vực chung"
                        : "Tạo phiếu bảo trì nội bộ cho phòng");
        return response(findTicket(ticket.getId()));
    }

    @PostMapping("/{id}/approve")
    @Transactional
    public ApiResponse<MaintenanceTicketDetailsResponse> approveMaintenanceTicket(@PathVariable Long id) {
        assertManagerOrOwner(requireRole());
        MaintenanceTicket ticket = findTicket(id);
        assertManagerCanAccessTicket(ticket);
        requireStatus(ticket, MaintenanceTicketStatus.PENDING_ACCEPTANCE, "Chỉ tiếp nhận phiếu đang chờ tiếp nhận.");
        MaintenanceTicket saved = maintenanceTicketRepository.save(ticket.toBuilder()
                .status(MaintenanceTicketStatus.ACCEPTED)
                .assignedToId(currentUserId())
                .build());
        recordEvent(saved.getId(), ticket.getStatus(), saved.getStatus(), MaintenanceTicketAction.ACCEPT,
                "Quản lý đã tiếp nhận phiếu sự cố");
        return response(saved);
    }

    @PostMapping("/{id}/decline")
    @Transactional
    public ApiResponse<MaintenanceTicketDetailsResponse> declineMaintenanceTicket(
            @PathVariable Long id,
            @RequestBody(required = false) RejectMaintenanceTicketRequest request
    ) {
        assertManagerOrOwner(requireRole());
        MaintenanceTicket ticket = findTicket(id);
        assertManagerCanAccessTicket(ticket);
        requireStatus(ticket, MaintenanceTicketStatus.PENDING_ACCEPTANCE, "Chỉ từ chối phiếu đang chờ tiếp nhận.");
        String reason = firstNonBlank(request == null ? null : request.getReason());
        if (reason.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng nhập lý do từ chối.");
        }
        MaintenanceTicket saved = maintenanceTicketRepository.save(ticket.toBuilder()
                .status(MaintenanceTicketStatus.REJECTED)
                .rejectionReason(reason)
                .assignedToId(currentUserId())
                .build());
        recordEvent(saved.getId(), ticket.getStatus(), saved.getStatus(), MaintenanceTicketAction.REJECT, reason);
        return response(saved);
    }

    @PostMapping("/{id}/progress")
    @Transactional
    public ApiResponse<MaintenanceTicketDetailsResponse> updateProgress(
            @PathVariable Long id,
            @RequestBody(required = false) UpdateMaintenanceTicketProgressRequest request
    ) {
        assertManagerOrOwner(requireRole());
        MaintenanceTicket ticket = findTicket(id);
        assertManagerCanAccessTicket(ticket);
        if (ticket.getStatus() != MaintenanceTicketStatus.ACCEPTED
                && ticket.getStatus() != MaintenanceTicketStatus.IN_PROGRESS) {
            throw invalidTransition(ticket.getStatus(), MaintenanceTicketStatus.IN_PROGRESS);
        }
        MaintenanceTicketStatus toStatus = ticket.getStatus() == MaintenanceTicketStatus.ACCEPTED
                ? MaintenanceTicketStatus.IN_PROGRESS
                : MaintenanceTicketStatus.IN_PROGRESS;
        MaintenanceTicket saved = maintenanceTicketRepository.save(ticket.toBuilder()
                .status(toStatus)
                .assignedToId(ticket.getAssignedToId() == null ? currentUserId() : ticket.getAssignedToId())
                .workerName(resolveRepairmanName(request, ticket.getWorkerName()))
                .repairmanPhone(firstNonBlank(request == null ? null : request.getRepairmanPhone(), ticket.getRepairmanPhone()))
                .repairItems(encodeRepairItems(
                        firstNonBlank(request == null ? null : request.getRepairItems(), readRepairItems(ticket.getRepairItems())),
                        firstNonBlank(request == null ? null : request.getRootCause(), readRootCause(ticket.getRepairItems()))
                ))
                .build());
        MaintenanceTicketAction action = ticket.getStatus() == MaintenanceTicketStatus.ACCEPTED
                ? MaintenanceTicketAction.START_PROGRESS
                : MaintenanceTicketAction.UPDATE_REPAIR_INFO;
        recordEvent(saved.getId(), ticket.getStatus(), saved.getStatus(), action,
                firstNonBlank(request == null ? null : request.getNote(), "Đang xử lý phiếu sự cố"));
        return response(saved);
    }

    @PatchMapping("/{id}/repair-info")
    @Transactional
    public ApiResponse<MaintenanceTicketDetailsResponse> updateRepairInfo(
            @PathVariable Long id,
            @RequestBody(required = false) CompleteMaintenanceTicketRequest request
    ) {
        Role role = requireRole();
        assertManagerOrOwner(role);
        MaintenanceTicket ticket = findTicket(id);
        assertManagerCanAccessTicket(ticket);
        if (ticket.getStatus() == MaintenanceTicketStatus.COMPLETED && role != Role.OWNER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ chủ trọ được sửa chi phí của ticket đã hoàn tất.");
        }
        if (ticket.getStatus() != MaintenanceTicketStatus.ACCEPTED
                && ticket.getStatus() != MaintenanceTicketStatus.IN_PROGRESS
                && ticket.getStatus() != MaintenanceTicketStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể cập nhật thông tin xử lý ở trạng thái này.");
        }
        MaintenanceTicketStatus fromStatus = ticket.getStatus();
        MaintenanceTicketStatus toStatus = fromStatus == MaintenanceTicketStatus.ACCEPTED
                ? MaintenanceTicketStatus.IN_PROGRESS
                : fromStatus;
        MaintenanceTicket saved = saveRepairInformation(ticket, request, toStatus);
        saveCost(saved.getId(), request);
        recordEvent(saved.getId(), fromStatus, saved.getStatus(), MaintenanceTicketAction.UPDATE_REPAIR_INFO,
                firstNonBlank(request == null ? null : request.getCompletionNote(), "Cập nhật thông tin xử lý"));
        return response(saved);
    }

    @PostMapping("/{id}/complete")
    @Transactional
    public ApiResponse<MaintenanceTicketDetailsResponse> completeMaintenanceTicket(
            @PathVariable Long id,
            @RequestBody(required = false) CompleteMaintenanceTicketRequest request
    ) {
        assertManagerOrOwner(requireRole());
        MaintenanceTicket ticket = findTicket(id);
        assertManagerCanAccessTicket(ticket);
        requireStatus(ticket, MaintenanceTicketStatus.IN_PROGRESS, "Chỉ hoàn tất khi phiếu đang xử lý.");
        Long amount = request == null ? null : firstNonNull(request.getActualCost(), request.getAmount());
        if (amount != null && amount < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chi phí thực tế không được âm.");
        }
        MaintenanceTicket saved = saveRepairInformation(ticket, request, MaintenanceTicketStatus.COMPLETED);
        saveCost(saved.getId(), request);
        if (shouldChargeTenant(saved, request)) {
            collectMaintenanceCompensation(saved, request);
        }
        if (request != null && request.getAttachmentIds() != null && !request.getAttachmentIds().isEmpty()) {
            attachFiles(saved, request.getAttachmentIds(), AttachmentPhase.AFTER, "Upload ảnh sau sửa");
        }
        recordEvent(saved.getId(), ticket.getStatus(), saved.getStatus(), MaintenanceTicketAction.CONFIRM_COMPLETED,
                firstNonBlank(request == null ? null : request.getCompletionNote(),
                        "Đã hoàn tất xử lý phiếu sự cố"));
        return response(saved);
    }

    @PostMapping("/{id}/confirm")
    @Transactional
    public ApiResponse<MaintenanceTicketDetailsResponse> confirmMaintenanceTicket(@PathVariable Long id) {
        MaintenanceTicket ticket = findTicket(id);
        requireStatus(ticket, MaintenanceTicketStatus.WAITING_CONFIRMATION, "Chỉ xác nhận phiếu đang chờ xác nhận.");
        if (ticket.getTicketScope() == TicketScope.COMMON_AREA || ticket.getTicketScope() == TicketScope.PROPERTY_OPERATION) {
            assertManagerOrOwner(requireRole());
            assertManagerCanAccessTicket(ticket);
        } else {
            assertTenantCanActOnRoomTicket(ticket);
        }
        MaintenanceTicket saved = maintenanceTicketRepository.save(ticket.toBuilder()
                .status(MaintenanceTicketStatus.COMPLETED)
                .completedAt(ticket.getCompletedAt() == null ? LocalDateTime.now() : ticket.getCompletedAt())
                .build());
        recordEvent(saved.getId(), ticket.getStatus(), saved.getStatus(), MaintenanceTicketAction.CONFIRM_COMPLETED,
                "Xác nhận sự cố đã xử lý xong");
        return response(saved);
    }

    @PostMapping("/{id}/report-not-fixed")
    @Transactional
    public ApiResponse<MaintenanceTicketDetailsResponse> reportNotFixed(
            @PathVariable Long id,
            @RequestBody(required = false) ReportMaintenanceNotFixedRequest request
    ) {
        MaintenanceTicket ticket = findTicket(id);
        assertTenantCanActOnRoomTicket(ticket);
        requireStatus(ticket, MaintenanceTicketStatus.WAITING_CONFIRMATION,
                "Chỉ báo chưa sửa xong khi phiếu đang chờ xác nhận.");
        String note = firstNonBlank(request == null ? null : request.getNote(), "Khách thuê báo sự cố chưa được sửa xong");
        MaintenanceTicket saved = maintenanceTicketRepository.save(ticket.toBuilder()
                .status(MaintenanceTicketStatus.IN_PROGRESS)
                .build());
        recordEvent(saved.getId(), ticket.getStatus(), saved.getStatus(), MaintenanceTicketAction.REPORT_NOT_FIXED, note);
        return response(saved);
    }

    @PostMapping("/{id}/review")
    @Transactional
    public ApiResponse<MaintenanceTicketDetailsResponse> reviewMaintenanceTicket(
            @PathVariable Long id,
            @RequestBody ReviewMaintenanceTicketRequest request
    ) {
        MaintenanceTicket ticket = findTicket(id);
        assertTenantCanActOnRoomTicket(ticket);
        requireStatus(ticket, MaintenanceTicketStatus.COMPLETED, "Chỉ đánh giá phiếu đã hoàn tất.");
        Long currentUserId = currentUserId();
        int rating = request == null || request.getRating() == null ? 0 : request.getRating();
        if (rating < 1 || rating > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đánh giá phải từ 1 đến 5 sao.");
        }
        if (jpaMaintenanceReviewRepository.findByTicket_IdAndReviewerUser_Id(id, currentUserId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bạn đã đánh giá phiếu này.");
        }
        jpaMaintenanceReviewRepository.save(MaintenanceReviewEntity.builder()
                .ticket(jpaMaintenanceTicketRepository.getReferenceById(id))
                .reviewerUser(jpaUserRepository.getReferenceById(currentUserId))
                .rating(rating)
                .comment(request == null ? null : request.getComment())
                .build());
        recordEvent(ticket.getId(), ticket.getStatus(), ticket.getStatus(), MaintenanceTicketAction.REVIEW,
                "Khách thuê đã đánh giá phiếu sự cố");
        return response(findTicket(id));
    }

    @PostMapping("/{id}/attachments")
    @Transactional
    public ApiResponse<MaintenanceTicketDetailsResponse> attachMaintenanceFiles(
            @PathVariable Long id,
            @RequestBody AttachMaintenanceTicketFilesRequest request
    ) {
        MaintenanceTicket ticket = findTicket(id);
        AttachmentPhase phase = request == null || request.getPhase() == null ? AttachmentPhase.AFTER : request.getPhase();
        List<Long> fileIds = request == null || request.getFileIds() == null ? List.of() : request.getFileIds();
        if (fileIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn ảnh cần upload.");
        }
        if (requireRole() == Role.TENANT) {
            assertTenantCanActOnRoomTicket(ticket);
            if (phase != AttachmentPhase.BEFORE) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Khách thuê chỉ được upload ảnh trước sửa.");
            }
            requireStatus(ticket, MaintenanceTicketStatus.PENDING_ACCEPTANCE,
                    "Chỉ bổ sung ảnh trước sửa khi phiếu đang chờ tiếp nhận.");
        } else {
            assertManagerOrOwner(requireRole());
            assertManagerCanAccessTicket(ticket);
            if (phase == AttachmentPhase.BEFORE && ticket.getStatus() != MaintenanceTicketStatus.PENDING_ACCEPTANCE) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ bổ sung ảnh trước sửa khi phiếu đang chờ tiếp nhận.");
            }
        }
        attachFiles(ticket, fileIds, phase, firstNonBlank(request == null ? null : request.getNote(), "Upload ảnh cho phiếu sự cố"));
        recordEvent(ticket.getId(), ticket.getStatus(), ticket.getStatus(), MaintenanceTicketAction.ATTACH_FILE,
                firstNonBlank(request == null ? null : request.getNote(), "Upload ảnh cho phiếu sự cố"));
        return response(findTicket(id));
    }

    private PageResponse<MaintenanceTicketResponse> searchTicketsForTenant(
            String code,
            String status,
            Long roomId,
            String category,
            String severity,
            String priority,
            String scope,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable
    ) {
        List<LeaseContractQueryService.ActiveRoomItem> activeRooms = activeTenantRooms();
        List<Long> activeRoomIds = activeRooms.stream()
                .map(LeaseContractQueryService.ActiveRoomItem::roomId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (activeRoomIds.isEmpty()) {
            return PageResponse.<MaintenanceTicketResponse>builder()
                    .data(List.of())
                    .pageSize(pageable.getPageSize())
                    .currentPage(pageable.getPageNumber() + 1)
                    .totalPages(0)
                    .totalElements(0)
                    .build();
        }
        if (roomId != null && !activeRoomIds.contains(roomId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền xem ticket của phòng này.");
        }
        List<Long> restrictedRoomIds = roomId == null ? activeRoomIds : List.of(roomId);
        return searchTickets(code, status, null, null, category, severity, priority,
                firstNonBlank(scope, "ROOM"), fromDate, toDate, pageable, restrictedRoomIds, null);
    }

    private PageResponse<MaintenanceTicketResponse> searchTickets(
            String code,
            String status,
            Long roomId,
            Long propertyId,
            String category,
            String severity,
            String priority,
            String scope,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable,
            List<Long> restrictedRoomIds,
            List<Long> restrictedPropertyIds
    ) {
        Specification<MaintenanceTicketEntity> spec = Specification.where(null);
        if (!firstNonBlank(code).isBlank()) {
            String keyword = firstNonBlank(code).replace("#", "").toLowerCase(Locale.ROOT);
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("ticketCode")), "%" + keyword + "%"));
        }
        MaintenanceTicketStatus normalizedStatus = parseStatus(status);
        if (normalizedStatus != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), normalizedStatus));
        }
        if (roomId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("room").get("id"), roomId));
        }
        if (propertyId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("property").get("id"), propertyId));
        }
        if (restrictedPropertyIds != null) {
            spec = spec.and((root, query, cb) -> root.get("property").get("id").in(restrictedPropertyIds));
        }
        if (restrictedRoomIds != null) {
            spec = spec.and((root, query, cb) -> root.get("room").get("id").in(restrictedRoomIds));
        }
        String normalizedCategory = firstNonBlank(category).isBlank() ? "" : normalizeCategory(category);
        if (!normalizedCategory.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(cb.upper(root.get("category")), normalizedCategory));
        }
        Priority normalizedPriority = parsePriority(firstNonBlank(severity, priority));
        if (normalizedPriority != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("priority"), normalizedPriority));
        }
        TicketScope normalizedScope = parseScope(scope);
        if (normalizedScope != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("ticketScope"), normalizedScope));
        }
        if (fromDate != null) {
            LocalDateTime from = fromDate.atStartOfDay();
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from));
        }
        if (toDate != null) {
            LocalDateTime to = toDate.plusDays(1).atStartOfDay().minusNanos(1);
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), to));
        }
        Page<MaintenanceTicketResponse> page = jpaMaintenanceTicketRepository.findAll(spec, pageable)
                .map(maintenanceTicketPersistenceMapper::toDomain)
                .map(this::toResponse);
        return PageResponse.fromPageToPageResponse(page);
    }

    private MaintenanceTicket createTenantRoomTicket(CreateMaintenanceTicketRequest request) {
        validateCreatePayload(request);
        if (request.getRoomId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không xác định được phòng đang thuê để tạo phiếu sự cố.");
        }
        TicketScope requestedScope = firstNonNull(request.getTicketScope(), request.getScope());
        if (requestedScope != null && requestedScope != TicketScope.TENANT_ROOM) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Khách thuê chỉ được tạo phiếu sự cố phòng đang thuê.");
        }
        LeaseContractQueryService.ActiveRoomItem activeRoom = activeTenantRooms().stream()
                .filter(room -> Objects.equals(room.roomId(), request.getRoomId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Bạn không có quyền tạo phiếu sự cố cho phòng này."));
        RoomEntity room = jpaRoomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không tìm thấy phòng."));
        List<Long> attachmentIds = attachmentIdsPreservingOrder(request.getAttachmentIds());
        validateImageFileIds(attachmentIds);
        if (attachmentIds.size() > MAX_BEFORE_ATTACHMENTS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ được upload tối đa 3 ảnh trước sửa.");
        }
        MaintenanceTicket ticket = saveNewTicket(
                room.getProperty().getId(),
                room.getId(),
                activeRoom.contractId(),
                TicketScope.TENANT_ROOM,
                request
        );
        attachFiles(ticket, attachmentIds, AttachmentPhase.BEFORE, "Khách thuê upload ảnh trước sửa");
        recordEvent(ticket.getId(), null, ticket.getStatus(), MaintenanceTicketAction.CREATE,
                "Khách thuê tạo phiếu sự cố từ app mobile");
        return findTicket(ticket.getId());
    }

    private MaintenanceTicket createManagementTicket(CreateMaintenanceTicketRequest request) {
        validateCreatePayload(request);
        Long propertyId = request.getPropertyId();
        Long roomId = request.getRoomId();
        Long contractId = null;
        if (roomId != null) {
            RoomEntity room = jpaRoomRepository.findById(roomId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không tìm thấy phòng."));
            propertyId = room.getProperty().getId();
        }
        if (propertyId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn cơ sở/property cho sự cố khu vực chung.");
        }
        if (!jpaPropertyRepository.existsById(propertyId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không tìm thấy cơ sở/property.");
        }
        assertManagerCanAccessProperty(propertyId);
        TicketScope scope = firstNonNull(request.getTicketScope(), request.getScope());
        if (scope == null) {
            scope = roomId == null ? TicketScope.COMMON_AREA : TicketScope.TENANT_ROOM;
        }
        if (roomId == null && scope == TicketScope.TENANT_ROOM) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ticket phòng cần có roomId.");
        }
        List<Long> attachmentIds = attachmentIdsPreservingOrder(request.getAttachmentIds());
        validateImageFileIds(attachmentIds);
        if (attachmentIds.size() > MAX_BEFORE_ATTACHMENTS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ được upload tối đa 3 ảnh trước sửa.");
        }
        MaintenanceTicket ticket = saveNewTicket(propertyId, roomId, contractId, scope, request);
        attachFiles(ticket, attachmentIds, AttachmentPhase.BEFORE, "Quản lý upload ảnh trước sửa");
        recordEvent(ticket.getId(), null, ticket.getStatus(), MaintenanceTicketAction.CREATE,
                scope == TicketScope.COMMON_AREA ? "Quản lý tạo phiếu sự cố khu vực chung" : "Quản lý tạo phiếu sự cố");
        return findTicket(ticket.getId());
    }

    private MaintenanceTicket saveNewTicket(
            Long propertyId,
            Long roomId,
            Long contractId,
            TicketScope scope,
            CreateMaintenanceTicketRequest request
    ) {
        String category = normalizeCategory(firstNonBlank(request.getCategory(), request.getType(), "OTHER"));
        Priority priority = firstNonNull(request.getSeverity(), request.getPriority(), Priority.MEDIUM);
        MaintenanceTicket ticket = MaintenanceTicket.builder()
                .ticketCode(String.format("#SC-TMP-%d-%d", currentUserId(), System.nanoTime()))
                .propertyId(propertyId)
                .roomId(roomId)
                .contractId(contractId)
                .createdById(currentUserId())
                .ticketScope(scope)
                .priority(priority)
                .category(category)
                .title(firstNonBlank(request.getTitle(), category))
                .description(request.getDescription().trim())
                .status(MaintenanceTicketStatus.PENDING_ACCEPTANCE)
                .build();
        ticket = maintenanceTicketRepository.save(ticket);
        ticket.setTicketCode(String.format("#SC-%04d", ticket.getId()));
        return maintenanceTicketRepository.save(ticket);
    }

    private MaintenanceTicket saveRepairInformation(
            MaintenanceTicket ticket,
            CompleteMaintenanceTicketRequest request,
            MaintenanceTicketStatus targetStatus
    ) {
        String rootCause = firstNonBlank(request == null ? null : request.getRootCause(), readRootCause(ticket.getRepairItems()));
        String repairItems = firstNonBlank(request == null ? null : request.getRepairItems(), readRepairItems(ticket.getRepairItems()));
        String repairmanName = firstNonBlank(
                request == null ? null : request.getRepairmanName(),
                request == null ? null : request.getWorkerName(),
                ticket.getWorkerName()
        );
        return maintenanceTicketRepository.save(ticket.toBuilder()
                .status(targetStatus)
                .assignedToId(ticket.getAssignedToId() == null ? currentUserId() : ticket.getAssignedToId())
                .workerName(repairmanName)
                .repairmanPhone(firstNonBlank(request == null ? null : request.getRepairmanPhone(), ticket.getRepairmanPhone()))
                .repairItems(encodeRepairItems(repairItems, rootCause))
                .completedAt((targetStatus == MaintenanceTicketStatus.WAITING_CONFIRMATION
                        || targetStatus == MaintenanceTicketStatus.COMPLETED) && ticket.getCompletedAt() == null
                        ? LocalDateTime.now()
                        : ticket.getCompletedAt())
                .build());
    }

    private void saveCost(Long ticketId, CompleteMaintenanceTicketRequest request) {
        if (request == null) {
            return;
        }
        Long amount = firstNonNull(request.getActualCost(), request.getAmount());
        if (amount == null) {
            return;
        }
        if (amount < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chi phí thực tế không được âm.");
        }
        String description = firstNonBlank(
                request.getCostDescription(),
                request.getRepairItems(),
                "Chi phí xử lý sự cố"
        );
        MaintenanceCostEntity cost = jpaMaintenanceCostRepository.findAllByTicket_IdOrderByCreatedAtAsc(ticketId)
                .stream()
                .findFirst()
                .orElseGet(() -> MaintenanceCostEntity.builder()
                        .ticket(jpaMaintenanceTicketRepository.getReferenceById(ticketId))
                        .createdBy(jpaUserRepository.getReferenceById(currentUserId()))
                        .build());
        cost.setCostType(request.getCostType() == null ? CostType.OTHER : request.getCostType());
        cost.setDescription(description);
        cost.setAmount(amount);
        boolean internalTicket = jpaMaintenanceTicketRepository.findById(ticketId)
                .map(ticket -> ticket.getTicketScope() == TicketScope.PROPERTY_OPERATION)
                .orElse(false);
        CostResponsibility responsibility = internalTicket
                ? CostResponsibility.OWNER
                : Boolean.TRUE.equals(request.getChargeToTenant())
                ? CostResponsibility.TENANT
                : request.getCostResponsibility() == null
                ? mapPaidByToResponsibility(request.getPaidBy())
                : request.getCostResponsibility();
        cost.setCostResponsibility(responsibility);
        cost.setPaidBy(mapResponsibilityToPaidBy(responsibility));
        jpaMaintenanceCostRepository.save(cost);
    }

    private void saveInternalCost(Long ticketId, CreateMaintenanceTicketRequest request) {
        MaintenanceCostEntity cost = MaintenanceCostEntity.builder()
                .ticket(jpaMaintenanceTicketRepository.getReferenceById(ticketId))
                .costType(request.getCostType() == null ? CostType.COMMON_OPERATING : request.getCostType())
                .description(firstNonBlank(request.getAccountingNote(), request.getDescription(), "Chi phí bảo trì nội bộ"))
                .amount(request.getActualCost())
                .paidBy(PaidBy.LANDLORD)
                .costResponsibility(CostResponsibility.OWNER)
                .createdBy(jpaUserRepository.getReferenceById(currentUserId()))
                .build();
        if (request.getReceiptFileId() != null) {
            FileMetadataEntity receipt = jpaFileMetadataRepository.findById(request.getReceiptFileId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không tìm thấy chứng từ chi phí."));
            cost.setReceiptFile(receipt);
        }
        jpaMaintenanceCostRepository.save(cost);
    }

    private boolean shouldChargeTenant(MaintenanceTicket ticket, CompleteMaintenanceTicketRequest request) {
        if (request == null) {
            return false;
        }
        if (ticket.getTicketScope() == TicketScope.PROPERTY_OPERATION) {
            return false;
        }
        if (Boolean.TRUE.equals(request.getChargeToTenant())) {
            return true;
        }
        return request.getPaidBy() == PaidBy.TENANT
                || request.getCostResponsibility() == CostResponsibility.TENANT;
    }

    @PostMapping("/{id}/invoice/issue")
    @Transactional
    public ApiResponse<MaintenanceTicketDetailsResponse> issueMaintenanceInvoice(@PathVariable Long id) {
        assertManagerOrOwner(requireRole());
        MaintenanceTicket ticket = findTicket(id);
        assertManagerCanAccessTicket(ticket);
        BillingInfo billing = summarizeBilling(ticket.getId(), summarizeCosts(ticket.getId()));
        if (billing.invoiceId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phiếu chưa có hóa đơn nháp để phát hành.");
        }
        issuedInvoiceChargeService.issueDraftInvoice(billing.invoiceId());
        recordEvent(ticket.getId(), ticket.getStatus(), ticket.getStatus(), MaintenanceTicketAction.UPDATE_REPAIR_INFO,
                "Phát hành hóa đơn phát sinh cho khách thuê");
        return response(findTicket(id));
    }

    private void collectMaintenanceCompensation(MaintenanceTicket ticket, CompleteMaintenanceTicketRequest request) {
        String method = normalizeCollectionMethod(request == null ? null : request.getCollectionMethod());
        if ("BILL_NOW".equals(method)) {
            issueMaintenanceCompensation(ticket, request);
            return;
        }
        scheduleMaintenanceCompensation(ticket, request);
    }

    private String normalizeCollectionMethod(String value) {
        String method = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (method.isBlank()) {
            return "MONTHLY_SCHEDULED";
        }
        if (Set.of("MONTHLY_SCHEDULED", "MONTHLY_DRAFT", "SCHEDULED", "SCHEDULED_MONTHLY").contains(method)) {
            return "MONTHLY_SCHEDULED";
        }
        if (Set.of("BILL_NOW", "IMMEDIATE_DRAFT", "CREATE_DRAFT").contains(method)) {
            return "BILL_NOW";
        }
        return "MONTHLY_SCHEDULED";
    }

    private void scheduleMaintenanceCompensation(MaintenanceTicket ticket, CompleteMaintenanceTicketRequest request) {
        Long amount = request == null ? null : firstNonNull(request.getActualCost(), request.getAmount());
        if (amount == null || amount <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chi phi boi thuong phai lon hon 0.");
        }
        RoomEntity room = ticket.getRoomId() == null
                ? null
                : jpaRoomRepository.findById(ticket.getRoomId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Khong tim thay phong de len lich thu khach."));
        LeaseContractEntity contract = resolveInvoiceableContract(ticket);
        scheduledBillingChargeService.scheduleCharge(
                room,
                contract,
                InvoiceLineType.MAINTENANCE_COMPENSATION,
                firstNonBlank(request.getCostDescription(), request.getCompletionNote(), "Boi thuong chi phi bao tri"),
                amount,
                IssuedInvoiceChargeService.SOURCE_MAINTENANCE_TICKET,
                ticket.getId(),
                request.getBillingPeriod(),
                jpaUserRepository.getReferenceById(currentUserId())
        );
    }

    private void issueMaintenanceCompensation(MaintenanceTicket ticket, CompleteMaintenanceTicketRequest request) {
        Long amount = request == null ? null : firstNonNull(request.getActualCost(), request.getAmount());
        if (amount == null || amount <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chi phí bồi thường phải lớn hơn 0.");
        }
        RoomEntity room = ticket.getRoomId() == null
                ? null
                : jpaRoomRepository.findById(ticket.getRoomId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không tìm thấy phòng để xuất hóa đơn."));
        LeaseContractEntity contract = resolveInvoiceableContract(ticket);
        issuedInvoiceChargeService.issueMaintenanceCharge(
                room,
                contract,
                InvoiceLineType.MAINTENANCE_COMPENSATION,
                firstNonBlank(request.getCostDescription(), request.getCompletionNote(), "Bồi thường chi phí bảo trì"),
                amount,
                ticket.getId(),
                jpaUserRepository.getReferenceById(currentUserId())
        );
    }

    private LeaseContractEntity resolveInvoiceableContract(MaintenanceTicket ticket) {
        if (ticket.getContractId() != null) {
            return jpaLeaseContractRepository.findById(ticket.getContractId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không tìm thấy hợp đồng của phiếu sự cố."));
        }
        if (ticket.getRoomId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phiếu khu vực chung không thể xuất hóa đơn cho khách.");
        }
        return jpaLeaseContractRepository
                .findFirstByRoom_IdAndStatusInAndDeletedAtIsNullOrderByIdDesc(ticket.getRoomId(), INVOICEABLE_CONTRACT_STATUSES)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Phòng chưa có hợp đồng đang hiệu lực, không thể xuất hóa đơn cho khách."
                ));
    }

    private void attachFiles(MaintenanceTicket ticket, List<Long> fileIds, AttachmentPhase phase, String note) {
        List<Long> normalizedIds = attachmentIdsPreservingOrder(fileIds);
        if (normalizedIds.isEmpty()) {
            return;
        }
        validateImageFileIds(normalizedIds);
        List<MaintenanceTicketAttachmentEntity> existing = jpaMaintenanceTicketAttachmentRepository
                .findAllByTicket_IdOrderBySortOrderAsc(ticket.getId());
        long existingBeforeCount = existing.stream()
                .filter(attachment -> attachment.getAttachmentPhase() == AttachmentPhase.BEFORE)
                .count();
        if (phase == AttachmentPhase.BEFORE && existingBeforeCount + normalizedIds.size() > MAX_BEFORE_ATTACHMENTS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ được upload tối đa 3 ảnh trước sửa.");
        }
        int nextSort = existing.stream()
                .map(MaintenanceTicketAttachmentEntity::getSortOrder)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .max()
                .orElse(-1) + 1;
        for (Long fileId : normalizedIds) {
            jpaMaintenanceTicketAttachmentRepository.save(MaintenanceTicketAttachmentEntity.builder()
                    .ticket(jpaMaintenanceTicketRepository.getReferenceById(ticket.getId()))
                    .file(jpaFileMetadataRepository.getReferenceById(fileId))
                    .attachmentPhase(phase)
                    .sortOrder(nextSort++)
                    .createdByUser(jpaUserRepository.getReferenceById(currentUserId()))
                    .build());
        }
    }

    private ApiResponse<MaintenanceTicketDetailsResponse> response(MaintenanceTicket ticket) {
        return ApiResponse.<MaintenanceTicketDetailsResponse>builder()
                .data(toDetailsResponse(ticket))
                .build();
    }

    private MaintenanceTicket findTicket(Long id) {
        return maintenanceTicketRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy phiếu sự cố."));
    }

    private MaintenanceTicketResponse toResponse(MaintenanceTicket ticket) {
        RoomInfo room = findRoomInfo(ticket);
        CostInfo cost = summarizeCosts(ticket.getId());
        BillingInfo billing = summarizeBilling(ticket.getId(), cost);
        Long displayCost = cost.amount() > 0 ? cost.amount() : billing.chargeAmount();
        MaintenanceTicketResponse.UserSummary createdBy = userSummary(ticket.getCreatedById());
        return MaintenanceTicketResponse.builder()
                .id(ticket.getId())
                .ticketCode(ticket.getTicketCode())
                .propertyId(ticket.getPropertyId())
                .propertyName(room.propertyName())
                .roomId(ticket.getRoomId())
                .roomCode(room.roomCode())
                .roomName(room.roomName())
                .ticketScope(toBusinessScope(ticket.getTicketScope()))
                .scope(toBusinessScope(ticket.getTicketScope()))
                .priority(ticket.getPriority())
                .severity(ticket.getPriority())
                .category(ticket.getCategory())
                .title(ticket.getTitle())
                .description(ticket.getDescription())
                .status(toBusinessStatus(ticket.getStatus()))
                .ticketStatus(toBusinessStatus(ticket.getStatus()))
                .ticketStatusLabel(ticketStatusLabel(ticket))
                .createdBy(createdBy)
                .workerName(ticket.getWorkerName())
                .repairmanName(ticket.getWorkerName())
                .repairmanPhone(ticket.getRepairmanPhone())
                .repairItems(readRepairItems(ticket.getRepairItems()))
                .rootCause(readRootCause(ticket.getRepairItems()))
                .costAmount(displayCost)
                .actualCost(displayCost)
                .costDescription(cost.description())
                .paidBy(cost.paidBy())
                .costResponsibility(cost.costResponsibility())
                .billingStatus(billing.status())
                .billingStatusLabel(billing.label())
                .billingPeriod(billing.billingPeriod())
                .invoiceId(billing.invoiceId())
                .invoiceCode(billing.invoiceCode())
                .invoiceStatus(billing.invoiceStatus())
                .paymentStatus(billing.status())
                .chargeToTenant(isTenantCharge(cost))
                .payer(cost.paidBy() == null ? null : cost.paidBy().name())
                .lineType(billing.lineType())
                .chargeAmount(billing.chargeAmount())
                .completedAt(ticket.getCompletedAt())
                .updatedAt(ticket.getUpdatedAt())
                .createdAt(ticket.getCreatedAt())
                .build();
    }

    private MaintenanceTicketDetailsResponse toDetailsResponse(MaintenanceTicket ticket) {
        RoomInfo room = findRoomInfo(ticket);
        CostInfo cost = summarizeCosts(ticket.getId());
        BillingInfo billing = summarizeBilling(ticket.getId(), cost);
        Long displayCost = cost.amount() > 0 ? cost.amount() : billing.chargeAmount();
        List<MaintenanceTicketDetailsResponse.AttachmentResponse> attachments = jpaMaintenanceTicketAttachmentRepository
                .findAllByTicket_IdOrderBySortOrderAsc(ticket.getId())
                .stream()
                .map(this::toAttachmentResponse)
                .toList();
        List<MaintenanceTicketDetailsResponse.EventResponse> events = jpaMaintenanceTicketEventRepository
                .findAllByTicket_IdOrderByCreatedAtAsc(ticket.getId())
                .stream()
                .map(this::toEventResponse)
                .toList();
        return MaintenanceTicketDetailsResponse.builder()
                .id(ticket.getId())
                .ticketCode(ticket.getTicketCode())
                .propertyId(ticket.getPropertyId())
                .propertyName(room.propertyName())
                .roomId(ticket.getRoomId())
                .roomCode(room.roomCode())
                .roomName(room.roomName())
                .ticketScope(toBusinessScope(ticket.getTicketScope()))
                .scope(toBusinessScope(ticket.getTicketScope()))
                .priority(ticket.getPriority())
                .severity(ticket.getPriority())
                .category(ticket.getCategory())
                .title(ticket.getTitle())
                .description(ticket.getDescription())
                .status(toBusinessStatus(ticket.getStatus()))
                .ticketStatus(toBusinessStatus(ticket.getStatus()))
                .ticketStatusLabel(ticketStatusLabel(ticket))
                .createdBy(detailUserSummary(ticket.getCreatedById()))
                .assignedTo(detailUserSummary(ticket.getAssignedToId()))
                .workerName(ticket.getWorkerName())
                .repairmanName(ticket.getWorkerName())
                .repairmanPhone(ticket.getRepairmanPhone())
                .repairItems(readRepairItems(ticket.getRepairItems()))
                .rootCause(readRootCause(ticket.getRepairItems()))
                .rejectionReason(ticket.getRejectionReason())
                .costAmount(displayCost)
                .actualCost(displayCost)
                .costDescription(cost.description())
                .paidBy(cost.paidBy())
                .costResponsibility(cost.costResponsibility())
                .billingStatus(billing.status())
                .billingStatusLabel(billing.label())
                .billingPeriod(billing.billingPeriod())
                .invoiceId(billing.invoiceId())
                .invoiceCode(billing.invoiceCode())
                .invoiceStatus(billing.invoiceStatus())
                .paymentStatus(billing.status())
                .chargeToTenant(isTenantCharge(cost))
                .payer(cost.paidBy() == null ? null : cost.paidBy().name())
                .lineType(billing.lineType())
                .chargeAmount(billing.chargeAmount())
                .completedAt(ticket.getCompletedAt())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .beforeAttachments(filterAttachments(attachments, AttachmentPhase.BEFORE))
                .afterAttachments(filterAttachments(attachments, AttachmentPhase.AFTER))
                .attachments(attachments)
                .events(events)
                .review(reviewResponse(ticket.getId()))
                .build();
    }

    private MaintenanceTicketDetailsResponse.EventResponse toEventResponse(MaintenanceTicketEventEntity event) {
        return MaintenanceTicketDetailsResponse.EventResponse.builder()
                .id(event.getId())
                .fromStatus(toBusinessStatus(event.getFromStatus()))
                .toStatus(toBusinessStatus(event.getToStatus()))
                .action(event.getAction())
                .note(event.getNote())
                .createdBy(detailUserSummary(event.getCreatedBy() == null ? null : event.getCreatedBy().getId()))
                .createdAt(event.getCreatedAt())
                .build();
    }

    private MaintenanceTicketDetailsResponse.ReviewResponse reviewResponse(Long ticketId) {
        return jpaMaintenanceReviewRepository.findFirstByTicket_IdOrderByCreatedAtDesc(ticketId)
                .map(review -> MaintenanceTicketDetailsResponse.ReviewResponse.builder()
                        .id(review.getId())
                        .rating(review.getRating())
                        .comment(review.getComment())
                        .reviewer(detailUserSummary(review.getReviewerUser() == null ? null : review.getReviewerUser().getId()))
                        .createdAt(review.getCreatedAt())
                        .build())
                .orElse(null);
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

    private MaintenanceTicketResponse.UserSummary userSummary(Long userId) {
        if (userId == null) {
            return null;
        }
        return jpaUserRepository.findById(userId)
                .map(user -> MaintenanceTicketResponse.UserSummary.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .role(user.getRole() == null ? null : user.getRole().name())
                        .build())
                .orElse(null);
    }

    private MaintenanceTicketDetailsResponse.UserSummary detailUserSummary(Long userId) {
        if (userId == null) {
            return null;
        }
        return jpaUserRepository.findById(userId)
                .map(user -> MaintenanceTicketDetailsResponse.UserSummary.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .role(user.getRole() == null ? null : user.getRole().name())
                        .build())
                .orElse(null);
    }

    private void assertCanRead(MaintenanceTicket ticket) {
        Role role = requireRole();
        if (role == Role.TENANT) {
            assertTenantCanRead(ticket);
            return;
        }
        assertManagerOrOwner(role);
        assertManagerCanAccessTicket(ticket);
    }

    private void assertManagerCanAccessTicket(MaintenanceTicket ticket) {
        assertManagerCanAccessProperty(ticket == null ? null : ticket.getPropertyId());
    }

    private void assertManagerCanAccessProperty(Long propertyId) {
        Role role = requireRole();
        if (role != Role.MANAGER) {
            return;
        }
        if (propertyId == null || !managerPropertyIds().contains(propertyId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền xử lý phiếu sự cố của cơ sở này.");
        }
    }

    private List<Long> restrictedPropertyIdsForCurrentManager(Role role) {
        return role == Role.MANAGER ? managerPropertyIds() : null;
    }

    private List<Long> managerPropertyIds() {
        return jpaRolePromotionRepository
                .findActivePropertyIds(currentUserId(), PromotionRole.MANAGER, RolePromotionStatus.ACTIVE)
                .stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private PageResponse<MaintenanceTicketResponse> emptyTicketPage(Pageable pageable) {
        return PageResponse.<MaintenanceTicketResponse>builder()
                .data(List.of())
                .pageSize(pageable.getPageSize())
                .currentPage(pageable.getPageNumber() + 1)
                .totalPages(0)
                .totalElements(0)
                .build();
    }

    private void assertTenantCanRead(MaintenanceTicket ticket) {
        Long currentUserId = currentUserId();
        boolean createdByCurrentUser = Objects.equals(ticket.getCreatedById(), currentUserId);
        boolean roomIsActive = ticket.getRoomId() != null && activeTenantRooms().stream()
                .anyMatch(room -> Objects.equals(room.roomId(), ticket.getRoomId()));
        if (!createdByCurrentUser && !roomIsActive) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền xem phiếu sự cố này.");
        }
    }

    private void assertTenantCanActOnRoomTicket(MaintenanceTicket ticket) {
        if (requireRole() != Role.TENANT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ khách thuê hợp lệ được thực hiện thao tác này.");
        }
        if (ticket.getTicketScope() != TicketScope.TENANT_ROOM || ticket.getRoomId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Khách thuê không được xác nhận phiếu khu vực chung.");
        }
        assertTenantCanRead(ticket);
    }

    private void assertManagerOrOwner(Role role) {
        if (role != Role.MANAGER && role != Role.OWNER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền xử lý phiếu sự cố.");
        }
    }

    private Role requireRole() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Phiên đăng nhập không hợp lệ.");
        }
        return principal.getRole();
    }

    private Long currentUserId() {
        Long userId = AuthUtils.getCurrentAuthenticationId();
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Phiên đăng nhập không hợp lệ.");
        }
        return userId;
    }

    private List<LeaseContractQueryService.ActiveRoomItem> activeTenantRooms() {
        return leaseContractQueryService.getRentalContexts(currentUserId())
                .stream()
                .filter(room -> room.contractStatus() != null
                        && ACTIVE_TENANT_CONTRACT_STATUSES.contains(room.contractStatus()))
                .toList();
    }

    private void validateCreatePayload(CreateMaintenanceTicketRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dữ liệu tạo phiếu không hợp lệ.");
        }
        String description = firstNonBlank(request.getDescription());
        if (description.length() < MIN_DESCRIPTION_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mô tả sự cố phải có tối thiểu 10 ký tự.");
        }
        normalizeCategory(firstNonBlank(request.getCategory(), request.getType(), "OTHER"));
    }

    private void validateImageFileIds(List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }
        List<FileMetadataEntity> files = jpaFileMetadataRepository.findAllById(fileIds);
        if (files.size() != uniqueIds(fileIds).size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Một hoặc nhiều file không tồn tại.");
        }
        for (FileMetadataEntity file : files) {
            String mimeType = file.getMimeType() == null ? "" : file.getMimeType().toLowerCase(Locale.ROOT);
            String originalName = file.getOriginalName() == null ? "" : file.getOriginalName().toLowerCase(Locale.ROOT);
            String extension = originalName.contains(".")
                    ? originalName.substring(originalName.lastIndexOf('.') + 1)
                    : "";
            if (!ALLOWED_IMAGE_MIME_TYPES.contains(mimeType) && !ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MVP chỉ hỗ trợ ảnh jpg, jpeg, png, webp.");
            }
        }
    }

    private void requireStatus(MaintenanceTicket ticket, MaintenanceTicketStatus required, String message) {
        if (ticket.getStatus() != required) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private ResponseStatusException invalidTransition(MaintenanceTicketStatus from, MaintenanceTicketStatus to) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Không thể chuyển trạng thái từ " + toBusinessStatus(from) + " sang " + toBusinessStatus(to) + ".");
    }

    private void recordEvent(
            Long ticketId,
            MaintenanceTicketStatus fromStatus,
            MaintenanceTicketStatus toStatus,
            MaintenanceTicketAction action,
            String note
    ) {
        if (ticketId == null || action == null) {
            return;
        }
        jpaMaintenanceTicketEventRepository.save(MaintenanceTicketEventEntity.builder()
                .ticket(jpaMaintenanceTicketRepository.getReferenceById(ticketId))
                .fromStatus(fromStatus == null ? null : fromStatus.name())
                .toStatus(toStatus == null ? null : toStatus.name())
                .action(action.name())
                .note(note)
                .createdBy(jpaUserRepository.getReferenceById(currentUserId()))
                .build());
    }

    private RoomInfo findRoomInfo(MaintenanceTicket ticket) {
        String propertyName = ticket.getPropertyId() == null
                ? null
                : jpaPropertyRepository.findById(ticket.getPropertyId()).map(PropertyEntity::getName).orElse(null);
        if (ticket.getRoomId() == null) {
            return new RoomInfo(null, null, propertyName);
        }
        Optional<RoomEntity> room = jpaRoomRepository.findById(ticket.getRoomId());
        return room.map(value -> new RoomInfo(value.getRoomCode(), value.getName(),
                        value.getProperty() == null ? propertyName : value.getProperty().getName()))
                .orElseGet(() -> new RoomInfo(null, null, propertyName));
    }

    private CostInfo summarizeCosts(Long ticketId) {
        List<MaintenanceCost> costs = maintenanceCostRepository.findAllByTicketId(ticketId);
        if (costs.isEmpty()) {
            return new CostInfo(0L, null, null, CostResponsibility.UNDECIDED);
        }
        long total = costs.stream()
                .map(MaintenanceCost::getAmount)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .sum();
        MaintenanceCost first = costs.getFirst();
        return new CostInfo(total, first.getPaidBy(), first.getDescription(),
                first.getCostResponsibility() == null
                        ? mapPaidByToResponsibility(first.getPaidBy())
                        : first.getCostResponsibility());
    }

    private BillingInfo summarizeBilling(Long ticketId, CostInfo cost) {
        return jpaInvoiceLineRepository
                .findFirstBySourceTypeAndSourceIdOrderByIdDesc(
                        IssuedInvoiceChargeService.SOURCE_MAINTENANCE_TICKET,
                        ticketId
                )
                .map(line -> {
                    InvoiceEntity invoice = line.getInvoice();
                    String invoiceStatus = invoice == null || invoice.getStatus() == null ? null : invoice.getStatus().name();
                    String status = billingStatus(invoice == null ? null : invoice.getStatus());
                    long chargeAmount = line.getAmount() == null
                            ? (long) line.getQuantity() * line.getUnitPrice()
                            : line.getAmount();
                    return new BillingInfo(
                            status,
                            billingStatusLabel(status),
                            invoice == null ? null : invoice.getId(),
                            invoice == null ? null : invoice.getInvoiceCode(),
                            invoiceStatus,
                            invoice == null ? null : invoice.getBillingPeriod(),
                            line.getLineType() == null ? null : line.getLineType().name(),
                            chargeAmount
                    );
                })
                .orElseGet(() -> {
                    Optional<PendingBillingChargeEntity> pendingCharge = jpaPendingBillingChargeRepository
                            .findFirstBySourceTypeAndSourceIdAndStatusInOrderByIdDesc(
                                    IssuedInvoiceChargeService.SOURCE_MAINTENANCE_TICKET,
                                    ticketId,
                                    List.of(PendingBillingChargeStatus.SCHEDULED, PendingBillingChargeStatus.FAILED)
                            );
                    if (pendingCharge.isPresent()) {
                        PendingBillingChargeEntity charge = pendingCharge.get();
                        return new BillingInfo(
                                charge.getStatus() == PendingBillingChargeStatus.FAILED ? "SCHEDULE_FAILED" : "SCHEDULED",
                                charge.getStatus() == PendingBillingChargeStatus.FAILED
                                        ? "Loi len lich hoa don"
                                        : "Da len lich gop hoa don dau thang",
                                null,
                                null,
                                null,
                                charge.getBillingPeriod(),
                                charge.getLineType() == null ? null : charge.getLineType().name(),
                                charge.getAmount()
                        );
                    }
                    if (cost.paidBy() == PaidBy.TENANT || cost.costResponsibility() == CostResponsibility.TENANT) {
                        return new BillingInfo("NOT_INVOICED", "Chưa tạo hóa đơn", null, null, null, null, null, cost.amount());
                    }
                    return new BillingInfo("NO_CHARGE", "Không thu khách", null, null, null, null, null, 0L);
                });
    }

    private String billingStatus(InvoiceStatus invoiceStatus) {
        if (invoiceStatus == null) {
            return "NO_CHARGE";
        }
        return switch (invoiceStatus) {
            case DRAFT -> "DRAFT";
            case ISSUED -> "PENDING_PAYMENT";
            case PARTIALLY_PAID -> "PARTIALLY_PAID";
            case PAID -> "PAID";
            case OVERDUE -> "OVERDUE";
            case VOIDED -> "VOIDED";
        };
    }

    private String billingStatusLabel(String status) {
        return switch (firstNonBlank(status)) {
            case "DRAFT" -> "Chờ phát hành";
            case "PARTIALLY_PAID" -> "Thanh toán một phần";
            case "PAID" -> "Đã thanh toán";
            case "OVERDUE" -> "Quá hạn";
            case "VOIDED" -> "Đã hủy";
            case "NOT_INVOICED" -> "Chưa lập hóa đơn";
            case "PENDING_PAYMENT" -> "Chờ thanh toán";
            default -> "Không thu khách";
        };
    }

    private String ticketStatusLabel(MaintenanceTicket ticket) {
        if ("RULE_VIOLATION".equalsIgnoreCase(ticket.getCategory()) && ticket.getStatus() == MaintenanceTicketStatus.COMPLETED) {
            return "Đã ghi nhận";
        }
        return switch (ticket.getStatus()) {
            case PENDING_ACCEPTANCE -> "Chờ tiếp nhận";
            case ACCEPTED -> "Đã tiếp nhận";
            case IN_PROGRESS -> "Đang xử lý";
            case WAITING_CONFIRMATION -> "Chờ xác nhận";
            case COMPLETED -> "Hoàn tất xử lý";
            case REJECTED -> "Từ chối";
            case CANCELLED -> "Đã hủy";
        };
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

    private MaintenanceTicketStatus parseStatus(String status) {
        String normalized = firstNonBlank(status).toUpperCase(Locale.ROOT);
        if (normalized.isBlank() || "ALL".equals(normalized)) {
            return null;
        }
        if ("PENDING".equals(normalized)) {
            return MaintenanceTicketStatus.PENDING_ACCEPTANCE;
        }
        try {
            return MaintenanceTicketStatus.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trạng thái phiếu sự cố không hợp lệ.");
        }
    }

    private Priority parsePriority(String priority) {
        String normalized = firstNonBlank(priority).toUpperCase(Locale.ROOT);
        if (normalized.isBlank() || "ALL".equals(normalized)) {
            return null;
        }
        try {
            return Priority.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mức độ sự cố không hợp lệ.");
        }
    }

    private TicketScope parseScope(String scope) {
        String normalized = firstNonBlank(scope).toUpperCase(Locale.ROOT);
        if (normalized.isBlank() || "ALL".equals(normalized)) {
            return null;
        }
        if ("ROOM".equals(normalized)) {
            return TicketScope.TENANT_ROOM;
        }
        try {
            return TicketScope.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phạm vi sự cố không hợp lệ.");
        }
    }

    private String normalizeCategory(String value) {
        String normalized = firstNonBlank(value, "OTHER").trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "ELECTRIC", "ELECTRICAL", "POWER" -> "ELECTRICITY";
            case "WIFI" -> "INTERNET";
            case "ROOM_EQUIPMENT", "EQUIPMENT" -> "FURNITURE";
            case "CLEANING_DRAINAGE", "TOILET", "BATHROOM" -> "SANITARY";
            default -> normalized;
        };
    }

    private String toBusinessStatus(MaintenanceTicketStatus status) {
        return status == MaintenanceTicketStatus.PENDING_ACCEPTANCE ? "PENDING" : status.name();
    }

    private String toBusinessStatus(String status) {
        if (status == null) {
            return null;
        }
        return "PENDING_ACCEPTANCE".equals(status) ? "PENDING" : status;
    }

    private String toBusinessScope(TicketScope scope) {
        return scope == TicketScope.TENANT_ROOM ? "ROOM" : scope.name();
    }

    private CostResponsibility mapPaidByToResponsibility(PaidBy paidBy) {
        if (paidBy == null) {
            return CostResponsibility.UNDECIDED;
        }
        return switch (paidBy) {
            case TENANT -> CostResponsibility.TENANT;
            case LANDLORD -> CostResponsibility.OWNER;
            case MANAGER -> CostResponsibility.OPERATION;
            case OTHER -> CostResponsibility.UNDECIDED;
        };
    }

    private PaidBy mapResponsibilityToPaidBy(CostResponsibility responsibility) {
        if (responsibility == null) {
            return PaidBy.OTHER;
        }
        return switch (responsibility) {
            case TENANT -> PaidBy.TENANT;
            case OWNER -> PaidBy.LANDLORD;
            case OPERATION -> PaidBy.MANAGER;
            case UNDECIDED -> PaidBy.OTHER;
        };
    }

    private String resolveRepairmanName(UpdateMaintenanceTicketProgressRequest request, String fallback) {
        return firstNonBlank(
                request == null ? null : request.getRepairmanName(),
                request == null ? null : request.getWorkerName(),
                fallback
        );
    }

    static List<Long> attachmentIdsPreservingOrder(List<Long> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        List<Long> ids = new ArrayList<>();
        for (Long value : values) {
            if (value != null) {
                ids.add(value);
            }
        }
        return ids;
    }

    private InternalMaintenanceCostResponse toInternalCostResponse(MaintenanceCostEntity cost) {
        MaintenanceTicketEntity ticket = cost.getTicket();
        PropertyEntity property = ticket.getProperty();
        RoomEntity room = ticket.getRoom();
        return new InternalMaintenanceCostResponse(
                ticket.getId(),
                ticket.getTicketCode(),
                property == null ? null : property.getId(),
                property == null ? null : property.getName(),
                room == null ? null : room.getId(),
                room == null ? null : room.getRoomCode(),
                ticket.getCategory(),
                toBusinessStatus(ticket.getStatus()),
                cost.getAmount(),
                PaidBy.LANDLORD.name(),
                "NO_CHARGE",
                cost.getDescription(),
                cost.getCreatedAt()
        );
    }

    private boolean isTenantCharge(CostInfo cost) {
        return cost.paidBy() == PaidBy.TENANT
                || cost.costResponsibility() == CostResponsibility.TENANT;
    }

    private List<Long> uniqueIds(List<Long> values) {
        return attachmentIdsPreservingOrder(values).stream().distinct().toList();
    }

    @SafeVarargs
    private <T> T firstNonNull(T... values) {
        if (values == null) {
            return null;
        }
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
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

    private record RoomInfo(String roomCode, String roomName, String propertyName) {
    }

    private record CostInfo(Long amount, PaidBy paidBy, String description, CostResponsibility costResponsibility) {
    }

    private record BillingInfo(
            String status,
            String label,
            Long invoiceId,
            String invoiceCode,
            String invoiceStatus,
            String billingPeriod,
            String lineType,
            Long chargeAmount
    ) {
    }
}
