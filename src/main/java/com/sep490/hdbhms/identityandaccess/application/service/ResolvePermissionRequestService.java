package com.sep490.hdbhms.identityandaccess.application.service;

import com.sep490.hdbhms.identityandaccess.application.port.in.command.ApprovePermissionRequestCommand;
import com.sep490.hdbhms.identityandaccess.application.port.in.command.RejectPermissionRequestCommand;
import com.sep490.hdbhms.identityandaccess.application.port.in.usecase.ResolvePermissionRequestUseCase;
import com.sep490.hdbhms.identityandaccess.application.port.out.PermissionRequestRepository;
import com.sep490.hdbhms.identityandaccess.domain.model.PermissionRequest;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ResolvePermissionRequestService implements ResolvePermissionRequestUseCase {
    PermissionRequestRepository permissionRequestRepository;

    @Override
    public PermissionRequest approve(ApprovePermissionRequestCommand command) {
        PermissionRequest permissionRequest = permissionRequestRepository.findById(command.permissionRequestId())
                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));
        permissionRequest.approve();
        return permissionRequestRepository.save(permissionRequest);
    }

    @Override
    public PermissionRequest reject(RejectPermissionRequestCommand command) {
        PermissionRequest permissionRequest = permissionRequestRepository.findById(command.permissionRequestId())
                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));
        permissionRequest.reject(command.reason());
        return permissionRequestRepository.save(permissionRequest);
    }
}
