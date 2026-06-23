package com.sep490.hdbhms.occupancy.infrastructure.web.mapper;

import com.sep490.hdbhms.occupancy.domain.model.DepositAgreement;
import com.sep490.hdbhms.occupancy.domain.model.Room;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.DepositAgreementDetailsResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.DepositAgreementResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = RoomWebMapper.class)
public interface DepositAgreementWebMapper {
    @Mapping(target = "id", source = "depositAgreement.id")
    @Mapping(target = "createdAt", source = "depositAgreement.createdAt")
    DepositAgreementResponse toResponse(DepositAgreement depositAgreement, Room room);

    @Mapping(target = "id", source = "depositAgreement.id")
    @Mapping(target = "createdAt", source = "depositAgreement.createdAt")
    DepositAgreementDetailsResponse toDetailsResponse(DepositAgreement depositAgreement, Room room);
}
