package com.sep490.hdbhms.property.application.service;

import com.sep490.hdbhms.billingandpayment.application.service.UtilityBillingRunService;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceReason;
import com.sep490.hdbhms.file.application.port.out.FileMetadataRepository;
import com.sep490.hdbhms.file.application.port.in.command.UploadFileCommand;
import com.sep490.hdbhms.file.application.port.in.usecase.UploadFileUseCase;
import com.sep490.hdbhms.file.domain.model.FileMetadata;
import com.sep490.hdbhms.file.domain.value_objects.FileCategory;
import com.sep490.hdbhms.identityandaccess.application.port.out.UserRepository;
import com.sep490.hdbhms.identityandaccess.domain.model.User;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaUserRepository;
import com.sep490.hdbhms.property.application.port.in.command.SubmitBatchMeterReadingsCommand;
import com.sep490.hdbhms.property.application.port.in.command.SubmitSingleMeterReadingCommand;
import com.sep490.hdbhms.property.application.port.in.usecase.SubmitMeterReadingUseCase;
import com.sep490.hdbhms.occupancy.application.port.out.LeaseContractRepository;
import com.sep490.hdbhms.property.application.port.out.MeterReadingBatchRepository;
import com.sep490.hdbhms.property.application.port.out.MeterReadingRepository;
import com.sep490.hdbhms.property.application.port.out.MeterRepository;
import com.sep490.hdbhms.property.application.port.out.PropertyRepository;
import com.sep490.hdbhms.property.application.port.out.RoomRepository;
import com.sep490.hdbhms.property.domain.value_objects.BatchStatus;
import com.sep490.hdbhms.property.domain.value_objects.AnomalyType;
import com.sep490.hdbhms.property.domain.value_objects.MeterReadingReviewStatus;
import com.sep490.hdbhms.property.domain.value_objects.MeterStatus;
import com.sep490.hdbhms.property.domain.value_objects.MeterType;
import com.sep490.hdbhms.property.domain.value_objects.ReadingSource;
import com.sep490.hdbhms.property.domain.value_objects.ReadingStatus;
import com.sep490.hdbhms.property.infrastructure.persistence.entity.MeterReadingAnomalyEntity;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaMeterReadingAnomalyRepository;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaMeterReadingBatchRepository;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaMeterReadingRepository;
import com.sep490.hdbhms.property.domain.model.*;
import com.sep490.hdbhms.property.infrastructure.web.dto.response.MeterReadingExcelImportResponse;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import com.sep490.hdbhms.shared.utils.AuthUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.math.BigDecimal;
import java.io.IOException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SubmitMeterReadingService implements SubmitMeterReadingUseCase {
    private static final int IMPORT_HEADER_SCAN_ROWS = 10;

    RoomRepository roomRepository;
    PropertyRepository propertyRepository;
    LeaseContractRepository leaseContractRepository;
    MeterRepository meterRepository;
    MeterReadingRepository meterReadingRepository;
    MeterReadingBatchRepository meterReadingBatchRepository;
    FileMetadataRepository fileMetadataRepository;
    UploadFileUseCase uploadFileUseCase;
    UserRepository userRepository;
    JpaUserRepository jpaUserRepository;
    JpaMeterReadingAnomalyRepository meterReadingAnomalyRepository;
    JpaMeterReadingRepository jpaMeterReadingRepository;
    JpaMeterReadingBatchRepository jpaMeterReadingBatchRepository;
    UtilityBillingRunService utilityBillingRunService;
    MeterReadingAnomalyPolicy anomalyPolicy;
    MeterUsageCalculator meterUsageCalculator;
    JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void submitSingleReading(SubmitSingleMeterReadingCommand command) {
        String readingPeriod = MeterReadingPeriod.normalize(command.readingPeriod());
        User currentUser = userRepository.findById(AuthUtils.getCurrentAuthenticationId())
                .orElseThrow(() -> new AppException(ApiErrorCode.ACCOUNT_NOT_FOUND));
        Room room = roomRepository.findById(command.roomId())
                .orElseThrow(() -> new AppException(ApiErrorCode.ROOM_NOT_FOUND));
        assertRoomRequiresMeterReading(room.getPropertyId(), room.getId(), readingPeriod);
        assertMonthlyInvoicesNotIssued(room.getPropertyId(), readingPeriod);

        // Submit electricity reading
        submitMeterValue(room, MeterType.ELECTRICITY, command.electricityValue(), 
                readingPeriod, command.readingDate(), command.electricityPhotoId(),
                null, currentUser);
        utilityBillingRunService.refreshMonthlyPreviewIfOpen(
                room.getPropertyId(),
                MeterReadingPeriod.parse(readingPeriod).toString(),
                currentUser.getId()
        );

    }

    @Override
    @Transactional
    public void submitBatchReadings(SubmitBatchMeterReadingsCommand command) {
        String readingPeriod = MeterReadingPeriod.normalize(command.readingPeriod());
        User currentUser = userRepository.findById(AuthUtils.getCurrentAuthenticationId())
                .orElseThrow(() -> new AppException(ApiErrorCode.ACCOUNT_NOT_FOUND));
        Property property = propertyRepository.findById(command.propertyId())
                .orElseThrow(() -> new AppException(ApiErrorCode.PROPERTY_NOT_FOUND));
        assertMonthlyInvoicesNotIssued(property.getId(), readingPeriod);
        int totalRooms = requireMeterReadingRooms(property.getId(), readingPeriod);

        // Create the batch record
        MeterReadingBatch batch = MeterReadingBatch.builder()
                .propertyId(property.getId())
                .readingPeriod(readingPeriod)
                .totalRooms(totalRooms)
                .status(BatchStatus.DRAFT)
                .createdById(currentUser.getId())
                .build();
        batch = meterReadingBatchRepository.save(batch);

        for (SubmitBatchMeterReadingsCommand.RoomReading input : command.readings()) {
            Room room = roomRepository.findById(input.roomId())
                    .orElseThrow(() -> new AppException(ApiErrorCode.ROOM_NOT_FOUND));

            // Validate that room belongs to the property
            if (!room.getPropertyId().equals(property.getId())) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
            }
            assertRoomRequiresMeterReading(property.getId(), room.getId(), readingPeriod);

            submitMeterValue(room, MeterType.ELECTRICITY, input.electricityValue(), 
                    readingPeriod, command.readingDate(), input.electricityPhotoId(),
                    batch, currentUser);

        }
        createMonthlyUtilityBillingBatch(batch, currentUser.getId());
    }

    private void submitMeterValue(Room room, MeterType meterType, BigDecimal newValue,
                                  String period, LocalDate readingDate, Long photoId,
                                  MeterReadingBatch batch, User currentUser) {
        submitMeterValue(room, meterType, newValue, period, readingDate, photoId,
                batch, currentUser, ReadingSource.MANUAL);
    }

    private void submitMeterValue(Room room, MeterType meterType, BigDecimal newValue,
                                  String period, LocalDate readingDate, Long photoId,
                                  MeterReadingBatch batch,
                                  User currentUser, ReadingSource source) {
        requireMeterValue(newValue);

        // Find active meter for the room
        Meter activeMeter = meterRepository.findFirstByRoomIdAndMeterTypeAndStatus(
                room.getId(), meterType, MeterStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ApiErrorCode.METER_NOT_FOUND));

        // Get previous value (latest reading currentValue)
        var latestReadingOpt = meterReadingRepository.findFirstByMeterIdOrderByReadingDateDesc(activeMeter.getId());
        MeterReading previousReading = latestReadingOpt.orElse(null);
        BigDecimal prevValue = previousReading == null ? BigDecimal.ZERO : previousReading.getCurrentValue();
        BigDecimal previousCycleUsage = usageOf(previousReading);
        requireMeterValueWithinCapacity(newValue, activeMeter.getCounterCapacity());

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
                .source(source)
                .status(ReadingStatus.CONFIRMED)
                .batchId(batch != null ? batch.getId() : null)
                .createdById(currentUser.getId())
                .photoFileId(photoId)
                .build();

        applyUsageCalculation(reading, activeMeter.getCounterCapacity());

        saveReadingWithReview(
                reading,
                meterType,
                previousCycleUsage,
                room.getPropertyId(),
                activeMeter.getCounterCapacity()
        );
    }

    @Override
    @Transactional
    public Long startBatch(String period, Long propertyId) {
        String readingPeriod = MeterReadingPeriod.normalize(period);
        User currentUser = userRepository.findById(AuthUtils.getCurrentAuthenticationId())
                .orElseThrow(() -> new AppException(ApiErrorCode.ACCOUNT_NOT_FOUND));
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new AppException(ApiErrorCode.PROPERTY_NOT_FOUND));
        int totalRooms = requireMeterReadingRooms(property.getId(), readingPeriod);

        Optional<MeterReadingBatch> existingBatch = selectPreferredBatch(
                meterReadingBatchRepository.findByPropertyIdAndReadingPeriodOrderByIdDesc(propertyId, readingPeriod)
        );
        if (existingBatch.isPresent()) return existingBatch.get().getId();

        MeterReadingBatch batch = MeterReadingBatch.builder()
                .propertyId(property.getId())
                .readingPeriod(readingPeriod)
                .totalRooms(totalRooms)
                .status(BatchStatus.DRAFT)
                .createdById(currentUser.getId())
                .build();
        batch = meterReadingBatchRepository.save(batch);
        return batch.getId();
    }

    static Optional<MeterReadingBatch> selectPreferredBatch(List<MeterReadingBatch> batches) {
        return batches.stream()
                .max(Comparator
                        .comparingInt(SubmitMeterReadingService::statusRank)
                        .thenComparing(batch -> batch.getId() == null ? 0L : batch.getId()));
    }

    private static int statusRank(MeterReadingBatch batch) {
        String status = batch.getStatus() == null ? "" : batch.getStatus().name();
        return switch (status) {
            case "DRAFT" -> 4;
            case "PREVIEWED" -> 3;
            case "CONFIRMED" -> 2;
            case "CANCELLED" -> 1;
            default -> 0;
        };
    }

    @Override
    @Transactional
    public void saveProgressiveRoomReading(
            Long batchId, Long roomId, BigDecimal electricityValue, Long elecPhotoId) {
        User currentUser = userRepository.findById(AuthUtils.getCurrentAuthenticationId())
                .orElseThrow(() -> new AppException(ApiErrorCode.ACCOUNT_NOT_FOUND));

        MeterReadingBatch batch = meterReadingBatchRepository.findById(batchId)
                .orElseThrow(() -> new AppException(ApiErrorCode.METER_READING_BATCH_NOT_FOUND));

        if (batch.getStatus() == BatchStatus.CANCELLED) {
            throw new AppException(ApiErrorCode.METER_READING_BATCH_CANCELLED);
        }
        assertBatchReadingsEditable(batch);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(ApiErrorCode.ROOM_NOT_FOUND));

        if (!room.getPropertyId().equals(batch.getPropertyId())) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        assertRoomRequiresMeterReading(batch.getPropertyId(), room.getId(), batch.getReadingPeriod());

        // Electricity
        saveOrUpdateMeterValue(
                room, MeterType.ELECTRICITY, electricityValue, batch, elecPhotoId, currentUser,
                ReadingSource.MANUAL, LocalDate.now()
        );
        
        refreshBatchProgress(batch.getId(), batch.getPropertyId(), batch.getReadingPeriod());
        refreshMonthlyPreview(batch, currentUser.getId());
    }

    @Override
    @Transactional
    public MeterReadingExcelImportResponse importExcel(Long batchId, MultipartFile file) {
        User currentUser = userRepository.findById(AuthUtils.getCurrentAuthenticationId())
                .orElseThrow(() -> new AppException(ApiErrorCode.ACCOUNT_NOT_FOUND));
        MeterReadingBatch batch = meterReadingBatchRepository.findById(batchId)
                .orElseThrow(() -> new AppException(ApiErrorCode.METER_READING_BATCH_NOT_FOUND));

        if (batch.getStatus() == BatchStatus.CANCELLED) {
            throw new AppException(ApiErrorCode.METER_READING_BATCH_CANCELLED);
        }
        assertBatchReadingsEditable(batch);

        List<ExcelReadingRow> parsedRows = parseExcel(file);
        Set<String> seenRoomCodes = new HashSet<>();
        List<ValidatedExcelReading> validatedRows = new ArrayList<>();

        for (ExcelReadingRow row : parsedRows) {
            String normalizedRoomCode = normalizeRoomCode(row.roomCode());
            if (!seenRoomCodes.add(normalizedRoomCode)) {
                throw new AppException(
                        ApiErrorCode.METER_READING_EXCEL_DUPLICATE_ROOM,
                        row.roomCode(),
                        row.excelRowNumber()
                );
            }

            Room room = roomRepository.findByRoomCode(row.roomCode())
                    .orElseThrow(() -> new AppException(
                            ApiErrorCode.METER_READING_EXCEL_ROOM_NOT_FOUND,
                            row.roomCode(),
                            row.excelRowNumber()
                    ));
            if (!room.getPropertyId().equals(batch.getPropertyId())) {
                throw new AppException(
                        ApiErrorCode.METER_READING_EXCEL_ROOM_WRONG_PROPERTY,
                        row.roomCode(),
                        row.excelRowNumber()
                );
            }
            try {
                assertRoomRequiresMeterReading(batch.getPropertyId(), room.getId(), batch.getReadingPeriod());
            } catch (AppException exception) {
                if (exception.getApiErrorCode() == ApiErrorCode.METER_READING_ROOM_NOT_ELIGIBLE) {
                    throw new AppException(
                            ApiErrorCode.METER_READING_EXCEL_ROOM_NOT_ELIGIBLE,
                            row.roomCode(),
                            batch.getReadingPeriod(),
                            row.excelRowNumber()
                    );
                }
                throw exception;
            }
            validatedRows.add(new ValidatedExcelReading(room, row.currentValue()));
        }

        FileMetadata importedFile;
        try {
            importedFile = uploadFileUseCase.execute(new UploadFileCommand(
                    currentUser.getId(), file, FileCategory.OTHER, false
            ));
        } catch (IOException exception) {
            throw new AppException(ApiErrorCode.FILE_UPLOAD_FAILED);
        }

        batch.setImportedFileId(importedFile.getId());
        meterReadingBatchRepository.save(batch);

        LocalDate readingDate = MeterReadingPeriod.parse(batch.getReadingPeriod()).atEndOfMonth();
        for (ValidatedExcelReading row : validatedRows) {
            saveOrUpdateMeterValue(
                    row.room(), MeterType.ELECTRICITY, row.currentValue(), batch, null, currentUser,
                    ReadingSource.EXCEL_IMPORT, readingDate
            );
        }
        refreshBatchProgress(batch.getId(), batch.getPropertyId(), batch.getReadingPeriod());
        refreshMonthlyPreview(batch, currentUser.getId());

        return MeterReadingExcelImportResponse.builder()
                .batchId(batch.getId())
                .fileName(file.getOriginalFilename())
                .importedRows(validatedRows.size())
                .build();
    }

    private List<ExcelReadingRow> parseExcel(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ApiErrorCode.METER_READING_EXCEL_FILE_REQUIRED);
        }
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!filename.endsWith(".xlsx")) {
            throw new AppException(ApiErrorCode.METER_READING_EXCEL_XLSX_REQUIRED);
        }

        try (XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream())) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new AppException(ApiErrorCode.METER_READING_EXCEL_WORKBOOK_EMPTY);
            }

            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            ExcelImportColumns columns = findImportColumns(sheet, formatter, evaluator);

            List<ExcelReadingRow> rows = new ArrayList<>();
            for (int rowIndex = columns.headerRowIndex() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                String roomCode = cellText(row == null ? null : row.getCell(columns.roomCodeColumn()), formatter, evaluator).trim();
                String valueText = cellText(row == null ? null : row.getCell(columns.electricityColumn()), formatter, evaluator).trim();
                if (roomCode.isBlank() && valueText.isBlank()) continue;
                int excelRowNumber = rowIndex + 1;
                if (roomCode.isBlank()) {
                    throw new AppException(ApiErrorCode.METER_READING_EXCEL_ROOM_REQUIRED, excelRowNumber);
                }
                if (valueText.isBlank()) {
                    throw new AppException(ApiErrorCode.METER_READING_EXCEL_VALUE_REQUIRED, excelRowNumber, roomCode);
                }

                BigDecimal value;
                try {
                    value = parseExcelNumber(valueText);
                } catch (NumberFormatException exception) {
                    throw new AppException(ApiErrorCode.METER_READING_EXCEL_VALUE_INVALID, excelRowNumber, roomCode);
                }
                if (value.compareTo(BigDecimal.ZERO) < 0) {
                    throw new AppException(ApiErrorCode.METER_READING_EXCEL_VALUE_NEGATIVE, excelRowNumber, roomCode);
                }
                rows.add(new ExcelReadingRow(roomCode, value, excelRowNumber));
            }
            if (rows.isEmpty()) throw new AppException(ApiErrorCode.METER_READING_EXCEL_NO_DATA);
            return rows;
        } catch (IOException exception) {
            throw new AppException(ApiErrorCode.METER_READING_EXCEL_XLSX_REQUIRED, exception);
        }
    }

    private ExcelImportColumns findImportColumns(
            Sheet sheet,
            DataFormatter formatter,
            FormulaEvaluator evaluator
    ) {
        int lastHeaderRow = Math.min(sheet.getLastRowNum(), IMPORT_HEADER_SCAN_ROWS - 1);
        boolean roomColumnFound = false;
        boolean electricityColumnFound = false;
        for (int rowIndex = 0; rowIndex <= lastHeaderRow; rowIndex++) {
            Row header = sheet.getRow(rowIndex);
            if (header == null || header.getLastCellNum() < 0) {
                continue;
            }

            int roomCodeColumn = -1;
            int roomDisplayColumn = -1;
            int electricityColumn = -1;
            for (int column = 0; column < header.getLastCellNum(); column++) {
                String normalized = normalizeHeader(cellText(header.getCell(column), formatter, evaluator));
                if (Set.of("maphong", "roomcode").contains(normalized)) {
                    roomCodeColumn = column;
                } else if (Set.of("phong", "room").contains(normalized)) {
                    roomDisplayColumn = column;
                }
                if (Set.of("chisodien", "chisodienmoi", "chisodienhientai", "electricityvalue", "electricity", "currentvalue").contains(normalized)) {
                    electricityColumn = column;
                }
            }

            int resolvedRoomColumn = roomCodeColumn >= 0 ? roomCodeColumn : roomDisplayColumn;
            roomColumnFound |= resolvedRoomColumn >= 0;
            electricityColumnFound |= electricityColumn >= 0;
            if (resolvedRoomColumn >= 0 && electricityColumn >= 0) {
                return new ExcelImportColumns(rowIndex, resolvedRoomColumn, electricityColumn);
            }
        }
        if (!roomColumnFound && !electricityColumnFound) {
            throw new AppException(ApiErrorCode.METER_READING_EXCEL_REQUIRED_COLUMNS_MISSING);
        }
        if (!roomColumnFound) {
            throw new AppException(ApiErrorCode.METER_READING_EXCEL_ROOM_COLUMN_MISSING);
        }
        throw new AppException(ApiErrorCode.METER_READING_EXCEL_VALUE_COLUMN_MISSING);
    }

    private String cellText(Cell cell, DataFormatter formatter, FormulaEvaluator evaluator) {
        return cell == null ? "" : formatter.formatCellValue(cell, evaluator);
    }

    private BigDecimal parseExcelNumber(String rawValue) {
        String value = rawValue == null ? "" : rawValue.trim().replaceAll("[\\s\\u00A0]", "");
        if (value.contains(",") && value.contains(".")) {
            char decimalSeparator = value.lastIndexOf(',') > value.lastIndexOf('.') ? ',' : '.';
            char groupingSeparator = decimalSeparator == ',' ? '.' : ',';
            value = value.replace(String.valueOf(groupingSeparator), "");
            if (decimalSeparator == ',') {
                value = value.replace(',', '.');
            }
        } else if (value.contains(",")) {
            value = normalizeSingleSeparatorNumber(value, ',');
        } else if (value.contains(".")) {
            value = normalizeSingleSeparatorNumber(value, '.');
        }
        return new BigDecimal(value);
    }

    private String normalizeSingleSeparatorNumber(String value, char separator) {
        if (isGroupedInteger(value, separator)) {
            return value.replace(String.valueOf(separator), "");
        }
        return separator == ',' ? value.replace(',', '.') : value;
    }

    private boolean isGroupedInteger(String value, char separator) {
        int start = value.startsWith("-") || value.startsWith("+") ? 1 : 0;
        String[] groups = value.substring(start).split(separator == '.' ? "\\." : ",", -1);
        if (groups.length < 2
                || groups[0].isBlank()
                || groups[0].length() > 3
                || !groups[0].chars().allMatch(Character::isDigit)) {
            return false;
        }
        for (int index = 1; index < groups.length; index++) {
            String group = groups[index];
            if (group.length() != 3 || !group.chars().allMatch(Character::isDigit)) {
                return false;
            }
        }
        return true;
    }

    private String normalizeHeader(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
    }

    private String normalizeRoomCode(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private record ExcelReadingRow(String roomCode, BigDecimal currentValue, int excelRowNumber) {
    }

    private record ExcelImportColumns(int headerRowIndex, int roomCodeColumn, int electricityColumn) {
    }

    private record ValidatedExcelReading(Room room, BigDecimal currentValue) {
    }

    private record ElectricityTariff(long unitPrice, long freeAllowance) {
    }

    @Override
    @Transactional
    public void resolveRoomReadingAnomalies(Long batchId, Long roomId) {
        User currentUser = userRepository.findById(AuthUtils.getCurrentAuthenticationId())
                .orElseThrow(() -> new AppException(ApiErrorCode.ACCOUNT_NOT_FOUND));
        MeterReadingBatch batch = meterReadingBatchRepository.findById(batchId)
                .orElseThrow(() -> new AppException(ApiErrorCode.METER_READING_BATCH_NOT_FOUND));

        if (batch.getStatus() == BatchStatus.CANCELLED) {
            throw new AppException(ApiErrorCode.METER_READING_BATCH_CANCELLED);
        }
        assertBatchReadingsEditable(batch);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(ApiErrorCode.ROOM_NOT_FOUND));
        if (!room.getPropertyId().equals(batch.getPropertyId())) {
            throw new AppException(ApiErrorCode.VISIT_002);
        }

        ensureReadingExistsBeforeResolving(room, batch, currentUser);

        List<MeterReadingAnomalyEntity> anomalies = meterReadingAnomalyRepository
                .findByBatch_IdAndMeterReading_Room_IdAndResolvedAtIsNullOrderByIdAsc(batchId, roomId);
        if (anomalies.isEmpty()) {
            return;
        }
        assertAnomaliesCanBeResolved(anomalies);

        var resolvedBy = jpaUserRepository.getReferenceById(currentUser.getId());
        var resolvedAt = java.time.LocalDateTime.now();
        Set<Long> touchedReadingIds = new HashSet<>();
        anomalies.forEach(anomaly -> {
            anomaly.setResolvedAt(resolvedAt);
            anomaly.setResolvedBy(resolvedBy);
            var reading = anomaly.getMeterReading();
            if (reading != null && touchedReadingIds.add(reading.getId())) {
                reading.setReviewStatus(MeterReadingReviewStatus.APPROVED);
                reading.setReviewCount(0);
            }
        });

        meterReadingAnomalyRepository.saveAll(anomalies);
        refreshBatchAnomalyCount(batchId);
        refreshMonthlyPreview(batch, currentUser.getId());
    }

    private void assertAnomaliesCanBeResolved(List<MeterReadingAnomalyEntity> anomalies) {
        if (anomalies.stream().anyMatch(anomaly -> anomaly.getAnomalyType() == AnomalyType.NEGATIVE_USAGE)) {
            throw new AppException(ApiErrorCode.INVALID_METER_READING_VALUE);
        }
    }

    private void ensureReadingExistsBeforeResolving(Room room, MeterReadingBatch batch, User currentUser) {
        Meter activeMeter = meterRepository.findFirstByRoomIdAndMeterTypeAndStatus(
                        room.getId(), MeterType.ELECTRICITY, MeterStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ApiErrorCode.METER_NOT_FOUND));

        if (meterReadingRepository.findByMeterIdAndBatchId(activeMeter.getId(), batch.getId()).isPresent()) {
            return;
        }

        MeterReading latestReading = meterReadingRepository
                .findFirstByRoomIdAndMeterTypeOrderByReadingDateDesc(room.getId(), MeterType.ELECTRICITY)
                .orElse(null);
        if (latestReading == null || latestReading.getCurrentValue() == null) {
            return;
        }

        // A missing-reading warning is resolved against the last known meter value.
        // This creates the current-period record before the anomaly is marked approved.
        saveOrUpdateMeterValue(
                room,
                MeterType.ELECTRICITY,
                latestReading.getCurrentValue(),
                batch,
                null,
                currentUser,
                ReadingSource.MANUAL,
                MeterReadingPeriod.parse(batch.getReadingPeriod()).atEndOfMonth()
        );
    }

    private void saveOrUpdateMeterValue(Room room, MeterType meterType, BigDecimal newValue, MeterReadingBatch batch, Long photoId, User currentUser) {
        saveOrUpdateMeterValue(room, meterType, newValue, batch, photoId, currentUser,
                ReadingSource.MANUAL, LocalDate.now());
    }

    private void saveOrUpdateMeterValue(
            Room room, MeterType meterType, BigDecimal newValue, MeterReadingBatch batch,
            Long photoId, User currentUser,
            ReadingSource source, LocalDate readingDate
    ) {
        if (newValue == null) return;
        requireMeterValue(newValue);

        Meter activeMeter = meterRepository.findFirstByRoomIdAndMeterTypeAndStatus(
                room.getId(), meterType, MeterStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ApiErrorCode.METER_NOT_FOUND));
        requireMeterValueWithinCapacity(newValue, activeMeter.getCounterCapacity());

        var existingReadingOpt = meterReadingRepository.findByMeterIdAndBatchId(activeMeter.getId(), batch.getId());
        if (existingReadingOpt.isPresent()) {
            MeterReading existing = existingReadingOpt.get();
            boolean preserveApprovedReview = isApprovedWithUnchangedValue(existing, newValue, source);
            existing.setBatchId(batch.getId());
            existing.setCurrentValue(newValue);
            existing.setPhotoFileId(photoId);
            existing.setSource(source);
            applyUsageCalculation(existing, activeMeter.getCounterCapacity());
            saveReadingWithReview(
                    existing,
                    meterType,
                    findPreviousCycleUsage(activeMeter.getId(), existing.getReadingDate()),
                    room.getPropertyId(),
                    activeMeter.getCounterCapacity(),
                    preserveApprovedReview
            );
            return;
        }

        var existingPeriodReadingOpt = meterReadingRepository.findFirstByMeterIdAndReadingPeriodOrderByRevisionNoDesc(
                activeMeter.getId(),
                batch.getReadingPeriod()
        );
        if (existingPeriodReadingOpt.isPresent()
                && existingPeriodReadingOpt.get().getStatus() != ReadingStatus.VOIDED) {
            MeterReading existing = existingPeriodReadingOpt.get();
            boolean preserveApprovedReview = isApprovedWithUnchangedValue(existing, newValue, source);
            existing.setBatchId(batch.getId());
            existing.setCurrentValue(newValue);
            existing.setPhotoFileId(photoId);
            existing.setSource(source);
            applyUsageCalculation(existing, activeMeter.getCounterCapacity());
            saveReadingWithReview(
                    existing,
                    meterType,
                    findPreviousCycleUsage(activeMeter.getId(), existing.getReadingDate()),
                    room.getPropertyId(),
                    activeMeter.getCounterCapacity(),
                    preserveApprovedReview
            );
            return;
        }

        var latestReadingOpt = meterReadingRepository.findFirstByMeterIdOrderByReadingDateDesc(activeMeter.getId());
        
        MeterReading previousReading = latestReadingOpt.orElse(null);
        BigDecimal prevValue = previousReading == null ? BigDecimal.ZERO : previousReading.getCurrentValue();
        int nextRevision = existingPeriodReadingOpt
                .map(MeterReading::getRevisionNo)
                .map(revision -> revision + 1)
                .orElse(1);
        MeterReading reading = MeterReading.builder()
                .meterId(activeMeter.getId())
                .roomId(room.getId())
                .readingPeriod(batch.getReadingPeriod())
                .revisionNo(nextRevision)
                .previousValue(prevValue)
                .currentValue(newValue)
                .readingDate(readingDate)
                .source(source)
                .status(ReadingStatus.CONFIRMED)
                .batchId(batch.getId())
                .createdById(currentUser.getId())
                .photoFileId(photoId)
                .build();
        applyUsageCalculation(reading, activeMeter.getCounterCapacity());
        saveReadingWithReview(
                reading,
                meterType,
                usageOf(previousReading),
                room.getPropertyId(),
                activeMeter.getCounterCapacity()
        );
    }

    private boolean isApprovedWithUnchangedValue(
            MeterReading reading,
            BigDecimal newValue,
            ReadingSource source
    ) {
        // A new Excel import must re-run anomaly detection, even when its value is unchanged.
        return source != ReadingSource.EXCEL_IMPORT
                && reading.getReviewStatus() == MeterReadingReviewStatus.APPROVED
                && reading.getCurrentValue() != null
                && (reading.getPreviousValue() == null
                || reading.getCurrentValue().compareTo(reading.getPreviousValue()) >= 0
                || (reading.getRolloverCount() != null && reading.getRolloverCount() > 0))
                && reading.getCurrentValue().compareTo(newValue) == 0;
    }

    private MeterReading saveReadingWithReview(
            MeterReading reading,
            MeterType meterType,
            BigDecimal previousCycleUsage,
            Long propertyId,
            BigDecimal counterCapacity
    ) {
        return saveReadingWithReview(
                reading,
                meterType,
                previousCycleUsage,
                propertyId,
                counterCapacity,
                false
        );
    }

    private MeterReading saveReadingWithReview(
            MeterReading reading,
            MeterType meterType,
            BigDecimal previousCycleUsage,
            Long propertyId,
            BigDecimal counterCapacity,
            boolean preserveApprovedReview
    ) {
        List<MeterReadingAnomalyPolicy.DetectedAnomaly> anomalies;
        if (preserveApprovedReview) {
            anomalies = List.of();
            reading.setReviewStatus(MeterReadingReviewStatus.APPROVED);
            reading.setReviewCount(0);
        } else {
            anomalies = anomalyPolicy.detect(
                    propertyId,
                    meterType,
                    reading.getPreviousValue(),
                    reading.getCurrentValue(),
                    reading.getRolloverCount(),
                    previousCycleUsage,
                    electricityAmountForReview(reading, meterType, counterCapacity, propertyId)
            );
            reading.setReviewStatus(anomalies.isEmpty()
                    ? MeterReadingReviewStatus.NONE
                    : MeterReadingReviewStatus.PENDING);
            reading.setReviewCount(anomalies.size());
        }

        MeterReading saved = meterReadingRepository.save(reading);
        replaceAnomalies(saved, anomalies);
        refreshBatchAnomalyCount(saved.getBatchId());
        return saved;
    }

    private Long electricityAmountForReview(
            MeterReading reading,
            MeterType meterType,
            BigDecimal counterCapacity,
            Long propertyId
    ) {
        if (meterType != MeterType.ELECTRICITY || reading == null) {
            return null;
        }

        MeterUsageCalculator.Calculation calculation = meterUsageCalculator.calculate(
                reading.getPreviousValue(),
                reading.getCurrentValue(),
                counterCapacity,
                reading.getRolloverCount()
        );
        if (!calculation.valid()) {
            return null;
        }

        ElectricityTariff tariff = jdbcTemplate.query("""
                        SELECT unit_price, free_allowance
                        FROM utility_tariffs
                        WHERE property_id = ?
                          AND utility_type = 'ELECTRICITY'
                          AND effective_from <= ?
                          AND (effective_to IS NULL OR effective_to >= ?)
                        ORDER BY effective_from DESC, utility_tariff_id DESC
                        LIMIT 1
                        """,
                (resultSet, rowNum) -> new ElectricityTariff(
                        resultSet.getLong("unit_price"),
                        resultSet.getLong("free_allowance")
                ),
                propertyId,
                reading.getReadingDate(),
                reading.getReadingDate()
        ).stream().findFirst().orElse(new ElectricityTariff(3500L, 0L));

        BigDecimal billableUsage = calculation.usage()
                .subtract(BigDecimal.valueOf(tariff.freeAllowance()));
        int quantity = billableUsage.signum() <= 0
                ? 0
                : billableUsage.setScale(0, java.math.RoundingMode.CEILING).intValue();
        return (long) quantity * tariff.unitPrice();
    }

    private void replaceAnomalies(MeterReading reading, List<MeterReadingAnomalyPolicy.DetectedAnomaly> anomalies) {
        if (reading.getId() == null) {
            return;
        }
        meterReadingAnomalyRepository.deleteByMeterReading_IdAndResolvedAtIsNull(reading.getId());
        if (anomalies.isEmpty()) {
            return;
        }

        var readingRef = jpaMeterReadingRepository.getReferenceById(reading.getId());
        var batchRef = reading.getBatchId() == null
                ? null
                : jpaMeterReadingBatchRepository.getReferenceById(reading.getBatchId());
        meterReadingAnomalyRepository.saveAll(anomalies.stream()
                .map(anomaly -> MeterReadingAnomalyEntity.builder()
                        .batch(batchRef)
                        .meterReading(readingRef)
                        .anomalyType(anomaly.type())
                        .severity(anomaly.severity())
                        .message(anomaly.message())
                        .build())
                .toList());
    }

    private void refreshBatchAnomalyCount(Long batchId) {
        if (batchId == null) {
            return;
        }
        jpaMeterReadingBatchRepository.findById(batchId).ifPresent(batch -> {
            long count = meterReadingAnomalyRepository.countByBatch_IdAndResolvedAtIsNull(batchId);
            batch.setAnomalyCount(count > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) count);
            jpaMeterReadingBatchRepository.save(batch);
        });
    }

    private void refreshBatchProgress(Long batchId, Long propertyId, String readingPeriod) {
        if (batchId == null) {
            return;
        }

        int totalRooms = countMeterReadingRooms(propertyId, readingPeriod);
        int completedRooms = countCompletedRooms(batchId, propertyId, readingPeriod);
        jpaMeterReadingBatchRepository.findById(batchId).ifPresent(batch -> {
            batch.setTotalRooms(totalRooms);
            batch.setCompletedRooms(completedRooms);
            jpaMeterReadingBatchRepository.save(batch);
        });
    }

    private BigDecimal findPreviousCycleUsage(Long meterId, LocalDate readingDate) {
        if (readingDate == null) {
            return null;
        }
        return meterReadingRepository.findByMeterIdAndReadingDateBeforeOrderByReadingDateDesc(meterId, readingDate)
                .stream()
                .findFirst()
                .map(this::usageOf)
                .orElse(null);
    }

    private BigDecimal usageOf(MeterReading reading) {
        if (reading == null) {
            return null;
        }
        if (reading.getUsageAmount() != null) {
            return reading.getUsageAmount();
        }
        if (reading.getCurrentValue() != null
                && reading.getPreviousValue() != null
                && reading.getCurrentValue().compareTo(reading.getPreviousValue()) < 0
                && (reading.getRolloverCount() == null || reading.getRolloverCount() == 0)) {
            return null;
        }
        return safe(reading.getCurrentValue())
                .subtract(safe(reading.getPreviousValue()))
                .add(safe(reading.getCounterCapacitySnapshot())
                        .multiply(BigDecimal.valueOf(reading.getRolloverCount() == null ? 0 : reading.getRolloverCount())));
    }

    private void applyUsageCalculation(MeterReading reading, BigDecimal capacity) {
        MeterUsageCalculator.Calculation calculation = meterUsageCalculator.calculate(
                reading.getPreviousValue(),
                reading.getCurrentValue(),
                capacity,
                null
        );
        reading.setRolloverCount(calculation.valid() ? calculation.rolloverCount() : 0);
        reading.setCounterCapacitySnapshot(calculation.valid() && calculation.rolloverCount() > 0
                ? calculation.counterCapacity()
                : BigDecimal.ZERO);
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private void requireMeterValue(BigDecimal newValue) {
        if (newValue == null || newValue.compareTo(BigDecimal.ZERO) < 0) {
            throw new AppException(ApiErrorCode.INVALID_METER_READING_VALUE);
        }
    }

    private void requireMeterValueWithinCapacity(BigDecimal newValue, BigDecimal capacity) {
        if (capacity != null && capacity.signum() > 0 && newValue.compareTo(capacity) >= 0) {
            throw new AppException(ApiErrorCode.INVALID_METER_READING_VALUE);
        }
    }

    private void assertBatchReadingsEditable(MeterReadingBatch batch) {
        if (batch.getStatus() == BatchStatus.CANCELLED) {
            throw new AppException(ApiErrorCode.METER_READING_BATCH_CANCELLED);
        }
        assertMonthlyInvoicesNotIssued(batch.getPropertyId(), batch.getReadingPeriod());
    }

    private void assertMonthlyInvoicesNotIssued(Long propertyId, String readingPeriod) {
        String billingPeriod = MeterReadingPeriod.parse(readingPeriod).toString();
        if (utilityBillingRunService.hasIssuedMonthlyInvoices(propertyId, billingPeriod)) {
            throw new AppException(ApiErrorCode.METER_READING_INVOICES_ISSUED);
        }
    }

    private void refreshMonthlyPreview(MeterReadingBatch batch, Long currentUserId) {
        utilityBillingRunService.refreshMonthlyPreviewIfOpen(
                batch.getPropertyId(),
                MeterReadingPeriod.parse(batch.getReadingPeriod()).toString(),
                currentUserId
        );
    }

    @Override
    @Transactional
    public void confirmBatch(Long batchId) {
        User currentUser = userRepository.findById(AuthUtils.getCurrentAuthenticationId())
                .orElseThrow(() -> new AppException(ApiErrorCode.ACCOUNT_NOT_FOUND));

        MeterReadingBatch batch = meterReadingBatchRepository.findById(batchId)
                .orElseThrow(() -> new AppException(ApiErrorCode.METER_READING_BATCH_NOT_FOUND));
        requireMeterReadingRooms(batch.getPropertyId(), batch.getReadingPeriod());
        assertMonthlyInvoicesNotIssued(batch.getPropertyId(), batch.getReadingPeriod());

        if (batch.getStatus() == BatchStatus.CANCELLED) {
            throw new AppException(ApiErrorCode.METER_READING_BATCH_CANCELLED);
        }

        requireNoUnresolvedAnomalies(batch);
        requireCompletedReadings(batch);
        createMonthlyUtilityBillingBatch(batch, currentUser.getId());
    }

    private void createMonthlyUtilityBillingBatch(MeterReadingBatch batch, Long currentUserId) {
        requireCompletedReadings(batch);
        requireNoUnresolvedAnomalies(batch);
        String billingPeriod = MeterReadingPeriod.parse(batch.getReadingPeriod()).toString();
        utilityBillingRunService.createPreview(
                batch.getPropertyId(),
                billingPeriod,
                InvoiceReason.MONTHLY.name(),
                currentUserId
        );
    }

    private void requireCompletedReadings(MeterReadingBatch batch) {
        int totalRooms = requireMeterReadingRooms(batch.getPropertyId(), batch.getReadingPeriod());
        int completedRooms = countCompletedRooms(batch.getId(), batch.getPropertyId(), batch.getReadingPeriod());
        if (completedRooms < totalRooms) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
    }

    private void requireNoUnresolvedAnomalies(MeterReadingBatch batch) {
        long unresolvedAnomalies = meterReadingAnomalyRepository.countByBatch_IdAndResolvedAtIsNull(batch.getId());
        if (unresolvedAnomalies > 0) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
    }

    private int countCompletedRooms(Long batchId, Long propertyId, String readingPeriod) {
        YearMonth period = MeterReadingPeriod.parse(readingPeriod);
        Integer completedRooms = jdbcTemplate.queryForObject("""
                        SELECT COUNT(1)
                        FROM (
                            SELECT mr.room_id
                            FROM meter_readings mr
                            JOIN meter_reading_batches mb
                              ON mb.meter_reading_batch_id = mr.batch_id
                            JOIN meters m
                              ON m.meter_id = mr.meter_id
                            JOIN (
                                SELECT DISTINCT lc.room_id
                                FROM lease_contracts lc
                                JOIN rooms r
                                  ON r.room_id = lc.room_id
                                LEFT JOIN contract_liquidations cl
                                  ON cl.contract_id = lc.lease_contract_id
                                 AND cl.status = 'CONFIRMED'
                                WHERE lc.deleted_at IS NULL
                                  AND r.deleted_at IS NULL
                                  AND lc.status IN (
                                      'ACTIVE',
                                      'EXPIRING_SOON',
                                      'TERMINATION_PENDING',
                                      'EXPIRED',
                                      'LIQUIDATED',
                                      'RENEWED'
                                  )
                                  AND (? IS NULL OR r.property_id = ?)
                                  AND COALESCE(lc.rent_start_date, lc.start_date) <= ?
                                  AND (
                                      COALESCE(cl.liquidation_date, lc.end_date) IS NULL
                                      OR COALESCE(cl.liquidation_date, lc.end_date) >= ?
                                  )
                            ) eligible_rooms
                              ON eligible_rooms.room_id = mr.room_id
                            WHERE mr.batch_id = ?
                              AND mr.status <> 'VOIDED'
                              AND (
                                  mb.status <> 'CONFIRMED'
                                  OR mb.confirmed_at IS NULL
                                  OR mr.created_at <= mb.confirmed_at
                              )
                              AND m.meter_type = 'ELECTRICITY'
                              AND m.status = 'ACTIVE'
                            GROUP BY mr.room_id
                            HAVING COUNT(DISTINCT m.meter_type) = 1
                        ) completed_rooms
                        """,
                Integer.class,
                propertyId,
                propertyId,
                period.atEndOfMonth(),
                period.atDay(1),
                batchId
        );
        return completedRooms == null ? 0 : completedRooms;
    }

    private int countMeterReadingRooms(Long propertyId, String readingPeriod) {
        YearMonth period = MeterReadingPeriod.parse(readingPeriod);
        return Math.toIntExact(leaseContractRepository.countMeterReadingRoomsByPeriodWithActiveMeter(
                propertyId,
                MeterReadingContractEligibility.STATUSES,
                period.atDay(1),
                period.atEndOfMonth(),
                MeterType.ELECTRICITY,
                MeterStatus.ACTIVE
        ));
    }

    private int requireMeterReadingRooms(Long propertyId, String readingPeriod) {
        int totalRooms = countMeterReadingRooms(propertyId, readingPeriod);
        if (totalRooms == 0) {
            throw new AppException(ApiErrorCode.METER_READING_NO_ELIGIBLE_ROOMS);
        }
        return totalRooms;
    }

    private void assertRoomRequiresMeterReading(Long propertyId, Long roomId, String readingPeriod) {
        YearMonth period = MeterReadingPeriod.parse(readingPeriod);
        boolean required = leaseContractRepository.roomRequiresMeterReadingForPeriodWithActiveMeter(
                propertyId,
                roomId,
                MeterReadingContractEligibility.STATUSES,
                period.atDay(1),
                period.atEndOfMonth(),
                MeterType.ELECTRICITY,
                MeterStatus.ACTIVE
        );
        if (!required) {
            throw new AppException(ApiErrorCode.METER_READING_ROOM_NOT_ELIGIBLE);
        }
    }
}
