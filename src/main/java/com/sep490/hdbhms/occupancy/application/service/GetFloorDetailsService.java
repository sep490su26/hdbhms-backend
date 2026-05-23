package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.occupancy.application.port.in.query.GetFloorDetailsQuery;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetFloorDetailsUseCase;
import com.sep490.hdbhms.occupancy.application.port.out.FloorRepository;
import com.sep490.hdbhms.occupancy.domain.model.Floor;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GetFloorDetailsService implements GetFloorDetailsUseCase {
    FloorRepository floorRepository;

    @Override
    public Floor execute(GetFloorDetailsQuery query) {
        return floorRepository.findById(query.floorId())
                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));
    }
}
