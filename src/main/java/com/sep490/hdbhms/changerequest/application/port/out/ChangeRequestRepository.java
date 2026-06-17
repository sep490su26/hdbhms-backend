package com.sep490.hdbhms.changerequest.application.port.out;

import com.sep490.hdbhms.changerequest.domain.model.ChangeRequest;
import java.util.Optional;
import java.util.List;

public interface ChangeRequestRepository {
    ChangeRequest save(ChangeRequest changeRequest);
    Optional<ChangeRequest> findById(Long id);
    List<ChangeRequest> findAll();
}
