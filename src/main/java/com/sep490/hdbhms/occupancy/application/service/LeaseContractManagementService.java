package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.billingandpayment.domain.value_objects.DepositAgreementStatus;
import com.sep490.hdbhms.file.application.port.in.command.UploadFileCommand;
import com.sep490.hdbhms.file.application.service.UploadFileService;
import com.sep490.hdbhms.file.domain.value_objects.FileCategory;
import com.sep490.hdbhms.file.infrastructure.persistence.jpa.JpaFileMetadataRepository;
import com.sep490.hdbhms.occupancy.domain.value_objects.LeaseStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.LiquidationStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.OccupantRole;
import com.sep490.hdbhms.occupancy.domain.value_objects.RoomStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.ContractLiquidationEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.DepositFormCoOccupantEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.DepositAgreementEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeaseContractEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RoomEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaContractLiquidationRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaDepositAgreementRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaLeaseContractRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomRepository;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.LeaseContractManagementResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.LeaseContractRenewalResponse;
import com.sep490.hdbhms.shared.utils.AuthUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LeaseContractManagementService {
    static final List<LeaseStatus> BLOCKING_ACTIVE_CONTRACT_STATUSES = List.of(
            LeaseStatus.ACTIVE,
            LeaseStatus.EXPIRING_SOON,
            LeaseStatus.TERMINATION_PENDING
    );
    static final Set<String> TENANT_INTENTIONS = Set.of(
            "RENEW",
            "MOVE_OUT",
            "TRANSFER",
            "UNDECIDED"
    );

    JdbcTemplate jdbcTemplate;
    UploadFileService uploadFileService;
    JpaRoomRepository roomRepository;
    JpaFileMetadataRepository fileMetadataRepository;
    JpaLeaseContractRepository leaseContractRepository;
    JpaDepositAgreementRepository depositAgreementRepository;
    JpaContractLiquidationRepository contractLiquidationRepository;
    RoomCommitmentChecker roomCommitmentChecker;

    @Transactional(readOnly = true)
    public List<LeaseContractManagementResponse> findAllForManagement() {
        List<LeaseContractManagementResponse> rows = new ArrayList<>();
        rows.addAll(jdbcTemplate.query("""
                SELECT
                    'DEPOSIT' AS source_type,
                    lc.id AS lease_contract_id,
                    da.id AS deposit_agreement_id,
                    da.deposit_code,
                    lc.contract_code,
                    p.id AS property_id,
                    p.name AS property_name,
                    p.address_line AS property_address,
                    COALESCE((
                        SELECT co.tenant_id
                        FROM contract_occupants co
                        WHERE co.contract_id = lc.id
                          AND co.status = 'ACTIVE'
                        ORDER BY CASE WHEN co.occupant_role = 'PRIMARY' THEN 0 ELSE 1 END, co.id ASC
                        LIMIT 1
                    ), da.tenant_id) AS tenant_id,
                    r.id AS room_id,
                    r.room_code,
                    r.current_status AS room_status,
                    pp.id AS primary_tenant_profile_id,
                    COALESCE(pp.full_name, df.full_name) AS customer_name,
                    COALESCE(pp.phone, df.phone) AS phone,
                    COALESCE(pp.email, df.email) AS email,
                    da.expected_lease_sign_date,
                    da.expected_move_in_date,
                    lc.start_date,
                    lc.end_date,
                    lc.rent_start_date,
                    COALESCE(lc.monthly_rent, r.listed_price) AS monthly_rent,
                    COALESCE(lc.payment_cycle_months, df.payment_cycle_months, 1) AS payment_cycle_months,
                    COALESCE(lc.deposit_amount, da.amount) AS deposit_amount,
                    lc.previous_contract_id,
                    previous_contract.contract_code AS previous_contract_code,
                    lc.tenant_intention,
                    lc.expected_vacant_date,
                    (
                        SELECT renewed.id
                        FROM lease_contracts renewed
                        WHERE renewed.previous_contract_id = lc.id
                          AND renewed.deleted_at IS NULL
                        ORDER BY renewed.id DESC
                        LIMIT 1
                    ) AS renewed_contract_id,
                    (
                        SELECT renewed.contract_code
                        FROM lease_contracts renewed
                        WHERE renewed.previous_contract_id = lc.id
                          AND renewed.deleted_at IS NULL
                        ORDER BY renewed.id DESC
                        LIMIT 1
                    ) AS renewed_contract_code,
                    GREATEST(
                        CASE
                            WHEN lc.id IS NOT NULL THEN (
                                SELECT COUNT(*)
                                FROM contract_occupants co
                                WHERE co.contract_id = lc.id
                                  AND co.status = 'ACTIVE'
                            )
                            ELSE 0
                        END,
                        COALESCE(df.occupant_count, 1),
                        1 + COALESCE((
                            SELECT COUNT(*)
                            FROM deposit_form_co_occupants dco_count
                            WHERE dco_count.deposit_form_id = df.id
                        ), 0)
                    ) AS occupants_count,
                    lc.status AS contract_status,
                    da.status AS deposit_status,
                    lc.contract_file_id,
                    fm.original_name AS contract_file_name,
                    fm.created_at AS contract_file_uploaded_at,
                    lc.signed_at,
                    COALESCE(lc.created_at, da.created_at) AS created_at,
                    u.id AS user_id,
                    u.last_login_at
                FROM deposit_agreements da
                JOIN rooms r ON r.id = da.room_id
                JOIN properties p ON p.id = r.property_id
                LEFT JOIN deposit_forms df ON df.id = da.deposit_form_id
                LEFT JOIN person_profiles pp ON pp.id = da.depositor_person_profile_id
                LEFT JOIN lease_contracts lc ON lc.deposit_agreement_id = da.id AND lc.deleted_at IS NULL
                LEFT JOIN lease_contracts previous_contract ON previous_contract.id = lc.previous_contract_id
                LEFT JOIN file_metadata fm ON fm.id = lc.contract_file_id
                LEFT JOIN users u ON u.id = pp.user_id AND u.deleted_at IS NULL
                WHERE da.status IN ('PAID', 'CONFIRMED', 'CONVERTED_TO_LEASE')
                ORDER BY COALESCE(lc.updated_at, da.updated_at) DESC, da.id DESC
                """, (rs, rowNum) -> toResponse(rs)));
        rows.addAll(jdbcTemplate.query("""
                SELECT
                    'CONTRACT' AS source_type,
                    lc.id AS lease_contract_id,
                    NULL AS deposit_agreement_id,
                    NULL AS deposit_code,
                    lc.contract_code,
                    p.id AS property_id,
                    p.name AS property_name,
                    p.address_line AS property_address,
                    (
                        SELECT co.tenant_id
                        FROM contract_occupants co
                        WHERE co.contract_id = lc.id
                          AND co.status = 'ACTIVE'
                        ORDER BY CASE WHEN co.occupant_role = 'PRIMARY' THEN 0 ELSE 1 END, co.id ASC
                        LIMIT 1
                    ) AS tenant_id,
                    r.id AS room_id,
                    r.room_code,
                    r.current_status AS room_status,
                    pp.id AS primary_tenant_profile_id,
                    pp.full_name AS customer_name,
                    pp.phone,
                    pp.email,
                    NULL AS expected_lease_sign_date,
                    lc.rent_start_date AS expected_move_in_date,
                    lc.start_date,
                    lc.end_date,
                    lc.rent_start_date,
                    lc.monthly_rent,
                    lc.payment_cycle_months,
                    lc.deposit_amount,
                    lc.previous_contract_id,
                    previous_contract.contract_code AS previous_contract_code,
                    lc.tenant_intention,
                    lc.expected_vacant_date,
                    (
                        SELECT renewed.id
                        FROM lease_contracts renewed
                        WHERE renewed.previous_contract_id = lc.id
                          AND renewed.deleted_at IS NULL
                        ORDER BY renewed.id DESC
                        LIMIT 1
                    ) AS renewed_contract_id,
                    (
                        SELECT renewed.contract_code
                        FROM lease_contracts renewed
                        WHERE renewed.previous_contract_id = lc.id
                          AND renewed.deleted_at IS NULL
                        ORDER BY renewed.id DESC
                        LIMIT 1
                    ) AS renewed_contract_code,
                    GREATEST(
                        (
                            SELECT COUNT(*)
                            FROM contract_occupants co
                            WHERE co.contract_id = lc.id
                              AND co.status = 'ACTIVE'
                        ),
                        COALESCE(df.occupant_count, 1),
                        1 + COALESCE((
                            SELECT COUNT(*)
                            FROM deposit_form_co_occupants dco_count
                            WHERE dco_count.deposit_form_id = df.id
                        ), 0)
                    ) AS occupants_count,
                    lc.status AS contract_status,
                    NULL AS deposit_status,
                    lc.contract_file_id,
                    fm.original_name AS contract_file_name,
                    fm.created_at AS contract_file_uploaded_at,
                    lc.signed_at,
                    lc.created_at,
                    u.id AS user_id,
                    u.last_login_at
                FROM lease_contracts lc
                JOIN rooms r ON r.id = lc.room_id
                JOIN properties p ON p.id = r.property_id
                JOIN person_profiles pp ON pp.id = lc.primary_tenant_profile_id
                LEFT JOIN deposit_agreements da ON da.id = lc.deposit_agreement_id
                LEFT JOIN deposit_forms df ON df.id = da.deposit_form_id
                LEFT JOIN lease_contracts previous_contract ON previous_contract.id = lc.previous_contract_id
                LEFT JOIN file_metadata fm ON fm.id = lc.contract_file_id
                LEFT JOIN users u ON u.id = pp.user_id AND u.deleted_at IS NULL
                WHERE lc.deleted_at IS NULL
                  AND lc.deposit_agreement_id IS NULL
                ORDER BY lc.updated_at DESC, lc.id DESC
                """, (rs, rowNum) -> toResponse(rs)));
        return rows;
    }

    public LeaseContractManagementResponse createDraftLeaseContractForDeposit(Long depositAgreementId) {
        DepositAgreementEntity deposit = getReadyDeposit(depositAgreementId);
        LeaseContractEntity existing = findLatestContractByDeposit(depositAgreementId);
        if (existing != null) {
            return findOne(existing.getId());
        }
        LeaseContractEntity created = createDraftLeaseContract(deposit);
        return findOne(created.getId());
    }

    public LeaseContractManagementResponse uploadSignedFileForDeposit(Long depositAgreementId, MultipartFile file) {
        DepositAgreementEntity deposit = getReadyDeposit(depositAgreementId);
        LeaseContractEntity contract = findLatestContractByDeposit(depositAgreementId);
        if (contract == null) {
            contract = createDraftLeaseContract(deposit);
        }
        return uploadSignedFile(contract.getId(), file);
    }

    public LeaseContractManagementResponse uploadSignedFile(Long leaseContractId, MultipartFile file) {
        LeaseContractEntity contract = leaseContractRepository.findById(leaseContractId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay hop dong thue."));
        if (contract.getStatus() == LeaseStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hop dong da ACTIVE, khong upload thay file trong luong nay.");
        }
        var metadata = uploadFileService.execute(new UploadFileCommand(
                AuthUtils.getCurrentAuthenticationId(),
                file,
                FileCategory.CONTRACT,
                true
        ));
        var contractFile = fileMetadataRepository.findById(metadata.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Khong luu duoc file hop dong."));
        contractFile.setCategory(FileCategory.CONTRACT);
        contractFile.setSensitive(true);
        contract.setContractFile(fileMetadataRepository.save(contractFile));
        leaseContractRepository.save(contract);
        return findOne(contract.getId());
    }

    public LeaseContractManagementResponse liquidate(Long leaseContractId, LocalDate liquidationDate, String reason) {
        LeaseContractEntity contract = leaseContractRepository.findById(leaseContractId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hợp đồng thuê."));
        if (contract.getStatus() == LeaseStatus.LIQUIDATED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hop dong da duoc thanh ly.");
        }
        if (contract.getStatus() != LeaseStatus.ACTIVE
                && contract.getStatus() != LeaseStatus.EXPIRING_SOON
                && contract.getStatus() != LeaseStatus.EXPIRED
                && contract.getStatus() != LeaseStatus.TERMINATION_PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Chi thanh ly hop dong dang hieu luc, sap het han, het han hoac cho thanh ly."
            );
        }
        RoomEntity room = contract.getRoom();
        if (room == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hợp đồng chưa gắn phòng.");
        }

        LocalDate finalLiquidationDate = liquidationDate != null ? liquidationDate : LocalDate.now();
        String finalReason = reason == null || reason.isBlank()
                ? "Khách không tiếp tục thuê phòng."
                : reason.trim();
        Long depositAmount = contract.getDepositAmount() != null ? contract.getDepositAmount() : 0L;

        ContractLiquidationEntity liquidation = contractLiquidationRepository.findByContract_Id(contract.getId())
                .orElseGet(() -> ContractLiquidationEntity.builder()
                        .contract(contract)
                        .depositAmount(depositAmount)
                        .build());
        liquidation.setLiquidationDate(finalLiquidationDate);
        liquidation.setReason(finalReason);
        liquidation.setDepositAmount(depositAmount);
        Long deductionAmount = liquidation.getDepositDeductionAmount() != null
                ? liquidation.getDepositDeductionAmount()
                : 0L;
        liquidation.setDepositDeductionAmount(deductionAmount);
        liquidation.setDepositRefundAmount(Math.max(0L, depositAmount - deductionAmount));
        liquidation.setStatus(LiquidationStatus.CONFIRMED);
        contractLiquidationRepository.save(liquidation);

        contract.setStatus(LeaseStatus.LIQUIDATED);
        leaseContractRepository.saveAndFlush(contract);

        jdbcTemplate.update("""
                        UPDATE contract_occupants
                        SET status = 'MOVED_OUT',
                            move_out_date = ?
                        WHERE contract_id = ?
                          AND status = 'ACTIVE'
                        """,
                finalLiquidationDate,
                contract.getId()
        );

        RoomStatus fromStatus = room.getCurrentStatus();
        room.setCurrentStatus(RoomStatus.VACANT);
        roomRepository.saveAndFlush(room);
        appendRoomStatusHistory(room.getId(), fromStatus, RoomStatus.VACANT, "Thanh ly hop dong thue " + contract.getContractCode());
        appendContractEvent(contract.getId(), "LIQUIDATED", finalReason);

        return findOne(contract.getId());
    }

    public LeaseContractRenewalResponse renew(
            Long leaseContractId,
            LocalDate newStartDate,
            LocalDate newEndDate,
            Long monthlyRent,
            Integer paymentCycleMonths,
            Long depositAmount,
            String note
    ) {
        assertOwnerCanRenew();
        LeaseContractEntity oldContract = leaseContractRepository.findById(leaseContractId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay hop dong thue."));
        if (oldContract.getDeletedAt() != null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay hop dong thue.");
        }
        if (!List.of(LeaseStatus.ACTIVE, LeaseStatus.EXPIRING_SOON, LeaseStatus.EXPIRED)
                .contains(oldContract.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Chi duoc tai ky hop dong ACTIVE, EXPIRING_SOON hoac EXPIRED."
            );
        }
        if (leaseContractRepository.existsByPreviousContract_IdAndDeletedAtIsNull(oldContract.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Hop dong nay da co hop dong tai ky.");
        }

        validateContractTerms(newStartDate, newEndDate, paymentCycleMonths, monthlyRent, depositAmount);
        RoomEntity room = oldContract.getRoom();
        if (room == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hop dong chua gan phong.");
        }
        if (hasOtherActiveContract(room.getId(), oldContract.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phong dang co hop dong hieu luc khac.");
        }

        RoomStatus previousRoomStatus = room.getCurrentStatus();
        if (previousRoomStatus == RoomStatus.SOON_VACANT) {
            RoomCommitmentChecker.Blocker blocker =
                    roomCommitmentChecker.checkRenewBlockers(room.getId(), oldContract.getId());
            if (blocker != RoomCommitmentChecker.Blocker.NONE) {
                throwRenewBlocked(blocker);
            }
            oldContract.setTenantIntention("RENEW");
            oldContract.setExpectedVacantDate(null);
            oldContract.setIntentionRecordedAt(LocalDateTime.now());
            leaseContractRepository.saveAndFlush(oldContract);
            room.setCurrentStatus(RoomStatus.OCCUPIED);
            roomRepository.saveAndFlush(room);
            appendRoomStatusHistory(
                    room.getId(),
                    previousRoomStatus,
                    RoomStatus.OCCUPIED,
                    "Khach cu doi y tai ky hop dong " + oldContract.getContractCode()
            );
            appendContractEvent(
                    oldContract.getId(),
                    "RENEWAL_AFTER_MOVE_OUT_INTENT",
                    "Owner xac nhan tai ky sau khi khach da bao chuyen di"
            );
        }

        String newContractCode = resolveRenewalContractCode(oldContract);
        LeaseContractEntity newContract = LeaseContractEntity.builder()
                .contractCode(newContractCode)
                .room(room)
                .primaryTenantProfile(oldContract.getPrimaryTenantProfile())
                .startDate(newStartDate)
                .endDate(newEndDate)
                .rentStartDate(resolveRentStartDate(newStartDate))
                .monthlyRent(monthlyRent)
                .paymentCycleMonths(paymentCycleMonths)
                .depositAmount(depositAmount)
                .status(LeaseStatus.PENDING_SIGNATURE)
                .previousContract(oldContract)
                .build();
        newContract = leaseContractRepository.save(newContract);
        copyContractOccupants(oldContract, newContract);

        RoomStatus currentRoomStatus = room.getCurrentStatus();
        if (currentRoomStatus != RoomStatus.OCCUPIED) {
            room.setCurrentStatus(RoomStatus.OCCUPIED);
            roomRepository.save(room);
            appendRoomStatusHistory(
                    room.getId(),
                    currentRoomStatus,
                    RoomStatus.OCCUPIED,
                    "Tao hop dong tai ky " + newContractCode
            );
        }

        String eventNote = note == null || note.isBlank() ? "Tao hop dong tai ky" : note.trim();
        appendContractEvent(
                newContract.getId(),
                "CREATED",
                "Tai ky tu hop dong " + oldContract.getContractCode() + "; note=" + eventNote
        );

        List<LeaseContractRenewalResponse.OccupantInfo> occupants = findRenewalOccupants(newContract.getId());
        return new LeaseContractRenewalResponse(
                oldContract.getId(),
                oldContract.getContractCode(),
                oldContract.getStatus(),
                newContract.getId(),
                newContract.getContractCode(),
                newContract.getStatus(),
                oldContract.getId(),
                room.getId(),
                room.getRoomCode(),
                occupants.size(),
                occupants
        );
    }

    public LeaseContractManagementResponse recordTenantIntention(
            Long leaseContractId,
            String intention,
            LocalDate expectedMoveOutDate,
            String note
    ) {
        return recordTenantIntention(leaseContractId, intention, expectedMoveOutDate, note, "MANAGEMENT_WEB");
    }

    private LeaseContractManagementResponse recordTenantIntention(
            Long leaseContractId,
            String intention,
            LocalDate expectedMoveOutDate,
            String note,
            String source
    ) {
        lockContractAndRoom(leaseContractId);
        LeaseContractEntity contract = leaseContractRepository.findById(leaseContractId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay hop dong thue."));
        if (contract.getDeletedAt() != null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay hop dong thue.");
        }
        if (!List.of(LeaseStatus.ACTIVE, LeaseStatus.EXPIRING_SOON).contains(contract.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Chi ghi nhan y dinh cho hop dong ACTIVE hoac EXPIRING_SOON."
            );
        }

        String normalizedIntention = normalizeTenantIntention(intention);
        log.info(normalizedIntention);
        if (!TENANT_INTENTIONS.contains(normalizedIntention)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Y dinh khach khong hop le.");
        }
        LocalDate today = LocalDate.now();
        boolean withinThreeMonths = isWithinThreeMonths(contract, today);
        if (List.of("MOVE_OUT", "TRANSFER").contains(normalizedIntention)) {
            if (!withinThreeMonths) {
                throw new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "INTENTION_TOO_EARLY: Chi ghi nhan MOVE_OUT/TRANSFER khi hop dong con 3 thang tro xuong."
                );
            }
            if (expectedMoveOutDate == null) {
                throw new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "EXPECTED_MOVE_OUT_DATE_REQUIRED: Can co ngay du kien ban giao phong."
                );
            }
            if (expectedMoveOutDate.isBefore(today)) {
                throw new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "EXPECTED_MOVE_OUT_DATE_IN_PAST: Ngay du kien ban giao khong duoc truoc hom nay."
                );
            }
            if (contract.getEndDate() != null && expectedMoveOutDate.isAfter(contract.getEndDate())) {
                throw new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "EXPECTED_MOVE_OUT_DATE_AFTER_CONTRACT_END: Ngay du kien ban giao khong duoc sau ngay ket thuc hop dong."
                );
            }
        }

        RoomEntity room = contract.getRoom();
        if (room == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hop dong chua gan phong.");
        }
        LocalDateTime now = LocalDateTime.now();
        contract.setTenantIntention(normalizedIntention);
        contract.setIntentionRecordedAt(now);

        if (List.of("MOVE_OUT", "TRANSFER").contains(normalizedIntention)) {
            contract.setExpectedVacantDate(expectedMoveOutDate);
            leaseContractRepository.saveAndFlush(contract);
            RoomStatus fromStatus = room.getCurrentStatus();
            if (fromStatus != RoomStatus.SOON_VACANT) {
                room.setCurrentStatus(RoomStatus.SOON_VACANT);
                roomRepository.saveAndFlush(room);
                appendRoomStatusHistory(
                        room.getId(),
                        fromStatus,
                        RoomStatus.SOON_VACANT,
                        "Khách dự kiến chuyển đi theo hợp đồng " + contract.getContractCode()
                );
            }
        } else {
            contract.setExpectedVacantDate(null);
            if ("RENEW".equals(normalizedIntention)
                    && (room.getCurrentStatus() == RoomStatus.SOON_VACANT
                    || room.getCurrentStatus() == RoomStatus.RESERVED)) {
                RoomCommitmentChecker.Blocker blocker =
                        roomCommitmentChecker.checkRenewBlockers(room.getId(), contract.getId());
                if (blocker != RoomCommitmentChecker.Blocker.NONE) {
                    throwRenewBlocked(blocker);
                }
            }
            if (room.getCurrentStatus() == RoomStatus.SOON_VACANT) {
                RoomStatus fromStatus = room.getCurrentStatus();
                room.setCurrentStatus(RoomStatus.OCCUPIED);
                roomRepository.saveAndFlush(room);
                appendRoomStatusHistory(
                        room.getId(),
                        fromStatus,
                        RoomStatus.OCCUPIED,
                        "Khách đổi ý tiếp tục thuê hợp đồng " + contract.getContractCode()
                );
                if ("RENEW".equals(normalizedIntention)) {
                    appendContractEvent(
                            contract.getId(),
                            "RENEWAL_AFTER_MOVE_OUT_INTENT",
                            "Khách đổi ý tiếp tuc thue sau khi da bao chuyen di"
                    );
                }
            }
            leaseContractRepository.saveAndFlush(contract);
        }
        String eventData = "intention=" + normalizedIntention
                + "; expectedVacantDate=" + (contract.getExpectedVacantDate() != null ? contract.getExpectedVacantDate() : "")
                + "; source=" + (source == null ? "" : source)
                + "; note=" + (note == null ? "" : note.trim());
        appendContractEvent(contract.getId(), "INTENTION_RECORDED", eventData);
        return findOne(contract.getId());
    }

    private void lockContractAndRoom(Long leaseContractId) {
        List<Long> locked = jdbcTemplate.query("""
                        SELECT lc.id
                        FROM lease_contracts lc
                        JOIN rooms r ON r.id = lc.room_id
                        WHERE lc.id = ?
                        FOR UPDATE
                        """,
                (rs, rowNum) -> rs.getLong("id"),
                leaseContractId
        );
        if (locked.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay hop dong thue.");
        }
    }

    public LeaseContractManagementResponse recordTenantIntentionForCurrentUser(
            Long leaseContractId,
            String intention,
            LocalDate expectedMoveOutDate,
            String note
    ) {
        if (currentUserHasRole("ROLE_TENANT")) {
            return recordTenantIntentionForCurrentTenant(leaseContractId, intention, expectedMoveOutDate, note);
        }
        return recordTenantIntention(leaseContractId, intention, expectedMoveOutDate, note);
    }

    public LeaseContractManagementResponse recordTenantIntentionForCurrentTenant(
            Long leaseContractId,
            String intention,
            LocalDate expectedMoveOutDate,
            String note
    ) {
        Long userId = AuthUtils.getCurrentAuthenticationId();
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED");
        }
        Integer contractExists = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM lease_contracts lc
                        WHERE lc.id = ?
                          AND lc.deleted_at IS NULL
                        """,
                Integer.class,
                leaseContractId
        );
        if (contractExists == null || contractExists == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay hop dong thue.");
        }

        Integer isPrimarySigner = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM lease_contracts lc
                        JOIN person_profiles pp ON pp.id = lc.primary_tenant_profile_id
                        LEFT JOIN tenant_account_provisionings tap
                               ON tap.tenant_profile_id = pp.id
                              AND tap.user_id = ?
                        WHERE lc.id = ?
                          AND lc.deleted_at IS NULL
                          AND pp.deleted_at IS NULL
                          AND (pp.user_id = ? OR (tap.user_id = ? AND tap.status <> 'DISABLED'))
                          AND NOT EXISTS (
                              SELECT 1
                              FROM contract_occupants disabled_primary
                              WHERE disabled_primary.contract_id = lc.id
                                AND disabled_primary.tenant_profile_id = pp.id
                                AND disabled_primary.status = 'DISABLED'
                          )
                        """,
                Integer.class,
                userId,
                leaseContractId,
                userId,
                userId
        );
        if (isPrimarySigner == null || isPrimarySigner == 0) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "CONTRACT_INTENTION_PRIMARY_ONLY: Chi nguoi ky chinh cua hop dong moi duoc ghi nhan y dinh."
            );
        }
        return recordTenantIntention(leaseContractId, intention, expectedMoveOutDate, note, "TENANT_MOBILE");
    }

    private boolean currentUserHasRole(String role) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.getAuthorities() != null
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> role.equals(authority.getAuthority()));
    }

    public LeaseContractManagementResponse activate(Long leaseContractId) {
        LeaseContractEntity contract = leaseContractRepository.findById(leaseContractId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay hop dong thue."));
        if (contract.getStatus() == LeaseStatus.ACTIVE) {
            return findOne(leaseContractId);
        }
        if (contract.getStatus() != LeaseStatus.DRAFT && contract.getStatus() != LeaseStatus.PENDING_SIGNATURE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chi duoc kich hoat hop dong dang cho ky.");
        }
        if (contract.getContractFile() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Can upload file hop dong da ky truoc khi kich hoat.");
        }
        if (contract.getPrimaryTenantProfile() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hop dong chua co nguoi ky chinh.");
        }
        if (contract.getStartDate() == null || contract.getEndDate() == null || contract.getEndDate().isBefore(contract.getStartDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ngay bat dau/ket thuc hop dong khong hop le.");
        }
        RoomEntity room = contract.getRoom();
        if (room == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hop dong chua gan phong.");
        }

        // Bắt buộc phải có bản ghi bàn giao MOVE_IN kèm chỉ số điện/nước trước khi kích hoạt
        // Skip check for renewal contracts (previous contract exists)
        if (contract.getPreviousContract() == null) {
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
            if (handoverCount == null || handoverCount == 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Cần hoàn thành bàn giao phòng (nhập số điện/nước và upload biên bản bàn giao đã ký) trước khi kích hoạt hợp đồng."
                );
            }
        }

        boolean renewalActivation = contract.getPreviousContract() != null
                && (room.getCurrentStatus() == RoomStatus.OCCUPIED
                || room.getCurrentStatus() == RoomStatus.EXPIRED);
        if (!renewalActivation
                && room.getCurrentStatus() != RoomStatus.RESERVED
                && room.getCurrentStatus() != RoomStatus.VACANT
                && room.getCurrentStatus() != RoomStatus.ON_HOLD) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phong phai o trang thai trong hoac da dat coc truoc khi kich hoat hop dong.");
        }
        Long previousContractId = contract.getPreviousContract() != null
                ? contract.getPreviousContract().getId()
                : null;
        if (hasOtherActiveContract(room.getId(), contract.getId(), previousContractId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phong da co hop dong dang hieu luc.");
        }

        ensureContractOccupants(contract, contract.getDepositAgreement());
        LeaseContractEntity previousContract = contract.getPreviousContract();
        if (previousContract != null) {
            copyContractOccupants(previousContract, contract);
            boolean legacyPrematureRenewal =
                    previousContract.getStatus() == LeaseStatus.RENEWED
                            && List.of(LeaseStatus.DRAFT, LeaseStatus.PENDING_SIGNATURE)
                            .contains(contract.getStatus());
            if (!legacyPrematureRenewal
                    && !List.of(LeaseStatus.ACTIVE, LeaseStatus.EXPIRING_SOON, LeaseStatus.EXPIRED)
                    .contains(previousContract.getStatus())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Hop dong truoc khong con o trang thai cho phep kich hoat gia han."
                );
            }
            previousContract.setStatus(LeaseStatus.RENEWED);
            leaseContractRepository.saveAndFlush(previousContract);
        }
        contract.setStatus(LeaseStatus.ACTIVE);
        contract.setSignedAt(LocalDateTime.now());
        if (contract.getRentStartDate() == null) {
            contract.setRentStartDate(resolveRentStartDate(contract.getStartDate()));
        }
        leaseContractRepository.save(contract);

        RoomStatus fromStatus = room.getCurrentStatus();
        room.setCurrentStatus(RoomStatus.OCCUPIED);
        roomRepository.save(room);
        appendRoomStatusHistory(room.getId(), fromStatus, RoomStatus.OCCUPIED, "Kich hoat hop dong thue " + contract.getContractCode());

        if (contract.getDepositAgreement() != null
                && contract.getDepositAgreement().getStatus() != DepositAgreementStatus.CONVERTED_TO_LEASE) {
            contract.getDepositAgreement().setStatus(DepositAgreementStatus.CONVERTED_TO_LEASE);
            depositAgreementRepository.save(contract.getDepositAgreement());
        }
        appendContractEvent(contract.getId(), "SIGNED", "Kich hoat hop dong thue");
        if (previousContract != null) {
            appendContractEvent(
                    previousContract.getId(),
                    "RENEWED",
                    "Kich hoat hop dong tai ky; newContractId=" + contract.getId()
            );
        }
        return findOne(contract.getId());
    }

    public LeaseContractManagementResponse updateTerms(
            Long leaseContractId,
            LocalDate startDate,
            LocalDate endDate,
            Integer paymentCycleMonths,
            Long monthlyRent,
            Long depositAmount
    ) {
        LeaseContractEntity contract = leaseContractRepository.findById(leaseContractId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay hop dong thue."));
        if (contract.getDeletedAt() != null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay hop dong thue.");
        }
        if (List.of(
                LeaseStatus.LIQUIDATED,
                LeaseStatus.EXPIRED,
                LeaseStatus.AUTO_TERMINATED,
                LeaseStatus.CANCELLED
        ).contains(contract.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Khong the cap nhat thoi han cua hop dong da ket thuc."
            );
        }

        validateContractTerms(startDate, endDate, paymentCycleMonths, monthlyRent, depositAmount);
        boolean rentChanged = !Objects.equals(contract.getMonthlyRent(), monthlyRent);

        contract.setStartDate(startDate);
        contract.setEndDate(endDate);
        contract.setRentStartDate(resolveRentStartDate(startDate));
        contract.setPaymentCycleMonths(paymentCycleMonths);
        contract.setMonthlyRent(monthlyRent);
        contract.setDepositAmount(depositAmount);
        applyLifecycleStatusAfterTermsUpdate(contract, LocalDate.now());
        leaseContractRepository.save(contract);

        if (rentChanged) {
            appendContractEvent(
                    contract.getId(),
                    "PRICE_CHANGED",
                    "Cap nhat gia thue hang thang thanh " + monthlyRent
            );
        }
        return findOne(contract.getId());
    }

    private void applyLifecycleStatusAfterTermsUpdate(LeaseContractEntity contract, LocalDate today) {
        if (!List.of(LeaseStatus.ACTIVE, LeaseStatus.EXPIRING_SOON).contains(contract.getStatus())
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
        if (newStatus == LeaseStatus.EXPIRING_SOON) {
            appendContractEvent(
                    contract.getId(),
                    "NOTICE_SENT",
                    "Cap nhat thoi han hop dong con duoi hoac bang 3 thang"
            );
            return;
        }
        if (newStatus == LeaseStatus.EXPIRED) {
            appendContractEvent(
                    contract.getId(),
                    "EXPIRED",
                    "Cap nhat thoi han hop dong da qua ngay ket thuc"
            );
            RoomEntity room = contract.getRoom();
            if (room != null && room.getCurrentStatus() == RoomStatus.OCCUPIED) {
                RoomStatus fromStatus = room.getCurrentStatus();
                room.setCurrentStatus(RoomStatus.EXPIRED);
                roomRepository.save(room);
                appendRoomStatusHistory(
                        room.getId(),
                        fromStatus,
                        RoomStatus.EXPIRED,
                        "Hop dong " + contract.getContractCode() + " da het han"
                );
            }
        }
    }

    @Transactional(readOnly = true)
    public LeaseContractManagementResponse findOne(Long leaseContractId) {
        return jdbcTemplate.query("""
                        SELECT
                            CASE WHEN da.id IS NULL THEN 'CONTRACT' ELSE 'DEPOSIT' END AS source_type,
                            lc.id AS lease_contract_id,
                            da.id AS deposit_agreement_id,
                            da.deposit_code,
                            lc.contract_code,
                            p.id AS property_id,
                            p.name AS property_name,
                            p.address_line AS property_address,
                            (
                                SELECT co.tenant_id
                                FROM contract_occupants co
                                WHERE co.contract_id = lc.id
                                  AND co.status = 'ACTIVE'
                                ORDER BY CASE WHEN co.occupant_role = 'PRIMARY' THEN 0 ELSE 1 END, co.id ASC
                                LIMIT 1
                            ) AS tenant_id,
                            r.id AS room_id,
                            r.room_code,
                            r.current_status AS room_status,
                            pp.id AS primary_tenant_profile_id,
                            pp.full_name AS customer_name,
                            pp.phone,
                            pp.email,
                            da.expected_lease_sign_date,
                            COALESCE(da.expected_move_in_date, lc.rent_start_date) AS expected_move_in_date,
                            lc.start_date,
                            lc.end_date,
                            lc.rent_start_date,
                            lc.monthly_rent,
                            lc.payment_cycle_months,
                            lc.deposit_amount,
                            lc.previous_contract_id,
                            previous_contract.contract_code AS previous_contract_code,
                            lc.tenant_intention,
                            lc.expected_vacant_date,
                            (
                                SELECT renewed.id
                                FROM lease_contracts renewed
                                WHERE renewed.previous_contract_id = lc.id
                                  AND renewed.deleted_at IS NULL
                                ORDER BY renewed.id DESC
                                LIMIT 1
                            ) AS renewed_contract_id,
                            (
                                SELECT renewed.contract_code
                                FROM lease_contracts renewed
                                WHERE renewed.previous_contract_id = lc.id
                                  AND renewed.deleted_at IS NULL
                                ORDER BY renewed.id DESC
                                LIMIT 1
                            ) AS renewed_contract_code,
                            GREATEST(
                                (
                                    SELECT COUNT(*)
                                    FROM contract_occupants co
                                    WHERE co.contract_id = lc.id
                                      AND co.status = 'ACTIVE'
                                ),
                                COALESCE(df.occupant_count, 1),
                                1 + COALESCE((
                                    SELECT COUNT(*)
                                    FROM deposit_form_co_occupants dco_count
                                    WHERE dco_count.deposit_form_id = df.id
                                ), 0)
                            ) AS occupants_count,
                            lc.status AS contract_status,
                            da.status AS deposit_status,
                            lc.contract_file_id,
                            fm.original_name AS contract_file_name,
                            fm.created_at AS contract_file_uploaded_at,
                            lc.signed_at,
                            lc.created_at,
                            u.id AS user_id,
                            u.last_login_at
                        FROM lease_contracts lc
                        JOIN rooms r ON r.id = lc.room_id
                        JOIN properties p ON p.id = r.property_id
                        JOIN person_profiles pp ON pp.id = lc.primary_tenant_profile_id
                        LEFT JOIN deposit_agreements da ON da.id = lc.deposit_agreement_id
                        LEFT JOIN deposit_forms df ON df.id = da.deposit_form_id
                        LEFT JOIN lease_contracts previous_contract ON previous_contract.id = lc.previous_contract_id
                        LEFT JOIN file_metadata fm ON fm.id = lc.contract_file_id
                        LEFT JOIN users u ON u.id = pp.user_id AND u.deleted_at IS NULL
                        WHERE lc.deleted_at IS NULL AND lc.id = ?
                        """,
                rs -> {
                    if (!rs.next()) {
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay hop dong thue.");
                    }
                    return toResponse(rs);
                },
                leaseContractId
        );
    }

    private LeaseContractEntity createDraftLeaseContract(DepositAgreementEntity deposit) {
        RoomEntity room = deposit.getRoom();
        LocalDate startDate = deposit.getExpectedMoveInDate();
        if (startDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hop dong coc chua co ngay vao o du kien.");
        }
        LocalDate endDate = startDate.plusYears(1).minusDays(1);
        Integer paymentCycleMonths = resolvePaymentCycleMonths(deposit);
        Long monthlyRent = room.getListedPrice();
        Long depositAmount = deposit.getAmount() != null ? deposit.getAmount() : 0L;
        validateDraftInput(
                room,
                deposit.getDepositorPersonProfile() != null ? deposit.getDepositorPersonProfile().getId() : null,
                startDate,
                endDate,
                paymentCycleMonths,
                monthlyRent,
                depositAmount,
                countRequestedOccupants(deposit)
        );

        String contractCode = "HD-" + startDate.getYear() + "-H" + room.getRoomCode() + "-" + deposit.getId();
        LeaseContractEntity contract = LeaseContractEntity.builder()
                .contractCode(contractCode)
                .room(room)
                .depositAgreement(deposit)
                .primaryTenantProfile(deposit.getDepositorPersonProfile())
                .startDate(startDate)
                .endDate(endDate)
                .rentStartDate(resolveRentStartDate(startDate))
                .monthlyRent(monthlyRent)
                .paymentCycleMonths(paymentCycleMonths)
                .depositAmount(depositAmount)
                .status(LeaseStatus.PENDING_SIGNATURE)
                .build();
        LeaseContractEntity saved = leaseContractRepository.save(contract);
        ensureContractOccupants(saved, deposit);
        appendContractEvent(saved.getId(), "CREATED", "Tao hop dong thue tu hop dong coc " + deposit.getId());
        return saved;
    }

    private void validateDraftInput(
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hop dong phai gan voi phong.");
        }
        if (primaryTenantProfileId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hop dong phai co nguoi ky chinh.");
        }
        if (startDate == null || endDate == null || !endDate.isAfter(startDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ngay bat dau va ngay ket thuc hop dong khong hop le.");
        }
        if (!Objects.equals(paymentCycleMonths, 1) && !Objects.equals(paymentCycleMonths, 3)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chu ky thanh toan chi duoc la 1 hoac 3 thang.");
        }
        if (monthlyRent == null || monthlyRent <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Gia thue hang thang phai lon hon 0.");
        }
        if (depositAmount == null || depositAmount < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tien coc khong hop le.");
        }
        boolean soonVacantDraft = room.getCurrentStatus() == RoomStatus.SOON_VACANT;
        if (room.getCurrentStatus() != RoomStatus.RESERVED
                && room.getCurrentStatus() != RoomStatus.VACANT
                && !soonVacantDraft) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chi duoc tao hop dong cho phong trong, sap trong hoac phong da dat coc.");
        }
        if (soonVacantDraft) {
            validateSoonVacantMoveInDate(room.getId(), startDate);
        } else if (leaseContractRepository.existsByRoom_IdAndStatusInAndDeletedAtIsNull(room.getId(), BLOCKING_ACTIVE_CONTRACT_STATUSES)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phong da co hop dong dang hieu luc.");
        }
        if (leaseContractRepository.existsByRoom_IdAndStatusInAndDeletedAtIsNull(
                room.getId(),
                List.of(LeaseStatus.DRAFT, LeaseStatus.PENDING_SIGNATURE)
        )) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "FUTURE_CONTRACT_EXISTS: Phong da co hop dong tuong lai.");
        }
        Integer maxOccupants = room.getMaxOccupants() != null ? room.getMaxOccupants() : 3;
        if (occupantsCount > maxOccupants) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "So nguoi o vuot qua so nguoi toi da cua phong.");
        }
    }

    private void validateSoonVacantMoveInDate(Long roomId, LocalDate expectedMoveInDate) {
        LocalDate expectedVacantDate = roomCommitmentChecker.findExpectedVacantDateForBooking(roomId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "EXPECTED_VACANT_DATE_MISSING: Phong sap trong chua co ngay du kien ban giao."
                ));
        if (expectedMoveInDate.isBefore(expectedVacantDate)) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "EXPECTED_MOVE_IN_BEFORE_VACANT_DATE: Ngay du kien vao o phai sau hoac bang ngay phong du kien trong."
            );
        }
    }

    private void validateContractTerms(
            LocalDate startDate,
            LocalDate endDate,
            Integer paymentCycleMonths,
            Long monthlyRent,
            Long depositAmount
    ) {
        if (startDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ngay bat dau hop dong la bat buoc.");
        }
        if (endDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ngay ket thuc hop dong la bat buoc.");
        }
        if (!endDate.isAfter(startDate)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ngay ket thuc phai sau ngay bat dau hop dong."
            );
        }
        if (!Objects.equals(paymentCycleMonths, 1) && !Objects.equals(paymentCycleMonths, 3)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Chu ky thanh toan chi duoc la 1 hoac 3 thang."
            );
        }
        if (monthlyRent == null || monthlyRent <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Gia thue hang thang phai lon hon 0.");
        }
        if (depositAmount == null || depositAmount < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tien coc phai lon hon hoac bang 0.");
        }
    }

    private LocalDate resolveRentStartDate(LocalDate startDate) {
        if (startDate.getDayOfMonth() <= 10) {
            return startDate;
        }
        return startDate.plusMonths(1).withDayOfMonth(1);
    }

    private int countRequestedOccupants(DepositAgreementEntity deposit) {
        int coOccupantCount = deposit.getDepositForm() != null && deposit.getDepositForm().getCoOccupants() != null
                ? (int) deposit.getDepositForm().getCoOccupants().stream()
                .filter(item -> item.getPhone() == null || deposit.getDepositorPersonProfile() == null
                                || deposit.getDepositorPersonProfile().getPhone() == null
                                || !normalizePhone(item.getPhone()).equals(normalizePhone(deposit.getDepositorPersonProfile().getPhone())))
                .count()
                : 0;
        return 1 + coOccupantCount;
    }

    private void ensureContractOccupants(LeaseContractEntity contract, DepositAgreementEntity deposit) {
        if (contract == null || contract.getPrimaryTenantProfile() == null || contract.getRoom() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hop dong chua du thong tin nguoi o.");
        }
        LocalDate moveInDate = contract.getStartDate();
        Long propertyId = contract.getRoom().getProperty() != null ? contract.getRoom().getProperty().getId() : null;
        Long primaryProfileId = contract.getPrimaryTenantProfile().getId();
        insertContractOccupantIfAbsent(
                contract.getId(),
                resolveTenantIdForProfile(primaryProfileId, propertyId),
                primaryProfileId,
                "PRIMARY",
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
                    "CO_OCCUPANT",
                    moveInDate
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

    private void copyContractOccupants(LeaseContractEntity oldContract, LeaseContractEntity newContract) {
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
                "PRIMARY",
                newContract.getStartDate()
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
                        "Nguoi o cung chua co ho so hien huu. Vui long cap nhat ho so truoc khi tai ky."
                );
            }
            insertContractOccupantIfAbsent(
                    newContract.getId(),
                    resolveTenantIdForProfile(profileId, propertyId),
                    profileId,
                    "CO_OCCUPANT",
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
                        SELECT id
                        FROM person_profiles
                        WHERE REPLACE(REPLACE(REPLACE(phone, ' ', ''), '.', ''), '-', '') = ?
                          AND deleted_at IS NULL
                        ORDER BY user_id IS NULL, id DESC
                        LIMIT 1
                        """,
                rs -> rs.next() ? rs.getLong("id") : null,
                normalizedPhone
        );
    }

    private List<LeaseContractRenewalResponse.OccupantInfo> findRenewalOccupants(Long contractId) {
        return jdbcTemplate.query("""
                        SELECT
                            co.tenant_profile_id,
                            pp.full_name,
                            pp.phone,
                            co.occupant_role
                        FROM contract_occupants co
                        LEFT JOIN person_profiles pp ON pp.id = co.tenant_profile_id
                        WHERE co.contract_id = ?
                          AND co.status = 'ACTIVE'
                        ORDER BY CASE WHEN co.occupant_role = 'PRIMARY' THEN 0 ELSE 1 END, co.id
                        """,
                (rs, rowNum) -> new LeaseContractRenewalResponse.OccupantInfo(
                        getLongOrNull(rs, "tenant_profile_id"),
                        rs.getString("full_name"),
                        rs.getString("phone"),
                        OccupantRole.valueOf(rs.getString("occupant_role"))
                ),
                contractId
        );
    }

    private String resolveRenewalContractCode(LeaseContractEntity oldContract) {
        LeaseContractEntity rootContract = oldContract;
        int renewalNumber = 1;
        while (rootContract.getPreviousContract() != null) {
            rootContract = rootContract.getPreviousContract();
            renewalNumber++;
        }

        String contractCode = rootContract.getContractCode() + "-R" + renewalNumber;
        if (contractCode.length() > 80) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ma hop dong moi khong duoc vuot qua 80 ky tu.");
        }
        if (leaseContractRepository.existsByContractCodeAndDeletedAtIsNull(contractCode)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ma hop dong moi da ton tai.");
        }
        return contractCode;
    }

    private Long resolveTenantIdForProfile(Long profileId, Long propertyId) {
        if (profileId == null || propertyId == null) {
            return null;
        }
        return jdbcTemplate.query("""
                        SELECT t.id
                        FROM person_profiles pp
                        JOIN tenants t ON t.user_id = pp.user_id
                        WHERE pp.id = ?
                          AND pp.deleted_at IS NULL
                          AND t.property_id = ?
                          AND t.deleted_at IS NULL
                        ORDER BY t.id DESC
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
                        SELECT id
                        FROM person_profiles
                        WHERE REPLACE(REPLACE(REPLACE(phone, ' ', ''), '.', ''), '-', '') = ?
                          AND deleted_at IS NULL
                        ORDER BY user_id IS NULL, id DESC
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
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Khong tao duoc ho so nguoi o cung.");
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

    private boolean hasOtherActiveContract(Long roomId, Long leaseContractId) {
        return hasOtherActiveContract(roomId, leaseContractId, null);
    }

    private boolean hasOtherActiveContract(Long roomId, Long leaseContractId, Long allowedPreviousContractId) {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM lease_contracts
                        WHERE room_id = ?
                          AND id <> ?
                          AND (? IS NULL OR id <> ?)
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

    private String normalizeTenantIntention(String intention) {
        String normalized = intention == null ? "" : intention.trim().toUpperCase();
        return "TRANSFER_ROOM".equals(normalized) ? "TRANSFER" : normalized;
    }

    private boolean isWithinThreeMonths(LeaseContractEntity contract, LocalDate today) {
        return contract.getEndDate() != null && !today.isBefore(contract.getEndDate().minusMonths(3));
    }

    private void throwRenewBlocked(RoomCommitmentChecker.Blocker blocker) {
        if (blocker == RoomCommitmentChecker.Blocker.ROOM_HOLD_IN_PROGRESS) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "ROOM_HOLD_IN_PROGRESS: Phong dang duoc giu cho cho nguoi khac. Vui long thu lai sau."
            );
        }
        throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "ROOM_ALREADY_RESERVED_FOR_FUTURE: Phong da co khach khac dat coc/giu cho, khong the gia han. Vui long lien he quan ly."
        );
    }

    private void assertOwnerCanRenew() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isOwner = authentication != null
                && authentication.getAuthorities() != null
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_OWNER".equals(authority.getAuthority()));
        if (!isOwner) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "OWNER_APPROVAL_REQUIRED: Chi chu tro moi co quyen xac nhan tai ky hop dong. Vui long trinh yeu cau cho chu tro."
            );
        }
    }

    private void appendContractEvent(Long contractId, String eventType, String eventData) {
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

    private void appendRoomStatusHistory(Long roomId, RoomStatus fromStatus, RoomStatus toStatus, String reason) {
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

    private DepositAgreementEntity getReadyDeposit(Long depositAgreementId) {
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

    private LeaseContractEntity findLatestContractByDeposit(Long depositAgreementId) {
        return jdbcTemplate.query("""
                        SELECT id FROM lease_contracts
                        WHERE deposit_agreement_id = ? AND deleted_at IS NULL
                        ORDER BY id DESC LIMIT 1
                        """,
                rs -> rs.next()
                        ? leaseContractRepository.findById(rs.getLong("id")).orElse(null)
                        : null,
                depositAgreementId
        );
    }

    private Integer resolvePaymentCycleMonths(DepositAgreementEntity deposit) {
        if (deposit.getDepositForm() != null && deposit.getDepositForm().getPaymentCycleMonths() != null) {
            return deposit.getDepositForm().getPaymentCycleMonths();
        }
        return 1;
    }

    private LeaseContractManagementResponse toResponse(ResultSet rs) throws SQLException {
        Long leaseContractId = getLongOrNull(rs, "lease_contract_id");
        String contractStatus = rs.getString("contract_status");
        String depositStatus = rs.getString("deposit_status");
        Long contractFileId = getLongOrNull(rs, "contract_file_id");
        Long userId = getLongOrNull(rs, "user_id");
        String code = rs.getString("contract_code");
        Long roomId = getLongOrNull(rs, "room_id");
        Long renewedContractId = getLongOrNull(rs, "renewed_contract_id");
        LeaseStatus parsedContractStatus = parseEnum(LeaseStatus.class, contractStatus);
        RoomCommitmentChecker.Blocker renewBlocker =
                resolveRenewBlocker(roomId, leaseContractId, renewedContractId, parsedContractStatus);
        return LeaseContractManagementResponse.builder()
                .sourceType(rs.getString("source_type"))
                .leaseContractId(leaseContractId)
                .depositAgreementId(getLongOrNull(rs, "deposit_agreement_id"))
                .code(code)
                .depositCode(rs.getString("deposit_code"))
                .contractCode(rs.getString("contract_code"))
                .propertyId(getLongOrNull(rs, "property_id"))
                .propertyName(rs.getString("property_name"))
                .propertyAddress(rs.getString("property_address"))
                .tenantId(getLongOrNull(rs, "tenant_id"))
                .roomId(roomId)
                .roomCode(rs.getString("room_code"))
                .roomStatus(parseEnum(RoomStatus.class, rs.getString("room_status")))
                .primaryTenantProfileId(getLongOrNull(rs, "primary_tenant_profile_id"))
                .customerName(rs.getString("customer_name"))
                .phone(rs.getString("phone"))
                .email(rs.getString("email"))
                .expectedLeaseSignDate(toLocalDate(rs, "expected_lease_sign_date"))
                .expectedMoveInDate(toLocalDate(rs, "expected_move_in_date"))
                .startDate(toLocalDate(rs, "start_date"))
                .endDate(toLocalDate(rs, "end_date"))
                .rentStartDate(toLocalDate(rs, "rent_start_date"))
                .monthlyRent(getLongOrNull(rs, "monthly_rent"))
                .paymentCycleMonths(getIntOrNull(rs, "payment_cycle_months"))
                .depositAmount(getLongOrNull(rs, "deposit_amount"))
                .occupantsCount(getIntOrNull(rs, "occupants_count"))
                .previousContractId(getLongOrNull(rs, "previous_contract_id"))
                .previousContractCode(rs.getString("previous_contract_code"))
                .renewedContractId(renewedContractId)
                .renewedContractCode(rs.getString("renewed_contract_code"))
                .tenantIntention(rs.getString("tenant_intention"))
                .expectedVacantDate(toLocalDate(rs, "expected_vacant_date"))
                .canRenew(canRenewFromBlocker(leaseContractId, renewedContractId, parsedContractStatus, renewBlocker))
                .canRenewBlockedReason(renewBlocker == RoomCommitmentChecker.Blocker.NONE
                        ? null
                        : renewBlockedReason(renewBlocker))
                .contractStatus(parsedContractStatus)
                .depositStatus(parseEnum(DepositAgreementStatus.class, depositStatus))
                .workflowStatus(resolveWorkflow(contractStatus, contractFileId))
                .contractFileId(contractFileId)
                .contractFileName(rs.getString("contract_file_name"))
                .contractFileUploadedAt(toLocalDateTime(rs, "contract_file_uploaded_at"))
                .signedAt(toLocalDateTime(rs, "signed_at"))
                .createdAt(toLocalDateTime(rs, "created_at"))
                .accountProvisioned(userId != null)
                .emailAvailable(rs.getString("email") != null && !rs.getString("email").isBlank())
                .build();
    }

    private RoomCommitmentChecker.Blocker resolveRenewBlocker(
            Long roomId,
            Long leaseContractId,
            Long renewedContractId,
            LeaseStatus contractStatus
    ) {
        if (roomId == null
                || leaseContractId == null
                || renewedContractId != null
                || !isRenewableContractStatus(contractStatus)) {
            return RoomCommitmentChecker.Blocker.NONE;
        }
        return roomCommitmentChecker.checkRenewBlockers(roomId, leaseContractId);
    }

    private boolean canRenewFromBlocker(
            Long leaseContractId,
            Long renewedContractId,
            LeaseStatus contractStatus,
            RoomCommitmentChecker.Blocker renewBlocker
    ) {
        return leaseContractId != null
                && renewedContractId == null
                && isRenewableContractStatus(contractStatus)
                && renewBlocker == RoomCommitmentChecker.Blocker.NONE;
    }

    private boolean isRenewableContractStatus(LeaseStatus contractStatus) {
        return contractStatus == LeaseStatus.ACTIVE
                || contractStatus == LeaseStatus.EXPIRING_SOON
                || contractStatus == LeaseStatus.EXPIRED;
    }

    private String renewBlockedReason(RoomCommitmentChecker.Blocker blocker) {
        if (blocker == RoomCommitmentChecker.Blocker.ROOM_HOLD_IN_PROGRESS) {
            return "Phong dang duoc giu cho cho khach khac.";
        }
        return "Phong da co khach khac dat coc/giu cho, khong the tai ky.";
    }

    private String resolveWorkflow(String contractStatus, Long contractFileId) {
        if (contractStatus != null && List.of(
                "ACTIVE",
                "EXPIRING_SOON",
                "EXPIRED",
                "TERMINATION_PENDING",
                "LIQUIDATED",
                "RENEWED",
                "AUTO_TERMINATED",
                "CANCELLED"
        ).contains(contractStatus)) {
            return contractStatus;
        }
        if (contractStatus == null || contractFileId == null) {
            return "WAITING_SIGN";
        }
        return "WAITING_ACTIVATE";
    }

    private <T extends Enum<T>> T parseEnum(Class<T> enumClass, String value) {
        return value == null ? null : Enum.valueOf(enumClass, value);
    }

    private LocalDate toLocalDate(ResultSet rs, String column) throws SQLException {
        var date = rs.getDate(column);
        return date != null ? date.toLocalDate() : null;
    }

    private LocalDateTime toLocalDateTime(ResultSet rs, String column) throws SQLException {
        var timestamp = rs.getTimestamp(column);
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }

    private Long getLongOrNull(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private Integer getIntOrNull(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }
}
