package com.sep490.hdbhms.file.infrastructure.web.controller;

import com.sep490.hdbhms.file.application.port.in.query.DownloadFileQuery;
import com.sep490.hdbhms.file.application.service.DownloadFileService;
import com.sep490.hdbhms.file.application.service.UploadBatchFileService;
import com.sep490.hdbhms.file.application.service.UploadFileService;
import com.sep490.hdbhms.file.infrastructure.web.dto.response.FileDataResponse;
import com.sep490.hdbhms.file.infrastructure.web.mapper.FileMetadataWebMapper;
import com.sep490.hdbhms.identityandaccess.domain.valueObjects.Role;
import com.sep490.hdbhms.identityandaccess.infrastructure.config.security.UserPrincipal;
import com.sep490.hdbhms.occupancy.application.service.LeaseContractQueryService;
import com.sep490.hdbhms.permissiongrant.application.service.PermissionGrantService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FileMetadataControllerSensitiveAccessTest {

    private final UploadFileService uploadFileService = mock(UploadFileService.class);
    private final DownloadFileService downloadFileService = mock(DownloadFileService.class);
    private final FileMetadataWebMapper fileMetadataWebMapper = mock(FileMetadataWebMapper.class);
    private final UploadBatchFileService uploadBatchFileService = mock(UploadBatchFileService.class);
    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final LeaseContractQueryService leaseContractQueryService = mock(LeaseContractQueryService.class);
    private final PermissionGrantService permissionGrantService = mock(PermissionGrantService.class);

    private final FileMetadataController controller = new FileMetadataController(
            uploadFileService,
            downloadFileService,
            fileMetadataWebMapper,
            uploadBatchFileService,
            jdbcTemplate,
            leaseContractQueryService,
            permissionGrantService
    );

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void tenantCanDownloadSensitiveHandoverItemFileWhenContractIsReadable() {
        setUser(88L, Role.TENANT);
        givenSensitiveFile(44L, 7L);
        givenLinkedHandoverContracts(44L, List.of(99L));

        var response = controller.download(44L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(leaseContractQueryService).assertCurrentUserCanReadContract(99L);
    }

    @Test
    void tenantCanDownloadSensitiveRoomAssetFileWhenRoomIsReadable() {
        setUser(88L, Role.TENANT);
        givenSensitiveFile(55L, 7L);
        givenLinkedHandoverContracts(55L, List.of());
        givenLinkedRoomAssetRooms(55L, List.of(15L));

        var response = controller.download(55L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(leaseContractQueryService).assertCurrentUserCanReadRoom(15L);
    }

    @Test
    void unrelatedTenantCannotDownloadSensitiveLinkedFile() {
        setUser(88L, Role.TENANT);
        givenSensitiveFile(66L, 7L);
        givenLinkedHandoverContracts(66L, List.of(99L));
        givenLinkedRoomAssetRooms(66L, List.of(15L));
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden"))
                .when(leaseContractQueryService).assertCurrentUserCanReadContract(99L);
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden"))
                .when(leaseContractQueryService).assertCurrentUserCanReadRoom(15L);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.download(66L)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void ownerCanDownloadSensitiveFileWithoutTenantLinkCheck() {
        setUser(1L, Role.OWNER);
        givenSensitiveFile(77L, 7L);

        var response = controller.download(77L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verifyNoInteractions(jdbcTemplate, leaseContractQueryService);
    }

    @Test
    void uploadOwnerCanDownloadOwnSensitiveFileWithoutTenantLinkCheck() {
        setUser(88L, Role.TENANT);
        givenSensitiveFile(88L, 88L);

        var response = controller.download(88L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verifyNoInteractions(jdbcTemplate, leaseContractQueryService);
    }

    private void givenSensitiveFile(Long fileId, Long ownerUserId) {
        when(downloadFileService.execute(eq(new DownloadFileQuery(fileId))))
                .thenReturn(new FileDataResponse(
                        "image/jpeg",
                        new ByteArrayResource(new byte[]{1, 2, 3}),
                        true,
                        ownerUserId
                ));
    }

    private void givenLinkedHandoverContracts(Long fileId, List<Long> contractIds) {
        when(jdbcTemplate.queryForList(
                contains("contract_handover_items"),
                eq(Long.class),
                eq(fileId),
                eq(fileId),
                eq(fileId)
        )).thenReturn(contractIds);
    }

    private void givenLinkedRoomAssetRooms(Long fileId, List<Long> roomIds) {
        when(jdbcTemplate.queryForList(
                contains("room_assets"),
                eq(Long.class),
                eq(fileId)
        )).thenReturn(roomIds);
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
