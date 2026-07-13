package com.sep490.hdbhms.occupancy.application.port.out;

import com.sep490.hdbhms.occupancy.domain.model.LeaseContract;
import com.sep490.hdbhms.occupancy.domain.value_objects.LeaseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LeaseContractRepository {
    LeaseContract save(LeaseContract leaseContract);

    Optional<LeaseContract> findById(Long id);

    List<LeaseContract> findAllByTenantPersonProfileId(Long tenantId);

    List<LeaseContract> findAll();

    Page<LeaseContract> findAll(List<Long> ids, LeaseStatus status, LocalDateTime signedFrom, LocalDateTime signedTo, Pageable pageable);

    List<LeaseContract> findAllByTenantProfileId(Long id);

    boolean isTenantHasAnyActiveContract(Long tenantId);

    Optional<LeaseContract> findFirstActiveContract(Long roomId, List<LeaseStatus> statuses);

    long countMeterReadingRoomsByPeriod(
            Long propertyId,
            List<LeaseStatus> statuses,
            LocalDate periodStart,
            LocalDate periodEnd
    );

    boolean roomRequiresMeterReadingForPeriod(
            Long propertyId,
            Long roomId,
            List<LeaseStatus> statuses,
            LocalDate periodStart,
            LocalDate periodEnd
    );
}
