package com.sep490.hdbhms.maintenance.infrastructure.adapter;

import com.sep490.hdbhms.maintenance.application.port.out.GetRoomFromLeaseContractPort;
import com.sep490.hdbhms.occupancy.application.port.out.LeaseContractRepository;
import com.sep490.hdbhms.occupancy.application.port.out.RoomRepository;
import com.sep490.hdbhms.occupancy.domain.model.LeaseContract;
import com.sep490.hdbhms.occupancy.domain.model.Room;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GetRoomFromLeaseContractAdapter implements GetRoomFromLeaseContractPort {
    RoomRepository roomRepository;
    LeaseContractRepository leaseContractRepository;

    @Override
    public Room execute(Long leaseContractId) {
        LeaseContract leaseContract = leaseContractRepository.findById(leaseContractId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid depositAgreementId: " + leaseContractId));
        return roomRepository.findById(leaseContract.getRoomId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid roomId: " + leaseContract.getRoomId()));
    }
}
