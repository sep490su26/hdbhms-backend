package com.sep490.hdbhms.booking.application.service;

import com.sep490.hdbhms.billingandpayment.domain.value_objects.DepositAgreementStatus;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaUserRepository;
import com.sep490.hdbhms.booking.domain.value_objects.DepositContactOutcome;
import com.sep490.hdbhms.property.domain.value_objects.RoomStatus;
import com.sep490.hdbhms.booking.infrastructure.persistence.entity.DepositFormEntity;
import com.sep490.hdbhms.booking.infrastructure.persistence.entity.DepositContactEventEntity;
import com.sep490.hdbhms.booking.infrastructure.persistence.entity.DepositExtensionEventEntity;
import com.sep490.hdbhms.booking.infrastructure.persistence.jpa.JpaDepositAgreementRepository;
import com.sep490.hdbhms.booking.infrastructure.persistence.jpa.JpaDepositContactEventRepository;
import com.sep490.hdbhms.booking.infrastructure.persistence.jpa.JpaDepositExtensionEventRepository;
import com.sep490.hdbhms.booking.infrastructure.persistence.jpa.JpaDepositFormRepository;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaRoomRepository;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DepositAgreementLifecycleService {
    JpaDepositAgreementRepository depositAgreementRepository;
    JpaDepositFormRepository depositFormRepository;
    JpaDepositContactEventRepository contactEventRepository;
    JpaDepositExtensionEventRepository extensionEventRepository;
    JpaUserRepository userRepository;
    JpaRoomRepository roomRepository;

    @Transactional(readOnly = true)
    public LifecycleSnapshot snapshot(Long depositAgreementId) {
        DepositFormEntity agreement = getAgreement(depositAgreementId);
        return snapshot(agreement);
    }

    @Transactional
    public void recordContact(Long depositAgreementId, Long actorId, DepositContactOutcome outcome, String note) {
        DepositFormEntity agreement = getAgreement(depositAgreementId);
        if (!DepositLifecyclePolicy.isActive(agreement.getDepositStatus())) {
            throw new AppException(ApiErrorCode.DEPOSIT_CONTACT_NOT_ALLOWED);
        }
        LocalDate today = LocalDate.now();
        if (today.isBefore(agreement.getExpectedMoveInDate())) {
            throw new AppException(ApiErrorCode.DEPOSIT_CONTACT_DATE_NOT_REACHED);
        }
        saveContactEvent(agreement, actorId, outcome, note);
    }

    @Transactional
    public void extend(Long depositAgreementId, Long actorId, int additionalDays, String reason) {
        DepositFormEntity agreement = getAgreement(depositAgreementId);
        LocalDate oldExpectedMoveInDate = agreement.getExpectedMoveInDate();
        LocalDate oldExpiresAt = effectiveExpiresAt(agreement);
        int extensionCount = valueOrZero(agreement.getExtensionCount());
        int maxExtensions = agreement.getMaxExtensions() == null ? 1 : agreement.getMaxExtensions();

        LocalDate newExpectedMoveInDate;
        try {
            newExpectedMoveInDate = DepositLifecyclePolicy.calculateExtensionDate(
                    agreement.getDepositStatus(),
                    oldExpectedMoveInDate,
                    extensionCount,
                    maxExtensions,
                    additionalDays,
                    LocalDate.now()
            );
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw new AppException(ApiErrorCode.DEPOSIT_EXTENSION_INVALID);
        }

        LocalDate newExpiresAt = DepositLifecyclePolicy.forfeitureDecisionDate(newExpectedMoveInDate);
        agreement.setExpectedMoveInDate(newExpectedMoveInDate);
        agreement.setDepositExpiresAt(newExpiresAt);
        agreement.setExtensionCount(extensionCount + 1);
        agreement.setDepositStatus(DepositAgreementStatus.EXTENDED);

        DepositFormEntity saved = depositAgreementRepository.save(agreement);
        var actor = userRepository.getReferenceById(actorId);
        extensionEventRepository.save(DepositExtensionEventEntity.builder()
                .depositAgreement(saved)
                .oldExpectedMoveInDate(oldExpectedMoveInDate)
                .newExpectedMoveInDate(newExpectedMoveInDate)
                .oldExpiresAt(oldExpiresAt)
                .newExpiresAt(newExpiresAt)
                .reason(reason.trim())
                .approvedBy(actor)
                .approvedAt(LocalDateTime.now())
                .build());
        saveContactEvent(saved, actorId, DepositContactOutcome.REACHED, "Khách xin gia hạn: " + reason.trim());
    }

    @Transactional
    public void forfeit(Long depositAgreementId, String reason) {
        DepositFormEntity agreement = getAgreement(depositAgreementId);
        LifecycleSnapshot lifecycle = snapshot(agreement);
        if (!lifecycle.forfeitureEligible()) {
            throw new AppException(ApiErrorCode.DEPOSIT_FORFEITURE_NOT_ELIGIBLE);
        }

        agreement.setDepositStatus(DepositAgreementStatus.FORFEITED);
        agreement.setForfeitureReason(reason.trim());
        depositAgreementRepository.save(agreement);

        if (agreement.getRoom() != null) {
            agreement.getRoom().setCurrentStatus(RoomStatus.VACANT);
            roomRepository.save(agreement.getRoom());
        }
    }

    private LifecycleSnapshot snapshot(DepositFormEntity agreement) {
        DepositContactEventEntity latestContact = contactEventRepository
                .findFirstByDepositAgreement_IdOrderByContactedAtDescIdDesc(agreement.getId())
                .orElse(null);
        LocalDate today = LocalDate.now();
        LocalDate expectedMoveInDate = agreement.getExpectedMoveInDate();
        LocalDateTime lastContactedAt = latestContact == null ? null : latestContact.getContactedAt();
        DepositContactOutcome contactOutcome = latestContact == null ? null : latestContact.getOutcome();
        int extensionCount = valueOrZero(agreement.getExtensionCount());
        int maxExtensions = agreement.getMaxExtensions() == null ? 1 : agreement.getMaxExtensions();
        long overdueDays = DepositLifecyclePolicy.overdueDays(
                agreement.getDepositStatus(), expectedMoveInDate, today
        );
        boolean canExtend = DepositLifecyclePolicy.isActive(agreement.getDepositStatus())
                && extensionCount < maxExtensions
                && !expectedMoveInDate.plusDays(DepositLifecyclePolicy.MAX_EXTENSION_DAYS).isBefore(today);

        return new LifecycleSnapshot(
                extensionCount,
                maxExtensions,
                effectiveExpiresAt(agreement),
                DepositLifecyclePolicy.forfeitureDecisionDate(expectedMoveInDate),
                overdueDays,
                latestContact == null ? null : latestContact.getOutcome(),
                lastContactedAt,
                latestContact == null ? null : latestContact.getNote(),
                DepositLifecyclePolicy.isContactRequired(
                        agreement.getDepositStatus(), expectedMoveInDate, today, lastContactedAt
                ),
                canExtend,
                DepositLifecyclePolicy.isForfeitureEligible(
                    agreement.getDepositStatus(), expectedMoveInDate, today, contactOutcome, lastContactedAt
                )
        );
    }

    private void saveContactEvent(
            DepositFormEntity agreement,
            Long actorId,
            DepositContactOutcome outcome,
            String note
    ) {
        contactEventRepository.save(DepositContactEventEntity.builder()
                .depositAgreement(agreement)
                .outcome(outcome)
                .note(note.trim())
                .contactedBy(userRepository.getReferenceById(actorId))
                .contactedAt(LocalDateTime.now())
                .build());
    }

    private DepositFormEntity getAgreement(Long depositAgreementId) {
        return depositAgreementRepository.findById(depositAgreementId)
                .orElseThrow(() -> new AppException(ApiErrorCode.DEPOSIT_AGREEMENT_CONTRACT_NOT_FOUND));
    }

    private LocalDate effectiveExpiresAt(DepositFormEntity agreement) {
        return agreement.getDepositExpiresAt() != null
                ? agreement.getDepositExpiresAt()
                : DepositLifecyclePolicy.forfeitureDecisionDate(agreement.getExpectedMoveInDate());
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    public record LifecycleSnapshot(
            int extensionCount,
            int maxExtensions,
            LocalDate depositExpiresAt,
            LocalDate forfeitureDecisionDate,
            long overdueDays,
            DepositContactOutcome latestContactOutcome,
            LocalDateTime lastContactedAt,
            String lastContactNote,
            boolean contactRequired,
            boolean canExtend,
            boolean forfeitureEligible
    ) {
    }
}
