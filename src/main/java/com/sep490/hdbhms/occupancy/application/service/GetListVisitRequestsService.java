package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.occupancy.application.port.in.query.GetListVisitRequestsQuery;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetListVisitRequestsUseCase;
import com.sep490.hdbhms.occupancy.application.port.out.VisitRequestRepository;
import com.sep490.hdbhms.occupancy.domain.model.VisitRequest;
import com.sep490.hdbhms.shared.utils.StringUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GetListVisitRequestsService implements GetListVisitRequestsUseCase {
    VisitRequestRepository visitRequestRepository;
    @Override
    public Page<VisitRequest> execute(GetListVisitRequestsQuery query) {
        List<Long> ids = Collections.emptyList();
        if (!StringUtils.isEmpty(query.keyword())) {
            ids = visitRequestRepository.findIdsByFullText(query.keyword());
            if (ids.isEmpty()) {
                return Page.empty(query.pageable());
            }
        }
        return visitRequestRepository.findAll(
                ids,
                query.propertyCode(),
                query.roomCode(),
                query.from(),
                query.to(),
                query.pageable()
        );
    }
}
