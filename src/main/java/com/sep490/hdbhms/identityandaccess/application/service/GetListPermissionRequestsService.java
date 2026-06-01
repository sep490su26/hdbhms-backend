package com.sep490.hdbhms.identityandaccess.application.service;

import com.sep490.hdbhms.identityandaccess.application.port.in.query.GetListPermissionRequestsQuery;
import com.sep490.hdbhms.identityandaccess.application.port.in.usecase.GetListPermissionRequestsUseCase;
import com.sep490.hdbhms.identityandaccess.application.port.out.PermissionRequestRepository;
import com.sep490.hdbhms.identityandaccess.domain.model.PermissionRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GetListPermissionRequestsService implements GetListPermissionRequestsUseCase {
    PermissionRequestRepository permissionRequestRepository;

    @Override
    public Page<PermissionRequest> execute(GetListPermissionRequestsQuery query) {
        return permissionRequestRepository.findAll(
                query.status(),
                query.pageable()
        );
    }
}
