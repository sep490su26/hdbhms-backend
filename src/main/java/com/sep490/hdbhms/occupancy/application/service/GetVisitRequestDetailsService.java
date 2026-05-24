package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.occupancy.application.port.in.query.GetVisitRequestDetailsQuery;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetVisitRequestDetailsUseCase;
import com.sep490.hdbhms.occupancy.application.port.out.VisitRequestRepository;
import com.sep490.hdbhms.occupancy.domain.model.VisitRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GetVisitRequestDetailsService implements GetVisitRequestDetailsUseCase {
    VisitRequestRepository visitRequestRepository;

    @Override
    public VisitRequest execute(GetVisitRequestDetailsQuery query) {
        return visitRequestRepository.findById(query.visitRequestId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid visit request"));
    }
}
