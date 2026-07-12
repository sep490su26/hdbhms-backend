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
        String readingPeriod = MeterReadingPeriod.normalize(command.readingPeriod());
        User currentUser = userRepository.findById(AuthUtils.getCurrentAuthenticationId())
                .orElseThrow(() -> new AppException(ApiErrorCode.ACCOUNT_NOT_FOUND));
        Room room = roomRepository.findById(command.roomId())
                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));

        // Submit electricity reading
        submitMeterValue(room, MeterType.ELECTRICITY, command.electricityValue(), 
                readingPeriod, command.readingDate(), command.electricityPhotoId(),
                null, currentUser);

        // Submit water reading
        submitMeterValue(room, MeterType.WATER, command.waterValue(), 
                readingPeriod, command.readingDate(), command.waterPhotoId(),
                null, currentUser);
    }

    @Override
    @Transactional
    public void submitBatchReadings(SubmitBatchMeterReadingsCommand command) {
        String readingPeriod = MeterReadingPeriod.normalize(command.readingPeriod());
        User currentUser = userRepository.findById(AuthUtils.getCurrentAuthenticationId())
                .orElseThrow(() -> new AppException(ApiErrorCode.ACCOUNT_NOT_FOUND));
        Property property = propertyRepository.findById(command.propertyId())
                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));

        // Create the batch record
        MeterReadingBatch batch = MeterReadingBatch.builder()
                .propertyId(property.getId())
                .readingPeriod(readingPeriod)
//                .source(BatchSource.MANUAL)
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
                    readingPeriod, command.readingDate(), input.electricityPhotoId(),
                    batch, currentUser);

            submitMeterValue(room, MeterType.WATER, input.waterValue(), 
                    readingPeriod, command.readingDate(), input.waterPhotoId(),
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

    @Override
    @Transactional
    public Long startBatch(String period, Long propertyId) {
        String readingPeriod = MeterReadingPeriod.normalize(period);
        User currentUser = userRepository.findById(AuthUtils.getCurrentAuthenticationId())
                .orElseThrow(() -> new AppException(ApiErrorCode.ACCOUNT_NOT_FOUND));
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));

        // Check if draft or confirmed batch already exists for this period
        // For simplicity, we just check if any batch exists
        var existingBatches = meterReadingBatchRepository.findByProperty_IdOrderByReadingPeriodDesc(propertyId).stream()
                .filter(b -> MeterReadingPeriod.normalize(b.getReadingPeriod()).equals(readingPeriod))
                .toList();

        if (!existingBatches.isEmpty()) {
            return existingBatches.get(0).getId();
        }

        MeterReadingBatch batch = MeterReadingBatch.builder()
                .propertyId(property.getId())
                .readingPeriod(readingPeriod)
                .status(BatchStatus.DRAFT)
                .createdById(currentUser.getId())
                .build();
        batch = meterReadingBatchRepository.save(batch);
        return batch.getId();
    }

    @Override
    @Transactional
    public void saveProgressiveRoomReading(Long batchId, Long roomId, BigDecimal electricityValue, BigDecimal waterValue, Long elecPhotoId, Long waterPhotoId) {
        User currentUser = userRepository.findById(AuthUtils.getCurrentAuthenticationId())
                .orElseThrow(() -> new AppException(ApiErrorCode.ACCOUNT_NOT_FOUND));

        MeterReadingBatch batch = meterReadingBatchRepository.findById(batchId)
                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));

        if (batch.getStatus() != BatchStatus.DRAFT) {
            throw new AppException(ApiErrorCode.UNDEFINED); // Cannot edit confirmed batch
        }

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));

        if (!room.getPropertyId().equals(batch.getPropertyId())) {
            throw new AppException(ApiErrorCode.VISIT_002);
        }

        // Electricity
        saveOrUpdateMeterValue(room, MeterType.ELECTRICITY, electricityValue, batch, elecPhotoId, currentUser);
        
        // Water
        saveOrUpdateMeterValue(room, MeterType.WATER, waterValue, batch, waterPhotoId, currentUser);
    }

    private void saveOrUpdateMeterValue(Room room, MeterType meterType, BigDecimal newValue, MeterReadingBatch batch, Long photoId, User currentUser) {
        if (newValue == null) return;

        Meter activeMeter = meterRepository.findFirstByRoomIdAndMeterTypeAndStatus(
                room.getId(), meterType, MeterStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ApiErrorCode.METER_NOT_FOUND));

        var latestReadingOpt = meterReadingRepository.findFirstByRoomIdAndMeterTypeOrderByReadingDateDesc(
                room.getId(), meterType);
        
        // If the latest reading belongs to THIS batch, we need to find the one BEFORE this batch
        BigDecimal prevValue = BigDecimal.ZERO;
        if (latestReadingOpt.isPresent()) {
            if (batch.getId().equals(latestReadingOpt.get().getBatchId())) {
                // Find reading before this one
                var previousReadings = meterReadingRepository.findByMeterIdAndReadingDateBeforeOrderByReadingDateDesc(
                    activeMeter.getId(), latestReadingOpt.get().getReadingDate()
                );
                if (!previousReadings.isEmpty()) {
                    prevValue = previousReadings.getFirst().getCurrentValue();
                }
            } else {
                prevValue = latestReadingOpt.get().getCurrentValue();
            }
        }

        if (newValue.compareTo(prevValue) < 0) {
            throw new AppException(ApiErrorCode.UNDEFINED); 
        }

        // Check if there is already a reading in this batch
        var existingReadingOpt = meterReadingRepository.findByMeterIdAndBatchId(activeMeter.getId(), batch.getId());
        
        if (existingReadingOpt.isPresent()) {
            MeterReading existing = existingReadingOpt.get();
            existing.setCurrentValue(newValue);
            existing.setPhotoFileId(photoId);
            meterReadingRepository.save(existing);
        } else {
            MeterReading reading = MeterReading.builder()
                    .meterId(activeMeter.getId())
                    .roomId(room.getId())
                    .readingPeriod(batch.getReadingPeriod())
                    .revisionNo(1)
                    .previousValue(prevValue)
                    .currentValue(newValue)
                    .readingDate(LocalDate.now())
                    .source(ReadingSource.MANUAL)
                    .status(ReadingStatus.CONFIRMED)
                    .batchId(batch.getId())
                    .createdById(currentUser.getId())
                    .photoFileId(photoId)
                    .build();
            meterReadingRepository.save(reading);
        }
    }

    @Override
    @Transactional
    public void confirmBatch(Long batchId) {
        User currentUser = userRepository.findById(AuthUtils.getCurrentAuthenticationId())
                .orElseThrow(() -> new AppException(ApiErrorCode.ACCOUNT_NOT_FOUND));

        MeterReadingBatch batch = meterReadingBatchRepository.findById(batchId)
                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));

        if (batch.getStatus() == BatchStatus.CONFIRMED) {
            return;
        }

        // Validate no pending rooms...
        // Simplified: just update status
        batch.setStatus(BatchStatus.CONFIRMED);
        batch.setConfirmedById(currentUser.getId());
        batch.setConfirmedAt(java.time.LocalDateTime.now());

        meterReadingBatchRepository.save(batch);
    }
}
