package com.sep490.hdbhms.occupancy.infrastructure.web.controller;

import com.sep490.hdbhms.billingandpayment.domain.valueObjects.DepositAgreementStatus;
import com.sep490.hdbhms.file.application.port.in.query.DownloadFileQuery;
import com.sep490.hdbhms.file.application.port.in.usecase.DownloadFileUseCase;
import com.sep490.hdbhms.file.application.port.in.usecase.UploadFileUseCase;
import com.sep490.hdbhms.file.infrastructure.persistence.jpa.JpaFileMetadataRepository;
import com.sep490.hdbhms.file.infrastructure.web.dto.response.FileDataResponse;
import com.sep490.hdbhms.identityandaccess.domain.valueObjects.Role;
import com.sep490.hdbhms.identityandaccess.infrastructure.config.security.UserPrincipal;
import com.sep490.hdbhms.occupancy.application.port.in.query.GetDepositAgreementDetailsQuery;
import com.sep490.hdbhms.occupancy.application.port.in.query.GetRoomDetailsQuery;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetDepositAgreementDetailsUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetLeaseContractDetailsUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetMyListDepositAgreementsUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetMyListLeaseContractsUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetRoomDetailsUseCase;
import com.sep490.hdbhms.occupancy.application.port.out.DepositAgreementRepository;
import com.sep490.hdbhms.occupancy.application.port.out.DepositFormRepository;
import com.sep490.hdbhms.occupancy.application.port.out.PropertyRepository;
import com.sep490.hdbhms.occupancy.application.port.out.RoomRepository;
import com.sep490.hdbhms.occupancy.application.service.DepositContractDocumentService;
import com.sep490.hdbhms.occupancy.application.service.HandoverDocumentService;
import com.sep490.hdbhms.occupancy.application.service.LeaseContractDocumentService;
import com.sep490.hdbhms.occupancy.application.service.LeaseContractManagementService;
import com.sep490.hdbhms.occupancy.application.service.LeaseContractQueryService;
import com.sep490.hdbhms.occupancy.application.service.ManageContractHandoverService;
import com.sep490.hdbhms.occupancy.application.service.RoomCommitmentChecker;
import com.sep490.hdbhms.occupancy.domain.model.DepositAgreement;
import com.sep490.hdbhms.occupancy.domain.model.DepositForm;
import com.sep490.hdbhms.occupancy.domain.model.Room;
import com.sep490.hdbhms.occupancy.domain.valueObjects.HandoverType;
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
import java.util.Optional;
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
    void downloadDraftDepositUsesRoomCodeHdcDateFilename() {
        setUser(1L, Role.OWNER);
        var getDetails = mock(GetDepositAgreementDetailsUseCase.class);
        var getRoom = mock(GetRoomDetailsUseCase.class);
        var depositFormRepository = mock(DepositFormRepository.class);
        var documentService = mock(DepositContractDocumentService.class);
        var controller = depositController(getRoom, depositFormRepository, getDetails, documentService, mock(DownloadFileUseCase.class));
        var agreement = depositAgreement(42L);

        when(getDetails.execute(any(GetDepositAgreementDetailsQuery.class))).thenReturn(agreement);
        when(getRoom.execute(any(GetRoomDetailsQuery.class))).thenReturn(room());
        when(depositFormRepository.findById(301L)).thenReturn(Optional.of(depositForm()));
        when(documentService.getOfficialContractFile(42L)).thenReturn(pdfData());

        var response = controller.downloadDepositDraftPdf(42L);

        assertAttachmentFilenameWithFallback(response.getHeaders(), "P101_HDC_29_06_2026.pdf");
    }

    @Test
    void downloadSignedDepositUsesStandardHdcFilename() {
        setUser(1L, Role.OWNER);
        var getDetails = mock(GetDepositAgreementDetailsUseCase.class);
        var getRoom = mock(GetRoomDetailsUseCase.class);
        var depositFormRepository = mock(DepositFormRepository.class);
        var downloadUseCase = mock(DownloadFileUseCase.class);
        var controller = depositController(getRoom, depositFormRepository, getDetails, mock(DepositContractDocumentService.class), downloadUseCase);

        when(getDetails.execute(any(GetDepositAgreementDetailsQuery.class))).thenReturn(depositAgreement(42L));
        when(getRoom.execute(any(GetRoomDetailsQuery.class))).thenReturn(room());
        when(depositFormRepository.findById(301L)).thenReturn(Optional.of(depositForm()));
        when(downloadUseCase.execute(new DownloadFileQuery(900L))).thenReturn(pdfData());

        var response = controller.downloadSignedDepositFile(42L);

        assertAttachmentFilename(response.getHeaders(), "P101_HDC_29_06_2026.pdf");
    }

    @Test
    void downloadDraftLeaseUsesStandardHdtFilename() {
        setUser(1L, Role.OWNER);
        var managementService = mock(LeaseContractManagementService.class);
        var documentService = mock(LeaseContractDocumentService.class);
        var controller = leaseController(managementService, documentService, mock(DownloadFileUseCase.class), mock(JdbcTemplate.class));

        when(managementService.findOne(9L)).thenReturn(leaseResponse().signedFileId(null).build());
        when(documentService.generateDraftPdf(9L)).thenReturn(new byte[]{1, 2, 3});

        var response = controller.getDraftPdf(9L);

        assertAttachmentFilename(response.getHeaders(), "HDT_P101_29.06.2026.pdf");
    }

    @Test
    void downloadSignedLeaseUsesStandardHdtFilename() {
        setUser(1L, Role.OWNER);
        var managementService = mock(LeaseContractManagementService.class);
        var downloadUseCase = mock(DownloadFileUseCase.class);
        var controller = leaseController(managementService, mock(LeaseContractDocumentService.class), downloadUseCase, mock(JdbcTemplate.class));

        when(managementService.findOne(9L)).thenReturn(leaseResponse().signedFileId(901L).build());
        when(downloadUseCase.execute(new DownloadFileQuery(901L))).thenReturn(pdfData());

        var response = controller.downloadSignedLeaseContractFile(9L);

        assertAttachmentFilename(response.getHeaders(), "HDT_P101_29.06.2026.pdf");
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

        assertAttachmentFilename(response.getHeaders(), "P101_Nguyen-Van-A_BBBG_29_06_2026.pdf");
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

        assertAttachmentFilename(response.getHeaders(), "P101_Nguyen-Van-A_BBBG_01_07_2026.pdf");
    }

    @Test
    void managerOutsidePropertyScopeCannotDownloadLeaseDocuments() {
        setUser(77L, Role.MANAGER);
        var jdbcTemplate = mock(JdbcTemplate.class);
        var documentService = mock(LeaseContractDocumentService.class);
        var controller = leaseController(mock(LeaseContractManagementService.class), documentService, mock(DownloadFileUseCase.class), jdbcTemplate);

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
                () -> leaseController(mock(LeaseContractManagementService.class), mock(LeaseContractDocumentService.class), mock(DownloadFileUseCase.class), mock(JdbcTemplate.class))
                        .uploadSignedFile(9L, file, false)
        );
        var handoverUploadException = assertThrows(
                ResponseStatusException.class,
                () -> handoverController(mock(HandoverDocumentService.class), mock(DownloadFileUseCase.class), mock(JdbcTemplate.class))
                        .uploadHandoverDocument(9L, HandoverType.MOVE_IN, file)
        );
        var depositUploadException = assertThrows(
                ResponseStatusException.class,
                () -> depositController(mock(GetRoomDetailsUseCase.class), mock(DepositFormRepository.class), mock(GetDepositAgreementDetailsUseCase.class), mock(DepositContractDocumentService.class), mock(DownloadFileUseCase.class))
                        .uploadSignedDepositFile(42L, file, null, null)
        );

        assertEquals(HttpStatus.FORBIDDEN, leaseUploadException.getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, handoverUploadException.getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, depositUploadException.getStatusCode());
    }

    private static DepositAgreementController depositController(
            GetRoomDetailsUseCase getRoom,
            DepositFormRepository depositFormRepository,
            GetDepositAgreementDetailsUseCase getDetails,
            DepositContractDocumentService documentService,
            DownloadFileUseCase downloadUseCase
    ) {
        return new DepositAgreementController(
                getRoom,
                mock(PropertyRepository.class),
                depositFormRepository,
                mock(DepositAgreementRepository.class),
                mock(RoomRepository.class),
                mock(GetMyListDepositAgreementsUseCase.class),
                getDetails,
                documentService,
                mock(UploadFileUseCase.class),
                downloadUseCase,
                mock(JpaFileMetadataRepository.class)
        );
    }

    private static LeaseContractController leaseController(
            LeaseContractManagementService managementService,
            LeaseContractDocumentService documentService,
            DownloadFileUseCase downloadUseCase,
            JdbcTemplate jdbcTemplate
    ) {
        return new LeaseContractController(
                mock(GetRoomDetailsUseCase.class),
                mock(LeaseContractWebMapper.class),
                mock(GetMyListLeaseContractsUseCase.class),
                mock(GetLeaseContractDetailsUseCase.class),
                managementService,
                mock(LeaseContractQueryService.class),
                documentService,
                downloadUseCase,
                mock(RoomCommitmentChecker.class),
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

    private static DepositAgreement depositAgreement(Long id) {
        return DepositAgreement.builder()
                .id(id)
                .depositCode("DC-2026-H101-9")
                .roomId(101L)
                .depositFormId(301L)
                .expectedMoveInDate(DOC_DATE)
                .status(DepositAgreementStatus.PAID)
                .signedFileId(900L)
                .build();
    }

    private static DepositForm depositForm() {
        return DepositForm.builder()
                .id(301L)
                .fullName("Nguyễn Văn A")
                .expectedMoveInDate(DOC_DATE)
                .build();
    }

    private static Room room() {
        return Room.builder()
                .id(101L)
                .propertyId(7L)
                .roomCode("101")
                .name("Phòng 101")
                .build();
    }

    private static LeaseContractManagementResponse.LeaseContractManagementResponseBuilder leaseResponse() {
        return LeaseContractManagementResponse.builder()
                .leaseContractId(9L)
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
