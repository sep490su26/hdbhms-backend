package com.sep490.hdbhms.property.infrastructure.web.controller;

import com.sep490.hdbhms.shared.exception.AppException;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;

import com.sep490.hdbhms.property.application.service.GetMeterReadingsService;
import com.sep490.hdbhms.property.application.port.in.usecase.SubmitMeterReadingUseCase;
import com.sep490.hdbhms.property.application.service.MeterReadingPeriod;
import com.sep490.hdbhms.property.infrastructure.web.mapper.MeterReadingWebMapper;
import com.sep490.hdbhms.property.application.service.GetBatchMeterReadingsService;
import com.sep490.hdbhms.property.infrastructure.web.dto.request.BatchMeterReadingRequest;
import com.sep490.hdbhms.property.infrastructure.web.dto.request.SingleMeterReadingRequest;
import com.sep490.hdbhms.property.infrastructure.web.dto.response.BatchMeterReadingStatusResponse;
import com.sep490.hdbhms.property.infrastructure.web.dto.response.MeterReadingBatchHistoryResponse;
import com.sep490.hdbhms.property.infrastructure.web.dto.response.MeterReadingListResponse;
import com.sep490.hdbhms.property.infrastructure.web.dto.response.UtilityDashboardResponse;
import com.sep490.hdbhms.property.infrastructure.web.dto.response.MeterReadingExcelImportResponse;
import com.sep490.hdbhms.shared.types.dto.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.ContentDisposition;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.ProgressiveRoomReadingRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/meter-readings")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MeterReadingController {
    private static final String[] IMPORT_TEMPLATE_HEADERS = {
            "Mã phòng",
            "Phòng",
            "Chỉ số điện kỳ trước",
            "Chỉ số điện mới"
    };
    private static final int[] IMPORT_TEMPLATE_COLUMN_WIDTHS = {16, 28, 22, 20};
    private static final String IMPORT_TEMPLATE_NUMBER_FORMAT = "#,##0";

    MeterReadingWebMapper meterReadingWebMapper;
    GetMeterReadingsService getMeterReadingsService;
    GetBatchMeterReadingsService getBatchMeterReadingsService;
    SubmitMeterReadingUseCase submitMeterReadingUseCase;

    /**
     * GET /api/v1/meter-readings?period=MM-yyyy&propertyId=1
     * <p>
     * Returns readings grouped by room.
     * - period: defaults to current month if omitted
     * - propertyId: optional; returns all properties if omitted
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ApiResponse<MeterReadingListResponse> getReadings(
            @RequestParam(required = false) String period,
            @RequestParam(required = false) Long propertyId) {
        return ApiResponse.<MeterReadingListResponse>builder()
                .data(getMeterReadingsService.getReadings(period, propertyId))
                .build();
    }

    /**
     * GET /api/v1/meter-readings/batch-status?period=MM-yyyy&propertyId=1
     * <p>
     * Returns rooms that have lease contracts overlapping the period and an active electricity meter.
     */
    @GetMapping("/batch-status")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ApiResponse<BatchMeterReadingStatusResponse> getBatchStatus(
            @RequestParam(required = false) String period,
            @RequestParam(required = false) Long propertyId,
            @RequestParam(required = false) Long batchId) {
        return ApiResponse.<BatchMeterReadingStatusResponse>builder()
                .data(getBatchMeterReadingsService.getBatchStatus(period, propertyId, batchId))
                .build();
    }

    /**
     * GET /api/v1/meter-readings/history?propertyId=1
     * <p>
     * Returns history of batch meter readings.
     */
    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ApiResponse<MeterReadingBatchHistoryResponse> getBatchHistory(
            @RequestParam(required = false) Long propertyId) {
        return ApiResponse.<MeterReadingBatchHistoryResponse>builder()
                .data(getBatchMeterReadingsService.getBatchHistory(propertyId))
                .build();
    }

    /**
     * GET /api/v1/meter-readings/dashboard?propertyId=1
     * <p>
     * Returns dashboard information for meter readings.
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ApiResponse<UtilityDashboardResponse> getDashboard(
            @RequestParam(required = false) Long propertyId) {
        return ApiResponse.<UtilityDashboardResponse>builder()
                .data(getBatchMeterReadingsService.getDashboard(propertyId))
                .build();
    }

    /**
     * POST /api/v1/meter-readings/submit
     * <p>
     * Submits an electricity reading for a single room.
     */
    @PostMapping("/submit")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ApiResponse<Void> submitSingleReading(
            @Valid @RequestBody SingleMeterReadingRequest request) {
        submitMeterReadingUseCase.submitSingleReading(meterReadingWebMapper.toCommand(request));
        return ApiResponse.<Void>builder()
                .message("Lưu chỉ số điện thành công")
                .build();
    }

    /**
     * POST /api/v1/meter-readings/batches
     * <p>
     * Submits electricity readings in batch for multiple rooms in a property.
     */
    @PostMapping("/batches")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ApiResponse<Void> submitBatchReadings(
            @Valid @RequestBody BatchMeterReadingRequest request) {
        submitMeterReadingUseCase.submitBatchReadings(meterReadingWebMapper.toCommand(request));
        return ApiResponse.<Void>builder()
                .message("Đã tạo/cập nhật bản nháp hóa đơn từ lô chỉ số điện")
                .build();
    }

    @PostMapping("/batches/start")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ApiResponse<Long> startBatch(
            @RequestParam String period,
            @RequestParam Long propertyId) {
        Long batchId = submitMeterReadingUseCase.startBatch(period, propertyId);
        return ApiResponse.<Long>builder()
                .data(batchId)
                .message("Đã bắt đầu lô nhập chỉ số")
                .build();
    }

    @PutMapping("/batches/{batchId}/rooms/{roomId}")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ApiResponse<Void> saveProgressiveRoomReading(
            @PathVariable Long batchId,
            @PathVariable Long roomId,
            @Valid @RequestBody ProgressiveRoomReadingRequest request) {
        submitMeterReadingUseCase.saveProgressiveRoomReading(
                batchId, roomId, 
                request.getElectricityValue(), request.getElectricityPhotoId()
        );
        return ApiResponse.<Void>builder()
                .message("Lưu tiến độ chỉ số thành công")
                .details("Lưu chỉ số phòng thành công")
                .build();
    }

    @PutMapping("/batches/{batchId}/rooms/{roomId}/anomalies/resolve")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ApiResponse<Void> resolveRoomReadingAnomalies(
            @PathVariable Long batchId,
            @PathVariable Long roomId) {
        submitMeterReadingUseCase.resolveRoomReadingAnomalies(batchId, roomId);
        return ApiResponse.<Void>builder()
                .message("Đã xử lý các cảnh báo chỉ số của phòng")
                .build();
    }

    @GetMapping("/import-template")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ResponseEntity<byte[]> downloadImportTemplate(
            @RequestParam(required = false) String period,
            @RequestParam(required = false) Long propertyId,
            @RequestParam(required = false) Long batchId) {
        try {
            BatchMeterReadingStatusResponse batchStatus = getBatchMeterReadingsService.getBatchStatus(period, propertyId, batchId);
            String readingPeriod = batchStatus != null && batchStatus.getReadingPeriod() != null
                    ? batchStatus.getReadingPeriod()
                    : MeterReadingPeriod.normalize(period);
            byte[] workbook = generateImportTemplateWorkbook(batchStatus, readingPeriod);
            String fileName = templateWorkbookTitle(readingPeriod) + ".xlsx";

            return ResponseEntity.ok()
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.attachment()
                                    .filename(fileName, StandardCharsets.UTF_8)
                                    .build()
                                    .toString()
                    )
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(workbook);
        } catch (IOException exception) {
            throw new AppException(ApiErrorCode.UNDEFINED, exception);
        }
    }

    @PostMapping(value = "/batches/{batchId}/import-excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ApiResponse<MeterReadingExcelImportResponse> importExcel(
            @PathVariable Long batchId,
            @RequestPart("file") MultipartFile file) {
        return ApiResponse.<MeterReadingExcelImportResponse>builder()
                .data(submitMeterReadingUseCase.importExcel(batchId, file))
                .message("Đã nhập chỉ số điện từ Excel")
                .build();
    }

    @PostMapping("/batches/{batchId}/confirm")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ApiResponse<Void> confirmBatch(
            @PathVariable Long batchId) {
        submitMeterReadingUseCase.confirmBatch(batchId);
        return ApiResponse.<Void>builder()
                .message("Đã tạo/cập nhật bản nháp hóa đơn từ kỳ ghi chỉ số.")
                .build();
    }

    private byte[] generateImportTemplateWorkbook(
            BatchMeterReadingStatusResponse batchStatus,
            String readingPeriod
    ) throws IOException {
        try (
                XSSFWorkbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream output = new ByteArrayOutputStream()
        ) {
            String workbookTitle = templateWorkbookTitle(readingPeriod);
            workbook.getProperties().getCoreProperties().setTitle(workbookTitle);

            Sheet sheet = workbook.createSheet(WorkbookUtil.createSafeSheetName(workbookTitle));
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle textStyle = createTextStyle(workbook);
            CellStyle numberStyle = createNumberStyle(workbook);
            CellStyle inputStyle = createInputStyle(workbook);

            Row header = sheet.createRow(0);
            header.setHeightInPoints(22);
            for (int column = 0; column < IMPORT_TEMPLATE_HEADERS.length; column++) {
                Cell cell = header.createCell(column);
                cell.setCellValue(IMPORT_TEMPLATE_HEADERS[column]);
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = 1;
            for (ImportTemplateRow templateRow : importTemplateRows(batchStatus)) {
                Row row = sheet.createRow(rowIndex++);
                row.setHeightInPoints(20);
                setTextCell(row, 0, templateRow.roomCode(), textStyle);
                setTextCell(row, 1, templateRow.roomName(), textStyle);
                setNumberCell(row, 2, templateRow.previousReading(), numberStyle);
                setNumberCell(row, 3, templateRow.currentReading(), inputStyle);
            }

            for (int column = 0; column < IMPORT_TEMPLATE_COLUMN_WIDTHS.length; column++) {
                sheet.setColumnWidth(column, IMPORT_TEMPLATE_COLUMN_WIDTHS[column] * 256);
            }
            sheet.setColumnHidden(0, true);
            sheet.createFreezePane(0, 1);
            sheet.setAutoFilter(new CellRangeAddress(0, Math.max(rowIndex - 1, 0), 0, IMPORT_TEMPLATE_HEADERS.length - 1));

            workbook.write(output);
            return output.toByteArray();
        }
    }

    private static List<ImportTemplateRow> importTemplateRows(BatchMeterReadingStatusResponse batchStatus) {
        if (batchStatus == null || batchStatus.getRooms() == null) {
            return List.of();
        }

        return batchStatus.getRooms().stream()
                .filter(Objects::nonNull)
                .filter(MeterReadingController::requiresImportTemplateEntry)
                .map(room -> new ImportTemplateRow(
                        room.getRoomCode(),
                        displayRoomName(room),
                        room.getElectricityPrevious(),
                        room.getElectricityCurrent()
                ))
                .toList();
    }

    private static String displayRoomName(BatchMeterReadingStatusResponse.RoomBatchStatus room) {
        String roomName = room.getRoomName() == null ? "" : room.getRoomName().trim();
        return roomName.isBlank() ? room.getRoomCode() : roomName;
    }

    private static boolean requiresImportTemplateEntry(BatchMeterReadingStatusResponse.RoomBatchStatus room) {
        String status = room.getStatus() == null ? "" : room.getStatus().trim().toLowerCase(Locale.ROOT);
        return !"synced".equals(status);
    }

    private static CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        setThinBorders(style);
        return style;
    }

    private static CellStyle createTextStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        setThinBorders(style);
        return style;
    }

    private static CellStyle createNumberStyle(Workbook workbook) {
        CellStyle style = createTextStyle(workbook);
        style.setDataFormat(workbook.createDataFormat().getFormat(IMPORT_TEMPLATE_NUMBER_FORMAT));
        return style;
    }

    private static CellStyle createInputStyle(Workbook workbook) {
        CellStyle style = createNumberStyle(workbook);
        style.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private static void setThinBorders(CellStyle style) {
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
    }

    private static void setTextCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value == null ? "" : value);
        cell.setCellStyle(style);
    }

    private static void setNumberCell(Row row, int column, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(column);
        if (value != null) {
            cell.setCellValue(value.doubleValue());
        }
        cell.setCellStyle(style);
    }

    private static String templateWorkbookTitle(String readingPeriod) {
        return "Kỳ nhập " + MeterReadingPeriod.normalize(readingPeriod);
    }

    private record ImportTemplateRow(
            String roomCode,
            String roomName,
            BigDecimal previousReading,
            BigDecimal currentReading
    ) {
    }
}
