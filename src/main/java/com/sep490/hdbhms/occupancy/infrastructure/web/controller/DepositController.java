package com.sep490.hdbhms.occupancy.infrastructure.web.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep490.hdbhms.billingandpayment.application.port.in.command.ReconcilePaymentCommand;
import com.sep490.hdbhms.billingandpayment.application.port.in.usecase.ReconcilePaymentUseCase;
import com.sep490.hdbhms.billingandpayment.domain.model.PaymentIntent;
import com.sep490.hdbhms.billingandpayment.domain.valueObjects.DepositAgreementStatus;
import com.sep490.hdbhms.billingandpayment.domain.valueObjects.PaymentIntentProvider;
import com.sep490.hdbhms.billingandpayment.domain.valueObjects.PaymentIntentStatus;
import com.sep490.hdbhms.billingandpayment.domain.valueObjects.TransactionProvider;
import com.sep490.hdbhms.billingandpayment.application.port.out.PaymentIntentRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.config.PayOSProperties;
import com.sep490.hdbhms.occupancy.application.service.DepositContractDocumentService;
import com.sep490.hdbhms.occupancy.application.service.DepositPaymentExpiryService;
import com.sep490.hdbhms.occupancy.application.service.RoomCommitmentChecker;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.BookRoomUseCase;
import com.sep490.hdbhms.occupancy.application.port.out.DepositAgreementRepository;
import com.sep490.hdbhms.occupancy.application.port.out.EarlyCancelRoomHoldTaskPort;
import com.sep490.hdbhms.occupancy.application.port.out.RoomHoldRepository;
import com.sep490.hdbhms.occupancy.application.port.out.RoomRepository;
import com.sep490.hdbhms.occupancy.domain.model.DepositAgreement;
import com.sep490.hdbhms.occupancy.domain.model.Room;
import com.sep490.hdbhms.occupancy.domain.model.RoomHold;
import com.sep490.hdbhms.occupancy.domain.valueObjects.RoomHoldStatus;
import com.sep490.hdbhms.occupancy.domain.valueObjects.RoomStatus;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.DepositContractPreviewRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.SendDepositFormRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.DepositCheckoutResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.DepositContractPreviewResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.DepositPaymentStatusResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.DepositRoomHoldStatusResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.mapper.RoomWebMapper;
import com.sep490.hdbhms.file.infrastructure.web.dto.response.FileDataResponse;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import vn.payos.model.v2.paymentRequests.PaymentLink;
import vn.payos.model.v2.paymentRequests.PaymentLinkStatus;
import vn.payos.model.v2.paymentRequests.Transaction;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/deposit")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DepositController {
    private static final int MAX_DEPOSIT_SCHEDULE_DAYS = 14;

    RoomWebMapper roomWebMapper;
    BookRoomUseCase bookRoomUseCase;
    RoomRepository roomRepository;
    RoomHoldRepository roomHoldRepository;
    PaymentIntentRepository paymentIntentRepository;
    DepositAgreementRepository depositAgreementRepository;
    EarlyCancelRoomHoldTaskPort earlyCancelRoomHoldTaskPort;
    DepositContractDocumentService depositContractDocumentService;
    DepositPaymentExpiryService depositPaymentExpiryService;
    RoomCommitmentChecker roomCommitmentChecker;
    ReconcilePaymentUseCase reconcilePaymentUseCase;
    PayOSProperties payOSProperties;
    ObjectMapper objectMapper;

    @PostMapping("/checkout")
    public ApiResponse<DepositCheckoutResponse> bookRoom(
            @Valid @RequestPart("metadata") SendDepositFormRequest request,
            @RequestPart("idFrontFile") MultipartFile idFrontFile,
            @RequestPart("idBackFile") MultipartFile idBackFile,
            @RequestPart("portraitFile") MultipartFile portraitFile
    ) {
        log.info("{}", request.toString());
        PaymentIntent paymentIntent = bookRoomUseCase.initDepositForm(
                roomWebMapper.toCommand(request, idFrontFile, idBackFile, portraitFile)
        );
        return ApiResponse.<DepositCheckoutResponse>builder()
                .data(toCheckoutResponse(paymentIntent))
                .build();
    }

    @PostMapping("/contracts/preview")
    public ApiResponse<DepositContractPreviewResponse> previewDepositContract(
            @Valid @RequestBody DepositContractPreviewRequest request
    ) {
        return ApiResponse.<DepositContractPreviewResponse>builder()
                .data(depositContractDocumentService.preview(request))
                .build();
    }

    @GetMapping("/rooms/{roomIdentifier}/hold-status")
    public ApiResponse<DepositRoomHoldStatusResponse> getRoomHoldStatus(
            @PathVariable String roomIdentifier,
            @RequestParam(required = false) LocalDate expectedMoveInDate,
            @RequestParam(required = false) LocalDate expectedLeaseSignDate
    ) {
        return ApiResponse.<DepositRoomHoldStatusResponse>builder()
                .data(resolveRoomHoldStatus(roomIdentifier, expectedMoveInDate, expectedLeaseSignDate))
                .build();
    }

    @GetMapping("/payments/{paymentIntentId}/status")
    public ApiResponse<DepositPaymentStatusResponse> getDepositPaymentStatus(@PathVariable Long paymentIntentId) {
        PaymentIntent paymentIntent = paymentIntentRepository.findById(paymentIntentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy phiên thanh toán."));
        paymentIntent = syncPayOSPaymentIfPaid(paymentIntent);
        DepositAgreement depositAgreement = paymentIntent.getDepositAgreementId() == null
                ? null
                : depositAgreementRepository.findById(paymentIntent.getDepositAgreementId()).orElse(null);
        Room room = depositAgreement == null
                ? null
                : roomRepository.findById(depositAgreement.getRoomId()).orElse(null);

        return ApiResponse.<DepositPaymentStatusResponse>builder()
                .data(DepositPaymentStatusResponse.builder()
                        .paymentIntentId(paymentIntent.getId())
                        .status(paymentIntent.getStatus())
                        .depositStatus(depositAgreement == null ? null : depositAgreement.getStatus())
                        .roomStatus(room == null ? null : room.getCurrentStatus())
                        .expiresAt(paymentIntent.getExpiresAt())
                        .paidAt(depositAgreement == null ? null : depositAgreement.getConfirmedAt())
                        .message(buildPaymentStatusMessage(paymentIntent, depositAgreement, room))
                        .build())
                .build();
    }

    @PostMapping("/payments/{paymentIntentId}/expire")
    public ApiResponse<DepositPaymentStatusResponse> expireDepositPayment(@PathVariable Long paymentIntentId) {
        paymentIntentRepository.findById(paymentIntentId)
                .map(this::syncPayOSPaymentIfPaid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay phien thanh toan."));
        PaymentIntent paymentIntent = depositPaymentExpiryService.expire(paymentIntentId);
        DepositAgreement depositAgreement = paymentIntent.getDepositAgreementId() == null
                ? null
                : depositAgreementRepository.findById(paymentIntent.getDepositAgreementId()).orElse(null);
        Room room = depositAgreement == null
                ? null
                : roomRepository.findById(depositAgreement.getRoomId()).orElse(null);

        return ApiResponse.<DepositPaymentStatusResponse>builder()
                .data(DepositPaymentStatusResponse.builder()
                        .paymentIntentId(paymentIntent.getId())
                        .status(paymentIntent.getStatus())
                        .depositStatus(depositAgreement == null ? null : depositAgreement.getStatus())
                        .roomStatus(room == null ? null : room.getCurrentStatus())
                        .expiresAt(paymentIntent.getExpiresAt())
                        .paidAt(depositAgreement == null ? null : depositAgreement.getConfirmedAt())
                        .message(buildPaymentStatusMessage(paymentIntent, depositAgreement, room))
                        .build())
                .build();
    }

    @GetMapping("/payments/{paymentIntentId}/contract")
    public ResponseEntity<Resource> downloadPaidDepositContract(
            @PathVariable Long paymentIntentId,
            @RequestParam String paymentContent
    ) {
        PaymentIntent paymentIntent = paymentIntentRepository.findById(paymentIntentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy phiên thanh toán."));
        if (paymentIntent.getDepositAgreementId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phiên thanh toán không thuộc đặt cọc phòng.");
        }
        if (paymentIntent.getStatus() != PaymentIntentStatus.SUCCEEDED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phiên thanh toán chưa hoàn tất.");
        }
        if (paymentIntent.getPaymentContent() == null || !paymentIntent.getPaymentContent().equals(paymentContent)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Thông tin phiên thanh toán không hợp lệ.");
        }

        DepositAgreement depositAgreement = depositAgreementRepository.findById(paymentIntent.getDepositAgreementId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thông tin đặt cọc."));
        FileDataResponse fileData = depositContractDocumentService.getOfficialContractFile(depositAgreement.getId());
        String contentType = fileData.contentType() == null
                ? MediaType.APPLICATION_PDF_VALUE
                : fileData.contentType();
        String filename = "deposit-contract-" + depositAgreement.getDepositCode() + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(fileData.resource());
    }

    @Transactional
    @PostMapping("/payments/{paymentIntentId}/cancel")
    public ApiResponse<DepositRoomHoldStatusResponse> cancelDepositPayment(@PathVariable Long paymentIntentId) {
        PaymentIntent paymentIntent = paymentIntentRepository.findById(paymentIntentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy phiên thanh toán."));
        if (paymentIntent.getDepositAgreementId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phiên thanh toán không thuộc đặt cọc phòng.");
        }

        DepositAgreement depositAgreement = depositAgreementRepository.findById(paymentIntent.getDepositAgreementId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thông tin đặt cọc."));
        RoomHold roomHold = roomHoldRepository.findById(depositAgreement.getRoomHoldId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy phiên giữ chỗ."));
        if (roomHold.getStatus() == RoomHoldStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phòng đã được đặt cọc thành công, không thể hủy giữ chỗ.");
        }

        roomHold.cancel();
        roomHoldRepository.save(roomHold);
        earlyCancelRoomHoldTaskPort.execute(roomHold.getId());
        markPaymentIntentFailedIfPending(paymentIntent);
        depositAgreement.changeStatus(DepositAgreementStatus.CANCELLED);
        depositAgreementRepository.save(depositAgreement);
        roomRepository.updateRoomStatusIfCurrent(
                depositAgreement.getRoomId(),
                RoomStatus.ON_HOLD,
                roomStatusAfterHoldRelease(depositAgreement.getRoomId())
        );

        return ApiResponse.<DepositRoomHoldStatusResponse>builder()
                .data(resolveRoomHoldStatus(String.valueOf(depositAgreement.getRoomId()), null, null))
                .build();
    }

    private DepositRoomHoldStatusResponse resolveRoomHoldStatus(String roomIdentifier, LocalDate expectedMoveInDate, LocalDate expectedLeaseSignDate) {
        Room room = resolveRoom(roomIdentifier);
        LocalDateTime now = LocalDateTime.now();
        RoomHold activeHold = roomHoldRepository.findActiveHoldByRoomId(room.getId(), now).orElse(null);

        if (activeHold != null) {
            long remainingSeconds = Math.max(1, Duration.between(now, activeHold.getExpiresAt()).getSeconds());
            return new DepositRoomHoldStatusResponse(
                    false,
                    room.getCurrentStatus().name(),
                    activeHold.getStatus().name(),
                    activeHold.getExpiresAt(),
                    remainingSeconds,
                    "Phòng đang có người đặt cọc. Vui lòng chờ " + remainingSeconds + " giây."
            );
        }

        if (room.getCurrentStatus() == RoomStatus.RESERVED) {
            return new DepositRoomHoldStatusResponse(
                    false,
                    room.getCurrentStatus().name(),
                    null,
                    null,
                    0,
                    "Phòng đã được đặt cọc. Vui lòng chọn phòng khác."
            );
        }

        if (room.getCurrentStatus() == RoomStatus.SOON_VACANT) {
            return resolveSoonVacantHoldStatus(room, expectedMoveInDate, expectedLeaseSignDate);
        }

        if (room.getCurrentStatus() != RoomStatus.VACANT) {
            return new DepositRoomHoldStatusResponse(
                    false,
                    room.getCurrentStatus().name(),
                    null,
                    null,
                    0,
                    "Phòng hiện không thể đặt cọc. Vui lòng chọn phòng khác."
            );
        }

        return new DepositRoomHoldStatusResponse(
                true,
                room.getCurrentStatus().name(),
                null,
                null,
                0,
                "Phòng đang trống, có thể đặt cọc."
        );
    }

    private Room resolveRoom(String roomIdentifier) {
        try {
            Long roomId = Long.valueOf(roomIdentifier);
            return roomRepository.findById(roomId)
                    .or(() -> roomRepository.findByRoomCode(roomIdentifier))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy phòng."));
        } catch (NumberFormatException ex) {
            return roomRepository.findByRoomCode(roomIdentifier)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy phòng."));
        }
    }

    private DepositRoomHoldStatusResponse resolveSoonVacantHoldStatus(Room room, LocalDate expectedMoveInDate, LocalDate expectedLeaseSignDate) {
        LocalDate expectedVacantDate = roomCommitmentChecker.findExpectedVacantDateForBooking(room.getId())
                .orElse(null);
        if (expectedVacantDate == null) {
            return new DepositRoomHoldStatusResponse(
                    false,
                    room.getCurrentStatus().name(),
                    null,
                    null,
                    0,
                    "PhÃ²ng sáº¯p trá»‘ng chÆ°a cÃ³ ngÃ y dá»± kiáº¿n bÃ n giao."
            );
        }
        if (expectedMoveInDate == null) {
            return new DepositRoomHoldStatusResponse(
                    false,
                    room.getCurrentStatus().name(),
                    null,
                    null,
                    0,
                    "PhÃ²ng sáº¯p trá»‘ng. Vui lÃ²ng chá»n ngÃ y dá»± kiáº¿n vÃ o á»Ÿ Ä‘á»ƒ kiá»ƒm tra kháº£ dá»¥ng."
            );
        }
        if (expectedLeaseSignDate == null) {
            return new DepositRoomHoldStatusResponse(
                    false,
                    room.getCurrentStatus().name(),
                    null,
                    null,
                    0,
                    "EXPECTED_SIGN_DATE_REQUIRED: Can co ngay du kien ky hop dong."
            );
        }
        LocalDate minAllowedDate = expectedVacantDate.plusDays(1);
        LocalDate maxAllowedDate = expectedVacantDate.plusDays(MAX_DEPOSIT_SCHEDULE_DAYS);
        if (expectedMoveInDate.isBefore(minAllowedDate)) {
            return new DepositRoomHoldStatusResponse(
                    false,
                    room.getCurrentStatus().name(),
                    null,
                    null,
                    0,
                    "Ngày dự kiến vào ở phải sau ngày khách cũ trả phòng."
            );
        }
        if (expectedLeaseSignDate.isBefore(minAllowedDate)) {
            return new DepositRoomHoldStatusResponse(
                    false,
                    room.getCurrentStatus().name(),
                    null,
                    null,
                    0,
                    "EXPECTED_SIGN_DATE_BEFORE_VACANT_DATE: Ngày hẹn ký hợp đồng phải sau ngày khách cũ trả phòng."
            );
        }
        if (expectedMoveInDate.isAfter(maxAllowedDate)) {
            return new DepositRoomHoldStatusResponse(
                    false,
                    room.getCurrentStatus().name(),
                    null,
                    null,
                    0,
                    "Ngày dự kiến vào ở chỉ được tối đa 14 ngày kể từ ngày khách cũ trả phòng."
            );
        }
        if (expectedLeaseSignDate.isAfter(maxAllowedDate)) {
            return new DepositRoomHoldStatusResponse(
                    false,
                    room.getCurrentStatus().name(),
                    null,
                    null,
                    0,
                    "EXPECTED_SIGN_DATE_TOO_FAR_AFTER_VACANT_DATE: Ngày hẹn ký hợp đồng chỉ được tối đa 14 ngày kể từ ngày khách cũ trả phòng."
            );
        }
        return new DepositRoomHoldStatusResponse(
                true,
                room.getCurrentStatus().name(),
                null,
                null,
                0,
                "PhÃ²ng sáº¯p trá»‘ng, cÃ³ thá»ƒ Ä‘áº·t cá»c theo ngÃ y dá»± kiáº¿n vÃ o á»Ÿ."
        );
    }

    private RoomStatus roomStatusAfterHoldRelease(Long roomId) {
        return roomCommitmentChecker.findExpectedVacantDateForBooking(roomId).isPresent()
                ? RoomStatus.SOON_VACANT
                : RoomStatus.VACANT;
    }

    private void markPaymentIntentFailedIfPending(PaymentIntent paymentIntent) {
        if (paymentIntent.getStatus() != PaymentIntentStatus.PENDING) {
            return;
        }
        try {
            paymentIntent.failPayment();
            paymentIntentRepository.save(paymentIntent);
        } catch (RuntimeException ex) {
            log.warn("Skip failing payment intent during hold cancel. providerOrderCode={}",
                    paymentIntent.getProviderOrderCode(), ex);
        }
    }

    private DepositCheckoutResponse toCheckoutResponse(PaymentIntent paymentIntent) {
        JsonNode payload = parseCheckoutPayload(paymentIntent.getQrPayload());
        String checkoutUrl = textValue(payload, "checkoutUrl");
        String qrCode = textValue(payload, "qrCode");
        String qrPayload = textValue(payload, "qrPayload");
        if (qrPayload == null && payload == null) {
            qrPayload = paymentIntent.getQrPayload();
        }

        Long orderCode = longValue(payload, "orderCode");
        String providerOrderCode = valueOrDefault(
                textValue(payload, "providerOrderCode"),
                paymentIntent.getProviderOrderCode()
        );
        String accountName = valueOrDefault(
                textValue(payload, "accountName"),
                textValue(payload, "receiverName")
        );
        String bankShortName = valueOrDefault(
                textValue(payload, "bankShortName"),
                textValue(payload, "bankName")
        );
        String transferDescription = valueOrDefault(
                textValue(payload, "transferDescription"),
                textValue(payload, "description")
        );
        return DepositCheckoutResponse.builder()
                .id(paymentIntent.getId())
                .paymentIntentId(paymentIntent.getId())
                .invoiceId(paymentIntent.getInvoiceId())
                .depositAgreementId(paymentIntent.getDepositAgreementId())
                .amount(paymentIntent.getAmount())
                .paymentContent(paymentIntent.getPaymentContent())
                .description(valueOrDefault(textValue(payload, "description"), paymentIntent.getPaymentContent()))
                .checkoutUrl(checkoutUrl)
                .checkOutUrl(checkoutUrl)
                .qrCode(qrCode)
                .qrPayload(qrPayload)
                .expiresAt(paymentIntent.getExpiresAt())
                .provider(paymentIntent.getProvider())
                .status(paymentIntent.getStatus())
                .orderCode(orderCode == null ? providerOrderCode : String.valueOf(orderCode))
                .providerOrderCode(providerOrderCode)
                .paymentLinkId(textValue(payload, "paymentLinkId"))
                .bankBin(textValue(payload, "bankBin"))
                .bankShortName(bankShortName)
                .accountName(accountName)
                .transferDescription(transferDescription)
                .receiverName(accountName)
                .bankName(bankShortName)
                .accountNumber(textValue(payload, "accountNumber"))
                .build();
    }

    private PaymentIntent syncPayOSPaymentIfPaid(PaymentIntent paymentIntent) {
        if (paymentIntent.getProvider() != PaymentIntentProvider.PAYOS
                || !canSyncPayOSPayment(paymentIntent)) {
            return paymentIntent;
        }

        try {
            PaymentLink paymentLink = payOSProperties.payOS().paymentRequests().get(paymentIntent.getProviderOrderCode());
            if (paymentLink == null || paymentLink.getStatus() != PaymentLinkStatus.PAID) {
                return paymentIntent;
            }

            Transaction transaction = firstTransaction(paymentLink);
            Long paidAmount = transaction != null && transaction.getAmount() != null
                    ? transaction.getAmount()
                    : paymentLink.getAmountPaid();
            reconcilePaymentUseCase.execute(ReconcilePaymentCommand.builder()
                    .paymentIntentId(paymentIntent.getId())
                    .provider(TransactionProvider.PAYOS)
                    .providerTransactionId(resolvePayOSProviderTransactionId(paymentLink, transaction))
                    .amount(paidAmount)
                    .content(transaction != null && transaction.getDescription() != null
                            ? transaction.getDescription()
                            : paymentIntent.getPaymentContent())
                    .payerName(transaction == null ? null : transaction.getCounterAccountName())
                    .payerAccount(transaction == null ? null : transaction.getCounterAccountNumber())
                    .transactionTime(parsePayOSTransactionDateTime(transaction == null ? null : transaction.getTransactionDateTime()))
                    .rawPayload(objectMapper.writeValueAsString(paymentLink))
                    .build());

            return paymentIntentRepository.findById(paymentIntent.getId()).orElse(paymentIntent);
        } catch (Exception ex) {
            log.warn("Could not sync PayOS payment status. providerOrderCode={}",
                    paymentIntent.getProviderOrderCode(), ex);
            return paymentIntent;
        }
    }

    private boolean canSyncPayOSPayment(PaymentIntent paymentIntent) {
        return paymentIntent.getStatus() == PaymentIntentStatus.PENDING
                || paymentIntent.getStatus() == PaymentIntentStatus.EXPIRED;
    }

    private Transaction firstTransaction(PaymentLink paymentLink) {
        if (paymentLink.getTransactions() == null || paymentLink.getTransactions().isEmpty()) {
            return null;
        }
        return paymentLink.getTransactions().getFirst();
    }

    private String resolvePayOSProviderTransactionId(PaymentLink paymentLink, Transaction transaction) {
        if (transaction != null && transaction.getReference() != null && !transaction.getReference().isBlank()) {
            return transaction.getReference();
        }
        if (paymentLink.getId() != null && !paymentLink.getId().isBlank()) {
            return "PAYOS-POLL-" + paymentLink.getOrderCode() + "-" + paymentLink.getId();
        }
        return "PAYOS-POLL-" + paymentLink.getOrderCode();
    }

    private LocalDateTime parsePayOSTransactionDateTime(String value) {
        if (value == null || value.isBlank()) {
            return LocalDateTime.now();
        }

        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ISO_LOCAL_DATE_TIME,
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
        );
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDateTime.parse(value, formatter);
            } catch (RuntimeException ignored) {
                // Try next PayOS timestamp format.
            }
        }
        return LocalDateTime.now();
    }

    private JsonNode parseCheckoutPayload(String qrPayload) {
        if (qrPayload == null || qrPayload.isBlank()) {
            return null;
        }
        try {
            JsonNode jsonNode = objectMapper.readTree(qrPayload);
            return jsonNode.isObject() ? jsonNode : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String textValue(JsonNode payload, String fieldName) {
        if (payload == null || !payload.hasNonNull(fieldName)) {
            return null;
        }
        String value = payload.get(fieldName).asText();
        return value == null || value.isBlank() ? null : value;
    }

    private Long longValue(JsonNode payload, String fieldName) {
        if (payload == null || !payload.hasNonNull(fieldName)) {
            return null;
        }
        try {
            return payload.get(fieldName).asLong();
        } catch (Exception ignored) {
            return null;
        }
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private String buildPaymentStatusMessage(PaymentIntent paymentIntent, DepositAgreement depositAgreement, Room room) {
        if (paymentIntent.getStatus() == PaymentIntentStatus.SUCCEEDED) {
            return "Thanh toán đặt cọc thành công.";
        }
        if (paymentIntent.getStatus() == PaymentIntentStatus.EXPIRED) {
            return "Phiên thanh toán đã hết hạn.";
        }
        if (paymentIntent.getStatus() == PaymentIntentStatus.FAILED) {
            return "Thanh toán không hợp lệ hoặc cần kiểm tra lại.";
        }
        if (paymentIntent.getStatus() == PaymentIntentStatus.CANCELLED) {
            return "Phiên thanh toán đã bị hủy.";
        }
        if (depositAgreement != null && room != null && room.getCurrentStatus() == RoomStatus.ON_HOLD) {
            return "Phòng đang được giữ chỗ, chờ thanh toán.";
        }
        return "Phiên thanh toán đang chờ xử lý.";
    }
}
