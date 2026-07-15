package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.file.application.service.UploadFileService;
import com.sep490.hdbhms.file.infrastructure.persistence.jpa.JpaFileMetadataRepository;
import com.sep490.hdbhms.occupancy.domain.value_objects.LeaseStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeaseContractEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RoomEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaContractLiquidationRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaDepositAgreementRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaLeaseContractRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
                mock(JpaDepositAgreementRepository.class),
                mock(JpaContractLiquidationRepository.class),
                mock(RoomCommitmentChecker.class)
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
}
