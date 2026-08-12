package com.sep490.hdbhms.occupancy.infrastructure.web.controller;

import com.sep490.hdbhms.file.application.port.in.query.DownloadFileQuery;
import com.sep490.hdbhms.file.application.port.in.usecase.DownloadFileUseCase;
import com.sep490.hdbhms.file.infrastructure.web.dto.response.FileDataResponse;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import com.sep490.hdbhms.identityandaccess.infrastructure.config.security.UserPrincipal;
import com.sep490.hdbhms.shared.types.dto.response.ApiResponse;
import com.sep490.hdbhms.shared.types.dto.response.PageResponse;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import com.sep490.hdbhms.shared.utils.DocumentFilenameBuilder;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
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
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/tenant-profiles")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TenantProfileManagementController {
    private static final String EXCEL_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String ZIP_CONTENT_TYPE = "application/zip";
    private static final List<PoliceReportColumn> DEFAULT_POLICE_REPORT_COLUMNS = List.of(
            PoliceReportColumn.PROPERTY_NAME,
            PoliceReportColumn.ROOM_CODE,
            PoliceReportColumn.CONTRACT_CODE,
            PoliceReportColumn.FULL_NAME,
            PoliceReportColumn.CCCD_NUMBER,
            PoliceReportColumn.DOCUMENT_TYPE,
            PoliceReportColumn.DATE_OF_BIRTH,
            PoliceReportColumn.GENDER,
            PoliceReportColumn.PHONE,
            PoliceReportColumn.PERMANENT_ADDRESS,
            PoliceReportColumn.ISSUED_DATE,
            PoliceReportColumn.ISSUED_PLACE,
            PoliceReportColumn.EXPIRY_DATE,
            PoliceReportColumn.CCCD_IMAGE_LINKS
    );

    JdbcTemplate jdbcTemplate;
    DownloadFileUseCase downloadFileUseCase;

    @GetMapping
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('OWNER') or hasRole('MANAGER')")
    public ApiResponse<PageResponse<TenantProfileSummaryResponse>> getTenantProfiles(
            @PageableDefault(size = 10) Pageable pageable
    ) {
        UserPrincipal principal = requireCurrentPrincipal();
        boolean isManager = principal.getRole() == Role.MANAGER;

        List<TenantProfileRow> rows = jdbcTemplate.query("""
                        SELECT *
                        FROM (
                            SELECT lc.lease_contract_id AS contract_id,
                                   lc.contract_code,
                                   lc.status AS contract_status,
                                   lc.start_date,
                                   lc.end_date,
                                   lc.monthly_rent,
                                   lc.deposit_amount,
                                   r.room_id AS room_id,
                                   r.room_code,
                                   r.max_occupants,
                                   p.property_id AS property_id,
                                   p.name AS property_name,
                                   p.address_line AS property_address,
                                   pp.person_profile_id AS profile_id,
                                   pp.user_id,
                                   pp.full_name,
                                   pp.dob,
                                   pp.gender,
                                   pp.phone,
                                   pp.email,
                                   pp.permanent_address,
                                   pp.portrait_file_id,
                                   u.status AS app_status,
                                   co.occupant_role AS room_role,
                                   co.move_in_date,
                                   co.move_out_date,
                                   'RENTING' AS residence_status
                            FROM lease_contracts lc
                            JOIN rooms r ON r.room_id = lc.room_id
                            JOIN properties p ON p.property_id = r.property_id
                            JOIN contract_occupants co ON co.contract_id = lc.lease_contract_id AND co.status = 'ACTIVE'
                            JOIN person_profiles pp ON pp.person_profile_id = co.tenant_profile_id
                            LEFT JOIN users u ON u.user_id = pp.user_id AND u.deleted_at IS NULL
                            WHERE lc.deleted_at IS NULL
                              AND lc.status IN ('ACTIVE','EXPIRING_SOON','TERMINATION_PENDING')
                              AND NOT EXISTS (
                                  SELECT 1
                                  FROM room_transfer_requests completed_transfer
                                  WHERE completed_transfer.old_contract_id = lc.lease_contract_id
                                    AND completed_transfer.status IN ('EXECUTED','COMPLETED')
                              )
                              AND pp.deleted_at IS NULL

                            UNION ALL

                            SELECT lc.lease_contract_id AS contract_id,
                                   lc.contract_code,
                                   lc.status AS contract_status,
                                   lc.start_date,
                                   lc.end_date,
                                   lc.monthly_rent,
                                   lc.deposit_amount,
                                   r.room_id AS room_id,
                                   r.room_code,
                                   r.max_occupants,
                                   p.property_id AS property_id,
                                   p.name AS property_name,
                                   p.address_line AS property_address,
                                   pp.person_profile_id AS profile_id,
                                   pp.user_id,
                                   pp.full_name,
                                   pp.dob,
                                   pp.gender,
                                   pp.phone,
                                   pp.email,
                                   pp.permanent_address,
                                   pp.portrait_file_id,
                                   u.status AS app_status,
                                   'PRIMARY' AS room_role,
                                   lc.start_date AS move_in_date,
                                   NULL AS move_out_date,
                                   'RENTING' AS residence_status
                            FROM lease_contracts lc
                            JOIN rooms r ON r.room_id = lc.room_id
                            JOIN properties p ON p.property_id = r.property_id
                            JOIN person_profiles pp ON pp.person_profile_id = lc.primary_tenant_profile_id
                            LEFT JOIN users u ON u.user_id = pp.user_id AND u.deleted_at IS NULL
                            WHERE lc.deleted_at IS NULL
                              AND lc.status IN ('ACTIVE','EXPIRING_SOON','TERMINATION_PENDING')
                              AND NOT EXISTS (
                                  SELECT 1
                                  FROM room_transfer_requests completed_transfer
                                  WHERE completed_transfer.old_contract_id = lc.lease_contract_id
                                    AND completed_transfer.status IN ('EXECUTED','COMPLETED')
                              )
                              AND pp.deleted_at IS NULL
                              AND NOT EXISTS (
                                  SELECT 1
                                  FROM contract_occupants co_primary
                                  WHERE co_primary.contract_id = lc.lease_contract_id
                                    AND co_primary.tenant_profile_id = pp.person_profile_id
                                    AND co_primary.status = 'ACTIVE'
                              )
                        ) tenant_profiles
                        ORDER BY property_name, room_code, contract_id, room_role DESC, full_name
                        """,
                (rs, rowNum) -> mapTenantProfileRow(rs)
        );

        if (isManager) {
            rows = rows.stream()
                    .filter(row -> isAssignedManager(principal.getId(), row.propertyId()))
                    .toList();
        }

        Map<Long, List<TenantProfileRow>> rowsByContract = new LinkedHashMap<>();
        for (TenantProfileRow row : rows) {
            rowsByContract.computeIfAbsent(row.contractId(), ignored -> new ArrayList<>()).add(row);
        }

        List<TenantProfileSummaryResponse> response = new ArrayList<>();
        for (TenantProfileRow row : rows) {
            List<TenantProfileRow> roomRows = rowsByContract.getOrDefault(row.contractId(), List.of());
            IdentityDocumentResponse identityDocument = getIdentityDocument(row.profileId());
            List<VehicleResponse> vehicles = getVehicles(row.profileId());
            List<EmergencyContactResponse> emergencyContacts = getEmergencyContacts(row.profileId());
            ProfileStatus profileStatus = resolveProfileStatus(row, identityDocument);
            List<RoommateResponse> roommates = roomRows.stream()
                    .filter(roommate -> !Objects.equals(roommate.profileId(), row.profileId())
                            || !Objects.equals(roommate.phone(), row.phone()))
                    .map(this::toRoommateResponse)
                    .toList();

            response.add(new TenantProfileSummaryResponse(
                    row.profileId(),
                    row.userId(),
                    row.fullName(),
                    row.dob(),
                    row.gender(),
                    row.phone(),
                    row.email(),
                    row.permanentAddress(),
                    fileUrl(row.portraitFileId()),
                    row.portraitFileId(),
                    identityDocument,
                    row.propertyId(),
                    row.propertyName(),
                    row.propertyAddress(),
                    row.roomId(),
                    row.roomCode(),
                    row.roomRole(),
                    roomRows.size(),
                    row.maxOccupants(),
                    row.moveInDate(),
                    row.moveOutDate(),
                    row.residenceStatus(),
                    row.appStatus(),
                    profileStatus.code(),
                    profileStatus.label(),
                    row.contractId(),
                    row.contractCode(),
                    row.contractStatus(),
                    row.startDate(),
                    row.endDate(),
                    row.monthlyRent(),
                    row.depositAmount(),
                    vehicles,
                    emergencyContacts,
                    roommates,
                    "APPROVED",
                    null,
                    true,
                    null,
                    null,
                    null
            ));
        }

        response.sort(Comparator
                .comparing(TenantProfileSummaryResponse::propertyName, Comparator.nullsLast(String::compareToIgnoreCase))
                .thenComparing(TenantProfileSummaryResponse::roomCode, Comparator.nullsLast(String::compareToIgnoreCase))
                .thenComparingInt(profile -> "PRIMARY".equalsIgnoreCase(profile.roomRole()) ? 0 : 1)
                .thenComparing(TenantProfileSummaryResponse::fullName, Comparator.nullsLast(String::compareToIgnoreCase)));

        List<TenantProfileSummaryResponse> pagedResponse = response.stream()
                .skip(pageable.getOffset())
                .limit(pageable.getPageSize())
                .toList();

        return ApiResponse.<PageResponse<TenantProfileSummaryResponse>>builder()
                .data(PageResponse.fromPageToPageResponse(new PageImpl<>(pagedResponse, pageable, response.size())))
                .build();
    }

    @GetMapping("/police-report/export")
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    public ResponseEntity<Resource> exportPoliceReport(
            @RequestParam(name = "columns", required = false) List<String> columns
    ) {
        List<PoliceReportColumn> selectedColumns = resolvePoliceReportColumns(columns);
        UserPrincipal principal = requireCurrentPrincipal();
        List<PoliceReportRow> rows = fetchPoliceReportRows(principal);
        if (rows.isEmpty()) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }

        byte[] bytes = generatePoliceReportWorkbook(rows, selectedColumns);
        String filename = "Danh sách cư dân báo công an " + LocalDate.now() + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, DocumentFilenameBuilder.attachmentContentDisposition(filename))
                .contentType(MediaType.parseMediaType(EXCEL_CONTENT_TYPE))
                .body(new ByteArrayResource(bytes));
    }

    @GetMapping("/police-report/export-package")
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    public ResponseEntity<Resource> exportPoliceReportPackage(
            @RequestParam(name = "columns", required = false) List<String> columns
    ) {
        List<PoliceReportColumn> selectedColumns = resolvePoliceReportColumns(columns);
        UserPrincipal principal = requireCurrentPrincipal();
        List<PoliceReportRow> rows = fetchPoliceReportRows(principal);
        if (rows.isEmpty()) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }

        byte[] bytes = generatePoliceReportPackage(rows, selectedColumns, principal);
        String filename = "Hồ sơ báo công an " + LocalDate.now() + ".zip";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, DocumentFilenameBuilder.attachmentContentDisposition(filename))
                .contentType(MediaType.parseMediaType(ZIP_CONTENT_TYPE))
                .body(new ByteArrayResource(bytes));
    }

    private List<PoliceReportColumn> resolvePoliceReportColumns(List<String> requestedColumns) {
        if (requestedColumns == null || requestedColumns.isEmpty()) {
            return DEFAULT_POLICE_REPORT_COLUMNS;
        }

        Set<String> columnKeys = new LinkedHashSet<>();
        for (String requestedColumn : requestedColumns) {
            if (requestedColumn == null || requestedColumn.isBlank()) {
                continue;
            }
            Arrays.stream(requestedColumn.split(","))
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .forEach(columnKeys::add);
        }
        if (columnKeys.isEmpty()) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }

        List<PoliceReportColumn> selectedColumns = new ArrayList<>();
        for (String columnKey : columnKeys) {
            selectedColumns.add(PoliceReportColumn.fromKey(columnKey));
        }
        return selectedColumns;
    }

    private List<PoliceReportRow> fetchPoliceReportRows(UserPrincipal principal) {
        return jdbcTemplate.query("""
                        SELECT resident_profiles.property_id,
                               resident_profiles.property_name,
                               resident_profiles.room_code,
                               resident_profiles.contract_code,
                               resident_profiles.full_name,
                               resident_profiles.dob,
                               resident_profiles.gender,
                               resident_profiles.phone,
                               resident_profiles.permanent_address,
                               id_doc.doc_type,
                               id_doc.doc_number,
                               id_doc.issued_date,
                               id_doc.issued_place,
                               id_doc.expiry_date,
                               id_doc.front_file_id,
                               id_doc.back_file_id
                        FROM (
                            SELECT lc.lease_contract_id AS contract_id,
                                   lc.contract_code,
                                   r.room_code,
                                    p.property_id,
                                    p.name AS property_name,
                                   pp.person_profile_id AS profile_id,
                                   pp.full_name,
                                   pp.dob,
                                   pp.gender,
                                   pp.phone,
                                   pp.permanent_address,
                                   co.occupant_role AS room_role
                            FROM lease_contracts lc
                            JOIN rooms r ON r.room_id = lc.room_id
                            JOIN properties p ON p.property_id = r.property_id
                            JOIN contract_occupants co ON co.contract_id = lc.lease_contract_id AND co.status = 'ACTIVE'
                            JOIN person_profiles pp ON pp.person_profile_id = co.tenant_profile_id
                            WHERE lc.deleted_at IS NULL
                              AND lc.status IN ('ACTIVE','EXPIRING_SOON','TERMINATION_PENDING')
                              AND NOT EXISTS (
                                  SELECT 1
                                  FROM room_transfer_requests completed_transfer
                                  WHERE completed_transfer.old_contract_id = lc.lease_contract_id
                                    AND completed_transfer.status IN ('EXECUTED','COMPLETED')
                              )
                              AND pp.deleted_at IS NULL

                            UNION ALL

                            SELECT lc.lease_contract_id AS contract_id,
                                   lc.contract_code,
                                   r.room_code,
                                   p.property_id,
                                   p.name AS property_name,
                                   pp.person_profile_id AS profile_id,
                                   pp.full_name,
                                   pp.dob,
                                   pp.gender,
                                   pp.phone,
                                   pp.permanent_address,
                                   'PRIMARY' AS room_role
                            FROM lease_contracts lc
                            JOIN rooms r ON r.room_id = lc.room_id
                            JOIN properties p ON p.property_id = r.property_id
                            JOIN person_profiles pp ON pp.person_profile_id = lc.primary_tenant_profile_id
                            WHERE lc.deleted_at IS NULL
                              AND lc.status IN ('ACTIVE','EXPIRING_SOON','TERMINATION_PENDING')
                              AND NOT EXISTS (
                                  SELECT 1
                                  FROM room_transfer_requests completed_transfer
                                  WHERE completed_transfer.old_contract_id = lc.lease_contract_id
                                    AND completed_transfer.status IN ('EXECUTED','COMPLETED')
                              )
                              AND pp.deleted_at IS NULL
                              AND NOT EXISTS (
                                  SELECT 1
                                  FROM contract_occupants co_primary
                                  WHERE co_primary.contract_id = lc.lease_contract_id
                                    AND co_primary.tenant_profile_id = pp.person_profile_id
                                    AND co_primary.status = 'ACTIVE'
                              )
                        ) resident_profiles
                        LEFT JOIN identity_documents id_doc
                          ON id_doc.identity_document_id = (
                              SELECT latest.identity_document_id
                              FROM identity_documents latest
                              WHERE latest.profile_id = resident_profiles.profile_id
                                AND latest.status = 'ACTIVE'
                              ORDER BY latest.updated_at DESC, latest.identity_document_id DESC
                              LIMIT 1
                          )
                        ORDER BY resident_profiles.property_name,
                                 resident_profiles.room_code,
                                 resident_profiles.contract_id,
                                 resident_profiles.room_role DESC,
                                 resident_profiles.full_name
                        """,
                (rs, rowNum) -> new PoliceReportRow(
                        nullableLong(rs, "property_id"),
                        rs.getString("property_name"),
                        rs.getString("room_code"),
                        rs.getString("contract_code"),
                        rs.getString("full_name"),
                        normalizeIdentityNumber(rs.getString("doc_number")),
                        rs.getString("doc_type"),
                        nullableLocalDate(rs, "dob"),
                        rs.getString("gender"),
                        rs.getString("phone"),
                        rs.getString("permanent_address"),
                        nullableLocalDate(rs, "issued_date"),
                        rs.getString("issued_place"),
                        nullableLocalDate(rs, "expiry_date"),
                        formatIdentityImageLinks(
                                nullableLong(rs, "front_file_id"),
                                nullableLong(rs, "back_file_id")
                        )
                )
        ).stream()
                .filter(row -> principal.getRole() == Role.OWNER
                        || isAssignedManager(principal.getId(), row.propertyId()))
                .toList();
    }

    private byte[] generatePoliceReportWorkbook(
            List<PoliceReportRow> rows,
            List<PoliceReportColumn> selectedColumns
    ) {
        try (
                Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
        ) {
            Sheet sheet = workbook.createSheet("Báo công an");
            PoliceReportExcelStyles styles = createPoliceReportExcelStyles(workbook);

            Row headerRow = sheet.createRow(0);
            for (int columnIndex = 0; columnIndex < selectedColumns.size(); columnIndex++) {
                PoliceReportColumn column = selectedColumns.get(columnIndex);
                Cell cell = headerRow.createCell(columnIndex);
                cell.setCellStyle(styles.header());
                cell.setCellValue(column.header());
                sheet.setColumnWidth(columnIndex, column.width());
            }

            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                Row row = sheet.createRow(rowIndex + 1);
                for (int columnIndex = 0; columnIndex < selectedColumns.size(); columnIndex++) {
                    writePoliceReportCell(
                            row,
                            columnIndex,
                            selectedColumns.get(columnIndex).value(rows.get(rowIndex)),
                            styles
                    );
                }
            }

            sheet.createFreezePane(0, 1);
            sheet.setAutoFilter(new CellRangeAddress(0, rows.size(), 0, selectedColumns.size() - 1));
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new AppException(ApiErrorCode.UNDEFINED);
        }
    }

    private byte[] generatePoliceReportPackage(
            List<PoliceReportRow> rows,
            List<PoliceReportColumn> selectedColumns,
            UserPrincipal principal
    ) {
        List<PoliceReportRoomPackage> rooms = fetchPoliceReportRoomPackages(principal);
        List<String> missingFiles = new ArrayList<>();
        Set<String> usedEntries = new LinkedHashSet<>();

        try (
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                ZipOutputStream zip = new ZipOutputStream(outputStream)
        ) {
            addZipEntry(
                    zip,
                    usedEntries,
                    "danh-sach-cu-dan.xlsx",
                    generatePoliceReportWorkbook(rows, selectedColumns)
            );

            for (PoliceReportRoomPackage room : rooms) {
                writePoliceReportRoomPackage(zip, usedEntries, room, missingFiles);
            }

            if (!missingFiles.isEmpty()) {
                addZipEntry(
                        zip,
                        usedEntries,
                        "MISSING_FILES.txt",
                        String.join(System.lineSeparator(), missingFiles).getBytes(java.nio.charset.StandardCharsets.UTF_8)
                );
            }

            zip.finish();
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new AppException(ApiErrorCode.UNDEFINED);
        }
    }

    private void writePoliceReportRoomPackage(
            ZipOutputStream zip,
            Set<String> usedEntries,
            PoliceReportRoomPackage room,
            List<String> missingFiles
    ) throws IOException {
        String roomLabel = policeReportRoomLabel(room.roomCode());
        String roomFolder = roomLabel + "/";
        int residentCount = room.residents().size();
        String countToken = residentCount < 10 ? "0" + residentCount : String.valueOf(residentCount);
        PoliceReportPackageRow primaryResident = room.residents().stream()
                .filter(row -> "PRIMARY".equalsIgnoreCase(row.roomRole()))
                .findFirst()
                .orElse(room.residents().getFirst());
        String contractPrefix = roomLabel + "_" + countToken + "_" + sanitizePoliceReportToken(primaryResident.fullName(), "KhongTen");

        addPoliceReportFile(
                zip,
                usedEntries,
                roomFolder + contractPrefix + "_hop_dong_tro.pdf",
                room.leaseSignedFileId() != null ? room.leaseSignedFileId() : room.leaseContractFileId(),
                "application/pdf",
                missingFiles,
                roomLabel + " - hợp đồng trọ"
        );

        for (PoliceReportPackageRow resident : room.residents()) {
            String residentPrefix = roomLabel + "_" + countToken + "_" + sanitizePoliceReportToken(resident.fullName(), "KhongTen");
            addPoliceReportFile(
                    zip,
                    usedEntries,
                    roomFolder + residentPrefix + "_CCCD_mat_truoc.jpg",
                    resident.frontFileId(),
                    "image/jpeg",
                    missingFiles,
                    roomLabel + " - " + resident.fullName() + " - CCCD mặt trước"
            );
            addPoliceReportFile(
                    zip,
                    usedEntries,
                    roomFolder + residentPrefix + "_CCCD_mat_sau.jpg",
                    resident.backFileId(),
                    "image/jpeg",
                    missingFiles,
                    roomLabel + " - " + resident.fullName() + " - CCCD mặt sau"
            );
        }
    }

    private void addPoliceReportFile(
            ZipOutputStream zip,
            Set<String> usedEntries,
            String entryName,
            Long fileId,
            String defaultContentType,
            List<String> missingFiles,
            String missingLabel
    ) throws IOException {
        if (fileId == null) {
            missingFiles.add(missingLabel + ": chưa có file.");
            return;
        }

        try {
            FileDataResponse fileData = downloadFileUseCase.execute(new DownloadFileQuery(fileId));
            if (fileData == null || fileData.resource() == null) {
                missingFiles.add(missingLabel + ": không tìm thấy file #" + fileId + ".");
                return;
            }

            String contentType = fileData.contentType() == null ? defaultContentType : fileData.contentType();
            String resolvedEntryName = replaceExtension(entryName, extensionForContentType(contentType, defaultContentType));
            try (InputStream inputStream = fileData.resource().getInputStream()) {
                addZipEntry(zip, usedEntries, resolvedEntryName, inputStream.readAllBytes());
            }
        } catch (IOException | RuntimeException exception) {
            missingFiles.add(missingLabel + ": không đọc được file #" + fileId + ".");
        }
    }

    private void addZipEntry(
            ZipOutputStream zip,
            Set<String> usedEntries,
            String requestedName,
            byte[] bytes
    ) throws IOException {
        String entryName = uniqueZipEntryName(usedEntries, requestedName);
        zip.putNextEntry(new ZipEntry(entryName));
        zip.write(bytes);
        zip.closeEntry();
    }

    private String uniqueZipEntryName(Set<String> usedEntries, String requestedName) {
        String entryName = requestedName;
        int counter = 2;
        while (!usedEntries.add(entryName)) {
            int slashIndex = requestedName.lastIndexOf('/');
            int dotIndex = requestedName.lastIndexOf('.');
            boolean hasExtension = dotIndex > slashIndex;
            String baseName = hasExtension ? requestedName.substring(0, dotIndex) : requestedName;
            String extension = hasExtension ? requestedName.substring(dotIndex) : "";
            entryName = baseName + "_" + counter + extension;
            counter++;
        }
        return entryName;
    }

    private String replaceExtension(String filename, String extension) {
        int slashIndex = filename.lastIndexOf('/');
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex <= slashIndex) {
            return filename + extension;
        }
        return filename.substring(0, dotIndex) + extension;
    }

    private String extensionForContentType(String contentType, String defaultContentType) {
        String effectiveContentType = contentType == null ? defaultContentType : contentType.toLowerCase();
        if (effectiveContentType.contains("pdf")) {
            return ".pdf";
        }
        if (effectiveContentType.contains("png")) {
            return ".png";
        }
        if (effectiveContentType.contains("jpeg") || effectiveContentType.contains("jpg")) {
            return ".jpg";
        }
        if (effectiveContentType.contains("webp")) {
            return ".webp";
        }
        return "application/pdf".equals(defaultContentType) ? ".pdf" : ".jpg";
    }

    private List<PoliceReportRoomPackage> fetchPoliceReportRoomPackages(UserPrincipal principal) {
        List<PoliceReportPackageRow> rows = jdbcTemplate.query("""
                        SELECT resident_profiles.contract_id,
                               resident_profiles.contract_code,
                               resident_profiles.room_code,
                               resident_profiles.property_id,
                               resident_profiles.property_name,
                               resident_profiles.profile_id,
                               resident_profiles.full_name,
                               resident_profiles.room_role,
                               id_doc.front_file_id,
                               id_doc.back_file_id,
                               lc.contract_file_id AS lease_contract_file_id,
                                lc.signed_file_id AS lease_signed_file_id
                        FROM (
                            SELECT lc.lease_contract_id AS contract_id,
                                   lc.contract_code,
                                   r.room_code,
                                    p.property_id,
                                    p.name AS property_name,
                                   pp.person_profile_id AS profile_id,
                                   pp.full_name,
                                   co.occupant_role AS room_role
                            FROM lease_contracts lc
                            JOIN rooms r ON r.room_id = lc.room_id
                            JOIN properties p ON p.property_id = r.property_id
                            JOIN contract_occupants co ON co.contract_id = lc.lease_contract_id AND co.status = 'ACTIVE'
                            JOIN person_profiles pp ON pp.person_profile_id = co.tenant_profile_id
                            WHERE lc.deleted_at IS NULL
                              AND lc.status IN ('ACTIVE','EXPIRING_SOON','TERMINATION_PENDING')
                              AND NOT EXISTS (
                                  SELECT 1
                                  FROM room_transfer_requests completed_transfer
                                  WHERE completed_transfer.old_contract_id = lc.lease_contract_id
                                    AND completed_transfer.status IN ('EXECUTED','COMPLETED')
                              )
                              AND pp.deleted_at IS NULL

                            UNION ALL

                            SELECT lc.lease_contract_id AS contract_id,
                                   lc.contract_code,
                                   r.room_code,
                                    p.property_id,
                                    p.name AS property_name,
                                   pp.person_profile_id AS profile_id,
                                   pp.full_name,
                                   'PRIMARY' AS room_role
                            FROM lease_contracts lc
                            JOIN rooms r ON r.room_id = lc.room_id
                            JOIN properties p ON p.property_id = r.property_id
                            JOIN person_profiles pp ON pp.person_profile_id = lc.primary_tenant_profile_id
                            WHERE lc.deleted_at IS NULL
                              AND lc.status IN ('ACTIVE','EXPIRING_SOON','TERMINATION_PENDING')
                              AND NOT EXISTS (
                                  SELECT 1
                                  FROM room_transfer_requests completed_transfer
                                  WHERE completed_transfer.old_contract_id = lc.lease_contract_id
                                    AND completed_transfer.status IN ('EXECUTED','COMPLETED')
                              )
                              AND pp.deleted_at IS NULL
                              AND NOT EXISTS (
                                  SELECT 1
                                  FROM contract_occupants co_primary
                                  WHERE co_primary.contract_id = lc.lease_contract_id
                                    AND co_primary.tenant_profile_id = pp.person_profile_id
                                    AND co_primary.status = 'ACTIVE'
                              )
                        ) resident_profiles
                        JOIN lease_contracts lc ON lc.lease_contract_id = resident_profiles.contract_id
                         LEFT JOIN identity_documents id_doc
                          ON id_doc.identity_document_id = (
                              SELECT latest.identity_document_id
                              FROM identity_documents latest
                              WHERE latest.profile_id = resident_profiles.profile_id
                                AND latest.status = 'ACTIVE'
                              ORDER BY latest.updated_at DESC, latest.identity_document_id DESC
                              LIMIT 1
                          )
                        ORDER BY resident_profiles.property_name,
                                 resident_profiles.room_code,
                                 resident_profiles.contract_id,
                                 resident_profiles.room_role DESC,
                                 resident_profiles.full_name
                        """,
                (rs, rowNum) -> new PoliceReportPackageRow(
                         nullableLong(rs, "contract_id"),
                         rs.getString("contract_code"),
                         rs.getString("room_code"),
                         nullableLong(rs, "property_id"),
                         rs.getString("property_name"),
                        nullableLong(rs, "profile_id"),
                        rs.getString("full_name"),
                        rs.getString("room_role"),
                        nullableLong(rs, "front_file_id"),
                        nullableLong(rs, "back_file_id"),
                        nullableLong(rs, "lease_contract_file_id"),
                         nullableLong(rs, "lease_signed_file_id")
                )
        ).stream()
                .filter(row -> principal.getRole() == Role.OWNER
                        || isAssignedManager(principal.getId(), row.propertyId()))
                .toList();

        Map<Long, List<PoliceReportPackageRow>> rowsByContract = new LinkedHashMap<>();
        for (PoliceReportPackageRow row : rows) {
            rowsByContract.computeIfAbsent(row.contractId(), ignored -> new ArrayList<>()).add(row);
        }

        List<PoliceReportRoomPackage> rooms = new ArrayList<>();
        for (List<PoliceReportPackageRow> roomRows : rowsByContract.values()) {
            if (roomRows.isEmpty()) {
                continue;
            }
            PoliceReportPackageRow first = roomRows.getFirst();
            rooms.add(new PoliceReportRoomPackage(
                    first.contractId(),
                    first.contractCode(),
                    first.roomCode(),
                    first.propertyName(),
                    first.leaseContractFileId(),
                    first.leaseSignedFileId(),
                    roomRows
            ));
        }
        return rooms;
    }

    private String policeReportRoomLabel(String roomCode) {
        String roomToken = sanitizePoliceReportToken(roomCode, "Phong");
        return roomToken.toLowerCase().startsWith("phong")
                ? roomToken
                : "Phong" + roomToken;
    }

    private String sanitizePoliceReportToken(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String normalized = Normalizer.normalize(value.replace('Đ', 'D').replace('đ', 'd'), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9]", "");
        return normalized.isBlank() ? fallback : normalized;
    }

    private PoliceReportExcelStyles createPoliceReportExcelStyles(Workbook workbook) {
        Font headerFont = workbook.createFont();
        headerFont.setFontName("Arial");
        headerFont.setFontHeightInPoints((short) 11);
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());

        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setWrapText(true);

        Font dataFont = workbook.createFont();
        dataFont.setFontName("Arial");
        dataFont.setFontHeightInPoints((short) 11);

        CellStyle textStyle = workbook.createCellStyle();
        textStyle.setFont(dataFont);
        textStyle.setVerticalAlignment(VerticalAlignment.TOP);
        textStyle.setWrapText(true);

        CellStyle dateStyle = workbook.createCellStyle();
        dateStyle.cloneStyleFrom(textStyle);
        dateStyle.setDataFormat(workbook.createDataFormat().getFormat("dd/MM/yyyy"));

        return new PoliceReportExcelStyles(headerStyle, textStyle, dateStyle);
    }

    private void writePoliceReportCell(
            Row row,
            int columnIndex,
            Object value,
            PoliceReportExcelStyles styles
    ) {
        Cell cell = row.createCell(columnIndex);
        if (value instanceof LocalDate dateValue) {
            cell.setCellStyle(styles.date());
            cell.setCellValue(java.sql.Date.valueOf(dateValue));
            return;
        }
        cell.setCellStyle(styles.text());
        cell.setCellValue(value == null ? "" : String.valueOf(value));
    }

    private String formatIdentityImageLinks(Long frontFileId, Long backFileId) {
        List<String> links = new ArrayList<>();
        if (frontFileId != null) {
            links.add("Mặt trước: " + absoluteFileUrl(frontFileId));
        }
        if (backFileId != null) {
            links.add("Mặt sau: " + absoluteFileUrl(backFileId));
        }
        return String.join("\n", links);
    }

    private String absoluteFileUrl(Long fileId) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(fileUrl(fileId))
                .toUriString();
    }

    private UserPrincipal requireCurrentPrincipal() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new AppException(ApiErrorCode.UNAUTHENTICATED);
        }
        return principal;
    }

    private boolean isAssignedManager(Long managerId, Long propertyId) {
        if (managerId == null || propertyId == null) {
            return false;
        }
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM role_promotions
                        WHERE user_id = ?
                          AND property_id = ?
                          AND role = 'MANAGER'
                          AND status = 'ACTIVE'
                          AND deleted_at IS NULL
                        """,
                Integer.class,
                managerId,
                propertyId
        );
        return count != null && count > 0;
    }

    private TenantProfileRow mapTenantProfileRow(ResultSet rs) throws SQLException {
        return new TenantProfileRow(
                rs.getLong("contract_id"),
                rs.getString("contract_code"),
                rs.getString("contract_status"),
                nullableLocalDate(rs, "start_date"),
                nullableLocalDate(rs, "end_date"),
                nullableLong(rs, "monthly_rent"),
                nullableLong(rs, "deposit_amount"),
                nullableLong(rs, "room_id"),
                rs.getString("room_code"),
                nullableInt(rs, "max_occupants"),
                nullableLong(rs, "property_id"),
                rs.getString("property_name"),
                rs.getString("property_address"),
                nullableLong(rs, "profile_id"),
                nullableLong(rs, "user_id"),
                rs.getString("full_name"),
                nullableLocalDate(rs, "dob"),
                rs.getString("gender"),
                rs.getString("phone"),
                rs.getString("email"),
                rs.getString("permanent_address"),
                nullableLong(rs, "portrait_file_id"),
                rs.getString("app_status"),
                rs.getString("room_role"),
                nullableLocalDate(rs, "move_in_date"),
                nullableLocalDate(rs, "move_out_date"),
                rs.getString("residence_status")
        );
    }

    private IdentityDocumentResponse getIdentityDocument(Long profileId) {
        if (profileId == null) {
            return null;
        }

        List<IdentityDocumentResponse> documents = jdbcTemplate.query("""
                        SELECT identity_document_id AS id,
                               doc_type,
                               doc_number,
                               issued_date,
                               issued_place,
                               expiry_date,
                               front_file_id,
                               back_file_id,
                               status
                        FROM identity_documents
                        WHERE profile_id = ?
                          AND status = 'ACTIVE'
                        ORDER BY updated_at DESC, identity_document_id DESC
                        LIMIT 1
                        """,
                (rs, rowNum) -> new IdentityDocumentResponse(
                        nullableLong(rs, "id"),
                        rs.getString("doc_type"),
                        normalizeIdentityNumber(rs.getString("doc_number")),
                        nullableLocalDate(rs, "issued_date"),
                        rs.getString("issued_place"),
                        nullableLocalDate(rs, "expiry_date"),
                        nullableLong(rs, "front_file_id"),
                        nullableLong(rs, "back_file_id"),
                        fileUrl(nullableLong(rs, "front_file_id")),
                        fileUrl(nullableLong(rs, "back_file_id")),
                        rs.getString("status")
                ),
                profileId
        );

        return documents.isEmpty() ? null : documents.getFirst();
    }

    private List<VehicleResponse> getVehicles(Long profileId) {
        if (profileId == null) {
            return List.of();
        }

        return jdbcTemplate.query("""
                        SELECT vehicle_id AS id,
                               vehicle_type,
                               license_plate,
                               image_file_id,
                               status
                        FROM vehicles
                        WHERE profile_id = ?
                          AND deleted_at IS NULL
                          AND status = 'ACTIVE'
                        ORDER BY vehicle_id
                        """,
                (rs, rowNum) -> new VehicleResponse(
                        nullableLong(rs, "id"),
                        rs.getString("vehicle_type"),
                        rs.getString("license_plate"),
                        nullableLong(rs, "image_file_id"),
                        fileUrl(nullableLong(rs, "image_file_id")),
                        rs.getString("status")
                ),
                profileId
        );
    }

    private List<EmergencyContactResponse> getEmergencyContacts(Long profileId) {
        if (profileId == null) {
            return List.of();
        }

        return jdbcTemplate.query("""
                        SELECT emergency_contact_id AS id,
                               full_name,
                               relationship,
                               phone
                        FROM emergency_contacts
                        WHERE tenant_profile_id = ?
                        ORDER BY emergency_contact_id
                        """,
                (rs, rowNum) -> new EmergencyContactResponse(
                        nullableLong(rs, "id"),
                        rs.getString("full_name"),
                        rs.getString("relationship"),
                        rs.getString("phone")
                ),
                profileId
        );
    }

    private RoommateResponse toRoommateResponse(TenantProfileRow row) {
        return new RoommateResponse(
                row.profileId(),
                row.fullName(),
                row.dob(),
                row.phone(),
                row.roomRole()
        );
    }

    private ProfileStatus resolveProfileStatus(
            TenantProfileRow row,
            IdentityDocumentResponse identityDocument
    ) {
        if (identityDocument == null
                || identityDocument.docNumber() == null
                || identityDocument.docNumber().isBlank()
                || identityDocument.frontFileId() == null
                || identityDocument.backFileId() == null) {
            return new ProfileStatus("MISSING_CCCD", "Thiếu CCCD");
        }
        if (row.portraitFileId() == null) {
            return new ProfileStatus("MISSING_PORTRAIT", "Thiếu ảnh chân dung");
        }
        return new ProfileStatus("COMPLETED", "Hồ sơ đủ");
    }

    private String fileUrl(Long fileId) {
        return fileId == null ? null : "/api/v1/files/private/" + fileId;
    }

    private String normalizeIdentityNumber(String value) {
        if (value == null || value.isBlank() || value.startsWith("PENDING-")) {
            return null;
        }
        return value;
    }

    private Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private Integer nullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private LocalDate nullableLocalDate(ResultSet rs, String column) throws SQLException {
        java.sql.Date value = rs.getDate(column);
        return value == null ? null : value.toLocalDate();
    }

    private record TenantProfileRow(
            Long contractId,
            String contractCode,
            String contractStatus,
            LocalDate startDate,
            LocalDate endDate,
            Long monthlyRent,
            Long depositAmount,
            Long roomId,
            String roomCode,
            Integer maxOccupants,
            Long propertyId,
            String propertyName,
            String propertyAddress,
            Long profileId,
            Long userId,
            String fullName,
            LocalDate dob,
            String gender,
            String phone,
            String email,
            String permanentAddress,
            Long portraitFileId,
            String appStatus,
            String roomRole,
            LocalDate moveInDate,
            LocalDate moveOutDate,
            String residenceStatus
    ) {
    }

    private record ProfileStatus(String code, String label) {
    }

    public record TenantProfileSummaryResponse(
            Long id,
            Long userId,
            String fullName,
            LocalDate dob,
            String gender,
            String phone,
            String email,
            String permanentAddress,
            String portraitUrl,
            Long portraitFileId,
            IdentityDocumentResponse identityDocument,
            Long propertyId,
            String propertyName,
            String propertyAddress,
            Long roomId,
            String roomCode,
            String roomRole,
            Integer roomOccupantCount,
            Integer roomMaxOccupants,
            LocalDate moveInDate,
            LocalDate moveOutDate,
            String residenceStatus,
            String appStatus,
            String profileStatus,
            String profileStatusLabel,
            Long contractId,
            String contractCode,
            String contractStatus,
            LocalDate contractStartDate,
            LocalDate contractEndDate,
            Long monthlyRent,
            Long depositAmount,
            List<VehicleResponse> vehicles,
            List<EmergencyContactResponse> emergencyContacts,
            List<RoommateResponse> roommates,
            String profileAccessStatus,
            Long profileAccessRequestId,
            Boolean canViewSensitiveProfile,
            Long profileAccessGrantId,
            LocalDateTime profileAccessExpiresAt,
            String profileAccessDurationCode
    ) {
    }

    public record IdentityDocumentResponse(
            Long id,
            String docType,
            String docNumber,
            LocalDate issuedDate,
            String issuedPlace,
            LocalDate expiryDate,
            Long frontFileId,
            Long backFileId,
            String frontFileUrl,
            String backFileUrl,
            String status
    ) {
    }

    public record VehicleResponse(
            Long id,
            String vehicleType,
            String licensePlate,
            Long imageFileId,
            String imageUrl,
            String status
    ) {
    }

    public record EmergencyContactResponse(
            Long id,
            String fullName,
            String relationship,
            String phone
    ) {
    }

    public record RoommateResponse(
            Long id,
            String fullName,
            LocalDate dob,
            String phone,
            String roomRole
    ) {
    }

    private record PoliceReportRow(
            Long propertyId,
            String propertyName,
            String roomCode,
            String contractCode,
            String fullName,
            String cccdNumber,
            String documentType,
            LocalDate dateOfBirth,
            String gender,
            String phone,
            String permanentAddress,
            LocalDate issuedDate,
            String issuedPlace,
            LocalDate expiryDate,
            String cccdImageLinks
    ) {
    }

    private record PoliceReportPackageRow(
            Long contractId,
            String contractCode,
            String roomCode,
            Long propertyId,
            String propertyName,
            Long profileId,
            String fullName,
            String roomRole,
            Long frontFileId,
            Long backFileId,
            Long leaseContractFileId,
            Long leaseSignedFileId
    ) {
    }

    private record PoliceReportRoomPackage(
            Long contractId,
            String contractCode,
            String roomCode,
            String propertyName,
            Long leaseContractFileId,
            Long leaseSignedFileId,
            List<PoliceReportPackageRow> residents
    ) {
    }

    private record PoliceReportExcelStyles(
            CellStyle header,
            CellStyle text,
            CellStyle date
    ) {
    }

    private enum PoliceReportColumn {
        PROPERTY_NAME("propertyName", "Cơ sở", 28 * 256),
        ROOM_CODE("roomCode", "Phòng", 16 * 256),
        CONTRACT_CODE("contractCode", "Mã hợp đồng", 22 * 256),
        FULL_NAME("fullName", "Họ tên", 28 * 256),
        CCCD_NUMBER("cccdNumber", "CCCD", 18 * 256),
        DOCUMENT_TYPE("documentType", "Loại giấy tờ", 18 * 256),
        DATE_OF_BIRTH("dateOfBirth", "Ngày sinh", 16 * 256),
        GENDER("gender", "Giới tính", 14 * 256),
        PHONE("phone", "Số điện thoại", 18 * 256),
        PERMANENT_ADDRESS("permanentAddress", "Địa chỉ thường trú", 44 * 256),
        ISSUED_DATE("issuedDate", "Ngày cấp", 16 * 256),
        ISSUED_PLACE("issuedPlace", "Nơi cấp", 32 * 256),
        EXPIRY_DATE("expiryDate", "Ngày hết hạn", 16 * 256),
        CCCD_IMAGE_LINKS("cccdImageLinks", "Link ảnh CCCD", 72 * 256);

        private final String key;
        private final String header;
        private final int width;

        PoliceReportColumn(String key, String header, int width) {
            this.key = key;
            this.header = header;
            this.width = width;
        }

        static PoliceReportColumn fromKey(String key) {
            for (PoliceReportColumn column : values()) {
                if (column.key.equalsIgnoreCase(key)) {
                    return column;
                }
            }
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }

        String header() {
            return header;
        }

        int width() {
            return width;
        }

        Object value(PoliceReportRow row) {
            return switch (this) {
                case PROPERTY_NAME -> row.propertyName();
                case ROOM_CODE -> row.roomCode();
                case CONTRACT_CODE -> row.contractCode();
                case FULL_NAME -> row.fullName();
                case CCCD_NUMBER -> row.cccdNumber();
                case DOCUMENT_TYPE -> row.documentType();
                case DATE_OF_BIRTH -> row.dateOfBirth();
                case GENDER -> row.gender();
                case PHONE -> row.phone();
                case PERMANENT_ADDRESS -> row.permanentAddress();
                case ISSUED_DATE -> row.issuedDate();
                case ISSUED_PLACE -> row.issuedPlace();
                case EXPIRY_DATE -> row.expiryDate();
                case CCCD_IMAGE_LINKS -> row.cccdImageLinks();
            };
        }
    }
}
