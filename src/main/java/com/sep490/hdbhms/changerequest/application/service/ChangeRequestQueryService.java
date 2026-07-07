package com.sep490.hdbhms.changerequest.application.service;

import com.sep490.hdbhms.changerequest.application.port.in.usecase.ChangeRequestQueryUseCase;
import com.sep490.hdbhms.changerequest.application.port.out.ChangeRequestRepository;
import com.sep490.hdbhms.changerequest.domain.model.ChangeRequest;
import com.sep490.hdbhms.changerequest.domain.valueObjects.RequestStatus;
import com.sep490.hdbhms.changerequest.domain.valueObjects.RequestType;
import com.sep490.hdbhms.changerequest.infrastructure.web.dto.response.ChangeRequestStatsResponse;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChangeRequestQueryService implements ChangeRequestQueryUseCase {

    ChangeRequestRepository repository;
    com.sep490.hdbhms.changerequest.infrastructure.persistence.jpa.JpaChangeRequestRepository jpaRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<ChangeRequest> getFilteredRequests(RequestType type, RequestStatus status, String search, Pageable pageable) {
        return repository.findFiltered(type, status, search, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ChangeRequest> getFilteredRequestsByRequester(
            Long requesterId,
            RequestType type,
            RequestStatus status,
            String search,
            Pageable pageable
    ) {
        return repository.findFilteredByRequester(requesterId, type, status, search, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public ChangeRequest getRequestById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));
    }

    @Override
    @Transactional(readOnly = true)
    public ChangeRequestStatsResponse getStats() {
        long pending = jpaRepository.countPending();
        long approved = jpaRepository.countApprovedToday();
        long rejected = jpaRepository.countRejectedToday();
        long thisMonth = jpaRepository.countThisMonth();

        var breakdownList = jpaRepository.countBreakdownByType();
        Map<String, Long> breakdown = breakdownList.stream()
                .collect(Collectors.toMap(
                        arr -> ((RequestType) arr[0]).name(),
                        arr -> (Long) arr[1]
                ));

        return new ChangeRequestStatsResponse(
                pending, approved, rejected, thisMonth, breakdown
        );
    }
}
