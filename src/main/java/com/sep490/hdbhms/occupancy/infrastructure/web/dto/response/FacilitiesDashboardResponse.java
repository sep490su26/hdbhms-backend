package com.sep490.hdbhms.occupancy.infrastructure.web.dto.response;

import com.sep490.hdbhms.occupancy.domain.valueObjects.FloorStatus;
import com.sep490.hdbhms.occupancy.domain.valueObjects.PropertyStatus;
import com.sep490.hdbhms.occupancy.domain.valueObjects.RoomStatus;
import com.sep490.hdbhms.shared.dto.response.PageResponse;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class FacilitiesDashboardResponse {
    Summary summary;
    List<PropertyStatus> availableStatuses;
    List<Facility> facilities;
    PageResponse<Facility> page;

    @Value
    @Builder
    public static class Summary {
        long totalProperties;
        long activeProperties;
        long totalFloors;
        long totalRooms;
        long occupiedRooms;
        long vacantRooms;
        int vacancyRate;
    }

    @Value
    @Builder
    public static class Facility {
        Long id;
        String code;
        String name;
        String address;
        String description;
        PropertyStatus status;
        int floorCount;
        int roomCount;
        int occupiedRoomCount;
        int vacantRoomCount;
        List<Floor> floors;
    }

    @Value
    @Builder
    public static class Floor {
        Long id;
        String code;
        String name;
        Integer sortOrder;
        FloorStatus status;
        int roomCount;
        int occupiedRoomCount;
        List<Room> rooms;
    }

    @Value
    @Builder
    public static class Room {
        Long id;
        String code;
        String name;
        RoomStatus status;
        Integer sortOrder;
    }
}
