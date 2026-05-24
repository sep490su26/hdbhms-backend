package com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa;

import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.VisitRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface JpaVisitRequestRepository extends JpaRepository<VisitRequestEntity, Long>, JpaSpecificationExecutor<VisitRequestEntity> {
    @Query(value = "SELECT id FROM hdbhms.visit_requests WHERE MATCH(visitor_name, visitor_email, visitor_phone) AGAINST (CONCAT(?1, '*') IN BOOLEAN MODE)",
            nativeQuery = true)
    List<Long> findIdsByFullText(String keyword);
}
