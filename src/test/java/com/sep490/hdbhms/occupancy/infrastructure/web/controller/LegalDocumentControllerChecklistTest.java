package com.sep490.hdbhms.occupancy.infrastructure.web.controller;

import com.sep490.hdbhms.file.application.port.in.query.DownloadFileQuery;
import com.sep490.hdbhms.file.application.port.in.usecase.DownloadFileUseCase;
import com.sep490.hdbhms.file.application.port.in.usecase.UploadFileUseCase;
import com.sep490.hdbhms.file.infrastructure.web.dto.response.FileDataResponse;
import com.sep490.hdbhms.identityandaccess.application.port.out.PersonProfileRepository;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import com.sep490.hdbhms.identityandaccess.infrastructure.config.security.UserPrincipal;
import com.sep490.hdbhms.property.application.port.in.query.GetRoomDetailsQuery;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetLeaseContractDetailsUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetLeaseContractManagementUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetMyListLeaseContractsUseCase;
import com.sep490.hdbhms.property.application.port.in.usecase.GetRoomDetailsUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.ActivateLeaseContractUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.CreateDraftLeaseContractForDepositUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.CompleteLeaseLiquidationUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.RecordTenantIntentionUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.RenewLeaseContractUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.UpdateLeaseContractTermsUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.UpdateLeaseLiquidationDraftUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.UploadSignedLeaseContractFileUseCase;
import com.sep490.hdbhms.occupancy.application.service.HandoverDocumentService;
import com.sep490.hdbhms.occupancy.application.service.LeaseContractDocumentService;
import com.sep490.hdbhms.occupancy.application.service.LeaseContractQueryService;
import com.sep490.hdbhms.occupancy.application.service.ManageContractHandoverService;
import com.sep490.hdbhms.property.application.service.RoomCommitmentChecker;
import com.sep490.hdbhms.occupancy.application.service.ContractLifecycleChangeRequestService;
import com.sep490.hdbhms.property.domain.model.Room;
import com.sep490.hdbhms.occupancy.domain.value_objects.HandoverType;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.LeaseContractManagementResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.mapper.LeaseContractWebMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LegalDocumentControllerChecklistTest {
    private static final LocalDate DOC_DATE = LocalDate.of(2026, 6, 29);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void downloadDraftLeaseUsesRoomCodeHdtDateFilename() {
        setUser(1L, Role.OWNER);
        var managementUseCase = mock(GetLeaseContractManagementUseCase.class);
        var documentService = mock(LeaseContractDocumentService.class);
        var controller = leaseController(managementUseCase, documentService, mock(DownloadFileUseCase.class), mock(JdbcTemplate.class));

        when(managementUseCase.findOne(9L)).thenReturn(leaseResponse().signedFileId(null).build());
        when(documentService.generateDraftPdf(9L)).thenReturn(new byte[]{1, 2, 3});

        var response = controller.getDraftPdf(9L);

        assertAttachmentFilename(response.getHeaders(), "HDT_P101_29_06_2026.pdf");
    }

    @Test
    void downloadSignedLeaseUsesRoomCodeHdtDateFilename() {
        setUser(1L, Role.OWNER);
        var managementUseCase = mock(GetLeaseContractManagementUseCase.class);
        var downloadUseCase = mock(DownloadFileUseCase.class);
        var controller = leaseController(managementUseCase, mock(LeaseContractDocumentService.class), downloadUseCase, mock(JdbcTemplate.class));

        when(managementUseCase.findOne(9L)).thenReturn(leaseResponse().signedFileId(901L).build());
        when(downloadUseCase.execute(new DownloadFileQuery(901L))).thenReturn(pdfData());

        var response = controller.downloadSignedLeaseContractFile(9L);

        assertAttachmentFilename(response.getHeaders(), "HDT_P101_29_06_2026.pdf");
    }

    @Test
    void downloadDraftHandoverUsesStandardBbbgFilename() {
        setUser(1L, Role.OWNER);
        var handoverService = mock(HandoverDocumentService.class);
        var controller = handoverController(handoverService, mock(DownloadFileUseCase.class), mock(JdbcTemplate.class));

        when(handoverService.generateHandoverDraftPdf(9L, HandoverType.MOVE_IN)).thenReturn(new byte[]{1, 2, 3});
        when(handoverService.getFilenameContext(9L, HandoverType.MOVE_IN))
                .thenReturn(new HandoverDocumentService.HandoverFilenameContext("101", "Nguyễn Văn A", DOC_DATE, DOC_DATE.plusDays(1), null));

        var response = controller.getHandoverDraftPdf(9L, HandoverType.MOVE_IN);

        assertAttachmentFilename(response.getHeaders(), "BBBG_P101_29_06_2026.pdf");
    }

    @Test
    void downloadSignedHandoverUsesStandardBbbgFilename() {
        setUser(1L, Role.OWNER);
        var handoverService = mock(HandoverDocumentService.class);
        var downloadUseCase = mock(DownloadFileUseCase.class);
        var controller = handoverController(handoverService, downloadUseCase, mock(JdbcTemplate.class));

        when(handoverService.getFilenameContext(9L, HandoverType.MOVE_IN))
                .thenReturn(new HandoverDocumentService.HandoverFilenameContext("101", "Nguyễn Văn A", DOC_DATE, LocalDate.of(2026, 7, 1), 902L));
        when(downloadUseCase.execute(new DownloadFileQuery(902L))).thenReturn(pdfData());

        var response = controller.downloadSignedHandoverPdf(9L, HandoverType.MOVE_IN);

        assertAttachmentFilename(response.getHeaders(), "BBBG_P101_01_07_2026.pdf");
    }

    @Test
    void managerOutsidePropertyScopeCannotDownloadLeaseDocuments() {
        setUser(77L, Role.MANAGER);
        var jdbcTemplate = mock(JdbcTemplate.class);
        var documentService = mock(LeaseContractDocumentService.class);
        var controller = leaseController(mock(GetLeaseContractManagementUseCase.class), documentService, mock(DownloadFileUseCase.class), jdbcTemplate);

        when(jdbcTemplate.query(anyString(), org.mockito.ArgumentMatchers.<ResultSetExtractor<Long>>any(), eq(9L)))
                .thenReturn(700L);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(77L), eq(700L))).thenReturn(0);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> controller.getDraftPdf(9L));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(documentService, never()).generateDraftPdf(9L);
    }

    @Test
    void tenantCannotUploadSignedLegalDocuments() {
        setUser(88L, Role.TENANT);
        var file = new MockMultipartFile("file", "signed.pdf", "application/pdf", new byte[]{1, 2, 3});

        var leaseUploadException = assertThrows(
                ResponseStatusException.class,
                () -> leaseController(mock(GetLeaseContractManagementUseCase.class), mock(LeaseContractDocumentService.class), mock(DownloadFileUseCase.class), mock(JdbcTemplate.class))
                        .uploadSignedFile(9L, file, false)
        );
        var handoverUploadException = assertThrows(
                ResponseStatusException.class,
                () -> handoverController(mock(HandoverDocumentService.class), mock(DownloadFileUseCase.class), mock(JdbcTemplate.class))
                        .uploadHandoverDocument(9L, HandoverType.MOVE_IN, file)
        );


        assertEquals(HttpStatus.FORBIDDEN, leaseUploadException.getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, handoverUploadException.getStatusCode());
    }

    private static LeaseContractController leaseController(
            GetLeaseContractManagementUseCase managementUseCase,
            LeaseContractDocumentService documentService,
            DownloadFileUseCase downloadUseCase,
            JdbcTemplate jdbcTemplate
    ) {
        return new LeaseContractController(
                mock(GetRoomDetailsUseCase.class),
                mock(LeaseContractWebMapper.class),
                mock(GetMyListLeaseContractsUseCase.class),
                mock(GetLeaseContractDetailsUseCase.class),
                managementUseCase,
                mock(UploadSignedLeaseContractFileUseCase.class),
                mock(ActivateLeaseContractUseCase.class),
                mock(CreateDraftLeaseContractForDepositUseCase.class),
                mock(UpdateLeaseContractTermsUseCase.class),
                mock(CompleteLeaseLiquidationUseCase.class),
                mock(UpdateLeaseLiquidationDraftUseCase.class),
                mock(RenewLeaseContractUseCase.class),
                mock(RecordTenantIntentionUseCase.class),
                mock(ContractLifecycleChangeRequestService.class),
                mock(LeaseContractQueryService.class),
                documentService,
                downloadUseCase,
                mock(RoomCommitmentChecker.class),
                mock(PersonProfileRepository.class),
                jdbcTemplate
        );
    }

    private static ContractHandoverController handoverController(
            HandoverDocumentService handoverService,
            DownloadFileUseCase downloadUseCase,
            JdbcTemplate jdbcTemplate
    ) {
        return new ContractHandoverController(
                mock(ManageContractHandoverService.class),
                handoverService,
                downloadUseCase,
                jdbcTemplate
        );
    }

    private static LeaseContractManagementResponse.LeaseContractManagementResponseBuilder leaseResponse() {
        return LeaseContractManagementResponse.builder()
                .leaseContractId(9L)
                .contractCode("LC-2026-H101-9")
                .roomCode("101")
                .customerName("Nguyễn Văn A")
                .startDate(DOC_DATE);
    }

    private static FileDataResponse pdfData() {
        return new FileDataResponse("application/pdf", new ByteArrayResource(new byte[]{1, 2, 3}), true, null);
    }

    private static void assertAttachmentFilename(HttpHeaders headers, String filename) {
        String contentDisposition = headers.getFirst(HttpHeaders.CONTENT_DISPOSITION);
        assertTrue(contentDisposition != null && contentDisposition.startsWith("attachment; filename*=UTF-8''"));
        assertTrue(contentDisposition.contains(filename));
    }

    private static void assertAttachmentFilenameWithFallback(HttpHeaders headers, String filename) {
        String contentDisposition = headers.getFirst(HttpHeaders.CONTENT_DISPOSITION);
        assertTrue(contentDisposition != null && contentDisposition.startsWith("attachment; filename=\"" + filename + "\""));
        assertTrue(contentDisposition.contains("filename*=UTF-8''" + filename));
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
