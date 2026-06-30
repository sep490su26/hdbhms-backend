package com.sep490.hdbhms.changerequest.infrastructure.persistence.jpa;

import com.sep490.hdbhms.changerequest.domain.valueObjects.RequestStatus;
import com.sep490.hdbhms.changerequest.domain.valueObjects.RequestType;
import com.sep490.hdbhms.changerequest.infrastructure.persistence.entity.ChangeRequestEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaChangeRequestRepository extends JpaRepository<ChangeRequestEntity, Long> {

    @Query("SELECT c FROM ChangeRequestEntity c " +
           "WHERE (:type IS NULL OR c.requestType = :type) " +
           "AND (:status IS NULL OR c.status = :status) " +
           "AND (:search IS NULL OR c.requestCode LIKE %:search% OR c.title LIKE %:search%)")
    Page<ChangeRequestEntity> findFiltered(
            @Param("type") RequestType type,
            @Param("status") RequestStatus status,
            @Param("search") String search,
            Pageable pageable);

    @Query("SELECT COUNT(c) FROM ChangeRequestEntity c WHERE c.status = 'PENDING'")
    long countPending();

    @Query("SELECT COUNT(c) FROM ChangeRequestEntity c WHERE c.status = 'APPROVED' AND FUNCTION('DATE', c.resolvedAt) = FUNCTION('CURRENT_DATE')")
    long countApprovedToday();

    @Query("SELECT COUNT(c) FROM ChangeRequestEntity c WHERE c.status = 'REJECTED' AND FUNCTION('DATE', c.resolvedAt) = FUNCTION('CURRENT_DATE')")
    long countRejectedToday();

    @Query("SELECT COUNT(c) FROM ChangeRequestEntity c WHERE FUNCTION('YEAR', c.createdAt) = FUNCTION('YEAR', FUNCTION('CURRENT_DATE')) AND FUNCTION('MONTH', c.createdAt) = FUNCTION('MONTH', FUNCTION('CURRENT_DATE'))")
    long countThisMonth();

    @Query("SELECT c.requestType, COUNT(c) FROM ChangeRequestEntity c GROUP BY c.requestType")
    java.util.List<Object[]> countBreakdownByType();
}
