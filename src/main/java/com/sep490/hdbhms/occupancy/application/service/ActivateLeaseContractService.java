package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.billingandpayment.domain.value_objects.DepositAgreementStatus;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.ActivateLeaseContractUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetLeaseContractManagementUseCase;
import com.sep490.hdbhms.occupancy.domain.value_objects.LeaseStatus;
import com.sep490.hdbhms.property.domain.value_objects.RoomStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeaseContractEntity;
import com.sep490.hdbhms.property.infrastructure.persistence.entity.RoomEntity;
import com.sep490.hdbhms.booking.infrastructure.persistence.jpa.JpaDepositAgreementRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaLeaseContractRepository;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaRoomRepository;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.LeaseContractManagementResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ActivateLeaseContractService implements ActivateLeaseContractUseCase {
    JpaLeaseContractRepository leaseContractRepository;
    JpaRoomRepository roomRepository;
    JpaDepositAgreementRepository depositAgreementRepository;
    JdbcTemplate jdbcTemplate;
    LeaseContractWorkflowSupport workflowSupport;
    GetLeaseContractManagementUseCase getLeaseContractManagementUseCase;

    @Override
    public LeaseContractManagementResponse execute(Long leaseContractId) {
        LeaseContractEntity contract = leaseContractRepository.findById(leaseContractId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hợp đồng thuê."));
        if (contract.getStatus() == LeaseStatus.ACTIVE) {
            return getLeaseContractManagementUseCase.findOne(leaseContractId);
        }
        workflowSupport.ensureNotRoomTransferManagedContract(leaseContractId);
        if (contract.getStatus() != LeaseStatus.DRAFT && contract.getStatus() != LeaseStatus.PENDING_SIGNATURE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ được kích hoạt hợp đồng đang chờ ký.");
        }
        if (contract.getSignedFile() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cần upload file hợp đồng đã ký trước khi kích hoạt.");
        }
        if (contract.getDepositAgreement() != null && contract.getDepositAgreement().getSignedFile() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cần upload file hợp đồng đặt cọc đã ký trước khi kích hoạt.");
        }
        if (contract.getPrimaryTenantProfile() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hợp đồng chưa có người ký chính.");
        }
        if (contract.getStartDate() == null || contract.getEndDate() == null || contract.getEndDate().isBefore(contract.getStartDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ngày bắt đầu/kết thúc hợp đồng không hợp lệ.");
        }
        RoomEntity room = contract.getRoom();
        if (room == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hợp đồng chưa gắn phòng.");
        }

        if (contract.getPreviousContract() == null && !hasCompletedMoveInHandover(leaseContractId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cần hoàn thành bàn giao phòng, nhập số điện/nước và upload biên bản bàn giao đã ký trước khi kích hoạt hợp đồng."
            );
        }

        boolean renewalActivation = contract.getPreviousContract() != null
                && (room.getCurrentStatus() == RoomStatus.OCCUPIED
                || room.getCurrentStatus() == RoomStatus.EXPIRED);
        if (!renewalActivation
                && room.getCurrentStatus() != RoomStatus.RESERVED
                && room.getCurrentStatus() != RoomStatus.VACANT
                && room.getCurrentStatus() != RoomStatus.ON_HOLD) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phòng phải ở trạng thái trống hoặc đã đặt cọc trước khi kích hoạt hợp đồng.");
        }
        Long previousContractId = contract.getPreviousContract() != null
                ? contract.getPreviousContract().getId()
                : null;
        if (workflowSupport.hasOtherActiveContract(room.getId(), contract.getId(), previousContractId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phòng đã có hợp đồng đang hiệu lực.");
        }

        workflowSupport.ensureContractOccupants(contract, contract.getDepositAgreement());
        LeaseContractEntity previousContract = contract.getPreviousContract();
        if (previousContract != null && workflowSupport.isHolderReplacementLiquidation(previousContract.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Hoàn tất luồng thanh lý để kích hoạt hợp đồng thay thế."
            );
        }
        if (previousContract != null) {
            workflowSupport.copyContractOccupants(previousContract, contract);
            boolean legacyPrematureRenewal =
                    previousContract.getStatus() == LeaseStatus.RENEWED
                            && List.of(LeaseStatus.DRAFT, LeaseStatus.PENDING_SIGNATURE)
                            .contains(contract.getStatus());
            if (!legacyPrematureRenewal
                    && !List.of(LeaseStatus.ACTIVE, LeaseStatus.EXPIRING_SOON, LeaseStatus.EXPIRED)
                    .contains(previousContract.getStatus())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Hợp đồng trước không còn ở trạng thái cho phép kích hoạt gia hạn."
                );
            }
            previousContract.setStatus(LeaseStatus.RENEWED);
            leaseContractRepository.saveAndFlush(previousContract);
        }
        contract.setStatus(LeaseStatus.ACTIVE);
        contract.setSignedAt(LocalDateTime.now());
        if (contract.getRentStartDate() == null) {
            contract.setRentStartDate(workflowSupport.resolveRentStartDate(contract.getStartDate()));
        }
        leaseContractRepository.save(contract);

        RoomStatus fromStatus = room.getCurrentStatus();
        room.setCurrentStatus(RoomStatus.OCCUPIED);
        roomRepository.save(room);
        workflowSupport.appendRoomStatusHistory(room.getId(), fromStatus, RoomStatus.OCCUPIED, "Kích hoạt hợp đồng thuê " + contract.getContractCode());

        if (contract.getDepositAgreement() != null
                && contract.getDepositAgreement().getStatus() != DepositAgreementStatus.CONVERTED_TO_LEASE) {
            contract.getDepositAgreement().setStatus(DepositAgreementStatus.CONVERTED_TO_LEASE);
            depositAgreementRepository.save(contract.getDepositAgreement());
        }
        workflowSupport.appendContractEvent(contract.getId(), "SIGNED", "Kích hoạt hợp đồng thuê");
        if (previousContract != null) {
            workflowSupport.appendContractEvent(
                    previousContract.getId(),
                    "RENEWED",
                    "Kích hoạt hợp đồng tái ký; newContractId=" + contract.getId()
            );
        }
        return getLeaseContractManagementUseCase.findOne(contract.getId());
    }

    private boolean hasCompletedMoveInHandover(Long leaseContractId) {
        Integer handoverCount = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM contract_handover_records
                        WHERE contract_id = ?
                          AND handover_type = 'MOVE_IN'
                          AND electricity_reading_id IS NOT NULL
                          AND water_reading_id IS NOT NULL
                          AND signed_document_id IS NOT NULL
                        """,
                Integer.class,
                leaseContractId
        );
        return handoverCount != null && handoverCount > 0;
    }
}
