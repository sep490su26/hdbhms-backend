package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.file.infrastructure.persistence.entity.FileMetadataEntity;
import com.sep490.hdbhms.identityandaccess.domain.valueObjects.Role;
import com.sep490.hdbhms.identityandaccess.infrastructure.config.security.UserPrincipal;
import com.sep490.hdbhms.occupancy.domain.valueObjects.AssetCondition;
import com.sep490.hdbhms.occupancy.domain.valueObjects.HandoverStatus;
import com.sep490.hdbhms.occupancy.domain.valueObjects.HandoverType;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.ContractHandoverItemEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.ContractHandoverRecordEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeaseContractEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RoomAssetEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RoomEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaContractHandoverItemRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaContractHandoverRecordRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaLeaseContractRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomAssetRepository;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TenantHandoverServiceTest {

    private final JpaLeaseContractRepository leaseContractRepository = mock(JpaLeaseContractRepository.class);
    private final JpaContractHandoverRecordRepository handoverRecordRepository = mock(JpaContractHandoverRecordRepository.class);
    private final JpaContractHandoverItemRepository handoverItemRepository = mock(JpaContractHandoverItemRepository.class);
    private final JpaRoomAssetRepository roomAssetRepository = mock(JpaRoomAssetRepository.class);
    private final LeaseContractQueryService leaseContractQueryService = mock(LeaseContractQueryService.class);

    private final TenantHandoverService service = new TenantHandoverService(
            leaseContractRepository,
            handoverRecordRepository,
            handoverItemRepository,
            roomAssetRepository,
            leaseContractQueryService
    );

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsHandoverItemsWithEvidenceFileUrls() {
        setUser(88L, Role.TENANT);
        RoomEntity room = RoomEntity.builder().id(15L).roomCode("201").name("Room 201").build();
        LeaseContractEntity contract = LeaseContractEntity.builder().id(99L).room(room).contractCode("HD-201").build();
        ContractHandoverRecordEntity record = ContractHandoverRecordEntity.builder()
                .id(5L)
                .contract(contract)
                .room(room)
                .handoverType(HandoverType.MOVE_IN)
                .status(HandoverStatus.CONFIRMED)
                .handoverDate(LocalDateTime.of(2026, 1, 2, 9, 0))
                .build();
        FileMetadataEntity evidenceFile = FileMetadataEntity.builder().id(44L).originalName("aircon.jpg").build();
        ContractHandoverItemEntity item = ContractHandoverItemEntity.builder()
                .id(7L)
                .handoverRecord(record)
                .assetName("Air conditioner")
                .quantity(1)
                .conditionStatus(AssetCondition.GOOD)
                .note("Remote included")
                .evidenceFile(evidenceFile)
                .build();

        when(leaseContractRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.of(contract));
        when(handoverRecordRepository.findFirstByContract_IdAndHandoverTypeOrderByCreatedAtDesc(99L, HandoverType.MOVE_IN))
                .thenReturn(Optional.of(record));
        when(handoverItemRepository.findWithEvidenceFileByHandoverRecordId(5L)).thenReturn(List.of(item));

        var response = service.getHandoverItems(99L, HandoverType.MOVE_IN);

        assertEquals(5L, response.getHandoverRecordId());
        assertEquals(1, response.getItems().size());
        assertEquals("Air conditioner", response.getItems().getFirst().getAssetName());
        assertEquals("/api/v1/files/download/44", response.getItems().getFirst().getEvidenceFileUrl());
        verify(leaseContractQueryService).assertCurrentUserCanReadContract(99L);
        verify(leaseContractQueryService).assertCurrentUserCanReadRoom(15L);
        verify(roomAssetRepository, never()).findActiveByRoomId(15L);
    }

    @Test
    void fallsBackToRoomAssetsWhenHandoverItemsAreEmpty() {
        setUser(88L, Role.TENANT);
        RoomEntity room = RoomEntity.builder().id(15L).roomCode("201").name("Room 201").build();
        LeaseContractEntity contract = LeaseContractEntity.builder().id(99L).room(room).contractCode("HD-201").build();
        ContractHandoverRecordEntity record = ContractHandoverRecordEntity.builder()
                .id(5L)
                .contract(contract)
                .room(room)
                .handoverType(HandoverType.MOVE_IN)
                .status(HandoverStatus.CONFIRMED)
                .handoverDate(LocalDateTime.now())
                .build();
        FileMetadataEntity imageFile = FileMetadataEntity.builder().id(55L).originalName("desk.jpg").build();
        RoomAssetEntity asset = RoomAssetEntity.builder()
                .id(12L)
                .room(room)
                .assetName("Desk")
                .quantity(2)
                .currentCondition(AssetCondition.ATTENTION)
                .description("Small scratch")
                .imageFile(imageFile)
                .build();

        when(leaseContractRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.of(contract));
        when(handoverRecordRepository.findFirstByContract_IdAndHandoverTypeOrderByCreatedAtDesc(99L, HandoverType.MOVE_IN))
                .thenReturn(Optional.of(record));
        when(handoverItemRepository.findWithEvidenceFileByHandoverRecordId(5L)).thenReturn(List.of());
        when(roomAssetRepository.findActiveByRoomId(15L)).thenReturn(List.of(asset));

        var response = service.getHandoverItems(99L, HandoverType.MOVE_IN);

        assertEquals(1, response.getItems().size());
        assertEquals("Desk", response.getItems().getFirst().getAssetName());
        assertEquals(AssetCondition.ATTENTION, response.getItems().getFirst().getConditionStatus());
        assertEquals("/api/v1/files/download/55", response.getItems().getFirst().getEvidenceFileUrl());
    }

    @Test
    void throwsWhenHandoverRecordDoesNotExist() {
        setUser(88L, Role.TENANT);
        RoomEntity room = RoomEntity.builder().id(15L).roomCode("201").name("Room 201").build();
        LeaseContractEntity contract = LeaseContractEntity.builder().id(99L).room(room).contractCode("HD-201").build();

        when(leaseContractRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.of(contract));
        when(handoverRecordRepository.findFirstByContract_IdAndHandoverTypeOrderByCreatedAtDesc(99L, HandoverType.MOVE_IN))
                .thenReturn(Optional.empty());

        AppException exception = assertThrows(
                AppException.class,
                () -> service.getHandoverItems(99L, HandoverType.MOVE_IN)
        );

        assertEquals(ApiErrorCode.CONTRACT_HANDOVER_RECORD_NOT_FOUND, exception.getApiErrorCode());
    }

    @Test
    void propagatesForbiddenWhenTenantCannotReadRoom() {
        setUser(88L, Role.TENANT);
        RoomEntity room = RoomEntity.builder().id(15L).roomCode("201").name("Room 201").build();
        LeaseContractEntity contract = LeaseContractEntity.builder().id(99L).room(room).contractCode("HD-201").build();

        when(leaseContractRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.of(contract));
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden"))
                .when(leaseContractQueryService).assertCurrentUserCanReadRoom(15L);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.getHandoverItems(99L, HandoverType.MOVE_IN)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(handoverRecordRepository, never())
                .findFirstByContract_IdAndHandoverTypeOrderByCreatedAtDesc(99L, HandoverType.MOVE_IN);
    }

    @Test
    void returnsEmptyListWhenItemsAndFallbackAssetsAreEmpty() {
        setUser(88L, Role.TENANT);
        RoomEntity room = RoomEntity.builder().id(15L).roomCode("201").name("Room 201").build();
        LeaseContractEntity contract = LeaseContractEntity.builder().id(99L).room(room).contractCode("HD-201").build();
        ContractHandoverRecordEntity record = ContractHandoverRecordEntity.builder()
                .id(5L)
                .contract(contract)
                .room(room)
                .handoverType(HandoverType.MOVE_IN)
                .status(HandoverStatus.CONFIRMED)
                .handoverDate(LocalDateTime.now())
                .build();

        when(leaseContractRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.of(contract));
        when(handoverRecordRepository.findFirstByContract_IdAndHandoverTypeOrderByCreatedAtDesc(99L, HandoverType.MOVE_IN))
                .thenReturn(Optional.of(record));
        when(handoverItemRepository.findWithEvidenceFileByHandoverRecordId(5L)).thenReturn(List.of());
        when(roomAssetRepository.findActiveByRoomId(15L)).thenReturn(List.of());

        var response = service.getHandoverItems(99L, HandoverType.MOVE_IN);

        assertEquals(0, response.getItems().size());
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
