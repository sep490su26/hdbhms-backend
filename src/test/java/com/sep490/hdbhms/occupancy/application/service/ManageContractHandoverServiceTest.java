package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.file.infrastructure.persistence.jpa.JpaFileMetadataRepository;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaUserRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RoomAssetEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaContractHandoverRecordRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaLeaseContractRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaMeterReadingRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaMeterRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomAssetRepository;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ManageContractHandoverServiceTest {

    private final JpaRoomAssetRepository roomAssetRepository = mock(JpaRoomAssetRepository.class);
    private final ManageContractHandoverService service = new ManageContractHandoverService(
            mock(JpaLeaseContractRepository.class),
            mock(JpaMeterReadingRepository.class),
            mock(JpaMeterRepository.class),
            mock(JpaContractHandoverRecordRepository.class),
            mock(JpaUserRepository.class),
            mock(JpaFileMetadataRepository.class),
            roomAssetRepository
    );

    @Test
    void softDeletesDistinctAssetsFromTheContractRoom() {
        RoomAssetEntity airConditioner = RoomAssetEntity.builder().id(10L).build();
        RoomAssetEntity remote = RoomAssetEntity.builder().id(11L).build();
        when(roomAssetRepository.findByIdAndRoom_IdAndDeletedAtIsNull(10L, 20L))
                .thenReturn(Optional.of(airConditioner));
        when(roomAssetRepository.findByIdAndRoom_IdAndDeletedAtIsNull(11L, 20L))
                .thenReturn(Optional.of(remote));

        service.softDeleteAssets(20L, List.of(10L, 11L, 10L));

        assertNotNull(airConditioner.getDeletedAt());
        assertNotNull(remote.getDeletedAt());
        assertEquals(airConditioner.getDeletedAt(), remote.getDeletedAt());
        verify(roomAssetRepository, times(1))
                .findByIdAndRoom_IdAndDeletedAtIsNull(10L, 20L);
        verify(roomAssetRepository).save(airConditioner);
        verify(roomAssetRepository).save(remote);
    }

    @Test
    void rejectsDeletionWhenAssetDoesNotBelongToTheContractRoom() {
        when(roomAssetRepository.findByIdAndRoom_IdAndDeletedAtIsNull(10L, 20L))
                .thenReturn(Optional.empty());

        AppException exception = assertThrows(
                AppException.class,
                () -> service.softDeleteAssets(20L, List.of(10L))
        );

        assertEquals(ApiErrorCode.ROOM_ASSET_NOT_FOUND, exception.getApiErrorCode());
        verify(roomAssetRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
