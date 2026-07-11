package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.file.application.port.in.command.UploadFileCommand;
import com.sep490.hdbhms.file.application.port.in.usecase.UploadFileUseCase;
import com.sep490.hdbhms.file.domain.model.FileMetadata;
import com.sep490.hdbhms.file.domain.value_objects.FileCategory;
import com.sep490.hdbhms.file.infrastructure.persistence.entity.FileMetadataEntity;
import com.sep490.hdbhms.file.infrastructure.persistence.jpa.JpaFileMetadataRepository;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import com.sep490.hdbhms.identityandaccess.infrastructure.config.security.UserPrincipal;
import com.sep490.hdbhms.occupancy.domain.value_objects.HandoverType;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.ContractHandoverRecordEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaContractHandoverRecordRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.thymeleaf.TemplateEngine;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HandoverDocumentServiceSignedFileTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void attachSignedDocumentUploadsWithCurrentUserId() throws Exception {
        setUser(88L, Role.MANAGER);
        var uploadUseCase = mock(UploadFileUseCase.class);
        var handoverRepository = mock(JpaContractHandoverRecordRepository.class);
        var fileRepository = mock(JpaFileMetadataRepository.class);
        var record = ContractHandoverRecordEntity.builder().id(5L).handoverType(HandoverType.MOVE_IN).build();
        var uploadedFile = FileMetadataEntity.builder().id(44L).originalName("handover.pdf").build();

        when(handoverRepository.findByContract_IdAndHandoverType(99L, HandoverType.MOVE_IN))
                .thenReturn(Optional.of(record));
        when(uploadUseCase.execute(org.mockito.ArgumentMatchers.any(UploadFileCommand.class)))
                .thenReturn(FileMetadata.builder().id(44L).ownerUserId(88L).category(FileCategory.HANDOVER_DOCUMENT).build());
        when(fileRepository.findById(44L)).thenReturn(Optional.of(uploadedFile));

        var service = new HandoverDocumentService(
                mock(TemplateEngine.class),
                mock(JdbcTemplate.class),
                uploadUseCase,
                handoverRepository,
                fileRepository
        );

        service.attachSignedDocument(99L, HandoverType.MOVE_IN, new MockMultipartFile(
                "file",
                "handover.pdf",
                "application/pdf",
                new byte[]{1, 2, 3}
        ));

        ArgumentCaptor<UploadFileCommand> commandCaptor = ArgumentCaptor.forClass(UploadFileCommand.class);
        verify(uploadUseCase).execute(commandCaptor.capture());
        assertEquals(88L, commandCaptor.getValue().ownerUserId());
        assertEquals(FileCategory.HANDOVER_DOCUMENT, commandCaptor.getValue().category());
        assertSame(uploadedFile, record.getSignedDocument());
        verify(handoverRepository).save(record);
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
