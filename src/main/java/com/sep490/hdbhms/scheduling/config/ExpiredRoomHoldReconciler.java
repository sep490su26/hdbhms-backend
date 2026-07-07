package com.sep490.hdbhms.scheduling.config;

import com.sep490.hdbhms.occupancy.application.service.RoomCommitmentChecker;
import com.sep490.hdbhms.occupancy.application.port.out.RoomHoldRepository;
import com.sep490.hdbhms.occupancy.application.port.out.RoomRepository;
import com.sep490.hdbhms.occupancy.domain.model.RoomHold;
import com.sep490.hdbhms.occupancy.domain.valueObjects.RoomHoldStatus;
import com.sep490.hdbhms.occupancy.domain.valueObjects.RoomStatus;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ExpiredRoomHoldReconciler {
    RoomRepository roomRepository;
    RoomHoldRepository roomHoldRepository;
    RoomCommitmentChecker roomCommitmentChecker;

    @Transactional
    @Scheduled(fixedDelay = 60_000, initialDelay = 15_000)
    public void releaseStaleRoomHolds() {
        LocalDateTime now = LocalDateTime.now();
        List<RoomHold> expiredHolds = roomHoldRepository.findExpiredUnconfirmedHolds(now);

        for (RoomHold roomHold : expiredHolds) {
            if (roomHold.getStatus() == RoomHoldStatus.ACTIVE
                    || roomHold.getStatus() == RoomHoldStatus.PAYMENT_PROCESSING) {
                roomHold.releaseOnAutoExpired();
                roomHoldRepository.save(roomHold);
            }

            boolean stillHasActiveHold = roomHoldRepository.existsByRoomIdAndStatusIn(
                    roomHold.getRoomId(),
                    List.of(RoomHoldStatus.ACTIVE, RoomHoldStatus.PAYMENT_PROCESSING)
            );
            if (stillHasActiveHold) {
                continue;
            }

            int updatedRows = roomRepository.updateRoomStatusIfCurrent(
                    roomHold.getRoomId(),
                    RoomStatus.ON_HOLD,
                    roomStatusAfterHoldRelease(roomHold.getRoomId())
            );
            if (updatedRows > 0) {
                log.info("Released stale room hold. roomHoldId={}, roomId={}",
                        roomHold.getId(),
                        roomHold.getRoomId());
            }
        }
    }

    private RoomStatus roomStatusAfterHoldRelease(Long roomId) {
        return roomCommitmentChecker.findExpectedVacantDateForBooking(roomId).isPresent()
                ? RoomStatus.SOON_VACANT
                : RoomStatus.VACANT;
    }
}
