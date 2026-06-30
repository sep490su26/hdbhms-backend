package com.sep490.hdbhms.changerequest.application.port.out;

import com.sep490.hdbhms.changerequest.domain.model.ChangeRequest;
import com.sep490.hdbhms.changerequest.domain.valueObjects.RequestStatus;
import com.sep490.hdbhms.changerequest.domain.valueObjects.RequestType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.List;

public interface ChangeRequestRepository {
    ChangeRequest save(ChangeRequest changeRequest);
    Optional<ChangeRequest> findById(Long id);
    List<ChangeRequest> findAll();
    Page<ChangeRequest> findFiltered(RequestType type, RequestStatus status, String search, Pageable pageable);
}
