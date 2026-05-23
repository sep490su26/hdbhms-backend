package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.occupancy.application.port.in.command.CreateFloorCommand;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.CreateFloorUseCase;
import com.sep490.hdbhms.occupancy.application.port.out.FloorRepository;
import com.sep490.hdbhms.occupancy.domain.model.Floor;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CreateFloorService implements CreateFloorUseCase {
    FloorRepository floorRepository;

    @Override
    public Floor execute(CreateFloorCommand command) {
        Floor floor = Floor.newFloor(
                command.propertyId(),
                command.floorCode(),
                command.name(),
                command.sortOrder()
        );
        return floorRepository.save(floor);
    }
}
