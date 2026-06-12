package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.file.application.port.out.FileMetadataRepository;
import com.sep490.hdbhms.identityandaccess.application.port.out.UserRepository;
import com.sep490.hdbhms.identityandaccess.domain.model.User;
import com.sep490.hdbhms.occupancy.application.port.in.command.SubmitBatchMeterReadingsCommand;
import com.sep490.hdbhms.occupancy.application.port.in.command.SubmitSingleMeterReadingCommand;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.SubmitMeterReadingUseCase;
import com.sep490.hdbhms.occupancy.application.port.out.*;
import com.sep490.hdbhms.occupancy.domain.model.*;
import com.sep490.hdbhms.occupancy.domain.value_objects.BatchSource;
import com.sep490.hdbhms.occupancy.domain.value_objects.BatchStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.MeterStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.MeterType;
import com.sep490.hdbhms.occupancy.domain.value_objects.ReadingSource;
import com.sep490.hdbhms.occupancy.domain.value_objects.ReadingStatus;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import com.sep490.hdbhms.shared.utils.AuthUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SubmitMeterReadingService implements SubmitMeterReadingUseCase {

    RoomRepository roomRepository;
    PropertyRepository propertyRepository;
    MeterRepository meterRepository;
    MeterReadingRepository meterReadingRepository;
    MeterReadingBatchRepository meterReadingBatchRepository;
    FileMetadataRepository fileMetadataRepository;
    UserRepository userRepository;

    @Override
    @Transactional
    public void submitSingleReading(SubmitSingleMeterReadingCommand command) {
        User currentUser = userRepository.findById(AuthUtils.getCurrentAuthenticationId())
                .orElseThrow(() -> new AppException(ApiErrorCode.ACCOUNT_NOT_FOUND));
        Room room = roomRepository.findById(command.roomId())
                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));

        // Submit electricity reading
        submitMeterValue(room, MeterType.ELECTRICITY, command.electricityValue(), 
                command.readingPeriod(), command.readingDate(), command.electricityPhotoId(), 
                null, currentUser);

        // Submit water reading
        submitMeterValue(room, MeterType.WATER, command.waterValue(), 
                command.readingPeriod(), command.readingDate(), command.waterPhotoId(), 
                null, currentUser);
    }

    @Override
    @Transactional
    public void submitBatchReadings(SubmitBatchMeterReadingsCommand command) {
        User currentUser = userRepository.findById(AuthUtils.getCurrentAuthenticationId())
                .orElseThrow(() -> new AppException(ApiErrorCode.ACCOUNT_NOT_FOUND));
        Property property = propertyRepository.findById(command.propertyId())
                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));

        // Create the batch record
        MeterReadingBatch batch = MeterReadingBatch.builder()
                .propertyId(property.getId())
                .readingPeriod(command.readingPeriod())
                .source(BatchSource.MANUAL)
                .status(BatchStatus.CONFIRMED)
                .createdById(currentUser.getId())
                .confirmedById(currentUser.getId())
                .confirmedAt(java.time.LocalDateTime.now())
                .build();
        batch = meterReadingBatchRepository.save(batch);

        for (SubmitBatchMeterReadingsCommand.RoomReading input : command.readings()) {
            Room room = roomRepository.findById(input.roomId())
                    .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));

            // Validate that room belongs to the property
            if (!room.getPropertyId().equals(property.getId())) {
                throw new AppException(ApiErrorCode.VISIT_002); // Invalid room property
            }

            submitMeterValue(room, MeterType.ELECTRICITY, input.electricityValue(), 
                    command.readingPeriod(), command.readingDate(), input.electricityPhotoId(), 
                    batch, currentUser);

            submitMeterValue(room, MeterType.WATER, input.waterValue(), 
                    command.readingPeriod(), command.readingDate(), input.waterPhotoId(), 
                    batch, currentUser);
        }
    }

    private void submitMeterValue(Room room, MeterType meterType, BigDecimal newValue,
                                  String period, LocalDate readingDate, Long photoId,
                                  MeterReadingBatch batch, User currentUser) {
        // Find active meter for the room
        Meter activeMeter = meterRepository.findFirstByRoomIdAndMeterTypeAndStatus(
                room.getId(), meterType, MeterStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ApiErrorCode.METER_NOT_FOUND));

        // Get previous value (latest reading currentValue)
        var latestReadingOpt = meterReadingRepository.findFirstByRoomIdAndMeterTypeOrderByReadingDateDesc(
                room.getId(), meterType);
        BigDecimal prevValue = latestReadingOpt.map(MeterReading::getCurrentValue).orElse(BigDecimal.ZERO);

        // Validation: New value must be >= previous value
        if (newValue.compareTo(prevValue) < 0) {
            throw new AppException(ApiErrorCode.UNDEFINED); // Or a specific warning code if defined
        }

        // Determine next revision number for the period
        int nextRevision = 1;
        var existingPeriodReadingOpt = meterReadingRepository.findFirstByMeterIdAndReadingPeriodOrderByRevisionNoDesc(
                activeMeter.getId(), period);
        if (existingPeriodReadingOpt.isPresent()) {
            nextRevision = existingPeriodReadingOpt.get().getRevisionNo() + 1;
        }

        MeterReading reading = MeterReading.builder()
                .meterId(activeMeter.getId())
                .roomId(room.getId())
                .readingPeriod(period)
                .revisionNo(nextRevision)
                .previousValue(prevValue)
                .currentValue(newValue)
                .readingDate(readingDate)
                .source(ReadingSource.MANUAL)
                .status(ReadingStatus.CONFIRMED)
                .batchId(batch != null ? batch.getId() : null)
                .createdById(currentUser.getId())
                .photoFileId(photoId)
                .build();

        meterReadingRepository.save(reading);
    }
}
