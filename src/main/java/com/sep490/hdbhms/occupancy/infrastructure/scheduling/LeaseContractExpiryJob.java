package com.sep490.hdbhms.occupancy.infrastructure.scheduling;

import com.sep490.hdbhms.occupancy.application.service.LeaseContractLifecycleService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LeaseContractExpiryJob {
    static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    LeaseContractLifecycleService leaseContractLifecycleService;

    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Ho_Chi_Minh")
    public void updateContractLifecycle() {
        leaseContractLifecycleService.processAll(LocalDate.now(BUSINESS_ZONE));
    }
}
