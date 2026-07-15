package com.sep490.hdbhms.occupancy.infrastructure.web.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep490.hdbhms.changerequest.application.port.out.ChangeRequestRepository;
import com.sep490.hdbhms.changerequest.domain.model.ChangeRequest;
import com.sep490.hdbhms.changerequest.domain.value_objects.AssignedRole;
import com.sep490.hdbhms.changerequest.domain.value_objects.RequestStatus;
import com.sep490.hdbhms.changerequest.domain.value_objects.RequestType;
import com.sep490.hdbhms.changerequest.domain.value_objects.RequesterRole;
import com.sep490.hdbhms.changerequest.domain.value_objects.TargetType;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import com.sep490.hdbhms.identityandaccess.infrastructure.config.security.UserPrincipal;
import com.sep490.hdbhms.notification.application.service.BusinessNotificationPublisher;
import com.sep490.hdbhms.permissiongrant.application.service.PermissionGrantService;
import com.sep490.hdbhms.permissiongrant.domain.model.PermissionGrant;
import com.sep490.hdbhms.permissiongrant.domain.value_objects.PermissionAccessAction;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import com.sep490.hdbhms.shared.dto.response.PageResponse;
import com.sep490.hdbhms.shared.id.SnowflakeIdGenerator;
import com.sep490.hdbhms.shared.utils.DocumentFilenameBuilder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/tenant-profiles")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TenantProfileManagementController {
    private static final String EXCEL_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final List<PoliceReportColumn> DEFAULT_POLICE_REPORT_COLUMNS = List.of(
            PoliceReportColumn.FULL_NAME,
            PoliceReportColumn.CCCD_NUMBER,
            PoliceReportColumn.DATE_OF_BIRTH,
            PoliceReportColumn.PERMANENT_ADDRESS,
            PoliceReportColumn.CCCD_IMAGE_LINKS
    );

    JdbcTemplate jdbcTemplate;
    ChangeRequestRepository changeRequestRepository;
    BusinessNotificationPublisher notificationPublisher;
    ObjectMapper objectMapper;
    SnowflakeIdGenerator snowflakeIdGenerator;
    PermissionGrantService permissionGrantService;

    @GetMapping
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('OWNER') or hasRole('MANAGER')")
    public ApiResponse<PageResponse<TenantProfileSummaryResponse>> getTenantProfiles(
            @PageableDefault(size = 10) Pageable pageable
    ) {
        UserPrincipal principal = requireCurrentPrincipal();
        boolean isOwner = principal.getRole() == Role.OWNER;
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
            ProfileAccessDecision accessDecision = resolveProfileAccess(row.profileId(), principal, isOwner);
            boolean canViewSensitiveProfile = accessDecision.canViewSensitiveProfile();
            if (canViewSensitiveProfile && isManager && accessDecision.grantId() != null) {
                permissionGrantService.recordAccess(
                        PermissionGrant.builder().id(accessDecision.grantId()).build(),
                        principal.getId(),
                        TargetType.TENANT_PROFILE,
                        row.profileId(),
                        PermissionAccessAction.VIEW_TENANT_PROFILE
                );
            }
            IdentityDocumentResponse identityDocument = canViewSensitiveProfile ? getIdentityDocument(row.profileId()) : null;
            List<VehicleResponse> vehicles = canViewSensitiveProfile ? getVehicles(row.profileId()) : List.of();
            List<EmergencyContactResponse> emergencyContacts = canViewSensitiveProfile
                    ? getEmergencyContacts(row.profileId())
                    : List.of();
            ProfileStatus profileStatus = canViewSensitiveProfile
                    ? resolveProfileStatus(row, identityDocument, emergencyContacts)
                    : restrictedProfileStatus(accessDecision);
            List<RoommateResponse> roommates = canViewSensitiveProfile
                    ? roomRows.stream()
                    .filter(roommate -> !Objects.equals(roommate.profileId(), row.profileId())
                            || !Objects.equals(roommate.phone(), row.phone()))
                    .map(this::toRoommateResponse)
                    .toList()
                    : List.of();

            response.add(new TenantProfileSummaryResponse(
                    row.profileId(),
                    row.userId(),
                    row.fullName(),
                    row.dob(),
                    row.gender(),
                    canViewSensitiveProfile ? row.phone() : maskPhone(row.phone()),
                    canViewSensitiveProfile ? row.email() : maskEmail(row.email()),
                    canViewSensitiveProfile ? row.permanentAddress() : null,
                    canViewSensitiveProfile ? fileUrl(row.portraitFileId()) : null,
                    canViewSensitiveProfile ? row.portraitFileId() : null,
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
                    accessDecision.status(),
                    accessDecision.requestId(),
                    accessDecision.canViewSensitiveProfile(),
                    accessDecision.grantId(),
                    accessDecision.expiresAt(),
                    accessDecision.durationCode()
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
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Resource> exportPoliceReport(
            @RequestParam(name = "columns", required = false) List<String> columns
    ) {
        List<PoliceReportColumn> selectedColumns = resolvePoliceReportColumns(columns);
        List<PoliceReportRow> rows = fetchPoliceReportRows();
        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chưa có dữ liệu cư dân để xuất.");
        }

        byte[] bytes = generatePoliceReportWorkbook(rows, selectedColumns);
        String filename = "Danh sách cư dân báo công an " + LocalDate.now() + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, DocumentFilenameBuilder.attachmentContentDisposition(filename))
                .contentType(MediaType.parseMediaType(EXCEL_CONTENT_TYPE))
                .body(new ByteArrayResource(bytes));
    }

    @PostMapping("/{profileId}/access-requests")
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @PreAuthorize("hasRole('MANAGER')")
    public ApiResponse<TenantProfileAccessRequestResponse> requestTenantProfileAccess(
            @PathVariable Long profileId,
            @Valid @RequestBody(required = false) TenantProfileAccessRequest request
    ) {
        UserPrincipal principal = requireCurrentPrincipal();
        TenantProfileAccessContext context = getTenantProfileAccessContext(profileId);
        if (!isAssignedManager(principal.getId(), context.propertyId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Manager is not assigned to this property.");
        }

        ProfileAccessDecision existingAccess = resolveProfileAccess(profileId, principal, false);
        if (existingAccess.canViewSensitiveProfile() || "PENDING".equals(existingAccess.status())) {
            return ApiResponse.<TenantProfileAccessRequestResponse>builder()
                    .data(new TenantProfileAccessRequestResponse(
                            existingAccess.requestId(),
                            existingAccess.status(),
                            existingAccess.canViewSensitiveProfile()
                    ))
                    .build();
        }

        String reason = trimToNull(request == null ? null : request.reason());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tenantProfileId", context.profileId());
        payload.put("contractId", context.contractId());
        payload.put("contractCode", context.contractCode());
        payload.put("propertyId", context.propertyId());
        payload.put("roomCode", context.roomCode());
        payload.put("propertyName", context.propertyName());
        payload.put("fullName", context.fullName());
        payload.put("reason", reason);

        ChangeRequest changeRequest = ChangeRequest.builder()
                .requestCode("CR-" + snowflakeIdGenerator.next())
                .requestType(RequestType.TENANT_PROFILE_ACCESS)
                .requesterId(principal.getId())
                .requesterRole(RequesterRole.MANAGER)
                .targetType(TargetType.TENANT_PROFILE)
                .targetId(profileId)
                .title("Yêu cầu xem hồ sơ " + context.fullName())
                .description(reason == null ? "Manager yêu cầu xem hồ sơ khách thuê." : reason)
                .requestPayload(toJson(payload))
                .assignedRole(AssignedRole.OWNER)
                .status(RequestStatus.PENDING)
                .build();
        ChangeRequest savedRequest = changeRequestRepository.save(changeRequest);
        notifyOwnersProfileAccessRequested(savedRequest, context, reason);

        return ApiResponse.<TenantProfileAccessRequestResponse>builder()
                .data(new TenantProfileAccessRequestResponse(savedRequest.getId(), savedRequest.getStatus().name(), false))
                .build();
    }

    private void notifyOwnersProfileAccessRequested(
            ChangeRequest request,
            TenantProfileAccessContext context,
            String reason
    ) {
        List<Long> ownerIds = jdbcTemplate.queryForList("""
                        SELECT user_id
                        FROM users
                        WHERE role = 'OWNER'
                          AND status = 'ACTIVE'
                          AND deleted_at IS NULL
                        """,
                Long.class
        );
        if (ownerIds.isEmpty()) {
            return;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("changeRequestId", request.getId());
        payload.put("requestId", request.getId());
        payload.put("requestCode", request.getRequestCode());
        payload.put("tenantProfileId", context.profileId());
        payload.put("profileId", context.profileId());
        payload.put("contractId", context.contractId());
        payload.put("contractCode", context.contractCode());
        payload.put("propertyId", context.propertyId());
        payload.put("roomCode", context.roomCode());
        payload.put("roomName", context.roomCode());
        payload.put("propertyName", context.propertyName());
        payload.put("fullName", context.fullName());
        payload.put("tenantName", context.fullName());
        payload.put("managerId", request.getRequesterId());
        payload.put("managerName", "Quản lý");
        payload.put("reason", reason);
        payload.put("targetRoute", "/dashboard/change-requests/" + request.getId());

        for (Long ownerId : ownerIds) {
            notificationPublisher.publish(
                    "TENANT_PROFILE_ACCESS_REQUESTED",
                    ownerId,
                    "CHANGE_REQUEST",
                    request.getId(),
                    payload
            );
        }
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn ít nhất một cột để xuất.");
        }

        List<PoliceReportColumn> selectedColumns = new ArrayList<>();
        for (String columnKey : columnKeys) {
            selectedColumns.add(PoliceReportColumn.fromKey(columnKey));
        }
        return selectedColumns;
    }

    private List<PoliceReportRow> fetchPoliceReportRows() {
        return jdbcTemplate.query("""
                        SELECT resident_profiles.full_name,
                               resident_profiles.dob,
                               resident_profiles.permanent_address,
                               id_doc.doc_number,
                               id_doc.front_file_id,
                               id_doc.back_file_id
                        FROM (
                            SELECT lc.lease_contract_id AS contract_id,
                                   r.room_code,
                                   p.name AS property_name,
                                   pp.person_profile_id AS profile_id,
                                   pp.full_name,
                                   pp.dob,
                                   pp.permanent_address,
                                   co.occupant_role AS room_role
                            FROM lease_contracts lc
                            JOIN rooms r ON r.room_id = lc.room_id
                            JOIN properties p ON p.property_id = r.property_id
                            JOIN contract_occupants co ON co.contract_id = lc.lease_contract_id AND co.status = 'ACTIVE'
                            JOIN person_profiles pp ON pp.person_profile_id = co.tenant_profile_id
                            WHERE lc.deleted_at IS NULL
                              AND lc.status IN ('ACTIVE','EXPIRING_SOON','TERMINATION_PENDING')
                              AND pp.deleted_at IS NULL

                            UNION ALL

                            SELECT lc.lease_contract_id AS contract_id,
                                   r.room_code,
                                   p.name AS property_name,
                                   pp.person_profile_id AS profile_id,
                                   pp.full_name,
                                   pp.dob,
                                   pp.permanent_address,
                                   'PRIMARY' AS room_role
                            FROM lease_contracts lc
                            JOIN rooms r ON r.room_id = lc.room_id
                            JOIN properties p ON p.property_id = r.property_id
                            JOIN person_profiles pp ON pp.person_profile_id = lc.primary_tenant_profile_id
                            WHERE lc.deleted_at IS NULL
                              AND lc.status IN ('ACTIVE','EXPIRING_SOON','TERMINATION_PENDING')
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
                        rs.getString("full_name"),
                        normalizeIdentityNumber(rs.getString("doc_number")),
                        nullableLocalDate(rs, "dob"),
                        rs.getString("permanent_address"),
                        formatIdentityImageLinks(
                                nullableLong(rs, "front_file_id"),
                                nullableLong(rs, "back_file_id")
                        )
                )
        );
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
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Xuất file Excel thất bại, vui lòng thử lại.");
        }
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
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthenticated.");
        }
        return principal;
    }

    private TenantProfileAccessContext getTenantProfileAccessContext(Long profileId) {
        List<TenantProfileAccessContext> contexts = jdbcTemplate.query("""
                        SELECT pp.person_profile_id AS profile_id,
                               pp.full_name,
                               lc.lease_contract_id AS contract_id,
                               lc.contract_code,
                               r.room_code,
                               p.property_id,
                               p.name AS property_name
                        FROM person_profiles pp
                        JOIN lease_contracts lc
                          ON lc.deleted_at IS NULL
                         AND lc.status IN ('ACTIVE','EXPIRING_SOON','TERMINATION_PENDING')
                         AND (
                             lc.primary_tenant_profile_id = pp.person_profile_id
                             OR EXISTS (
                                 SELECT 1
                                 FROM contract_occupants co
                                 WHERE co.contract_id = lc.lease_contract_id
                                   AND co.tenant_profile_id = pp.person_profile_id
                                   AND co.status = 'ACTIVE'
                             )
                         )
                        JOIN rooms r ON r.room_id = lc.room_id
                        JOIN properties p ON p.property_id = r.property_id
                        WHERE pp.person_profile_id = ?
                          AND pp.deleted_at IS NULL
                        ORDER BY lc.start_date DESC, lc.lease_contract_id DESC
                        LIMIT 1
                        """,
                (rs, rowNum) -> new TenantProfileAccessContext(
                        nullableLong(rs, "profile_id"),
                        rs.getString("full_name"),
                        nullableLong(rs, "contract_id"),
                        rs.getString("contract_code"),
                        rs.getString("room_code"),
                        nullableLong(rs, "property_id"),
                        rs.getString("property_name")
                ),
                profileId
        );
        if (contexts.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant profile not found.");
        }
        return contexts.getFirst();
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

    private ProfileAccessDecision resolveProfileAccess(Long profileId, UserPrincipal principal, boolean isOwner) {
        if (isOwner) {
            return new ProfileAccessDecision("APPROVED", null, true, null, null, null);
        }
        if (profileId == null || principal == null || principal.getRole() != Role.MANAGER) {
            return new ProfileAccessDecision("NONE", null, false, null, null, null);
        }

        var activeGrant = permissionGrantService.findActiveTenantProfileGrant(principal.getId(), profileId);
        if (activeGrant.isPresent()) {
            PermissionGrant grant = activeGrant.get();
            return new ProfileAccessDecision(
                    "APPROVED",
                    grant.getSourceChangeRequestId(),
                    true,
                    grant.getId(),
                    grant.getExpiresAt(),
                    grant.getDurationCode() == null ? null : grant.getDurationCode().name()
            );
        }

        List<ProfileAccessDecision> decisions = jdbcTemplate.query("""
                        SELECT change_request_id AS request_id,
                               status
                        FROM change_requests
                        WHERE request_type = 'TENANT_PROFILE_ACCESS'
                          AND target_type = 'TENANT_PROFILE'
                          AND target_id = ?
                          AND requester_id = ?
                        ORDER BY created_at DESC,
                                 change_request_id DESC
                        LIMIT 1
                        """,
                (rs, rowNum) -> {
                    String status = rs.getString("status");
                    return new ProfileAccessDecision(
                            status,
                            nullableLong(rs, "request_id"),
                            false,
                            null,
                            null,
                            null
                    );
                },
                profileId,
                principal.getId()
        );
        ProfileAccessDecision latestRequest = decisions.isEmpty() ? null : decisions.getFirst();
        if (latestRequest != null && ("PENDING".equals(latestRequest.status()) || "REJECTED".equals(latestRequest.status()))) {
            return latestRequest;
        }

        return permissionGrantService.findLatestTenantProfileGrant(principal.getId(), profileId)
                .map(grant -> new ProfileAccessDecision(
                        permissionGrantService.statusOf(grant),
                        grant.getSourceChangeRequestId(),
                        false,
                        grant.getId(),
                        grant.getExpiresAt(),
                        grant.getDurationCode() == null ? null : grant.getDurationCode().name()
                ))
                .orElseGet(() -> latestRequest == null
                        ? new ProfileAccessDecision("NONE", null, false, null, null, null)
                        : latestRequest);
    }

    private ProfileStatus restrictedProfileStatus(ProfileAccessDecision accessDecision) {
        return switch (accessDecision.status()) {
            case "EXPIRED" -> new ProfileStatus("ACCESS_EXPIRED", "Quyền xem đã hết hạn");
            case "REVOKED" -> new ProfileStatus("ACCESS_REVOKED", "Quyền xem đã bị thu hồi");
            case "PENDING" -> new ProfileStatus("ACCESS_PENDING", "Chờ chủ trọ duyệt");
            case "REJECTED" -> new ProfileStatus("ACCESS_REJECTED", "Yêu cầu bị từ chối");
            default -> new ProfileStatus("ACCESS_REQUIRED", "Cần gửi yêu cầu");
        };
    }

    private String maskPhone(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String digits = value.replaceAll("\\D", "");
        if (digits.length() < 4) {
            return "***";
        }
        return "*** *** " + digits.substring(digits.length() - 3);
    }

    private String maskEmail(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        int atIndex = value.indexOf('@');
        if (atIndex <= 1) {
            return "***";
        }
        return value.charAt(0) + "***" + value.substring(atIndex);
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not serialize request payload.");
        }
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
            IdentityDocumentResponse identityDocument,
            List<EmergencyContactResponse> emergencyContacts
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
        if (emergencyContacts == null || emergencyContacts.isEmpty()) {
            return new ProfileStatus("MISSING_EMERGENCY_CONTACT", "Thiếu liên hệ khẩn cấp");
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

    private record ProfileAccessDecision(
            String status,
            Long requestId,
            boolean canViewSensitiveProfile,
            Long grantId,
            LocalDateTime expiresAt,
            String durationCode
    ) {
    }

    private record TenantProfileAccessContext(
            Long profileId,
            String fullName,
            Long contractId,
            String contractCode,
            String roomCode,
            Long propertyId,
            String propertyName
    ) {
    }

    public record TenantProfileAccessRequest(
            @Size(max = 1000, message = "Lý do không được vượt quá 1000 ký tự.")
            String reason
    ) {
    }

    public record TenantProfileAccessRequestResponse(
            Long requestId,
            String status,
            Boolean canViewSensitiveProfile
    ) {
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
            String fullName,
            String cccdNumber,
            LocalDate dateOfBirth,
            String permanentAddress,
            String cccdImageLinks
    ) {
    }

    private record PoliceReportExcelStyles(
            CellStyle header,
            CellStyle text,
            CellStyle date
    ) {
    }

    private enum PoliceReportColumn {
        FULL_NAME("fullName", "Họ tên", 28 * 256),
        CCCD_NUMBER("cccdNumber", "CCCD", 18 * 256),
        DATE_OF_BIRTH("dateOfBirth", "Ngày sinh", 16 * 256),
        PERMANENT_ADDRESS("permanentAddress", "Địa chỉ thường trú", 44 * 256),
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cột xuất Excel không hợp lệ: " + key);
        }

        String header() {
            return header;
        }

        int width() {
            return width;
        }

        Object value(PoliceReportRow row) {
            return switch (this) {
                case FULL_NAME -> row.fullName();
                case CCCD_NUMBER -> row.cccdNumber();
                case DATE_OF_BIRTH -> row.dateOfBirth();
                case PERMANENT_ADDRESS -> row.permanentAddress();
                case CCCD_IMAGE_LINKS -> row.cccdImageLinks();
            };
        }
    }
}
