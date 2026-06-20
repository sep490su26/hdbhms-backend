package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.changerequest.application.port.out.ChangeRequestRepository;
import com.sep490.hdbhms.changerequest.domain.model.ChangeRequest;
import com.sep490.hdbhms.changerequest.domain.value_objects.RequestType;
import com.sep490.hdbhms.identityandaccess.application.port.out.PersonProfileRepository;
import com.sep490.hdbhms.identityandaccess.domain.model.PersonProfile;
import com.sep490.hdbhms.occupancy.application.port.in.command.AcceptHolderNominationCommand;
import com.sep490.hdbhms.occupancy.application.port.in.command.ApproveTransferCommand;
import com.sep490.hdbhms.occupancy.application.port.in.command.CompleteTransferCommand;
import com.sep490.hdbhms.occupancy.application.port.in.command.CreateTransferRequestCommand;
import com.sep490.hdbhms.occupancy.application.port.in.command.ExecuteTransferCommand;
import com.sep490.hdbhms.occupancy.application.port.in.command.NominateHolderCommand;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.RoomTransferUseCase;
import com.sep490.hdbhms.occupancy.application.port.out.LeaseContractRepository;
import com.sep490.hdbhms.occupancy.application.port.out.RoomRepository;
import com.sep490.hdbhms.occupancy.application.port.out.RoomTransferRepository;
import com.sep490.hdbhms.occupancy.application.port.out.TenantRepository;
import com.sep490.hdbhms.occupancy.domain.model.LeaseContract;
import com.sep490.hdbhms.occupancy.domain.model.Room;
import com.sep490.hdbhms.occupancy.domain.model.RoomTransferRequest;
import com.sep490.hdbhms.occupancy.domain.model.Tenant;
import com.sep490.hdbhms.occupancy.domain.value_objects.RoomStatus;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomTransferService implements RoomTransferUseCase {
    RoomRepository roomRepository;
    TenantRepository tenantRepository;
    PersonProfileRepository personProfileRepository;
    LeaseContractRepository leaseContractRepository;
    RoomTransferRepository roomTransferRepository;
    ChangeRequestRepository changeRequestRepository;

    @Override
    @Transactional
    public Long createTransferRequest(CreateTransferRequestCommand command) {
        log.info("Creating transfer request for user {}", command.requesterId());
        //TODO: Check xem tài khoản có đang bị khoá không
        tenantRepository.findByUserId(command.requesterId())
                .orElseThrow(() -> new AppException(ApiErrorCode.TENANT_NOT_FOUND));
        PersonProfile requesterProfile = personProfileRepository.findByUserId(command.requesterId())
                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));
        if (leaseContractRepository.isTenantHasAnyActiveContract(requesterProfile.getId())) {
            throw new RuntimeException("Người gửi yêu cầu chuyển không có hợp đồng hợp lệ nào");
        }

        LeaseContract sourceContract = leaseContractRepository.findById(command.sourceContractId())
                .orElseThrow(() -> new AppException(ApiErrorCode.ROOM_NOT_FOUND));

        if (!sourceContract.getPrimaryTenantProfileId().equals(requesterProfile.getId())) {
            throw new RuntimeException("Hợp đồng không");
        }
        //TODO: Check xem khách thuê đang có yêu cầu chuyển phòng nào đang chưa được giải quyết không, tránh spam

        //TODO: Check xem khách thuê có vừa mới chuyển phòng không, tránh spam

        //TODO: Check xem contract hiện tại có đang active không? có còn hiệu lực không? có sắp hết hạn không?


        Room targetRoom = roomRepository.findById(command.targetRoomId())
                .orElseThrow(() -> new AppException(ApiErrorCode.ROOM_NOT_FOUND));
        //TODO: Check xem target room có đang active không
        if (targetRoom.getCurrentStatus() == RoomStatus.MAINTENANCE) {
            throw new AppException(ApiErrorCode.UNDEFINED);
        }
        //TODO: Check xem target room còn sức chứa không

        RoomTransferRequest roomTransferRequest = RoomTransferRequest.builder()
//                .oldRoomId(command.oldRoomId())
                .targetRoomId(command.targetRoomId())
                .requestedTransferDate(command.requestedTransferDate())
                .reason(command.reason())
                .build();
        roomTransferRequest = roomTransferRepository.save(roomTransferRequest);
        ChangeRequest changeRequest = ChangeRequest.builder()
                .requesterId(command.requesterId())
                .requestType(RequestType.ROOM_TRANSFER)
                .title(String.format("Yêu cầu chuyển sang phòng %s", targetRoom.getName()))
                .description(command.reason())
                .build();
        changeRequest = changeRequestRepository.save(changeRequest);
        return 1L;
    }

    @Override
    public void nominateHolder(NominateHolderCommand command) {

    }

    @Override
    public void acceptHolderNomination(AcceptHolderNominationCommand command) {

    }


    @Override
    @Transactional
    public void approveTransfer(ApproveTransferCommand command) {
        log.info("Manager {} approving transfer request {}", command.managerId(), command.requestId());
        // TODO: Implement actual logic
    }

    @Override
    @Transactional
    public void executeTransfer(ExecuteTransferCommand command) {
        log.info("Executing transfer request {}", command.requestId());
        // TODO: Implement actual logic
    }

    @Override
    @Transactional
    public void completeTransfer(CompleteTransferCommand command) {
        log.info("Completing transfer request {}", command.requestId());
        // TODO: Implement actual logic
    }
}
