package com.sep490.hdbhms.property.application.service;

import com.sep490.hdbhms.property.domain.value_objects.MeterType;
import com.sep490.hdbhms.property.domain.value_objects.ReadingStatus;
import com.sep490.hdbhms.property.infrastructure.persistence.entity.MeterReadingEntity;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaMeterReadingRepository;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaRoomRepository;
import com.sep490.hdbhms.property.application.service.GetLatestMeterReadingsService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        var response = service.getLatestReadings(roomId);

        assertEquals(new BigDecimal("345.500"), response.getElectricity().getPreviousValue());
        assertEquals(new BigDecimal("345.500"), response.getElectricity().getSuggestedValue());
        assertEquals(LocalDate.of(2026, 7, 1), response.getElectricity().getLastReadingDate());

        verify(meterReadingRepository)
                .findFirstByRoom_IdAndMeter_MeterTypeAndStatusNotOrderByReadingDateDescCreatedAtDescIdDesc(
                        roomId,
                        MeterType.ELECTRICITY,
                        ReadingStatus.VOIDED
                );
    }
}
