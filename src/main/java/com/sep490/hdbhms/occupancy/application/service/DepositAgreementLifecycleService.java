package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.billingandpayment.domain.value_objects.DepositAgreementStatus;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaUserRepository;
import com.sep490.hdbhms.occupancy.domain.value_objects.DepositContactOutcome;
import com.sep490.hdbhms.occupancy.domain.value_objects.RoomStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.DepositAgreementEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.DepositContactEventEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.DepositExtensionEventEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaDepositAgreementRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaDepositContactEventRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaDepositExtensionEventRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaDepositFormRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
    DepositContractDocumentService depositContractDocumentService;

    @Transactional(readOnly = true)
    public LifecycleSnapshot snapshot(Long depositAgreementId) {
        DepositAgreementEntity agreement = getAgreement(depositAgreementId);
        return snapshot(agreement);
    }

    @Transactional
    public void recordContact(Long depositAgreementId, Long actorId, DepositContactOutcome outcome, String note) {
        DepositAgreementEntity agreement = getAgreement(depositAgreementId);
        if (!DepositLifecyclePolicy.isActive(agreement.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ ghi nhận liên hệ cho khoản cọc đang giữ chỗ.");
        }
        LocalDate today = LocalDate.now();
        if (today.isBefore(agreement.getExpectedMoveInDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ liên hệ xử lý từ ngày khách dự kiến vào ở.");
        }
        saveContactEvent(agreement, actorId, outcome, note);
    }

    @Transactional
    public void extend(Long depositAgreementId, Long actorId, int additionalDays, String reason) {
        DepositAgreementEntity agreement = getAgreement(depositAgreementId);
        LocalDate oldExpectedMoveInDate = agreement.getExpectedMoveInDate();
        LocalDate oldExpiresAt = effectiveExpiresAt(agreement);
        int extensionCount = valueOrZero(agreement.getExtensionCount());
        int maxExtensions = agreement.getMaxExtensions() == null ? 1 : agreement.getMaxExtensions();

        LocalDate newExpectedMoveInDate;
        try {
            newExpectedMoveInDate = DepositLifecyclePolicy.calculateExtensionDate(
                    agreement.getStatus(),
                    oldExpectedMoveInDate,
                    extensionCount,
                    maxExtensions,
                    additionalDays,
                    LocalDate.now()
            );
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
        }

        LocalDate newExpiresAt = DepositLifecyclePolicy.forfeitureDecisionDate(newExpectedMoveInDate);
        agreement.setExpectedMoveInDate(newExpectedMoveInDate);
        agreement.setDepositExpiresAt(newExpiresAt);
        agreement.setExtensionCount(extensionCount + 1);
        agreement.setStatus(DepositAgreementStatus.EXTENDED);

        if (agreement.getDepositForm() != null) {
            agreement.getDepositForm().setExpectedMoveInDate(newExpectedMoveInDate);
            agreement.getDepositForm().setDepositExpiresAt(newExpiresAt);
            depositFormRepository.save(agreement.getDepositForm());
        }

        DepositAgreementEntity saved = depositAgreementRepository.save(agreement);
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
        depositContractDocumentService.regenerateOfficialContractAfterCommit(saved.getId());
    }

    @Transactional
    public void forfeit(Long depositAgreementId, String reason) {
        DepositAgreementEntity agreement = getAgreement(depositAgreementId);
        LifecycleSnapshot lifecycle = snapshot(agreement);
        if (!lifecycle.forfeitureEligible()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Chỉ được xử lý mất cọc sau 14 ngày quá hạn và lần liên hệ gần nhất ghi nhận không liên lạc được."
            );
        }

        agreement.setStatus(DepositAgreementStatus.FORFEITED);
        agreement.setForfeitureReason(reason.trim());
        depositAgreementRepository.save(agreement);

        if (agreement.getRoom() != null) {
            agreement.getRoom().setCurrentStatus(RoomStatus.VACANT);
            roomRepository.save(agreement.getRoom());
        }
    }

    private LifecycleSnapshot snapshot(DepositAgreementEntity agreement) {
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
                agreement.getStatus(), expectedMoveInDate, today
        );
        boolean canExtend = DepositLifecyclePolicy.isActive(agreement.getStatus())
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
                        agreement.getStatus(), expectedMoveInDate, today, lastContactedAt
                ),
                canExtend,
                DepositLifecyclePolicy.isForfeitureEligible(
                        agreement.getStatus(), expectedMoveInDate, today, contactOutcome, lastContactedAt
                )
        );
    }

    private void saveContactEvent(
            DepositAgreementEntity agreement,
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

    private DepositAgreementEntity getAgreement(Long depositAgreementId) {
        return depositAgreementRepository.findById(depositAgreementId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hợp đồng đặt cọc."));
    }

    private LocalDate effectiveExpiresAt(DepositAgreementEntity agreement) {
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
