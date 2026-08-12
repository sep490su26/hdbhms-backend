package com.sep490.hdbhms.booking.application.service;

import com.sep490.hdbhms.booking.application.port.out.RoomDepositFailureRepository;
import com.sep490.hdbhms.booking.domain.model.RoomDepositFailure;
import com.sep490.hdbhms.booking.domain.value_objects.RoomDepositFailureReason;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoomDepositLockService {
    private static final int FAILURE_THRESHOLD = 3;
    private static final Duration FAILURE_WINDOW = Duration.ofMinutes(16);
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    RoomDepositFailureRepository roomDepositFailureRepository;

    public void assertNotLocked(Long roomId) {
        getActiveLock(roomId).ifPresent(lock -> {
            throw new AppException(ApiErrorCode.ROOM_DEPOSIT_LOCKED);
        });
    }

    public Optional<RoomDepositLock> getActiveLock(Long roomId) {
        if (roomId == null) {
            return Optional.empty();
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime searchStart = now.minus(FAILURE_WINDOW).minus(LOCK_DURATION);
        List<RoomDepositFailure> failures = roomDepositFailureRepository
                .findByRoomIdAndOccurredAtAfter(roomId, searchStart);

        LocalDateTime activeLockedUntil = null;
        for (int i = 0; i <= failures.size() - FAILURE_THRESHOLD; i++) {
            LocalDateTime firstFailureAt = failures.get(i).getOccurredAt();
            LocalDateTime thresholdFailureAt = failures.get(i + FAILURE_THRESHOLD - 1).getOccurredAt();
            if (!thresholdFailureAt.isBefore(firstFailureAt.plus(FAILURE_WINDOW))) {
                continue;
            }

            LocalDateTime lockedUntil = thresholdFailureAt.plus(LOCK_DURATION);
            if (lockedUntil.isAfter(now)
                    && (activeLockedUntil == null || lockedUntil.isAfter(activeLockedUntil))) {
                activeLockedUntil = lockedUntil;
            }
        }

        if (activeLockedUntil == null) {
            return Optional.empty();
        }
        return Optional.of(new RoomDepositLock(
                activeLockedUntil,
                Math.max(1, Duration.between(now, activeLockedUntil).getSeconds())
        ));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(
            Long roomId,
            Long roomHoldId,
            Long paymentIntentId,
            RoomDepositFailureReason reason
    ) {
        if (roomId == null || reason == null) {
            return;
        }
        if (roomHoldId != null && roomDepositFailureRepository.existsByRoomHoldId(roomHoldId)) {
            return;
        }
        try {
            roomDepositFailureRepository.save(RoomDepositFailure.builder()
                    .roomId(roomId)
                    .roomHoldId(roomHoldId)
                    .paymentIntentId(paymentIntentId)
                    .reason(reason)
                    .occurredAt(LocalDateTime.now())
                    .build());
        } catch (DataIntegrityViolationException ignored) {
            // Another path already recorded this hold failure.
        }
    }

    public String buildLockMessage(long remainingSeconds) {
        return "Phòng tạm khóa đặt cọc do có 3 lượt đặt cọc thất bại trong dưới 16 phút. Vui lòng thử lại sau "
                + remainingSeconds
                + " giây.";
    }

    public record RoomDepositLock(LocalDateTime lockedUntil, long remainingSeconds) {
    }
}
