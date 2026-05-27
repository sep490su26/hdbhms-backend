package com.sep490.hdbhms.occupancy.infrastructure.web.mapper;

import com.sep490.hdbhms.occupancy.domain.model.LeaseContract;
import com.sep490.hdbhms.occupancy.domain.model.Room;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.LeaseContractDetailsResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.LeaseContractResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = RoomWebMapper.class)
public interface LeaseContractWebMapper {
    @Mapping(target = "id", source = "leaseContract.id")
    LeaseContractResponse toResponse(LeaseContract leaseContract, Room room);

    @Mapping(target = "id", source = "leaseContract.id")
    @Mapping(target = "createdAt", source = "leaseContract.createdAt")
    LeaseContractDetailsResponse toDetailsResponse(LeaseContract leaseContract, Room room);
}
