package com.sep490.hdbhms.occupancy.infrastructure.scheduling;

import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaVisitRequestRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VisitRequestTrashCleanupJob {
    JpaVisitRequestRepository visitRequestRepository;

    @Transactional
    @Scheduled(cron = "0 0 3 * * *")
    public void purgeExpiredTrashItems() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        long deletedCount = visitRequestRepository.deleteByDeletedAtIsNotNullAndDeletedAtLessThanEqual(cutoff);
        if (deletedCount > 0) {
            log.info("Permanently deleted {} visit requests from trash", deletedCount);
        }
    }
}
