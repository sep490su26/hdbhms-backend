package com.sep490.hdbhms.occupancy.infrastructure.web.mapper;

import com.sep490.hdbhms.occupancy.application.port.in.command.CreateVisitRequestCommand;
import com.sep490.hdbhms.occupancy.domain.model.Property;
import com.sep490.hdbhms.occupancy.domain.model.VisitRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.CreateVisitRequestRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.VisitRequestDetailsResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.VisitRequestResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {PropertyWebMapper.class})
public interface VisitRequestWebMapper {
    CreateVisitRequestCommand toCommand(CreateVisitRequestRequest request);

    @Mapping(target = "id", source = "visitRequest.id")
    @Mapping(target = "createdAt", source = "visitRequest.createdAt")
    VisitRequestDetailsResponse toDetailsResponse(VisitRequest visitRequest, Property property);

    VisitRequestResponse toResponse(VisitRequest visitRequest);
}
