package com.sep490.hdbhms.identityandaccess.application.port.in.usecase;

import com.sep490.hdbhms.identityandaccess.application.port.in.query.GetListPermissionRequestsQuery;
import com.sep490.hdbhms.identityandaccess.domain.model.PermissionRequest;
import org.springframework.data.domain.Page;

public interface GetListPermissionRequestsUseCase {
    Page<PermissionRequest> execute(GetListPermissionRequestsQuery query);
}
