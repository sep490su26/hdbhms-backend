package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.identityandaccess.application.port.out.PersonProfileRepository;
import com.sep490.hdbhms.identityandaccess.domain.model.PersonProfile;
import com.sep490.hdbhms.occupancy.application.port.in.command.ConfirmLeaseContractCommand;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.ConfirmLeaseContractUseCase;
import com.sep490.hdbhms.occupancy.application.port.out.LeaseContractRepository;
import com.sep490.hdbhms.occupancy.application.port.out.PromoteToTenantPort;
import com.sep490.hdbhms.occupancy.application.port.out.RoomRepository;
import com.sep490.hdbhms.occupancy.domain.model.LeaseContract;
import com.sep490.hdbhms.occupancy.domain.model.Room;
import com.sep490.hdbhms.occupancy.domain.value_objects.LeaseStatus;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConfirmLeaseContractService implements ConfirmLeaseContractUseCase {
    RoomRepository roomRepository;
    PromoteToTenantPort promoteToTenantPort;
    LeaseContractRepository leaseContractRepository;
    PersonProfileRepository personProfileRepository;

    @Override
    public void execute(ConfirmLeaseContractCommand command) {
        LeaseContract leaseContract = leaseContractRepository
                .findById(command.leaseContractId())
                .orElseThrow(() -> new IllegalArgumentException("Lease contract not found"));
        if (leaseContract.getStatus() != LeaseStatus.DRAFT) {
            return;
        }
        PersonProfile personProfile = personProfileRepository
                .findById(leaseContract.getPrimaryTenantProfileId())
                .orElseThrow(() -> new IllegalArgumentException("Primary tenant profile not found"));
        Room room = roomRepository.findById(leaseContract.getRoomId())
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));
        if (personProfile.getUserId() != null) {
            promoteToTenantPort.execute(room.getPropertyId(), personProfile.getUserId());
        }
        room.occupyRoom();
        roomRepository.save(room);
        
        leaseContract.activateContract();
        leaseContractRepository.save(leaseContract);
    }
}
