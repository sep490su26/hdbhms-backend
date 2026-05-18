package com.sep490.hdbhms.occupancy.infrastructure.web.mapper;

import com.sep490.hdbhms.occupancy.application.port.in.command.SendDepositFormCommand;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.SendDepositFormRequest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class RoomWebMapper {
    public abstract SendDepositFormCommand toCommand(SendDepositFormRequest request);
}
