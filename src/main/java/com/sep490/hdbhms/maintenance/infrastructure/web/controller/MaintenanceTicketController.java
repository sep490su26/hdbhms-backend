package com.sep490.hdbhms.maintenance.infrastructure.web.controller;

import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;

import com.sep490.hdbhms.billingandpayment.application.service.IssuedInvoiceChargeService;
import com.sep490.hdbhms.billingandpayment.application.service.ScheduledBillingChargeService;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceLineType;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceStatus;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.PendingBillingChargeStatus;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.InvoiceEntity;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.InvoiceLineEntity;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.PendingBillingChargeEntity;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaInvoiceLineRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaPendingBillingChargeRepository;
import com.sep490.hdbhms.accounting.domain.value_objects.ExpenseStatus;
import com.sep490.hdbhms.accounting.domain.value_objects.ExpenseType;
import com.sep490.hdbhms.accounting.infrastructure.persistence.entity.ExpenseApprovalRequestEntity;
import com.sep490.hdbhms.accounting.infrastructure.persistence.entity.OperatingExpenseEntity;
import com.sep490.hdbhms.accounting.infrastructure.persistence.jpa.JpaExpenseApprovalRequestRepository;
import com.sep490.hdbhms.accounting.infrastructure.persistence.jpa.JpaOperatingExpenseRepository;
import com.sep490.hdbhms.changerequest.domain.value_objects.AssignedRole;
import com.sep490.hdbhms.changerequest.domain.value_objects.RequestStatus;
import com.sep490.hdbhms.changerequest.domain.value_objects.RequestType;
import com.sep490.hdbhms.changerequest.domain.value_objects.RequesterRole;
import com.sep490.hdbhms.changerequest.domain.value_objects.TargetType;
import com.sep490.hdbhms.changerequest.infrastructure.persistence.entity.ChangeRequestEntity;
import com.sep490.hdbhms.changerequest.infrastructure.persistence.entity.ChangeRequestEventEntity;
import com.sep490.hdbhms.changerequest.infrastructure.persistence.jpa.JpaChangeRequestEventRepository;
import com.sep490.hdbhms.changerequest.infrastructure.persistence.jpa.JpaChangeRequestRepository;
import com.sep490.hdbhms.file.infrastructure.persistence.entity.FileMetadataEntity;
import com.sep490.hdbhms.file.infrastructure.persistence.jpa.JpaFileMetadataRepository;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.PromotionRole;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.RolePromotionStatus;
import com.sep490.hdbhms.identityandaccess.infrastructure.config.security.UserPrincipal;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.PersonProfileEntity;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.UserEntity;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaPersonProfileRepository;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaRolePromotionRepository;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaUserRepository;
import com.sep490.hdbhms.maintenance.application.port.out.MaintenanceCostRepository;
import com.sep490.hdbhms.maintenance.application.port.out.MaintenanceTicketRepository;
import com.sep490.hdbhms.maintenance.domain.model.MaintenanceCost;
import com.sep490.hdbhms.maintenance.domain.model.MaintenanceTicket;
import com.sep490.hdbhms.maintenance.domain.value_objects.AttachmentPhase;
import com.sep490.hdbhms.maintenance.domain.value_objects.CostResponsibility;
import com.sep490.hdbhms.maintenance.domain.value_objects.CostType;
import com.sep490.hdbhms.maintenance.domain.value_objects.MaintenanceTicketAction;
import com.sep490.hdbhms.maintenance.domain.value_objects.MaintenanceTicketStatus;
import com.sep490.hdbhms.maintenance.domain.value_objects.PaidBy;
import com.sep490.hdbhms.maintenance.domain.value_objects.TicketScope;
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
import com.sep490.hdbhms.occupancy.domain.value_objects.LeaseStatus;
import com.sep490.hdbhms.property.infrastructure.persistence.entity.PropertyEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeaseContractEntity;
import com.sep490.hdbhms.property.infrastructure.persistence.entity.RoomEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaLeaseContractRepository;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaPropertyRepository;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaRoomRepository;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import com.sep490.hdbhms.shared.dto.response.PageResponse;
import com.sep490.hdbhms.shared.utils.AuthUtils;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
    JpaPersonProfileRepository jpaPersonProfileRepository;
    JpaInvoiceLineRepository jpaInvoiceLineRepository;
    JpaFileMetadataRepository jpaFileMetadataRepository;
    JpaMaintenanceTicketRepository jpaMaintenanceTicketRepository;
    JpaMaintenanceTicketEventRepository jpaMaintenanceTicketEventRepository;
    JpaMaintenanceReviewRepository jpaMaintenanceReviewRepository;
    JpaMaintenanceTicketAttachmentRepository jpaMaintenanceTicketAttachmentRepository;
    JpaMaintenanceCostRepository jpaMaintenanceCostRepository;
    JpaPendingBillingChargeRepository jpaPendingBillingChargeRepository;
    JpaOperatingExpenseRepository jpaOperatingExpenseRepository;
    JpaExpenseApprovalRequestRepository jpaExpenseApprovalRequestRepository;
    JpaChangeRequestRepository jpaChangeRequestRepository;
    JpaChangeRequestEventRepository jpaChangeRequestEventRepository;
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
            @RequestParam(required = false) Long floorId,
            @RequestParam(required = false) Long propertyId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String scope,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Role role = requireRole();
        if (role == Role.TENANT) {
            return searchTicketsForTenant(code, status, roomId, floorId, category, scope, fromDate, toDate, pageable);
        }
        assertManagerOrOwner(role);
        List<Long> restrictedPropertyIds = restrictedPropertyIdsForCurrentManager(role);
        if (restrictedPropertyIds != null && restrictedPropertyIds.isEmpty()) {
            return emptyTicketPage(pageable);
        }
        if (propertyId != null && restrictedPropertyIds != null && !restrictedPropertyIds.contains(propertyId)) {
            throw new AppException(ApiErrorCode.MIGRATED_BAN_KHONG_CO_QUYEN_XEM_PHIEU_SU_CO_CUA_CO_SO_NAY);
        }
        return searchTickets(code, status, roomId, floorId, propertyId, firstNonBlank(category, type), scope, fromDate, toDate, pageable, null, restrictedPropertyIds);
    }

    @GetMapping("/my")
    @Transactional(readOnly = true)
    public PageResponse<MaintenanceTicketResponse> getMyMaintenanceTickets(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) Long floorId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String scope,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        if (requireRole() != Role.TENANT) {
            throw new AppException(ApiErrorCode.MIGRATED_CHI_KHACH_THUE_MOI_UOC_SU_DUNG_CHUC_NANG_NAY);
        }
        return searchTicketsForTenant(code, status, roomId, floorId, category, scope, fromDate, toDate, pageable);
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
    public PageResponse<InternalMaintenanceCostResponse> getInternalMaintenanceCosts(
            @PageableDefault(size = 10) Pageable pageable
    ) {
        Role role = requireRole();
        if (role != Role.OWNER && role != Role.MANAGER && role != Role.ACCOUNTANT) {
            throw new AppException(ApiErrorCode.MIGRATED_BAN_KHONG_CO_QUYEN_XEM_BAO_CAO_CHI_PHI_NOI_BO);
        }
        List<Long> restrictedPropertyIds = role == Role.MANAGER
                ? restrictedPropertyIdsForCurrentManager(role)
                : null;
        List<InternalMaintenanceCostResponse> rows = jpaMaintenanceCostRepository.findAllByPaidByOrderByCreatedAtDesc(PaidBy.LANDLORD)
                .stream()
                .filter(cost -> cost.getTicket() != null
                        && cost.getTicket().getTicketScope() == TicketScope.PROPERTY_OPERATION)
                .filter(cost -> restrictedPropertyIds == null
                        || restrictedPropertyIds.contains(cost.getTicket().getProperty().getId()))
                .map(this::toInternalCostResponse)
                .toList();
        List<InternalMaintenanceCostResponse> pageRows = rows.stream()
                .skip(pageable.getOffset())
                .limit(pageable.getPageSize())
                .toList();
        return PageResponse.fromPageToPageResponse(new PageImpl<>(pageRows, pageable, rows.size()));
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
                    .orElseThrow(() -> new AppException(ApiErrorCode.MIGRATED_KHONG_TIM_THAY_PHONG));
            propertyId = room.getProperty().getId();
        }
        if (propertyId == null || !jpaPropertyRepository.existsById(propertyId)) {
            throw new AppException(ApiErrorCode.MIGRATED_VUI_LONG_CHON_CO_SO_HOP_LE);
        }
        assertManagerCanAccessProperty(propertyId);

        List<Long> attachmentIds = attachmentIdsPreservingOrder(request.getAttachmentIds());
        validateImageFileIds(attachmentIds);
        if (attachmentIds.size() > MAX_BEFORE_ATTACHMENTS) {
            throw new AppException(ApiErrorCode.MIGRATED_CHI_UOC_UPLOAD_TOI_A_3_ANH_HIEN_TRANG);
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
        requireStatus(ticket, MaintenanceTicketStatus.PENDING_ACCEPTANCE, ApiErrorCode.MIGRATED_CHI_TIEP_NHAN_PHIEU_ANG_CHO_TIEP_NHAN);
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
        requireStatus(ticket, MaintenanceTicketStatus.PENDING_ACCEPTANCE, ApiErrorCode.MIGRATED_CHI_TU_CHOI_PHIEU_ANG_CHO_TIEP_NHAN);
        String reason = firstNonBlank(request == null ? null : request.getReason());
        if (reason.isBlank()) {
            throw new AppException(ApiErrorCode.MIGRATED_VUI_LONG_NHAP_LY_DO_TU_CHOI);
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
            throw new AppException(ApiErrorCode.MIGRATED_CHI_CHU_TRO_UOC_SUA_CHI_PHI_CUA_TICKET_A_HOAN_TAT);
        }
        if (ticket.getStatus() != MaintenanceTicketStatus.ACCEPTED
                && ticket.getStatus() != MaintenanceTicketStatus.IN_PROGRESS
                && ticket.getStatus() != MaintenanceTicketStatus.COMPLETED) {
            throw new AppException(ApiErrorCode.MIGRATED_KHONG_THE_CAP_NHAT_THONG_TIN_XU_LY_O_TRANG_THAI_NAY);
        }
        MaintenanceTicketStatus fromStatus = ticket.getStatus();
        MaintenanceTicketStatus toStatus = fromStatus == MaintenanceTicketStatus.ACCEPTED
                ? MaintenanceTicketStatus.IN_PROGRESS
                : fromStatus;
        MaintenanceTicket saved = saveRepairInformation(ticket, request, toStatus);
        saveCost(saved.getId(), request);
        if (saved.getStatus() == MaintenanceTicketStatus.COMPLETED) {
            syncOwnerMaintenanceExpense(saved);
        }
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
        requireStatus(ticket, MaintenanceTicketStatus.IN_PROGRESS, ApiErrorCode.MIGRATED_CHI_HOAN_TAT_KHI_PHIEU_ANG_XU_LY);
        Long amount = request == null ? null : firstNonNull(request.getActualCost(), request.getAmount());
        if (amount != null && amount < 0) {
            throw new AppException(ApiErrorCode.MIGRATED_CHI_PHI_THUC_TE_KHONG_UOC_AM);
        }
        MaintenanceTicket saved = saveRepairInformation(ticket, request, targetStatusAfterManagerCompletion());
        saveCost(saved.getId(), request);
        if (shouldChargeTenant(saved, request)) {
            collectMaintenanceCompensation(saved, request);
        }
        if (request != null && request.getAttachmentIds() != null && !request.getAttachmentIds().isEmpty()) {
            attachFiles(saved, request.getAttachmentIds(), AttachmentPhase.AFTER, "Upload ảnh sau sửa");
        }
        recordEvent(saved.getId(), ticket.getStatus(), saved.getStatus(), MaintenanceTicketAction.REQUEST_CONFIRMATION,
                firstNonBlank(request == null ? null : request.getCompletionNote(),
                        "Đã hoàn tất xử lý phiếu sự cố"));
        return response(saved);
    }

    @PostMapping("/{id}/confirm")
    @Transactional
    public ApiResponse<MaintenanceTicketDetailsResponse> confirmMaintenanceTicket(@PathVariable Long id) {
        MaintenanceTicket ticket = findTicket(id);
        requireStatus(ticket, MaintenanceTicketStatus.WAITING_CONFIRMATION, ApiErrorCode.MIGRATED_CHI_XAC_NHAN_PHIEU_ANG_CHO_XAC_NHAN);
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
        syncOwnerMaintenanceExpense(saved);
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
        requireStatus(ticket, MaintenanceTicketStatus.WAITING_CONFIRMATION, ApiErrorCode.MIGRATED_CHI_BAO_CHUA_SUA_XONG_KHI_PHIEU_ANG_CHO_XAC_NHAN);
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
        requireStatus(ticket, MaintenanceTicketStatus.COMPLETED, ApiErrorCode.MIGRATED_CHI_ANH_GIA_PHIEU_A_HOAN_TAT);
        Long currentUserId = currentUserId();
        int rating = request == null || request.getRating() == null ? 0 : request.getRating();
        if (rating < 1 || rating > 5) {
            throw new AppException(ApiErrorCode.MIGRATED_ANH_GIA_PHAI_TU_1_EN_5_SAO);
        }
        if (jpaMaintenanceReviewRepository.findByTicket_IdAndReviewerUser_Id(id, currentUserId).isPresent()) {
            throw new AppException(ApiErrorCode.MIGRATED_BAN_A_ANH_GIA_PHIEU_NAY);
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
            throw new AppException(ApiErrorCode.MIGRATED_VUI_LONG_CHON_ANH_CAN_UPLOAD);
        }
        if (requireRole() == Role.TENANT) {
            assertTenantCanActOnRoomTicket(ticket);
            if (phase != AttachmentPhase.BEFORE) {
                throw new AppException(ApiErrorCode.MIGRATED_KHACH_THUE_CHI_UOC_UPLOAD_ANH_TRUOC_SUA);
            }
            requireStatus(ticket, MaintenanceTicketStatus.PENDING_ACCEPTANCE, ApiErrorCode.MIGRATED_CHI_BO_SUNG_ANH_TRUOC_SUA_KHI_PHIEU_ANG_CHO_TIEP_NHAN);
        } else {
            assertManagerOrOwner(requireRole());
            assertManagerCanAccessTicket(ticket);
            if (phase == AttachmentPhase.BEFORE && ticket.getStatus() != MaintenanceTicketStatus.PENDING_ACCEPTANCE) {
                throw new AppException(ApiErrorCode.MIGRATED_CHI_BO_SUNG_ANH_TRUOC_SUA_KHI_PHIEU_ANG_CHO_TIEP_NHAN);
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
            Long floorId,
            String category,
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
            throw new AppException(ApiErrorCode.MIGRATED_BAN_KHONG_CO_QUYEN_XEM_TICKET_CUA_PHONG_NAY);
        }
        List<Long> restrictedRoomIds = roomId == null ? activeRoomIds : List.of(roomId);
        return searchTickets(code, status, null, floorId, null, category,
                firstNonBlank(scope, "ROOM"), fromDate, toDate, pageable, restrictedRoomIds, null);
    }

    private PageResponse<MaintenanceTicketResponse> searchTickets(
            String code,
            String status,
            Long roomId,
            Long floorId,
            Long propertyId,
            String category,
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
        if (floorId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("room").get("floor").get("id"), floorId));
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
            throw new AppException(ApiErrorCode.MIGRATED_KHONG_XAC_INH_UOC_PHONG_ANG_THUE_E_TAO_PHIEU_SU_CO);
        }
        TicketScope requestedScope = firstNonNull(request.getTicketScope(), request.getScope());
        if (requestedScope != null && requestedScope != TicketScope.TENANT_ROOM) {
            throw new AppException(ApiErrorCode.MIGRATED_KHACH_THUE_CHI_UOC_TAO_PHIEU_SU_CO_PHONG_ANG_THUE);
        }
        LeaseContractQueryService.ActiveRoomItem activeRoom = activeTenantRooms().stream()
                .filter(room -> Objects.equals(room.roomId(), request.getRoomId()))
                .findFirst()
                .orElseThrow(() -> new AppException(ApiErrorCode.MIGRATED_BAN_KHONG_CO_QUYEN_TAO_PHIEU_SU_CO_CHO_PHONG_NAY));
        RoomEntity room = jpaRoomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new AppException(ApiErrorCode.MIGRATED_KHONG_TIM_THAY_PHONG));
        List<Long> attachmentIds = attachmentIdsPreservingOrder(request.getAttachmentIds());
        validateImageFileIds(attachmentIds);
        if (attachmentIds.size() > MAX_BEFORE_ATTACHMENTS) {
            throw new AppException(ApiErrorCode.MIGRATED_CHI_UOC_UPLOAD_TOI_A_3_ANH_TRUOC_SUA);
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
                    .orElseThrow(() -> new AppException(ApiErrorCode.MIGRATED_KHONG_TIM_THAY_PHONG));
            propertyId = room.getProperty().getId();
        }
        if (propertyId == null) {
            throw new AppException(ApiErrorCode.MIGRATED_VUI_LONG_CHON_CO_SO_PROPERTY_CHO_SU_CO_KHU_VUC_CHUNG);
        }
        if (!jpaPropertyRepository.existsById(propertyId)) {
            throw new AppException(ApiErrorCode.MIGRATED_KHONG_TIM_THAY_CO_SO_PROPERTY);
        }
        assertManagerCanAccessProperty(propertyId);
        TicketScope scope = firstNonNull(request.getTicketScope(), request.getScope());
        if (scope == null) {
            scope = roomId == null ? TicketScope.COMMON_AREA : TicketScope.TENANT_ROOM;
        }
        if (roomId == null && scope == TicketScope.TENANT_ROOM) {
            throw new AppException(ApiErrorCode.MIGRATED_TICKET_PHONG_CAN_CO_ROOMID);
        }
        List<Long> attachmentIds = attachmentIdsPreservingOrder(request.getAttachmentIds());
        validateImageFileIds(attachmentIds);
        if (attachmentIds.size() > MAX_BEFORE_ATTACHMENTS) {
            throw new AppException(ApiErrorCode.MIGRATED_CHI_UOC_UPLOAD_TOI_A_3_ANH_TRUOC_SUA);
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
        MaintenanceTicket ticket = MaintenanceTicket.builder()
                .ticketCode(String.format("#SC-TMP-%d-%d", currentUserId(), System.nanoTime()))
                .propertyId(propertyId)
                .roomId(roomId)
                .contractId(contractId)
                .createdById(currentUserId())
                .ticketScope(scope)
                .category(category)
                .title(firstNonBlank(request.getTitle(), category))
                .description(request.getDescription().trim())
                .repairRequested(request.getRepairRequested() == null || request.getRepairRequested())
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
            throw new AppException(ApiErrorCode.MIGRATED_CHI_PHI_THUC_TE_KHONG_UOC_AM);
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

    private void syncOwnerMaintenanceExpense(MaintenanceTicket ticket) {
        if (ticket == null || ticket.getId() == null) {
            return;
        }
        MaintenanceCostEntity cost = latestMaintenanceCost(ticket.getId());
        if (cost == null
                || cost.getAmount() == null
                || cost.getAmount() <= 0
                || cost.getCostResponsibility() != CostResponsibility.OWNER) {
            cancelUnpaidMaintenanceExpense(ticket, cost);
            return;
        }

        OperatingExpenseEntity expense = jpaOperatingExpenseRepository
                .findFirstByTicketIdOrderByIdAsc(ticket.getId())
                .orElse(null);
        if (expense != null && expense.getStatus() == ExpenseStatus.PAID) {
            if (!Objects.equals(expense.getAmount(), cost.getAmount())) {
                throw new AppException(ApiErrorCode.MIGRATED_KHOAN_CHI_SU_CO_DA_THANH_TOAN_KHONG_THE_DOI_SO_TIEN);
            }
            return;
        }

        UserEntity actor = resolveMaintenanceExpenseActor(ticket, cost);
        LocalDateTime now = LocalDateTime.now();
        if (expense == null) {
            expense = OperatingExpenseEntity.builder()
                    .expenseCode(nextMaintenanceExpenseCode(ticket.getId()))
                    .createdBy(actor)
                    .build();
        } else if (expense.getCreatedBy() == null) {
            expense.setCreatedBy(actor);
        }

        expense.setProperty(jpaPropertyRepository.getReferenceById(ticket.getPropertyId()));
        expense.setRoom(ticket.getRoomId() == null ? null : jpaRoomRepository.getReferenceById(ticket.getRoomId()));
        expense.setTicketId(ticket.getId());
        expense.setExpenseType(maintenanceExpenseType(cost.getCostType()));
        expense.setDescription(buildMaintenanceExpenseDescription(ticket, cost));
        expense.setAmount(cost.getAmount());
        expense.setExpenseDate(ticket.getCompletedAt() == null ? LocalDate.now() : ticket.getCompletedAt().toLocalDate());
        expense.setStatus(ExpenseStatus.READY_FOR_PAYMENT);
        expense.setApprovedBy(actor);
        expense.setApprovedAt(now);

        OperatingExpenseEntity savedExpense = jpaOperatingExpenseRepository.save(expense);
        syncMaintenanceExpenseApproval(savedExpense, ticket, cost, actor, now);
    }

    private void cancelUnpaidMaintenanceExpense(MaintenanceTicket ticket, MaintenanceCostEntity cost) {
        OperatingExpenseEntity expense = jpaOperatingExpenseRepository
                .findFirstByTicketIdOrderByIdAsc(ticket.getId())
                .orElse(null);
        if (expense == null || expense.getStatus() == ExpenseStatus.CANCELLED) {
            return;
        }
        if (expense.getStatus() == ExpenseStatus.PAID) {
            throw new AppException(ApiErrorCode.MIGRATED_KHOAN_CHI_SU_CO_DA_THANH_TOAN_KHONG_THE_CHUYEN_NGUOI_CHIU_PHI);
        }
        UserEntity actor = resolveMaintenanceExpenseActor(ticket, cost);
        expense.setStatus(ExpenseStatus.CANCELLED);
        jpaOperatingExpenseRepository.save(expense);
        jpaExpenseApprovalRequestRepository.findByOperatingExpense_Id(expense.getId())
                .map(ExpenseApprovalRequestEntity::getChangeRequest)
                .ifPresent(changeRequest -> {
                    if (changeRequest.getStatus() != RequestStatus.COMPLETED) {
                        changeRequest.setStatus(RequestStatus.CANCELLED);
                        changeRequest.setResolutionNote("Tu dong huy vi chi phi khong con do chu tro chiu.");
                        changeRequest.setResolvedBy(actor);
                        changeRequest.setResolvedAt(LocalDateTime.now());
                        jpaChangeRequestRepository.save(changeRequest);
                    }
                });
    }

    private MaintenanceCostEntity latestMaintenanceCost(Long ticketId) {
        if (ticketId == null) {
            return null;
        }
        return jpaMaintenanceCostRepository.findAllByTicket_IdOrderByCreatedAtAsc(ticketId)
                .stream()
                .reduce((ignored, current) -> current)
                .orElse(null);
    }

    private void syncMaintenanceExpenseApproval(
            OperatingExpenseEntity expense,
            MaintenanceTicket ticket,
            MaintenanceCostEntity cost,
            UserEntity actor,
            LocalDateTime now
    ) {
        String reason = buildMaintenanceExpenseDescription(ticket, cost);
        ExpenseApprovalRequestEntity approval = jpaExpenseApprovalRequestRepository
                .findByOperatingExpense_Id(expense.getId())
                .orElse(null);
        if (approval == null) {
            ChangeRequestEntity changeRequest = jpaChangeRequestRepository.save(ChangeRequestEntity.builder()
                    .requestCode(nextMaintenanceExpenseRequestCode(ticket.getId()))
                    .requestType(RequestType.EXPENSE_APPROVAL)
                    .requester(actor)
                    .requesterRole(toRequesterRole(actor.getRole()))
                    .targetType(TargetType.OPERATING_EXPENSE)
                    .targetId(expense.getId())
                    .title("Khoan chi bao tri " + firstNonBlank(ticket.getTicketCode(), "#" + ticket.getId()))
                    .description(reason)
                    .assignedRole(AssignedRole.OWNER)
                    .status(RequestStatus.APPROVED)
                    .resolvedBy(actor)
                    .resolvedAt(now)
                    .build());
            jpaExpenseApprovalRequestRepository.save(ExpenseApprovalRequestEntity.builder()
                    .operatingExpense(expense)
                    .changeRequest(changeRequest)
                    .reason(reason)
                    .expectedPaymentDate(expense.getExpenseDate())
                    .build());
            jpaChangeRequestEventRepository.save(ChangeRequestEventEntity.builder()
                    .changeRequest(changeRequest)
                    .toStatus(RequestStatus.APPROVED)
                    .note("Tu dong ghi nhan khoan chi bao tri do chu tro chiu.")
                    .actedBy(actor)
                    .build());
            return;
        }

        approval.setReason(reason);
        approval.setExpectedPaymentDate(expense.getExpenseDate());
        ChangeRequestEntity changeRequest = approval.getChangeRequest();
        if (changeRequest != null) {
            changeRequest.setTitle("Khoan chi bao tri " + firstNonBlank(ticket.getTicketCode(), "#" + ticket.getId()));
            changeRequest.setDescription(reason);
            changeRequest.setStatus(RequestStatus.APPROVED);
            changeRequest.setResolvedBy(actor);
            changeRequest.setResolvedAt(now);
            jpaChangeRequestRepository.save(changeRequest);
        }
        jpaExpenseApprovalRequestRepository.save(approval);
    }

    private UserEntity resolveMaintenanceExpenseActor(MaintenanceTicket ticket, MaintenanceCostEntity cost) {
        Long actorId = cost == null || cost.getCreatedBy() == null ? null : cost.getCreatedBy().getId();
        if (actorId == null) {
            actorId = firstNonNull(ticket.getAssignedToId(), ticket.getCreatedById(), currentUserId());
        }
        return jpaUserRepository.getReferenceById(actorId);
    }

    private String buildMaintenanceExpenseDescription(MaintenanceTicket ticket, MaintenanceCostEntity cost) {
        String ticketLabel = firstNonBlank(ticket.getTicketCode(), "#" + ticket.getId());
        String detail = firstNonBlank(cost.getDescription(), ticket.getTitle(), ticket.getCategory(), "Chi phi bao tri");
        return "Chi phi bao tri " + ticketLabel + ": " + detail;
    }

    private String nextMaintenanceExpenseCode(Long ticketId) {
        for (int sequence = 1; sequence <= 999; sequence++) {
            String candidate = maintenanceExpenseCode(ticketId, sequence);
            if (!jpaOperatingExpenseRepository.existsByExpenseCode(candidate)) {
                return candidate;
            }
        }
        throw new AppException(ApiErrorCode.UNDEFINED);
    }

    private String nextMaintenanceExpenseRequestCode(Long ticketId) {
        for (int sequence = 1; sequence <= 999; sequence++) {
            String candidate = maintenanceExpenseRequestCode(ticketId, sequence);
            if (!jpaChangeRequestRepository.existsByRequestCode(candidate)) {
                return candidate;
            }
        }
        throw new AppException(ApiErrorCode.UNDEFINED);
    }

    static String maintenanceExpenseCode(Long ticketId, int sequence) {
        String base = "EXP-MT-" + (ticketId == null ? "UNKNOWN" : ticketId);
        return sequence <= 1 ? base : base + "-" + sequence;
    }

    static String maintenanceExpenseRequestCode(Long ticketId, int sequence) {
        String base = "DYC-MT-" + (ticketId == null ? "UNKNOWN" : ticketId);
        return sequence <= 1 ? base : base + "-" + sequence;
    }

    static ExpenseType maintenanceExpenseType(CostType costType) {
        if (costType == null) {
            return ExpenseType.OTHER;
        }
        return switch (costType) {
            case LABOR, MATERIAL, TENANT_COMPENSATION -> ExpenseType.REPAIR;
            case COMMON_OPERATING -> ExpenseType.COMMON_UTILITY;
            case OTHER -> ExpenseType.OTHER;
        };
    }

    private RequesterRole toRequesterRole(Role role) {
        if (role == null) {
            return RequesterRole.MANAGER;
        }
        return switch (role) {
            case OWNER -> RequesterRole.OWNER;
            case MANAGER -> RequesterRole.MANAGER;
            case ACCOUNTANT -> RequesterRole.ACCOUNTANT;
            case TENANT -> RequesterRole.TENANT;
            case LEAD -> RequesterRole.LEAD;
        };
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
                    .orElseThrow(() -> new AppException(ApiErrorCode.MIGRATED_KHONG_TIM_THAY_CHUNG_TU_CHI_PHI));
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
            throw new AppException(ApiErrorCode.MIGRATED_PHIEU_CHUA_CO_HOA_ON_NHAP_E_PHAT_HANH);
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
            throw new AppException(ApiErrorCode.MIGRATED_CHI_PHI_BOI_THUONG_PHAI_LON_HON_0);
        }
        RoomEntity room = ticket.getRoomId() == null
                ? null
                : jpaRoomRepository.findById(ticket.getRoomId())
                .orElseThrow(() -> new AppException(ApiErrorCode.MIGRATED_KHONG_TIM_THAY_PHONG_DE_LEN_LICH_THU_KHACH));
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
            throw new AppException(ApiErrorCode.MIGRATED_CHI_PHI_BOI_THUONG_PHAI_LON_HON_0);
        }
        RoomEntity room = ticket.getRoomId() == null
                ? null
                : jpaRoomRepository.findById(ticket.getRoomId())
                .orElseThrow(() -> new AppException(ApiErrorCode.MIGRATED_KHONG_TIM_THAY_PHONG_E_XUAT_HOA_ON));
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
                    .orElseThrow(() -> new AppException(ApiErrorCode.MIGRATED_KHONG_TIM_THAY_HOP_ONG_CUA_PHIEU_SU_CO));
        }
        if (ticket.getRoomId() == null) {
            throw new AppException(ApiErrorCode.MIGRATED_PHIEU_KHU_VUC_CHUNG_KHONG_THE_XUAT_HOA_ON_CHO_KHACH);
        }
        return jpaLeaseContractRepository
                .findFirstByRoom_IdAndStatusInAndDeletedAtIsNullOrderByIdDesc(ticket.getRoomId(), INVOICEABLE_CONTRACT_STATUSES)
                .orElseThrow(() -> new AppException(ApiErrorCode.MIGRATED_PHONG_CHUA_CO_HOP_ONG_ANG_HIEU_LUC_KHONG_THE_XUAT_HOA_ON_C3FA9D));
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
            throw new AppException(ApiErrorCode.MIGRATED_CHI_UOC_UPLOAD_TOI_A_3_ANH_TRUOC_SUA);
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
                .orElseThrow(() -> new AppException(ApiErrorCode.MIGRATED_KHONG_TIM_THAY_PHIEU_SU_CO));
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
                .category(ticket.getCategory())
                .title(ticket.getTitle())
                .description(ticket.getDescription())
                .repairRequested(ticket.getRepairRequested() == null || ticket.getRepairRequested())
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
                .category(ticket.getCategory())
                .title(ticket.getTitle())
                .description(ticket.getDescription())
                .repairRequested(ticket.getRepairRequested() == null || ticket.getRepairRequested())
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
                        .fullName(userDisplayName(user))
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
                        .fullName(userDisplayName(user))
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .role(user.getRole() == null ? null : user.getRole().name())
                        .build())
                .orElse(null);
    }

    private String userDisplayName(UserEntity user) {
        if (user == null) {
            return "";
        }
        String profileName = jpaPersonProfileRepository.findFirstByUser_IdAndDeletedAtIsNullOrderByIdDesc(user.getId())
                .map(PersonProfileEntity::getFullName)
                .orElse(null);
        return firstNonBlank(profileName, user.getEmail(), user.getPhone());
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
            throw new AppException(ApiErrorCode.MIGRATED_BAN_KHONG_CO_QUYEN_XU_LY_PHIEU_SU_CO_CUA_CO_SO_NAY);
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
            throw new AppException(ApiErrorCode.MIGRATED_BAN_KHONG_CO_QUYEN_XEM_PHIEU_SU_CO_NAY);
        }
    }

    private void assertTenantCanActOnRoomTicket(MaintenanceTicket ticket) {
        if (requireRole() != Role.TENANT) {
            throw new AppException(ApiErrorCode.MIGRATED_CHI_KHACH_THUE_HOP_LE_UOC_THUC_HIEN_THAO_TAC_NAY);
        }
        if (ticket.getTicketScope() != TicketScope.TENANT_ROOM || ticket.getRoomId() == null) {
            throw new AppException(ApiErrorCode.MIGRATED_KHACH_THUE_KHONG_UOC_XAC_NHAN_PHIEU_KHU_VUC_CHUNG);
        }
        assertTenantCanRead(ticket);
    }

    private void assertManagerOrOwner(Role role) {
        if (role != Role.MANAGER && role != Role.OWNER) {
            throw new AppException(ApiErrorCode.MIGRATED_BAN_KHONG_CO_QUYEN_XU_LY_PHIEU_SU_CO);
        }
    }

    private Role requireRole() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new AppException(ApiErrorCode.MIGRATED_PHIEN_ANG_NHAP_KHONG_HOP_LE);
        }
        return principal.getRole();
    }

    private Long currentUserId() {
        Long userId = AuthUtils.getCurrentAuthenticationId();
        if (userId == null) {
            throw new AppException(ApiErrorCode.MIGRATED_PHIEN_ANG_NHAP_KHONG_HOP_LE);
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
            throw new AppException(ApiErrorCode.MIGRATED_DU_LIEU_TAO_PHIEU_KHONG_HOP_LE);
        }
        String description = firstNonBlank(request.getDescription());
        if (description.length() < MIN_DESCRIPTION_LENGTH) {
            throw new AppException(ApiErrorCode.MIGRATED_MO_TA_SU_CO_PHAI_CO_TOI_THIEU_10_KY_TU);
        }
        normalizeCategory(firstNonBlank(request.getCategory(), request.getType(), "OTHER"));
    }

    private void validateImageFileIds(List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }
        List<FileMetadataEntity> files = jpaFileMetadataRepository.findAllById(fileIds);
        if (files.size() != uniqueIds(fileIds).size()) {
            throw new AppException(ApiErrorCode.MIGRATED_MOT_HOAC_NHIEU_FILE_KHONG_TON_TAI);
        }
        for (FileMetadataEntity file : files) {
            String mimeType = file.getMimeType() == null ? "" : file.getMimeType().toLowerCase(Locale.ROOT);
            String originalName = file.getOriginalName() == null ? "" : file.getOriginalName().toLowerCase(Locale.ROOT);
            String extension = originalName.contains(".")
                    ? originalName.substring(originalName.lastIndexOf('.') + 1)
                    : "";
            if (!ALLOWED_IMAGE_MIME_TYPES.contains(mimeType) && !ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
                throw new AppException(ApiErrorCode.MIGRATED_MVP_CHI_HO_TRO_ANH_JPG_JPEG_PNG_WEBP);
            }
        }
    }

    private void requireStatus(MaintenanceTicket ticket, MaintenanceTicketStatus required, ApiErrorCode apiErrorCode) {
        if (ticket.getStatus() != required) {
            throw new AppException(apiErrorCode);
        }
    }

    private AppException invalidTransition(MaintenanceTicketStatus from, MaintenanceTicketStatus to) {
        return new AppException(ApiErrorCode.MIGRATED_KHONG_THE_CHUYEN_TRANG_THAI_TU_TOBUSINESSSTATUS_FROM_SAN_B457BD);
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
            throw new AppException(ApiErrorCode.MIGRATED_TRANG_THAI_PHIEU_SU_CO_KHONG_HOP_LE);
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
            throw new AppException(ApiErrorCode.MIGRATED_PHAM_VI_SU_CO_KHONG_HOP_LE);
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
            case PROPERTY -> PaidBy.LANDLORD;
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

    static MaintenanceTicketStatus targetStatusAfterManagerCompletion() {
        return MaintenanceTicketStatus.WAITING_CONFIRMATION;
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
