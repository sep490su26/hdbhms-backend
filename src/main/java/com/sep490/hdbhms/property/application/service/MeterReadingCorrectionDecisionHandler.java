package com.sep490.hdbhms.property.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep490.hdbhms.billingandpayment.application.service.UtilityBillingRunService;
import com.sep490.hdbhms.changerequest.application.port.out.ChangeRequestDecisionHandler;
import com.sep490.hdbhms.changerequest.domain.model.ChangeRequest;
import com.sep490.hdbhms.changerequest.domain.value_objects.RequestType;
import com.sep490.hdbhms.property.domain.value_objects.MeterReadingReviewStatus;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaMeterReadingRepository;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MeterReadingCorrectionDecisionHandler implements ChangeRequestDecisionHandler {
    JpaMeterReadingRepository meterReadingRepository;
    UtilityBillingRunService utilityBillingRunService;
    ObjectMapper objectMapper;

    @Override
    public boolean supports(RequestType requestType) {
        return requestType == RequestType.METER_READING_CORRECTION;
    }

    @Override
    public void onApproved(ChangeRequest request, Long managerId) {
        if (request != null && request.getTargetId() != null) {
            JsonNode payload = readPayload(request.getRequestPayload());
            utilityBillingRunService.applyMeterReadingCorrection(
                    request.getTargetId(),
                    longValue(payload, "invoiceId", "invoice_id"),
                    longValue(payload, "invoiceLineId", "invoice_line_id"),
                    decimalValue(payload, "reportedCurrentValue", "reported_current_value")
            );
        }
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

    private JsonNode readPayload(String payload) {
        if (payload == null || payload.isBlank()) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST_PAYLOAD);
        }
        try {
            JsonNode node = objectMapper.readTree(payload);
            if (node == null || !node.isObject()) {
                throw new AppException(ApiErrorCode.INVALID_REQUEST_PAYLOAD);
            }
            return node;
        } catch (AppException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST_PAYLOAD, exception);
        }
    }

    private BigDecimal decimalValue(JsonNode payload, String... fields) {
        for (String field : fields) {
            JsonNode value = payload.get(field);
            if (value == null || value.isNull()) {
                continue;
            }
            try {
                return value.isNumber()
                        ? value.decimalValue()
                        : new BigDecimal(value.asText().trim());
            } catch (RuntimeException exception) {
                throw new AppException(ApiErrorCode.INVALID_REQUEST_PAYLOAD, exception);
            }
        }
        throw new AppException(ApiErrorCode.BILLING_METER_READING_PROPOSAL_REQUIRED);
    }

    private Long longValue(JsonNode payload, String... fields) {
        for (String field : fields) {
            JsonNode value = payload.get(field);
            if (value == null || value.isNull()) {
                continue;
            }
            try {
                return value.isNumber() ? value.longValue() : Long.valueOf(value.asText().trim());
            } catch (RuntimeException exception) {
                throw new AppException(ApiErrorCode.INVALID_REQUEST_PAYLOAD, exception);
            }
        }
        return null;
    }
}
