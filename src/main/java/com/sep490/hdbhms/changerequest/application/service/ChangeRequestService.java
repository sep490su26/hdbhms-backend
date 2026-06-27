package com.sep490.hdbhms.changerequest.application.service;

import com.sep490.hdbhms.changerequest.application.port.in.command.ApproveRequestCommand;
import com.sep490.hdbhms.changerequest.application.port.in.command.RejectRequestCommand;
import com.sep490.hdbhms.changerequest.application.port.in.usecase.ChangeRequestUseCase;
import com.sep490.hdbhms.changerequest.application.port.out.ChangeRequestDecisionHandler;
import com.sep490.hdbhms.changerequest.application.port.out.ChangeRequestRepository;
import com.sep490.hdbhms.changerequest.domain.model.ChangeRequest;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChangeRequestService implements ChangeRequestUseCase {
    ChangeRequestRepository repository;
    List<ChangeRequestDecisionHandler> decisionHandlers;

    @Override
    @Transactional
    public void approveRequest(ApproveRequestCommand command) {
        ChangeRequest request = repository.findById(command.requestId())
                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));
        request.approve(command.managerId());
        repository.save(request);
        dispatchApproved(request, command.managerId());
    }

    @Override
    @Transactional
    public void rejectRequest(RejectRequestCommand command) {
        ChangeRequest request = repository.findById(command.requestId())
                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));
        request.reject(command.managerId(), command.resolutionNote());
        repository.save(request);
        dispatchRejected(request, command.managerId(), command.resolutionNote());
    }

    private void dispatchApproved(ChangeRequest request, Long managerId) {
        decisionHandlers.stream()
                .filter(handler -> handler.supports(request.getRequestType()))
                .forEach(handler -> handler.onApproved(request, managerId));
    }

    private void dispatchRejected(ChangeRequest request, Long managerId, String resolutionNote) {
        decisionHandlers.stream()
                .filter(handler -> handler.supports(request.getRequestType()))
                .forEach(handler -> handler.onRejected(request, managerId, resolutionNote));
    }
}
