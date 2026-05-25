package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.occupancy.application.port.in.query.GetListRoomsQuery;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetListRoomsUseCase;
import com.sep490.hdbhms.occupancy.application.port.out.RoomRepository;
import com.sep490.hdbhms.occupancy.domain.model.Room;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GetListRoomsService implements GetListRoomsUseCase {
    RoomRepository roomRepository;

    @Override
    public Page<Room> execute(GetListRoomsQuery query) {
        List<Long> ids = roomRepository
                .findAllByPropertyIdAndFloorId(
                        query.propertyId(),
                        query.floorId()
                ).stream()
                .map(Room::getId)
                .toList();
        return roomRepository.findAll(
                ids,
                query.status(),
                query.minPrice(),
                query.maxPrice(),
                query.pageable()
        );
    }
}
