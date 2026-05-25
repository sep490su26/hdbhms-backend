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
    @Mapping(target = "propertyId", source = "visitRequest.propertyId")
    @Mapping(target = "propertyName", source = "visitRequest.propertyName")
    @Mapping(target = "roomCode", source = "visitRequest.roomCode")
    @Mapping(target = "customerName", source = "visitRequest.visitorName")
    @Mapping(target = "phone", source = "visitRequest.visitorPhone")
    @Mapping(target = "appointmentAt", source = "visitRequest.preferredStart")
    @Mapping(target = "note", source = "visitRequest.notes")
    @Mapping(target = "status", source = "visitRequest.status")
    @Mapping(target = "source", source = "visitRequest.source")
    @Mapping(target = "statusLabel", expression = "java(visitRequest.getStatus() != null ? visitRequest.getStatus().label() : null)")
    @Mapping(target = "createdAt", source = "visitRequest.createdAt")
    @Mapping(target = "updatedAt", source = "visitRequest.updatedAt")
    VisitRequestDetailsResponse toDetailsResponse(VisitRequest visitRequest, Property property);

    @Mapping(target = "customerName", source = "visitorName")
    @Mapping(target = "phone", source = "visitorPhone")
    @Mapping(target = "appointmentAt", source = "preferredStart")
    @Mapping(target = "note", source = "notes")
    @Mapping(target = "statusLabel", expression = "java(visitRequest.getStatus() != null ? visitRequest.getStatus().label() : null)")
    VisitRequestResponse toResponse(VisitRequest visitRequest);
}
