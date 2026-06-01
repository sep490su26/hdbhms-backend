package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.occupancy.application.port.in.command.CreateVisitRequestCommand;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.CreateVisitRequestUseCase;
import com.sep490.hdbhms.occupancy.application.port.out.VisitRequestRepository;
import com.sep490.hdbhms.occupancy.domain.model.VisitRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CreateVisitRequestService implements CreateVisitRequestUseCase {
    VisitRequestRepository visitRequestRepository;

    @Override
    public VisitRequest execute(CreateVisitRequestCommand command) {
        VisitRequest visitRequest = VisitRequest.create(
                command.propertyId(),
                command.roomId(),
                command.visitorName(),
                command.visitorPhone(),
                command.visitorEmail(),
                command.preferredStart(),
                command.notes()
        );
        return visitRequestRepository.save(visitRequest);
    }
}
