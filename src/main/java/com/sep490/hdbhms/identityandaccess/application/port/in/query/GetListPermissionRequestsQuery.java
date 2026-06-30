package com.sep490.hdbhms.identityandaccess.application.port.in.query;

import com.sep490.hdbhms.identityandaccess.domain.valueObjects.PermissionRequestStatus;
import org.springframework.data.domain.Pageable;

public record GetListPermissionRequestsQuery(PermissionRequestStatus status, Pageable pageable) {
}
