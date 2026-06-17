package com.sep490.hdbhms.changerequest.infrastructure.persistence.jpa;

import com.sep490.hdbhms.changerequest.infrastructure.persistence.entity.ChangeRequestEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaChangeRequestEventRepository extends JpaRepository<ChangeRequestEventEntity, Long> {
    List<ChangeRequestEventEntity> findAllByChangeRequest_Id(Long requestId);
}
