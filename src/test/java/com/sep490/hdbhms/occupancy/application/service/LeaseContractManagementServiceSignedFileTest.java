package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.file.application.port.in.command.UploadFileCommand;
import com.sep490.hdbhms.file.application.service.UploadFileService;
import com.sep490.hdbhms.file.domain.model.FileMetadata;
import com.sep490.hdbhms.file.domain.value_objects.FileCategory;
import com.sep490.hdbhms.file.infrastructure.persistence.entity.FileMetadataEntity;
import com.sep490.hdbhms.file.infrastructure.persistence.jpa.JpaFileMetadataRepository;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import com.sep490.hdbhms.identityandaccess.infrastructure.config.security.UserPrincipal;
import com.sep490.hdbhms.occupancy.domain.value_objects.LeaseStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeaseContractEntity;
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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
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

    private static LeaseContractManagementService newService(
            UploadFileService uploadFileService,
            JpaFileMetadataRepository fileMetadataRepository,
            JpaLeaseContractRepository leaseContractRepository
    ) {
        return new LeaseContractManagementService(
                mock(JdbcTemplate.class),
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
