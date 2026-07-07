package com.sep490.hdbhms.changerequest.application.service;

import com.sep490.hdbhms.changerequest.application.port.in.command.ApproveRequestCommand;
import com.sep490.hdbhms.changerequest.application.port.out.ChangeRequestRepository;
import com.sep490.hdbhms.changerequest.domain.model.ChangeRequest;
import com.sep490.hdbhms.changerequest.domain.valueObjects.RequestType;
import com.sep490.hdbhms.changerequest.domain.valueObjects.TargetType;
import com.sep490.hdbhms.notification.application.port.out.NotificationOutboxRepository;
import com.sep490.hdbhms.permissiongrant.application.service.PermissionGrantService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChangeRequestServiceTest {

    @Test
    void approvedPermissionAccessCreatesGrant() {
        ChangeRequestRepository repository = mock(ChangeRequestRepository.class);
        NotificationOutboxRepository notificationRepository = mock(NotificationOutboxRepository.class);
        PermissionGrantService permissionGrantService = mock(PermissionGrantService.class);
        ChangeRequest request = ChangeRequest.builder()
                .id(10L)
                .requestType(RequestType.PERMISSION_ACCESS)
                .requesterId(20L)
                .targetType(TargetType.FILE)
                .targetId(30L)
                .build();
        when(repository.findById(10L)).thenReturn(Optional.of(request));

        ChangeRequestService service = new ChangeRequestService(
                repository,
                List.of(),
                notificationRepository,
                permissionGrantService
        );

        service.approveRequest(new ApproveRequestCommand(10L, 40L, "DAYS_7"));

        verify(permissionGrantService).grantAccess(request, 40L, "DAYS_7");
        verify(repository).save(request);
    }
}
