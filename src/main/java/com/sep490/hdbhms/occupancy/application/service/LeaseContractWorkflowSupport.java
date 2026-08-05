package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.property.application.service.RoomCommitmentChecker;

import com.sep490.hdbhms.billingandpayment.domain.value_objects.DepositAgreementStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.LeaseStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.OccupantRole;
import com.sep490.hdbhms.property.domain.value_objects.RoomStatus;
import com.sep490.hdbhms.booking.infrastructure.persistence.entity.DepositAgreementEntity;
import com.sep490.hdbhms.booking.infrastructure.persistence.entity.DepositFormCoOccupantEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeaseContractEntity;
import com.sep490.hdbhms.property.infrastructure.persistence.entity.RoomEntity;
import com.sep490.hdbhms.booking.infrastructure.persistence.jpa.JpaDepositAgreementRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaLeaseContractRepository;
import com.sep490.hdbhms.shared.utils.AuthUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LeaseContractWorkflowSupport {
    static final List<LeaseStatus> BLOCKING_ACTIVE_CONTRACT_STATUSES = List.of(
            LeaseStatus.ACTIVE,
            LeaseStatus.EXPIRING_SOON,
            LeaseStatus.TERMINATION_PENDING
    );

    JdbcTemplate jdbcTemplate;
    JpaLeaseContractRepository leaseContractRepository;
    JpaDepositAgreementRepository depositAgreementRepository;
    RoomCommitmentChecker roomCommitmentChecker;

    DepositAgreementEntity getReadyDeposit(Long depositAgreementId) {
        DepositAgreementEntity deposit = depositAgreementRepository.findById(depositAgreementId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hợp đồng đặt cọc."));
        if (deposit.getStatus() != DepositAgreementStatus.PAID
                && deposit.getStatus() != DepositAgreementStatus.CONFIRMED
                && deposit.getStatus() != DepositAgreementStatus.CONVERTED_TO_LEASE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ được tạo hợp đồng thuê từ cọc đã thanh toán.");
        }
        if (deposit.getDepositorPersonProfile() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hợp đồng cọc chưa có hồ sơ người ký chính.");
        }
        if (deposit.getRoom() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hợp đồng cọc chưa gắn phòng.");
        }
        return deposit;
    }

    LeaseContractEntity findLatestContractByDeposit(Long depositAgreementId) {
        return jdbcTemplate.query("""
                        SELECT lease_contract_id AS id FROM lease_contracts
                        WHERE deposit_agreement_id = ? AND deleted_at IS NULL
                        ORDER BY lease_contract_id DESC LIMIT 1
                        """,
                rs -> rs.next()
                        ? leaseContractRepository.findById(rs.getLong("id")).orElse(null)
                        : null,
                depositAgreementId
        );
    }

    Integer resolvePaymentCycleMonths(DepositAgreementEntity deposit) {
        if (deposit.getDepositForm() != null && deposit.getDepositForm().getPaymentCycleMonths() != null) {
            return deposit.getDepositForm().getPaymentCycleMonths();
        }
        return 1;
    }

    void validateDraftInput(
            RoomEntity room,
            Long primaryTenantProfileId,
            LocalDate startDate,
            LocalDate endDate,
            Integer paymentCycleMonths,
            Long monthlyRent,
            Long depositAmount,
            int occupantsCount
    ) {
        if (room == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hợp đồng phải gắn với phòng.");
        }
        if (primaryTenantProfileId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hợp đồng phải có người ký chính.");
        }
        if (startDate == null || endDate == null || !endDate.isAfter(startDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ngày bắt đầu và ngày kết thúc hợp đồng không hợp lệ.");
        }
        validateContractTerms(startDate, paymentCycleMonths, monthlyRent, depositAmount);
        boolean soonVacantDraft = room.getCurrentStatus() == RoomStatus.SOON_VACANT;
        if (room.getCurrentStatus() != RoomStatus.RESERVED
                && room.getCurrentStatus() != RoomStatus.VACANT
                && !soonVacantDraft) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ được tạo hợp đồng cho phòng trống, sắp trống hoặc phòng đã đặt cọc.");
        }
        if (soonVacantDraft) {
            validateSoonVacantMoveInDate(room.getId(), startDate);
        } else if (leaseContractRepository.existsByRoom_IdAndStatusInAndDeletedAtIsNull(room.getId(), BLOCKING_ACTIVE_CONTRACT_STATUSES)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phòng đã có hợp đồng đang hiệu lực.");
        }
        assertRoomHasNoPendingContract(room);
        Integer maxOccupants = room.getMaxOccupants() != null ? room.getMaxOccupants() : 3;
        if (occupantsCount > maxOccupants) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số người ở vượt quá số người tối đa của phòng.");
        }
    }

    void validateContractTerms(
            LocalDate startDate,
            Integer paymentCycleMonths,
            Long monthlyRent,
            Long depositAmount
    ) {
        if (startDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ngày bắt đầu hợp đồng là bắt buộc.");
        }
        if (!Objects.equals(paymentCycleMonths, 1) && !Objects.equals(paymentCycleMonths, 3)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chu kỳ thanh toán chỉ được là 1 hoặc 3 tháng.");
        }
        if (monthlyRent == null || monthlyRent <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giá thuê hằng tháng phải lớn hơn 0.");
        }
        if (depositAmount == null || depositAmount < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tiền cọc phải lớn hơn hoặc bằng 0.");
        }
    }

    LocalDate resolveRentStartDate(LocalDate startDate) {
        if (startDate.getDayOfMonth() <= 10) {
            return startDate;
        }
        return startDate.plusMonths(1).withDayOfMonth(1);
    }

    int countRequestedOccupants(DepositAgreementEntity deposit) {
        int coOccupantCount = deposit.getDepositForm() != null && deposit.getDepositForm().getCoOccupants() != null
                ? (int) deposit.getDepositForm().getCoOccupants().stream()
                .filter(item -> item.getPhone() == null || deposit.getDepositorPersonProfile() == null
                        || deposit.getDepositorPersonProfile().getPhone() == null
                        || !normalizePhone(item.getPhone()).equals(normalizePhone(deposit.getDepositorPersonProfile().getPhone())))
                .count()
                : 0;
        return 1 + coOccupantCount;
    }

    void ensureContractOccupants(LeaseContractEntity contract, DepositAgreementEntity deposit) {
        if (contract == null || contract.getPrimaryTenantProfile() == null || contract.getRoom() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hợp đồng chưa đủ thông tin người ở.");
        }
        LocalDate moveInDate = contract.getStartDate();
        Long propertyId = contract.getRoom().getProperty() != null ? contract.getRoom().getProperty().getId() : null;
        Long primaryProfileId = contract.getPrimaryTenantProfile().getId();
        insertContractOccupantIfAbsent(
                contract.getId(),
                resolveTenantIdForProfile(primaryProfileId, propertyId),
                primaryProfileId,
                OccupantRole.PRIMARY.name(),
                moveInDate
        );

        if (deposit == null || deposit.getDepositForm() == null || deposit.getDepositForm().getCoOccupants() == null) {
            return;
        }
        String primaryPhone = contract.getPrimaryTenantProfile().getPhone();
        for (DepositFormCoOccupantEntity coOccupant : deposit.getDepositForm().getCoOccupants()) {
            if (coOccupant == null || isSamePhone(primaryPhone, coOccupant.getPhone())) {
                continue;
            }
            Long profileId = resolveOrCreateCoOccupantProfile(coOccupant);
            insertContractOccupantIfAbsent(
                    contract.getId(),
                    resolveTenantIdForProfile(profileId, propertyId),
                    profileId,
                    OccupantRole.CO_OCCUPANT.name(),
                    moveInDate
            );
        }
    }

    void copyContractOccupants(LeaseContractEntity oldContract, LeaseContractEntity newContract) {
        jdbcTemplate.update("""
                        INSERT INTO contract_occupants (
                            contract_id,
                            tenant_id,
                            tenant_profile_id,
                            occupant_role,
                            move_in_date,
                            move_out_date,
                            status,
                            created_at
                        )
                        SELECT
                            ?,
                            tenant_id,
                            tenant_profile_id,
                            occupant_role,
                            ?,
                            NULL,
                            'ACTIVE',
                            NOW(6)
                        FROM contract_occupants source_occupant
                        WHERE source_occupant.contract_id = ?
                          AND source_occupant.status = 'ACTIVE'
                          AND NOT EXISTS (
                              SELECT 1
                              FROM contract_occupants existing_occupant
                              WHERE existing_occupant.contract_id = ?
                                AND existing_occupant.tenant_profile_id <=> source_occupant.tenant_profile_id
                          )
                        """,
                newContract.getId(),
                newContract.getStartDate(),
                oldContract.getId(),
                newContract.getId()
        );
        copyLegacyDepositOccupants(oldContract, newContract);
        insertContractOccupantIfAbsent(
                newContract.getId(),
                resolveTenantIdForProfile(
                        newContract.getPrimaryTenantProfile().getId(),
                        newContract.getRoom().getProperty().getId()
                ),
                newContract.getPrimaryTenantProfile().getId(),
                OccupantRole.PRIMARY.name(),
                newContract.getStartDate()
        );
    }

    void appendContractEvent(Long contractId, String eventType, String eventData) {
        jdbcTemplate.update("""
                        INSERT INTO contract_events (
                            contract_id,
                            event_type,
                            event_data,
                            created_by,
                            created_at
                        )
                        VALUES (?, ?, ?, ?, NOW(6))
                        """,
                contractId,
                eventType,
                eventData != null ? eventData.getBytes(StandardCharsets.UTF_8) : null,
                AuthUtils.getCurrentAuthenticationId()
        );
    }

    void appendRoomStatusHistory(Long roomId, RoomStatus fromStatus, RoomStatus toStatus, String reason) {
        jdbcTemplate.update("""
                        INSERT INTO room_status_history (
                            room_id,
                            from_status,
                            to_status,
                            reason,
                            changed_by,
                            changed_at
                        )
                        VALUES (?, ?, ?, ?, ?, NOW(6))
                        """,
                roomId,
                fromStatus != null ? fromStatus.name() : null,
                toStatus.name(),
                reason,
                AuthUtils.getCurrentAuthenticationId()
        );
    }

    boolean hasOtherActiveContract(Long roomId, Long leaseContractId, Long allowedPreviousContractId) {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM lease_contracts
                        WHERE room_id = ?
                          AND lease_contract_id <> ?
                          AND (? IS NULL OR lease_contract_id <> ?)
                          AND status IN ('ACTIVE', 'EXPIRING_SOON', 'TERMINATION_PENDING')
                          AND deleted_at IS NULL
                        """,
                Integer.class,
                roomId,
                leaseContractId,
                allowedPreviousContractId,
                allowedPreviousContractId
        );
        return count != null && count > 0;
    }

    boolean isHolderReplacementLiquidation(Long contractId) {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM change_requests
                        WHERE request_type = 'CONTRACT_LIQUIDATION'
                          AND target_type = 'CONTRACT'
                          AND target_id = ?
                          AND status IN ('PENDING', 'PROCESSING', 'APPROVED', 'COMPLETED')
                          AND JSON_UNQUOTE(JSON_EXTRACT(request_payload, '$.liquidationMode')) = ?
                        """,
                Integer.class,
                contractId,
                LeaseContractManagementService.LIQUIDATION_MODE_PRIMARY_LEAVES_CO_OCCUPANT_STAYS
        );
        if (count != null && count > 0) {
            return true;
        }
        return leaseContractRepository.findById(contractId)
                .flatMap(contract -> leaseContractRepository
                        .findFirstByPreviousContract_IdAndDeletedAtIsNullOrderByIdDesc(contractId)
                        .filter(replacement -> replacement.getStatus() != LeaseStatus.CANCELLED
                                && replacement.getStatus() != LeaseStatus.LIQUIDATED)
                        .filter(replacement -> replacement.getRoom() != null
                                && contract.getRoom() != null
                                && Objects.equals(replacement.getRoom().getId(), contract.getRoom().getId()))
                        .filter(replacement -> replacement.getPrimaryTenantProfile() != null
                                && contract.getPrimaryTenantProfile() != null
                                && !Objects.equals(
                                replacement.getPrimaryTenantProfile().getId(),
                                contract.getPrimaryTenantProfile().getId()
                        )))
                .isPresent();
    }

    void ensureNotRoomTransferManagedContract(Long leaseContractId) {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM room_transfer_requests
                        WHERE new_contract_id = ? OR replacement_old_contract_id = ?
                        """,
                Integer.class,
                leaseContractId,
                leaseContractId
        );
        if (count != null && count > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Hợp đồng thuộc yêu cầu chuyển phòng; vui lòng xử lý bàn giao/kích hoạt trong luồng chuyển phòng."
            );
        }
    }

    private void assertRoomHasNoPendingContract(RoomEntity room) {
        leaseContractRepository
                .findFirstByRoom_IdAndStatusInAndDeletedAtIsNullOrderByIdDesc(
                        room.getId(),
                        List.of(LeaseStatus.DRAFT, LeaseStatus.PENDING_SIGNATURE)
                )
                .ifPresent(contract -> {
                    String roomCode = room.getRoomCode() != null ? room.getRoomCode() : String.valueOf(room.getId());
                    String contractCode = contract.getContractCode() != null
                            ? contract.getContractCode()
                            : "#" + contract.getId();
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "FUTURE_CONTRACT_EXISTS: Phòng " + roomCode
                                    + " đã có hợp đồng " + contractCode
                                    + " ở trạng thái " + contract.getStatus()
                                    + ". Vui lòng xử lý hợp đồng này trước."
                    );
                });
    }

    private void validateSoonVacantMoveInDate(Long roomId, LocalDate expectedMoveInDate) {
        LocalDate expectedVacantDate = roomCommitmentChecker.findExpectedVacantDateForBooking(roomId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "EXPECTED_VACANT_DATE_MISSING: Phòng sắp trống chưa có ngày dự kiến bàn giao."
                ));
        if (expectedMoveInDate.isBefore(expectedVacantDate)) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "EXPECTED_MOVE_IN_BEFORE_VACANT_DATE: Ngày dự kiến vào ở phải sau hoặc bằng ngày phòng dự kiến trống."
            );
        }
    }

    private void insertContractOccupantIfAbsent(
            Long contractId,
            Long tenantId,
            Long tenantProfileId,
            String occupantRole,
            LocalDate moveInDate
    ) {
        Integer exists = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM contract_occupants
                        WHERE contract_id = ?
                          AND tenant_profile_id = ?
                        """,
                Integer.class,
                contractId,
                tenantProfileId
        );
        if (exists != null && exists > 0) {
            return;
        }
        jdbcTemplate.update("""
                        INSERT IGNORE INTO contract_occupants (
                            contract_id,
                            tenant_id,
                            tenant_profile_id,
                            occupant_role,
                            move_in_date,
                            status,
                            created_at
                        )
                        VALUES (?, ?, ?, ?, ?, 'ACTIVE', NOW(6))
                        """,
                contractId,
                tenantId,
                tenantProfileId,
                occupantRole,
                moveInDate
        );
    }

    private void copyLegacyDepositOccupants(
            LeaseContractEntity oldContract,
            LeaseContractEntity newContract
    ) {
        DepositAgreementEntity deposit = oldContract.getDepositAgreement();
        if (deposit == null
                || deposit.getDepositForm() == null
                || deposit.getDepositForm().getCoOccupants() == null) {
            return;
        }
        String primaryPhone = oldContract.getPrimaryTenantProfile() != null
                ? oldContract.getPrimaryTenantProfile().getPhone()
                : null;
        Long propertyId = newContract.getRoom() != null && newContract.getRoom().getProperty() != null
                ? newContract.getRoom().getProperty().getId()
                : null;

        for (DepositFormCoOccupantEntity coOccupant : deposit.getDepositForm().getCoOccupants()) {
            if (coOccupant == null || isSamePhone(primaryPhone, coOccupant.getPhone())) {
                continue;
            }
            Long profileId = findExistingProfileIdByPhone(coOccupant.getPhone());
            if (profileId == null) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Người ở cùng chưa có hồ sơ hiện hữu. Vui lòng cập nhật hồ sơ trước khi tái ký."
                );
            }
            insertContractOccupantIfAbsent(
                    newContract.getId(),
                    resolveTenantIdForProfile(profileId, propertyId),
                    profileId,
                    OccupantRole.CO_OCCUPANT.name(),
                    newContract.getStartDate()
            );
        }
    }

    private Long findExistingProfileIdByPhone(String phone) {
        String normalizedPhone = normalizePhone(phone);
        if (normalizedPhone.isBlank()) {
            return null;
        }
        return jdbcTemplate.query("""
                        SELECT person_profile_id AS id
                        FROM person_profiles
                        WHERE REPLACE(REPLACE(REPLACE(phone, ' ', ''), '.', ''), '-', '') = ?
                          AND deleted_at IS NULL
                        ORDER BY user_id IS NULL, person_profile_id DESC
                        LIMIT 1
                        """,
                rs -> rs.next() ? rs.getLong("id") : null,
                normalizedPhone
        );
    }

    private Long resolveTenantIdForProfile(Long profileId, Long propertyId) {
        if (profileId == null || propertyId == null) {
            return null;
        }
        return jdbcTemplate.query("""
                        SELECT t.tenant_id AS id
                        FROM person_profiles pp
                        JOIN tenants t ON t.user_id = pp.user_id
                        WHERE pp.person_profile_id = ?
                          AND pp.deleted_at IS NULL
                          AND t.property_id = ?
                          AND t.deleted_at IS NULL
                        ORDER BY t.tenant_id DESC
                        LIMIT 1
                        """,
                rs -> rs.next() ? rs.getLong("id") : null,
                profileId,
                propertyId
        );
    }

    private Long resolveOrCreateCoOccupantProfile(DepositFormCoOccupantEntity coOccupant) {
        String normalizedPhone = normalizePhone(coOccupant.getPhone());
        Long existingProfileId = jdbcTemplate.query("""
                        SELECT person_profile_id AS id
                        FROM person_profiles
                        WHERE REPLACE(REPLACE(REPLACE(phone, ' ', ''), '.', ''), '-', '') = ?
                          AND deleted_at IS NULL
                        ORDER BY user_id IS NULL, person_profile_id DESC
                        LIMIT 1
                        """,
                rs -> rs.next() ? rs.getLong("id") : null,
                normalizedPhone
        );
        if (existingProfileId != null) {
            return existingProfileId;
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement("""
                            INSERT INTO person_profiles (
                                full_name,
                                phone,
                                created_at,
                                updated_at
                            )
                            VALUES (?, ?, NOW(6), NOW(6))
                            """,
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, coOccupant.getFullName());
            statement.setString(2, coOccupant.getPhone());
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không tạo được hồ sơ người ở cùng.");
        }
        return key.longValue();
    }

    private boolean isSamePhone(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        return normalizePhone(left).equals(normalizePhone(right));
    }

    private String normalizePhone(String phone) {
        return phone == null ? "" : phone.replaceAll("\\D+", "");
    }
}
