package com.sep490.hdbhms.billingandpayment.infrastructure.web.controller;

import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep490.hdbhms.billingandpayment.application.port.in.command.ReconcilePaymentCommand;
import com.sep490.hdbhms.billingandpayment.application.port.in.usecase.ReconcilePaymentUseCase;
import com.sep490.hdbhms.billingandpayment.application.port.out.ExternalPaymentPort;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceLineType;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceStatus;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceType;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.PaymentIntentProvider;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.PaymentIntentStatus;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.TransactionProvider;
import com.sep490.hdbhms.billingandpayment.infrastructure.config.PayOSProperties;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.InvoiceEntity;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.InvoiceLineEntity;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.PaymentIntentEntity;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaInvoiceLineRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaInvoiceRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaPaymentAllocationRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaPaymentIntentRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.request.PaymentRequest;
import com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.request.TenantMeterReadingReviewRequest;
import com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.response.TenantInvoiceLineResponse;
import com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.response.TenantInvoiceResponse;
import com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.response.TenantMeterReadingReviewResponse;
import com.sep490.hdbhms.changerequest.application.service.ChangeRequestNotificationService;
import com.sep490.hdbhms.changerequest.domain.model.ChangeRequest;
import com.sep490.hdbhms.changerequest.domain.value_objects.AssignedRole;
import com.sep490.hdbhms.changerequest.domain.value_objects.RequestStatus;
import com.sep490.hdbhms.changerequest.domain.value_objects.RequestType;
import com.sep490.hdbhms.changerequest.domain.value_objects.RequesterRole;
import com.sep490.hdbhms.changerequest.domain.value_objects.TargetType;
import com.sep490.hdbhms.changerequest.infrastructure.persistence.entity.ChangeRequestEntity;
import com.sep490.hdbhms.changerequest.infrastructure.persistence.jpa.JpaChangeRequestRepository;
import com.sep490.hdbhms.file.infrastructure.persistence.entity.FileMetadataEntity;
import com.sep490.hdbhms.file.infrastructure.persistence.jpa.JpaFileMetadataRepository;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.UserEntity;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaUserRepository;
import com.sep490.hdbhms.property.domain.value_objects.MeterReadingReviewStatus;
import com.sep490.hdbhms.property.infrastructure.persistence.entity.MeterReadingEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeaseContractEntity;
import com.sep490.hdbhms.property.infrastructure.persistence.entity.PropertyEntity;
import com.sep490.hdbhms.property.infrastructure.persistence.entity.RoomEntity;
import com.sep490.hdbhms.shared.types.dto.response.ApiResponse;
import com.sep490.hdbhms.shared.utils.AuthUtils;
import com.sep490.hdbhms.shared.utils.RequestCodeBuilder;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StringUtils;
import vn.payos.model.v2.paymentRequests.PaymentLink;
import vn.payos.model.v2.paymentRequests.PaymentLinkStatus;
import vn.payos.model.v2.paymentRequests.Transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/api/v1/tenant/invoices")
public class TenantInvoiceController {
    static final List<RequestStatus> OPEN_REVIEW_STATUSES = List.of(
            RequestStatus.PENDING,
            RequestStatus.UNDER_REVIEW,
            RequestStatus.PROCESSING
    );

    JpaInvoiceRepository jpaInvoiceRepository;
    JpaInvoiceLineRepository jpaInvoiceLineRepository;
    JpaPaymentIntentRepository jpaPaymentIntentRepository;
    JpaPaymentAllocationRepository jpaPaymentAllocationRepository;
    JpaChangeRequestRepository jpaChangeRequestRepository;
    JpaUserRepository jpaUserRepository;
    JpaFileMetadataRepository jpaFileMetadataRepository;
    ObjectMapper objectMapper;
    PayOSProperties payOSProperties;
    ExternalPaymentPort externalPaymentPort;
    ReconcilePaymentUseCase reconcilePaymentUseCase;
    ChangeRequestNotificationService changeRequestNotificationService;

    @GetMapping
    @Transactional
    public ApiResponse<List<TenantInvoiceResponse>> getMyInvoices() {
        Long userId = AuthUtils.getCurrentAuthenticationId();
        EnumSet<InvoiceStatus> visibleStatuses = EnumSet.of(
                InvoiceStatus.ISSUED,
                InvoiceStatus.PARTIALLY_PAID,
                InvoiceStatus.PAID,
                InvoiceStatus.OVERDUE
        );
        List<InvoiceEntity> invoices = jpaInvoiceRepository.findTenantVisibleInvoices(userId, visibleStatuses);
        if (syncPayOSPayments(invoices)) {
            invoices = jpaInvoiceRepository.findTenantVisibleInvoices(userId, visibleStatuses);
        }
        if (ensurePayOSCheckouts(invoices)) {
            invoices = jpaInvoiceRepository.findTenantVisibleInvoices(userId, visibleStatuses);
        }
        return ApiResponse.<List<TenantInvoiceResponse>>builder()
                .data(invoices.stream().map(this::toResponse).toList())
                .build();
    }

    @GetMapping("/electricity-history")
    @Transactional(readOnly = true)
    public ApiResponse<List<TenantInvoiceResponse>> getMyElectricityHistory(
            @RequestParam(required = false) Long contractId
    ) {
        Long userId = AuthUtils.getCurrentAuthenticationId();
        EnumSet<InvoiceStatus> visibleStatuses = EnumSet.of(
                InvoiceStatus.ISSUED,
                InvoiceStatus.PARTIALLY_PAID,
                InvoiceStatus.PAID,
                InvoiceStatus.OVERDUE
        );

        List<TenantInvoiceResponse> history = jpaInvoiceRepository
                .findTenantVisibleInvoices(userId, visibleStatuses)
                .stream()
                .filter(invoice -> contractId == null
                        || invoice.getLeastContract() != null
                        && contractId.equals(invoice.getLeastContract().getId()))
                .map(this::toElectricityHistoryResponse)
                .filter(response -> response.getLines() != null && !response.getLines().isEmpty())
                .toList();

        return ApiResponse.<List<TenantInvoiceResponse>>builder()
                .data(history)
                .build();
    }

    @PostMapping("/{invoiceId}/lines/{lineId}/meter-reading-reviews")
    @Transactional
    public ApiResponse<TenantMeterReadingReviewResponse> createMeterReadingReview(
            @PathVariable Long invoiceId,
            @PathVariable Long lineId,
            @RequestBody TenantMeterReadingReviewRequest request
    ) {
        Long userId = AuthUtils.getCurrentAuthenticationId();
        InvoiceEntity invoice = requireTenantVisibleInvoice(invoiceId, userId);
        InvoiceLineEntity line = jpaInvoiceLineRepository.findById(lineId)
                .orElseThrow(() -> new AppException(ApiErrorCode.MIGRATED_KHONG_TIM_THAY_DONG_HOA_ON));
        if (line.getInvoice() == null || !invoice.getId().equals(line.getInvoice().getId())) {
            throw new AppException(ApiErrorCode.MIGRATED_DONG_HOA_ON_KHONG_THUOC_HOA_ON_A_CHON);
        }
        if (invoice.getInvoiceType() != InvoiceType.UTILITY) {
            throw new AppException(ApiErrorCode.MIGRATED_CHI_HO_TRO_KHIEU_NAI_HOA_ON_IEN_NUOC);
        }
        if (!isReviewableInvoice(invoice)) {
            throw new AppException(ApiErrorCode.MIGRATED_CHI_CO_THE_KHIEU_NAI_HOA_ON_IEN_NUOC_CHUA_THANH_TOAN);
        }
        if (!isUtilityMeterLine(line)) {
            throw new AppException(ApiErrorCode.MIGRATED_CHI_CO_THE_KHIEU_NAI_DONG_IEN_HOAC_NUOC);
        }
        MeterReadingEntity reading = line.getMeterReading();
        if (reading == null) {
            throw new AppException(ApiErrorCode.MIGRATED_DONG_HOA_ON_CHUA_LIEN_KET_CHI_SO_IEN_NUOC);
        }
        BigDecimal reportedValue = request == null ? null : request.reportedCurrentValue();
        if (reportedValue == null) {
            throw new AppException(ApiErrorCode.MIGRATED_VUI_LONG_NHAP_CHI_SO_BAN_CHO_LA_UNG);
        }
        if (reading.getPreviousValue() != null && reportedValue.compareTo(reading.getPreviousValue()) < 0) {
            throw new AppException(ApiErrorCode.MIGRATED_CHI_SO_E_XUAT_KHONG_UOC_NHO_HON_CHI_SO_CU);
        }
        if (jpaChangeRequestRepository.existsByRequestTypeAndTargetTypeAndTargetIdAndStatusIn(
                RequestType.METER_READING_CORRECTION,
                TargetType.METER_READING,
                reading.getId(),
                OPEN_REVIEW_STATUSES
        )) {
            throw new AppException(ApiErrorCode.MIGRATED_CHI_SO_NAY_ANG_CO_KHIEU_NAI_CHO_XU_LY);
        }

        UserEntity requester = jpaUserRepository.findById(userId)
                .orElseThrow(() -> new AppException(ApiErrorCode.MIGRATED_KHONG_TIM_THAY_TAI_KHOAN));
        FileMetadataEntity evidence = request == null || request.evidenceFileId() == null
                ? null
                : jpaFileMetadataRepository.findById(request.evidenceFileId())
                .orElseThrow(() -> new AppException(ApiErrorCode.MIGRATED_KHONG_TIM_THAY_TEP_MINH_CHUNG));

        ChangeRequestEntity created = jpaChangeRequestRepository.save(ChangeRequestEntity.builder()
                .requestCode(RequestCodeBuilder.nextAvailable(
                        RequestType.METER_READING_CORRECTION,
                        reading.getRoom() == null ? null : reading.getRoom().getRoomCode(),
                        LocalDate.now(),
                        jpaChangeRequestRepository::existsByRequestCode
                ))
                .requestType(RequestType.METER_READING_CORRECTION)
                .requester(requester)
                .requesterRole(RequesterRole.TENANT)
                .targetType(TargetType.METER_READING)
                .targetId(reading.getId())
                .title("Khiếu nại chỉ số " + utilityLabel(line.getLineType()) + " hóa đơn " + invoice.getInvoiceCode())
                .description(defaultText(request == null ? null : request.description(), "Khách cho rằng chỉ số điện/nước trên hóa đơn chưa chính xác."))
                .requestPayload(reviewPayload(invoice, line, reading, reportedValue, request == null ? null : request.description()))
                .evidenceFile(evidence)
                .assignedRole(AssignedRole.MANAGER)
                .status(RequestStatus.PENDING)
                .build());

        reading.setReviewStatus(MeterReadingReviewStatus.PENDING);
        reading.setReviewCount((reading.getReviewCount() == null ? 0 : reading.getReviewCount()) + 1);
        changeRequestNotificationService.notifyCreated(toDomain(created));

        return ApiResponse.<TenantMeterReadingReviewResponse>builder()
                .data(new TenantMeterReadingReviewResponse(
                        created.getId(),
                        created.getRequestCode(),
                        created.getStatus().name()
                ))
                .message("Đã gửi khiếu nại chỉ số điện nước.")
                .build();
    }

    private ChangeRequest toDomain(ChangeRequestEntity entity) {
        return ChangeRequest.builder()
                .id(entity.getId())
                .requestCode(entity.getRequestCode())
                .requestType(entity.getRequestType())
                .requesterId(entity.getRequester() == null ? null : entity.getRequester().getId())
                .requesterRole(entity.getRequesterRole())
                .targetType(entity.getTargetType())
                .targetId(entity.getTargetId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .requestPayload(entity.getRequestPayload())
                .evidenceFileId(entity.getEvidenceFile() == null ? null : entity.getEvidenceFile().getId())
                .assignedRole(entity.getAssignedRole())
                .assignedTo(entity.getAssignedTo() == null ? null : entity.getAssignedTo().getId())
                .status(entity.getStatus())
                .build();
    }

    private boolean syncPayOSPayments(List<InvoiceEntity> invoices) {
        boolean synced = false;
        for (InvoiceEntity invoice : invoices) {
            if (invoice.getStatus() == InvoiceStatus.PAID) {
                continue;
            }
            PaymentIntentEntity paymentIntent = jpaPaymentIntentRepository
                    .findFirstByInvoice_IdAndStatusOrderByIdDesc(invoice.getId(), PaymentIntentStatus.PENDING)
                    .orElse(null);
            if (paymentIntent != null && syncPayOSPayment(paymentIntent)) {
                synced = true;
            }
        }
        return synced;
    }

    private boolean ensurePayOSCheckouts(List<InvoiceEntity> invoices) {
        boolean refreshed = false;
        for (InvoiceEntity invoice : invoices) {
            if (!isPayable(invoice)) {
                continue;
            }
            PaymentIntentEntity paymentIntent = jpaPaymentIntentRepository
                    .findFirstByInvoice_IdAndStatusOrderByIdDesc(invoice.getId(), PaymentIntentStatus.PENDING)
                    .orElse(null);
            if (paymentIntent == null
                    || paymentIntent.getProvider() != PaymentIntentProvider.PAYOS
                    || hasUsablePayOSPayload(paymentIntent)) {
                continue;
            }
            try {
                refreshPayOSCheckout(invoice, paymentIntent);
                refreshed = true;
            } catch (RuntimeException exception) {
                log.warn("Failed to create a PayOS payment session for the tenant invoice. invoiceId={}, paymentIntentId={}, message={}",
                        invoice.getId(),
                        paymentIntent.getId(),
                        exception.getMessage());
            }
        }
        return refreshed;
    }

    private boolean isPayable(InvoiceEntity invoice) {
        if (invoice.getRemainingAmount() == null || invoice.getRemainingAmount() <= 0) {
            return false;
        }
        return invoice.getStatus() == InvoiceStatus.ISSUED
                || invoice.getStatus() == InvoiceStatus.PARTIALLY_PAID
                || invoice.getStatus() == InvoiceStatus.OVERDUE;
    }

    private boolean hasUsablePayOSPayload(PaymentIntentEntity paymentIntent) {
        JsonNode payload = parsePayload(paymentIntent.getQrPayload());
        String qrCode = text(payload, "qrCode");
        if (!StringUtils.hasText(qrCode) || looksLikeUrl(qrCode) || isSeedDemoPlaceholder(payload, qrCode)) {
            return false;
        }
        String providerOrderCode = firstNonBlank(text(payload, "providerOrderCode"), paymentIntent.getProviderOrderCode());
        String checkoutUrl = text(payload, "checkoutUrl");
        String paymentLinkId = text(payload, "paymentLinkId");
        return StringUtils.hasText(providerOrderCode)
                && (StringUtils.hasText(checkoutUrl) || StringUtils.hasText(paymentLinkId));
    }

    private boolean isSeedDemoPlaceholder(JsonNode payload, String qrCode) {
        String source = text(payload, "source");
        return "seed-demo-v44".equals(source)
                || (StringUtils.hasText(qrCode) && qrCode.contains("pay.payos.vn/web/seed-"));
    }

    private boolean looksLikeUrl(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        String normalized = value.trim().toLowerCase();
        return normalized.startsWith("http://") || normalized.startsWith("https://");
    }

    private void refreshPayOSCheckout(InvoiceEntity invoice, PaymentIntentEntity paymentIntent) {
        LocalDateTime expiresAt = usableExpiresAt(paymentIntent.getExpiresAt());
        Long amount = paymentIntent.getAmount() != null && paymentIntent.getAmount() > 0
                ? paymentIntent.getAmount()
                : invoice.getRemainingAmount();
        String description = payOSDescription(paymentIntent, invoice);
        com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.response.PaymentIntent checkout =
                externalPaymentPort.createCheckoutRequest(new PaymentRequest(
                        paymentIntent.getId(),
                        amount,
                        description,
                        expiresAt
                ));
        paymentIntent.setProviderOrderCode(checkout.providerOrderCode());
        paymentIntent.setPaymentContent(description);
        paymentIntent.setAmount(amount);
        paymentIntent.setExpiresAt(checkout.expiresAt() == null ? expiresAt : checkout.expiresAt());
        paymentIntent.setQrPayload(toCheckoutPayload(checkout, paymentIntent));
        jpaPaymentIntentRepository.save(paymentIntent);
    }

    private LocalDateTime usableExpiresAt(LocalDateTime currentExpiresAt) {
        LocalDateTime minimum = LocalDateTime.now().plusMinutes(5);
        if (currentExpiresAt == null || currentExpiresAt.isBefore(minimum)) {
            return LocalDateTime.now().plusDays(7);
        }
        return currentExpiresAt;
    }

    private String payOSDescription(PaymentIntentEntity paymentIntent, InvoiceEntity invoice) {
        String value = firstNonBlank(paymentIntent.getPaymentContent(), invoice.getInvoiceCode(), "INV " + invoice.getId());
        value = value.replaceAll("[^A-Za-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
        if (!StringUtils.hasText(value)) {
            value = "INV " + invoice.getId();
        }
        return value.length() <= 25 ? value : value.substring(0, 25).trim();
    }

    private String toCheckoutPayload(
            com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.response.PaymentIntent checkout,
            PaymentIntentEntity paymentIntent
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("checkoutUrl", checkout.checkOutUrl());
        payload.put("qrCode", checkout.qrCode());
        payload.put("providerOrderCode", checkout.providerOrderCode());
        payload.put("paymentLinkId", checkout.paymentLinkId());
        payload.put("bankBin", checkout.bankBin());
        payload.put("bankShortName", checkout.bankShortName());
        payload.put("accountNumber", checkout.accountNumber());
        payload.put("accountName", checkout.accountName());
        payload.put("transferDescription", checkout.transferDescription());
        payload.put("amount", paymentIntent.getAmount());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new AppException(ApiErrorCode.UNDEFINED, exception);
        }
    }

    private boolean syncPayOSPayment(PaymentIntentEntity paymentIntent) {
        if (paymentIntent.getProvider() != PaymentIntentProvider.PAYOS
                || !StringUtils.hasText(paymentIntent.getProviderOrderCode())) {
            return false;
        }
        try {
            PaymentLink paymentLink = payOSProperties.payOS().paymentRequests().get(paymentIntent.getProviderOrderCode());
            if (paymentLink == null || paymentLink.getStatus() != PaymentLinkStatus.PAID) {
                return false;
            }
            Transaction transaction = firstTransaction(paymentLink);
            Long amount = transaction != null && transaction.getAmount() != null
                    ? transaction.getAmount()
                    : paymentLink.getAmountPaid();
            if (amount == null || amount <= 0) {
                return false;
            }
            reconcilePaymentUseCase.execute(ReconcilePaymentCommand.builder()
                    .paymentIntentId(paymentIntent.getId())
                    .provider(TransactionProvider.PAYOS)
                    .providerTransactionId(resolveProviderTransactionId(paymentLink, transaction))
                    .amount(amount)
                    .content(transaction == null ? paymentIntent.getPaymentContent() : transaction.getDescription())
                    .payerName(transaction == null ? null : transaction.getCounterAccountName())
                    .payerAccount(transaction == null ? null : transaction.getCounterAccountNumber())
                    .transactionTime(parseTransactionDateTime(transaction == null ? null : transaction.getTransactionDateTime()))
                    .rawPayload(objectMapper.writeValueAsString(paymentLink))
                    .build());
            return true;
        } catch (vn.payos.exception.APIException exception) {
            log.debug("No PayOS payment link is available to synchronize the tenant invoice. paymentIntentId={}, providerOrderCode={}, message={}",
                    paymentIntent.getId(), paymentIntent.getProviderOrderCode(), exception.getMessage());
            return false;
        } catch (Exception exception) {
            log.warn("Failed to synchronize the tenant invoice PayOS payment. paymentIntentId={}, providerOrderCode={}, message={}",
                    paymentIntent.getId(), paymentIntent.getProviderOrderCode(), exception.getMessage(), exception);
            return false;
        }
    }

    private Transaction firstTransaction(PaymentLink paymentLink) {
        if (paymentLink.getTransactions() == null || paymentLink.getTransactions().isEmpty()) {
            return null;
        }
        return paymentLink.getTransactions().getFirst();
    }

    private String resolveProviderTransactionId(PaymentLink paymentLink, Transaction transaction) {
        if (transaction != null && StringUtils.hasText(transaction.getReference())) {
            return transaction.getReference();
        }
        if (StringUtils.hasText(paymentLink.getId())) {
            return "PAYOS-INVOICE-POLL-" + paymentLink.getOrderCode() + "-" + paymentLink.getId();
        }
        return "PAYOS-INVOICE-POLL-" + paymentLink.getOrderCode();
    }

    private LocalDateTime parseTransactionDateTime(String value) {
        if (!StringUtils.hasText(value)) {
            return LocalDateTime.now();
        }
        for (DateTimeFormatter formatter : List.of(
                DateTimeFormatter.ISO_LOCAL_DATE_TIME,
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
        )) {
            try {
                return LocalDateTime.parse(value, formatter);
            } catch (RuntimeException ignored) {
            }
        }
        return LocalDateTime.now();
    }

    private TenantInvoiceResponse toResponse(InvoiceEntity invoice) {
        RoomEntity room = invoice.getRoom();
        PropertyEntity property = room == null ? invoice.getProperty() : room.getProperty();
        LeaseContractEntity contract = invoice.getLeastContract();
        List<TenantInvoiceLineResponse> lines = jpaInvoiceLineRepository
                .findByInvoice_IdOrderByIdAsc(invoice.getId())
                .stream()
                .map(this::toLineResponse)
                .toList();
        return toInvoiceResponse(invoice, lines);
    }

    private TenantInvoiceResponse toElectricityHistoryResponse(InvoiceEntity invoice) {
        List<TenantInvoiceLineResponse> lines = jpaInvoiceLineRepository
                .findByInvoice_IdOrderByIdAsc(invoice.getId())
                .stream()
                .filter(this::isElectricityMeterLine)
                .map(this::toLineResponse)
                .toList();
        TenantInvoiceResponse response = toInvoiceResponse(invoice, lines);
        return response;
    }

    private TenantInvoiceResponse toInvoiceResponse(
            InvoiceEntity invoice,
            List<TenantInvoiceLineResponse> lines
    ) {
        RoomEntity room = invoice.getRoom();
        PropertyEntity property = room == null ? invoice.getProperty() : room.getProperty();
        LeaseContractEntity contract = invoice.getLeastContract();
        PaymentInfo paymentInfo = paymentInfo(invoice);

        return TenantInvoiceResponse.builder()
                .id(invoice.getId())
                .invoiceCode(invoice.getInvoiceCode())
                .invoiceType(invoice.getInvoiceType() == null ? null : invoice.getInvoiceType().name())
                .billingPeriod(invoice.getBillingPeriod())
                .status(invoice.getStatus() == null ? null : invoice.getStatus().name())
                .propertyId(property == null ? null : property.getId())
                .propertyName(property == null ? null : property.getName())
                .roomId(room == null ? null : room.getId())
                .roomCode(room == null ? null : room.getRoomCode())
                .contractId(contract == null ? null : contract.getId())
                .contractCode(contract == null ? null : contract.getContractCode())
                .issueDate(invoice.getIssueDate())
                .dueDate(invoice.getDueDate())
                .issuedAt(invoice.getIssuedAt())
                .paidAt(resolvePaidAt(invoice))
                .subtotalAmount(invoice.getSubtotalAmount())
                .discountAmount(invoice.getDiscountAmount())
                .totalAmount(invoice.getTotalAmount())
                .paidAmount(invoice.getPaidAmount())
                .remainingAmount(invoice.getRemainingAmount())
                .paymentIntentId(paymentInfo.paymentIntentId())
                .checkoutUrl(paymentInfo.checkoutUrl())
                .qrCode(paymentInfo.qrCode())
                .qrPayload(null)
                .providerOrderCode(paymentInfo.providerOrderCode())
                .paymentLinkId(paymentInfo.paymentLinkId())
                .bankBin(paymentInfo.bankBin())
                .bankShortName(paymentInfo.bankShortName())
                .accountNumber(paymentInfo.accountNumber())
                .accountName(paymentInfo.accountName())
                .transferDescription(paymentInfo.transferDescription())
                .hasOpenMeterReadingReview(lines.stream().anyMatch(line -> line.getOpenReviewId() != null))
                .lines(lines)
                .build();
    }

    private LocalDateTime resolvePaidAt(InvoiceEntity invoice) {
        if (invoice.getStatus() != InvoiceStatus.PAID) {
            return null;
        }
        return jpaPaymentAllocationRepository.findLatestTransactionTimeByInvoiceId(invoice.getId())
                .map(instant -> LocalDateTime.ofInstant(instant, ZoneId.systemDefault()))
                .orElse(invoice.getUpdatedAt());
    }

    private PaymentInfo paymentInfo(InvoiceEntity invoice) {
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            return PaymentInfo.empty();
        }
        if (invoice.getStatus() != InvoiceStatus.ISSUED
                && invoice.getStatus() != InvoiceStatus.PARTIALLY_PAID
                && invoice.getStatus() != InvoiceStatus.OVERDUE) {
            return PaymentInfo.empty();
        }
        return jpaPaymentIntentRepository
                .findFirstByInvoice_IdAndStatusOrderByIdDesc(invoice.getId(), PaymentIntentStatus.PENDING)
                .map(this::toPaymentInfo)
                .orElseGet(PaymentInfo::empty);
    }

    private PaymentInfo toPaymentInfo(PaymentIntentEntity paymentIntent) {
        JsonNode payload = parsePayload(paymentIntent.getQrPayload());
        return new PaymentInfo(
                paymentIntent.getId(),
                text(payload, "checkoutUrl"),
                text(payload, "qrCode"),
                text(payload, "qrPayload"),
                firstNonBlank(text(payload, "providerOrderCode"), paymentIntent.getProviderOrderCode()),
                text(payload, "paymentLinkId"),
                text(payload, "bankBin"),
                text(payload, "bankShortName"),
                text(payload, "accountNumber"),
                text(payload, "accountName"),
                text(payload, "transferDescription")
        );
    }

    private JsonNode parsePayload(String value) {
        if (value == null || value.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(value);
        } catch (Exception exception) {
            return objectMapper.createObjectNode();
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private TenantInvoiceLineResponse toLineResponse(InvoiceLineEntity line) {
        long amount = line.getAmount() != null
                ? line.getAmount()
                : (long) (line.getQuantity() == null ? 0 : line.getQuantity()) * (line.getUnitPrice() == null ? 0 : line.getUnitPrice());
        MeterReadingEntity reading = line.getMeterReading();
        ChangeRequestEntity openReview = reading == null ? null : findOpenReview(reading.getId());
        return TenantInvoiceLineResponse.builder()
                .id(line.getId())
                .lineType(line.getLineType() == null ? null : line.getLineType().name())
                .description(line.getDescription())
                .quantity(line.getQuantity())
                .unitPrice(line.getUnitPrice())
                .amount(amount)
                .sourceType(line.getSourceType())
                .sourceId(line.getSourceId())
                .meterReadingId(reading == null ? null : reading.getId())
                .meterType(reading == null || reading.getMeter() == null || reading.getMeter().getMeterType() == null
                        ? null
                        : reading.getMeter().getMeterType().name())
                .readingPeriod(reading == null ? null : reading.getReadingPeriod())
                .previousValue(reading == null ? null : reading.getPreviousValue())
                .currentValue(reading == null ? null : reading.getCurrentValue())
                .usageAmount(reading == null ? null : reading.getUsageAmount())
                .readingDate(reading == null ? null : reading.getReadingDate())
                .photoFileId(reading == null || reading.getPhotoFile() == null ? null : reading.getPhotoFile().getId())
                .reviewStatus(resolveReviewStatus(reading, openReview))
                .reviewCount(reading == null ? 0 : reading.getReviewCount())
                .openReviewId(openReview == null ? null : openReview.getId())
                .canComplain(canComplain(line, reading, openReview))
                .build();
    }

    private InvoiceEntity requireTenantVisibleInvoice(Long invoiceId, Long userId) {
        return jpaInvoiceRepository.findTenantVisibleInvoices(userId, EnumSet.of(
                        InvoiceStatus.ISSUED,
                        InvoiceStatus.PARTIALLY_PAID,
                        InvoiceStatus.PAID,
                        InvoiceStatus.OVERDUE
                ))
                .stream()
                .filter(invoice -> invoice.getId().equals(invoiceId))
                .findFirst()
                .orElseThrow(() -> new AppException(ApiErrorCode.MIGRATED_KHONG_TIM_THAY_HOA_ON));
    }

    private boolean isReviewableInvoice(InvoiceEntity invoice) {
        if (invoice == null) {
            return false;
        }
        boolean issued = invoice.getStatus() == InvoiceStatus.ISSUED || invoice.getStatus() == InvoiceStatus.OVERDUE;
        return issued && (invoice.getPaidAmount() == null || invoice.getPaidAmount() == 0L);
    }

    private boolean isUtilityMeterLine(InvoiceLineEntity line) {
        return line != null && (line.getLineType() == InvoiceLineType.ELECTRICITY || line.getLineType() == InvoiceLineType.WATER);
    }

    private boolean isElectricityMeterLine(InvoiceLineEntity line) {
        return line != null && line.getLineType() == InvoiceLineType.ELECTRICITY;
    }

    private ChangeRequestEntity findOpenReview(Long meterReadingId) {
        return jpaChangeRequestRepository
                .findFirstByRequestTypeAndTargetTypeAndTargetIdAndStatusInOrderByCreatedAtDesc(
                        RequestType.METER_READING_CORRECTION,
                        TargetType.METER_READING,
                        meterReadingId,
                        OPEN_REVIEW_STATUSES
                )
                .orElse(null);
    }

    private String resolveReviewStatus(MeterReadingEntity reading, ChangeRequestEntity openReview) {
        if (openReview != null && openReview.getStatus() != null) {
            return openReview.getStatus().name();
        }
        if (reading == null || reading.getReviewStatus() == null) {
            return MeterReadingReviewStatus.NONE.name();
        }
        return reading.getReviewStatus().name();
    }

    private boolean canComplain(InvoiceLineEntity line, MeterReadingEntity reading, ChangeRequestEntity openReview) {
        return reading != null
                && openReview == null
                && isUtilityMeterLine(line)
                && line.getInvoice() != null
                && line.getInvoice().getInvoiceType() == InvoiceType.UTILITY
                && isReviewableInvoice(line.getInvoice());
    }

    private String reviewPayload(
            InvoiceEntity invoice,
            InvoiceLineEntity line,
            MeterReadingEntity reading,
            BigDecimal reportedValue,
            String description
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("invoiceId", invoice.getId());
        payload.put("invoiceCode", invoice.getInvoiceCode());
        payload.put("invoiceLineId", line.getId());
        payload.put("lineType", line.getLineType() == null ? null : line.getLineType().name());
        payload.put("roomId", invoice.getRoom() == null ? null : invoice.getRoom().getId());
        payload.put("roomCode", invoice.getRoom() == null ? null : invoice.getRoom().getRoomCode());
        payload.put("billingPeriod", invoice.getBillingPeriod());
        payload.put("meterReadingId", reading.getId());
        payload.put("meterType", reading.getMeter() == null || reading.getMeter().getMeterType() == null ? null : reading.getMeter().getMeterType().name());
        payload.put("previousValue", reading.getPreviousValue());
        payload.put("currentValue", reading.getCurrentValue());
        payload.put("usageAmount", reading.getUsageAmount());
        payload.put("reportedCurrentValue", reportedValue);
        payload.put("lineAmount", line.getAmount());
        payload.put("unitPrice", line.getUnitPrice());
        payload.put("quantity", line.getQuantity());
        payload.put("description", description);
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception exception) {
            throw new AppException(ApiErrorCode.MIGRATED_KHONG_THE_LUU_DU_LIEU_KHIEU_NAI);
        }
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String utilityLabel(InvoiceLineType lineType) {
        if (lineType == InvoiceLineType.ELECTRICITY) {
            return "điện";
        }
        if (lineType == InvoiceLineType.WATER) {
            return "nước";
        }
        return "điện nước";
    }

    private record PaymentInfo(
            Long paymentIntentId,
            String checkoutUrl,
            String qrCode,
            String qrPayload,
            String providerOrderCode,
            String paymentLinkId,
            String bankBin,
            String bankShortName,
            String accountNumber,
            String accountName,
            String transferDescription
    ) {
        static PaymentInfo empty() {
            return new PaymentInfo(null, null, null, null, null, null, null, null, null, null, null);
        }
    }
}
