package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.occupancy.application.port.in.command.AddCoOccupantToContractCommand;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.AddCoOccupantToContractUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetLeaseContractManagementUseCase;
import com.sep490.hdbhms.occupancy.domain.value_objects.LeaseStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeaseContractEntity;
import com.sep490.hdbhms.property.infrastructure.persistence.entity.RoomEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaLeaseContractRepository;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.LeaseContractManagementResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AddCoOccupantToContractService implements AddCoOccupantToContractUseCase {
    JpaLeaseContractRepository leaseContractRepository;
    JdbcTemplate jdbcTemplate;
    LeaseContractWorkflowSupport workflowSupport;
    GetLeaseContractManagementUseCase getLeaseContractManagementUseCase;

    @Override
    public LeaseContractManagementResponse execute(AddCoOccupantToContractCommand command) {
        LeaseContractEntity contract = leaseContractRepository.findById(command.leaseContractId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hợp đồng thuê."));
        if (contract.getDeletedAt() != null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hợp đồng thuê.");
        }
        if (!List.of(LeaseStatus.ACTIVE, LeaseStatus.EXPIRING_SOON).contains(contract.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hợp đồng không thể thêm người ở cùng.");
        }
        if (command.tenantProfileId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hồ sơ người ở cùng là bắt buộc.");
        }
        if (contract.getPrimaryTenantProfile() != null
                && Objects.equals(contract.getPrimaryTenantProfile().getId(), command.tenantProfileId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Người này đã là người đứng tên hợp đồng.");
        }
        Integer profileExists = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM person_profiles
                        WHERE person_profile_id = ?
                          AND deleted_at IS NULL
                        """,
                Integer.class,
                command.tenantProfileId()
        );
        if (profileExists == null || profileExists == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hồ sơ người ở cùng.");
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
                command.tenantProfileId()
        );
        if (activeDuplicate != null && activeDuplicate > 0) {
            return getLeaseContractManagementUseCase.findOne(contract.getId());
        }

        RoomEntity room = contract.getRoom();
        if (room == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hợp đồng chưa gắn phòng.");
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
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phòng đã đạt số người ở tối đa.");
        }

        Long propertyId = room.getProperty() == null ? null : room.getProperty().getId();
        Long tenantId = resolveTenantIdForProfile(command.tenantProfileId(), propertyId);
        LocalDate finalMoveInDate = command.moveInDate() == null ? LocalDate.now() : command.moveInDate();
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
                command.tenantProfileId(),
                finalMoveInDate
        );
        workflowSupport.appendContractEvent(
                contract.getId(),
                "OCCUPANT_CHANGED",
                "Thêm người ở cùng profileId=" + command.tenantProfileId() + "; approvedBy=" + command.approvedBy()
        );
        return getLeaseContractManagementUseCase.findOne(contract.getId());
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
}
