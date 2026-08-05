package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.occupancy.application.port.in.command.UpdateLeaseContractTermsCommand;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetLeaseContractManagementUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.UpdateLeaseContractTermsUseCase;
import com.sep490.hdbhms.occupancy.domain.value_objects.LeaseStatus;
import com.sep490.hdbhms.property.domain.value_objects.RoomStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeaseContractEntity;
import com.sep490.hdbhms.property.infrastructure.persistence.entity.RoomEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaLeaseContractRepository;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaRoomRepository;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.LeaseContractManagementResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UpdateLeaseContractTermsService implements UpdateLeaseContractTermsUseCase {
    JpaLeaseContractRepository leaseContractRepository;
    JpaRoomRepository roomRepository;
    LeaseContractWorkflowSupport workflowSupport;
    GetLeaseContractManagementUseCase getLeaseContractManagementUseCase;

    @Override
    public LeaseContractManagementResponse execute(UpdateLeaseContractTermsCommand command) {
        LeaseContractEntity contract = leaseContractRepository.findById(command.leaseContractId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hợp đồng thuê."));
        if (contract.getDeletedAt() != null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hợp đồng thuê.");
        }
        if (List.of(
                LeaseStatus.LIQUIDATED,
                LeaseStatus.AUTO_TERMINATED,
                LeaseStatus.CANCELLED
        ).contains(contract.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Không thể cập nhật thời hạn của hợp đồng đã kết thúc."
            );
        }

        workflowSupport.validateContractTerms(
                command.startDate(),
                command.paymentCycleMonths(),
                command.monthlyRent(),
                command.depositAmount()
        );
        boolean rentChanged = !Objects.equals(contract.getMonthlyRent(), command.monthlyRent());

        contract.setStartDate(command.startDate());
        contract.setRentStartDate(workflowSupport.resolveRentStartDate(command.startDate()));
        contract.setPaymentCycleMonths(command.paymentCycleMonths());
        contract.setMonthlyRent(command.monthlyRent());
        contract.setDepositAmount(command.depositAmount());
        applyLifecycleStatusAfterTermsUpdate(contract, LocalDate.now());
        leaseContractRepository.save(contract);

        if (rentChanged) {
            workflowSupport.appendContractEvent(
                    contract.getId(),
                    "PRICE_CHANGED",
                    "Cập nhật giá thuê hằng tháng thành " + command.monthlyRent()
            );
        }
        return getLeaseContractManagementUseCase.findOne(contract.getId());
    }

    private void applyLifecycleStatusAfterTermsUpdate(LeaseContractEntity contract, LocalDate today) {
        if (!List.of(LeaseStatus.ACTIVE, LeaseStatus.EXPIRING_SOON, LeaseStatus.EXPIRED).contains(contract.getStatus())
                || contract.getEndDate() == null) {
            return;
        }

        LeaseStatus oldStatus = contract.getStatus();
        LeaseStatus newStatus;
        if (today.isAfter(contract.getEndDate())) {
            newStatus = LeaseStatus.EXPIRED;
        } else if (!today.isBefore(contract.getEndDate().minusMonths(3))) {
            newStatus = LeaseStatus.EXPIRING_SOON;
        } else {
            newStatus = LeaseStatus.ACTIVE;
        }

        if (newStatus == oldStatus) {
            return;
        }

        contract.setStatus(newStatus);
        if ((newStatus == LeaseStatus.ACTIVE || newStatus == LeaseStatus.EXPIRING_SOON)
                && oldStatus == LeaseStatus.EXPIRED) {
            RoomEntity room = contract.getRoom();
            if (room != null && room.getCurrentStatus() == RoomStatus.EXPIRED) {
                RoomStatus fromStatus = room.getCurrentStatus();
                room.setCurrentStatus(RoomStatus.OCCUPIED);
                roomRepository.save(room);
                workflowSupport.appendRoomStatusHistory(
                        room.getId(),
                        fromStatus,
                        RoomStatus.OCCUPIED,
                        "Gia hạn hợp đồng thuê " + contract.getContractCode()
                );
            }
        }
        if (newStatus == LeaseStatus.EXPIRING_SOON) {
            workflowSupport.appendContractEvent(
                    contract.getId(),
                    "NOTICE_SENT",
                    "Cập nhật thời hạn hợp đồng còn dưới hoặc bằng 3 tháng"
            );
            return;
        }
        if (newStatus == LeaseStatus.EXPIRED) {
            workflowSupport.appendContractEvent(
                    contract.getId(),
                    "EXPIRED",
                    "Cập nhật thời hạn hợp đồng đã qua ngày kết thúc"
            );
            RoomEntity room = contract.getRoom();
            if (room != null && room.getCurrentStatus() == RoomStatus.OCCUPIED) {
                RoomStatus fromStatus = room.getCurrentStatus();
                room.setCurrentStatus(RoomStatus.EXPIRED);
                roomRepository.save(room);
                workflowSupport.appendRoomStatusHistory(
                        room.getId(),
                        fromStatus,
                        RoomStatus.EXPIRED,
                        "Hợp đồng " + contract.getContractCode() + " đã hết hạn"
                );
            }
        }
    }
}
