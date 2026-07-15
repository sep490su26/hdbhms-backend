package com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa;

import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.VisitRequestEntity;
import com.sep490.hdbhms.occupancy.domain.value_objects.VisitRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface JpaVisitRequestRepository extends JpaRepository<VisitRequestEntity, Long>, JpaSpecificationExecutor<VisitRequestEntity> {
    @Query(value = "SELECT visit_request_id FROM visit_requests WHERE deleted_at IS NULL AND MATCH(visitor_name, visitor_email, visitor_phone) AGAINST (CONCAT(?1, '*') IN BOOLEAN MODE)",
            nativeQuery = true)
    List<Long> findIdsByFullText(String keyword);

    Optional<VisitRequestEntity> findByIdAndDeletedAtIsNull(Long id);

    long countByDeletedAtIsNullAndPreferredStartBetween(LocalDateTime from, LocalDateTime to);

    long countByDeletedAtIsNullAndStatus(VisitRequestStatus status);

    Optional<VisitRequestEntity> findByIdAndDeletedAtIsNotNull(Long id);

    Page<VisitRequestEntity> findAllByDeletedAtIsNotNullOrderByDeletedAtDesc(Pageable pageable);

    long deleteByDeletedAtIsNotNullAndDeletedAtLessThanEqual(LocalDateTime cutoff);
}
