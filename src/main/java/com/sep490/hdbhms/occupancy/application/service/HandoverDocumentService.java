package com.sep490.hdbhms.occupancy.application.service;

import com.lowagie.text.pdf.BaseFont;
import com.sep490.hdbhms.file.application.port.in.command.UploadFileCommand;
import com.sep490.hdbhms.file.application.port.in.usecase.UploadFileUseCase;
import com.sep490.hdbhms.file.domain.model.FileMetadata;
import com.sep490.hdbhms.file.domain.value_objects.FileCategory;
import com.sep490.hdbhms.file.infrastructure.persistence.entity.FileMetadataEntity;
import com.sep490.hdbhms.file.infrastructure.persistence.jpa.JpaFileMetadataRepository;
import com.sep490.hdbhms.occupancy.domain.value_objects.HandoverType;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.ContractHandoverRecordEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaContractHandoverRecordRepository;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import com.sep490.hdbhms.shared.utils.AuthUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextFontResolver;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class HandoverDocumentService {
    static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    TemplateEngine templateEngine;
    JdbcTemplate jdbcTemplate;
    UploadFileUseCase uploadFileUseCase;
    JpaContractHandoverRecordRepository handoverRepository;
    JpaFileMetadataRepository jpaFileMetadataRepository;

    public byte[] generateHandoverDraftPdf(Long contractId, HandoverType type) {
        HandoverTemplateData data = fetchHandoverData(contractId, type);
        String html = buildHandoverTemplateHtml(data);
        return renderHtmlToPdf(html);
    }

    @Transactional
    public void attachSignedDocument(Long contractId, HandoverType type, MultipartFile file) {
        Long currentUserId = AuthUtils.getCurrentAuthenticationId();
        if (currentUserId == null) {
            throw new AppException(ApiErrorCode.UNAUTHENTICATED);
        }
        ContractHandoverRecordEntity record = handoverRepository
                .findByContract_IdAndHandoverType(contractId, type)
                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));

        try {
            UploadFileCommand command = new UploadFileCommand(
                    currentUserId,
                    file,
                    FileCategory.HANDOVER_DOCUMENT,
                    false
            );
            FileMetadata uploadedFile = uploadFileUseCase.execute(command);
            
            FileMetadataEntity fileEntity = jpaFileMetadataRepository.findById(uploadedFile.getId())
                    .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));
                    
            record.setSignedDocument(fileEntity);
            handoverRepository.save(record);
        } catch (Exception e) {
            throw new AppException(ApiErrorCode.UNDEFINED);
        }
    }

    public HandoverFilenameContext getFilenameContext(Long contractId, HandoverType type) {
        return jdbcTemplate.query("""
                        SELECT
                            r.room_code,
                            pp.full_name AS tenant_name,
                            lc.start_date,
                            h.handover_date,
                            h.signed_document_id
                        FROM contract_handover_records h
                        JOIN lease_contracts lc ON lc.lease_contract_id = h.contract_id
                        JOIN rooms r ON r.room_id = h.room_id
                        LEFT JOIN person_profiles pp ON pp.person_profile_id = lc.primary_tenant_profile_id
                        WHERE h.contract_id = ?
                          AND h.handover_type = ?
                        ORDER BY h.contract_handover_record_id DESC
                        LIMIT 1
                        """,
                rs -> {
                    if (!rs.next()) {
                        throw new AppException(ApiErrorCode.CONTRACT_HANDOVER_RECORD_NOT_FOUND);
                    }
                    LocalDate handoverDate = rs.getTimestamp("handover_date") == null
                            ? null
                            : rs.getTimestamp("handover_date").toLocalDateTime().toLocalDate();
                    return new HandoverFilenameContext(
                            rs.getString("room_code"),
                            rs.getString("tenant_name"),
                            rs.getDate("start_date") != null ? rs.getDate("start_date").toLocalDate() : null,
                            handoverDate,
                            rs.getObject("signed_document_id", Long.class)
                    );
                },
                contractId,
                type.name()
        );
    }

    private HandoverTemplateData fetchHandoverData(Long contractId, HandoverType type) {
        String sql = """
            SELECT 
                r.room_code, f.floor_code,
                e.current_value as elec_val, e.reading_date as elec_date,
                w.current_value as water_val, w.reading_date as water_date
            FROM contract_handover_records h
            JOIN rooms r ON h.room_id = r.room_id
            LEFT JOIN floors f ON r.floor_id = f.floor_id
            LEFT JOIN meter_readings e ON h.electricity_reading_id = e.meter_reading_id
            LEFT JOIN meter_readings w ON h.water_reading_id = w.meter_reading_id
            WHERE h.contract_id = ? AND h.handover_type = ?
        """;
        
        List<HandoverTemplateData> results = jdbcTemplate.query(
            sql, 
            this::mapRowToData, 
            contractId, 
            type.name()
        );
        if (results.isEmpty()) {
            throw new AppException(ApiErrorCode.UNDEFINED);
        }
        return results.get(0);
    }

    private HandoverTemplateData mapRowToData(ResultSet rs, int rowNum) throws SQLException {
        return HandoverTemplateData.builder()
                .roomNumber(rs.getString("room_code"))
                .roomFloorNumber(rs.getString("floor_code"))
                .elecValue(rs.getLong("elec_val"))
                .elecDate(rs.getDate("elec_date") != null ? rs.getDate("elec_date").toLocalDate() : null)
                .waterValue(rs.getLong("water_val"))
                .waterDate(rs.getDate("water_date") != null ? rs.getDate("water_date").toLocalDate() : null)
                .build();
    }

    private String buildHandoverTemplateHtml(HandoverTemplateData data) {
        Context context = new Context();
        context.setVariables(buildHandoverVariables(data));
        return templateEngine.process("contractTemplates/html/handover_contract_template", context);
    }

    private Map<String, Object> buildHandoverVariables(HandoverTemplateData data) {
        Map<String, Object> variables = new HashMap<>();
        
        variables.put("roomNumber", valueOrDefault(data.roomNumber, "......"));
        variables.put("roomFloorNumber", valueOrDefault(data.roomFloorNumber, "..."));
        
        variables.put("roomElectricityValue", data.elecValue != null ? data.elecValue : "......");
        variables.put("roomElectricityReadingDate", formatDate(data.elecDate));
        
        variables.put("roomWaterValue", data.waterValue != null ? data.waterValue : "......");
        variables.put("roomWaterReadingDate", formatDate(data.waterDate));
        
        variables.put("issuedAtDateString", formatVietnameseDate(LocalDate.now()));

        return variables;
    }

    private byte[] renderHtmlToPdf(String html) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            ITextFontResolver fontResolver = renderer.getFontResolver();

            ClassLoader cl = getClass().getClassLoader();
            for (String f : List.of(
                    "fonts/times.ttf",
                    "fonts/timesbd.ttf",
                    "fonts/timesi.ttf",
                    "fonts/timesbi.ttf"
            )) {
                URL fontUrl = cl.getResource(f);
                if (fontUrl != null) {
                    fontResolver.addFont(fontUrl.toExternalForm(), BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                }
            }

            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(output);
            return output.toByteArray();
        } catch (Exception e) {
            throw new AppException(ApiErrorCode.UNDEFINED);
        }
    }

    private String formatDate(LocalDate date) {
        return date == null ? "............" : DATE_FORMATTER.format(date);
    }

    private String formatVietnameseDate(LocalDate date) {
        if (date == null) return "............";
        return "ngày %02d tháng %02d năm %d".formatted(
                date.getDayOfMonth(),
                date.getMonthValue(),
                date.getYear()
        );
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
    }

    @lombok.Data
    @lombok.Builder
    private static class HandoverTemplateData {
        String roomNumber;
        String roomFloorNumber;
        Long elecValue;
        LocalDate elecDate;
        Long waterValue;
        LocalDate waterDate;
    }

    public record HandoverFilenameContext(
            String roomCode,
            String tenantName,
            LocalDate startDate,
            LocalDate handoverDate,
            Long signedFileId
    ) {
    }
}
