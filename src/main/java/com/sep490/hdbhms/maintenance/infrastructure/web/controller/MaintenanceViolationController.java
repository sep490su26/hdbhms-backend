package com.sep490.hdbhms.maintenance.infrastructure.web.controller;

import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceLineType;
import com.sep490.hdbhms.billingandpayment.application.service.IssuedInvoiceChargeService;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.InvoiceEntity;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.InvoiceLineEntity;
import com.sep490.hdbhms.file.infrastructure.persistence.entity.FileMetadataEntity;
import com.sep490.hdbhms.file.infrastructure.persistence.jpa.JpaFileMetadataRepository;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.PersonProfileEntity;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.PromotionRole;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.RolePromotionStatus;
import com.sep490.hdbhms.identityandaccess.infrastructure.config.security.UserPrincipal;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaRolePromotionRepository;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaPersonProfileRepository;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaUserRepository;
import com.sep490.hdbhms.maintenance.domain.value_objects.AttachmentPhase;
import com.sep490.hdbhms.maintenance.domain.value_objects.MaintenanceTicketAction;
import com.sep490.hdbhms.maintenance.domain.value_objects.MaintenanceTicketStatus;
import com.sep490.hdbhms.maintenance.domain.value_objects.Priority;
import com.sep490.hdbhms.maintenance.domain.value_objects.TicketScope;
import com.sep490.hdbhms.maintenance.infrastructure.persistence.entity.MaintenanceTicketAttachmentEntity;
import com.sep490.hdbhms.maintenance.infrastructure.persistence.entity.MaintenanceTicketEntity;
import com.sep490.hdbhms.maintenance.infrastructure.persistence.entity.MaintenanceTicketEventEntity;
import com.sep490.hdbhms.maintenance.infrastructure.persistence.jpa.JpaMaintenanceTicketAttachmentRepository;
import com.sep490.hdbhms.maintenance.infrastructure.persistence.jpa.JpaMaintenanceTicketEventRepository;
import com.sep490.hdbhms.maintenance.infrastructure.persistence.jpa.JpaMaintenanceTicketRepository;
import com.sep490.hdbhms.maintenance.infrastructure.web.dto.request.CreateRuleViolationRequest;
import com.sep490.hdbhms.maintenance.infrastructure.web.dto.response.RuleViolationResponse;
import com.sep490.hdbhms.occupancy.domain.value_objects.LeaseStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.OccupantStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.RoomStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.RuleStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.ViolationStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeaseContractEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.PropertyRuleEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RoomEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RuleViolationEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaContractOccupantRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaLeaseContractRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaPropertyRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaPropertyRuleRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRuleViolationRepository;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import com.sep490.hdbhms.shared.utils.AuthUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/maintenance/violations")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MaintenanceViolationController {
    private static final String RESET_WIFI_PASSWORD = "RESET_WIFI_PASSWORD";
    private static final String WIFI_RESET_RULE_CODE = "WIFI_RESET";
    private static final long RESET_WIFI_DEFAULT_FINE = 200_000L;
    private static final int MAX_EVIDENCE_FILES = 3;
    private static final Set<LeaseStatus> ACTIVE_CONTRACT_STATUSES = Set.of(
            LeaseStatus.ACTIVE,
            LeaseStatus.EXPIRING_SOON,
            LeaseStatus.TERMINATION_PENDING
    );

    JpaRoomRepository jpaRoomRepository;
    JpaPropertyRepository jpaPropertyRepository;
    JpaPropertyRuleRepository jpaPropertyRuleRepository;
    JpaRuleViolationRepository jpaRuleViolationRepository;
    JpaLeaseContractRepository jpaLeaseContractRepository;
    JpaContractOccupantRepository jpaContractOccupantRepository;
    JpaMaintenanceTicketRepository jpaMaintenanceTicketRepository;
    JpaMaintenanceTicketAttachmentRepository jpaMaintenanceTicketAttachmentRepository;
    JpaMaintenanceTicketEventRepository jpaMaintenanceTicketEventRepository;
    JpaFileMetadataRepository jpaFileMetadataRepository;
    JpaPersonProfileRepository jpaPersonProfileRepository;
    JpaUserRepository jpaUserRepository;
    JpaRolePromotionRepository jpaRolePromotionRepository;
    IssuedInvoiceChargeService issuedInvoiceChargeService;

    @PostMapping
    @Transactional
    public ApiResponse<RuleViolationResponse> createViolation(@RequestBody CreateRuleViolationRequest request) {
        Role role = requireRole();
        if (role != Role.OWNER && role != Role.MANAGER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ chủ trọ hoặc quản lý được ghi nhận vi phạm.");
        }
        validateRequest(request);
        String violationType = normalizeViolationType(request.getViolationType());

        RoomEntity room = jpaRoomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không tìm thấy phòng."));
        Long propertyId = request.getPropertyId();
        if (room.getProperty() == null || !Objects.equals(room.getProperty().getId(), propertyId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phòng không thuộc cơ sở đã chọn.");
        }
        assertManagerCanAccessProperty(propertyId);
        if (!jpaPropertyRepository.existsById(propertyId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không tìm thấy cơ sở.");
        }

        LeaseContractEntity contract = jpaLeaseContractRepository
                .findFirstByRoom_IdAndStatusInAndDeletedAtIsNullOrderByIdDesc(
                        room.getId(),
                        ACTIVE_CONTRACT_STATUSES.stream().toList()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Phòng chưa có hợp đồng đang hiệu lực, không thể ghi nhận vi phạm cho khách thuê."
                ));
        if (room.getCurrentStatus() != RoomStatus.OCCUPIED && room.getCurrentStatus() != RoomStatus.SOON_VACANT) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Phòng chưa có hợp đồng đang hiệu lực, không thể ghi nhận vi phạm cho khách thuê."
            );
        }
        PersonProfileEntity tenantProfile = resolveTenantProfile(request, contract);
        PropertyRuleEntity rule = findRule(propertyId, violationType);
        long fineAmount = resolveFineAmount(request.getAmount(), rule, violationType);
        List<Long> attachmentIds = validateAttachmentIds(request.getAttachmentIds());

        MaintenanceTicketEntity ticket = createViolationTicket(room, contract, violationType, request.getDescription(), fineAmount);
        attachEvidenceFiles(ticket, attachmentIds);
        RuleViolationEntity violation = createRuleViolation(
                room,
                contract,
                rule,
                tenantProfile,
                request.getOccurredAt() == null ? LocalDate.now() : request.getOccurredAt(),
                request.getDescription().trim(),
                fineAmount,
                attachmentIds
        );

        InvoiceLineEntity invoiceLine = null;
        InvoiceEntity invoice = null;
        IssuedInvoiceChargeService.IssuedChargeResult chargeResult = null;
        boolean includeInMonthlyInvoice = request.getIncludeInMonthlyInvoice() == null || request.getIncludeInMonthlyInvoice();
        if (includeInMonthlyInvoice) {
            chargeResult = issuedInvoiceChargeService.issueMaintenanceCharge(
                    room,
                    contract,
                    InvoiceLineType.VIOLATION_FINE,
                    "Phạt vi phạm nội quy: Tự ý reset mật khẩu modem/wifi",
                    fineAmount,
                    ticket.getId(),
                    jpaUserRepository.getReferenceById(currentUserId())
            );
            invoice = chargeResult.invoice();
            invoiceLine = chargeResult.invoiceLine();
            violation.setInvoice(invoice);
            violation = jpaRuleViolationRepository.save(violation);
        }

        return ApiResponse.<RuleViolationResponse>builder()
                .data(RuleViolationResponse.builder()
                        .id(violation.getId())
                        .ticketId(ticket.getId())
                        .ticketCode(ticket.getTicketCode())
                        .violationType(violationType)
                        .lineType(includeInMonthlyInvoice ? InvoiceLineType.VIOLATION_FINE.name() : null)
                        .amount(fineAmount)
                        .status(ticket.getStatus().name())
                        .billingStatus(includeInMonthlyInvoice ? "PENDING_PAYMENT" : "NO_CHARGE")
                        .billingStatusLabel(includeInMonthlyInvoice ? "Chờ thanh toán" : "Không thu khách")
                        .invoiceId(invoice == null ? null : invoice.getId())
                        .invoiceCode(invoice == null ? null : invoice.getInvoiceCode())
                        .invoiceStatus(invoice == null ? null : invoice.getStatus().name())
                        .invoiceLineId(invoiceLine == null ? null : invoiceLine.getId())
                        .checkoutUrl(chargeResult == null ? null : chargeResult.checkout().checkOutUrl())
                        .providerOrderCode(chargeResult == null ? null : chargeResult.paymentIntent().getProviderOrderCode())
                        .message(includeInMonthlyInvoice
                                ? "Đã ghi nhận vi phạm reset wifi 200.000đ và phát hành hóa đơn cho khách thanh toán."
                                : "Đã ghi nhận vi phạm reset wifi 200.000đ.")
                        .build())
                .build();
    }

    private void validateRequest(CreateRuleViolationRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dữ liệu ghi nhận vi phạm không hợp lệ.");
        }
        if (request.getPropertyId() == null || request.getPropertyId() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn cơ sở.");
        }
        if (request.getRoomId() == null || request.getRoomId() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn phòng.");
        }
        if (request.getDescription() == null || request.getDescription().trim().length() < 10) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng nhập mô tả vi phạm tối thiểu 10 ký tự.");
        }
    }

    private String normalizeViolationType(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!RESET_WIFI_PASSWORD.equals(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Loại vi phạm chưa được hỗ trợ trong MVP.");
        }
        return normalized;
    }

    private PersonProfileEntity resolveTenantProfile(CreateRuleViolationRequest request, LeaseContractEntity contract) {
        if (request.getOccupantId() == null) {
            return null;
        }
        jpaContractOccupantRepository.findFirstByContract_IdAndTenantProfile_IdAndStatus(
                contract.getId(),
                request.getOccupantId(),
                OccupantStatus.ACTIVE
        ).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Người vi phạm không thuộc hợp đồng/phòng này."));
        return jpaPersonProfileRepository.getReferenceById(request.getOccupantId());
    }

    private PropertyRuleEntity findRule(Long propertyId, String violationType) {
        String ruleCode = RESET_WIFI_PASSWORD.equals(violationType) ? WIFI_RESET_RULE_CODE : violationType;
        return jpaPropertyRuleRepository
                .findFirstByProperty_IdAndRuleCodeAndStatus(propertyId, ruleCode, RuleStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Chưa cấu hình nội quy reset wifi cho cơ sở này."
                ));
    }

    private long resolveFineAmount(Long amount, PropertyRuleEntity rule, String violationType) {
        long resolved = amount == null
                ? (rule.getDefaultFineAmount() == null ? RESET_WIFI_DEFAULT_FINE : rule.getDefaultFineAmount())
                : amount;
        if (resolved <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số tiền phạt phải lớn hơn 0.");
        }
        if (RESET_WIFI_PASSWORD.equals(violationType) && resolved <= 0) {
            return RESET_WIFI_DEFAULT_FINE;
        }
        return resolved;
    }

    private List<Long> validateAttachmentIds(List<Long> attachmentIds) {
        List<Long> ids = attachmentIds == null
                ? List.of()
                : attachmentIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.size() > MAX_EVIDENCE_FILES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ được upload tối đa 3 ảnh bằng chứng.");
        }
        if (ids.isEmpty()) {
            return ids;
        }
        List<FileMetadataEntity> files = jpaFileMetadataRepository.findAllById(ids);
        if (files.size() != ids.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Một hoặc nhiều ảnh bằng chứng không tồn tại.");
        }
        for (FileMetadataEntity file : files) {
            String mimeType = file.getMimeType() == null ? "" : file.getMimeType().toLowerCase(Locale.ROOT);
            if (!mimeType.startsWith("image/")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bằng chứng chỉ hỗ trợ file ảnh.");
            }
        }
        return ids;
    }

    private MaintenanceTicketEntity createViolationTicket(
            RoomEntity room,
            LeaseContractEntity contract,
            String violationType,
            String description,
            long fineAmount
    ) {
        MaintenanceTicketEntity ticket = MaintenanceTicketEntity.builder()
                .ticketCode(String.format("#SC-TMP-%d-%d", currentUserId(), System.nanoTime()))
                .property(room.getProperty())
                .room(room)
                .contract(contract)
                .createdBy(jpaUserRepository.getReferenceById(currentUserId()))
                .ticketScope(TicketScope.TENANT_ROOM)
                .priority(Priority.MEDIUM)
                .category("RULE_VIOLATION")
                .title("Vi phạm nội quy: Tự ý reset mật khẩu modem/wifi")
                .description(description.trim() + "\nTiền phạt: " + fineAmount + " đ\nLoại vi phạm: " + violationType)
                .status(MaintenanceTicketStatus.COMPLETED)
                .assignedTo(jpaUserRepository.getReferenceById(currentUserId()))
                .completedAt(LocalDateTime.now())
                .build();
        ticket = jpaMaintenanceTicketRepository.save(ticket);
        ticket.setTicketCode(String.format("#SC-%04d", ticket.getId()));
        ticket = jpaMaintenanceTicketRepository.save(ticket);
        jpaMaintenanceTicketEventRepository.save(MaintenanceTicketEventEntity.builder()
                .ticket(ticket)
                .action(MaintenanceTicketAction.CREATE.name())
                .toStatus(ticket.getStatus().name())
                .note("Ghi nhận vi phạm nội quy reset wifi")
                .createdBy(jpaUserRepository.getReferenceById(currentUserId()))
                .build());
        return ticket;
    }

    private void attachEvidenceFiles(MaintenanceTicketEntity ticket, List<Long> attachmentIds) {
        int sortOrder = 0;
        for (Long fileId : attachmentIds) {
            jpaMaintenanceTicketAttachmentRepository.save(MaintenanceTicketAttachmentEntity.builder()
                    .ticket(ticket)
                    .file(jpaFileMetadataRepository.getReferenceById(fileId))
                    .attachmentPhase(AttachmentPhase.BEFORE)
                    .sortOrder(sortOrder++)
                    .createdByUser(jpaUserRepository.getReferenceById(currentUserId()))
                    .build());
        }
    }

    private RuleViolationEntity createRuleViolation(
            RoomEntity room,
            LeaseContractEntity contract,
            PropertyRuleEntity rule,
            PersonProfileEntity tenantProfile,
            LocalDate violationDate,
            String description,
            long fineAmount,
            List<Long> attachmentIds
    ) {
        return jpaRuleViolationRepository.save(RuleViolationEntity.builder()
                .property(room.getProperty())
                .room(room)
                .contract(contract)
                .tenantProfile(tenantProfile)
                .rule(rule)
                .violationDate(violationDate)
                .description(description)
                .fineAmount(fineAmount)
                .evidenceFile(attachmentIds.isEmpty() ? null : jpaFileMetadataRepository.getReferenceById(attachmentIds.getFirst()))
                .status(ViolationStatus.RECORDED)
                .createdBy(jpaUserRepository.getReferenceById(currentUserId()))
                .build());
    }

    private void assertManagerCanAccessProperty(Long propertyId) {
        if (requireRole() != Role.MANAGER) {
            return;
        }
        if (propertyId == null || !managerPropertyIds().contains(propertyId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền ghi nhận vi phạm cho cơ sở này.");
        }
    }

    private List<Long> managerPropertyIds() {
        return jpaRolePromotionRepository
                .findActivePropertyIds(currentUserId(), PromotionRole.MANAGER, RolePromotionStatus.ACTIVE)
                .stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
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
}
