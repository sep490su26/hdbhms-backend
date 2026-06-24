package com.sep490.hdbhms.occupancy.infrastructure.web.mapper;

import com.sep490.hdbhms.occupancy.application.port.in.command.CreateTransferRequestCommand;
import com.sep490.hdbhms.occupancy.domain.model.RoomTransferRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.CreateTransferRequestRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.RoomTransferResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RoomTransferWebMapper {

    @Mapping(target = "requesterId", ignore = true)
    CreateTransferRequestCommand toCommand(CreateTransferRequestRequest request);

    RoomTransferResponse toResponse(RoomTransferRequest request);

}
