package com.sep490.hdbhms.portal.application.service;

import com.sep490.hdbhms.occupancy.application.port.out.FloorRepository;
import com.sep490.hdbhms.occupancy.application.port.out.RoomRepository;
import com.sep490.hdbhms.occupancy.domain.model.Room;
import com.sep490.hdbhms.occupancy.domain.value_objects.RoomStatus;
import com.sep490.hdbhms.portal.application.port.in.query.GetDashboardQuery;
import com.sep490.hdbhms.portal.application.port.in.usecase.GetDashboardUseCase;
import com.sep490.hdbhms.portal.infrastructure.web.dto.response.DashboardResponse;
import com.sep490.hdbhms.portal.infrastructure.web.dto.response.FloorEfficiencyResponse;
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
public class GetDashboardService implements GetDashboardUseCase {
    RoomRepository roomRepository;
    FloorRepository floorRepository;

    @Override
    public DashboardResponse execute(GetDashboardQuery query) {
        //TODO: Xóa fixed cứng sau khi implement quản lý đa cơ sở
        List<FloorEfficiencyResponse> floorsEfficiencyResponses =
                floorRepository.findAllByPropertyId(1L).stream()
                        .map(floor -> {
                            List<Room> rooms = roomRepository
                                    .findAllByPropertyIdAndFloorId(
                                            1L,
                                            floor.getId()
                                    );
                            long roomCount = rooms.size();
                            long vacantRoomCount = rooms.stream()
                                    .filter(room -> room.getCurrentStatus() == RoomStatus.VACANT)
                                    .count();
                            return FloorEfficiencyResponse.builder()
                                    .roomCount(roomCount)
                                    .vacantRoomCount(vacantRoomCount)
                                    .floorName(floor.getName())
                                    .build();
                        })
                        .toList();
        List<Room> rooms = roomRepository.findAll();
        long totalRoomCount = rooms.size();
        long totalOccupiedRoomCount = rooms.stream()
                .filter(room -> room.getCurrentStatus() == RoomStatus.OCCUPIED)
                .count();
        long totalVacantRoomCount = rooms.stream()
                .filter(room -> room.getCurrentStatus() == RoomStatus.VACANT)
                .count();
        return DashboardResponse.builder()
                .totalVacantRoomCount(totalVacantRoomCount)
                .totalRoomCount(totalRoomCount)
                .totalOccupiedRoomCount(totalOccupiedRoomCount)
                .floorEfficiencies(floorsEfficiencyResponses)
                .build();
    }
}
