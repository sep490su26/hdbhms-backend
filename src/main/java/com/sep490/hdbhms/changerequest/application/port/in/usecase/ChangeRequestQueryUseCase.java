package com.sep490.hdbhms.changerequest.application.port.in.usecase;

import com.sep490.hdbhms.changerequest.domain.model.ChangeRequest;
import com.sep490.hdbhms.changerequest.domain.valueObjects.RequestStatus;
import com.sep490.hdbhms.changerequest.domain.valueObjects.RequestType;
import com.sep490.hdbhms.changerequest.infrastructure.web.dto.response.ChangeRequestStatsResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ChangeRequestQueryUseCase {
    Page<ChangeRequest> getFilteredRequests(RequestType type, RequestStatus status, String search, Pageable pageable);
    Page<ChangeRequest> getFilteredRequestsByRequester(Long requesterId, RequestType type, RequestStatus status, String search, Pageable pageable);
    ChangeRequest getRequestById(Long id);
    ChangeRequestStatsResponse getStats();
}
