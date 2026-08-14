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
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRow;

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

    /** Builds the current handover appendix from the updated DOCX template. */
    public byte[] generateHandoverDraftDocx(Long contractId, HandoverType type) {
        HandoverDocxData data = fetchDocxData(contractId);
        try (InputStream template = getClass().getClassLoader().getResourceAsStream(
                "templates/contractTemplates/docx/handover_contract_template.docx")) {
            if (template == null) {
                throw new AppException(ApiErrorCode.UNDEFINED);
            }
            try (XWPFDocument document = new XWPFDocument(template);
                 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                updateEquipmentHeading(document, data);
                updateEquipmentTable(document, data.assets());
                document.write(output);
                return output.toByteArray();
            }
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(ApiErrorCode.UNDEFINED);
        }
    }

    private HandoverDocxData fetchDocxData(Long contractId) {
        HandoverDocxData room = jdbcTemplate.query("""
                        SELECT r.room_id, r.room_code, f.floor_code
                        FROM lease_contracts lc
                        JOIN rooms r ON r.room_id = lc.room_id
                        LEFT JOIN floors f ON f.floor_id = r.floor_id
                        WHERE lc.lease_contract_id = ?
                          AND lc.deleted_at IS NULL
                        LIMIT 1
                        """,
                rs -> rs.next()
                        ? new HandoverDocxData(rs.getLong("room_id"), rs.getString("room_code"), rs.getString("floor_code"), List.of())
                        : null,
                contractId
        );
        if (room == null) {
            throw new AppException(ApiErrorCode.CONTRACT_NOT_FOUND);
        }
        List<EquipmentRow> assets = jdbcTemplate.query("""
                        SELECT asset_name, asset_category, quantity, current_condition, description
                        FROM room_assets
                        WHERE room_id = ?
                          AND deleted_at IS NULL
                        ORDER BY room_asset_id ASC
                        """,
                (rs, rowNum) -> new EquipmentRow(
                        rs.getString("asset_name"),
                        rs.getString("asset_category"),
                        rs.getInt("quantity"),
                        rs.getString("current_condition"),
                        rs.getString("description")
                ),
                room.roomId()
        );
        return new HandoverDocxData(room.roomId(), room.roomCode(), room.floorCode(), assets);
    }

    private void updateEquipmentHeading(XWPFDocument document, HandoverDocxData data) {
        String floor = data.floorCode() == null || data.floorCode().isBlank()
                ? ""
                : ", tầng " + data.floorCode();
        for (XWPFParagraph paragraph : document.getParagraphs()) {
            if (paragraph.getText() != null && paragraph.getText().contains("Danh mục nội thất bàn giao")) {
                replaceParagraphText(paragraph, "Danh mục nội thất bàn giao - Phòng " + data.roomCode() + floor);
                return;
            }
        }
    }

    private void replaceParagraphText(XWPFParagraph paragraph, String text) {
        while (!paragraph.getRuns().isEmpty()) {
            paragraph.removeRun(0);
        }
        XWPFRun run = paragraph.createRun();
        run.setBold(true);
        run.setText(text);
    }

    private void updateEquipmentTable(XWPFDocument document, List<EquipmentRow> assets) {
        if (document.getTables().isEmpty()) {
            return;
        }
        XWPFTable table = document.getTables().get(0);
        if (table.getNumberOfRows() < 2) {
            return;
        }

        CTRow template = (CTRow) table.getRow(1).getCtRow().copy();
        while (table.getNumberOfRows() > 1) {
            table.removeRow(1);
        }

        List<EquipmentRow> rows = assets == null || assets.isEmpty()
                ? List.of(new EquipmentRow("", "", null, "", ""))
                : assets;
        for (int index = 0; index < rows.size(); index++) {
            table.addRow(new XWPFTableRow((CTRow) template.copy(), table), index + 1);
            XWPFTableRow row = table.getRow(index + 1);
            fillEquipmentRow(row, index + 1, rows.get(index));
        }
    }

    private void fillEquipmentRow(XWPFTableRow row, int number, EquipmentRow asset) {
        String category = asset.category() == null ? "" : asset.category().trim();
        String description = asset.description() == null ? "" : asset.description().trim();
        if (!category.isBlank() && !description.isBlank()) {
            description = category + ": " + description;
        } else if (description.isBlank()) {
            description = category;
        }
        setCellText(row.getCell(0), String.valueOf(number), ParagraphAlignment.CENTER);
        setCellText(row.getCell(1), asset.name(), ParagraphAlignment.LEFT);
        setCellText(row.getCell(2), description, ParagraphAlignment.LEFT);
        setCellText(row.getCell(3), asset.quantity() == null ? "" : String.valueOf(asset.quantity()), ParagraphAlignment.CENTER);
        setCellText(row.getCell(4), localizeCondition(asset.condition()), ParagraphAlignment.CENTER);
    }

    private void setCellText(XWPFTableCell cell, String value, ParagraphAlignment alignment) {
        while (cell.getParagraphs().size() > 1) {
            cell.removeParagraph(1);
        }
        XWPFParagraph paragraph = cell.getParagraphs().get(0);
        while (!paragraph.getRuns().isEmpty()) {
            paragraph.removeRun(0);
        }
        paragraph.setAlignment(alignment);
        paragraph.createRun().setText(value == null ? "" : value);
    }

    private String localizeCondition(String condition) {
        if (condition == null) {
            return "";
        }
        return switch (condition) {
            case "GOOD" -> "Tốt";
            case "ATTENTION" -> "Cần chú ý";
            case "BROKEN" -> "Hỏng";
            case "MISSING" -> "Thiếu";
            default -> condition;
        };
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
            SELECT r.room_id, r.room_code, f.floor_code
            FROM contract_handover_records h
            JOIN rooms r ON h.room_id = r.room_id
            LEFT JOIN floors f ON r.floor_id = f.floor_id
            WHERE h.contract_id = ? AND h.handover_type = ?
            ORDER BY h.contract_handover_record_id DESC
            LIMIT 1
        """;

        HandoverTemplateData room = jdbcTemplate.query(
                sql,
                rs -> rs.next()
                        ? HandoverTemplateData.builder()
                        .roomId(rs.getLong("room_id"))
                        .roomNumber(rs.getString("room_code"))
                        .roomFloorNumber(rs.getString("floor_code"))
                        .build()
                        : null,
                contractId,
                type.name()
        );
        if (room == null) {
            throw new AppException(ApiErrorCode.UNDEFINED);
        }

        List<EquipmentRow> assets = jdbcTemplate.query("""
                        SELECT asset_name, asset_category, quantity, current_condition, description
                        FROM room_assets
                        WHERE room_id = ?
                          AND deleted_at IS NULL
                        ORDER BY room_asset_id ASC
                        """,
                (rs, rowNum) -> new EquipmentRow(
                        rs.getString("asset_name"),
                        rs.getString("asset_category"),
                        rs.getObject("quantity", Integer.class),
                        rs.getString("current_condition"),
                        rs.getString("description")
                ),
                room.roomId
        );
        room.assets = assets;
        return room;
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
        List<EquipmentRow> assets = data.assets == null || data.assets.isEmpty()
                ? List.of(new EquipmentRow("", "", null, "", ""))
                : data.assets;
        variables.put("equipmentRows", assets.stream()
                .map(this::toEquipmentTemplateRow)
                .toList());

        return variables;
    }

    private Map<String, Object> toEquipmentTemplateRow(EquipmentRow asset) {
        Map<String, Object> row = new HashMap<>();
        String category = valueOrDefault(asset.category(), "");
        String description = valueOrDefault(asset.description(), "");
        String detail = !category.isBlank() && !description.isBlank()
                ? category + ": " + description
                : !description.isBlank() ? description : category;
        row.put("name", valueOrDefault(asset.name(), ""));
        row.put("description", detail);
        row.put("quantity", asset.quantity() == null ? "" : asset.quantity());
        row.put("condition", localizeCondition(asset.condition()));
        return row;
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

    private String valueOrDefault(String value, String defaultValue) {
        return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
    }

    @lombok.Data
    @lombok.Builder
    private static class HandoverTemplateData {
        Long roomId;
        String roomNumber;
        String roomFloorNumber;
        List<EquipmentRow> assets;
    }

    private record HandoverDocxData(
            Long roomId,
            String roomCode,
            String floorCode,
            List<EquipmentRow> assets
    ) {
    }

    private record EquipmentRow(
            String name,
            String category,
            Integer quantity,
            String condition,
            String description
    ) {
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
