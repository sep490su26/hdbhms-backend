package com.sep490.hdbhms.maintenance.infrastructure.persistence.jpa;

import com.sep490.hdbhms.maintenance.infrastructure.persistence.entity.MaintenanceTicketEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JpaMaintenanceTicketRepository extends JpaRepository<MaintenanceTicketEntity, Long>, JpaSpecificationExecutor<MaintenanceTicketEntity> {
    @Query("SELECT t.id FROM MaintenanceTicketEntity t WHERE LOWER(t.ticketCode) LIKE LOWER(CONCAT('%', :code, '%'))")
    List<Long> findIdsByTicketCode(@Param("code") String code);
}
