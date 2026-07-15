package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.occupancy.domain.value_objects.MeterType;
import com.sep490.hdbhms.occupancy.domain.value_objects.ReadingStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.MeterReadingEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaMeterReadingRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomRepository;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.LatestMeterReadingsResponse;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GetLatestMeterReadingsService {

    JpaMeterReadingRepository meterReadingRepository;
    JpaRoomRepository roomRepository;

    @Transactional(readOnly = true)
    public LatestMeterReadingsResponse getLatestReadings(Long roomId) {
        if (!roomRepository.existsById(roomId)) {
            throw new AppException(ApiErrorCode.UNDEFINED);
        }

        return LatestMeterReadingsResponse.builder()
                .electricity(buildReadingDetail(roomId, MeterType.ELECTRICITY))
                .water(buildReadingDetail(roomId, MeterType.WATER))
                .build();
    }

    private LatestMeterReadingsResponse.ReadingDetail buildReadingDetail(Long roomId, MeterType meterType) {
        var latestReadingOpt = meterReadingRepository
                .findFirstByRoom_IdAndMeter_MeterTypeAndStatusNotOrderByReadingDateDescCreatedAtDescIdDesc(
                        roomId,
                        meterType,
                        ReadingStatus.VOIDED
                );

        if (latestReadingOpt.isEmpty()) {
            return LatestMeterReadingsResponse.ReadingDetail.builder()
                    .previousValue(BigDecimal.ZERO)
                    .suggestedValue(BigDecimal.ZERO)
                    .lastReadingDate(null)
                    .build();
        }

        MeterReadingEntity latestReading = latestReadingOpt.get();
        return LatestMeterReadingsResponse.ReadingDetail.builder()
                .previousValue(latestReading.getCurrentValue())
                .suggestedValue(latestReading.getCurrentValue()) // Gợi ý bằng số cũ
                .lastReadingDate(latestReading.getReadingDate())
                .build();
    }
}
