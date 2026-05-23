package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.occupancy.application.port.in.query.GetListFloorsQuery;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetListFloorsUseCase;
import com.sep490.hdbhms.occupancy.application.port.out.FloorRepository;
import com.sep490.hdbhms.occupancy.domain.model.Floor;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GetListFloorsService implements GetListFloorsUseCase {
    FloorRepository floorRepository;

    @Override
    public List<Floor> execute(GetListFloorsQuery query) {
        return floorRepository.findAllByPropertyId(query.propertyId());
    }
}
