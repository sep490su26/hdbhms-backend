package com.sep490.hdbhms.occupancy.infrastructure.scheduling;

import com.sep490.hdbhms.occupancy.application.port.in.usecase.RoomTransferUseCase;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoomTransferApprovalTimeoutJob {

    RoomTransferUseCase roomTransferUseCase;

    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Ho_Chi_Minh")
    public void expireTargetHolderApprovals() {
        int expiredCount = roomTransferUseCase.expireTargetHolderApprovals();
        if (expiredCount > 0) {
            log.info("Expired {} room transfer requests waiting for target holder approval", expiredCount);
        }
        int nominationExpiredCount = roomTransferUseCase.expireSourceHolderNominations();
        if (nominationExpiredCount > 0) {
            log.info("Expired {} room transfer source holder nominations", nominationExpiredCount);
        }
    }
}
