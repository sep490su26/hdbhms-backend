package com.sep490.hdbhms.occupancy.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep490.hdbhms.billingandpayment.application.port.out.ExternalPaymentPort;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.*;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.InvoiceEntity;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.InvoiceLineEntity;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.PaymentIntentEntity;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaInvoiceLineRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaInvoiceRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaPaymentIntentRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.request.PaymentRequest;
import com.sep490.hdbhms.file.domain.model.FileMetadata;
import com.sep490.hdbhms.file.domain.value_objects.FileCategory;
import com.sep490.hdbhms.file.infrastructure.persistence.entity.FileMetadataEntity;
import com.sep490.hdbhms.file.infrastructure.persistence.jpa.JpaFileMetadataRepository;
import com.sep490.hdbhms.identityandaccess.application.port.out.OtpCodeGenerator;
import com.sep490.hdbhms.occupancy.application.port.out.CreateRoomHoldTaskPort;
import com.sep490.hdbhms.occupancy.application.port.out.EarlyCancelRoomHoldTaskPort;
import com.sep490.hdbhms.occupancy.application.port.out.UploadIdentityFilePort;
import com.sep490.hdbhms.occupancy.domain.model.RoomHold;
import com.sep490.hdbhms.occupancy.domain.value_objects.*;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.*;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.*;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.BatchDepositCheckoutRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.BatchDepositCheckoutResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.BatchDepositStatusResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DepositBatchCheckoutService {
    static final long DEPOSIT_AMOUNT_PER_ROOM = 2_000L;
    static final long HOLD_DURATION_MINUTES = 5L;

    JpaRoomRepository roomRepository;
    JpaRoomHoldRepository roomHoldRepository;
    JpaDepositFormRepository depositFormRepository;
    JpaDepositAgreementRepository depositAgreementRepository;
    JpaDepositBatchRepository depositBatchRepository;
    JpaDepositBatchItemRepository depositBatchItemRepository;
    JpaInvoiceRepository invoiceRepository;
    JpaInvoiceLineRepository invoiceLineRepository;
    JpaPaymentIntentRepository paymentIntentRepository;
    JpaFileMetadataRepository fileMetadataRepository;
    UploadIdentityFilePort uploadIdentityFilePort;
    CreateRoomHoldTaskPort createRoomHoldTaskPort;
    EarlyCancelRoomHoldTaskPort earlyCancelRoomHoldTaskPort;
    OtpCodeGenerator otpCodeGenerator;
    ExternalPaymentPort externalPaymentPort;
    Environment environment;
    ObjectMapper objectMapper;
    JdbcTemplate jdbcTemplate;
    RoomCommitmentChecker roomCommitmentChecker;

    @Transactional
    public BatchDepositCheckoutResponse checkout(
            BatchDepositCheckoutRequest request,
            MultipartFile frontIdCardFile,
            MultipartFile backIdCardFile,
            MultipartFile portraitFile
    ) {
        validateRequest(request);
        List<Long> roomIds = request.getRooms().stream()
                .map(BatchDepositCheckoutRequest.RoomRequest::getRoomId)
                .sorted()
                .toList();

        List<RoomEntity> lockedRooms = roomRepository.findAllByIdForUpdate(roomIds);
        Map<Long, RoomEntity> roomsById = lockedRooms.stream()
                .collect(Collectors.toMap(RoomEntity::getId, Function.identity()));
        validateLockedRooms(request, roomIds, lockedRooms, roomsById);

        FileMetadata idFront = upload(frontIdCardFile, FileCategory.ID_CARD);
        FileMetadata idBack = upload(backIdCardFile, FileCategory.ID_CARD);
        FileMetadata portrait = upload(portraitFile, FileCategory.PORTRAIT_PHOTO);
        FileMetadataEntity idFrontEntity = fileMetadataRepository.getReferenceById(idFront.getId());
        FileMetadataEntity idBackEntity = fileMetadataRepository.getReferenceById(idBack.getId());
        FileMetadataEntity portraitEntity = fileMetadataRepository.getReferenceById(portrait.getId());

        long totalAmount = DEPOSIT_AMOUNT_PER_ROOM * roomIds.size();
        LocalDateTime holdExpiresAt = LocalDateTime.now().plusMinutes(HOLD_DURATION_MINUTES);
        RoomEntity firstRoom = lockedRooms.getFirst();
        DepositBatchEntity batch = depositBatchRepository.save(DepositBatchEntity.builder()
                .batchCode(generateBatchCode())
                .property(firstRoom.getProperty())
                .fullName(request.getFullName().trim())
                .phone(request.getPhone().trim())
                .email(trimToNull(request.getEmail()))
                .idNumber(request.getIdNumber().trim())
                .expectedMoveInDate(request.getExpectedMoveInDate())
                .expectedLeaseSignDate(request.getExpectedLeaseSignDate())
                .totalDepositAmount(totalAmount)
                .status(DepositBatchStatus.DRAFT)
                .build());

        Map<Long, BatchDepositCheckoutRequest.RoomRequest> requestRooms = request.getRooms().stream()
                .collect(Collectors.toMap(BatchDepositCheckoutRequest.RoomRequest::getRoomId, Function.identity()));
        List<BatchDepositCheckoutResponse.RoomInfo> responseRooms = new ArrayList<>();

        for (Long roomId : roomIds) {
            RoomEntity room = roomsById.get(roomId);
            BatchDepositCheckoutRequest.RoomRequest roomRequest = requestRooms.get(roomId);

            RoomHoldEntity roomHold;
            try {
                roomHold = roomHoldRepository.save(RoomHoldEntity.builder()
                        .room(room)
                        .status(RoomHoldStatus.ACTIVE)
                        .expiresAt(holdExpiresAt)
                        .build());
            } catch (DataIntegrityViolationException ex) {
                throw roomBecameUnavailable(room, lockedRooms);
            }

            DepositFormEntity depositForm = DepositFormEntity.builder()
                    .room(room)
                    .idNumber(request.getIdNumber().trim())
                    .permanentAddress(trimToNull(request.getPermanentAddress()))
                    .idIssueDate(request.getIdIssueDate())
                    .idIssuePlace(trimToNull(request.getIdIssuePlace()))
                    .dob(request.getDob())
                    .fullName(request.getFullName().trim())
                    .email(request.getEmail() == null ? "" : request.getEmail().trim())
                    .phone(request.getPhone().trim())
                    .idFrontFile(idFrontEntity)
                    .idBackFile(idBackEntity)
                    .portraitFile(portraitEntity)
                    .depositMonths(request.getDepositMonths())
                    .paymentCycleMonths(request.getPaymentCycleMonths())
                    .occupantCount(roomRequest.getOccupantCount())
                    .expectedMoveInDate(request.getExpectedMoveInDate())
                    .expectedLeaseSignDate(request.getExpectedLeaseSignDate())
                    .paymentDueAt(holdExpiresAt)
                    .status(DepositFormStatus.APPROVED)
                    .confirmedAt(LocalDateTime.now())
                    .build();
            depositForm.setCoOccupants(buildCoOccupants(roomRequest, depositForm));
            depositForm = depositFormRepository.save(depositForm);

            DepositAgreementEntity agreement = depositAgreementRepository.save(DepositAgreementEntity.builder()
                    .depositCode(otpCodeGenerator.generate())
                    .room(room)
                    .depositForm(depositForm)
                    .roomHold(roomHold)
                    .amount(DEPOSIT_AMOUNT_PER_ROOM)
                    .expectedMoveInDate(request.getExpectedMoveInDate())
                    .expectedLeaseSignDate(request.getExpectedLeaseSignDate())
                    .paymentDueAt(holdExpiresAt)
                    .status(DepositAgreementStatus.PENDING_PAYMENT)
                    .build());

            depositBatchItemRepository.save(DepositBatchItemEntity.builder()
                    .batch(batch)
                    .room(room)
                    .roomHold(roomHold)
                    .depositForm(depositForm)
                    .depositAgreement(agreement)
                    .depositAmount(DEPOSIT_AMOUNT_PER_ROOM)
                    .occupantCount(roomRequest.getOccupantCount())
                    .status(DepositBatchItemStatus.PENDING_PAYMENT)
                    .build());

            room.setCurrentStatus(RoomStatus.ON_HOLD);
            roomRepository.save(room);
            createRoomHoldTaskPort.execute(RoomHold.builder()
                    .id(roomHold.getId())
                    .roomId(roomId)
                    .status(RoomHoldStatus.ACTIVE)
                    .expiresAt(holdExpiresAt)
                    .build());

            responseRooms.add(new BatchDepositCheckoutResponse.RoomInfo(
                    roomId,
                    room.getRoomCode(),
                    DEPOSIT_AMOUNT_PER_ROOM,
                    holdExpiresAt
            ));
        }

        InvoiceEntity invoice = invoiceRepository.save(InvoiceEntity.builder()
                .invoiceCode(otpCodeGenerator.generate())
                .property(firstRoom.getProperty())
                .depositBatch(batch)
                .invoiceType(InvoiceType.DEPOSIT)
                .issueDate(LocalDateTime.now())
                .dueDate(holdExpiresAt)
                .status(InvoiceStatus.ISSUED)
                .subtotalAmount(totalAmount)
                .discountAmount(0L)
                .totalAmount(totalAmount)
                .paidAmount(0L)
                .remainingAmount(totalAmount)
                .build());
        invoiceLineRepository.save(InvoiceLineEntity.builder()
                .invoice(invoice)
                .lineType(InvoiceLineType.DEPOSIT_DEDUCTION)
                .description("Đặt cọc nhiều phòng " + batch.getBatchCode())
                .quantity(1)
                .unitPrice(totalAmount)
                .sourceType("DEPOSIT_BATCH")
                .sourceId(batch.getId())
                .build());

        PaymentIntentEntity paymentIntent = paymentIntentRepository.save(PaymentIntentEntity.builder()
                .invoice(invoice)
                .depositBatch(batch)
                .amount(totalAmount)
                .provider(resolveProvider())
                .paymentContent(batch.getBatchCode())
                .status(PaymentIntentStatus.PENDING)
                .expiresAt(holdExpiresAt)
                .build());

        com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.response.PaymentIntent checkout;
        try {
            checkout = externalPaymentPort.createCheckoutRequest(new PaymentRequest(
                    paymentIntent.getId(),
                    totalAmount,
                    batch.getBatchCode(),
                    holdExpiresAt
            ));
        } catch (RuntimeException ex) {
            throw new BatchDepositRequestException(
                    HttpStatus.BAD_GATEWAY,
                    "PAYMENT_INIT_FAILED",
                    "Không thể khởi tạo thanh toán. Vui lòng thử lại."
            );
        }
        paymentIntent.setProviderOrderCode(checkout.providerOrderCode());
        paymentIntent.setQrPayload(toCheckoutPayload(checkout, paymentIntent));
        paymentIntentRepository.save(paymentIntent);

        batch.setInvoiceId(invoice.getId());
        batch.setPaymentIntentId(paymentIntent.getId());
        batch.setStatus(DepositBatchStatus.PENDING_PAYMENT);
        depositBatchRepository.save(batch);

        return BatchDepositCheckoutResponse.builder()
                .batchId(batch.getId())
                .batchCode(batch.getBatchCode())
                .paymentIntentId(paymentIntent.getId())
                .totalAmount(totalAmount)
                .currency("VND")
                .checkoutUrl(checkout.checkOutUrl())
                .qrCode(checkout.qrCode())
                .qrPayload(checkout.qrPayload())
                .providerOrderCode(checkout.providerOrderCode())
                .paymentLinkId(checkout.paymentLinkId())
                .bankBin(checkout.bankBin())
                .bankShortName(checkout.bankShortName())
                .accountNumber(checkout.accountNumber())
                .accountName(checkout.accountName())
                .transferDescription(checkout.transferDescription())
                .expiresAt(holdExpiresAt)
                .rooms(responseRooms)
                .build();
    }

    @Transactional(readOnly = true)
    public BatchDepositStatusResponse getStatus(Long batchId) {
        DepositBatchEntity batch = depositBatchRepository.findById(batchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy batch đặt cọc."));
        PaymentIntentEntity paymentIntent = batch.getPaymentIntentId() == null
                ? null
                : paymentIntentRepository.findById(batch.getPaymentIntentId()).orElse(null);
        List<DepositBatchItemEntity> items =
                depositBatchItemRepository.findAllByBatch_IdOrderByRoom_RoomCodeAsc(batchId);

        return new BatchDepositStatusResponse(
                batch.getId(),
                batch.getBatchCode(),
                batch.getStatus(),
                paymentIntent == null ? null : paymentIntent.getStatus(),
                statusMessage(batch.getStatus()),
                items.stream()
                        .map(item -> new BatchDepositStatusResponse.RoomStatusInfo(
                                item.getRoom().getId(),
                                item.getRoom().getRoomCode(),
                                roomItemStatus(item)
                        ))
                        .toList()
        );
    }

    @Transactional(readOnly = true)
    public Long getPaymentIntentId(Long batchId) {
        return depositBatchRepository.findById(batchId)
                .map(DepositBatchEntity::getPaymentIntentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy batch đặt cọc."));
    }

    @Transactional
    public BatchDepositStatusResponse cancel(Long batchId) {
        DepositBatchEntity batch = depositBatchRepository.findById(batchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy batch đặt cọc."));
        if (batch.getStatus() == DepositBatchStatus.CONFIRMED || batch.getStatus() == DepositBatchStatus.PAID) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Batch đã thanh toán thành công, không thể hủy giữ chỗ."
            );
        }
        if (batch.getStatus() == DepositBatchStatus.CANCELLED) {
            return getStatus(batchId);
        }

        List<DepositBatchItemEntity> items =
                depositBatchItemRepository.findAllByBatch_IdOrderByRoom_RoomCodeAsc(batchId);
        for (DepositBatchItemEntity item : items) {
            RoomHoldEntity hold = item.getRoomHold();
            if (hold != null && hold.getStatus() != RoomHoldStatus.CONFIRMED) {
                hold.setStatus(RoomHoldStatus.CANCELLED);
                roomHoldRepository.save(hold);
                earlyCancelRoomHoldTaskPort.execute(hold.getId());
            }

            DepositAgreementEntity agreement = item.getDepositAgreement();
            if (agreement != null && agreement.getStatus() == DepositAgreementStatus.PENDING_PAYMENT) {
                agreement.setStatus(DepositAgreementStatus.CANCELLED);
                depositAgreementRepository.save(agreement);
            }

            RoomEntity room = item.getRoom();
            if (room != null && room.getCurrentStatus() == RoomStatus.ON_HOLD) {
                RoomStatus restoredStatus = roomCommitmentChecker.findExpectedVacantDateForBooking(room.getId()).isPresent()
                        ? RoomStatus.SOON_VACANT
                        : RoomStatus.VACANT;
                room.setCurrentStatus(restoredStatus);
                roomRepository.save(room);
            }
            item.setStatus(DepositBatchItemStatus.CANCELLED);
            depositBatchItemRepository.save(item);
        }

        if (batch.getPaymentIntentId() != null) {
            paymentIntentRepository.findById(batch.getPaymentIntentId()).ifPresent(paymentIntent -> {
                if (paymentIntent.getStatus() == PaymentIntentStatus.PENDING
                        || paymentIntent.getStatus() == PaymentIntentStatus.CREATED) {
                    paymentIntent.setStatus(PaymentIntentStatus.CANCELLED);
                    paymentIntentRepository.save(paymentIntent);
                }
            });
        }
        if (batch.getInvoiceId() != null) {
            invoiceRepository.findById(batch.getInvoiceId()).ifPresent(invoice -> {
                if (invoice.getStatus() != InvoiceStatus.PAID) {
                    invoice.setStatus(InvoiceStatus.VOIDED);
                    invoice.setVoidedAt(LocalDateTime.now());
                    invoice.setVoidReason("Khách hủy phiên đặt cọc.");
                    invoiceRepository.save(invoice);
                }
            });
        }

        batch.setStatus(DepositBatchStatus.CANCELLED);
        depositBatchRepository.save(batch);
        return getStatus(batchId);
    }

    private void validateRequest(BatchDepositCheckoutRequest request) {
        if (request.getRooms() == null || request.getRooms().isEmpty()) {
            throw new BatchDepositRequestException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "Phải có ít nhất 1 phòng để đặt cọc."
            );
        }
        Set<Long> uniqueIds = new HashSet<>();
        for (BatchDepositCheckoutRequest.RoomRequest room : request.getRooms()) {
            if (room.getRoomId() == null || !uniqueIds.add(room.getRoomId())) {
                throw new BatchDepositRequestException(
                        HttpStatus.BAD_REQUEST,
                        "DUPLICATE_ROOM",
                        "Danh sách phòng có roomId trùng lặp."
                );
            }
        }
        if (request.getExpectedMoveInDate().isBefore(LocalDate.now())
                || request.getExpectedLeaseSignDate().isBefore(LocalDate.now())) {
            throw new BatchDepositRequestException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "Ngày dự kiến không được trước ngày hiện tại."
            );
        }
        if (!List.of(1, 3).contains(request.getPaymentCycleMonths())) {
            throw new BatchDepositRequestException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "Chu kỳ thanh toán chỉ được là 1 hoặc 3 tháng."
            );
        }
    }

    private void validateLockedRooms(
            BatchDepositCheckoutRequest request,
            List<Long> roomIds,
            List<RoomEntity> lockedRooms,
            Map<Long, RoomEntity> roomsById
    ) {
        List<BatchRoomUnavailableException.UnavailableRoom> unavailable = new ArrayList<>();
        List<BatchRoomUnavailableException.AvailableRoom> available = new ArrayList<>();
        Long propertyId = lockedRooms.isEmpty() ? null : lockedRooms.getFirst().getProperty().getId();

        for (Long roomId : roomIds) {
            RoomEntity room = roomsById.get(roomId);
            if (room == null || room.getDeletedAt() != null) {
                unavailable.add(new BatchRoomUnavailableException.UnavailableRoom(
                        roomId, String.valueOf(roomId), "ROOM_NOT_FOUND", "Không tìm thấy phòng " + roomId + "."
                ));
                continue;
            }
            if (!Objects.equals(propertyId, room.getProperty().getId())) {
                throw new BatchDepositRequestException(
                        HttpStatus.BAD_REQUEST,
                        "DIFFERENT_PROPERTY_NOT_ALLOWED",
                        "Các phòng trong batch phải cùng một cơ sở."
                );
            }

            BatchDepositCheckoutRequest.RoomRequest roomRequest = request.getRooms().stream()
                    .filter(item -> Objects.equals(item.getRoomId(), roomId))
                    .findFirst()
                    .orElseThrow();
            int coOccupantCount = roomRequest.getCoOccupants() == null
                    ? 0
                    : roomRequest.getCoOccupants().size();
            if (roomRequest.getOccupantCount() > room.getMaxOccupants()
                    || coOccupantCount != roomRequest.getOccupantCount() - 1) {
                throw new BatchDepositRequestException(
                        HttpStatus.BAD_REQUEST,
                        "OCCUPANT_COUNT_EXCEEDED",
                        "Số người ở phòng " + room.getRoomCode() + " không hợp lệ."
                );
            }

            String reason = unavailableReason(room, request.getExpectedMoveInDate(), request.getExpectedLeaseSignDate());
            if (reason != null) {
                unavailable.add(new BatchRoomUnavailableException.UnavailableRoom(
                        room.getId(),
                        room.getRoomCode(),
                        reason,
                        "Phòng " + room.getRoomCode()
                                + " đã có người đặt cọc hoặc đang được xử lý thanh toán."
                ));
            } else {
                available.add(new BatchRoomUnavailableException.AvailableRoom(room.getId(), room.getRoomCode()));
            }
        }

        if (!unavailable.isEmpty()) {
            throw new BatchRoomUnavailableException(unavailable, available);
        }
    }

    private String unavailableReason(RoomEntity room, LocalDate expectedMoveInDate, LocalDate expectedLeaseSignDate) {
        boolean soonVacant = room.getCurrentStatus() == RoomStatus.SOON_VACANT;
        if (soonVacant) {
            LocalDate expectedVacantDate = roomCommitmentChecker.findExpectedVacantDateForBooking(room.getId())
                    .orElse(null);
            if (expectedVacantDate == null) {
                return "EXPECTED_VACANT_DATE_MISSING";
            }
            if (expectedLeaseSignDate.isBefore(expectedVacantDate)) {
                throw new BatchDepositRequestException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "EXPECTED_SIGN_DATE_BEFORE_VACANT_DATE",
                        "Ngay den ky hop dong phai sau hoac bang ngay phong du kien trong."
                );
            }
            if (expectedMoveInDate.isBefore(expectedVacantDate)) {
                throw new BatchDepositRequestException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "EXPECTED_MOVE_IN_BEFORE_VACANT_DATE",
                        "Ngay du kien vao o phai sau hoac bang ngay phong du kien trong."
                );
            }
        }
        if (!soonVacant && room.getCurrentStatus() != RoomStatus.VACANT) {
            return "ROOM_NOT_VACANT";
        }
        boolean activeHold = roomHoldRepository
                .findFirstByRoom_IdAndStatusInAndExpiresAtAfterOrderByExpiresAtAsc(
                        room.getId(),
                        List.of(RoomHoldStatus.ACTIVE, RoomHoldStatus.PAYMENT_PROCESSING),
                        LocalDateTime.now()
                )
                .isPresent();
        if (activeHold) {
            return "ROOM_HOLD_ACTIVE";
        }
        Integer activeDeposit = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM deposit_agreements da
                LEFT JOIN room_holds rh ON rh.room_hold_id = da.room_hold_id
                WHERE da.room_id = ?
                  AND da.status IN ('PENDING_PAYMENT','PAID','CONFIRMED','EXTENDED')
                  AND (
                      da.status <> 'PENDING_PAYMENT'
                      OR rh.room_hold_id IS NULL
                      OR rh.status NOT IN ('CANCELLED','EXPIRED')
                  )
                """, Integer.class, room.getId());
        if (activeDeposit != null && activeDeposit > 0) {
            return "DEPOSIT_ACTIVE";
        }
        if (soonVacant) {
            return null;
        }
        Integer activeContract = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM lease_contracts
                WHERE room_id = ?
                  AND deleted_at IS NULL
                  AND status IN ('ACTIVE','EXPIRING_SOON','TERMINATION_PENDING')
                """, Integer.class, room.getId());
        return activeContract != null && activeContract > 0 ? "LEASE_ACTIVE" : null;
    }

    private BatchRoomUnavailableException roomBecameUnavailable(
            RoomEntity unavailableRoom,
            List<RoomEntity> lockedRooms
    ) {
        List<BatchRoomUnavailableException.AvailableRoom> availableRooms = lockedRooms.stream()
                .filter(room -> !Objects.equals(room.getId(), unavailableRoom.getId()))
                .map(room -> new BatchRoomUnavailableException.AvailableRoom(room.getId(), room.getRoomCode()))
                .toList();
        return new BatchRoomUnavailableException(
                List.of(new BatchRoomUnavailableException.UnavailableRoom(
                        unavailableRoom.getId(),
                        unavailableRoom.getRoomCode(),
                        "ROOM_HOLD_ACTIVE",
                        "Phòng " + unavailableRoom.getRoomCode()
                                + " đã có người đặt cọc hoặc đang được xử lý thanh toán."
                )),
                availableRooms
        );
    }

    private List<DepositFormCoOccupantEntity> buildCoOccupants(
            BatchDepositCheckoutRequest.RoomRequest request,
            DepositFormEntity depositForm
    ) {
        List<BatchDepositCheckoutRequest.CoOccupantRequest> coOccupants =
                request.getCoOccupants() == null ? List.of() : request.getCoOccupants();
        return coOccupants.stream()
                .sorted(Comparator.comparing(BatchDepositCheckoutRequest.CoOccupantRequest::getDisplayOrder))
                .map(item -> DepositFormCoOccupantEntity.builder()
                        .depositForm(depositForm)
                        .fullName(item.getFullName().trim())
                        .phone(item.getPhone().trim())
                        .displayOrder(item.getDisplayOrder())
                        .build())
                .toList();
    }

    private FileMetadata upload(MultipartFile file, FileCategory category) {
        if (file == null || file.isEmpty()) {
            throw new BatchDepositRequestException(
                    HttpStatus.BAD_REQUEST,
                    "FILE_UPLOAD_INVALID",
                    "Thiếu file CCCD hoặc ảnh chân dung."
            );
        }
        try {
            return uploadIdentityFilePort.execute(file, category);
        } catch (IOException ex) {
            throw new BatchDepositRequestException(
                    HttpStatus.BAD_REQUEST,
                    "FILE_UPLOAD_INVALID",
                    "Không thể upload file định danh."
            );
        }
    }

    private PaymentIntentProvider resolveProvider() {
        return "payos".equalsIgnoreCase(environment.getProperty("app.payment.provider", "vnpay"))
                ? PaymentIntentProvider.PAYOS
                : PaymentIntentProvider.BANK_TRANSFER;
    }

    private String generateBatchCode() {
        return "DEP-BATCH-"
                + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-"
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private String toCheckoutPayload(
            com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.response.PaymentIntent checkout,
            PaymentIntentEntity paymentIntent
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("checkoutUrl", checkout.checkOutUrl());
        payload.put("qrCode", checkout.qrCode());
        payload.put("qrPayload", checkout.qrPayload());
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
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Không thể lưu dữ liệu checkout PayOS.", ex);
        }
    }

    private String roomItemStatus(DepositBatchItemEntity item) {
        if (item.getStatus() == DepositBatchItemStatus.CONFIRMED) return "RESERVED";
        if (item.getStatus() == DepositBatchItemStatus.EXPIRED) return "EXPIRED";
        if (item.getStatus() == DepositBatchItemStatus.CANCELLED) return "CANCELLED";
        return "HOLDING";
    }

    private String statusMessage(DepositBatchStatus status) {
        return switch (status) {
            case CONFIRMED -> "Thanh toán đặt cọc nhiều phòng thành công.";
            case REFUND_REQUIRED -> "Thanh toán đến sau khi giữ chỗ hết hạn. Vui lòng liên hệ để hoàn tiền.";
            case EXPIRED -> "Phiên giữ chỗ đã hết hạn.";
            case CANCELLED -> "Batch đặt cọc đã bị hủy.";
            default -> "Đang chờ thanh toán.";
        };
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
