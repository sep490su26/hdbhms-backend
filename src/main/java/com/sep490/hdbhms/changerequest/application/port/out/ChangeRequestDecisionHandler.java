package com.sep490.hdbhms.changerequest.application.port.out;

import com.sep490.hdbhms.changerequest.domain.model.ChangeRequest;
import com.sep490.hdbhms.changerequest.domain.valueObjects.RequestType;

public interface ChangeRequestDecisionHandler {
    boolean supports(RequestType requestType);

    void onApproved(ChangeRequest request, Long managerId);

    void onRejected(ChangeRequest request, Long managerId, String resolutionNote);
}
