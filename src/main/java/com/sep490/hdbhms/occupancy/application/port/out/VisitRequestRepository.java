package com.sep490.hdbhms.occupancy.application.port.out;

import com.sep490.hdbhms.occupancy.domain.model.VisitRequest;
import com.sep490.hdbhms.occupancy.domain.valueObjects.VisitRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VisitRequestRepository {
    VisitRequest save(VisitRequest visitRequest);

    Optional<VisitRequest> findById(Long id);

    List<Long> findIdsByFullText(String keyword);

    Page<VisitRequest> findAll(
            List<Long> ids,
            String propertyCode,
            String roomCode,
            Long propertyId,
            Long roomId,
            VisitRequestStatus status,
            LocalDateTime from,
            LocalDateTime localDateTime,
            Pageable pageable
    );

    Page<VisitRequest> findDeleted(Pageable pageable);

    void deleteById(Long id);
}
