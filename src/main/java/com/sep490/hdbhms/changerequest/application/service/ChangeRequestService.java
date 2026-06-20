package com.sep490.hdbhms.changerequest.application.service;

import com.sep490.hdbhms.changerequest.application.port.in.command.ApproveRequestCommand;
import com.sep490.hdbhms.changerequest.application.port.in.command.RejectRequestCommand;
import com.sep490.hdbhms.changerequest.application.port.in.usecase.ChangeRequestUseCase;
import com.sep490.hdbhms.changerequest.application.port.out.ChangeRequestRepository;
import com.sep490.hdbhms.changerequest.domain.model.ChangeRequest;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChangeRequestService implements ChangeRequestUseCase {
    ChangeRequestRepository repository;

    @Override
    @Transactional
    public void approveRequest(ApproveRequestCommand command) {
        ChangeRequest request = repository.findById(command.requestId())
                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));
        request.approve(command.managerId());
        repository.save(request);

        // TODO: Dispatch domain events or call other services based on request.getRequestType()
    }

    @Override
    @Transactional
    public void rejectRequest(RejectRequestCommand command) {
        ChangeRequest request = repository.findById(command.requestId())
                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));
        request.reject(command.managerId(), command.resolutionNote());
        repository.save(request);
    }
}
