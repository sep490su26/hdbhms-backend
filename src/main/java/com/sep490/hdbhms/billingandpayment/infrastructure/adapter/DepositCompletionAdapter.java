package com.sep490.hdbhms.billingandpayment.infrastructure.adapter;

import com.sep490.hdbhms.billingandpayment.application.port.out.DepositCompletionPort;
import com.sep490.hdbhms.billingandpayment.domain.model.Invoice;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.DepositAgreementStatus;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.AccountStatus;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.PromotionRole;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.RolePromotionStatus;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaRolePromotionRepository;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaUserRepository;
import com.sep490.hdbhms.notification.application.service.BusinessNotificationPublisher;
import com.sep490.hdbhms.occupancy.application.service.DepositContractDocumentService;
import com.sep490.hdbhms.occupancy.application.port.out.*;
import com.sep490.hdbhms.occupancy.domain.model.DepositAgreement;
import com.sep490.hdbhms.occupancy.domain.model.Property;
import com.sep490.hdbhms.occupancy.domain.model.Room;
import com.sep490.hdbhms.occupancy.domain.model.RoomHold;
import com.sep490.hdbhms.occupancy.domain.value_objects.RoomHoldStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.RoomStatus;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DepositCompletionAdapter implements DepositCompletionPort {
    static final String DEPOSIT_CREATED_EVENT = "DEPOSIT_CREATED";
    static final String DEPOSIT_TARGET_TYPE = "DEPOSIT_AGREEMENT";

    RoomRepository roomRepository;
    RoomHoldRepository roomHoldRepository;
    PropertyRepository propertyRepository;
    DepositAgreementRepository depositAgreementRepository;
    EarlyCancelRoomHoldTaskPort earlyCancelRoomHoldTaskPort;
    CreateLeadOrAssignTenantPort createLeadOrAssignTenantPort;
    DepositContractDocumentService depositContractDocumentService;
    JpaUserRepository userRepository;
    JpaRolePromotionRepository rolePromotionRepository;
    BusinessNotificationPublisher notificationPublisher;


    @Override
    public void execute(Invoice invoice) {
        DepositAgreement depositAgreement = depositAgreementRepository.findById(
                        invoice.getDepositAgreementId()
                )
                .orElseThrow(() -> new AppException(ApiErrorCode.DEPOSIT_AGREEMENT_NOT_FOUND));
        if (depositAgreement.getStatus() != DepositAgreementStatus.PENDING_PAYMENT) {
            return;
        }
        RoomHold roomHold = roomHoldRepository.findById(depositAgreement.getRoomHoldId())
                .orElseThrow(() -> new AppException(ApiErrorCode.DEPOSIT_AGREEMENT_NOT_FOUND));
        if (roomHold.getStatus() != RoomHoldStatus.CONFIRMED) {
            roomHold.confirmPaidHold();
            roomHoldRepository.save(roomHold);
        }
        earlyCancelRoomHoldTaskPort.execute(roomHold.getId());
        int updatedRows = roomRepository.updateRoomStatusIfCurrent(
                depositAgreement.getRoomId(),
                RoomStatus.ON_HOLD,
                RoomStatus.RESERVED
        );
        if (updatedRows == 0) {
            roomRepository.updateRoomStatusIfCurrent(
                    depositAgreement.getRoomId(),
                    RoomStatus.VACANT,
                    RoomStatus.RESERVED
            );
        }
        createLeadOrAssignTenantPort.execute(depositAgreement);
        depositAgreement.markPaid();
        depositAgreement = depositAgreementRepository.save(depositAgreement);
        publishDepositCreated(depositAgreement);
        depositContractDocumentService.generateOfficialContractAfterCommit(depositAgreement.getId());
    }

    private void publishDepositCreated(DepositAgreement depositAgreement) {
        if (depositAgreement == null || depositAgreement.getId() == null || depositAgreement.getRoomId() == null) {
            return;
        }

        Room room = roomRepository.findById(depositAgreement.getRoomId()).orElse(null);
        if (room == null || room.getPropertyId() == null) {
            return;
        }

        Property property = propertyRepository.findById(room.getPropertyId()).orElse(null);
        Map<String, Object> data = depositNotificationData(depositAgreement, room, property);
        for (Long recipientUserId : recipientUserIds(room.getPropertyId())) {
            notificationPublisher.publish(
                    DEPOSIT_CREATED_EVENT,
                    recipientUserId,
                    DEPOSIT_TARGET_TYPE,
                    depositAgreement.getId(),
                    data
            );
        }
    }

    private List<Long> recipientUserIds(Long propertyId) {
        LinkedHashSet<Long> userIds = new LinkedHashSet<>();
        userIds.addAll(userRepository.findIdsByRolesAndStatus(List.of(Role.OWNER), AccountStatus.ACTIVE));
        userIds.addAll(rolePromotionRepository.findActiveUserIdsByPropertyId(
                propertyId,
                PromotionRole.MANAGER,
                RolePromotionStatus.ACTIVE,
                AccountStatus.ACTIVE
        ));
        userIds.remove(null);
        return List.copyOf(userIds);
    }

    private Map<String, Object> depositNotificationData(
            DepositAgreement depositAgreement,
            Room room,
            Property property
    ) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("depositAgreementId", depositAgreement.getId());
        data.put("depositCode", depositAgreement.getDepositCode());
        data.put("amount", depositAgreement.getAmount());
        data.put("status", depositAgreement.getStatus() == null ? null : depositAgreement.getStatus().name());
        data.put("roomId", room.getId());
        data.put("roomCode", room.getRoomCode());
        data.put("roomName", roomName(room));
        data.put("propertyId", room.getPropertyId());
        data.put("propertyName", property == null ? "" : property.getName());
        data.put("expectedMoveInDate", depositAgreement.getExpectedMoveInDate() == null
                ? ""
                : depositAgreement.getExpectedMoveInDate().toString());
        data.put("expectedLeaseSignDate", depositAgreement.getExpectedLeaseSignDate() == null
                ? ""
                : depositAgreement.getExpectedLeaseSignDate().toString());
        data.put("targetRoute", "/dashboard/deposit-contracts");
        return data;
    }

    private String roomName(Room room) {
        if (room == null) {
            return "";
        }
        if (room.getRoomCode() != null && !room.getRoomCode().isBlank()) {
            return "Phong " + room.getRoomCode();
        }
        return room.getName() == null ? "" : room.getName();
    }
}
