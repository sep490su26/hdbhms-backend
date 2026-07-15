package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.occupancy.domain.value_objects.MeterType;
import com.sep490.hdbhms.occupancy.domain.value_objects.ReadingStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.MeterReadingEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaMeterReadingRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GetLatestMeterReadingsServiceTest {

    private final JpaMeterReadingRepository meterReadingRepository = mock(JpaMeterReadingRepository.class);
    private final JpaRoomRepository roomRepository = mock(JpaRoomRepository.class);
    private final GetLatestMeterReadingsService service = new GetLatestMeterReadingsService(
            meterReadingRepository,
            roomRepository
    );

    @Test
    void returnsLatestActiveReadingAndZeroWhenMeterHasNoReading() {
        long roomId = 23L;
        var latestElectricity = MeterReadingEntity.builder()
                .currentValue(new BigDecimal("345.500"))
                .readingDate(LocalDate.of(2026, 7, 1))
                .status(ReadingStatus.CONFIRMED)
                .build();

        when(roomRepository.existsById(roomId)).thenReturn(true);
        when(meterReadingRepository
                .findFirstByRoom_IdAndMeter_MeterTypeAndStatusNotOrderByReadingDateDescCreatedAtDescIdDesc(
                        roomId,
                        MeterType.ELECTRICITY,
                        ReadingStatus.VOIDED
                ))
                .thenReturn(Optional.of(latestElectricity));
        when(meterReadingRepository
                .findFirstByRoom_IdAndMeter_MeterTypeAndStatusNotOrderByReadingDateDescCreatedAtDescIdDesc(
                        roomId,
                        MeterType.WATER,
                        ReadingStatus.VOIDED
                ))
                .thenReturn(Optional.empty());

        var response = service.getLatestReadings(roomId);

        assertEquals(new BigDecimal("345.500"), response.getElectricity().getPreviousValue());
        assertEquals(new BigDecimal("345.500"), response.getElectricity().getSuggestedValue());
        assertEquals(LocalDate.of(2026, 7, 1), response.getElectricity().getLastReadingDate());
        assertEquals(BigDecimal.ZERO, response.getWater().getPreviousValue());
        assertEquals(BigDecimal.ZERO, response.getWater().getSuggestedValue());
        assertNull(response.getWater().getLastReadingDate());

        verify(meterReadingRepository)
                .findFirstByRoom_IdAndMeter_MeterTypeAndStatusNotOrderByReadingDateDescCreatedAtDescIdDesc(
                        roomId,
                        MeterType.ELECTRICITY,
                        ReadingStatus.VOIDED
                );
        verify(meterReadingRepository)
                .findFirstByRoom_IdAndMeter_MeterTypeAndStatusNotOrderByReadingDateDescCreatedAtDescIdDesc(
                        roomId,
                        MeterType.WATER,
                        ReadingStatus.VOIDED
                );
    }
}
