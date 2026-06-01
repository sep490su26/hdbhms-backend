package com.sep490.hdbhms.occupancy.infrastructure.web.controller;

import com.sep490.hdbhms.billingandpayment.domain.model.PaymentIntent;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.PaymentIntentStatus;
import com.sep490.hdbhms.billingandpayment.application.port.out.PaymentIntentRepository;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.BookRoomUseCase;
import com.sep490.hdbhms.occupancy.application.port.out.DepositAgreementRepository;
import com.sep490.hdbhms.occupancy.application.port.out.EarlyCancelRoomHoldTaskPort;
import com.sep490.hdbhms.occupancy.application.port.out.RoomHoldRepository;
import com.sep490.hdbhms.occupancy.application.port.out.RoomRepository;
import com.sep490.hdbhms.occupancy.domain.model.DepositAgreement;
import com.sep490.hdbhms.occupancy.domain.model.Room;
import com.sep490.hdbhms.occupancy.domain.model.RoomHold;
import com.sep490.hdbhms.occupancy.domain.value_objects.RoomHoldStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.RoomStatus;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.SendDepositFormRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.DepositRoomHoldStatusResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.mapper.RoomWebMapper;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/deposit")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DepositController {
    RoomWebMapper roomWebMapper;
    BookRoomUseCase bookRoomUseCase;
    RoomRepository roomRepository;
    RoomHoldRepository roomHoldRepository;
    PaymentIntentRepository paymentIntentRepository;
    DepositAgreementRepository depositAgreementRepository;
    EarlyCancelRoomHoldTaskPort earlyCancelRoomHoldTaskPort;

    @PostMapping("/checkout")
    public ApiResponse<PaymentIntent> bookRoom(
            @Valid @RequestPart("metadata") SendDepositFormRequest request,
            @RequestPart("id_front_file") MultipartFile idFrontFile,
            @RequestPart("id_back_file") MultipartFile idBackFile,
            @RequestPart("portrait_file") MultipartFile portraitFile
    ) {
        log.info("{}", request.toString());
        PaymentIntent paymentIntent = bookRoomUseCase.initDepositForm(
                roomWebMapper.toCommand(request, idFrontFile, idBackFile, portraitFile)
        );
        return ApiResponse.<PaymentIntent>builder()
                .data(paymentIntent)
                .build();
    }

    @GetMapping("/rooms/{roomIdentifier}/hold-status")
    public ApiResponse<DepositRoomHoldStatusResponse> getRoomHoldStatus(@PathVariable String roomIdentifier) {
        return ApiResponse.<DepositRoomHoldStatusResponse>builder()
                .data(resolveRoomHoldStatus(roomIdentifier))
                .build();
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
        roomRepository.updateRoomStatusIfCurrent(depositAgreement.getRoomId(), RoomStatus.ON_HOLD, RoomStatus.VACANT);

        return ApiResponse.<DepositRoomHoldStatusResponse>builder()
                .data(resolveRoomHoldStatus(String.valueOf(depositAgreement.getRoomId())))
                .build();
    }

    private DepositRoomHoldStatusResponse resolveRoomHoldStatus(String roomIdentifier) {
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

    private void markPaymentIntentFailedIfPending(PaymentIntent paymentIntent) {
        if (paymentIntent.getStatus() != PaymentIntentStatus.PENDING) {
            return;
        }
        try {
            paymentIntent.failPayment();
            paymentIntentRepository.save(paymentIntent);
        } catch (RuntimeException ex) {
            log.warn("Skip failing payment intent during hold cancel. paymentIntentId={}", paymentIntent.getId(), ex);
        }
    }
}
