package com.sep490.hdbhms.maintenance.infrastructure.web.controller;

import com.sep490.hdbhms.shared.exception.AppException;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;

import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceLineType;
import com.sep490.hdbhms.billingandpayment.application.service.IssuedInvoiceChargeService;
import com.sep490.hdbhms.billingandpayment.application.service.ScheduledBillingChargeService;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.InvoiceEntity;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.InvoiceLineEntity;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.PendingBillingChargeEntity;
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
import com.sep490.hdbhms.property.domain.value_objects.RoomStatus;
import com.sep490.hdbhms.property.domain.value_objects.RuleStatus;
import com.sep490.hdbhms.property.domain.value_objects.ViolationStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeaseContractEntity;
import com.sep490.hdbhms.property.infrastructure.persistence.entity.PropertyRuleEntity;
import com.sep490.hdbhms.property.infrastructure.persistence.entity.RoomEntity;
import com.sep490.hdbhms.property.infrastructure.persistence.entity.RuleViolationEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaContractOccupantRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaLeaseContractRepository;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaPropertyRepository;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaPropertyRuleRepository;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaRoomRepository;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaRuleViolationRepository;
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
    ScheduledBillingChargeService scheduledBillingChargeService;

    @PostMapping
    @Transactional
    public ApiResponse<RuleViolationResponse> createViolation(@RequestBody CreateRuleViolationRequest request) {
        Role role = requireRole();
        if (role != Role.OWNER && role != Role.MANAGER) {
            throw new AppException(ApiErrorCode.FORBIDDEN_OPERATION);
        }
        validateRequest(request);
        String ruleCode = normalizeViolationType(request.getViolationType());

        RoomEntity room = jpaRoomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new AppException(ApiErrorCode.INVALID_REQUEST));
        Long propertyId = request.getPropertyId();
        if (room.getProperty() == null || !Objects.equals(room.getProperty().getId(), propertyId)) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        assertManagerCanAccessProperty(propertyId);
        if (!jpaPropertyRepository.existsById(propertyId)) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }

        LeaseContractEntity contract = jpaLeaseContractRepository
                .findFirstByRoom_IdAndStatusInAndDeletedAtIsNullOrderByIdDesc(
                        room.getId(),
                        ACTIVE_CONTRACT_STATUSES.stream().toList()
                )
                .orElseThrow(() -> new AppException(ApiErrorCode.INVALID_REQUEST));
        if (room.getCurrentStatus() != RoomStatus.OCCUPIED && room.getCurrentStatus() != RoomStatus.SOON_VACANT) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        PersonProfileEntity tenantProfile = resolveTenantProfile(request, contract);
        PropertyRuleEntity rule = findRule(propertyId, ruleCode);
        long fineAmount = resolveFineAmount(request.getAmount(), rule, ruleCode);
        List<Long> attachmentIds = validateAttachmentIds(request.getAttachmentIds());

        MaintenanceTicketEntity ticket = createViolationTicket(room, contract, rule, request.getDescription(), fineAmount);
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
        if (request.getCollectionMethod() == null || request.getCollectionMethod().isBlank()) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        String collectionMethod = normalizeCollectionMethod(request.getCollectionMethod());
        boolean billNow = "BILL_NOW".equals(collectionMethod);
        boolean monthlyScheduled = "MONTHLY_SCHEDULED".equals(collectionMethod);
        boolean createDraftInvoice = billNow;
        PendingBillingChargeEntity pendingCharge = null;
        if (billNow) {
            IssuedInvoiceChargeService.DraftChargeResult chargeResult = issuedInvoiceChargeService.createMaintenanceChargeDraft(
                    room,
                    contract,
                    InvoiceLineType.VIOLATION_FINE,
                    "Phạt vi phạm nội quy: " + ruleTitle(rule),
                    fineAmount,
                    ticket.getId(),
                    jpaUserRepository.getReferenceById(currentUserId())
            );
            invoice = chargeResult.invoice();
            invoiceLine = chargeResult.invoiceLine();
            violation.setInvoice(invoice);
            violation = jpaRuleViolationRepository.save(violation);
        } else if (monthlyScheduled) {
            pendingCharge = scheduledBillingChargeService.scheduleCharge(
                    room,
                    contract,
                    InvoiceLineType.VIOLATION_FINE,
                    "Phạt vi phạm nội quy: " + ruleTitle(rule),
                    fineAmount,
                    IssuedInvoiceChargeService.SOURCE_MAINTENANCE_TICKET,
                    ticket.getId(),
                    request.getBillingPeriod(),
                    jpaUserRepository.getReferenceById(currentUserId())
            );
        }

        return ApiResponse.<RuleViolationResponse>builder()
                .message("Tạo biên bản vi phạm thành công")
                .data(RuleViolationResponse.builder()
                        .id(violation.getId())
                        .ticketId(ticket.getId())
                        .ticketCode(ticket.getTicketCode())
                        .violationType(rule.getRuleCode())
                        .lineType(billNow || monthlyScheduled ? InvoiceLineType.VIOLATION_FINE.name() : null)
                        .amount(fineAmount)
                        .status(ticket.getStatus().name())
                        .billingStatus(billNow ? "DRAFT" : monthlyScheduled ? "SCHEDULED" : "NO_CHARGE")
                        .billingStatusLabel(billNow
                                ? "Chờ phát hành"
                                : monthlyScheduled ? "Đã lên lịch gộp hóa đơn đầu tháng" : "Không thu khách")
                        .invoiceId(invoice == null ? null : invoice.getId())
                        .invoiceCode(invoice == null ? null : invoice.getInvoiceCode())
                        .invoiceStatus(invoice == null ? null : invoice.getStatus().name())
                        .invoiceLineId(invoiceLine == null ? null : invoiceLine.getId())
                        .checkoutUrl(null)
                        .providerOrderCode(null)
                        .message(billNow
                                ? "Đã ghi nhận vi phạm \"" + ruleTitle(rule) + "\" và tạo hóa đơn nháp. Khách thuê chỉ thấy sau khi phát hành."
                                : monthlyScheduled
                                ? "Đã ghi nhận vi phạm \"" + ruleTitle(rule) + "\" và lên lịch gộp hóa đơn đầu tháng."
                                : "Đã ghi nhận vi phạm \"" + ruleTitle(rule) + "\", chưa thu khách.")
                        .build())
                .build();
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

    @SuppressWarnings("unused")
    private boolean shouldCreateDraftInvoice(CreateRuleViolationRequest request) {
        String method = request.getCollectionMethod() == null
                ? ""
                : request.getCollectionMethod().trim().toUpperCase(Locale.ROOT);
        if (Set.of("RECORD_ONLY", "NO_CHARGE", "NONE").contains(method)) {
            return false;
        }
        if (Set.of("MONTHLY_DRAFT", "BILL_NOW", "IMMEDIATE_DRAFT", "CREATE_DRAFT").contains(method)) {
            return true;
        }
        return request.getIncludeInMonthlyInvoice() == null || request.getIncludeInMonthlyInvoice();
    }

    private void validateRequest(CreateRuleViolationRequest request) {
        if (request == null) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        if (request.getViolationType() == null || request.getViolationType().isBlank()) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        if (request.getPropertyId() == null || request.getPropertyId() <= 0) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        if (request.getRoomId() == null || request.getRoomId() <= 0) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        if (request.getDescription() == null || request.getDescription().trim().length() < 10) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
    }

    private String normalizeViolationType(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (RESET_WIFI_PASSWORD.equals(normalized)) {
            return WIFI_RESET_RULE_CODE;
        }
        normalized = normalized.replaceAll("[^A-Z0-9_\\-]", "_").replaceAll("_+", "_");
        if (normalized.isBlank()) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        if (normalized.length() > 50) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
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
        ).orElseThrow(() -> new AppException(ApiErrorCode.INVALID_REQUEST));
        return jpaPersonProfileRepository.getReferenceById(request.getOccupantId());
    }

    private PropertyRuleEntity findRule(Long propertyId, String ruleCode) {
        return jpaPropertyRuleRepository
                .findFirstByProperty_IdAndRuleCodeAndStatus(propertyId, ruleCode, RuleStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ApiErrorCode.INVALID_REQUEST));
    }

    private long resolveFineAmount(Long amount, PropertyRuleEntity rule, String ruleCode) {
        long defaultFine = rule.getDefaultFineAmount() == null ? 0 : rule.getDefaultFineAmount();
        if (defaultFine <= 0 && WIFI_RESET_RULE_CODE.equals(ruleCode)) {
            defaultFine = RESET_WIFI_DEFAULT_FINE;
        }
        long resolved = amount == null ? defaultFine : amount;
        if (resolved <= 0) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        return resolved;
    }

    private List<Long> validateAttachmentIds(List<Long> attachmentIds) {
        List<Long> ids = attachmentIds == null
                ? List.of()
                : attachmentIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.size() > MAX_EVIDENCE_FILES) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        if (ids.isEmpty()) {
            return ids;
        }
        List<FileMetadataEntity> files = jpaFileMetadataRepository.findAllById(ids);
        if (files.size() != ids.size()) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        for (FileMetadataEntity file : files) {
            String mimeType = file.getMimeType() == null ? "" : file.getMimeType().toLowerCase(Locale.ROOT);
            if (!mimeType.startsWith("image/")) {
                throw new AppException(ApiErrorCode.INVALID_REQUEST);
            }
        }
        return ids;
    }

    private MaintenanceTicketEntity createViolationTicket(
            RoomEntity room,
            LeaseContractEntity contract,
            PropertyRuleEntity rule,
            String description,
            long fineAmount
    ) {
        String title = ruleTitle(rule);
        MaintenanceTicketEntity ticket = MaintenanceTicketEntity.builder()
                .ticketCode(String.format("#SC-TMP-%d-%d", currentUserId(), System.nanoTime()))
                .property(room.getProperty())
                .room(room)
                .contract(contract)
                .createdBy(jpaUserRepository.getReferenceById(currentUserId()))
                .ticketScope(TicketScope.TENANT_ROOM)
                .category("RULE_VIOLATION")
                .title("Vi phạm nội quy: " + title)
                .description(description.trim() + "\nTiền phạt: " + fineAmount + " đ\nNội quy vi phạm: " + title)
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
                .note("Ghi nhận vi phạm nội quy: " + title)
                .createdBy(jpaUserRepository.getReferenceById(currentUserId()))
                .build());
        return ticket;
    }

    private String ruleTitle(PropertyRuleEntity rule) {
        String title = rule == null ? "" : rule.getTitle();
        if (title == null || title.isBlank()) {
            return rule == null ? "Nội quy" : rule.getRuleCode();
        }
        return title.trim();
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
            throw new AppException(ApiErrorCode.FORBIDDEN_OPERATION);
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
            throw new AppException(ApiErrorCode.UNAUTHENTICATED);
        }
        return principal.getRole();
    }

    private Long currentUserId() {
        Long userId = AuthUtils.getCurrentAuthenticationId();
        if (userId == null) {
            throw new AppException(ApiErrorCode.UNAUTHENTICATED);
        }
        return userId;
    }
}
