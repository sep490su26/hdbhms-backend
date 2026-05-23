package com.sep490.hdbhms.identityandaccess.application.service;

import com.sep490.hdbhms.identityandaccess.application.port.in.command.CreatePermissionRequestCommand;
import com.sep490.hdbhms.identityandaccess.application.port.in.usecase.CreatePermissionRequestUseCase;
import com.sep490.hdbhms.identityandaccess.application.port.out.PermissionRequestRepository;
import com.sep490.hdbhms.identityandaccess.domain.model.PermissionRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CreatePermissionRequestService implements CreatePermissionRequestUseCase {
    PermissionRequestRepository permissionRequestRepository;

    @Override
    public PermissionRequest execute(CreatePermissionRequestCommand command) {
        PermissionRequest permissionRequest = PermissionRequest.submit(
                command.requesterUserId(),
                command.targetType(),
                command.targetId()
        );
        return permissionRequestRepository.save(permissionRequest);
    }
}
