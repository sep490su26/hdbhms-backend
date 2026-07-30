package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.billingandpayment.domain.value_objects.DepositAgreementStatus;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceLineType;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceReason;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceStatus;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceType;
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
import com.sep490.hdbhms.occupancy.domain.value_objects.MeterStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.MeterType;
import com.sep490.hdbhms.occupancy.domain.value_objects.OccupantRole;
import com.sep490.hdbhms.occupancy.domain.value_objects.OccupantStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.ReadingPurpose;
import com.sep490.hdbhms.occupancy.domain.value_objects.ReadingStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.RoomStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.ContractLiquidationEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.ContractHandoverRecordEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.ContractOccupantEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.DepositFormCoOccupantEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.DepositAgreementEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeaseContractEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.MeterEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.MeterReadingEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RoomEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaContractLiquidationRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaContractHandoverRecordRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaContractOccupantRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaDepositAgreementRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaLeaseContractRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaMeterReadingRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaMeterRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomRepository;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.LeaseContractManagementResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.LeaseContractRenewalResponse;
import com.sep490.hdbhms.shared.dto.response.PageResponse;
import com.sep490.hdbhms.shared.utils.AuthUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
    JpaDepositAgreementRepository depositAgreementRepository;
    JpaContractLiquidationRepository contractLiquidationRepository;
    JpaContractHandoverRecordRepository handoverRecordRepository;
    JpaInvoiceRepository invoiceRepository;
    JpaInvoiceLineRepository invoiceLineRepository;
    JpaMeterRepository meterRepository;
    JpaMeterReadingRepository meterReadingRepository;
    RoomCommitmentChecker roomCommitmentChecker;
    LeaseExpiryReminderService leaseExpiryReminderService;
    ExpenseRequestService expenseRequestService;

    @Transactional(readOnly = true)
    public List<LeaseContractManagementResponse> findAllForManagement() {
        List<LeaseContractManagementResponse> rows = new ArrayList<>();
        rows.addAll(jdbcTemplate.query("""
                SELECT
                    'DEPOSIT' AS source_type,
                    lc.lease_contract_id AS lease_contract_id,
                    da.deposit_agreement_id AS deposit_agreement_id,
                    da.deposit_code,
                    lc.contract_code,
                    p.property_id AS property_id,
                    p.name AS property_name,
                    p.address_line AS property_address,
                    COALESCE((
                        SELECT co.tenant_id
                        FROM contract_occupants co
                        WHERE co.contract_id = lc.lease_contract_id
                          AND co.status = 'ACTIVE'
                        ORDER BY CASE WHEN co.occupant_role = 'PRIMARY' THEN 0 ELSE 1 END, co.contract_occupant_id ASC
                        LIMIT 1
                    ), da.tenant_id) AS tenant_id,
                    r.room_id AS room_id,
                    r.room_code,
                    r.current_status AS room_status,
                    pp.person_profile_id AS primary_tenant_profile_id,
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
                    GREATEST(
                        CASE
                            WHEN lc.lease_contract_id IS NOT NULL THEN (
                                SELECT COUNT(*)
                                FROM contract_occupants co
                                WHERE co.contract_id = lc.lease_contract_id
                                  AND co.status = 'ACTIVE'
                            )
                            ELSE 0
                        END,
                        COALESCE(df.occupant_count, 1),
                        1 + COALESCE((
                            SELECT COUNT(*)
                            FROM deposit_form_co_occupants dco_count
                            WHERE dco_count.deposit_form_id = df.deposit_form_id
                        ), 0)
                    ) AS occupants_count,
                    lc.status AS contract_status,
                    da.status AS deposit_status,
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
                    COALESCE(lc.created_at, da.created_at) AS created_at,
                    u.user_id AS user_id,
                    u.last_login_at
                FROM deposit_agreements da
                JOIN rooms r ON r.room_id = da.room_id
                JOIN properties p ON p.property_id = r.property_id
                LEFT JOIN deposit_forms df ON df.deposit_form_id = da.deposit_form_id
                LEFT JOIN person_profiles pp ON pp.person_profile_id = da.depositor_person_profile_id
                LEFT JOIN lease_contracts lc ON lc.deposit_agreement_id = da.deposit_agreement_id AND lc.deleted_at IS NULL
                LEFT JOIN lease_contracts previous_contract ON previous_contract.lease_contract_id = lc.previous_contract_id
                LEFT JOIN contract_liquidations cl ON cl.contract_id = lc.lease_contract_id
                LEFT JOIN file_metadata fm ON fm.file_metadata_id = lc.contract_file_id
                LEFT JOIN file_metadata sfm ON sfm.file_metadata_id = lc.signed_file_id
                LEFT JOIN room_transfer_requests tr
                  ON lc.lease_contract_id IS NOT NULL
                 AND (tr.new_contract_id = lc.lease_contract_id OR tr.replacement_old_contract_id = lc.lease_contract_id)
                LEFT JOIN users u ON u.user_id = pp.user_id AND u.deleted_at IS NULL
                WHERE da.status IN ('PAID', 'CONFIRMED', 'CONVERTED_TO_LEASE')
                ORDER BY COALESCE(lc.updated_at, da.updated_at) DESC, da.deposit_agreement_id DESC
                """, (rs, rowNum) -> toResponse(rs)));
        rows.addAll(jdbcTemplate.query("""
                SELECT
                    'CONTRACT' AS source_type,
                    lc.lease_contract_id AS lease_contract_id,
                    NULL AS deposit_agreement_id,
                    NULL AS deposit_code,
                    lc.contract_code,
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
                    lc.monthly_rent,
                    lc.payment_cycle_months,
                    lc.deposit_amount,
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
                    GREATEST(
                        (
                            SELECT COUNT(*)
                            FROM contract_occupants co
                            WHERE co.contract_id = lc.lease_contract_id
                              AND co.status = 'ACTIVE'
                        ),
                        COALESCE(df.occupant_count, 1),
                        1 + COALESCE((
                            SELECT COUNT(*)
                            FROM deposit_form_co_occupants dco_count
                            WHERE dco_count.deposit_form_id = df.deposit_form_id
                        ), 0)
                    ) AS occupants_count,
                    lc.status AS contract_status,
                    NULL AS deposit_status,
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
                    lc.created_at,
                    u.user_id AS user_id,
                    u.last_login_at
                FROM lease_contracts lc
                JOIN rooms r ON r.room_id = lc.room_id
                JOIN properties p ON p.property_id = r.property_id
                JOIN person_profiles pp ON pp.person_profile_id = lc.primary_tenant_profile_id
                LEFT JOIN deposit_agreements da ON da.deposit_agreement_id = lc.deposit_agreement_id
                LEFT JOIN deposit_forms df ON df.deposit_form_id = da.deposit_form_id
                LEFT JOIN lease_contracts previous_contract ON previous_contract.lease_contract_id = lc.previous_contract_id
                LEFT JOIN contract_liquidations cl ON cl.contract_id = lc.lease_contract_id
                LEFT JOIN file_metadata fm ON fm.file_metadata_id = lc.contract_file_id
                LEFT JOIN room_transfer_requests tr
                  ON tr.new_contract_id = lc.lease_contract_id OR tr.replacement_old_contract_id = lc.lease_contract_id
                LEFT JOIN file_metadata sfm ON sfm.file_metadata_id = lc.signed_file_id
                LEFT JOIN users u ON u.user_id = pp.user_id AND u.deleted_at IS NULL
                WHERE lc.deleted_at IS NULL
                  AND lc.deposit_agreement_id IS NULL
                ORDER BY lc.updated_at DESC, lc.lease_contract_id DESC
                """, (rs, rowNum) -> toResponse(rs)));
        return rows;
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
        return uploadSignedFile(leaseContractId, file, false);
    }

    public LeaseContractManagementResponse uploadSignedFile(Long leaseContractId, MultipartFile file, boolean replace) {
        LeaseContractEntity contract = leaseContractRepository.findById(leaseContractId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay hop dong thue."));
        if (contract.getStatus() == LeaseStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hop dong da ACTIVE, khong upload thay file trong luong nay.");
        }
        if (contract.getSignedFile() != null && !replace) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Hop dong thue da co file da ky. Gui replace=true neu muon thay the.");
        }
        Long currentUserId = AuthUtils.getCurrentAuthenticationId();
        var metadata = uploadFileService.execute(new UploadFileCommand(
                currentUserId,
                file,
                FileCategory.CONTRACT,
                true
        ));
        var signedFile = fileMetadataRepository.findById(metadata.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Khong luu duoc file hop dong."));
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
        Long depositAmount = resolveLiquidationDepositAmount(contract);
        ContractLiquidationEntity liquidation = contractLiquidationRepository.findByContract_Id(contract.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Vui long lap ho so thanh ly truoc khi hoan tat."
                ));
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
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Vui long lap hop dong thay the cho nguoi o lai truoc khi hoan tat thanh ly."
                ))
                : null;

        requireNoUnpaidInvoicesForLiquidation(contract.getId());
        if (!holderReplacement) {
            requireConfirmedMoveOutHandover(contract.getId());
            requireLiquidationDepositRefundConfirmed(contract, liquidation);
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
            appendRoomStatusHistory(room.getId(), fromStatus, RoomStatus.VACANT, "Thanh ly hop dong thue " + contract.getContractCode());
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hợp đồng thuê."));
        if (contract.getDeletedAt() != null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hợp đồng thuê.");
        }
        if (contract.getStatus() == LeaseStatus.LIQUIDATED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hợp đồng đã được thanh lý.");
        }
        if (contract.getStatus() != LeaseStatus.ACTIVE
                && contract.getStatus() != LeaseStatus.EXPIRING_SOON
                && contract.getStatus() != LeaseStatus.EXPIRED
                && contract.getStatus() != LeaseStatus.TERMINATION_PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Chỉ xử lý thanh lý cho hợp đồng đang hiệu lực, sắp hết hạn, hết hạn hoặc chờ thanh lý."
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hồ sơ thanh lý đã được xác nhận.");
        }
        liquidation.setLiquidationDate(finalLiquidationDate);
        liquidation.setReason(finalReason);
        liquidation.setDepositAmount(depositAmount);
        liquidation.setDepositDeductionAmount(0L);
        liquidation.setDepositDeductionReason(null);
        liquidation.setDepositRefundAmount(calculateLiquidationDepositRefund(
                contract,
                finalLiquidationDate,
                depositAmount,
                holderReplacement ? depositAmount : 0L
        ));
        liquidation.setStatus(LiquidationStatus.DRAFT);
        contractLiquidationRepository.save(liquidation);

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
                        "Nguoi o cung tiep tuc thue sau thanh ly hop dong " + contract.getContractCode()
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
        leaseExpiryReminderService.onTenantIntentionRecorded(contract, LocalDate.now());
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contract primary tenant is required.");
        }
        if (replacementPrimaryTenantProfileId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Replacement primary tenant is required.");
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
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Contract has no active occupants.");
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Holder replacement payload is required.");
        }
        RoomEntity room = oldContract.getRoom();
        Optional<LeaseContractEntity> existingReplacement = latestReplacementContract(oldContract.getId());
        Long allowedExistingContractId = existingReplacement.map(LeaseContractEntity::getId).orElse(null);
        if (hasOtherActiveContract(room.getId(), oldContract.getId(), allowedExistingContractId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Room already has another active contract.");
        }
        Long carriedDepositAmount = resolveLiquidationDepositAmount(oldContract);
        validateContractTerms(
                effectiveDate,
                oldContract.getEndDate(),
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
        String contractCode = generateHolderReplacementContractCode(oldContract);
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
                "Holder replacement liquidation from contract " + oldContract.getContractCode()
        );
        return replacement;
    }

    private void assertHolderReplacementContractMatches(
            LeaseContractEntity oldContract,
            LeaseContractEntity replacement,
            HolderReplacementPlan plan
    ) {
        if (replacement.getStatus() == LeaseStatus.CANCELLED || replacement.getStatus() == LeaseStatus.LIQUIDATED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Replacement contract is not usable.");
        }
        if (replacement.getRoom() == null || !Objects.equals(replacement.getRoom().getId(), oldContract.getRoom().getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Replacement contract must stay in the same room.");
        }
        Long currentPrimaryId = replacement.getPrimaryTenantProfile() == null
                ? null
                : replacement.getPrimaryTenantProfile().getId();
        if (!Objects.equals(currentPrimaryId, plan.replacementPrimaryTenantProfileId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Replacement contract primary tenant does not match request.");
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
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Replacement contract must be signed before liquidation.");
        }
        if (!List.of(
                LeaseStatus.DRAFT,
                LeaseStatus.PENDING_SIGNATURE,
                LeaseStatus.CONFIRMED,
                LeaseStatus.SIGNED,
                LeaseStatus.ACTIVE
        ).contains(replacementContract.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Replacement contract status is not valid for activation.");
        }
        if (replacementContract.getStatus() != LeaseStatus.ACTIVE) {
            replacementContract.setStartDate(effectiveDate);
            replacementContract.setRentStartDate(effectiveDate);
            replacementContract.setStatus(LeaseStatus.ACTIVE);
            if (replacementContract.getSignedAt() == null) {
                replacementContract.setSignedAt(LocalDateTime.now());
            }
            replacementContract = leaseContractRepository.saveAndFlush(replacementContract);
            appendContractEvent(replacementContract.getId(), "SIGNED", "Activate holder replacement contract.");
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
                    "Kich hoat hop dong thay the " + replacementContract.getContractCode()
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

    private String generateHolderReplacementContractCode(LeaseContractEntity oldContract) {
        int revision = 1;
        String contractCode;
        do {
            String suffix = "-LIQ-" + oldContract.getId() + (revision == 1 ? "" : "-" + revision);
            String prefix = oldContract.getContractCode() == null
                    ? "HD"
                    : oldContract.getContractCode();
            int maxPrefixLength = Math.max(1, 80 - suffix.length());
            if (prefix.length() > maxPrefixLength) {
                prefix = prefix.substring(0, maxPrefixLength);
            }
            contractCode = prefix + suffix;
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Leaving and staying occupants are required.");
        }
        if (!leavingIds.contains(currentPrimaryTenantProfileId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current primary tenant must be in leavingProfileIds.");
        }
        if (!stayingIds.contains(replacementPrimaryTenantProfileId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Replacement primary tenant must be in stayingProfileIds.");
        }

        Set<Long> overlap = new HashSet<>(leavingIds);
        overlap.retainAll(stayingIds);
        if (!overlap.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Leaving and staying occupants must not overlap.");
        }

        Set<Long> classifiedIds = new LinkedHashSet<>(leavingIds);
        classifiedIds.addAll(stayingIds);
        if (activeProfileIds == null || !activeProfileIds.equals(classifiedIds)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "All active occupants must be classified as leaving or staying.");
        }
        if (!activeProfileIds.contains(replacementPrimaryTenantProfileId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Replacement primary tenant must be an active occupant.");
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Occupant is not active in contract."));
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "KhÃ´ng tÃ¬m tháº¥y há»£p Ä‘á»“ng thuÃª."));
        if (contract.getDeletedAt() != null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "KhÃ´ng tÃ¬m tháº¥y há»£p Ä‘á»“ng thuÃª.");
        }
        if (contract.getStatus() == LeaseStatus.LIQUIDATED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Há»£p Ä‘á»“ng Ä‘Ã£ Ä‘Æ°á»£c thanh lÃ½.");
        }
        if (contract.getStatus() != LeaseStatus.TERMINATION_PENDING
                && contract.getStatus() != LeaseStatus.ACTIVE
                && contract.getStatus() != LeaseStatus.EXPIRING_SOON
                && contract.getStatus() != LeaseStatus.EXPIRED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Há»£p Ä‘á»“ng chÆ°a thá»ƒ láº­p há»“ sÆ¡ thanh lÃ½.");
        }

        Long depositAmount = resolveLiquidationDepositAmount(contract);
        ContractLiquidationEntity liquidation = contractLiquidationRepository.findByContract_Id(contract.getId())
                .orElseGet(() -> ContractLiquidationEntity.builder()
                        .contract(contract)
                        .depositAmount(depositAmount)
                        .status(LiquidationStatus.DRAFT)
                        .build());
        if (liquidation.getStatus() == LiquidationStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Há»“ sÆ¡ thanh lÃ½ Ä‘Ã£ Ä‘Æ°á»£c xÃ¡c nháº­n.");
        }

        LocalDate finalLiquidationDate = liquidationDate != null
                ? liquidationDate
                : liquidation.getLiquidationDate() != null ? liquidation.getLiquidationDate() : LocalDate.now();
        String finalReason = reason == null || reason.isBlank()
                ? liquidation.getReason() != null && !liquidation.getReason().isBlank()
                ? liquidation.getReason().trim()
                : "KhÃ¡ch khÃ´ng tiáº¿p tá»¥c thuÃª phÃ²ng."
                : reason.trim();
        boolean holderReplacement = isHolderReplacementLiquidation(contract.getId());
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
        InvoiceEntity finalInvoice = upsertFinalSettlementInvoice(contract, liquidation, charges, true);
        liquidation.setFinalInvoice(finalInvoice);
        contractLiquidationRepository.save(liquidation);
        if (!holderReplacement) {
            ensureLiquidationDepositRefundRequest(contract, liquidation);
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
        liquidation.setDepositDeductionAmount(0L);
        liquidation.setDepositDeductionReason(null);
        liquidation.setDepositRefundAmount(calculateLiquidationDepositRefund(
                contract,
                liquidationDate,
                depositAmount,
                depositCarriedForward ? safe(depositAmount) : 0L
        ));
    }

    private InvoiceEntity upsertFinalSettlementInvoice(
            LeaseContractEntity contract,
            ContractLiquidationEntity liquidation,
            List<LiquidationChargeInput> charges,
            boolean issue
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
        liquidation.setDepositDeductionAmount(0L);
        liquidation.setDepositDeductionReason(null);
        liquidation.setDepositRefundAmount(calculateLiquidationDepositRefund(
                contract,
                liquidation.getLiquidationDate(),
                depositAmount,
                0L
        ));

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
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Khong the cap nhat hoa don thanh ly da co thanh toan."
                );
            }
            invoice.setStatus(InvoiceStatus.VOIDED);
            invoice.setVoidedAt(now);
            invoice.setVoidReason("Thay the bang hoa don thanh ly moi sau khi cap nhat ho so.");
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
                    .status(issue ? InvoiceStatus.ISSUED : InvoiceStatus.DRAFT)
                    .subtotalAmount(subtotal)
                    .discountAmount(0L)
                    .totalAmount(totalAmount)
                    .paidAmount(0L)
                    .remainingAmount(totalAmount)
                    .createdBy(AuthUtils.getCurrentAuthenticationId() == null ? null : UserEntity.builder().id(AuthUtils.getCurrentAuthenticationId()).build())
                    .issuedAt(issue ? now : null)
                    .build());
        } else {
            invoice.setInvoiceType(InvoiceType.FINAL_SETTLEMENT);
            invoice.setInvoiceReason(InvoiceReason.ROOM_CLOSE);
            invoice.setBillingPeriod(billingPeriod);
            invoice.setIssueDate(invoice.getIssueDate() == null ? now : invoice.getIssueDate());
            invoice.setDueDate(invoice.getDueDate() == null || issue ? now.plusDays(7) : invoice.getDueDate());
            invoice.setStatus(issue ? InvoiceStatus.ISSUED : InvoiceStatus.DRAFT);
            invoice.setSubtotalAmount(subtotal);
            invoice.setDiscountAmount(0L);
            invoice.setTotalAmount(totalAmount);
            invoice.setPaidAmount(0L);
            invoice.setRemainingAmount(totalAmount);
            invoice.setIssuedAt(issue ? now : null);
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
        return invoiceRepository.save(invoice);
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
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM invoices
                        WHERE lease_contract_id = ?
                          AND status NOT IN ('PAID', 'VOIDED')
                          AND remaining_amount > 0
                        """,
                Integer.class,
                contractId
        );
        if (count != null && count > 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Khach thue can thanh toan het hoa don con no truoc khi hoan tat thanh ly."
            );
        }
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
                AuthUtils.getCurrentAuthenticationId()
        );
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
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Vui lòng hoàn tất hoàn cọc và chờ khách thuê xác nhận đã nhận tiền trước khi thanh lý hợp đồng."
            );
        }
    }

    private void requireConfirmedMoveOutHandover(Long contractId) {
        ContractHandoverRecordEntity handover = handoverRecordRepository
                .findFirstByContract_IdAndHandoverTypeOrderByCreatedAtDesc(contractId, HandoverType.MOVE_OUT)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Vui long hoan tat ban giao tra phong truoc khi thanh ly hop dong."
                ));
        if (handover.getStatus() != HandoverStatus.CONFIRMED
                || handover.getElectricityReading() == null
                || handover.getWaterReading() == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Vui long hoan tat ban giao tra phong truoc khi thanh ly hop dong."
            );
        }
    }

    private long resolveLiquidationDepositAmount(LeaseContractEntity contract) {
        if (contract == null) {
            return 0L;
        }
        if (contract.getDepositAgreement() != null && contract.getDepositAgreement().getAmount() != null) {
            return contract.getDepositAgreement().getAmount();
        }
        return safe(contract.getDepositAmount());
    }

    private long calculateLiquidationDepositRefund(
            LeaseContractEntity contract,
            LocalDate liquidationDate,
            Long depositAmount,
            long deductionAmount
    ) {
        return Math.max(0L, safe(depositAmount) - deductionAmount);
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
                "Tien phong thang " + YearMonth.from(liquidationDate) + " den ngay " + liquidationDate,
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chi so moi khong duoc nho hon chi so cu.");
        }
        MeterType meterType = charge.lineType() == InvoiceLineType.ELECTRICITY
                ? MeterType.ELECTRICITY
                : MeterType.WATER;
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
                existing.setVoidReason("Superseded by liquidation reading revision " + nextRevision);
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
        return lineType == InvoiceLineType.ELECTRICITY || lineType == InvoiceLineType.WATER;
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
            case ELECTRICITY -> "Tien dien chot phong";
            case WATER -> "Tien nuoc chot phong";
            case SERVICE_FEE -> "Phi dich vu chot phong";
            case ROOM_RENT -> "Tien phong chot";
            case MAINTENANCE_COMPENSATION -> "Boi thuong sua chua";
            case VIOLATION_FINE -> "Phat vi pham";
            case TRANSFER_DIFFERENCE -> "Chenh lech chuyen phong";
            case DEPOSIT_DEDUCTION -> "Khau tru coc";
            case MANUAL_ADJUSTMENT -> "Dieu chinh thu cong";
            case OTHER -> "Phi phat sinh";
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

        String newContractCode = resolveRenewalContractCode(oldContract, requestedContractCode);
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

    public LeaseContractManagementResponse addCoOccupantFromChangeRequest(
            Long leaseContractId,
            Long tenantProfileId,
            LocalDate moveInDate,
            Long approvedBy
    ) {
        LeaseContractEntity contract = leaseContractRepository.findById(leaseContractId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay hop dong thue."));
        if (contract.getDeletedAt() != null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay hop dong thue.");
        }
        if (!List.of(LeaseStatus.ACTIVE, LeaseStatus.EXPIRING_SOON).contains(contract.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hop dong khong the them nguoi o cung.");
        }
        if (tenantProfileId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ho so nguoi o cung la bat buoc.");
        }
        if (contract.getPrimaryTenantProfile() != null
                && Objects.equals(contract.getPrimaryTenantProfile().getId(), tenantProfileId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Nguoi nay da la nguoi dung ten hop dong.");
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
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay ho so nguoi o cung.");
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hop dong chua gan phong.");
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
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phong da dat so nguoi o toi da.");
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
                "Them nguoi o cung profileId=" + tenantProfileId + "; approvedBy=" + approvedBy
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
        leaseExpiryReminderService.onTenantIntentionRecorded(contract, LocalDate.now());
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
                        WHERE lc.lease_contract_id = ?
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
        ensureNotRoomTransferManagedContract(leaseContractId);
        if (contract.getStatus() != LeaseStatus.DRAFT && contract.getStatus() != LeaseStatus.PENDING_SIGNATURE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chi duoc kich hoat hop dong dang cho ky.");
        }
        if (contract.getSignedFile() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Can upload file hop dong da ky truoc khi kich hoat.");
        }
        if (contract.getDepositAgreement() != null && contract.getDepositAgreement().getSignedFile() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Can upload file hop dong dat coc da ky truoc khi kich hoat.");
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

        // Bắt buộc phải có bản ghi bàn giao MOVE_IN kèm chỉ số điện/nước và bản ký trước khi kích hoạt
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
                        "Cần hoàn thành bàn giao phòng, nhập số điện/nước và upload biên bản bàn giao đã ký trước khi kích hoạt hợp đồng."
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
        if (previousContract != null && isHolderReplacementLiquidation(previousContract.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Finish the liquidation flow to activate this replacement contract."
            );
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
                LeaseStatus.AUTO_TERMINATED,
                LeaseStatus.CANCELLED
        ).contains(contract.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Khong the cap nhat thoi han cua hop dong da ket thuc."
            );
        }

        validateContractTerms(startDate, endDate, paymentCycleMonths, monthlyRent, depositAmount);
        RoomEntity room = contract.getRoom();
        LocalDate currentEndDate = contract.getEndDate();
        boolean extendsEndDate = currentEndDate != null && endDate.isAfter(currentEndDate);
        if (extendsEndDate && room != null) {
            if (hasOtherActiveContract(room.getId(), contract.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Phong dang co hop dong hieu luc khac.");
            }
            RoomCommitmentChecker.Blocker blocker =
                    roomCommitmentChecker.checkRenewBlockers(room.getId(), contract.getId());
            if (blocker != RoomCommitmentChecker.Blocker.NONE) {
                throwRenewBlocked(blocker);
            }
            if (room.getCurrentStatus() == RoomStatus.SOON_VACANT) {
                RoomStatus fromStatus = room.getCurrentStatus();
                room.setCurrentStatus(RoomStatus.OCCUPIED);
                roomRepository.saveAndFlush(room);
                contract.setTenantIntention("RENEW");
                contract.setExpectedVacantDate(null);
                contract.setIntentionRecordedAt(LocalDateTime.now());
                appendRoomStatusHistory(
                        room.getId(),
                        fromStatus,
                        RoomStatus.OCCUPIED,
                        "Gia han hop dong thue " + contract.getContractCode()
                );
                appendContractEvent(
                        contract.getId(),
                        "RENEWAL_AFTER_MOVE_OUT_INTENT",
                        "Gia han hop dong sau khi khach da bao chuyen di"
                );
            }
        }
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
                        "Gia han hop dong thue " + contract.getContractCode()
                );
            }
        }
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
                            CASE WHEN da.deposit_agreement_id IS NULL THEN 'CONTRACT' ELSE 'DEPOSIT' END AS source_type,
                            lc.lease_contract_id AS lease_contract_id,
                            da.deposit_agreement_id AS deposit_agreement_id,
                            da.deposit_code,
                            lc.contract_code,
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
                            GREATEST(
                                (
                                    SELECT COUNT(*)
                                    FROM contract_occupants co
                                    WHERE co.contract_id = lc.lease_contract_id
                                      AND co.status = 'ACTIVE'
                                ),
                                COALESCE(df.occupant_count, 1),
                                1 + COALESCE((
                                    SELECT COUNT(*)
                                    FROM deposit_form_co_occupants dco_count
                                    WHERE dco_count.deposit_form_id = df.deposit_form_id
                                ), 0)
                            ) AS occupants_count,
                            lc.status AS contract_status,
                            da.status AS deposit_status,
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
                            lc.created_at,
                            u.user_id AS user_id,
                            u.last_login_at
                        FROM lease_contracts lc
                        JOIN rooms r ON r.room_id = lc.room_id
                        JOIN properties p ON p.property_id = r.property_id
                        JOIN person_profiles pp ON pp.person_profile_id = lc.primary_tenant_profile_id
                        LEFT JOIN deposit_agreements da ON da.deposit_agreement_id = lc.deposit_agreement_id
                        LEFT JOIN deposit_forms df ON df.deposit_form_id = da.deposit_form_id
                        LEFT JOIN lease_contracts previous_contract ON previous_contract.lease_contract_id = lc.previous_contract_id
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
        assertRoomHasNoPendingContract(room);
        Integer maxOccupants = room.getMaxOccupants() != null ? room.getMaxOccupants() : 3;
        if (occupantsCount > maxOccupants) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "So nguoi o vuot qua so nguoi toi da cua phong.");
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

    private String resolveRenewalContractCode(LeaseContractEntity oldContract, String requestedContractCode) {
        String contractCode = requestedContractCode == null ? "" : requestedContractCode.trim();
        if (contractCode.isBlank()) {
            contractCode = generateRenewalContractCode(oldContract);
        }
        if (contractCode.length() > 80) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ma hop dong moi khong duoc vuot qua 80 ky tu.");
        }
        if (leaseContractRepository.existsByContractCodeAndDeletedAtIsNull(contractCode)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ma hop dong moi da ton tai.");
        }
        return contractCode;
    }

    private String generateRenewalContractCode(LeaseContractEntity oldContract) {
        LeaseContractEntity rootContract = oldContract;
        int renewalNumber = 1;
        while (rootContract.getPreviousContract() != null) {
            rootContract = rootContract.getPreviousContract();
            renewalNumber++;
        }

        return rootContract.getContractCode() + "-R" + renewalNumber;
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

    private void assertOwnerOrManagerCanRenew() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean canRenew = authentication != null
                && authentication.getAuthorities() != null
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_OWNER".equals(authority.getAuthority())
                        || "ROLE_MANAGER".equals(authority.getAuthority()));
        if (!canRenew) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "RENEWAL_APPROVAL_REQUIRED: Chi chu tro hoac quan ly moi co quyen xac nhan tai ky hop dong."
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
        Long signedFileId = getLongOrNull(rs, "signed_file_id");
        Long userId = getLongOrNull(rs, "user_id");
        String code = rs.getString("contract_code");
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
                resolveRenewBlocker(roomId, leaseContractId, renewedContractId, parsedContractStatus);
        Long liquidationFinalInvoiceId = getLongOrNull(rs, "liquidation_final_invoice_id");
        LiquidationInvoiceSummary liquidationInvoiceSummary = liquidationInvoiceSummary(liquidationFinalInvoiceId);
        ExpenseRequestService.LiquidationDepositRefundLink refundLink =
                expenseRequestService.getLiquidationDepositRefundLink(leaseContractId);
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
                .canLiquidate(leaseContractId != null && isLiquidatableContractStatus(parsedContractStatus))
                .transferRequestId(transferRequestId)
                .transferRequestCode(rs.getString("transfer_request_code"))
                .transferStatus(transferStatus)
                .transferRequestedDate(toLocalDate(rs, "transfer_requested_date"))
                .transferContractRole(sourceTransferCompleted && transferContractRole == null
                        ? "OLD_CONTRACT"
                        : transferContractRole)
                .transferActivationLocked(isTransferActivationLocked(transferRequestId, transferStatus))
                .contractStatus(parsedContractStatus)
                .depositStatus(parseEnum(DepositAgreementStatus.class, depositStatus))
                .workflowStatus(resolveWorkflow(effectiveContractStatus, signedFileId != null ? signedFileId : contractFileId))
                .contractFileId(contractFileId)
                .contractFileName(rs.getString("contract_file_name"))
                .contractFileUploadedAt(toLocalDateTime(rs, "contract_file_uploaded_at"))
                .signedFileId(signedFileId)
                .signedFileName(rs.getString("signed_file_name"))
                .signedFileUploadedAt(toLocalDateTime(rs, "signed_file_uploaded_at"))
                .signedUploadedById(getLongOrNull(rs, "signed_uploaded_by"))
                .handoverSignedFileId(getLongOrNull(rs, "handover_signed_file_id"))
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

    private boolean isLiquidatableContractStatus(LeaseStatus contractStatus) {
        return contractStatus == LeaseStatus.ACTIVE
                || contractStatus == LeaseStatus.EXPIRING_SOON
                || contractStatus == LeaseStatus.EXPIRED
                || contractStatus == LeaseStatus.TERMINATION_PENDING;
    }

    private String renewBlockedReason(RoomCommitmentChecker.Blocker blocker) {
        if (blocker == RoomCommitmentChecker.Blocker.ROOM_HOLD_IN_PROGRESS) {
            return "Phong dang duoc giu cho cho khach khac.";
        }
        return "Phong da co khach khac dat coc/giu cho, khong the tai ky.";
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
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Hop dong thuoc yeu cau chuyen phong; vui long xu ly ban giao/kich hoat trong luong chuyen phong."
            );
        }
    }

    private boolean isTransferActivationLocked(Long transferRequestId, String transferStatus) {
        if (transferRequestId == null) {
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
