package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.property.application.service.RoomCommitmentChecker;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;

import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceLineType;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceReason;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceStatus;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceType;
import com.sep490.hdbhms.billingandpayment.application.service.IssuedInvoiceChargeService;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.InvoiceEntity;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.InvoiceLineEntity;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaInvoiceLineRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaInvoiceRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.response.BillingInvoiceLineResponse;
import com.sep490.hdbhms.accounting.application.service.ExpenseRequestService;
import com.sep490.hdbhms.file.application.port.in.command.UploadFileCommand;
import com.sep490.hdbhms.file.application.service.UploadFileService;
import com.sep490.hdbhms.file.domain.value_objects.FileCategory;
import com.sep490.hdbhms.file.infrastructure.persistence.jpa.JpaFileMetadataRepository;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.UserEntity;
import com.sep490.hdbhms.occupancy.domain.value_objects.LeaseStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.HandoverStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.HandoverType;
import com.sep490.hdbhms.occupancy.domain.value_objects.LiquidationStatus;
import com.sep490.hdbhms.property.domain.value_objects.MeterStatus;
import com.sep490.hdbhms.property.domain.value_objects.MeterType;
import com.sep490.hdbhms.occupancy.domain.value_objects.OccupantRole;
import com.sep490.hdbhms.occupancy.domain.value_objects.OccupantStatus;
import com.sep490.hdbhms.property.domain.value_objects.ReadingPurpose;
import com.sep490.hdbhms.property.domain.value_objects.ReadingStatus;
import com.sep490.hdbhms.property.domain.value_objects.RoomStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.ContractLiquidationEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.ContractHandoverRecordEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.ContractOccupantEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeaseContractEntity;
import com.sep490.hdbhms.property.infrastructure.persistence.entity.MeterEntity;
import com.sep490.hdbhms.property.infrastructure.persistence.entity.MeterReadingEntity;
import com.sep490.hdbhms.property.infrastructure.persistence.entity.RoomEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaContractLiquidationRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaContractHandoverRecordRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaContractOccupantRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaLeaseContractRepository;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaMeterReadingRepository;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaMeterRepository;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaRoomRepository;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.LeaseContractManagementResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.LeaseContractRenewalResponse;
import com.sep490.hdbhms.shared.types.dto.response.PageResponse;
import com.sep490.hdbhms.shared.utils.AuthUtils;
import com.sep490.hdbhms.shared.utils.DocumentFilenameBuilder;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LeaseContractManagementService {
    static final String LIQUIDATION_MODE_PRIMARY_LEAVES_CO_OCCUPANT_STAYS = "PRIMARY_LEAVES_CO_OCCUPANT_STAYS";
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
    JpaContractOccupantRepository contractOccupantRepository;
    JpaContractLiquidationRepository contractLiquidationRepository;
    JpaContractHandoverRecordRepository handoverRecordRepository;
    JpaInvoiceRepository invoiceRepository;
    JpaInvoiceLineRepository invoiceLineRepository;
    IssuedInvoiceChargeService issuedInvoiceChargeService;
    JpaMeterRepository meterRepository;
    JpaMeterReadingRepository meterReadingRepository;
    RoomCommitmentChecker roomCommitmentChecker;
    LeaseExpiryReminderService leaseExpiryReminderService;
    ExpenseRequestService expenseRequestService;

    @Transactional(readOnly = true)
    public List<LeaseContractManagementResponse> findAllForManagement() {
        return jdbcTemplate.query("""
                SELECT *
                FROM (
                SELECT
                    'CONTRACT' AS source_type,
                    lc.lease_contract_id AS lease_contract_id,
                    lc.deposit_form_id AS deposit_form_id,
                    lc.contract_code,
                    df.deposit_code,
                    p.property_id AS property_id,
                    p.name AS property_name,
                    p.address_line AS property_address,
                    (
                        SELECT co.tenant_id
                        FROM contract_occupants co
                        WHERE co.contract_id = lc.lease_contract_id
                          AND co.status = 'ACTIVE'
                        ORDER BY CASE WHEN co.occupant_role = 'PRIMARY' THEN 0 ELSE 1 END, co.contract_occupant_id ASC
                        LIMIT 1
                    ) AS tenant_id,
                    r.room_id AS room_id,
                    r.room_code,
                    r.current_status AS room_status,
                    pp.person_profile_id AS primary_tenant_profile_id,
                    pp.full_name AS customer_name,
                    pp.phone,
                    pp.email,
                    NULL AS expected_lease_sign_date,
                    lc.rent_start_date AS expected_move_in_date,
                    lc.start_date,
                    lc.end_date,
                    lc.rent_start_date,
                    lc.activation_electricity_value,
                    lc.activation_reading_date,
                    lc.monthly_rent,
                    lc.payment_cycle_months,
                    lc.deposit_amount,
                    df.contract_term_months,
                    lc.previous_contract_id,
                    previous_contract.contract_code AS previous_contract_code,
                    lc.tenant_intention,
                    lc.expected_vacant_date,
                    cl.contract_liquidation_id AS liquidation_id,
                    cl.liquidation_date,
                    cl.reason AS liquidation_reason,
                    cl.deposit_amount AS liquidation_deposit_amount,
                    cl.deposit_deduction_amount AS liquidation_deposit_deduction_amount,
                    cl.deposit_deduction_reason AS liquidation_deposit_deduction_reason,
                    cl.deposit_refund_amount AS liquidation_deposit_refund_amount,
                    cl.final_invoice_id AS liquidation_final_invoice_id,
                    cl.signed_file_id AS liquidation_signed_file_id,
                    cl.status AS liquidation_status,
                    cl.created_at AS liquidation_created_at,
                    tr.room_transfer_request_id AS transfer_request_id,
                    tr.request_code AS transfer_request_code,
                    tr.status AS transfer_status,
                    tr.requested_transfer_date AS transfer_requested_date,
                    CASE
                        WHEN EXISTS (
                            SELECT 1
                            FROM room_transfer_requests source_transfer
                            WHERE source_transfer.old_contract_id = lc.lease_contract_id
                              AND source_transfer.status IN ('EXECUTED', 'COMPLETED')
                        ) THEN TRUE
                        ELSE FALSE
                    END AS source_transfer_completed,
                    CASE
                        WHEN tr.new_contract_id = lc.lease_contract_id THEN 'NEW_CONTRACT'
                        WHEN tr.replacement_old_contract_id = lc.lease_contract_id THEN 'REPLACEMENT_OLD_CONTRACT'
                        ELSE NULL
                    END AS transfer_contract_role,
                    (
                        SELECT renewed.lease_contract_id
                        FROM lease_contracts renewed
                        WHERE renewed.previous_contract_id = lc.lease_contract_id
                          AND renewed.deleted_at IS NULL
                        ORDER BY renewed.lease_contract_id DESC
                        LIMIT 1
                    ) AS renewed_contract_id,
                    (
                        SELECT renewed.contract_code
                        FROM lease_contracts renewed
                        WHERE renewed.previous_contract_id = lc.lease_contract_id
                          AND renewed.deleted_at IS NULL
                        ORDER BY renewed.lease_contract_id DESC
                        LIMIT 1
                    ) AS renewed_contract_code,
                    (
                        SELECT COUNT(*)
                        FROM contract_occupants co
                        WHERE co.contract_id = lc.lease_contract_id
                          AND co.status = 'ACTIVE'
                    ) AS occupants_count,
                    lc.status AS contract_status,
                    df.deposit_status,
                    lc.contract_file_id,
                    fm.original_name AS contract_file_name,
                    fm.created_at AS contract_file_uploaded_at,
                    lc.signed_file_id,
                    sfm.original_name AS signed_file_name,
                    sfm.created_at AS signed_file_uploaded_at,
                    lc.signed_uploaded_by,
                    lc.signed_at,
                    (
                        SELECT handover.signed_document_id
                        FROM contract_handover_records handover
                        WHERE handover.contract_id = lc.lease_contract_id
                          AND handover.handover_type = 'MOVE_IN'
                        ORDER BY handover.contract_handover_record_id DESC
                        LIMIT 1
                     ) AS handover_signed_file_id,
                     (
                         SELECT handover.contract_handover_record_id
                         FROM contract_handover_records handover
                         WHERE handover.contract_id = lc.lease_contract_id
                           AND handover.handover_type = 'MOVE_OUT'
                         ORDER BY handover.contract_handover_record_id DESC
                         LIMIT 1
                     ) AS move_out_handover_record_id,
                     (
                         SELECT handover.status
                         FROM contract_handover_records handover
                         WHERE handover.contract_id = lc.lease_contract_id
                           AND handover.handover_type = 'MOVE_OUT'
                         ORDER BY handover.contract_handover_record_id DESC
                         LIMIT 1
                     ) AS move_out_handover_status,
                     (
                         SELECT handover.handover_date
                         FROM contract_handover_records handover
                         WHERE handover.contract_id = lc.lease_contract_id
                           AND handover.handover_type = 'MOVE_OUT'
                         ORDER BY handover.contract_handover_record_id DESC
                         LIMIT 1
                     ) AS move_out_handover_date,
                     (
                         SELECT handover.electricity_reading_id
                         FROM contract_handover_records handover
                         WHERE handover.contract_id = lc.lease_contract_id
                           AND handover.handover_type = 'MOVE_OUT'
                         ORDER BY handover.contract_handover_record_id DESC
                         LIMIT 1
                     ) AS move_out_handover_electricity_reading_id,
                     (
                         SELECT handover.signed_document_id
                         FROM contract_handover_records handover
                         WHERE handover.contract_id = lc.lease_contract_id
                           AND handover.handover_type = 'MOVE_OUT'
                         ORDER BY handover.contract_handover_record_id DESC
                         LIMIT 1
                     ) AS move_out_handover_signed_file_id,
                     lc.created_at,
                    u.user_id AS user_id,
                    u.last_login_at
                FROM lease_contracts lc
                JOIN rooms r ON r.room_id = lc.room_id
                JOIN properties p ON p.property_id = r.property_id
                JOIN person_profiles pp ON pp.person_profile_id = lc.primary_tenant_profile_id
                LEFT JOIN lease_contracts previous_contract ON previous_contract.lease_contract_id = lc.previous_contract_id
                LEFT JOIN deposit_forms df ON df.deposit_form_id = lc.deposit_form_id
                LEFT JOIN contract_liquidations cl ON cl.contract_id = lc.lease_contract_id
                LEFT JOIN file_metadata fm ON fm.file_metadata_id = lc.contract_file_id
                LEFT JOIN room_transfer_requests tr
                  ON tr.new_contract_id = lc.lease_contract_id OR tr.replacement_old_contract_id = lc.lease_contract_id
                LEFT JOIN file_metadata sfm ON sfm.file_metadata_id = lc.signed_file_id
                LEFT JOIN users u ON u.user_id = pp.user_id AND u.deleted_at IS NULL
                WHERE lc.deleted_at IS NULL
                UNION ALL
                SELECT
                    'DEPOSIT' AS source_type,
                    NULL AS lease_contract_id,
                    df.deposit_form_id AS deposit_form_id,
                    NULL AS contract_code,
                    df.deposit_code,
                    p.property_id AS property_id,
                    p.name AS property_name,
                    p.address_line AS property_address,
                    df.tenant_id AS tenant_id,
                    r.room_id AS room_id,
                    r.room_code,
                    r.current_status AS room_status,
                    df.depositor_person_profile_id AS primary_tenant_profile_id,
                    df.full_name AS customer_name,
                    df.phone,
                    df.email,
                    df.expected_lease_sign_date,
                    df.expected_move_in_date,
                    NULL AS start_date,
                    NULL AS end_date,
                    df.expected_move_in_date AS rent_start_date,
                    NULL AS activation_electricity_value,
                    NULL AS activation_reading_date,
                    r.listed_price AS monthly_rent,
                    df.payment_cycle_months,
                    df.amount AS deposit_amount,
                    df.contract_term_months,
                    NULL AS previous_contract_id,
                    NULL AS previous_contract_code,
                    NULL AS tenant_intention,
                    NULL AS expected_vacant_date,
                    NULL AS liquidation_id,
                    NULL AS liquidation_date,
                    NULL AS liquidation_reason,
                    NULL AS liquidation_deposit_amount,
                    NULL AS liquidation_deposit_deduction_amount,
                    NULL AS liquidation_deposit_deduction_reason,
                    NULL AS liquidation_deposit_refund_amount,
                    NULL AS liquidation_final_invoice_id,
                    NULL AS liquidation_signed_file_id,
                    NULL AS liquidation_status,
                    NULL AS liquidation_created_at,
                    NULL AS transfer_request_id,
                    NULL AS transfer_request_code,
                    NULL AS transfer_status,
                    NULL AS transfer_requested_date,
                    FALSE AS source_transfer_completed,
                    NULL AS transfer_contract_role,
                    NULL AS renewed_contract_id,
                    NULL AS renewed_contract_code,
                    df.occupant_count AS occupants_count,
                    NULL AS contract_status,
                    df.deposit_status,
                    NULL AS contract_file_id,
                    NULL AS contract_file_name,
                    NULL AS contract_file_uploaded_at,
                    NULL AS signed_file_id,
                    NULL AS signed_file_name,
                    NULL AS signed_file_uploaded_at,
                    NULL AS signed_uploaded_by,
                    NULL AS signed_at,
                    NULL AS handover_signed_file_id,
                    NULL AS move_out_handover_record_id,
                    NULL AS move_out_handover_status,
                    NULL AS move_out_handover_date,
                    NULL AS move_out_handover_electricity_reading_id,
                    NULL AS move_out_handover_signed_file_id,
                    df.created_at,
                    u.user_id AS user_id,
                    u.last_login_at
                FROM deposit_forms df
                JOIN rooms r ON r.room_id = df.room_id
                JOIN properties p ON p.property_id = r.property_id
                LEFT JOIN person_profiles pp ON pp.person_profile_id = df.depositor_person_profile_id
                LEFT JOIN users u ON u.user_id = pp.user_id AND u.deleted_at IS NULL
                WHERE df.deposit_status IN ('PAID', 'CONFIRMED', 'CONVERTED_TO_LEASE')
                  AND NOT EXISTS (
                      SELECT 1
                      FROM lease_contracts existing_contract
                      WHERE existing_contract.deposit_form_id = df.deposit_form_id
                        AND existing_contract.deleted_at IS NULL
                  )
                ) management_rows
                ORDER BY created_at DESC, lease_contract_id DESC, deposit_form_id DESC
                """, (rs, rowNum) -> toResponse(rs));
    }

    @Transactional(readOnly = true)
    public PageResponse<LeaseContractManagementResponse> findAllForManagement(Pageable pageable) {
        List<LeaseContractManagementResponse> rows = findAllForManagement();
        List<LeaseContractManagementResponse> pageRows = rows.stream()
                .skip(pageable.getOffset())
                .limit(pageable.getPageSize())
                .toList();
        return PageResponse.fromPageToPageResponse(new PageImpl<>(pageRows, pageable, rows.size()));
    }

    public LeaseContractManagementResponse uploadSignedFile(Long leaseContractId, MultipartFile file) {
        return uploadSignedFile(leaseContractId, file, false);
    }

    public LeaseContractManagementResponse uploadSignedFile(Long leaseContractId, MultipartFile file, boolean replace) {
        LeaseContractEntity contract = leaseContractRepository.findById(leaseContractId)
                .orElseThrow(() -> new AppException(ApiErrorCode.LEASE_CONTRACT_NOT_FOUND));
        if (contract.getStatus() == LeaseStatus.ACTIVE) {
            throw new AppException(ApiErrorCode.LEASE_ACTIVE_SIGNED_FILE_REPLACEMENT_FORBIDDEN);
        }
        if (contract.getSignedFile() != null && !replace) {
            throw new AppException(ApiErrorCode.LEASE_SIGNED_FILE_ALREADY_EXISTS);
        }
        Long currentUserId = AuthUtils.getCurrentAuthenticationId();
        var metadata = uploadFileService.execute(new UploadFileCommand(
                currentUserId,
                file,
                FileCategory.CONTRACT,
                true
        ));
        var signedFile = fileMetadataRepository.findById(metadata.getId())
                .orElseThrow(() -> new AppException(ApiErrorCode.LEASE_SIGNED_FILE_SAVE_FAILED));
        signedFile.setCategory(FileCategory.CONTRACT);
        signedFile.setSensitive(true);
        contract.setSignedFile(fileMetadataRepository.save(signedFile));
        contract.setSignedUploadedBy(currentUserId != null ? UserEntity.builder().id(currentUserId).build() : null);
        leaseContractRepository.save(contract);
        return findOne(contract.getId());
    }

    public LeaseContractManagementResponse liquidate(
            Long leaseContractId,
            LocalDate liquidationDate,
            String reason,
            List<LiquidationChargeInput> charges
    ) {
        LeaseContractEntity contract = leaseContractRepository.findById(leaseContractId)
                .orElseThrow(() -> new AppException(ApiErrorCode.LEASE_CONTRACT_NOT_FOUND_FOR_LIQUIDATION));
        if (contract.getStatus() == LeaseStatus.LIQUIDATED) {
            throw new AppException(ApiErrorCode.LEASE_ALREADY_LIQUIDATED);
        }
        if (contract.getStatus() != LeaseStatus.ACTIVE
                && contract.getStatus() != LeaseStatus.EXPIRING_SOON
                && contract.getStatus() != LeaseStatus.EXPIRED
                && contract.getStatus() != LeaseStatus.TERMINATION_PENDING) {
            throw new AppException(ApiErrorCode.LEASE_LIQUIDATION_STATUS_INVALID);
        }
        RoomEntity room = contract.getRoom();
        if (room == null) {
            throw new AppException(ApiErrorCode.LEASE_ROOM_REQUIRED);
        }
        ensureLiquidationAllowedForRoomCommitment(contract);
        LeaseContractDebtPolicy.requireNoOutstandingDebt(jdbcTemplate, contract.getId());

        LocalDate finalLiquidationDate = liquidationDate != null ? liquidationDate : LocalDate.now();
        String finalReason = reason == null || reason.isBlank()
                ? "Khách không tiếp tục thuê phòng."
                : reason.trim();
        Long depositAmount = resolveLiquidationDepositAmount(contract);
        ContractLiquidationEntity liquidation = contractLiquidationRepository.findByContract_Id(contract.getId())
                .orElseThrow(() -> new AppException(ApiErrorCode.LEASE_LIQUIDATION_RECORD_REQUIRED));
        finalLiquidationDate = liquidationDate != null
                ? liquidationDate
                : liquidation.getLiquidationDate() != null ? liquidation.getLiquidationDate() : finalLiquidationDate;
        finalReason = reason == null || reason.isBlank()
                ? liquidation.getReason() != null && !liquidation.getReason().isBlank()
                ? liquidation.getReason().trim()
                : finalReason
                : finalReason;
        boolean holderReplacement = isHolderReplacementLiquidation(contract.getId());
        applyLiquidationDraftValues(
                liquidation,
                contract,
                finalLiquidationDate,
                finalReason,
                depositAmount,
                holderReplacement
        );
        liquidation = contractLiquidationRepository.saveAndFlush(liquidation);

        LeaseContractEntity replacementContract = holderReplacement
                ? latestReplacementContract(contract.getId())
                .orElseThrow(() -> new AppException(ApiErrorCode.LEASE_REPLACEMENT_CONTRACT_REQUIRED_FOR_REMAINING_OCCUPANTS))
                : null;

        requireNoUnpaidInvoicesForLiquidation(contract.getId());
        if (!holderReplacement) {
            requireLiquidationDepositForfeitureConfirmed(contract, liquidation);
            requireLiquidationDepositRefundConfirmed(contract, liquidation);
            requireConfirmedMoveOutHandover(contract.getId());
        }

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

        if (holderReplacement) {
            activateReplacementContract(replacementContract, finalLiquidationDate);
        }
        deactivateTenantAccountsWithoutValidContract(contract.getId());

        liquidation.setStatus(LiquidationStatus.CONFIRMED);
        contractLiquidationRepository.save(liquidation);

        if (!holderReplacement) {
            RoomStatus fromStatus = room.getCurrentStatus();
            room.setCurrentStatus(RoomStatus.VACANT);
            roomRepository.saveAndFlush(room);
            appendRoomStatusHistory(room.getId(), fromStatus, RoomStatus.VACANT, "Thanh lý hợp đồng thuê " + contract.getContractCode());
        }
        appendContractEvent(contract.getId(), "LIQUIDATED", finalReason);
        expenseRequestService.completeLiquidationRequest(contract.getId());

        return findOne(contract.getId());
    }

    int deactivateTenantAccountsWithoutValidContract(Long contractId) {
        return jdbcTemplate.update("""
                UPDATE users u
                SET u.status = 'INACTIVE',
                    u.updated_at = CURRENT_TIMESTAMP(6)
                WHERE u.role = 'TENANT'
                  AND u.status = 'ACTIVE'
                  AND u.deleted_at IS NULL
                  AND u.user_id IN (
                    SELECT user_id
                    FROM (
                        SELECT pp.user_id
                        FROM contract_occupants co
                        JOIN person_profiles pp ON pp.person_profile_id = co.tenant_profile_id
                        WHERE co.contract_id = ?
                          AND pp.user_id IS NOT NULL
                          AND pp.deleted_at IS NULL
                        UNION
                        SELECT t.user_id
                        FROM contract_occupants co
                        JOIN tenants t ON t.tenant_id = co.tenant_id
                        WHERE co.contract_id = ?
                          AND t.deleted_at IS NULL
                        UNION
                        SELECT pp.user_id
                        FROM lease_contracts lc
                        JOIN person_profiles pp ON pp.person_profile_id = lc.primary_tenant_profile_id
                        WHERE lc.lease_contract_id = ?
                          AND pp.user_id IS NOT NULL
                          AND pp.deleted_at IS NULL
                    ) liquidated_users
                  )
                  AND NOT EXISTS (
                    SELECT 1
                    FROM contract_occupants active_co
                    JOIN lease_contracts active_lc ON active_lc.lease_contract_id = active_co.contract_id
                    LEFT JOIN person_profiles active_pp
                      ON active_pp.person_profile_id = active_co.tenant_profile_id
                     AND active_pp.deleted_at IS NULL
                    LEFT JOIN tenants active_t
                      ON active_t.tenant_id = active_co.tenant_id
                     AND active_t.deleted_at IS NULL
                    WHERE active_co.status = 'ACTIVE'
                      AND active_lc.deleted_at IS NULL
                      AND active_lc.status IN ('ACTIVE', 'EXPIRING_SOON', 'TERMINATION_PENDING')
                      AND (active_pp.user_id = u.user_id OR active_t.user_id = u.user_id)
                  )
                """,
                contractId,
                contractId,
                contractId
        );
    }

    public LeaseContractManagementResponse startLiquidationProcessing(
            Long leaseContractId,
            LocalDate liquidationDate,
            String reason
    ) {
        return startLiquidationProcessing(leaseContractId, liquidationDate, reason, null, List.of(), List.of(), null);
    }

    public LeaseContractManagementResponse startLiquidationProcessing(
            Long leaseContractId,
            LocalDate liquidationDate,
            String reason,
            String liquidationMode,
            List<Long> leavingProfileIds,
            List<Long> stayingProfileIds,
            Long replacementPrimaryTenantProfileId
    ) {
        lockContractAndRoom(leaseContractId);
        LeaseContractEntity contract = leaseContractRepository.findById(leaseContractId)
                .orElseThrow(() -> new AppException(ApiErrorCode.LEASE_CONTRACT_NOT_FOUND_FOR_LIQUIDATION));
        if (contract.getDeletedAt() != null) {
            throw new AppException(ApiErrorCode.LEASE_CONTRACT_NOT_FOUND_FOR_LIQUIDATION);
        }
        if (contract.getStatus() == LeaseStatus.LIQUIDATED) {
            throw new AppException(ApiErrorCode.LEASE_LIQUIDATION_ALREADY_COMPLETED);
        }
        if (contract.getStatus() != LeaseStatus.ACTIVE
                && contract.getStatus() != LeaseStatus.EXPIRING_SOON
                && contract.getStatus() != LeaseStatus.EXPIRED
                && contract.getStatus() != LeaseStatus.TERMINATION_PENDING) {
            throw new AppException(ApiErrorCode.LEASE_LIQUIDATION_PROCESSING_STATUS_INVALID);
        }
        RoomEntity room = contract.getRoom();
        if (room == null) {
            throw new AppException(ApiErrorCode.LEASE_ROOM_REQUIRED);
        }
        ensureLiquidationAllowedForRoomCommitment(contract);
        LeaseContractDebtPolicy.requireNoOutstandingDebt(jdbcTemplate, contract.getId());

        LocalDate finalLiquidationDate = liquidationDate != null ? liquidationDate : LocalDate.now();
        String finalReason = reason == null || reason.isBlank()
                ? "Khách không tiếp tục thuê phòng."
                : reason.trim();
        Long depositAmount = resolveLiquidationDepositAmount(contract);

        boolean holderReplacement = isHolderReplacementMode(liquidationMode);
        HolderReplacementPlan holderReplacementPlan = holderReplacement
                ? validateHolderReplacementLiquidation(
                contract,
                leavingProfileIds,
                stayingProfileIds,
                replacementPrimaryTenantProfileId
        )
                : null;

        ContractLiquidationEntity liquidation = contractLiquidationRepository.findByContract_Id(contract.getId())
                .orElseGet(() -> ContractLiquidationEntity.builder()
                        .contract(contract)
                        .depositAmount(depositAmount)
                        .build());
        if (liquidation.getStatus() == LiquidationStatus.CONFIRMED) {
            throw new AppException(ApiErrorCode.LEASE_LIQUIDATION_ALREADY_CONFIRMED);
        }
        liquidation.setLiquidationDate(finalLiquidationDate);
        liquidation.setReason(finalReason);
        liquidation.setDepositAmount(depositAmount);
        LiquidationDepositSettlement depositSettlement = calculateLiquidationDepositSettlement(
                contract,
                finalLiquidationDate,
                depositAmount,
                holderReplacement
        );
        liquidation.setDepositDeductionAmount(depositSettlement.deductionAmount());
        liquidation.setDepositDeductionReason(depositSettlement.deductionReason());
        liquidation.setDepositRefundAmount(depositSettlement.refundAmount());
        liquidation.setStatus(LiquidationStatus.DRAFT);
        contractLiquidationRepository.save(liquidation);

        if (!holderReplacement) {
            ensureLiquidationDepositForfeitureRequest(contract, liquidation);
            // Create the refund workflow immediately after the liquidation request is approved.
            ensureLiquidationDepositRefundRequest(contract, liquidation);
        }

        contract.setStatus(LeaseStatus.TERMINATION_PENDING);
        contract.setTenantIntention("MOVE_OUT");
        contract.setExpectedVacantDate(holderReplacement ? null : finalLiquidationDate);
        contract.setIntentionRecordedAt(LocalDateTime.now());
        leaseContractRepository.saveAndFlush(contract);

        if (holderReplacement) {
            ensureHolderReplacementContract(contract, holderReplacementPlan, finalLiquidationDate);
            if (room.getCurrentStatus() != RoomStatus.OCCUPIED) {
                RoomStatus holderFromStatus = room.getCurrentStatus();
                room.setCurrentStatus(RoomStatus.OCCUPIED);
                roomRepository.saveAndFlush(room);
                appendRoomStatusHistory(
                        room.getId(),
                        holderFromStatus,
                        RoomStatus.OCCUPIED,
                        "Người ở cùng tiếp tục thuê sau thanh lý hợp đồng " + contract.getContractCode()
                );
            }
        } else {
        RoomStatus fromStatus = room.getCurrentStatus();
        if (fromStatus != RoomStatus.SOON_VACANT) {
            room.setCurrentStatus(RoomStatus.SOON_VACANT);
            roomRepository.saveAndFlush(room);
            appendRoomStatusHistory(
                    room.getId(),
                    fromStatus,
                    RoomStatus.SOON_VACANT,
                    "Bắt đầu quy trình thanh lý hợp đồng " + contract.getContractCode()
            );
        }
        leaseExpiryReminderService.onTenantIntentionRecorded(contract.getId(), LocalDate.now());
        }
        appendContractEvent(contract.getId(), "LIQUIDATION_PROCESSING_STARTED", finalReason);
        return findOne(contract.getId());
    }

    HolderReplacementPlan validateHolderReplacementLiquidation(
            LeaseContractEntity contract,
            List<Long> leavingProfileIds,
            List<Long> stayingProfileIds,
            Long replacementPrimaryTenantProfileId
    ) {
        if (contract.getPrimaryTenantProfile() == null || contract.getPrimaryTenantProfile().getId() == null) {
            throw new AppException(ApiErrorCode.LEASE_PRIMARY_HOLDER_REQUIRED);
        }
        if (replacementPrimaryTenantProfileId == null) {
            throw new AppException(ApiErrorCode.LEASE_REPLACEMENT_HOLDER_REQUIRED);
        }

        List<ContractOccupantEntity> activeOccupants = contractOccupantRepository
                .findAllByContract_IdAndStatus(contract.getId(), OccupantStatus.ACTIVE);
        Set<Long> activeProfileIds = new LinkedHashSet<>();
        for (ContractOccupantEntity occupant : activeOccupants) {
            Long profileId = profileId(occupant);
            if (profileId != null) {
                activeProfileIds.add(profileId);
            }
        }
        if (activeProfileIds.isEmpty()) {
            throw new AppException(ApiErrorCode.LEASE_ACTIVE_OCCUPANTS_REQUIRED);
        }

        HolderReplacementProfilePlan profilePlan = validateHolderReplacementProfileIds(
                contract.getPrimaryTenantProfile().getId(),
                activeProfileIds,
                leavingProfileIds,
                stayingProfileIds,
                replacementPrimaryTenantProfileId
        );
        return new HolderReplacementPlan(
                profilePlan.leavingProfileIds(),
                profilePlan.stayingProfileIds(),
                profilePlan.replacementPrimaryTenantProfileId(),
                activeOccupants
        );
    }

    private LeaseContractEntity ensureHolderReplacementContract(
            LeaseContractEntity oldContract,
            HolderReplacementPlan plan,
            LocalDate effectiveDate
    ) {
        if (plan == null) {
            throw new AppException(ApiErrorCode.LEASE_HOLDER_REPLACEMENT_DATA_REQUIRED);
        }
        RoomEntity room = oldContract.getRoom();
        Optional<LeaseContractEntity> existingReplacement = latestReplacementContract(oldContract.getId());
        Long allowedExistingContractId = existingReplacement.map(LeaseContractEntity::getId).orElse(null);
        if (hasOtherActiveContract(room.getId(), oldContract.getId(), allowedExistingContractId)) {
            throw new AppException(ApiErrorCode.LEASE_ROOM_ACTIVE_CONTRACT_CONFLICT);
        }
        Long carriedDepositAmount = resolveLiquidationDepositAmount(oldContract);
        validateContractTerms(
                effectiveDate,
                oldContract.getPaymentCycleMonths(),
                oldContract.getMonthlyRent(),
                carriedDepositAmount
        );

        LeaseContractEntity replacement = existingReplacement.orElse(null);
        if (replacement != null) {
            assertHolderReplacementContractMatches(oldContract, replacement, plan);
            replacement.setStartDate(effectiveDate);
            replacement.setRentStartDate(effectiveDate);
            replacement.setEndDate(oldContract.getEndDate());
            replacement.setMonthlyRent(oldContract.getMonthlyRent());
            replacement.setPaymentCycleMonths(oldContract.getPaymentCycleMonths());
            replacement.setDepositAmount(carriedDepositAmount);
            if (replacement.getStatus() == LeaseStatus.DRAFT) {
                replacement.setStatus(LeaseStatus.PENDING_SIGNATURE);
            }
            replacement = leaseContractRepository.saveAndFlush(replacement);
            syncHolderReplacementOccupants(replacement, plan, effectiveDate);
            return replacement;
        }

        ContractOccupantEntity replacementPrimary = occupantByProfile(plan, plan.replacementPrimaryTenantProfileId());
        String contractCode = generateHolderReplacementContractCode(oldContract, effectiveDate);
        replacement = leaseContractRepository.saveAndFlush(LeaseContractEntity.builder()
                .contractCode(contractCode)
                .room(room)
                .primaryTenantProfile(replacementPrimary.getTenantProfile())
                .startDate(effectiveDate)
                .endDate(oldContract.getEndDate())
                .rentStartDate(effectiveDate)
                .monthlyRent(oldContract.getMonthlyRent())
                .paymentCycleMonths(oldContract.getPaymentCycleMonths())
                .depositAmount(carriedDepositAmount)
                .status(LeaseStatus.PENDING_SIGNATURE)
                .previousContract(oldContract)
                .build());
        syncHolderReplacementOccupants(replacement, plan, effectiveDate);
        appendContractEvent(
                replacement.getId(),
                "CREATED",
                "Thanh lý do thay thế người đại diện từ hợp đồng " + oldContract.getContractCode()
        );
        return replacement;
    }

    private void assertHolderReplacementContractMatches(
            LeaseContractEntity oldContract,
            LeaseContractEntity replacement,
            HolderReplacementPlan plan
    ) {
        if (replacement.getStatus() == LeaseStatus.CANCELLED || replacement.getStatus() == LeaseStatus.LIQUIDATED) {
            throw new AppException(ApiErrorCode.LEASE_REPLACEMENT_CONTRACT_UNUSABLE);
        }
        if (replacement.getRoom() == null || !Objects.equals(replacement.getRoom().getId(), oldContract.getRoom().getId())) {
            throw new AppException(ApiErrorCode.LEASE_REPLACEMENT_CONTRACT_ROOM_IMMUTABLE);
        }
        Long currentPrimaryId = replacement.getPrimaryTenantProfile() == null
                ? null
                : replacement.getPrimaryTenantProfile().getId();
        if (!Objects.equals(currentPrimaryId, plan.replacementPrimaryTenantProfileId())) {
            throw new AppException(ApiErrorCode.LEASE_REPLACEMENT_HOLDER_MISMATCH);
        }
    }

    private void syncHolderReplacementOccupants(
            LeaseContractEntity replacement,
            HolderReplacementPlan plan,
            LocalDate effectiveDate
    ) {
        for (Long stayingProfileId : plan.stayingProfileIds()) {
            ContractOccupantEntity oldOccupant = occupantByProfile(plan, stayingProfileId);
            OccupantRole role = Objects.equals(stayingProfileId, plan.replacementPrimaryTenantProfileId())
                    ? OccupantRole.PRIMARY
                    : OccupantRole.CO_OCCUPANT;
            upsertHolderReplacementOccupant(
                    replacement.getId(),
                    oldOccupant.getTenant() == null ? null : oldOccupant.getTenant().getId(),
                    stayingProfileId,
                    role,
                    effectiveDate
            );
        }

        List<ContractOccupantEntity> replacementOccupants = contractOccupantRepository
                .findAllByContract_IdAndStatus(replacement.getId(), OccupantStatus.ACTIVE);
        for (ContractOccupantEntity occupant : replacementOccupants) {
            Long profileId = profileId(occupant);
            if (profileId != null && !plan.stayingProfileIds().contains(profileId)) {
                occupant.setStatus(OccupantStatus.MOVED_OUT);
                occupant.setMoveOutDate(effectiveDate);
            }
        }
        contractOccupantRepository.saveAll(replacementOccupants);
    }

    private void upsertHolderReplacementOccupant(
            Long contractId,
            Long tenantId,
            Long tenantProfileId,
            OccupantRole role,
            LocalDate moveInDate
    ) {
        jdbcTemplate.update("""
                        INSERT INTO contract_occupants (
                            contract_id,
                            tenant_id,
                            tenant_profile_id,
                            occupant_role,
                            move_in_date,
                            status,
                            created_at
                        )
                        VALUES (?, ?, ?, ?, ?, 'ACTIVE', NOW(6))
                        ON DUPLICATE KEY UPDATE
                            tenant_id = VALUES(tenant_id),
                            occupant_role = VALUES(occupant_role),
                            move_in_date = VALUES(move_in_date),
                            move_out_date = NULL,
                            status = 'ACTIVE',
                            disabled_reason = NULL,
                            disabled_by = NULL,
                            disabled_at = NULL
                        """,
                contractId,
                tenantId,
                tenantProfileId,
                role.name(),
                moveInDate
        );
    }

    private void activateReplacementContract(LeaseContractEntity replacementContract, LocalDate effectiveDate) {
        if (replacementContract.getSignedFile() == null) {
            throw new AppException(ApiErrorCode.LEASE_REPLACEMENT_CONTRACT_MUST_BE_SIGNED);
        }
        if (!List.of(
                LeaseStatus.DRAFT,
                LeaseStatus.PENDING_SIGNATURE,
                LeaseStatus.CONFIRMED,
                LeaseStatus.SIGNED,
                LeaseStatus.ACTIVE
        ).contains(replacementContract.getStatus())) {
            throw new AppException(ApiErrorCode.LEASE_REPLACEMENT_CONTRACT_STATUS_INVALID);
        }
        if (replacementContract.getStatus() != LeaseStatus.ACTIVE) {
            replacementContract.setStartDate(effectiveDate);
            replacementContract.setRentStartDate(effectiveDate);
            replacementContract.setStatus(LeaseStatus.ACTIVE);
            if (replacementContract.getSignedAt() == null) {
                replacementContract.setSignedAt(LocalDateTime.now());
            }
            replacementContract = leaseContractRepository.saveAndFlush(replacementContract);
            appendContractEvent(replacementContract.getId(), "SIGNED", "Kích hoạt hợp đồng thay thế người đại diện.");
        }

        RoomEntity room = replacementContract.getRoom();
        if (room != null && room.getCurrentStatus() != RoomStatus.OCCUPIED) {
            RoomStatus fromStatus = room.getCurrentStatus();
            room.setCurrentStatus(RoomStatus.OCCUPIED);
            roomRepository.saveAndFlush(room);
            appendRoomStatusHistory(
                    room.getId(),
                    fromStatus,
                    RoomStatus.OCCUPIED,
                    "Kích hoạt hợp đồng thay thế " + replacementContract.getContractCode()
            );
        }
        updateTenantProvisioningLatestContract(replacementContract.getId());
    }

    private void updateTenantProvisioningLatestContract(Long contractId) {
        jdbcTemplate.update("""
                        UPDATE tenant_account_provisionings tap
                        JOIN contract_occupants co
                          ON co.tenant_profile_id = tap.tenant_profile_id
                         AND co.contract_id = ?
                         AND co.status = 'ACTIVE'
                        SET tap.latest_contract_id = ?,
                            tap.updated_at = NOW(6)
                        WHERE tap.status <> 'DISABLED'
                        """,
                contractId,
                contractId
        );
    }

    private boolean isHolderReplacementLiquidation(Long contractId) {
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
                LIQUIDATION_MODE_PRIMARY_LEAVES_CO_OCCUPANT_STAYS
        );
        if (count != null && count > 0) {
            return true;
        }
        return leaseContractRepository.findById(contractId)
                .flatMap(contract -> latestReplacementContract(contractId)
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

    private Optional<LeaseContractEntity> latestReplacementContract(Long oldContractId) {
        return leaseContractRepository
                .findFirstByPreviousContract_IdAndDeletedAtIsNullOrderByIdDesc(oldContractId)
                .filter(contract -> contract.getStatus() != LeaseStatus.CANCELLED
                        && contract.getStatus() != LeaseStatus.LIQUIDATED);
    }

    private String generateHolderReplacementContractCode(LeaseContractEntity oldContract, LocalDate effectiveDate) {
        String roomCode = oldContract.getRoom() == null ? null : oldContract.getRoom().getRoomCode();
        String baseCode = DocumentFilenameBuilder.buildLeaseContractCode(roomCode, effectiveDate);
        if (!leaseContractRepository.existsByContractCodeAndDeletedAtIsNull(baseCode)) {
            return baseCode;
        }

        int revision = 1;
        String contractCode;
        do {
            String suffix = "-LIQ-" + oldContract.getId() + (revision == 1 ? "" : "-" + revision);
            contractCode = baseCode + suffix;
            revision++;
        } while (leaseContractRepository.existsByContractCodeAndDeletedAtIsNull(contractCode));
        return contractCode;
    }

    static boolean isHolderReplacementMode(String liquidationMode) {
        return liquidationMode != null
                && LIQUIDATION_MODE_PRIMARY_LEAVES_CO_OCCUPANT_STAYS.equalsIgnoreCase(liquidationMode.trim());
    }

    static HolderReplacementProfilePlan validateHolderReplacementProfileIds(
            Long currentPrimaryTenantProfileId,
            Set<Long> activeProfileIds,
            List<Long> leavingProfileIds,
            List<Long> stayingProfileIds,
            Long replacementPrimaryTenantProfileId
    ) {
        Set<Long> leavingIds = normalizeProfileIds(leavingProfileIds);
        Set<Long> stayingIds = normalizeProfileIds(stayingProfileIds);
        if (leavingIds.isEmpty() || stayingIds.isEmpty()) {
            throw new AppException(ApiErrorCode.HOLDER_REPLACEMENT_OCCUPANTS_REQUIRED);
        }
        if (!leavingIds.contains(currentPrimaryTenantProfileId)) {
            throw new AppException(ApiErrorCode.CURRENT_PRIMARY_TENANT_MUST_LEAVE);
        }
        if (!stayingIds.contains(replacementPrimaryTenantProfileId)) {
            throw new AppException(ApiErrorCode.REPLACEMENT_PRIMARY_TENANT_MUST_STAY);
        }

        Set<Long> overlap = new HashSet<>(leavingIds);
        overlap.retainAll(stayingIds);
        if (!overlap.isEmpty()) {
            throw new AppException(ApiErrorCode.HOLDER_REPLACEMENT_OCCUPANTS_OVERLAP);
        }

        Set<Long> classifiedIds = new LinkedHashSet<>(leavingIds);
        classifiedIds.addAll(stayingIds);
        if (activeProfileIds == null || !activeProfileIds.equals(classifiedIds)) {
            throw new AppException(ApiErrorCode.HOLDER_REPLACEMENT_OCCUPANTS_UNCLASSIFIED);
        }
        if (!activeProfileIds.contains(replacementPrimaryTenantProfileId)) {
            throw new AppException(ApiErrorCode.REPLACEMENT_PRIMARY_TENANT_NOT_ACTIVE);
        }

        return new HolderReplacementProfilePlan(leavingIds, stayingIds, replacementPrimaryTenantProfileId);
    }

    static Set<Long> normalizeProfileIds(List<Long> profileIds) {
        Set<Long> normalized = new LinkedHashSet<>();
        if (profileIds == null) {
            return normalized;
        }
        for (Long profileId : profileIds) {
            if (profileId != null) {
                normalized.add(profileId);
            }
        }
        return normalized;
    }

    private ContractOccupantEntity occupantByProfile(HolderReplacementPlan plan, Long profileId) {
        return plan.activeOccupants().stream()
                .filter(occupant -> Objects.equals(profileId(occupant), profileId))
                .findFirst()
                .orElseThrow(() -> new AppException(ApiErrorCode.LEASE_OCCUPANT_NOT_ACTIVE));
    }

    private Long profileId(ContractOccupantEntity occupant) {
        return occupant == null || occupant.getTenantProfile() == null
                ? null
                : occupant.getTenantProfile().getId();
    }

    record HolderReplacementPlan(
            Set<Long> leavingProfileIds,
            Set<Long> stayingProfileIds,
            Long replacementPrimaryTenantProfileId,
            List<ContractOccupantEntity> activeOccupants
    ) {
    }

    record HolderReplacementProfilePlan(
            Set<Long> leavingProfileIds,
            Set<Long> stayingProfileIds,
            Long replacementPrimaryTenantProfileId
    ) {
    }

    public LeaseContractManagementResponse updateLiquidationDraft(
            Long leaseContractId,
            LocalDate liquidationDate,
            String reason,
            List<LiquidationChargeInput> charges
    ) {
        LeaseContractEntity contract = leaseContractRepository.findById(leaseContractId)
                .orElseThrow(() -> new AppException(ApiErrorCode.LEASE_CONTRACT_NOT_FOUND_FOR_HANDOVER));
        if (contract.getDeletedAt() != null) {
            throw new AppException(ApiErrorCode.LEASE_CONTRACT_NOT_FOUND_FOR_HANDOVER);
        }
        if (contract.getStatus() == LeaseStatus.LIQUIDATED) {
            throw new AppException(ApiErrorCode.LEASE_ALREADY_LIQUIDATED_FOR_HANDOVER);
        }
        if (contract.getStatus() != LeaseStatus.TERMINATION_PENDING
                && contract.getStatus() != LeaseStatus.ACTIVE
                && contract.getStatus() != LeaseStatus.EXPIRING_SOON
                && contract.getStatus() != LeaseStatus.EXPIRED) {
            throw new AppException(ApiErrorCode.LEASE_LIQUIDATION_NOT_ALLOWED);
        }
        LeaseContractDebtPolicy.requireNoOutstandingDebt(jdbcTemplate, contract.getId());

        Long depositAmount = resolveLiquidationDepositAmount(contract);
        ContractLiquidationEntity liquidation = contractLiquidationRepository.findByContract_Id(contract.getId())
                .orElseGet(() -> ContractLiquidationEntity.builder()
                        .contract(contract)
                        .depositAmount(depositAmount)
                        .status(LiquidationStatus.DRAFT)
                        .build());
        if (liquidation.getStatus() == LiquidationStatus.CONFIRMED) {
            throw new AppException(ApiErrorCode.LEASE_LIQUIDATION_ALREADY_CONFIRMED_FOR_HANDOVER);
        }

        LocalDate finalLiquidationDate = liquidationDate != null
                ? liquidationDate
                : liquidation.getLiquidationDate() != null ? liquidation.getLiquidationDate() : LocalDate.now();
        String finalReason = reason == null || reason.isBlank()
                ? liquidation.getReason() != null && !liquidation.getReason().isBlank()
                ? liquidation.getReason().trim()
                : "Khách không tiếp tục thuê phòng."
                : reason.trim();
        boolean holderReplacement = isHolderReplacementLiquidation(contract.getId());
        if (!holderReplacement) {
            requireConfirmedMoveOutHandover(contract.getId());
        }
        applyLiquidationDraftValues(
                liquidation,
                contract,
                finalLiquidationDate,
                finalReason,
                depositAmount,
                holderReplacement
        );
        liquidation.setStatus(LiquidationStatus.DRAFT);
        liquidation = contractLiquidationRepository.saveAndFlush(liquidation);
        // Keep the invoice/payment intent locally issued; PayOS checkout creation is retryable.
        InvoiceEntity finalInvoice = upsertFinalSettlementInvoice(
                contract,
                liquidation,
                charges,
                true,
                holderReplacement
        );
        liquidation.setFinalInvoice(finalInvoice);
        contractLiquidationRepository.save(liquidation);
        if (!holderReplacement) {
            ensureLiquidationDepositForfeitureRequest(contract, liquidation);
        }

        contract.setStatus(LeaseStatus.TERMINATION_PENDING);
        contract.setTenantIntention("MOVE_OUT");
        contract.setExpectedVacantDate(holderReplacement ? null : finalLiquidationDate);
        if (contract.getIntentionRecordedAt() == null) {
            contract.setIntentionRecordedAt(LocalDateTime.now());
        }
        leaseContractRepository.saveAndFlush(contract);

        return findOne(contract.getId());
    }

    private void applyLiquidationDraftValues(
            ContractLiquidationEntity liquidation,
            LeaseContractEntity contract,
            LocalDate liquidationDate,
            String reason,
            Long depositAmount,
            boolean depositCarriedForward
    ) {
        liquidation.setLiquidationDate(liquidationDate);
        liquidation.setReason(reason);
        liquidation.setDepositAmount(depositAmount);
        LiquidationDepositSettlement depositSettlement = calculateLiquidationDepositSettlement(
                contract,
                liquidationDate,
                depositAmount,
                depositCarriedForward
        );
        liquidation.setDepositDeductionAmount(depositSettlement.deductionAmount());
        liquidation.setDepositDeductionReason(depositSettlement.deductionReason());
        liquidation.setDepositRefundAmount(depositSettlement.refundAmount());
    }

    private InvoiceEntity upsertFinalSettlementInvoice(
            LeaseContractEntity contract,
            ContractLiquidationEntity liquidation,
            List<LiquidationChargeInput> charges,
            boolean issue,
            boolean depositCarriedForward
    ) {
        List<LiquidationChargeInput> normalizedCharges = normalizeLiquidationCharges(contract, liquidation.getLiquidationDate(), charges);
        String billingPeriod = liquidation.getLiquidationDate() == null
                ? YearMonth.now().toString()
                : YearMonth.from(liquidation.getLiquidationDate()).toString();
        long subtotal = normalizedCharges.stream()
                .mapToLong(charge -> safe(charge.unitPrice()) * safeInt(charge.quantity()))
                .sum();
        long depositAmount = liquidation.getDepositAmount() != null
                ? liquidation.getDepositAmount()
                : resolveLiquidationDepositAmount(contract);
        long totalAmount = Math.max(0L, subtotal);

        liquidation.setDepositAmount(depositAmount);
        LiquidationDepositSettlement depositSettlement = calculateLiquidationDepositSettlement(
                contract,
                liquidation.getLiquidationDate(),
                depositAmount,
                depositCarriedForward
        );
        liquidation.setDepositDeductionAmount(depositSettlement.deductionAmount());
        liquidation.setDepositDeductionReason(depositSettlement.deductionReason());
        liquidation.setDepositRefundAmount(depositSettlement.refundAmount());

        InvoiceEntity invoice = liquidation.getFinalInvoice() != null
                ? liquidation.getFinalInvoice()
                : invoiceRepository.findFirstByLeastContract_IdAndBillingPeriodAndInvoiceTypeAndStatusNotOrderByIdDesc(
                        contract.getId(),
                        billingPeriod,
                        InvoiceType.FINAL_SETTLEMENT,
                        InvoiceStatus.VOIDED
                ).orElse(null);
        LocalDateTime now = LocalDateTime.now();
        if (invoice != null && invoice.getStatus() != InvoiceStatus.DRAFT) {
            if (safe(invoice.getPaidAmount()) > 0 || invoice.getStatus() == InvoiceStatus.PAID || invoice.getStatus() == InvoiceStatus.PARTIALLY_PAID) {
                throw new AppException(ApiErrorCode.LEASE_LIQUIDATION_INVOICE_PAID_IMMUTABLE);
            }
            invoice.setStatus(InvoiceStatus.VOIDED);
            invoice.setVoidedAt(now);
            invoice.setVoidReason("Thay thế bằng hóa đơn thanh lý mới sau khi cập nhật hồ sơ.");
            invoiceRepository.saveAndFlush(invoice);
            invoice = null;
        }

        if (invoice == null) {
            invoice = invoiceRepository.save(InvoiceEntity.builder()
                    .invoiceCode(buildLiquidationInvoiceCode(contract.getId(), billingPeriod, now))
                    .property(contract.getRoom().getProperty())
                    .room(contract.getRoom())
                    .leastContract(contract)
                    .invoiceType(InvoiceType.FINAL_SETTLEMENT)
                    .invoiceReason(InvoiceReason.ROOM_CLOSE)
                    .revisionNo(nextFinalSettlementRevision(contract.getId(), billingPeriod))
                    .billingPeriod(billingPeriod)
                    .issueDate(now)
                    .dueDate(now.plusDays(7))
                    // Issue after all liquidation lines are persisted so the shared payment flow
                    // can create the payment intent with the final invoice amount.
                    .status(InvoiceStatus.DRAFT)
                    .subtotalAmount(subtotal)
                    .discountAmount(0L)
                    .totalAmount(totalAmount)
                    .paidAmount(0L)
                    .remainingAmount(totalAmount)
                    .createdBy(AuthUtils.getCurrentAuthenticationId() == null ? null : UserEntity.builder().id(AuthUtils.getCurrentAuthenticationId()).build())
                    .issuedAt(null)
                    .build());
        } else {
            invoice.setInvoiceType(InvoiceType.FINAL_SETTLEMENT);
            invoice.setInvoiceReason(InvoiceReason.ROOM_CLOSE);
            invoice.setBillingPeriod(billingPeriod);
            invoice.setIssueDate(invoice.getIssueDate() == null ? now : invoice.getIssueDate());
            invoice.setDueDate(invoice.getDueDate() == null || issue ? now.plusDays(7) : invoice.getDueDate());
            invoice.setStatus(InvoiceStatus.DRAFT);
            invoice.setSubtotalAmount(subtotal);
            invoice.setDiscountAmount(0L);
            invoice.setTotalAmount(totalAmount);
            invoice.setPaidAmount(0L);
            invoice.setRemainingAmount(totalAmount);
            invoice.setIssuedAt(null);
            invoice = invoiceRepository.save(invoice);
            invoiceLineRepository.deleteAll(invoiceLineRepository.findByInvoice_IdOrderByIdAsc(invoice.getId()));
        }

        for (LiquidationChargeInput charge : normalizedCharges) {
            MeterReadingEntity meterReading = createLiquidationMeterReading(contract, liquidation.getLiquidationDate(), charge);
            invoiceLineRepository.save(InvoiceLineEntity.builder()
                    .invoice(invoice)
                    .lineType(charge.lineType())
                    .description(charge.description())
                    .quantity(safeInt(charge.quantity()))
                    .unitPrice(safe(charge.unitPrice()))
                    .meterReading(meterReading)
                    .sourceType("CONTRACT_LIQUIDATION")
                    .sourceId(liquidation.getId())
                    .build());
        }
        invoice = invoiceRepository.save(invoice);
        if (!issue) {
            return invoice;
        }
        if (totalAmount <= 0) {
            invoice.setStatus(InvoiceStatus.ISSUED);
            invoice.setIssuedAt(now);
            return invoiceRepository.save(invoice);
        }
        return issuedInvoiceChargeService.issueDraftInvoiceForLiquidation(invoice.getId()).invoice();
    }

    private String buildLiquidationInvoiceCode(Long contractId, String billingPeriod, LocalDateTime now) {
        return "INV-LIQ-" + contractId + "-" + billingPeriod.replace("-", "") + "-"
                + now.toString().replace(":", "").replace("-", "").replace("T", "").replace(".", "");
    }

    private int nextFinalSettlementRevision(Long contractId, String billingPeriod) {
        Integer maxRevision = jdbcTemplate.queryForObject("""
                        SELECT COALESCE(MAX(revision_no), 0)
                        FROM invoices
                        WHERE lease_contract_id = ?
                          AND billing_period = ?
                          AND invoice_type = ?
                        """,
                Integer.class,
                contractId,
                billingPeriod,
                InvoiceType.FINAL_SETTLEMENT.name()
        );
        return (maxRevision == null ? 0 : maxRevision) + 1;
    }

    private void requireNoUnpaidInvoicesForLiquidation(Long contractId) {
        LeaseContractDebtPolicy.requireNoOutstandingDebt(jdbcTemplate, contractId);
    }

    private ExpenseRequestService.LiquidationDepositRefundLink ensureLiquidationDepositRefundRequest(
            LeaseContractEntity contract,
            ContractLiquidationEntity liquidation
    ) {
        RoomEntity room = contract.getRoom();
        return expenseRequestService.ensureLiquidationDepositRefundRequest(
                contract.getId(),
                contract.getContractCode(),
                room == null || room.getProperty() == null ? null : room.getProperty().getId(),
                room == null ? null : room.getId(),
                room == null ? null : room.getRoomCode(),
                liquidation.getDepositRefundAmount(),
                liquidation.getLiquidationDate(),
                AuthUtils.getCurrentAuthenticationId(),
                contract.getPrimaryTenantProfile() == null || contract.getPrimaryTenantProfile().getUser() == null
                        ? null
                        : contract.getPrimaryTenantProfile().getUser().getId()
        );
    }

    private ExpenseRequestService.LiquidationDepositForfeitureLink ensureLiquidationDepositForfeitureRequest(
            LeaseContractEntity contract,
            ContractLiquidationEntity liquidation
    ) {
        RoomEntity room = contract.getRoom();
        return expenseRequestService.ensureLiquidationDepositForfeitureRequest(
                contract.getId(),
                contract.getContractCode(),
                room == null || room.getProperty() == null ? null : room.getProperty().getId(),
                room == null ? null : room.getId(),
                room == null ? null : room.getRoomCode(),
                liquidation.getDepositDeductionAmount(),
                liquidation.getDepositDeductionReason(),
                liquidation.getLiquidationDate(),
                AuthUtils.getCurrentAuthenticationId(),
                contract.getPrimaryTenantProfile() == null || contract.getPrimaryTenantProfile().getUser() == null
                        ? null
                        : contract.getPrimaryTenantProfile().getUser().getId()
        );
    }

    private void requireLiquidationDepositForfeitureConfirmed(
            LeaseContractEntity contract,
            ContractLiquidationEntity liquidation
    ) {
        if (safe(liquidation.getDepositDeductionAmount()) <= 0) {
            return;
        }
        ExpenseRequestService.LiquidationDepositForfeitureLink forfeitureLink =
                expenseRequestService.getLiquidationDepositForfeitureLink(contract.getId());
        if (!"TENANT_CONFIRMED".equals(forfeitureLink.status())
                && !"AUTOMATICALLY_FORFEITED".equals(forfeitureLink.status())) {
            throw new AppException(ApiErrorCode.LIQUIDATION_DEPOSIT_FORFEITURE_CONFIRMATION_REQUIRED);
        }
    }

    private void requireLiquidationDepositRefundConfirmed(
            LeaseContractEntity contract,
            ContractLiquidationEntity liquidation
    ) {
        if (safe(liquidation.getDepositRefundAmount()) <= 0) {
            ensureLiquidationDepositRefundRequest(contract, liquidation);
            return;
        }
        ExpenseRequestService.LiquidationDepositRefundLink refundLink =
                ensureLiquidationDepositRefundRequest(contract, liquidation);
        if (!"TENANT_CONFIRMED".equals(refundLink.status())) {
            throw new AppException(ApiErrorCode.LEASE_DEPOSIT_SETTLEMENT_CONFIRMATION_REQUIRED);
        }
    }

    private void requireConfirmedMoveOutHandover(Long contractId) {
        ContractHandoverRecordEntity handover = handoverRecordRepository
                .findFirstByContract_IdAndHandoverTypeOrderByCreatedAtDesc(contractId, HandoverType.MOVE_OUT)
                .orElseThrow(() -> new AppException(ApiErrorCode.LEASE_MOVE_OUT_HANDOVER_REQUIRED));
        if (handover.getStatus() != HandoverStatus.CONFIRMED
                || handover.getElectricityReading() == null) {
            throw new AppException(ApiErrorCode.LEASE_MOVE_OUT_HANDOVER_REQUIRED);
        }
    }

    private long resolveLiquidationDepositAmount(LeaseContractEntity contract) {
        if (contract == null) {
            return 0L;
        }
        return safe(contract.getDepositAmount());
    }

    static boolean isShortTermEarlyTermination(
            LocalDate contractEndDate,
            LocalDate liquidationDate
    ) {
        if (contractEndDate == null || liquidationDate == null) {
            return false;
        }
        return liquidationDate.isBefore(contractEndDate)
                && liquidationDate.isAfter(contractEndDate.minusMonths(1));
    }

    private LiquidationDepositSettlement calculateLiquidationDepositSettlement(
            LeaseContractEntity contract,
            LocalDate liquidationDate,
            Long depositAmount,
            boolean depositCarriedForward
    ) {
        long safeDepositAmount = safe(depositAmount);
        if (safeDepositAmount <= 0L) {
            return new LiquidationDepositSettlement(0L, 0L, null);
        }
        if (depositCarriedForward) {
            return new LiquidationDepositSettlement(safeDepositAmount, 0L, null);
        }
        if (isShortTermEarlyTermination(contract == null ? null : contract.getEndDate(), liquidationDate)) {
            return new LiquidationDepositSettlement(
                    safeDepositAmount,
                    safeDepositAmount,
                    "Khách chấm dứt hoặc trả phòng trước hạn khi hợp đồng còn dưới 1 tháng."
            );
        }
        return new LiquidationDepositSettlement(safeDepositAmount, 0L, null);
    }

    private List<LiquidationChargeInput> normalizeLiquidationCharges(
            LeaseContractEntity contract,
            LocalDate liquidationDate,
            List<LiquidationChargeInput> charges
    ) {
        List<LiquidationChargeInput> normalized = (charges == null ? List.<LiquidationChargeInput>of() : charges).stream()
                .filter(Objects::nonNull)
                .filter(charge -> charge.lineType() != null)
                .filter(charge -> charge.lineType() != InvoiceLineType.ROOM_RENT)
                .filter(charge -> charge.lineType() != InvoiceLineType.WATER)
                .filter(charge -> safe(charge.unitPrice()) > 0)
                .map(charge -> new LiquidationChargeInput(
                        charge.lineType(),
                        charge.description() == null || charge.description().isBlank()
                                ? defaultLiquidationChargeLabel(charge.lineType())
                                : charge.description().trim(),
                        isMeterCharge(charge.lineType())
                                ? Math.max(0, safeInt(charge.quantity()))
                                : charge.quantity() == null || charge.quantity() <= 0 ? 1 : charge.quantity(),
                        charge.unitPrice(),
                        charge.previousValue(),
                        charge.currentValue(),
                        charge.photoFileId()
                ))
                .toList();
        long proratedRoomRent = calculateLiquidationRoomRent(contract, liquidationDate);
        if (proratedRoomRent <= 0) {
            return normalized;
        }
        List<LiquidationChargeInput> withRoomRent = new ArrayList<>(normalized);
        withRoomRent.add(new LiquidationChargeInput(
                InvoiceLineType.ROOM_RENT,
                "Tiền phòng tháng " + YearMonth.from(liquidationDate) + " đến ngày " + liquidationDate,
                1,
                proratedRoomRent,
                null,
                null,
                null
        ));
        return withRoomRent;
    }

    private MeterReadingEntity createLiquidationMeterReading(
            LeaseContractEntity contract,
            LocalDate liquidationDate,
            LiquidationChargeInput charge
    ) {
        if (contract == null || contract.getRoom() == null || !isMeterCharge(charge.lineType())) {
            return null;
        }
        if (charge.previousValue() == null || charge.currentValue() == null) {
            return null;
        }
        if (charge.currentValue().compareTo(charge.previousValue()) < 0) {
            throw new AppException(ApiErrorCode.LEASE_HANDOVER_METER_READING_BELOW_PREVIOUS);
        }
        MeterType meterType = MeterType.ELECTRICITY;
        RoomEntity room = contract.getRoom();
        LocalDate readingDate = liquidationDate == null ? LocalDate.now() : liquidationDate;
        String readingPeriod = YearMonth.from(readingDate).toString();
        MeterEntity activeMeter = meterRepository
                .findFirstByRoom_IdAndMeterTypeAndStatus(room.getId(), meterType, MeterStatus.ACTIVE)
                .orElseGet(() -> meterRepository.save(MeterEntity.builder()
                        .room(room)
                        .meterType(meterType)
                        .status(MeterStatus.ACTIVE)
                        .installedAt(readingDate)
                        .build()));

        int nextRevision = 1;
        var existingPeriodReading = meterReadingRepository
                .findFirstByMeter_IdAndReadingPeriodOrderByRevisionNoDesc(activeMeter.getId(), readingPeriod);
        if (existingPeriodReading.isPresent()) {
            MeterReadingEntity existing = existingPeriodReading.get();
            nextRevision = safeInt(existing.getRevisionNo()) + 1;
            if (existing.getStatus() != ReadingStatus.VOIDED) {
                existing.setStatus(ReadingStatus.VOIDED);
                existing.setVoidReason("Bị thay thế bởi phiên bản chốt thanh lý số " + nextRevision);
                meterReadingRepository.saveAndFlush(existing);
            }
        }

        Long currentUserId = AuthUtils.getCurrentAuthenticationId();
        MeterReadingEntity reading = MeterReadingEntity.builder()
                .meter(activeMeter)
                .room(room)
                .readingPeriod(readingPeriod)
                .revisionNo(nextRevision)
                .previousValue(charge.previousValue())
                .currentValue(charge.currentValue())
                .readingDate(readingDate)
                .purpose(ReadingPurpose.MOVE_OUT)
                .status(ReadingStatus.CONFIRMED)
                .createdBy(currentUserId == null ? null : UserEntity.builder().id(currentUserId).build())
                .build();
        if (charge.photoFileId() != null) {
            reading.setPhotoFile(fileMetadataRepository.getReferenceById(charge.photoFileId()));
        }
        return meterReadingRepository.save(reading);
    }

    private boolean isMeterCharge(InvoiceLineType lineType) {
        return lineType == InvoiceLineType.ELECTRICITY;
    }

    private long calculateLiquidationRoomRent(LeaseContractEntity contract, LocalDate liquidationDate) {
        if (contract == null || liquidationDate == null || safe(contract.getMonthlyRent()) <= 0) {
            return 0L;
        }
        LocalDate periodStart = liquidationDate.withDayOfMonth(1);
        LocalDate chargeStart = contract.getRentStartDate() != null
                ? contract.getRentStartDate()
                : contract.getStartDate();
        if (chargeStart == null || chargeStart.isBefore(periodStart)) {
            chargeStart = periodStart;
        }
        LocalDate chargeEnd = liquidationDate;
        if (contract.getEndDate() != null && contract.getEndDate().isBefore(chargeEnd)) {
            chargeEnd = contract.getEndDate();
        }
        if (chargeEnd.isBefore(chargeStart)) {
            return 0L;
        }
        int chargeableDays = chargeEnd.getDayOfMonth() - chargeStart.getDayOfMonth() + 1;
        int daysInMonth = liquidationDate.lengthOfMonth();
        long monthlyRent = safe(contract.getMonthlyRent());
        return (monthlyRent * chargeableDays + daysInMonth - 1) / daysInMonth;
    }

    private String defaultLiquidationChargeLabel(InvoiceLineType lineType) {
        return switch (lineType) {
            case ELECTRICITY -> "Tiền điện chốt phòng";
            case WATER -> "Tiền nước chốt phòng";
            case SERVICE_FEE -> "Phí dịch vụ chốt phòng";
            case ROOM_RENT -> "Tiền phòng chốt";
            case MAINTENANCE_COMPENSATION -> "Bồi thường sửa chữa";
            case VIOLATION_FINE -> "Phạt vi phạm";
            case TRANSFER_DIFFERENCE -> "Chênh lệch chuyển phòng";
            case DEPOSIT_DEDUCTION -> "Khấu trừ cọc";
            case MANUAL_ADJUSTMENT -> "Điều chỉnh thủ công";
            case OTHER -> "Phí phát sinh";
        };
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private long safe(Long value) {
        return value == null ? 0L : value;
    }

    public LeaseContractRenewalResponse renew(
            Long leaseContractId,
            LocalDate newStartDate,
            LocalDate newEndDate,
            Long monthlyRent,
            Integer paymentCycleMonths,
            Long depositAmount,
            String requestedContractCode,
            String note
    ) {
        assertOwnerOrManagerCanRenew();
        LeaseContractEntity oldContract = leaseContractRepository.findById(leaseContractId)
                .orElseThrow(() -> new AppException(ApiErrorCode.LEASE_CONTRACT_NOT_FOUND));
        if (oldContract.getDeletedAt() != null) {
            throw new AppException(ApiErrorCode.LEASE_CONTRACT_NOT_FOUND);
        }
        if (!List.of(LeaseStatus.ACTIVE, LeaseStatus.EXPIRING_SOON, LeaseStatus.EXPIRED)
                .contains(oldContract.getStatus())) {
            throw new AppException(ApiErrorCode.LEASE_RENEWAL_STATUS_INVALID);
        }
        if (leaseContractRepository.existsByPreviousContract_IdAndDeletedAtIsNull(oldContract.getId())) {
            throw new AppException(ApiErrorCode.LEASE_RENEWAL_ALREADY_EXISTS);
        }

        validateContractTerms(newStartDate, paymentCycleMonths, monthlyRent, depositAmount);
        RoomEntity room = oldContract.getRoom();
        if (room == null) {
            throw new AppException(ApiErrorCode.LEASE_RENEWAL_ROOM_REQUIRED);
        }
        if (hasOtherActiveContract(room.getId(), oldContract.getId())) {
            throw new AppException(ApiErrorCode.LEASE_RENEWAL_ROOM_CONTRACT_CONFLICT);
        }

        RoomStatus previousRoomStatus = room.getCurrentStatus();
        RoomCommitmentChecker.Blocker blocker =
                roomCommitmentChecker.checkRenewBlockers(
                        room.getId(),
                        oldContract.getId(),
                        oldContract.getEndDate()
                );
        if (blocker != RoomCommitmentChecker.Blocker.NONE) {
            throwRenewBlocked(blocker);
        }
        if (previousRoomStatus == RoomStatus.SOON_VACANT) {
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
                    "Khách cũ đổi ý tái ký hợp đồng " + oldContract.getContractCode()
            );
            appendContractEvent(
                    oldContract.getId(),
                    "RENEWAL_AFTER_MOVE_OUT_INTENT",
                    "Chủ trọ xác nhận tái ký sau khi khách đã báo chuyển đi"
            );
        }

        String newContractCode = resolveRenewalContractCode(oldContract, requestedContractCode, newStartDate);
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
                    "Tạo hợp đồng tái ký " + newContractCode
            );
        }

        String eventNote = note == null || note.isBlank() ? "Tạo hợp đồng tái ký" : note.trim();
        appendContractEvent(
                newContract.getId(),
                "CREATED",
                "Tái ký từ hợp đồng " + oldContract.getContractCode() + "; ghi chú=" + eventNote
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

    public LeaseContractManagementResponse addCoOccupantFromChangeRequest(
            Long leaseContractId,
            Long tenantProfileId,
            LocalDate moveInDate,
            Long approvedBy
    ) {
        LeaseContractEntity contract = leaseContractRepository.findById(leaseContractId)
                .orElseThrow(() -> new AppException(ApiErrorCode.LEASE_CONTRACT_NOT_FOUND));
        if (contract.getDeletedAt() != null) {
            throw new AppException(ApiErrorCode.LEASE_CONTRACT_NOT_FOUND);
        }
        if (!List.of(LeaseStatus.ACTIVE, LeaseStatus.EXPIRING_SOON).contains(contract.getStatus())) {
            throw new AppException(ApiErrorCode.LEASE_CO_OCCUPANT_ADD_FORBIDDEN);
        }
        if (tenantProfileId == null) {
            throw new AppException(ApiErrorCode.LEASE_CO_OCCUPANT_PROFILE_REQUIRED);
        }
        if (contract.getPrimaryTenantProfile() != null
                && Objects.equals(contract.getPrimaryTenantProfile().getId(), tenantProfileId)) {
            throw new AppException(ApiErrorCode.LEASE_PERSON_ALREADY_PRIMARY_HOLDER);
        }
        Integer profileExists = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM person_profiles
                        WHERE person_profile_id = ?
                          AND deleted_at IS NULL
                        """,
                Integer.class,
                tenantProfileId
        );
        if (profileExists == null || profileExists == 0) {
            throw new AppException(ApiErrorCode.LEASE_CO_OCCUPANT_PROFILE_NOT_FOUND);
        }

        Integer activeDuplicate = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM contract_occupants
                        WHERE contract_id = ?
                          AND tenant_profile_id = ?
                          AND status = 'ACTIVE'
                        """,
                Integer.class,
                contract.getId(),
                tenantProfileId
        );
        if (activeDuplicate != null && activeDuplicate > 0) {
            return findOne(contract.getId());
        }

        RoomEntity room = contract.getRoom();
        if (room == null) {
            throw new AppException(ApiErrorCode.LEASE_RENEWAL_ROOM_REQUIRED);
        }
        if (roomCommitmentChecker.isSoonVacantBookingCase(
                room.getId(),
                contract.getId(),
                contract.getEndDate()
        )) {
            throw new AppException(ApiErrorCode.ROOM_CO_OCCUPANT_ADD_BLOCKED_BY_BOOKING);
        }
        Integer activeOccupants = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM contract_occupants
                        WHERE contract_id = ?
                          AND status = 'ACTIVE'
                        """,
                Integer.class,
                contract.getId()
        );
        int maxOccupants = room.getMaxOccupants() != null ? room.getMaxOccupants() : 3;
        if (activeOccupants != null && activeOccupants >= maxOccupants) {
            throw new AppException(ApiErrorCode.LEASE_OCCUPANCY_LIMIT_REACHED);
        }

        Long propertyId = room.getProperty() == null ? null : room.getProperty().getId();
        Long tenantId = resolveTenantIdForProfile(tenantProfileId, propertyId);
        LocalDate finalMoveInDate = moveInDate == null ? LocalDate.now() : moveInDate;
        jdbcTemplate.update("""
                        INSERT INTO contract_occupants (
                            contract_id,
                            tenant_id,
                            tenant_profile_id,
                            occupant_role,
                            move_in_date,
                            status,
                            created_at
                        )
                        VALUES (?, ?, ?, 'CO_OCCUPANT', ?, 'ACTIVE', NOW(6))
                        ON DUPLICATE KEY UPDATE
                            tenant_id = VALUES(tenant_id),
                            occupant_role = 'CO_OCCUPANT',
                            move_in_date = VALUES(move_in_date),
                            move_out_date = NULL,
                            status = 'ACTIVE',
                            disabled_reason = NULL,
                            disabled_by = NULL,
                            disabled_at = NULL
                        """,
                contract.getId(),
                tenantId,
                tenantProfileId,
                finalMoveInDate
        );
        appendContractEvent(
                contract.getId(),
                "OCCUPANT_CHANGED",
                "Thêm người ở cùng profileId=" + tenantProfileId + "; approvedBy=" + approvedBy
        );
        return findOne(contract.getId());
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
                .orElseThrow(() -> new AppException(ApiErrorCode.LEASE_CONTRACT_NOT_FOUND));
        if (contract.getDeletedAt() != null) {
            throw new AppException(ApiErrorCode.LEASE_CONTRACT_NOT_FOUND);
        }
        if (!List.of(LeaseStatus.ACTIVE, LeaseStatus.EXPIRING_SOON).contains(contract.getStatus())) {
            throw new AppException(ApiErrorCode.LEASE_INTENT_STATUS_INVALID);
        }

        String normalizedIntention = normalizeTenantIntention(intention);
        log.info(normalizedIntention);
        if (!TENANT_INTENTIONS.contains(normalizedIntention)) {
            throw new AppException(ApiErrorCode.LEASE_TENANT_INTENT_INVALID);
        }
        LocalDate today = LocalDate.now();
        boolean withinThreeMonths = isWithinThreeMonths(contract, today);
        if (List.of("MOVE_OUT", "TRANSFER").contains(normalizedIntention)) {
            if (!withinThreeMonths) {
                throw new AppException(ApiErrorCode.LEASE_MOVE_OUT_TRANSFER_WINDOW_INVALID);
            }
            if (expectedMoveOutDate == null) {
                throw new AppException(ApiErrorCode.LEASE_EXPECTED_HANDOVER_DATE_REQUIRED);
            }
            if (expectedMoveOutDate.isBefore(today)) {
                throw new AppException(ApiErrorCode.LEASE_EXPECTED_HANDOVER_DATE_IN_PAST);
            }
            if (contract.getEndDate() != null && expectedMoveOutDate.isAfter(contract.getEndDate())) {
                throw new AppException(ApiErrorCode.LEASE_EXPECTED_HANDOVER_DATE_AFTER_CONTRACT_END);
            }
        }

        RoomEntity room = contract.getRoom();
        if (room == null) {
            throw new AppException(ApiErrorCode.LEASE_RENEWAL_ROOM_REQUIRED);
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
            if ("RENEW".equals(normalizedIntention)) {
                RoomCommitmentChecker.Blocker blocker =
                        roomCommitmentChecker.checkRenewBlockers(
                                room.getId(),
                                contract.getId(),
                                contract.getEndDate()
                        );
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
                            "Khách đổi ý tiếp tục thuê sau khi đã báo chuyển đi"
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
        leaseExpiryReminderService.onTenantIntentionRecorded(contract.getId(), LocalDate.now());
        return findOne(contract.getId());
    }

    private void lockContractAndRoom(Long leaseContractId) {
        List<Long> locked = jdbcTemplate.query("""
                        SELECT lc.lease_contract_id AS id
                        FROM lease_contracts lc
                        JOIN rooms r ON r.room_id = lc.room_id
                        WHERE lc.lease_contract_id = ?
                        FOR UPDATE
                        """,
                (rs, rowNum) -> rs.getLong("id"),
                leaseContractId
        );
        if (locked.isEmpty()) {
            throw new AppException(ApiErrorCode.LEASE_CONTRACT_NOT_FOUND);
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
            throw new AppException(ApiErrorCode.UNAUTHENTICATED);
        }
        Integer contractExists = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM lease_contracts lc
                        WHERE lc.lease_contract_id = ?
                          AND lc.deleted_at IS NULL
                        """,
                Integer.class,
                leaseContractId
        );
        if (contractExists == null || contractExists == 0) {
            throw new AppException(ApiErrorCode.LEASE_CONTRACT_NOT_FOUND);
        }

        Integer isPrimarySigner = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM lease_contracts lc
                        JOIN person_profiles pp ON pp.person_profile_id = lc.primary_tenant_profile_id
                        LEFT JOIN tenant_account_provisionings tap
                               ON tap.tenant_profile_id = pp.person_profile_id
                              AND tap.user_id = ?
                        WHERE lc.lease_contract_id = ?
                          AND lc.deleted_at IS NULL
                          AND pp.deleted_at IS NULL
                          AND (pp.user_id = ? OR (tap.user_id = ? AND tap.status <> 'DISABLED'))
                          AND NOT EXISTS (
                              SELECT 1
                              FROM contract_occupants disabled_primary
                              WHERE disabled_primary.contract_id = lc.lease_contract_id
                                AND disabled_primary.tenant_profile_id = pp.person_profile_id
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
            throw new AppException(ApiErrorCode.LEASE_PRIMARY_HOLDER_ONLY_INTENT);
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
                .orElseThrow(() -> new AppException(ApiErrorCode.LEASE_CONTRACT_NOT_FOUND));
        if (contract.getStatus() == LeaseStatus.ACTIVE) {
            return findOne(leaseContractId);
        }
        ensureNotRoomTransferManagedContract(leaseContractId);
        if (contract.getStatus() != LeaseStatus.DRAFT && contract.getStatus() != LeaseStatus.PENDING_SIGNATURE) {
            throw new AppException(ApiErrorCode.LEASE_ACTIVATION_STATUS_INVALID);
        }
        if (contract.getSignedFile() == null) {
            throw new AppException(ApiErrorCode.LEASE_SIGNED_FILE_REQUIRED_FOR_ACTIVATION);
        }
        if (contract.getPrimaryTenantProfile() == null) {
            throw new AppException(ApiErrorCode.LEASE_ACTIVATION_PRIMARY_HOLDER_REQUIRED);
        }
        if (contract.getStartDate() == null || contract.getEndDate() == null || contract.getEndDate().isBefore(contract.getStartDate())) {
            throw new AppException(ApiErrorCode.LEASE_CONTRACT_DATES_INVALID);
        }
        RoomEntity room = contract.getRoom();
        if (room == null) {
            throw new AppException(ApiErrorCode.LEASE_RENEWAL_ROOM_REQUIRED);
        }

        // Bắt buộc phải có bản ghi bàn giao MOVE_IN kèm chỉ số điện và bản ký trước khi kích hoạt
        // Skip check for renewal contracts (previous contract exists)
        if (contract.getPreviousContract() == null) {
            Integer handoverCount = jdbcTemplate.queryForObject("""
                            SELECT COUNT(*)
                            FROM contract_handover_records
                            WHERE contract_id = ?
                              AND handover_type = 'MOVE_IN'
                              AND electricity_reading_id IS NOT NULL
                              AND signed_document_id IS NOT NULL
                            """,
                    Integer.class,
                    leaseContractId
            );
            if (handoverCount == null || handoverCount == 0) {
                throw new AppException(ApiErrorCode.LEASE_ACTIVATION_HANDOVER_REQUIRED);
            }
        }

        boolean renewalActivation = contract.getPreviousContract() != null
                && (room.getCurrentStatus() == RoomStatus.OCCUPIED
                || room.getCurrentStatus() == RoomStatus.EXPIRED);
        if (!renewalActivation
                && room.getCurrentStatus() != RoomStatus.RESERVED
                && room.getCurrentStatus() != RoomStatus.VACANT
                && room.getCurrentStatus() != RoomStatus.ON_HOLD) {
            throw new AppException(ApiErrorCode.LEASE_ROOM_STATUS_INVALID_FOR_ACTIVATION);
        }
        Long previousContractId = contract.getPreviousContract() != null
                ? contract.getPreviousContract().getId()
                : null;
        if (hasOtherActiveContract(room.getId(), contract.getId(), previousContractId)) {
            throw new AppException(ApiErrorCode.LEASE_ROOM_ACTIVE_CONTRACT_EXISTS);
        }

        ensureContractOccupants(contract);
        LeaseContractEntity previousContract = contract.getPreviousContract();
        if (previousContract != null && isHolderReplacementLiquidation(previousContract.getId())) {
            throw new AppException(ApiErrorCode.LEASE_REPLACEMENT_LIQUIDATION_REQUIRED);
        }
        if (previousContract != null) {
            copyContractOccupants(previousContract, contract);
            boolean legacyPrematureRenewal =
                    previousContract.getStatus() == LeaseStatus.RENEWED
                            && List.of(LeaseStatus.DRAFT, LeaseStatus.PENDING_SIGNATURE)
                            .contains(contract.getStatus());
            if (!legacyPrematureRenewal
                    && !List.of(LeaseStatus.ACTIVE, LeaseStatus.EXPIRING_SOON, LeaseStatus.EXPIRED)
                    .contains(previousContract.getStatus())) {
                throw new AppException(ApiErrorCode.LEASE_PREVIOUS_CONTRACT_RENEWAL_STATUS_INVALID);
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
        appendRoomStatusHistory(room.getId(), fromStatus, RoomStatus.OCCUPIED, "Kích hoạt hợp đồng thuê " + contract.getContractCode());

        appendContractEvent(contract.getId(), "SIGNED", "Kích hoạt hợp đồng thuê");
        if (previousContract != null) {
            appendContractEvent(
                    previousContract.getId(),
                    "RENEWED",
                    "Kích hoạt hợp đồng tái ký; newContractId=" + contract.getId()
            );
        }
        return findOne(contract.getId());
    }

    public LeaseContractManagementResponse updateTerms(
            Long leaseContractId,
            LocalDate startDate,
            Integer paymentCycleMonths,
            Long monthlyRent,
            Long depositAmount
    ) {
        LeaseContractEntity contract = leaseContractRepository.findById(leaseContractId)
                .orElseThrow(() -> new AppException(ApiErrorCode.LEASE_CONTRACT_NOT_FOUND));
        if (contract.getDeletedAt() != null) {
            throw new AppException(ApiErrorCode.LEASE_CONTRACT_NOT_FOUND);
        }
        if (List.of(
                LeaseStatus.LIQUIDATED,
                LeaseStatus.AUTO_TERMINATED,
                LeaseStatus.CANCELLED
        ).contains(contract.getStatus())) {
            throw new AppException(ApiErrorCode.LEASE_ENDED_CONTRACT_IMMUTABLE);
        }

        validateContractTerms(startDate, paymentCycleMonths, monthlyRent, depositAmount);
        RoomEntity room = contract.getRoom();
        LocalDate currentEndDate = contract.getEndDate();
//        if (extendsEndDate && room != null) {
//            if (hasOtherActiveContract(room.getId(), contract.getId())) {
//                throw new AppException(ApiErrorCode.LEASE_RENEWAL_ROOM_CONTRACT_CONFLICT);
//            }
//            RoomCommitmentChecker.Blocker blocker =
//                    roomCommitmentChecker.checkRenewBlockers(room.getId(), contract.getId(), contract.getEndDate());
//            if (blocker != RoomCommitmentChecker.Blocker.NONE) {
//                throwRenewBlocked(blocker);
//            }
//            if (room.getCurrentStatus() == RoomStatus.SOON_VACANT) {
//                RoomStatus fromStatus = room.getCurrentStatus();
//                room.setCurrentStatus(RoomStatus.OCCUPIED);
//                roomRepository.saveAndFlush(room);
//                contract.setTenantIntention("RENEW");
//                contract.setExpectedVacantDate(null);
//                contract.setIntentionRecordedAt(LocalDateTime.now());
//                appendRoomStatusHistory(
//                        room.getId(),
//                        fromStatus,
//                        RoomStatus.OCCUPIED,
//                        "Gia hạn hợp đồng thuê " + contract.getContractCode()
//                );
//                appendContractEvent(
//                        contract.getId(),
//                        "RENEWAL_AFTER_MOVE_OUT_INTENT",
//                        "Gia hạn hợp đồng sau khi khách đã báo chuyển đi"
//                );
//            }
//        }
        boolean rentChanged = !Objects.equals(contract.getMonthlyRent(), monthlyRent);

        contract.setStartDate(startDate);
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
                    "Cập nhật giá thuê hằng tháng thành " + monthlyRent
            );
        }
        return findOne(contract.getId());
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
                appendRoomStatusHistory(
                        room.getId(),
                        fromStatus,
                        RoomStatus.OCCUPIED,
                        "Gia hạn hợp đồng thuê " + contract.getContractCode()
                );
            }
        }
        if (newStatus == LeaseStatus.EXPIRING_SOON) {
            appendContractEvent(
                    contract.getId(),
                    "NOTICE_SENT",
                    "Cập nhật thời hạn hợp đồng còn dưới hoặc bằng 3 tháng"
            );
            return;
        }
        if (newStatus == LeaseStatus.EXPIRED) {
            appendContractEvent(
                    contract.getId(),
                    "EXPIRED",
                    "Cập nhật thời hạn hợp đồng đã qua ngày kết thúc"
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
                        "Hợp đồng " + contract.getContractCode() + " đã hết hạn"
                );
            }
        }
    }

    @Transactional(readOnly = true)
    public LeaseContractManagementResponse findOne(Long leaseContractId) {
        return jdbcTemplate.query("""
                        SELECT
                            'CONTRACT' AS source_type,
                            lc.lease_contract_id AS lease_contract_id,
                            lc.deposit_form_id AS deposit_form_id,
                            lc.contract_code,
                            df.deposit_code,
                            p.property_id AS property_id,
                            p.name AS property_name,
                            p.address_line AS property_address,
                            (
                                SELECT co.tenant_id
                                FROM contract_occupants co
                                WHERE co.contract_id = lc.lease_contract_id
                                  AND co.status = 'ACTIVE'
                                ORDER BY CASE WHEN co.occupant_role = 'PRIMARY' THEN 0 ELSE 1 END, co.contract_occupant_id ASC
                                LIMIT 1
                            ) AS tenant_id,
                            r.room_id AS room_id,
                            r.room_code,
                            r.current_status AS room_status,
                            pp.person_profile_id AS primary_tenant_profile_id,
                            pp.full_name AS customer_name,
                            pp.phone,
                            pp.email,
                            NULL AS expected_lease_sign_date,
                            lc.rent_start_date AS expected_move_in_date,
                            lc.start_date,
                            lc.end_date,
                            lc.rent_start_date,
                            lc.activation_electricity_value,
                            lc.activation_reading_date,
                            lc.monthly_rent,
                            lc.payment_cycle_months,
                            lc.deposit_amount,
                            df.contract_term_months,
                            lc.previous_contract_id,
                            previous_contract.contract_code AS previous_contract_code,
                            lc.tenant_intention,
                            lc.expected_vacant_date,
                            cl.contract_liquidation_id AS liquidation_id,
                            cl.liquidation_date,
                            cl.reason AS liquidation_reason,
                            cl.deposit_amount AS liquidation_deposit_amount,
                            cl.deposit_deduction_amount AS liquidation_deposit_deduction_amount,
                            cl.deposit_deduction_reason AS liquidation_deposit_deduction_reason,
                            cl.deposit_refund_amount AS liquidation_deposit_refund_amount,
                            cl.final_invoice_id AS liquidation_final_invoice_id,
                            cl.signed_file_id AS liquidation_signed_file_id,
                            cl.status AS liquidation_status,
                            cl.created_at AS liquidation_created_at,
                            tr.room_transfer_request_id AS transfer_request_id,
                            tr.request_code AS transfer_request_code,
                            tr.status AS transfer_status,
                            tr.requested_transfer_date AS transfer_requested_date,
                            CASE
                                WHEN EXISTS (
                                    SELECT 1
                                    FROM room_transfer_requests source_transfer
                                    WHERE source_transfer.old_contract_id = lc.lease_contract_id
                                      AND source_transfer.status IN ('EXECUTED', 'COMPLETED')
                                ) THEN TRUE
                                ELSE FALSE
                            END AS source_transfer_completed,
                            CASE
                                WHEN tr.new_contract_id = lc.lease_contract_id THEN 'NEW_CONTRACT'
                                WHEN tr.replacement_old_contract_id = lc.lease_contract_id THEN 'REPLACEMENT_OLD_CONTRACT'
                                ELSE NULL
                            END AS transfer_contract_role,
                            (
                                SELECT renewed.lease_contract_id
                                FROM lease_contracts renewed
                                WHERE renewed.previous_contract_id = lc.lease_contract_id
                                  AND renewed.deleted_at IS NULL
                                ORDER BY renewed.lease_contract_id DESC
                                LIMIT 1
                            ) AS renewed_contract_id,
                            (
                                SELECT renewed.contract_code
                                FROM lease_contracts renewed
                                WHERE renewed.previous_contract_id = lc.lease_contract_id
                                  AND renewed.deleted_at IS NULL
                                ORDER BY renewed.lease_contract_id DESC
                                LIMIT 1
                            ) AS renewed_contract_code,
                            (
                                SELECT COUNT(*)
                                FROM contract_occupants co
                                WHERE co.contract_id = lc.lease_contract_id
                                  AND co.status = 'ACTIVE'
                            ) AS occupants_count,
                            lc.status AS contract_status,
                            df.deposit_status,
                            lc.contract_file_id,
                            fm.original_name AS contract_file_name,
                            fm.created_at AS contract_file_uploaded_at,
                            lc.signed_file_id,
                            sfm.original_name AS signed_file_name,
                            sfm.created_at AS signed_file_uploaded_at,
                            lc.signed_uploaded_by,
                            lc.signed_at,
                            (
                                SELECT handover.signed_document_id
                                FROM contract_handover_records handover
                                WHERE handover.contract_id = lc.lease_contract_id
                                  AND handover.handover_type = 'MOVE_IN'
                                ORDER BY handover.contract_handover_record_id DESC
                                LIMIT 1
                            ) AS handover_signed_file_id,
                            (
                                SELECT handover.contract_handover_record_id
                                FROM contract_handover_records handover
                                WHERE handover.contract_id = lc.lease_contract_id
                                  AND handover.handover_type = 'MOVE_OUT'
                                ORDER BY handover.contract_handover_record_id DESC
                                LIMIT 1
                            ) AS move_out_handover_record_id,
                            (
                                SELECT handover.status
                                FROM contract_handover_records handover
                                WHERE handover.contract_id = lc.lease_contract_id
                                  AND handover.handover_type = 'MOVE_OUT'
                                ORDER BY handover.contract_handover_record_id DESC
                                LIMIT 1
                            ) AS move_out_handover_status,
                            (
                                SELECT handover.handover_date
                                FROM contract_handover_records handover
                                WHERE handover.contract_id = lc.lease_contract_id
                                  AND handover.handover_type = 'MOVE_OUT'
                                ORDER BY handover.contract_handover_record_id DESC
                                LIMIT 1
                            ) AS move_out_handover_date,
                            (
                                SELECT handover.electricity_reading_id
                                FROM contract_handover_records handover
                                WHERE handover.contract_id = lc.lease_contract_id
                                  AND handover.handover_type = 'MOVE_OUT'
                                ORDER BY handover.contract_handover_record_id DESC
                                LIMIT 1
                            ) AS move_out_handover_electricity_reading_id,
                            (
                                SELECT handover.signed_document_id
                                FROM contract_handover_records handover
                                WHERE handover.contract_id = lc.lease_contract_id
                                  AND handover.handover_type = 'MOVE_OUT'
                                ORDER BY handover.contract_handover_record_id DESC
                                LIMIT 1
                            ) AS move_out_handover_signed_file_id,
                            lc.created_at,
                            u.user_id AS user_id,
                            u.last_login_at
                        FROM lease_contracts lc
                        JOIN rooms r ON r.room_id = lc.room_id
                        JOIN properties p ON p.property_id = r.property_id
                        JOIN person_profiles pp ON pp.person_profile_id = lc.primary_tenant_profile_id
                        LEFT JOIN lease_contracts previous_contract ON previous_contract.lease_contract_id = lc.previous_contract_id
                        LEFT JOIN deposit_forms df ON df.deposit_form_id = lc.deposit_form_id
                        LEFT JOIN contract_liquidations cl ON cl.contract_id = lc.lease_contract_id
                        LEFT JOIN file_metadata fm ON fm.file_metadata_id = lc.contract_file_id
                        LEFT JOIN file_metadata sfm ON sfm.file_metadata_id = lc.signed_file_id
                        LEFT JOIN room_transfer_requests tr
                          ON tr.new_contract_id = lc.lease_contract_id OR tr.replacement_old_contract_id = lc.lease_contract_id
                        LEFT JOIN users u ON u.user_id = pp.user_id AND u.deleted_at IS NULL
                        WHERE lc.deleted_at IS NULL AND lc.lease_contract_id = ?
                        """,
                rs -> {
                    if (!rs.next()) {
                        throw new AppException(ApiErrorCode.LEASE_CONTRACT_NOT_FOUND);
                    }
                    return toResponse(rs);
                },
                leaseContractId
        );
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
            throw new AppException(ApiErrorCode.LEASE_CONTRACT_ROOM_REQUIRED);
        }
        if (primaryTenantProfileId == null) {
            throw new AppException(ApiErrorCode.CONTRACT_MUST_HAVE_HOLDER);
        }
        if (startDate == null || endDate == null || !endDate.isAfter(startDate)) {
            throw new AppException(ApiErrorCode.LEASE_CONTRACT_DATES_RANGE_INVALID);
        }
        if (!Objects.equals(paymentCycleMonths, 1) && !Objects.equals(paymentCycleMonths, 3)) {
            throw new AppException(ApiErrorCode.LEASE_PAYMENT_CYCLE_INVALID);
        }
        if (monthlyRent == null || monthlyRent <= 0) {
            throw new AppException(ApiErrorCode.LEASE_RENT_AMOUNT_INVALID);
        }
        if (depositAmount == null || depositAmount < 0) {
            throw new AppException(ApiErrorCode.INVALID_DEPOSIT_AMOUNT);
        }
        boolean soonVacantDraft = room.getCurrentStatus() == RoomStatus.SOON_VACANT;
        if (room.getCurrentStatus() != RoomStatus.RESERVED
                && room.getCurrentStatus() != RoomStatus.VACANT
                && !soonVacantDraft) {
            throw new AppException(ApiErrorCode.LEASE_ROOM_STATUS_INVALID_FOR_CREATION);
        }
        if (soonVacantDraft) {
            validateSoonVacantMoveInDate(room.getId(), startDate);
        } else if (leaseContractRepository.existsByRoom_IdAndStatusInAndDeletedAtIsNull(room.getId(), BLOCKING_ACTIVE_CONTRACT_STATUSES)) {
            throw new AppException(ApiErrorCode.LEASE_ROOM_ACTIVE_CONTRACT_EXISTS);
        }
        assertRoomHasNoPendingContract(room);
        Integer maxOccupants = room.getMaxOccupants() != null ? room.getMaxOccupants() : 3;
        if (occupantsCount > maxOccupants) {
            throw new AppException(ApiErrorCode.LEASE_OCCUPANCY_LIMIT_EXCEEDED);
        }
    }

    void assertRoomHasNoPendingContract(RoomEntity room) {
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
                    throw new AppException(
                            ApiErrorCode.PENDING_CONTRACT_EXISTS,
                            roomCode,
                            contractCode,
                            contract.getStatus()
                    );
                });
    }

    private void validateSoonVacantMoveInDate(Long roomId, LocalDate expectedMoveInDate) {
        LocalDate expectedVacantDate = roomCommitmentChecker.findExpectedVacantDateForBooking(roomId)
                .orElseThrow(() -> new AppException(ApiErrorCode.LEASE_EXPECTED_VACANCY_DATE_REQUIRED));
        if (expectedMoveInDate.isBefore(expectedVacantDate)) {
            throw new AppException(ApiErrorCode.LEASE_EXPECTED_MOVE_IN_DATE_INVALID);
        }
    }

    private void validateContractTerms(
            LocalDate startDate,
            Integer paymentCycleMonths,
            Long monthlyRent,
            Long depositAmount
    ) {
        if (startDate == null) {
            throw new AppException(ApiErrorCode.LEASE_START_DATE_REQUIRED);
        }
        if (!Objects.equals(paymentCycleMonths, 1) && !Objects.equals(paymentCycleMonths, 3)) {
            throw new AppException(ApiErrorCode.LEASE_PAYMENT_CYCLE_INVALID);
        }
        if (monthlyRent == null || monthlyRent <= 0) {
            throw new AppException(ApiErrorCode.LEASE_RENT_AMOUNT_INVALID);
        }
        if (depositAmount == null || depositAmount < 0) {
            throw new AppException(ApiErrorCode.DEPOSIT_AMOUNT_MUST_GREATER_THAN_ZERO);
        }
    }

    private LocalDate resolveRentStartDate(LocalDate startDate) {
        if (startDate.getDayOfMonth() <= 10) {
            return startDate;
        }
        return startDate.plusMonths(1).withDayOfMonth(1);
    }

    private void ensureContractOccupants(LeaseContractEntity contract) {
        if (contract == null || contract.getPrimaryTenantProfile() == null || contract.getRoom() == null) {
            throw new AppException(ApiErrorCode.LEASE_OCCUPANTS_INCOMPLETE);
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

    private List<LeaseContractRenewalResponse.OccupantInfo> findRenewalOccupants(Long contractId) {
        return jdbcTemplate.query("""
                        SELECT
                            co.tenant_profile_id,
                            pp.full_name,
                            pp.phone,
                            co.occupant_role
                        FROM contract_occupants co
                        LEFT JOIN person_profiles pp ON pp.person_profile_id = co.tenant_profile_id
                        WHERE co.contract_id = ?
                          AND co.status = 'ACTIVE'
                        ORDER BY CASE WHEN co.occupant_role = 'PRIMARY' THEN 0 ELSE 1 END, co.contract_occupant_id
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

    private String resolveRenewalContractCode(
            LeaseContractEntity oldContract,
            String requestedContractCode,
            LocalDate newStartDate
    ) {
        String contractCode = requestedContractCode == null ? "" : requestedContractCode.trim();
        if (contractCode.isBlank()) {
            contractCode = generateRenewalContractCode(oldContract, newStartDate);
        }
        if (contractCode.length() > 80) {
            throw new AppException(ApiErrorCode.NEW_CONTRACT_CODE_OVERFLOWS);
        }
        if (leaseContractRepository.existsByContractCodeAndDeletedAtIsNull(contractCode)) {
            throw new AppException(ApiErrorCode.NEW_CONTRACT_CODE_EXISTED);
        }
        return contractCode;
    }

    private String generateRenewalContractCode(LeaseContractEntity oldContract, LocalDate newStartDate) {
        LeaseContractEntity rootContract = oldContract;
        int renewalNumber = 1;
        while (rootContract.getPreviousContract() != null) {
            rootContract = rootContract.getPreviousContract();
            renewalNumber++;
        }

        String roomCode = rootContract.getRoom() == null ? null : rootContract.getRoom().getRoomCode();
        String baseCode = DocumentFilenameBuilder.buildLeaseContractCode(roomCode, newStartDate);
        String contractCode = baseCode;
        while (leaseContractRepository.existsByContractCodeAndDeletedAtIsNull(contractCode)) {
            contractCode = baseCode + "-R" + renewalNumber++;
        }
        return contractCode;
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

    private boolean hasOtherActiveContract(Long roomId, Long leaseContractId) {
        return hasOtherActiveContract(roomId, leaseContractId, null);
    }

    private boolean hasOtherActiveContract(Long roomId, Long leaseContractId, Long allowedPreviousContractId) {
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

    private void ensureLiquidationAllowedForRoomCommitment(LeaseContractEntity contract) {
        if (contract.getRoom() != null
                && roomCommitmentChecker.isSoonVacantBookingCase(
                contract.getRoom().getId(),
                contract.getId(),
                contract.getEndDate()
        )) {
            throw new AppException(ApiErrorCode.ROOM_LIQUIDATION_BLOCKED_BY_BOOKING);
        }
    }

    private String normalizeTenantIntention(String intention) {
        String normalized = intention == null ? "" : intention.trim().toUpperCase();
        return "TRANSFER_ROOM".equals(normalized) ? "TRANSFER" : normalized;
    }

    private boolean isWithinThreeMonths(LeaseContractEntity contract, LocalDate today) {
        return contract.getEndDate() != null && !today.isBefore(contract.getEndDate().minusMonths(3));
    }

    private void throwRenewBlocked(RoomCommitmentChecker.Blocker blocker) {
        if (blocker == RoomCommitmentChecker.Blocker.ROOM_ALREADY_RESERVED_BY_NEW_TENANT) {
            throw new AppException(ApiErrorCode.LEASE_ROOM_PREBOOKED_BY_OTHER_TENANT);
        }
        throw new AppException(ApiErrorCode.LEASE_RENEWAL_ROOM_RESERVED_BY_OTHER_TENANT);
    }

    private void assertOwnerOrManagerCanRenew() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean canRenew = authentication != null
                && authentication.getAuthorities() != null
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_OWNER".equals(authority.getAuthority())
                        || "ROLE_MANAGER".equals(authority.getAuthority()));
        if (!canRenew) {
            throw new AppException(ApiErrorCode.LEASE_RENEWAL_CONFIRMATION_FORBIDDEN);
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

    private LeaseContractManagementResponse toResponse(ResultSet rs) throws SQLException {
        Long leaseContractId = getLongOrNull(rs, "lease_contract_id");
        Long depositFormId = getLongOrNull(rs, "deposit_form_id");
        boolean depositRow = "DEPOSIT".equals(rs.getString("source_type"));
        String contractStatus = rs.getString("contract_status");
        Long contractFileId = getLongOrNull(rs, "contract_file_id");
        Long signedFileId = getLongOrNull(rs, "signed_file_id");
        Long userId = getLongOrNull(rs, "user_id");
        String contractCode = rs.getString("contract_code");
        String depositCode = rs.getString("deposit_code");
        String code = contractCode != null ? contractCode : depositCode;
        Long roomId = getLongOrNull(rs, "room_id");
        Long renewedContractId = getLongOrNull(rs, "renewed_contract_id");
        Long transferRequestId = getLongOrNull(rs, "transfer_request_id");
        String transferStatus = rs.getString("transfer_status");
        String transferContractRole = rs.getString("transfer_contract_role");
        boolean sourceTransferCompleted = rs.getBoolean("source_transfer_completed");
        String effectiveContractStatus = sourceTransferCompleted
                ? LeaseStatus.TRANSFERRED.name()
                : contractStatus;
        LeaseStatus parsedContractStatus = parseEnum(LeaseStatus.class, effectiveContractStatus);
        RoomCommitmentChecker.Blocker renewBlocker =
                resolveRenewBlocker(
                        roomId,
                        leaseContractId,
                        renewedContractId,
                        parsedContractStatus,
                        toLocalDate(rs, "end_date")
                );
        long outstandingDebt = leaseContractId == null
                ? 0L
                : LeaseContractDebtPolicy.outstandingAmount(jdbcTemplate, leaseContractId);
        String debtBlockedReason = LeaseContractDebtPolicy.blockingReason(outstandingDebt);
        boolean liquidationBlockedByBooking = leaseContractId != null
                && roomCommitmentChecker.isSoonVacantBookingCase(
                roomId,
                leaseContractId,
                toLocalDate(rs, "end_date")
        );
        Long liquidationFinalInvoiceId = getLongOrNull(rs, "liquidation_final_invoice_id");
        LiquidationInvoiceSummary liquidationInvoiceSummary = liquidationInvoiceSummary(liquidationFinalInvoiceId);
        ExpenseRequestService.LiquidationDepositRefundLink refundLink = depositRow
                ? ExpenseRequestService.LiquidationDepositRefundLink.empty()
                : expenseRequestService.getLiquidationDepositRefundLink(leaseContractId);
        ExpenseRequestService.LiquidationDepositForfeitureLink forfeitureLink = depositRow
                ? ExpenseRequestService.LiquidationDepositForfeitureLink.empty()
                : expenseRequestService.getLiquidationDepositForfeitureLink(leaseContractId);
        return LeaseContractManagementResponse.builder()
                .sourceType(rs.getString("source_type"))
                .leaseContractId(leaseContractId)
                .depositFormId(depositFormId)
                .code(code)
                .contractCode(contractCode)
                .depositCode(depositCode)
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
                .activationElectricityValue(getBigDecimalOrNull(rs, "activation_electricity_value"))
                .activationReadingDate(toLocalDate(rs, "activation_reading_date"))
                .monthlyRent(getLongOrNull(rs, "monthly_rent"))
                .paymentCycleMonths(getIntOrNull(rs, "payment_cycle_months"))
                .depositAmount(getLongOrNull(rs, "deposit_amount"))
                .contractTermMonths(getIntOrNull(rs, "contract_term_months"))
                .occupantsCount(getIntOrNull(rs, "occupants_count"))
                .previousContractId(getLongOrNull(rs, "previous_contract_id"))
                .previousContractCode(rs.getString("previous_contract_code"))
                .renewedContractId(renewedContractId)
                .renewedContractCode(rs.getString("renewed_contract_code"))
                .tenantIntention(rs.getString("tenant_intention"))
                .expectedVacantDate(toLocalDate(rs, "expected_vacant_date"))
                .canRenew(canRenewFromBlocker(leaseContractId, renewedContractId, parsedContractStatus, renewBlocker)
                        && outstandingDebt == 0)
                .canRenewBlockedReason(debtBlockedReason != null
                        ? debtBlockedReason
                        : renewBlocker == RoomCommitmentChecker.Blocker.NONE
                        ? null
                        : renewBlockedReason(renewBlocker))
                .canLiquidate(leaseContractId != null
                        && isLiquidatableContractStatus(parsedContractStatus)
                        && !liquidationBlockedByBooking
                        && outstandingDebt == 0)
                .canLiquidateBlockedReason(debtBlockedReason != null
                        ? debtBlockedReason
                        : liquidationBlockedByBooking
                        ? ApiErrorCode.ROOM_LIQUIDATION_BLOCKED_BY_BOOKING.getDetails()
                        : null)
                .transferRequestId(transferRequestId)
                .transferRequestCode(rs.getString("transfer_request_code"))
                .transferStatus(transferStatus)
                .transferRequestedDate(toLocalDate(rs, "transfer_requested_date"))
                .transferContractRole(sourceTransferCompleted && transferContractRole == null
                        ? "OLD_CONTRACT"
                        : transferContractRole)
                .transferActivationLocked(isTransferActivationLocked(
                        transferRequestId,
                        transferStatus,
                        getLongOrNull(rs, "previous_contract_id")
                ))
                .contractStatus(parsedContractStatus)
                .workflowStatus(resolveWorkflow(effectiveContractStatus, signedFileId != null ? signedFileId : contractFileId))
                .depositStatus(rs.getString("deposit_status"))
                .contractFileId(contractFileId)
                .contractFileName(rs.getString("contract_file_name"))
                .contractFileUploadedAt(toLocalDateTime(rs, "contract_file_uploaded_at"))
                .signedFileId(signedFileId)
                .signedFileName(rs.getString("signed_file_name"))
                .signedFileUploadedAt(toLocalDateTime(rs, "signed_file_uploaded_at"))
                .signedUploadedById(getLongOrNull(rs, "signed_uploaded_by"))
                .handoverSignedFileId(getLongOrNull(rs, "handover_signed_file_id"))
                .moveOutHandoverRecordId(getLongOrNull(rs, "move_out_handover_record_id"))
                .moveOutHandoverStatus(parseEnum(HandoverStatus.class, rs.getString("move_out_handover_status")))
                .moveOutHandoverDate(toLocalDateTime(rs, "move_out_handover_date"))
                .moveOutHandoverElectricityReadingId(getLongOrNull(rs, "move_out_handover_electricity_reading_id"))
                .moveOutHandoverSignedFileId(getLongOrNull(rs, "move_out_handover_signed_file_id"))
                .signedAt(toLocalDateTime(rs, "signed_at"))
                .createdAt(toLocalDateTime(rs, "created_at"))
                .liquidationId(getLongOrNull(rs, "liquidation_id"))
                .liquidationDate(toLocalDate(rs, "liquidation_date"))
                .liquidationReason(rs.getString("liquidation_reason"))
                .liquidationDepositAmount(getLongOrNull(rs, "liquidation_deposit_amount"))
                .liquidationDepositDeductionAmount(getLongOrNull(rs, "liquidation_deposit_deduction_amount"))
                .liquidationDepositDeductionReason(rs.getString("liquidation_deposit_deduction_reason"))
                .liquidationDepositRefundAmount(getLongOrNull(rs, "liquidation_deposit_refund_amount"))
                .liquidationFinalInvoiceId(liquidationFinalInvoiceId)
                .liquidationFinalInvoiceCode(liquidationInvoiceSummary.invoiceCode())
                .liquidationFinalInvoiceStatus(liquidationInvoiceSummary.status())
                .liquidationFinalInvoiceSubtotalAmount(liquidationInvoiceSummary.subtotalAmount())
                .liquidationFinalInvoiceDiscountAmount(liquidationInvoiceSummary.discountAmount())
                .liquidationFinalInvoiceTotalAmount(liquidationInvoiceSummary.totalAmount())
                .liquidationFinalInvoiceRemainingAmount(liquidationInvoiceSummary.remainingAmount())
                .liquidationFinalInvoiceLines(liquidationInvoiceLines(liquidationFinalInvoiceId))
                .liquidationSignedFileId(getLongOrNull(rs, "liquidation_signed_file_id"))
                .liquidationStatus(parseEnum(LiquidationStatus.class, rs.getString("liquidation_status")))
                .liquidationCreatedAt(toLocalDateTime(rs, "liquidation_created_at"))
                .liquidationDepositRefundRequestId(refundLink.liquidationChangeRequestId())
                .liquidationDepositRefundExpenseId(refundLink.expenseId())
                .liquidationDepositRefundExpenseRequestId(refundLink.expenseRequestId())
                .liquidationDepositRefundStatus(refundLink.status())
                .liquidationDepositRefundProofFileId(refundLink.proofFileId())
                .liquidationDepositRefundedAmount(refundLink.refundedAmount())
                .liquidationDepositRefundedAt(refundLink.refundedAt())
                .liquidationDepositRefundTransactionRef(refundLink.transactionRef())
                .liquidationDepositForfeitureRequestId(forfeitureLink.liquidationChangeRequestId())
                .liquidationDepositForfeitureStatus(forfeitureLink.status())
                .liquidationDepositForfeitureConfirmedBy(forfeitureLink.confirmedBy())
                .liquidationDepositForfeitureConfirmedAt(forfeitureLink.confirmedAt())
                .accountProvisioned(userId != null)
                .emailAvailable(rs.getString("email") != null && !rs.getString("email").isBlank())
                .build();
    }

    private RoomCommitmentChecker.Blocker resolveRenewBlocker(
            Long roomId,
            Long leaseContractId,
            Long renewedContractId,
            LeaseStatus contractStatus,
            LocalDate contractEndDate
    ) {
        if (roomId == null
                || leaseContractId == null
                || renewedContractId != null
                || !isRenewableContractStatus(contractStatus)) {
            return RoomCommitmentChecker.Blocker.NONE;
        }
        return roomCommitmentChecker.checkRenewBlockers(roomId, leaseContractId, contractEndDate);
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

    private boolean isLiquidatableContractStatus(LeaseStatus contractStatus) {
        return contractStatus == LeaseStatus.ACTIVE
                || contractStatus == LeaseStatus.EXPIRING_SOON
                || contractStatus == LeaseStatus.EXPIRED
                || contractStatus == LeaseStatus.TERMINATION_PENDING;
    }

    private String renewBlockedReason(RoomCommitmentChecker.Blocker blocker) {
        if (blocker == RoomCommitmentChecker.Blocker.ROOM_ALREADY_RESERVED_BY_NEW_TENANT) {
            return "Phòng đang được giữ chỗ cho khách khác.";
        }
        return "Phòng đã có khách khác đặt cọc/giữ chỗ, không thể tái ký.";
    }

    private void ensureNotRoomTransferManagedContract(Long leaseContractId) {
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
            throw new AppException(ApiErrorCode.LEASE_TRANSFER_WORKFLOW_REQUIRED);
        }
    }

    private boolean isTransferActivationLocked(
            Long transferRequestId,
            String transferStatus,
            Long previousContractId
    ) {
        if (transferRequestId == null) {
            return false;
        }
        if (previousContractId != null) {
            return false;
        }
        return !"EXECUTED".equals(transferStatus);
    }

    private LiquidationInvoiceSummary liquidationInvoiceSummary(Long invoiceId) {
        if (invoiceId == null) {
            return LiquidationInvoiceSummary.empty();
        }
        return invoiceRepository.findById(invoiceId)
                .map(invoice -> new LiquidationInvoiceSummary(
                        invoice.getInvoiceCode(),
                        invoice.getStatus() == null ? null : invoice.getStatus().name(),
                        invoice.getSubtotalAmount(),
                        invoice.getDiscountAmount(),
                        invoice.getTotalAmount(),
                        invoice.getRemainingAmount()
                ))
                .orElseGet(LiquidationInvoiceSummary::empty);
    }

    private List<BillingInvoiceLineResponse> liquidationInvoiceLines(Long invoiceId) {
        if (invoiceId == null) {
            return List.of();
        }
        return invoiceLineRepository.findByInvoice_IdOrderByIdAsc(invoiceId).stream()
                .map(line -> new BillingInvoiceLineResponse(
                        line.getId(),
                        line.getLineType() == null ? null : line.getLineType().name(),
                        line.getDescription(),
                        line.getQuantity(),
                        line.getUnitPrice(),
                        safe(line.getUnitPrice()) * safeInt(line.getQuantity()),
                        line.getMeterReading() == null ? null : line.getMeterReading().getId(),
                        line.getMeterReading() == null || line.getMeterReading().getPhotoFile() == null
                                ? null
                                : line.getMeterReading().getPhotoFile().getId(),
                        line.getMeterReading() == null || line.getMeterReading().getPreviousValue() == null
                                ? null
                                : line.getMeterReading().getPreviousValue().stripTrailingZeros().toPlainString(),
                        line.getMeterReading() == null || line.getMeterReading().getCurrentValue() == null
                                ? null
                                : line.getMeterReading().getCurrentValue().stripTrailingZeros().toPlainString()
                ))
                .toList();
    }

    private String resolveWorkflow(String contractStatus, Long contractFileId) {
        if (contractStatus != null && List.of(
                "ACTIVE",
                "EXPIRING_SOON",
                "EXPIRED",
                "TERMINATION_PENDING",
                "LIQUIDATED",
                "RENEWED",
                "TRANSFERRED",
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

    private BigDecimal getBigDecimalOrNull(ResultSet rs, String column) throws SQLException {
        return rs.getBigDecimal(column);
    }

    public record LiquidationChargeInput(
            InvoiceLineType lineType,
            String description,
            Integer quantity,
            Long unitPrice,
            BigDecimal previousValue,
            BigDecimal currentValue,
            Long photoFileId
    ) {
    }

    private record LiquidationDepositSettlement(
            long depositAmount,
            long deductionAmount,
            String deductionReason
    ) {
        long refundAmount() {
            return Math.max(0L, depositAmount - deductionAmount);
        }
    }

    private record LiquidationInvoiceSummary(
            String invoiceCode,
            String status,
            Long subtotalAmount,
            Long discountAmount,
            Long totalAmount,
            Long remainingAmount
    ) {
        static LiquidationInvoiceSummary empty() {
            return new LiquidationInvoiceSummary(null, null, null, null, null, null);
        }
    }
}
