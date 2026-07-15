package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.file.application.port.in.command.UploadFileCommand;
import com.sep490.hdbhms.file.application.service.UploadFileService;
import com.sep490.hdbhms.file.domain.model.FileMetadata;
import com.sep490.hdbhms.file.domain.value_objects.FileCategory;
import com.sep490.hdbhms.file.infrastructure.persistence.entity.FileMetadataEntity;
import com.sep490.hdbhms.file.infrastructure.persistence.jpa.JpaFileMetadataRepository;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import com.sep490.hdbhms.identityandaccess.infrastructure.config.security.UserPrincipal;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.PersonProfileEntity;
import com.sep490.hdbhms.occupancy.domain.value_objects.LeaseStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.RoomStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.DepositAgreementEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeaseContractEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RoomEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaContractLiquidationRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaDepositAgreementRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaLeaseContractRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomRepository;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.LeaseContractManagementResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LeaseContractManagementServiceSignedFileTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void uploadSignedFileUpdatesSignedFileAndDoesNotOverwriteContractFile() {
        setUser(77L, Role.OWNER);
        var leaseContractRepository = mock(JpaLeaseContractRepository.class);
        var fileMetadataRepository = mock(JpaFileMetadataRepository.class);
        var uploadFileService = mock(UploadFileService.class);
        var existingDraftFile = FileMetadataEntity.builder().id(11L).originalName("draft.pdf").build();
        var uploadedSignedFile = FileMetadataEntity.builder().id(22L).originalName("signed.pdf").build();
        var contract = LeaseContractEntity.builder()
                .id(99L)
                .contractCode("HD-2026-H101-9")
                .startDate(LocalDate.of(2026, 6, 29))
                .endDate(LocalDate.of(2027, 6, 28))
                .rentStartDate(LocalDate.of(2026, 7, 1))
                .status(LeaseStatus.DRAFT)
                .contractFile(existingDraftFile)
                .build();

        when(leaseContractRepository.findById(99L)).thenReturn(Optional.of(contract));
        when(uploadFileService.execute(any(UploadFileCommand.class))).thenReturn(FileMetadata.builder()
                .id(22L)
                .ownerUserId(77L)
                .category(FileCategory.CONTRACT)
                .build());
        when(fileMetadataRepository.findById(22L)).thenReturn(Optional.of(uploadedSignedFile));
        when(fileMetadataRepository.save(uploadedSignedFile)).thenReturn(uploadedSignedFile);

        var service = spy(newService(uploadFileService, fileMetadataRepository, leaseContractRepository));
        doReturn(LeaseContractManagementResponse.builder()
                .leaseContractId(99L)
                .signedFileId(22L)
                .contractFileId(11L)
                .build())
                .when(service).findOne(99L);

        service.uploadSignedFile(99L, pdfFile(), false);

        assertSame(uploadedSignedFile, contract.getSignedFile());
        assertSame(existingDraftFile, contract.getContractFile());
        assertEquals(77L, contract.getSignedUploadedBy().getId());

        var commandCaptor = ArgumentCaptor.forClass(UploadFileCommand.class);
        verify(uploadFileService).execute(commandCaptor.capture());
        assertEquals(77L, commandCaptor.getValue().ownerUserId());
        verify(leaseContractRepository).save(contract);
    }

    @Test
    void uploadSignedFileRejectsExistingSignedFileWithoutReplaceFlag() {
        var leaseContractRepository = mock(JpaLeaseContractRepository.class);
        var contract = LeaseContractEntity.builder()
                .id(99L)
                .status(LeaseStatus.DRAFT)
                .signedFile(FileMetadataEntity.builder().id(22L).build())
                .build();
        when(leaseContractRepository.findById(99L)).thenReturn(Optional.of(contract));

        var service = newService(mock(UploadFileService.class), mock(JpaFileMetadataRepository.class), leaseContractRepository);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.uploadSignedFile(99L, pdfFile(), false)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void activateRejectsDraftDocumentWhenSignedLeaseIsMissing() {
        var leaseContractRepository = mock(JpaLeaseContractRepository.class);
        var contract = LeaseContractEntity.builder()
                .id(99L)
                .status(LeaseStatus.DRAFT)
                .contractFile(FileMetadataEntity.builder().id(11L).build())
                .build();
        when(leaseContractRepository.findById(99L)).thenReturn(Optional.of(contract));

        var service = newService(
                mock(UploadFileService.class),
                mock(JpaFileMetadataRepository.class),
                leaseContractRepository
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.activate(99L)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getReason().contains("hop dong da ky"));
    }

    @Test
    void activateRejectsUnsignedDepositDocument() {
        var leaseContractRepository = mock(JpaLeaseContractRepository.class);
        var contract = LeaseContractEntity.builder()
                .id(99L)
                .status(LeaseStatus.PENDING_SIGNATURE)
                .signedFile(FileMetadataEntity.builder().id(22L).build())
                .depositAgreement(DepositAgreementEntity.builder().id(33L).build())
                .build();
        when(leaseContractRepository.findById(99L)).thenReturn(Optional.of(contract));

        var service = newService(
                mock(UploadFileService.class),
                mock(JpaFileMetadataRepository.class),
                leaseContractRepository
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.activate(99L)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getReason().contains("hop dong dat coc da ky"));
    }

    @Test
    void activateHandoverCheckDoesNotRequireSignedDocument() {
        var jdbcTemplate = mock(JdbcTemplate.class);
        var leaseContractRepository = mock(JpaLeaseContractRepository.class);
        var room = RoomEntity.builder()
                .id(8L)
                .currentStatus(RoomStatus.MAINTENANCE)
                .build();
        var contract = LeaseContractEntity.builder()
                .id(99L)
                .status(LeaseStatus.PENDING_SIGNATURE)
                .signedFile(FileMetadataEntity.builder().id(22L).build())
                .primaryTenantProfile(PersonProfileEntity.builder().id(44L).build())
                .startDate(LocalDate.of(2026, 7, 15))
                .endDate(LocalDate.of(2027, 7, 14))
                .room(room)
                .build();

        when(leaseContractRepository.findById(99L)).thenReturn(Optional.of(contract));
        when(jdbcTemplate.queryForObject(
                anyString(),
                eq(Integer.class),
                any(Object[].class)
        )).thenReturn(0, 1);

        var service = newService(
                jdbcTemplate,
                mock(UploadFileService.class),
                mock(JpaFileMetadataRepository.class),
                leaseContractRepository
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.activate(99L)
        );

        assertTrue(exception.getReason().contains("Phong phai o trang thai"));
        var sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, times(2)).queryForObject(
                sqlCaptor.capture(),
                eq(Integer.class),
                any(Object[].class)
        );
        String handoverSql = sqlCaptor.getAllValues().stream()
                .filter(sql -> sql.contains("contract_handover_records"))
                .findFirst()
                .orElseThrow();
        assertFalse(handoverSql.contains("signed_document_id"));
    }

    private static LeaseContractManagementService newService(
            UploadFileService uploadFileService,
            JpaFileMetadataRepository fileMetadataRepository,
            JpaLeaseContractRepository leaseContractRepository
    ) {
        return newService(
                mock(JdbcTemplate.class),
                uploadFileService,
                fileMetadataRepository,
                leaseContractRepository
        );
    }

    private static LeaseContractManagementService newService(
            JdbcTemplate jdbcTemplate,
            UploadFileService uploadFileService,
            JpaFileMetadataRepository fileMetadataRepository,
            JpaLeaseContractRepository leaseContractRepository
    ) {
        return new LeaseContractManagementService(
                jdbcTemplate,
                uploadFileService,
                mock(JpaRoomRepository.class),
                fileMetadataRepository,
                leaseContractRepository,
                mock(JpaDepositAgreementRepository.class),
                mock(JpaContractLiquidationRepository.class),
                mock(RoomCommitmentChecker.class)
        );
    }

    private static MockMultipartFile pdfFile() {
        return new MockMultipartFile("file", "signed.pdf", "application/pdf", new byte[]{1, 2, 3});
    }

    private static void setUser(Long userId, Role role) {
        var authority = new SimpleGrantedAuthority("ROLE_" + role.name());
        var principal = UserPrincipal.builder()
                .id(userId)
                .role(role)
                .authorities(Set.of(authority))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }
}
