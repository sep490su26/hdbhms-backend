package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.identityandaccess.application.port.out.PersonProfileRepository;
import com.sep490.hdbhms.identityandaccess.domain.model.PersonProfile;
import com.sep490.hdbhms.occupancy.application.port.in.command.ConfirmLeaseContractCommand;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.ConfirmLeaseContractUseCase;
import com.sep490.hdbhms.occupancy.application.port.out.LeaseContractRepository;
import com.sep490.hdbhms.occupancy.application.port.out.PromoteToTenantPort;
import com.sep490.hdbhms.property.application.port.out.RoomRepository;
import com.sep490.hdbhms.occupancy.domain.model.LeaseContract;
import com.sep490.hdbhms.property.domain.model.Room;
import com.sep490.hdbhms.occupancy.domain.value_objects.LeaseStatus;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
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
                .orElseThrow(() -> new AppException(ApiErrorCode.CONTRACT_NOT_FOUND));
        if (leaseContract.getStatus() != LeaseStatus.DRAFT) {
            return;
        }
        PersonProfile personProfile = personProfileRepository
                .findById(leaseContract.getPrimaryTenantProfileId())
                .orElseThrow(() -> new AppException(ApiErrorCode.USER_PROFILE_NOT_FOUND));
        Room room = roomRepository.findById(leaseContract.getRoomId())
                .orElseThrow(() -> new AppException(ApiErrorCode.ROOM_NOT_FOUND));
        if (personProfile.getUserId() != null) {
            promoteToTenantPort.execute(room.getPropertyId(), personProfile.getUserId());
        }
        room.occupyRoom();
        roomRepository.save(room);
        
        leaseContract.activateContract();
        leaseContractRepository.save(leaseContract);
    }
}
