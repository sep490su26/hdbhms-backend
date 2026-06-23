package com.sep490.hdbhms.changerequest.application.port.out;

import com.sep490.hdbhms.changerequest.domain.model.ChangeRequestEvent;
import java.util.Optional;
import java.util.List;

public interface ChangeRequestEventRepository {
    ChangeRequestEvent save(ChangeRequestEvent changeRequestEvent);
    Optional<ChangeRequestEvent> findById(Long id);
    List<ChangeRequestEvent> findAllByRequestId(Long requestId);
}
