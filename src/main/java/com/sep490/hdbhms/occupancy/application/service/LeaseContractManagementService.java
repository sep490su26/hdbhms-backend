package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.billingandpayment.domain.value_objects.DepositAgreementStatus;
import com.sep490.hdbhms.file.application.port.in.command.UploadFileCommand;
import com.sep490.hdbhms.file.application.service.UploadFileService;
import com.sep490.hdbhms.file.domain.value_objects.FileCategory;
import com.sep490.hdbhms.file.infrastructure.persistence.jpa.JpaFileMetadataRepository;
import com.sep490.hdbhms.occupancy.domain.value_objects.LeaseStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.LiquidationStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.RoomStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.ContractLiquidationEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.DepositAgreementEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeaseContractEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RoomEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaContractLiquidationRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaDepositAgreementRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaLeaseContractRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomRepository;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.LeaseContractManagementResponse;
import com.sep490.hdbhms.shared.utils.AuthUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LeaseContractManagementService {
    JdbcTemplate jdbcTemplate;
    UploadFileService uploadFileService;
    JpaRoomRepository roomRepository;
    JpaFileMetadataRepository fileMetadataRepository;
    JpaLeaseContractRepository leaseContractRepository;
    JpaDepositAgreementRepository depositAgreementRepository;
    JpaContractLiquidationRepository contractLiquidationRepository;

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
                            COALESCE(lc.monthly_rent, r.listed_price) AS monthly_rent,
                            COALESCE(lc.payment_cycle_months, df.payment_cycle_months, 1) AS payment_cycle_months,
                            COALESCE(lc.deposit_amount, da.amount) AS deposit_amount,
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
                            lc.monthly_rent,
                            lc.payment_cycle_months,
                            lc.deposit_amount,
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
            return findOne(leaseContractId);
        }
        if (contract.getStatus() != LeaseStatus.ACTIVE
                && contract.getStatus() != LeaseStatus.EXPIRING_SOON
                && contract.getStatus() != LeaseStatus.TERMINATION_PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ thanh lý hợp đồng đang hiệu lực hoặc sắp kết thúc.");
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
        leaseContractRepository.save(contract);

        room.setCurrentStatus(RoomStatus.VACANT);
        roomRepository.save(room);

        return findOne(contract.getId());
    }

    public LeaseContractManagementResponse activate(Long leaseContractId) {
        LeaseContractEntity contract = leaseContractRepository.findById(leaseContractId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay hop dong thue."));
        if (contract.getStatus() == LeaseStatus.ACTIVE) {
            return findOne(leaseContractId);
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
        if (room.getCurrentStatus() != RoomStatus.RESERVED && room.getCurrentStatus() != RoomStatus.ON_HOLD) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phong phai o trang thai RESERVED truoc khi kich hoat hop dong.");
        }

        contract.setStatus(LeaseStatus.ACTIVE);
        contract.setSignedAt(LocalDateTime.now());
        leaseContractRepository.save(contract);

        room.setCurrentStatus(RoomStatus.OCCUPIED);
        roomRepository.save(room);

        if (contract.getDepositAgreement() != null
                && contract.getDepositAgreement().getStatus() != DepositAgreementStatus.CONVERTED_TO_LEASE) {
            contract.getDepositAgreement().setStatus(DepositAgreementStatus.CONVERTED_TO_LEASE);
            depositAgreementRepository.save(contract.getDepositAgreement());
        }
        return findOne(contract.getId());
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
                            lc.monthly_rent,
                            lc.payment_cycle_months,
                            lc.deposit_amount,
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
        String contractCode = "HD-" + startDate.getYear() + "-H" + room.getRoomCode() + "-" + deposit.getId();
        LeaseContractEntity contract = LeaseContractEntity.builder()
                .contractCode(contractCode)
                .room(room)
                .depositAgreement(deposit)
                .primaryTenantProfile(deposit.getDepositorPersonProfile())
                .startDate(startDate)
                .endDate(startDate.plusYears(1).minusDays(1))
                .rentStartDate(startDate)
                .monthlyRent(room.getListedPrice())
                .paymentCycleMonths(resolvePaymentCycleMonths(deposit))
                .depositAmount(deposit.getAmount())
                .status(LeaseStatus.PENDING_SIGNATURE)
                .build();
        return leaseContractRepository.save(contract);
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
        String code = leaseContractId != null
                ? rs.getString("contract_code")
                : rs.getString("deposit_code");
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
                .roomId(getLongOrNull(rs, "room_id"))
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
                .monthlyRent(getLongOrNull(rs, "monthly_rent"))
                .paymentCycleMonths(getIntOrNull(rs, "payment_cycle_months"))
                .depositAmount(getLongOrNull(rs, "deposit_amount"))
                .contractStatus(parseEnum(LeaseStatus.class, contractStatus))
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

    private String resolveWorkflow(String contractStatus, Long contractFileId) {
        if ("ACTIVE".equals(contractStatus)) {
            return "ACTIVE";
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
