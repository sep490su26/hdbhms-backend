package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.changerequest.application.port.out.ChangeRequestDecisionHandler;
import com.sep490.hdbhms.changerequest.domain.model.ChangeRequest;
import com.sep490.hdbhms.changerequest.domain.value_objects.RequestType;
import com.sep490.hdbhms.occupancy.domain.value_objects.MeterReadingReviewStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaMeterReadingRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MeterReadingCorrectionDecisionHandler implements ChangeRequestDecisionHandler {
    JpaMeterReadingRepository meterReadingRepository;

    @Override
    public boolean supports(RequestType requestType) {
        return requestType == RequestType.METER_READING_CORRECTION;
    }

    @Override
    public void onApproved(ChangeRequest request, Long managerId) {
        updateReviewStatus(request, MeterReadingReviewStatus.APPROVED);
    }

    @Override
    public void onRejected(ChangeRequest request, Long managerId, String resolutionNote) {
        updateReviewStatus(request, MeterReadingReviewStatus.REJECTED);
    }

    private void updateReviewStatus(ChangeRequest request, MeterReadingReviewStatus status) {
        if (request == null || request.getTargetId() == null) {
            return;
        }
        meterReadingRepository.findById(request.getTargetId()).ifPresent(reading -> {
            reading.setReviewStatus(status);
            meterReadingRepository.save(reading);
        });
    }
}
