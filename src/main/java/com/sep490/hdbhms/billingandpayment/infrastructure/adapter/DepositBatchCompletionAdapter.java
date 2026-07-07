package com.sep490.hdbhms.billingandpayment.infrastructure.adapter;

import com.sep490.hdbhms.billingandpayment.application.port.out.DepositBatchCompletionPort;
import com.sep490.hdbhms.billingandpayment.domain.model.Invoice;
import com.sep490.hdbhms.billingandpayment.domain.valueObjects.DepositAgreementStatus;
import com.sep490.hdbhms.billingandpayment.domain.valueObjects.PaymentIntentStatus;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.PaymentIntentEntity;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaPaymentIntentRepository;
import com.sep490.hdbhms.occupancy.application.port.out.CreateLeadOrAssignTenantPort;
import com.sep490.hdbhms.occupancy.application.port.out.DepositAgreementRepository;
import com.sep490.hdbhms.occupancy.application.port.out.EarlyCancelRoomHoldTaskPort;
import com.sep490.hdbhms.occupancy.application.service.DepositContractDocumentService;
import com.sep490.hdbhms.occupancy.domain.model.DepositAgreement;
import com.sep490.hdbhms.occupancy.domain.valueObjects.*;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.DepositBatchEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.DepositBatchItemEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RoomHoldEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaDepositBatchItemRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaDepositBatchRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomHoldRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DepositBatchCompletionAdapter implements DepositBatchCompletionPort {
    JpaDepositBatchRepository batchRepository;
    JpaDepositBatchItemRepository itemRepository;
    JpaRoomHoldRepository roomHoldRepository;
    JpaRoomRepository roomRepository;
    JpaPaymentIntentRepository paymentIntentRepository;
    DepositAgreementRepository depositAgreementRepository;
    EarlyCancelRoomHoldTaskPort earlyCancelRoomHoldTaskPort;
    CreateLeadOrAssignTenantPort createLeadOrAssignTenantPort;
    DepositContractDocumentService depositContractDocumentService;

    @Override
    public void execute(Invoice invoice) {
        if (invoice.getDepositBatchId() == null) {
            return;
        }
        DepositBatchEntity batch = batchRepository.findById(invoice.getDepositBatchId())
                .orElseThrow(() -> new IllegalStateException("Deposit batch not found"));
        if (batch.getStatus() == DepositBatchStatus.CONFIRMED
                || batch.getStatus() == DepositBatchStatus.REFUND_REQUIRED) {
            return;
        }
        PaymentIntentEntity paymentIntent = batch.getPaymentIntentId() == null
                ? null
                : paymentIntentRepository.findById(batch.getPaymentIntentId()).orElse(null);
        if (paymentIntent != null && paymentIntent.getStatus() == PaymentIntentStatus.REFUND_REQUIRED) {
            List<DepositBatchItemEntity> items =
                    itemRepository.findAllByBatch_IdOrderByRoom_RoomCodeAsc(batch.getId());
            markRefundRequired(batch, items);
            return;
        }

        List<DepositBatchItemEntity> items =
                itemRepository.findAllByBatch_IdOrderByRoom_RoomCodeAsc(batch.getId());
        LocalDateTime now = LocalDateTime.now();
        boolean paymentAccepted = paymentIntent == null
                || paymentIntent.getStatus() == PaymentIntentStatus.SUCCEEDED;
        boolean holdsValid = !items.isEmpty() && items.stream().allMatch(item -> {
            RoomHoldEntity hold = item.getRoomHold();
            return hold != null
                    && !hasAnotherActiveHold(item, hold, now)
                    && (paymentAccepted || hold.getExpiresAt().isAfter(now))
                    && (hold.getStatus() == RoomHoldStatus.ACTIVE
                    || hold.getStatus() == RoomHoldStatus.PAYMENT_PROCESSING
                    || hold.getStatus() == RoomHoldStatus.CONFIRMED
                    || (paymentAccepted && hold.getStatus() == RoomHoldStatus.EXPIRED));
        });

        if (!holdsValid) {
            markRefundRequired(batch, items);
            return;
        }

        for (DepositBatchItemEntity item : items) {
            RoomHoldEntity hold = item.getRoomHold();
            hold.setStatus(RoomHoldStatus.CONFIRMED);
            hold.setReleasedAt(null);
            roomHoldRepository.save(hold);
            earlyCancelRoomHoldTaskPort.execute(hold.getId());

            int updated = roomRepository.updateRoomStatusIfCurrent(
                    item.getRoom().getId(),
                    RoomStatus.ON_HOLD,
                    RoomStatus.RESERVED
            );
            if (updated == 0 && item.getRoom().getCurrentStatus() == RoomStatus.VACANT) {
                updated = roomRepository.updateRoomStatusIfCurrent(
                        item.getRoom().getId(),
                        RoomStatus.VACANT,
                        RoomStatus.RESERVED
                );
            }
            if (updated == 0 && item.getRoom().getCurrentStatus() == RoomStatus.SOON_VACANT) {
                updated = roomRepository.updateRoomStatusIfCurrent(
                        item.getRoom().getId(),
                        RoomStatus.SOON_VACANT,
                        RoomStatus.RESERVED
                );
            }
            if (updated == 0 && item.getRoom().getCurrentStatus() != RoomStatus.RESERVED) {
                throw new IllegalStateException("Room status changed during batch payment");
            }

            item.getDepositAgreement().setStatus(DepositAgreementStatus.PAID);
            item.getDepositAgreement().setConfirmedAt(now);
            item.setStatus(DepositBatchItemStatus.CONFIRMED);
            itemRepository.save(item);

            DepositAgreement agreement = depositAgreementRepository.findById(
                            item.getDepositAgreement().getId()
                    )
                    .orElseThrow(() -> new IllegalStateException("Deposit agreement not found"));
            createLeadOrAssignTenantPort.execute(agreement);
            depositContractDocumentService.generateOfficialContractAfterCommit(agreement.getId());
        }

        batch.setStatus(DepositBatchStatus.CONFIRMED);
        batchRepository.save(batch);
    }

    private boolean hasAnotherActiveHold(
            DepositBatchItemEntity item,
            RoomHoldEntity currentHold,
            LocalDateTime now
    ) {
        return roomHoldRepository
                .findFirstByRoom_IdAndStatusInAndExpiresAtAfterOrderByExpiresAtAsc(
                        item.getRoom().getId(),
                        List.of(RoomHoldStatus.ACTIVE, RoomHoldStatus.PAYMENT_PROCESSING),
                        now
                )
                .filter(activeHold -> !Objects.equals(activeHold.getId(), currentHold.getId()))
                .isPresent();
    }

    private void markRefundRequired(
            DepositBatchEntity batch,
            List<DepositBatchItemEntity> items
    ) {
        batch.setStatus(DepositBatchStatus.REFUND_REQUIRED);
        batchRepository.save(batch);
        for (DepositBatchItemEntity item : items) {
            if (item.getStatus() != DepositBatchItemStatus.CONFIRMED) {
                item.setStatus(DepositBatchItemStatus.EXPIRED);
                itemRepository.save(item);
            }
        }
        if (batch.getPaymentIntentId() != null) {
            PaymentIntentEntity paymentIntent = paymentIntentRepository.findById(batch.getPaymentIntentId())
                    .orElse(null);
            if (paymentIntent != null) {
                paymentIntent.setStatus(PaymentIntentStatus.REFUND_REQUIRED);
                paymentIntentRepository.save(paymentIntent);
            }
        }
        log.warn("Late batch deposit payment requires refund. batchId={}, batchCode={}",
                batch.getId(), batch.getBatchCode());
    }
}
