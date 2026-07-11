package com.sep490.hdbhms.identityandaccess.application.port.out;

import com.sep490.hdbhms.identityandaccess.domain.model.PermissionRequest;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.PermissionRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface PermissionRequestRepository {
    PermissionRequest save(PermissionRequest permissionRequest);

    Optional<PermissionRequest> findById(Long id);

    Page<PermissionRequest> findAll(PermissionRequestStatus status, Pageable pageable);
}
