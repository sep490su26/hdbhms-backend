package com.sep490.hdbhms.occupancy.infrastructure.persistence.repository;

import com.sep490.hdbhms.occupancy.application.port.out.MeterRepository;
import com.sep490.hdbhms.occupancy.domain.model.Meter;
import com.sep490.hdbhms.occupancy.domain.value_objects.MeterStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.MeterType;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaMeterRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.mapper.MeterPersistenceMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SpringDataMeterRepository implements MeterRepository {

    JpaMeterRepository jpaMeterRepository;
    MeterPersistenceMapper meterPersistenceMapper;

    @Override
    public Optional<Meter> findFirstByRoomIdAndMeterTypeAndStatus(Long roomId, MeterType meterType, MeterStatus status) {
        return jpaMeterRepository.findFirstByRoom_IdAndMeterTypeAndStatus(roomId, meterType, status)
                .map(meterPersistenceMapper::toDomain);
    }
}
