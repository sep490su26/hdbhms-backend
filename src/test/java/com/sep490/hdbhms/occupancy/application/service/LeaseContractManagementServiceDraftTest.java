package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaInvoiceLineRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaInvoiceRepository;
import com.sep490.hdbhms.accounting.application.service.ExpenseRequestService;
import com.sep490.hdbhms.file.application.service.UploadFileService;
import com.sep490.hdbhms.file.infrastructure.persistence.jpa.JpaFileMetadataRepository;
import com.sep490.hdbhms.occupancy.domain.value_objects.LeaseStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeaseContractEntity;
import com.sep490.hdbhms.property.infrastructure.persistence.entity.RoomEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaRoomRepository;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaMeterRepository;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaMeterReadingRepository;
import com.sep490.hdbhms.property.application.service.RoomCommitmentChecker;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaRoomRepository;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaMeterRepository;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaMeterReadingRepository;
import com.sep490.hdbhms.property.application.service.RoomCommitmentChecker;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaRoomRepository;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaMeterRepository;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaMeterReadingRepository;
import com.sep490.hdbhms.property.application.service.RoomCommitmentChecker;
import static org.mockito.ArgumentMatchers.anyString;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaRoomRepository;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaMeterRepository;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaMeterReadingRepository;
import com.sep490.hdbhms.property.application.service.RoomCommitmentChecker;
import static org.mockito.ArgumentMatchers.eq;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaRoomRepository;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaMeterRepository;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaMeterReadingRepository;
import com.sep490.hdbhms.property.application.service.RoomCommitmentChecker;
import static org.mockito.Mockito.mock;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaRoomRepository;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaMeterRepository;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaMeterReadingRepository;
import com.sep490.hdbhms.property.application.service.RoomCommitmentChecker;
import static org.mockito.Mockito.verify;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaRoomRepository;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaMeterRepository;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaMeterReadingRepository;
import com.sep490.hdbhms.property.application.service.RoomCommitmentChecker;
import static org.mockito.Mockito.when;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaRoomRepository;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaMeterRepository;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaMeterReadingRepository;
import com.sep490.hdbhms.property.application.service.RoomCommitmentChecker;

class LeaseContractManagementServiceDraftTest {

    @Test
    void pendingContractConflictIncludesRoomAndContractCode() {
        JpaLeaseContractRepository leaseContractRepository = mock(JpaLeaseContractRepository.class);
        RoomEntity room = RoomEntity.builder().id(35L).roomCode("505").build();
        LeaseContractEntity blockingContract = LeaseContractEntity.builder()
                .id(1L)
                .contractCode("DEMO-LEASE-505-DRAFT")
                .status(LeaseStatus.DRAFT)
                .build();
        when(leaseContractRepository.findFirstByRoom_IdAndStatusInAndDeletedAtIsNullOrderByIdDesc(
                35L,
                List.of(LeaseStatus.DRAFT, LeaseStatus.PENDING_SIGNATURE)
        )).thenReturn(Optional.of(blockingContract));

        LeaseContractManagementService service = new LeaseContractManagementService(
                mock(JdbcTemplate.class),
                mock(UploadFileService.class),
                mock(JpaRoomRepository.class),
                mock(JpaFileMetadataRepository.class),
                leaseContractRepository,
                mock(JpaContractOccupantRepository.class),
                mock(JpaContractLiquidationRepository.class),
                mock(JpaContractHandoverRecordRepository.class),
                mock(JpaInvoiceRepository.class),
                mock(JpaInvoiceLineRepository.class),
                mock(JpaMeterRepository.class),
                mock(JpaMeterReadingRepository.class),
                mock(RoomCommitmentChecker.class),
                mock(LeaseExpiryReminderService.class),
                mock(ExpenseRequestService.class)
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.assertRoomHasNoPendingContract(room)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Phòng 505"));
        assertTrue(exception.getReason().contains("DEMO-LEASE-505-DRAFT"));
        assertTrue(exception.getReason().contains("DRAFT"));
    }

    @Test
    void liquidationDeactivatesTenantOnlyWhenNoValidContractRemains() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.update(anyString(), eq(99L), eq(99L), eq(99L))).thenReturn(1);

        LeaseContractManagementService service = new LeaseContractManagementService(
                jdbcTemplate,
                mock(UploadFileService.class),
                mock(JpaRoomRepository.class),
                mock(JpaFileMetadataRepository.class),
                mock(JpaLeaseContractRepository.class),
                mock(JpaContractOccupantRepository.class),
                mock(JpaContractLiquidationRepository.class),
                mock(JpaContractHandoverRecordRepository.class),
                mock(JpaInvoiceRepository.class),
                mock(JpaInvoiceLineRepository.class),
                mock(JpaMeterRepository.class),
                mock(JpaMeterReadingRepository.class),
                mock(RoomCommitmentChecker.class),
                mock(LeaseExpiryReminderService.class),
                mock(ExpenseRequestService.class)
        );

        assertEquals(1, service.deactivateTenantAccountsWithoutValidContract(99L));

        var sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).update(sqlCaptor.capture(), eq(99L), eq(99L), eq(99L));
        String sql = sqlCaptor.getValue();
        assertTrue(sql.contains("SET u.status = 'INACTIVE'"));
        assertTrue(sql.contains("u.role = 'TENANT'"));
        assertTrue(sql.contains("NOT EXISTS"));
        assertTrue(sql.contains("active_co.status = 'ACTIVE'"));
        assertTrue(sql.contains("active_lc.status IN ('ACTIVE', 'EXPIRING_SOON', 'TERMINATION_PENDING')"));
    }

}
