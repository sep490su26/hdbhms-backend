//package com.sep490.hdbhms.modules.maintenance.service;
//
//import com.sep490.hdbhms.common.AuditService;
//import com.sep490.hdbhms.common.exception.ApiException;
//import com.sep490.hdbhms.modules.maintenance.dto.ConfirmAndReviewResponse;
//import com.sep490.hdbhms.modules.maintenance.dto.MaintenanceTicketActionResponse;
//import com.sep490.hdbhms.modules.maintenance.dto.MaintenanceTicketDetailResponse;
//import com.sep490.hdbhms.modules.maintenance.dto.MaintenanceTicketListResponse;
//import com.sep490.hdbhms.modules.maintenance.dto.MaintenanceTicketRequests;
//import com.sep490.hdbhms.modules.maintenance.dto.MaintenanceTicketResponse;
//import com.sep490.hdbhms.modules.maintenance.dto.MaintenanceTicketReviewResponse;
//import java.math.BigDecimal;
//import java.math.RoundingMode;
//import java.sql.PreparedStatement;
//import java.sql.ResultSet;
//import java.sql.Statement;
//import java.sql.Timestamp;
//import java.sql.Types;
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.time.LocalTime;
//import java.time.OffsetDateTime;
//import java.time.ZoneId;
//import java.time.format.DateTimeFormatter;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Locale;
//import java.util.Set;
//import org.springframework.http.HttpStatus;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.jdbc.support.GeneratedKeyHolder;
//import org.springframework.jdbc.support.KeyHolder;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.util.StringUtils;
//import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
//
//@Service
//public class MaintenanceTicketQueryService {
//
//    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Bangkok");
//    private static final DateTimeFormatter DISPLAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
//    private static final Set<String> VALID_STATUSES = Set.of(
//            "PENDING_ACCEPTANCE",
//            "ACCEPTED",
//            "IN_PROGRESS",
//            "WAITING_CONFIRMATION",
//            "COMPLETED",
//            "REJECTED",
//            "CANCELLED"
//    );
//    private static final List<String> ELECTRIC_ALIASES = List.of("ELECTRIC", "ELECTRICAL", "POWER");
//    private static final List<String> WATER_ALIASES = List.of("WATER", "PLUMBING", "WATER_LEAK");
//    private static final List<String> EQUIPMENT_ALIASES = List.of("EQUIPMENT", "ASSET", "ASSET_DAMAGE", "APPLIANCE");
//    private static final List<String> ELECTRIC_KEYWORDS = List.of("dien", "điện", "den", "đèn", "o cam", "ổ cắm", "aptomat");
//    private static final List<String> WATER_KEYWORDS = List.of("nuoc", "nước", "voi", "vòi", "sen", "bon", "bồn", "ro ri", "rò rỉ", "ong nuoc", "ống nước");
//    private static final List<String> EQUIPMENT_KEYWORDS = List.of(
//            "thiet bi",
//            "thiết bị",
//            "dieu hoa",
//            "điều hòa",
//            "may lanh",
//            "máy lạnh",
//            "tủ",
//            "giuong",
//            "giường",
//            "khoa",
//            "khóa",
//            "remote"
//    );
//
//    private final JdbcTemplate jdbcTemplate;
//    private final AuditService auditService;
//
//    public MaintenanceTicketQueryService(JdbcTemplate jdbcTemplate, AuditService auditService) {
//        this.jdbcTemplate = jdbcTemplate;
//        this.auditService = auditService;
//    }
//
//    public MaintenanceTicketListResponse listTickets(Long userId, Long tenantId, Query query) {
//        TenantAccess access = resolveTenantAccess(userId, tenantId);
//        NormalizedQuery normalized = normalize(query);
//        SqlParts where = buildWhereClause(access, normalized);
//
//        Long totalResult = jdbcTemplate.queryForObject("""
//                SELECT COUNT(*)
//                FROM maintenance_tickets mt
//                LEFT JOIN rooms r ON r.id = mt.room_id
//                """.concat(where.sql()), Long.class, where.args().toArray());
//        long total = totalResult == null ? 0 : totalResult;
//
//        if (total == 0) {
//            return new MaintenanceTicketListResponse(
//                    List.of(),
//                    normalized.page(),
//                    normalized.size(),
//                    0,
//                    normalized.hasFilters() ? "Không tìm thấy sự cố phù hợp" : "Bạn chưa có phiếu sự cố nào"
//            );
//        }
//
//        List<Object> args = new ArrayList<>(where.args());
//        args.add(normalized.size());
//        args.add((normalized.page() - 1) * normalized.size());
//
//        List<MaintenanceTicketListResponse.Item> items = jdbcTemplate.query("""
//                SELECT mt.id,
//                       mt.ticket_code,
//                       mt.room_id,
//                       r.room_code,
//                       mt.title,
//                       mt.description,
//                       mt.ticket_scope,
//                       mt.category,
//                       mt.priority,
//                       mt.status,
//                       (
//                         SELECT COUNT(*)
//                         FROM maintenance_ticket_attachments mta
//                         WHERE mta.ticket_id = mt.id
//                       ) AS attachment_count,
//                       COALESCE(
//                         mt.rejection_reason,
//                         (
//                           SELECT mte.note
//                           FROM maintenance_ticket_events mte
//                           WHERE mte.ticket_id = mt.id
//                             AND mte.to_status = 'REJECTED'
//                           ORDER BY mte.created_at DESC, mte.id DESC
//                           LIMIT 1
//                         )
//                       ) AS rejected_reason,
//                       mt.created_at
//                FROM maintenance_tickets mt
//                LEFT JOIN rooms r ON r.id = mt.room_id
//                """
//                        + where.sql()
//                        + """
//                ORDER BY
//                  CASE
//                    WHEN mt.status = 'PENDING_ACCEPTANCE' THEN 0
//                    WHEN mt.status = 'IN_PROGRESS' THEN 1
//                    WHEN mt.status = 'ACCEPTED' THEN 2
//                    WHEN mt.status = 'WAITING_CONFIRMATION' THEN 3
//                    ELSE 4
//                  END ASC,
//                  mt.created_at DESC
//                LIMIT ? OFFSET ?
//                """, (rs, rowNum) -> mapItem(rs), args.toArray());
//
//        return new MaintenanceTicketListResponse(
//                items,
//                normalized.page(),
//                normalized.size(),
//                total,
//                null
//        );
//    }
//
//    @Transactional
//    public MaintenanceTicketResponse createTicket(
//            Long userId,
//            Long tenantId,
//            MaintenanceTicketRequests.CreateTicketRequest request
//    ) {
//        TenantAccess access = resolveTenantAccess(userId, tenantId);
//        if (request == null || request.roomId() == null) {
//            throw new ApiException(HttpStatus.BAD_REQUEST, "TICKET_008", "Chỉ được báo sự cố phòng đang thuê");
//        }
//
//        String scope = normalizeScope(request.ticketScope());
//        if ("TENANT".equals(access.actorRole()) && !"TENANT_ROOM".equals(scope)) {
//            throw new ApiException(HttpStatus.FORBIDDEN, "TICKET_008", "Chỉ được báo sự cố phòng đang thuê");
//        }
//
//        ActiveRoom room = resolveRoomForTicket(access, tenantId, request.roomId(), "TENANT".equals(access.actorRole()));
//        String category = normalizeCategoryInput(request.category());
//        String title = resolveTitle(request.title(), category);
//        String description = cleanText(request.description());
//        String priority = normalizePriority(request.priority());
//        List<Long> attachmentFileIds = normalizedFileIds(request.attachmentFileIds());
//
//        if (!StringUtils.hasText(title)) {
//            throw new ApiException(HttpStatus.BAD_REQUEST, "TICKET_004", "Vui lòng chọn loại sự cố");
//        }
//        if (!StringUtils.hasText(description)) {
//            throw new ApiException(HttpStatus.BAD_REQUEST, "TICKET_005", "Vui lòng mô tả vấn đề");
//        }
//        validateAttachmentFiles(userId, attachmentFileIds);
//
//        String storedCategory = StringUtils.hasText(category) ? category : categorize(title, title, description);
//        String ticketCode = nextTicketCode();
//        KeyHolder keyHolder = new GeneratedKeyHolder();
//        jdbcTemplate.update(connection -> {
//            PreparedStatement statement = connection.prepareStatement("""
//                    INSERT INTO maintenance_tickets (
//                        ticket_code,
//                        property_id,
//                        room_id,
//                        contract_id,
//                        created_by,
//                        ticket_scope,
//                        priority,
//                        category,
//                        title,
//                        description,
//                        status
//                    )
//                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING_ACCEPTANCE')
//                    """, Statement.RETURN_GENERATED_KEYS);
//            statement.setString(1, ticketCode);
//            statement.setLong(2, access.propertyId());
//            statement.setLong(3, request.roomId());
//            if (room.contractId() == null) {
//                statement.setNull(4, Types.BIGINT);
//            } else {
//                statement.setLong(4, room.contractId());
//            }
//            statement.setLong(5, userId);
//            statement.setString(6, scope);
//            statement.setString(7, priority);
//            statement.setString(8, storedCategory);
//            statement.setString(9, title);
//            statement.setString(10, description);
//            return statement;
//        }, keyHolder);
//
//        Long ticketId = generatedId(keyHolder);
//        insertAttachments(ticketId, attachmentFileIds, "BEFORE");
//        insertEvent(ticketId, null, "PENDING_ACCEPTANCE", "Đã gửi báo cáo sự cố", userId);
//        notifyManagers(access.propertyId(), "TICKET_CREATED", ticketId,
//                "Có phiếu sự cố mới " + displayCode(ticketCode),
//                "Phòng " + room.roomCode() + " báo sự cố: " + title);
//        auditService.record(userId, "TICKET_CREATED", "MAINTENANCE_TICKET", ticketId);
//
//        return getCreatedTicketResponse(ticketId);
//    }
//
//    public MaintenanceTicketDetailResponse getTicketDetail(Long userId, Long tenantId, Long ticketId) {
//        TenantAccess access = resolveTenantAccess(userId, tenantId);
//        TicketDetailRow ticket = findTicketDetail(access, tenantId, ticketId);
//        if (ticket == null) {
//            throw new ApiException(HttpStatus.NOT_FOUND, "TICKET_001", "Không tìm thấy phiếu sự cố");
//        }
//
//        List<MaintenanceTicketDetailResponse.Attachment> beforeAttachments = getDetailAttachments(ticketId, "BEFORE");
//        List<MaintenanceTicketDetailResponse.Attachment> afterAttachments = getDetailAttachments(ticketId, "AFTER");
//        MaintenanceTicketDetailResponse.RepairInfo repairInfo = getRepairInfo(ticket);
//
//        return new MaintenanceTicketDetailResponse(
//                ticket.id(),
//                ticket.ticketCode(),
//                ticket.status(),
//                statusLabel(ticket.status()),
//                ticket.roomId(),
//                ticket.roomCode(),
//                ticket.propertyId(),
//                ticket.propertyName(),
//                ticket.title(),
//                ticket.description(),
//                ticket.priority(),
//                ticket.ticketScope(),
//                toOffset(ticket.createdAt()),
//                new MaintenanceTicketDetailResponse.UserSummary(ticket.createdBy(), ticket.createdByName()),
//                beforeAttachments,
//                afterAttachments,
//                repairInfo,
//                getReview(ticketId),
//                getEvents(ticketId)
//        );
//    }
//
//    @Transactional
//    public MaintenanceTicketActionResponse acceptTicket(
//            Long userId,
//            Long tenantId,
//            Long ticketId,
//            MaintenanceTicketRequests.AcceptTicketRequest request
//    ) {
//        TenantAccess access = requireManagerAccess(userId, tenantId);
//        TicketStatusRow ticket = requireTicketForAction(access, tenantId, ticketId);
//        int updated = jdbcTemplate.update("""
//                UPDATE maintenance_tickets
//                SET status = 'ACCEPTED',
//                    assigned_to = ?,
//                    updated_at = NOW(6)
//                WHERE id = ?
//                  AND property_id = ?
//                  AND status = 'PENDING_ACCEPTANCE'
//                """, userId, ticketId, access.propertyId());
//        if (updated == 0) {
//            throw invalidState();
//        }
//        insertEvent(ticketId, ticket.status(), "ACCEPTED", noteOrDefault(request == null ? null : request.note(), "Ban quản lý đã xác nhận yêu cầu"), userId);
//        notifyRoomTenants(ticket.roomId(), "TICKET_ACCEPTED", ticketId,
//                "Phiếu sự cố " + displayCode(ticket.ticketCode()) + " đã được tiếp nhận",
//                noteOrDefault(request == null ? null : request.note(), "Ban quản lý đã tiếp nhận phiếu sự cố."));
//        auditService.record(userId, "TICKET_ACCEPTED", "MAINTENANCE_TICKET", ticketId);
//        return actionResponse(ticketId, "ACCEPTED");
//    }
//
//    @Transactional
//    public MaintenanceTicketActionResponse rejectTicket(
//            Long userId,
//            Long tenantId,
//            Long ticketId,
//            MaintenanceTicketRequests.RejectTicketRequest request
//    ) {
//        TenantAccess access = requireManagerAccess(userId, tenantId);
//        String reason = cleanText(request == null ? null : request.reason());
//        if (!StringUtils.hasText(reason)) {
//            throw new ApiException(HttpStatus.BAD_REQUEST, "TICKET_009", "Vui lòng nhập lý do từ chối");
//        }
//
//        TicketStatusRow ticket = requireTicketForAction(access, tenantId, ticketId);
//        if (!"PENDING_ACCEPTANCE".equals(ticket.status()) && !"ACCEPTED".equals(ticket.status())) {
//            throw invalidState();
//        }
//
//        int updated = jdbcTemplate.update("""
//                UPDATE maintenance_tickets
//                SET status = 'REJECTED',
//                    rejection_reason = ?,
//                    updated_at = NOW(6)
//                WHERE id = ?
//                  AND property_id = ?
//                  AND status IN ('PENDING_ACCEPTANCE', 'ACCEPTED')
//                """, reason, ticketId, access.propertyId());
//        if (updated == 0) {
//            throw invalidState();
//        }
//        insertEvent(ticketId, ticket.status(), "REJECTED", reason, userId);
//        notifyRoomTenants(ticket.roomId(), "TICKET_REJECTED", ticketId,
//                "Phiếu sự cố " + displayCode(ticket.ticketCode()) + " bị từ chối",
//                reason);
//        auditService.record(userId, "TICKET_REJECTED", "MAINTENANCE_TICKET", ticketId);
//        return actionResponse(ticketId, "REJECTED");
//    }
//
//    @Transactional
//    public MaintenanceTicketActionResponse updateProgress(
//            Long userId,
//            Long tenantId,
//            Long ticketId,
//            MaintenanceTicketRequests.UpdateTicketProgressRequest request
//    ) {
//        TenantAccess access = requireManagerAccess(userId, tenantId);
//        TicketStatusRow ticket = requireTicketForAction(access, tenantId, ticketId);
//        if (!"ACCEPTED".equals(ticket.status()) && !"IN_PROGRESS".equals(ticket.status())) {
//            throw invalidState();
//        }
//
//        String note = noteOrDefault(request == null ? null : request.note(), "Đang xử lý sự cố");
//        int updated = jdbcTemplate.update("""
//                UPDATE maintenance_tickets
//                SET status = 'IN_PROGRESS',
//                    worker_name = COALESCE(?, worker_name),
//                    repair_items = COALESCE(?, repair_items),
//                    updated_at = NOW(6)
//                WHERE id = ?
//                  AND property_id = ?
//                  AND status IN ('ACCEPTED', 'IN_PROGRESS')
//                """,
//                cleanText(request == null ? null : request.workerName()),
//                cleanText(request == null ? null : request.repairItems()),
//                ticketId,
//                access.propertyId());
//        if (updated == 0) {
//            throw invalidState();
//        }
//        insertEvent(ticketId, ticket.status(), "IN_PROGRESS", note, userId);
//        notifyRoomTenants(ticket.roomId(), "TICKET_IN_PROGRESS", ticketId,
//                "Phiếu sự cố " + displayCode(ticket.ticketCode()) + " đang được xử lý",
//                note);
//        auditService.record(userId, "TICKET_PROGRESS_UPDATED", "MAINTENANCE_TICKET", ticketId);
//        return actionResponse(ticketId, "IN_PROGRESS");
//    }
//
//    @Transactional
//    public MaintenanceTicketActionResponse completeTicket(
//            Long userId,
//            Long tenantId,
//            Long ticketId,
//            MaintenanceTicketRequests.CompleteTicketRequest request
//    ) {
//        TenantAccess access = requireManagerAccess(userId, tenantId);
//        TicketStatusRow ticket = requireTicketForAction(access, tenantId, ticketId);
//        if (!"ACCEPTED".equals(ticket.status()) && !"IN_PROGRESS".equals(ticket.status())) {
//            throw invalidState();
//        }
//
//        List<Long> afterPhotoFileIds = normalizedFileIds(request == null ? null : request.afterPhotoFileIds());
//        validateAttachmentFiles(userId, afterPhotoFileIds);
//        validateCosts(request == null ? null : request.costs());
//
//        int updated = jdbcTemplate.update("""
//                UPDATE maintenance_tickets
//                SET status = 'WAITING_CONFIRMATION',
//                    completed_at = NOW(6),
//                    updated_at = NOW(6)
//                WHERE id = ?
//                  AND property_id = ?
//                  AND status IN ('ACCEPTED', 'IN_PROGRESS')
//                """, ticketId, access.propertyId());
//        if (updated == 0) {
//            throw invalidState();
//        }
//
//        insertAttachments(ticketId, afterPhotoFileIds, "AFTER");
//        insertCosts(ticketId, userId, request == null ? null : request.costs());
//        String note = noteOrDefault(request == null ? null : request.completionNote(), "Đã sửa xong, chờ khách xác nhận");
//        insertEvent(ticketId, ticket.status(), "WAITING_CONFIRMATION", note, userId);
//        notifyRoomTenants(ticket.roomId(), "TICKET_WAITING_CONFIRMATION", ticketId,
//                "Sự cố " + displayCode(ticket.ticketCode()) + " đã sửa xong",
//                "Sự cố đã sửa xong, vui lòng xác nhận.");
//        auditService.record(userId, "TICKET_REPAIRED", "MAINTENANCE_TICKET", ticketId);
//        return actionResponse(ticketId, "WAITING_CONFIRMATION");
//    }
//
//    @Transactional
//    public MaintenanceTicketActionResponse confirmTicket(
//            Long userId,
//            Long tenantId,
//            Long ticketId,
//            MaintenanceTicketRequests.ConfirmTicketRequest request
//    ) {
//        TenantAccess access = requireTenantAccess(userId, tenantId);
//        TicketStatusRow ticket = requireTicketForAction(access, tenantId, ticketId);
//        if (!"WAITING_CONFIRMATION".equals(ticket.status())) {
//            throw invalidState();
//        }
//
//        int updated = jdbcTemplate.update("""
//                UPDATE maintenance_tickets
//                SET status = 'COMPLETED',
//                    updated_at = NOW(6)
//                WHERE id = ?
//                  AND property_id = ?
//                  AND status = 'WAITING_CONFIRMATION'
//                """, ticketId, access.propertyId());
//        if (updated == 0) {
//            throw invalidState();
//        }
//        insertEvent(ticketId, "WAITING_CONFIRMATION", "COMPLETED",
//                noteOrDefault(request == null ? null : request.satisfactionNote(), "Khách thuê xác nhận hoàn tất"), userId);
//        notifyManagers(access.propertyId(), "TICKET_COMPLETED", ticketId,
//                "Khách đã xác nhận hoàn tất " + displayCode(ticket.ticketCode()),
//                "Khách thuê đã xác nhận phiếu sự cố hoàn tất.");
//        auditService.record(userId, "TICKET_CONFIRMED", "MAINTENANCE_TICKET", ticketId);
//        return actionResponse(ticketId, "COMPLETED");
//    }
//
//    @Transactional
//    public MaintenanceTicketReviewResponse reviewTicket(
//            Long userId,
//            Long tenantId,
//            Long ticketId,
//            MaintenanceTicketRequests.ReviewTicketRequest request
//    ) {
//        TenantAccess access = requireTenantAccess(userId, tenantId);
//        TicketStatusRow ticket = requireTicketForAction(access, tenantId, ticketId);
//        if (!"COMPLETED".equals(ticket.status())) {
//            throw invalidState();
//        }
//        return createReview(userId, ticketId, request, true);
//    }
//
//    @Transactional
//    public ConfirmAndReviewResponse confirmAndReview(
//            Long userId,
//            Long tenantId,
//            Long ticketId,
//            MaintenanceTicketRequests.ConfirmAndReviewRequest request
//    ) {
//        TenantAccess access = requireTenantAccess(userId, tenantId);
//        TicketStatusRow ticket = requireTicketForAction(access, tenantId, ticketId);
//        if (!"WAITING_CONFIRMATION".equals(ticket.status())) {
//            throw invalidState();
//        }
//
//        int updated = jdbcTemplate.update("""
//                UPDATE maintenance_tickets
//                SET status = 'COMPLETED',
//                    updated_at = NOW(6)
//                WHERE id = ?
//                  AND property_id = ?
//                  AND status = 'WAITING_CONFIRMATION'
//                """, ticketId, access.propertyId());
//        if (updated == 0) {
//            throw invalidState();
//        }
//
//        insertEvent(ticketId, "WAITING_CONFIRMATION", "COMPLETED",
//                noteOrDefault(request == null ? null : request.satisfactionNote(), "Khách thuê xác nhận hoàn tất"), userId);
//        MaintenanceTicketReviewResponse review = createReview(
//                userId,
//                ticketId,
//                request == null ? null : new MaintenanceTicketRequests.ReviewTicketRequest(request.rating(), request.comment()),
//                false
//        );
//        notifyManagers(access.propertyId(), "TICKET_COMPLETED", ticketId,
//                "Khách đã xác nhận hoàn tất " + displayCode(ticket.ticketCode()),
//                "Khách thuê đã xác nhận hoàn tất và đánh giá phiếu sự cố.");
//        auditService.record(userId, "TICKET_CONFIRMED_AND_REVIEWED", "MAINTENANCE_TICKET", ticketId);
//        return new ConfirmAndReviewResponse(actionResponse(ticketId, "COMPLETED"), review);
//    }
//
//    private TenantAccess resolveTenantAccess(Long userId, Long tenantId) {
//        TenantAccess access = jdbcTemplate.query("""
//                SELECT requested_tenant.property_id,
//                       requested_tenant.user_id = cu.id AS owns_requested_tenant,
//                       cu.role AS user_role,
//                       EXISTS (
//                         SELECT 1
//                         FROM tenants own_tenant
//                         WHERE own_tenant.user_id = cu.id
//                           AND own_tenant.property_id = requested_tenant.property_id
//                           AND own_tenant.deleted_at IS NULL
//                       ) AS has_property_tenant,
//                       EXISTS (
//                         SELECT 1
//                         FROM role_promotions rp
//                         WHERE rp.user_id = cu.id
//                           AND rp.property_id = requested_tenant.property_id
//                           AND rp.role = 'MANAGER'
//                           AND rp.status = 'ACTIVE'
//                           AND rp.deleted_at IS NULL
//                       ) AS has_manager_promotion
//                FROM tenants requested_tenant
//                JOIN users cu ON cu.id = ?
//                WHERE requested_tenant.id = ?
//                  AND requested_tenant.deleted_at IS NULL
//                  AND cu.deleted_at IS NULL
//                  AND cu.status = 'ACTIVE'
//                """, rs -> {
//                    if (!rs.next()) {
//                        return null;
//                    }
//                    String userRole = rs.getString("user_role");
//                    boolean ownsRequestedTenant = rs.getBoolean("owns_requested_tenant");
//                    boolean hasPropertyTenant = rs.getBoolean("has_property_tenant");
//                    boolean hasManagerPromotion = rs.getBoolean("has_manager_promotion");
//                    String actorRole = resolveActorRole(userRole, ownsRequestedTenant, hasPropertyTenant, hasManagerPromotion);
//                    return new TenantAccess(
//                            rs.getLong("property_id"),
//                            actorRole,
//                            ownsRequestedTenant
//                    );
//                }, userId, tenantId);
//
//        if (access == null || access.actorRole() == null) {
//            throw new ApiException(HttpStatus.FORBIDDEN, "Bạn không có quyền xem danh sách phiếu sự cố của tenant này");
//        }
//        return access;
//    }
//
//    private static String resolveActorRole(
//            String userRole,
//            boolean ownsRequestedTenant,
//            boolean hasPropertyTenant,
//            boolean hasManagerPromotion
//    ) {
//        if (("OWNER".equals(userRole) || "MANAGER".equals(userRole)) && hasPropertyTenant) {
//            return userRole;
//        }
//        if (hasManagerPromotion) {
//            return "MANAGER";
//        }
//        if (ownsRequestedTenant && "TENANT".equals(userRole)) {
//            return "TENANT";
//        }
//        return null;
//    }
//
//    private TenantAccess requireManagerAccess(Long userId, Long tenantId) {
//        TenantAccess access = resolveTenantAccess(userId, tenantId);
//        if (!"OWNER".equals(access.actorRole()) && !"MANAGER".equals(access.actorRole())) {
//            throw new ApiException(HttpStatus.FORBIDDEN, "TICKET_002", "Không có quyền truy cập phiếu sự cố");
//        }
//        return access;
//    }
//
//    private TenantAccess requireTenantAccess(Long userId, Long tenantId) {
//        TenantAccess access = resolveTenantAccess(userId, tenantId);
//        if (!"TENANT".equals(access.actorRole())) {
//            throw new ApiException(HttpStatus.FORBIDDEN, "TICKET_002", "Không có quyền truy cập phiếu sự cố");
//        }
//        return access;
//    }
//
//    private ActiveRoom resolveRoomForTicket(TenantAccess access, Long tenantId, Long roomId, boolean tenantOnly) {
//        ActiveRoom room;
//        if (tenantOnly) {
//            room = jdbcTemplate.query("""
//                    SELECT lc.id AS contract_id,
//                           r.room_code
//                    FROM contract_occupants co
//                    JOIN lease_contracts lc ON lc.id = co.contract_id
//                    JOIN rooms r ON r.id = lc.room_id
//                    WHERE co.tenant_id = ?
//                      AND co.status = 'ACTIVE'
//                      AND lc.room_id = ?
//                      AND lc.deleted_at IS NULL
//                      AND lc.status IN ('ACTIVE', 'EXPIRING_SOON', 'TERMINATION_PENDING')
//                    ORDER BY lc.start_date DESC, lc.id DESC
//                    LIMIT 1
//                    """, rs -> rs.next()
//                    ? new ActiveRoom(rs.getLong("contract_id"), rs.getString("room_code"))
//                    : null, tenantId, roomId);
//        } else {
//            room = jdbcTemplate.query("""
//                    SELECT (
//                             SELECT lc.id
//                             FROM lease_contracts lc
//                             WHERE lc.room_id = r.id
//                               AND lc.deleted_at IS NULL
//                               AND lc.status IN ('ACTIVE', 'EXPIRING_SOON', 'TERMINATION_PENDING')
//                             ORDER BY lc.start_date DESC, lc.id DESC
//                             LIMIT 1
//                           ) AS contract_id,
//                           r.room_code
//                    FROM rooms r
//                    WHERE r.id = ?
//                      AND r.property_id = ?
//                      AND r.deleted_at IS NULL
//                    """, rs -> rs.next()
//                    ? new ActiveRoom(nullableLong(rs, "contract_id"), rs.getString("room_code"))
//                    : null, roomId, access.propertyId());
//        }
//
//        if (room == null) {
//            throw new ApiException(HttpStatus.FORBIDDEN, "TICKET_008", "Chỉ được báo sự cố phòng đang thuê");
//        }
//        return room;
//    }
//
//    private TicketStatusRow requireTicketForAction(TenantAccess access, Long tenantId, Long ticketId) {
//        TicketStatusRow ticket = jdbcTemplate.query("""
//                SELECT mt.id,
//                       mt.ticket_code,
//                       mt.status,
//                       mt.room_id
//                FROM maintenance_tickets mt
//                WHERE mt.id = ?
//                  AND mt.property_id = ?
//                  AND (
//                    ? <> 'TENANT'
//                    OR (
//                      mt.ticket_scope = 'TENANT_ROOM'
//                      AND mt.room_id IN (
//                        SELECT DISTINCT lc.room_id
//                        FROM contract_occupants co
//                        JOIN lease_contracts lc ON lc.id = co.contract_id
//                        WHERE co.tenant_id = ?
//                          AND co.status = 'ACTIVE'
//                          AND lc.deleted_at IS NULL
//                          AND lc.status IN ('ACTIVE', 'EXPIRING_SOON', 'TERMINATION_PENDING')
//                      )
//                    )
//                  )
//                """, rs -> rs.next()
//                ? new TicketStatusRow(
//                rs.getLong("id"),
//                rs.getString("ticket_code"),
//                rs.getString("status"),
//                nullableLong(rs, "room_id")
//        )
//                : null, ticketId, access.propertyId(), access.actorRole(), tenantId);
//        if (ticket == null) {
//            throw new ApiException(HttpStatus.NOT_FOUND, "TICKET_001", "Không tìm thấy phiếu sự cố");
//        }
//        return ticket;
//    }
//
//    private MaintenanceTicketResponse getCreatedTicketResponse(Long ticketId) {
//        return jdbcTemplate.queryForObject("""
//                SELECT mt.id,
//                       mt.ticket_code,
//                       mt.status,
//                       mt.room_id,
//                       r.room_code,
//                       mt.title,
//                       mt.description,
//                       mt.priority,
//                       mt.ticket_scope,
//                       mt.created_at
//                FROM maintenance_tickets mt
//                LEFT JOIN rooms r ON r.id = mt.room_id
//                WHERE mt.id = ?
//                """, (rs, rowNum) -> new MaintenanceTicketResponse(
//                rs.getLong("id"),
//                rs.getString("ticket_code"),
//                rs.getString("status"),
//                statusLabel(rs.getString("status")),
//                nullableLong(rs, "room_id"),
//                rs.getString("room_code"),
//                rs.getString("title"),
//                rs.getString("description"),
//                rs.getString("priority"),
//                rs.getString("ticket_scope"),
//                getResponseAttachments(ticketId),
//                toOffset(rs.getTimestamp("created_at").toLocalDateTime())
//        ), ticketId);
//    }
//
//    private TicketDetailRow findTicketDetail(TenantAccess access, Long tenantId, Long ticketId) {
//        return jdbcTemplate.query("""
//                SELECT mt.id,
//                       mt.ticket_code,
//                       mt.status,
//                       mt.room_id,
//                       r.room_code,
//                       p.id AS property_id,
//                       p.name AS property_name,
//                       mt.title,
//                       mt.description,
//                       mt.priority,
//                       mt.ticket_scope,
//                       mt.created_at,
//                       mt.created_by,
//                       COALESCE(pp.full_name, u.email, u.phone) AS created_by_name,
//                       mt.worker_name,
//                       mt.repair_items,
//                       mt.completed_at,
//                       (
//                         SELECT mte.note
//                         FROM maintenance_ticket_events mte
//                         WHERE mte.ticket_id = mt.id
//                           AND mte.to_status = 'WAITING_CONFIRMATION'
//                         ORDER BY mte.created_at DESC, mte.id DESC
//                         LIMIT 1
//                       ) AS completion_note
//                FROM maintenance_tickets mt
//                JOIN properties p ON p.id = mt.property_id
//                LEFT JOIN rooms r ON r.id = mt.room_id
//                JOIN users u ON u.id = mt.created_by
//                LEFT JOIN person_profiles pp
//                  ON pp.deleted_at IS NULL
//                 AND (pp.phone = u.phone OR LOWER(pp.email) = LOWER(u.email))
//                WHERE mt.id = ?
//                  AND mt.property_id = ?
//                  AND (
//                    ? <> 'TENANT'
//                    OR (
//                      mt.ticket_scope = 'TENANT_ROOM'
//                      AND mt.room_id IN (
//                        SELECT DISTINCT lc.room_id
//                        FROM contract_occupants co
//                        JOIN lease_contracts lc ON lc.id = co.contract_id
//                        WHERE co.tenant_id = ?
//                          AND co.status = 'ACTIVE'
//                          AND lc.deleted_at IS NULL
//                          AND lc.status IN ('ACTIVE', 'EXPIRING_SOON', 'TERMINATION_PENDING')
//                      )
//                    )
//                  )
//                ORDER BY pp.id DESC
//                LIMIT 1
//                """, rs -> rs.next()
//                ? new TicketDetailRow(
//                rs.getLong("id"),
//                rs.getString("ticket_code"),
//                rs.getString("status"),
//                nullableLong(rs, "room_id"),
//                rs.getString("room_code"),
//                rs.getLong("property_id"),
//                rs.getString("property_name"),
//                rs.getString("title"),
//                rs.getString("description"),
//                rs.getString("priority"),
//                rs.getString("ticket_scope"),
//                rs.getTimestamp("created_at").toLocalDateTime(),
//                rs.getLong("created_by"),
//                rs.getString("created_by_name"),
//                rs.getString("worker_name"),
//                rs.getString("repair_items"),
//                nullableLocalDateTime(rs, "completed_at"),
//                rs.getString("completion_note")
//        )
//                : null, ticketId, access.propertyId(), access.actorRole(), tenantId);
//    }
//
//    private List<MaintenanceTicketResponse.Attachment> getResponseAttachments(Long ticketId) {
//        return jdbcTemplate.query("""
//                SELECT mta.id,
//                       mta.file_id,
//                       mta.attachment_phase,
//                       mta.sort_order
//                FROM maintenance_ticket_attachments mta
//                WHERE mta.ticket_id = ?
//                ORDER BY mta.sort_order, mta.created_at, mta.id
//                """, (rs, rowNum) -> new MaintenanceTicketResponse.Attachment(
//                rs.getLong("id"),
//                rs.getLong("file_id"),
//                fileUrl(rs.getLong("file_id")),
//                rs.getString("attachment_phase"),
//                rs.getInt("sort_order")
//        ), ticketId);
//    }
//
//    private List<MaintenanceTicketDetailResponse.Attachment> getDetailAttachments(Long ticketId, String phase) {
//        return jdbcTemplate.query("""
//                SELECT mta.id,
//                       mta.file_id,
//                       fm.mime_type,
//                       mta.attachment_phase,
//                       mta.sort_order
//                FROM maintenance_ticket_attachments mta
//                JOIN file_metadata fm ON fm.id = mta.file_id
//                WHERE mta.ticket_id = ?
//                  AND mta.attachment_phase = ?
//                  AND fm.deleted_at IS NULL
//                ORDER BY mta.sort_order, mta.created_at, mta.id
//                """, (rs, rowNum) -> new MaintenanceTicketDetailResponse.Attachment(
//                rs.getLong("id"),
//                rs.getLong("file_id"),
//                fileUrl(rs.getLong("file_id")),
//                rs.getString("mime_type"),
//                rs.getString("attachment_phase"),
//                rs.getInt("sort_order")
//        ), ticketId, phase);
//    }
//
//    private MaintenanceTicketDetailResponse.RepairInfo getRepairInfo(TicketDetailRow ticket) {
//        BigDecimal totalCost = jdbcTemplate.queryForObject("""
//                SELECT COALESCE(SUM(amount), 0)
//                FROM maintenance_costs
//                WHERE ticket_id = ?
//                """, BigDecimal.class, ticket.id());
//        String totalCostText = totalCost == null ? "0.00" : totalCost.setScale(2, RoundingMode.UNNECESSARY).toPlainString();
//        return new MaintenanceTicketDetailResponse.RepairInfo(
//                ticket.workerName(),
//                ticket.repairItems(),
//                ticket.completionNote(),
//                totalCostText,
//                ticket.completedAt() == null ? null : toOffset(ticket.completedAt())
//        );
//    }
//
//    private MaintenanceTicketDetailResponse.Review getReview(Long ticketId) {
//        return jdbcTemplate.query("""
//                SELECT rating,
//                       comment,
//                       created_at
//                FROM maintenance_reviews
//                WHERE ticket_id = ?
//                ORDER BY created_at DESC, id DESC
//                LIMIT 1
//                """, rs -> rs.next()
//                ? new MaintenanceTicketDetailResponse.Review(
//                rs.getInt("rating"),
//                rs.getString("comment"),
//                toOffset(rs.getTimestamp("created_at").toLocalDateTime())
//        )
//                : null, ticketId);
//    }
//
//    private List<MaintenanceTicketDetailResponse.Event> getEvents(Long ticketId) {
//        return jdbcTemplate.query("""
//                SELECT id,
//                       to_status,
//                       note,
//                       created_at
//                FROM maintenance_ticket_events
//                WHERE ticket_id = ?
//                ORDER BY created_at ASC, id ASC
//                """, (rs, rowNum) -> new MaintenanceTicketDetailResponse.Event(
//                rs.getLong("id"),
//                rs.getString("to_status"),
//                eventTitle(rs.getString("to_status")),
//                rs.getString("note"),
//                toOffset(rs.getTimestamp("created_at").toLocalDateTime())
//        ), ticketId);
//    }
//
//    private MaintenanceTicketReviewResponse createReview(
//            Long userId,
//            Long ticketId,
//            MaintenanceTicketRequests.ReviewTicketRequest request,
//            boolean writeAudit
//    ) {
//        Integer rating = request == null ? null : request.rating();
//        if (rating == null) {
//            throw new ApiException(HttpStatus.BAD_REQUEST, "TICKET_010", "Vui lòng chọn số sao đánh giá");
//        }
//        if (rating < 1 || rating > 5) {
//            throw new ApiException(HttpStatus.BAD_REQUEST, "TICKET_010", "Vui lòng chọn số sao đánh giá");
//        }
//        String comment = cleanText(request.comment());
//        if (comment != null && comment.length() > 500) {
//            comment = comment.substring(0, 500);
//        }
//
//        Long existingReviewId = jdbcTemplate.query("""
//                SELECT id
//                FROM maintenance_reviews
//                WHERE ticket_id = ?
//                LIMIT 1
//                """, rs -> rs.next() ? rs.getLong("id") : null, ticketId);
//        if (existingReviewId != null) {
//            throw new ApiException(HttpStatus.CONFLICT, "TICKET_011", "Ticket đã được đánh giá");
//        }
//
//        KeyHolder keyHolder = new GeneratedKeyHolder();
//        String finalComment = comment;
//        jdbcTemplate.update(connection -> {
//            PreparedStatement statement = connection.prepareStatement("""
//                    INSERT INTO maintenance_reviews (ticket_id, reviewer_user_id, rating, comment)
//                    VALUES (?, ?, ?, ?)
//                    """, Statement.RETURN_GENERATED_KEYS);
//            statement.setLong(1, ticketId);
//            statement.setLong(2, userId);
//            statement.setInt(3, rating);
//            statement.setString(4, finalComment);
//            return statement;
//        }, keyHolder);
//
//        Long reviewId = generatedId(keyHolder);
//        insertEvent(ticketId, "COMPLETED", "COMPLETED", "Khách thuê đánh giá " + rating + " sao", userId);
//        if (writeAudit) {
//            auditService.record(userId, "TICKET_REVIEWED", "MAINTENANCE_TICKET", ticketId);
//        }
//        return jdbcTemplate.queryForObject("""
//                SELECT id,
//                       ticket_id,
//                       rating,
//                       comment,
//                       created_at
//                FROM maintenance_reviews
//                WHERE id = ?
//                """, (rs, rowNum) -> new MaintenanceTicketReviewResponse(
//                rs.getLong("id"),
//                rs.getLong("ticket_id"),
//                rs.getInt("rating"),
//                rs.getString("comment"),
//                toOffset(rs.getTimestamp("created_at").toLocalDateTime())
//        ), reviewId);
//    }
//
//    private void validateCosts(List<MaintenanceTicketRequests.CostRequest> costs) {
//        if (costs == null) {
//            return;
//        }
//        for (MaintenanceTicketRequests.CostRequest cost : costs) {
//            if (cost == null) {
//                continue;
//            }
//            normalizeCostType(cost.costType());
//            normalizePaidBy(cost.paidBy());
//            toWholeCurrency(cost.amount());
//        }
//    }
//
//    private void insertCosts(Long ticketId, Long userId, List<MaintenanceTicketRequests.CostRequest> costs) {
//        if (costs == null || costs.isEmpty()) {
//            return;
//        }
//        for (MaintenanceTicketRequests.CostRequest cost : costs) {
//            if (cost == null) {
//                continue;
//            }
//            jdbcTemplate.update("""
//                    INSERT INTO maintenance_costs (ticket_id, cost_type, description, amount, paid_by, created_by)
//                    VALUES (?, ?, ?, ?, ?, ?)
//                    """,
//                    ticketId,
//                    normalizeCostType(cost.costType()),
//                    cleanText(cost.description()),
//                    toWholeCurrency(cost.amount()),
//                    normalizePaidBy(cost.paidBy()),
//                    userId);
//        }
//    }
//
//    private void validateAttachmentFiles(Long userId, List<Long> fileIds) {
//        if (fileIds.size() > 3) {
//            throw new ApiException(HttpStatus.BAD_REQUEST, "TICKET_006", "Chỉ được đính kèm tối đa 3 ảnh/video");
//        }
//        for (Long fileId : fileIds) {
//            Integer count = jdbcTemplate.queryForObject("""
//                    SELECT COUNT(*)
//                    FROM file_metadata
//                    WHERE id = ?
//                      AND owner_user_id = ?
//                      AND deleted_at IS NULL
//                      AND category IN ('MAINTENANCE', 'TICKET_ATTACHMENT')
//                    """, Integer.class, fileId, userId);
//            if (count == null || count == 0) {
//                throw new ApiException(HttpStatus.BAD_REQUEST, "TICKET_007", "File không hợp lệ");
//            }
//        }
//    }
//
//    private void insertAttachments(Long ticketId, List<Long> fileIds, String phase) {
//        for (int index = 0; index < fileIds.size(); index++) {
//            jdbcTemplate.update("""
//                    INSERT INTO maintenance_ticket_attachments (ticket_id, file_id, attachment_phase, sort_order, created_by)
//                    VALUES (?, ?, ?, ?, NULL)
//                    """, ticketId, fileIds.get(index), phase, index);
//        }
//    }
//
//    private void insertEvent(Long ticketId, String fromStatus, String toStatus, String note, Long userId) {
//        jdbcTemplate.update("""
//                INSERT INTO maintenance_ticket_events (ticket_id, from_status, to_status, note, created_by)
//                VALUES (?, ?, ?, ?, ?)
//                """, ticketId, fromStatus, toStatus, note, userId);
//    }
//
//    private void notifyManagers(Long propertyId, String eventType, Long ticketId, String title, String body) {
//        List<Long> recipients = jdbcTemplate.queryForList("""
//                SELECT DISTINCT recipient_id
//                FROM (
//                    SELECT u.id AS recipient_id
//                    FROM tenants t
//                    JOIN users u ON u.id = t.user_id
//                    WHERE t.property_id = ?
//                      AND t.deleted_at IS NULL
//                      AND u.deleted_at IS NULL
//                      AND u.status = 'ACTIVE'
//                      AND u.role IN ('OWNER', 'MANAGER')
//
//                    UNION
//
//                    SELECT rp.user_id AS recipient_id
//                    FROM role_promotions rp
//                    JOIN users u ON u.id = rp.user_id
//                    WHERE rp.property_id = ?
//                      AND rp.role = 'MANAGER'
//                      AND rp.status = 'ACTIVE'
//                      AND rp.deleted_at IS NULL
//                      AND u.deleted_at IS NULL
//                      AND u.status = 'ACTIVE'
//                ) recipients
//                """, Long.class, propertyId, propertyId);
//        insertNotifications(eventType, ticketId, title, body, recipients);
//    }
//
//    private void notifyRoomTenants(Long roomId, String eventType, Long ticketId, String title, String body) {
//        if (roomId == null) {
//            return;
//        }
//        List<Long> recipients = jdbcTemplate.queryForList("""
//                SELECT DISTINCT t.user_id
//                FROM contract_occupants co
//                JOIN tenants t ON t.id = co.tenant_id
//                JOIN users u ON u.id = t.user_id
//                JOIN lease_contracts lc ON lc.id = co.contract_id
//                WHERE lc.room_id = ?
//                  AND co.status = 'ACTIVE'
//                  AND lc.deleted_at IS NULL
//                  AND lc.status IN ('ACTIVE', 'EXPIRING_SOON', 'TERMINATION_PENDING')
//                  AND t.deleted_at IS NULL
//                  AND u.deleted_at IS NULL
//                  AND u.status = 'ACTIVE'
//                """, Long.class, roomId);
//        insertNotifications(eventType, ticketId, title, body, recipients);
//    }
//
//    private void insertNotifications(String eventType, Long ticketId, String title, String body, List<Long> recipientIds) {
//        for (Long recipientId : recipientIds) {
//            jdbcTemplate.update("""
//                    INSERT INTO notification_outbox (
//                        event_type,
//                        target_type,
//                        target_id,
//                        recipient_user_id,
//                        channel,
//                        title,
//                        body,
//                        payload
//                    )
//                    VALUES (
//                        ?,
//                        'MAINTENANCE_TICKET',
//                        ?,
//                        ?,
//                        'PUSH',
//                        ?,
//                        ?,
//                        JSON_OBJECT('ticket_id', ?)
//                    )
//                    """, eventType, ticketId, recipientId, title, body, ticketId);
//        }
//    }
//
//    private MaintenanceTicketActionResponse actionResponse(Long ticketId, String status) {
//        return new MaintenanceTicketActionResponse(ticketId, status, statusLabel(status));
//    }
//
//    private ApiException invalidState() {
//        return new ApiException(HttpStatus.CONFLICT, "TICKET_003", "Phiếu đã được xử lý hoặc trạng thái không hợp lệ");
//    }
//
//    private String nextTicketCode() {
//        int year = LocalDate.now(APP_ZONE).getYear();
//        Long next = jdbcTemplate.queryForObject("""
//                SELECT COALESCE(MAX(CAST(SUBSTRING(ticket_code, 9) AS UNSIGNED)), 0) + 1
//                FROM maintenance_tickets
//                WHERE ticket_code REGEXP ?
//                """, Long.class, "^SC-" + year + "-[0-9]{4}$");
//        long sequence = next == null ? 1 : next;
//        return "SC-%d-%04d".formatted(year, sequence);
//    }
//
//    private Long generatedId(KeyHolder keyHolder) {
//        Number key = keyHolder.getKey();
//        if (key == null) {
//            throw new IllegalStateException("Cannot read generated id");
//        }
//        return key.longValue();
//    }
//
//    private List<Long> normalizedFileIds(List<Long> fileIds) {
//        if (fileIds == null || fileIds.isEmpty()) {
//            return List.of();
//        }
//        List<Long> normalized = new ArrayList<>();
//        for (Long fileId : fileIds) {
//            if (fileId != null && !normalized.contains(fileId)) {
//                normalized.add(fileId);
//            }
//        }
//        return normalized;
//    }
//
//    private String normalizeScope(String scope) {
//        if (!StringUtils.hasText(scope)) {
//            return "TENANT_ROOM";
//        }
//        String normalized = scope.trim().toUpperCase(Locale.ROOT);
//        if (!Set.of("TENANT_ROOM", "COMMON_AREA", "PROPERTY_OPERATION").contains(normalized)) {
//            throw new ApiException(HttpStatus.BAD_REQUEST, "TICKET_003", "Trạng thái phiếu không hợp lệ");
//        }
//        return normalized;
//    }
//
//    private String normalizePriority(String priority) {
//        if (!StringUtils.hasText(priority)) {
//            return "MEDIUM";
//        }
//        String normalized = priority.trim().toUpperCase(Locale.ROOT);
//        if (!Set.of("LOW", "MEDIUM", "HIGH", "URGENT").contains(normalized)) {
//            return "MEDIUM";
//        }
//        return normalized;
//    }
//
//    private String normalizeCostType(String costType) {
//        if (!StringUtils.hasText(costType)) {
//            throw new ApiException(HttpStatus.BAD_REQUEST, "TICKET_003", "Trạng thái phiếu không hợp lệ");
//        }
//        String normalized = costType.trim().toUpperCase(Locale.ROOT);
//        if (!Set.of("LABOR", "MATERIAL", "TENANT_COMPENSATION", "COMMON_OPERATING", "OTHER").contains(normalized)) {
//            throw new ApiException(HttpStatus.BAD_REQUEST, "TICKET_003", "Trạng thái phiếu không hợp lệ");
//        }
//        return normalized;
//    }
//
//    private String normalizePaidBy(String paidBy) {
//        if (!StringUtils.hasText(paidBy)) {
//            return "LANDLORD";
//        }
//        String normalized = paidBy.trim().toUpperCase(Locale.ROOT);
//        if (!Set.of("LANDLORD", "TENANT", "MANAGER", "OTHER").contains(normalized)) {
//            throw new ApiException(HttpStatus.BAD_REQUEST, "TICKET_003", "Trạng thái phiếu không hợp lệ");
//        }
//        return normalized;
//    }
//
//    private long toWholeCurrency(BigDecimal amount) {
//        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
//            throw new ApiException(HttpStatus.BAD_REQUEST, "TICKET_003", "Trạng thái phiếu không hợp lệ");
//        }
//        try {
//            return amount.setScale(0, RoundingMode.UNNECESSARY).longValueExact();
//        } catch (ArithmeticException ex) {
//            throw new ApiException(HttpStatus.BAD_REQUEST, "TICKET_003", "Trạng thái phiếu không hợp lệ");
//        }
//    }
//
//    private String resolveTitle(String title, String category) {
//        if (StringUtils.hasText(title)) {
//            return title.trim();
//        }
//        return switch (category == null ? "" : category) {
//            case "ELECTRIC" -> "Điện";
//            case "WATER" -> "Nước";
//            case "EQUIPMENT" -> "Thiết bị";
//            case "OTHER" -> "Khác";
//            default -> null;
//        };
//    }
//
//    private String noteOrDefault(String note, String fallback) {
//        String cleaned = cleanText(note);
//        return StringUtils.hasText(cleaned) ? cleaned : fallback;
//    }
//
//    private String cleanText(String value) {
//        return StringUtils.hasText(value) ? value.trim() : null;
//    }
//
//    private String fileUrl(Long fileId) {
//        if (fileId == null) {
//            return null;
//        }
//        return ServletUriComponentsBuilder.fromCurrentContextPath()
//                .path("/api/v1/files/{fileId}")
//                .buildAndExpand(fileId)
//                .toUriString();
//    }
//
//    private OffsetDateTime toOffset(LocalDateTime value) {
//        return value == null ? null : value.atZone(APP_ZONE).toOffsetDateTime();
//    }
//
//    private LocalDateTime nullableLocalDateTime(ResultSet resultSet, String column) throws java.sql.SQLException {
//        Timestamp value = resultSet.getTimestamp(column);
//        return value == null ? null : value.toLocalDateTime();
//    }
//
//    private SqlParts buildWhereClause(TenantAccess access, NormalizedQuery query) {
//        StringBuilder sql = new StringBuilder("""
//                WHERE mt.property_id = ?
//                """);
//        List<Object> args = new ArrayList<>();
//        args.add(access.propertyId());
//
//        if ("TENANT".equals(access.actorRole())) {
//            sql.append("""
//                  AND mt.ticket_scope = 'TENANT_ROOM'
//                  AND mt.room_id IN (
//                    SELECT DISTINCT lc.room_id
//                    FROM contract_occupants co
//                    JOIN lease_contracts lc ON lc.id = co.contract_id
//                    WHERE co.tenant_id = ?
//                      AND co.status = 'ACTIVE'
//                      AND lc.deleted_at IS NULL
//                      AND lc.status IN ('ACTIVE', 'EXPIRING_SOON', 'TERMINATION_PENDING')
//                  )
//                """);
//            args.add(query.tenantId());
//        }
//
//        if (StringUtils.hasText(query.status())) {
//            sql.append("  AND mt.status = ?\n");
//            args.add(query.status());
//        }
//
//        if (StringUtils.hasText(query.category())) {
//            appendCategoryFilter(sql, args, query.category(), query.rawCategory());
//        }
//
//        if (query.fromDate() != null) {
//            sql.append("  AND mt.created_at >= ?\n");
//            args.add(Timestamp.valueOf(query.fromDate().atStartOfDay()));
//        }
//
//        if (query.toDate() != null) {
//            sql.append("  AND mt.created_at <= ?\n");
//            args.add(Timestamp.valueOf(LocalDateTime.of(query.toDate(), LocalTime.MAX)));
//        }
//
//        if (StringUtils.hasText(query.keyword())) {
//            sql.append("  AND UPPER(mt.ticket_code) LIKE ?\n");
//            args.add("%" + query.keyword().toUpperCase(Locale.ROOT) + "%");
//        }
//
//        return new SqlParts(sql.toString(), args);
//    }
//
//    private void appendCategoryFilter(StringBuilder sql, List<Object> args, String category, String rawCategory) {
//        if ("OTHER".equals(category)) {
//            sql.append("  AND NOT (");
//            appendCategoryGroupPredicate(sql, args, ELECTRIC_ALIASES, ELECTRIC_KEYWORDS);
//            sql.append(" OR ");
//            appendCategoryGroupPredicate(sql, args, WATER_ALIASES, WATER_KEYWORDS);
//            sql.append(" OR ");
//            appendCategoryGroupPredicate(sql, args, EQUIPMENT_ALIASES, EQUIPMENT_KEYWORDS);
//            sql.append(")\n");
//            return;
//        }
//
//        if ("ELECTRIC".equals(category)) {
//            sql.append("  AND ");
//            appendCategoryGroupPredicate(sql, args, ELECTRIC_ALIASES, ELECTRIC_KEYWORDS);
//            sql.append("\n");
//            return;
//        }
//
//        if ("WATER".equals(category)) {
//            sql.append("  AND ");
//            appendCategoryGroupPredicate(sql, args, WATER_ALIASES, WATER_KEYWORDS);
//            sql.append("\n");
//            return;
//        }
//
//        if ("EQUIPMENT".equals(category)) {
//            sql.append("  AND ");
//            appendCategoryGroupPredicate(sql, args, EQUIPMENT_ALIASES, EQUIPMENT_KEYWORDS);
//            sql.append("\n");
//            return;
//        }
//
//        sql.append("  AND UPPER(mt.category) = ?\n");
//        args.add(rawCategory.toUpperCase(Locale.ROOT));
//    }
//
//    private void appendCategoryGroupPredicate(
//            StringBuilder sql,
//            List<Object> args,
//            List<String> aliases,
//            List<String> keywords
//    ) {
//        sql.append("(");
//        sql.append("UPPER(mt.category) IN (");
//        for (int i = 0; i < aliases.size(); i++) {
//            if (i > 0) {
//                sql.append(", ");
//            }
//            sql.append("?");
//            args.add(aliases.get(i));
//        }
//        sql.append(")");
//
//        for (String keyword : keywords) {
//            sql.append(" OR LOWER(CONCAT(COALESCE(mt.title, ''), ' ', COALESCE(mt.description, ''))) LIKE ?");
//            args.add("%" + keyword.toLowerCase(Locale.ROOT) + "%");
//        }
//        sql.append(")");
//    }
//
//    private NormalizedQuery normalize(Query query) {
//        int page = query.page() == null || query.page() < 1 ? 1 : query.page();
//        int size = query.size() == null || query.size() < 1 ? 20 : Math.min(query.size(), 100);
//        String status = normalizeStatus(query.status());
//        String rawCategory = firstText(query.category(), query.ticketType());
//        String category = normalizeCategoryInput(rawCategory);
//        String keyword = normalizeKeyword(query.keyword());
//
//        if (query.fromDate() != null && query.toDate() != null && query.toDate().isBefore(query.fromDate())) {
//            throw new ApiException(HttpStatus.BAD_REQUEST, "to_date phải lớn hơn hoặc bằng from_date");
//        }
//
//        boolean hasFilters = StringUtils.hasText(status)
//                || StringUtils.hasText(category)
//                || query.fromDate() != null
//                || query.toDate() != null
//                || StringUtils.hasText(keyword);
//
//        return new NormalizedQuery(
//                query.tenantId(),
//                status,
//                rawCategory,
//                category,
//                query.fromDate(),
//                query.toDate(),
//                keyword,
//                page,
//                size,
//                hasFilters
//        );
//    }
//
//    private String normalizeStatus(String status) {
//        if (!StringUtils.hasText(status) || "ALL".equalsIgnoreCase(status)) {
//            return null;
//        }
//
//        String normalized = status.trim().toUpperCase(Locale.ROOT);
//        normalized = switch (normalized) {
//            case "CHO_TIEP_NHAN", "CHỜ TIẾP NHẬN", "CHO TIEP NHAN" -> "PENDING_ACCEPTANCE";
//            case "DA_TIEP_NHAN", "ĐÃ TIẾP NHẬN", "DA TIEP NHAN" -> "ACCEPTED";
//            case "DANG_XU_LY", "ĐANG XỬ LÝ", "DANG XU LY" -> "IN_PROGRESS";
//            case "CHO_XAC_NHAN", "CHỜ XÁC NHẬN", "CHO XAC NHAN" -> "WAITING_CONFIRMATION";
//            case "HOAN_TAT", "HOÀN TẤT", "HOAN TAT" -> "COMPLETED";
//            case "TU_CHOI", "TỪ CHỐI", "TU CHOI" -> "REJECTED";
//            default -> normalized;
//        };
//
//        if (!VALID_STATUSES.contains(normalized)) {
//            throw new ApiException(HttpStatus.BAD_REQUEST, "Trạng thái phiếu sự cố không hợp lệ");
//        }
//        return normalized;
//    }
//
//    private String normalizeCategoryInput(String category) {
//        if (!StringUtils.hasText(category) || "ALL".equalsIgnoreCase(category)) {
//            return null;
//        }
//
//        String normalized = category.trim().toUpperCase(Locale.ROOT);
//        return switch (normalized) {
//            case "DIEN", "ĐIỆN", "ELECTRIC", "ELECTRICAL", "POWER" -> "ELECTRIC";
//            case "NUOC", "NƯỚC", "WATER", "PLUMBING", "WATER_LEAK" -> "WATER";
//            case "THIET_BI", "THIẾT BỊ", "THIET BI", "EQUIPMENT", "ASSET", "ASSET_DAMAGE", "APPLIANCE" -> "EQUIPMENT";
//            case "KHAC", "KHÁC", "OTHER" -> "OTHER";
//            default -> normalized;
//        };
//    }
//
//    private String normalizeKeyword(String keyword) {
//        if (!StringUtils.hasText(keyword)) {
//            return null;
//        }
//        String normalized = keyword.trim().replace("#", "");
//        return StringUtils.hasText(normalized) ? normalized : null;
//    }
//
//    private MaintenanceTicketListResponse.Item mapItem(ResultSet rs) throws java.sql.SQLException {
//        String ticketCode = rs.getString("ticket_code");
//        String category = categorize(
//                rs.getString("category"),
//                rs.getString("title"),
//                rs.getString("description")
//        );
//        LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();
//
//        String shortDescription = shortDescription(rs.getString("description"));
//        return new MaintenanceTicketListResponse.Item(
//                rs.getLong("id"),
//                ticketCode,
//                displayCode(ticketCode),
//                nullableLong(rs, "room_id"),
//                rs.getString("room_code"),
//                rs.getString("title"),
//                rs.getString("description"),
//                shortDescription,
//                shortDescription,
//                rs.getString("ticket_scope"),
//                category,
//                categoryLabel(category),
//                rs.getString("priority"),
//                rs.getString("status"),
//                statusLabel(rs.getString("status")),
//                rs.getString("rejected_reason"),
//                createdAt.atZone(APP_ZONE).toOffsetDateTime(),
//                createdAt.toLocalDate().format(DISPLAY_DATE_FORMATTER),
//                rs.getLong("attachment_count")
//        );
//    }
//
//    private String categorize(String category, String title, String description) {
//        String normalizedCategory = normalizeCategoryInput(category);
//        if ("ELECTRIC".equals(normalizedCategory)
//                || "WATER".equals(normalizedCategory)
//                || "EQUIPMENT".equals(normalizedCategory)
//                || "OTHER".equals(normalizedCategory)) {
//            return normalizedCategory;
//        }
//
//        String haystack = ((title == null ? "" : title) + " " + (description == null ? "" : description))
//                .toLowerCase(Locale.ROOT);
//        if (containsAny(haystack, ELECTRIC_KEYWORDS)) {
//            return "ELECTRIC";
//        }
//        if (containsAny(haystack, WATER_KEYWORDS)) {
//            return "WATER";
//        }
//        if (containsAny(haystack, EQUIPMENT_KEYWORDS)) {
//            return "EQUIPMENT";
//        }
//        return "OTHER";
//    }
//
//    private boolean containsAny(String value, List<String> keywords) {
//        for (String keyword : keywords) {
//            if (value.contains(keyword.toLowerCase(Locale.ROOT))) {
//                return true;
//            }
//        }
//        return false;
//    }
//
//    private String categoryLabel(String category) {
//        return switch (category) {
//            case "ELECTRIC" -> "Điện";
//            case "WATER" -> "Nước";
//            case "EQUIPMENT" -> "Thiết bị";
//            default -> "Khác";
//        };
//    }
//
//    private String statusLabel(String status) {
//        return switch (status) {
//            case "PENDING_ACCEPTANCE" -> "Chờ tiếp nhận";
//            case "ACCEPTED" -> "Đã tiếp nhận";
//            case "IN_PROGRESS" -> "Đang xử lý";
//            case "WAITING_CONFIRMATION" -> "Chờ xác nhận";
//            case "COMPLETED" -> "Hoàn tất";
//            case "REJECTED" -> "Từ chối";
//            case "CANCELLED" -> "Đã hủy";
//            default -> status;
//        };
//    }
//
//    private String eventTitle(String status) {
//        return switch (status) {
//            case "PENDING_ACCEPTANCE" -> "Yêu cầu mới";
//            case "ACCEPTED" -> "Đã tiếp nhận";
//            case "IN_PROGRESS" -> "Đang xử lý";
//            case "WAITING_CONFIRMATION" -> "Chờ xác nhận";
//            case "COMPLETED" -> "Hoàn tất";
//            case "REJECTED" -> "Từ chối";
//            default -> statusLabel(status);
//        };
//    }
//
//    private String displayCode(String ticketCode) {
//        if (!StringUtils.hasText(ticketCode)) {
//            return null;
//        }
//        return ticketCode.startsWith("#") ? ticketCode : "#" + ticketCode;
//    }
//
//    private String shortDescription(String description) {
//        if (description == null || description.length() <= 140) {
//            return description;
//        }
//        return description.substring(0, 137).trim() + "...";
//    }
//
//    private Long nullableLong(ResultSet resultSet, String column) throws java.sql.SQLException {
//        long value = resultSet.getLong(column);
//        return resultSet.wasNull() ? null : value;
//    }
//
//    private String firstText(String first, String second) {
//        if (StringUtils.hasText(first)) {
//            return first.trim();
//        }
//        return StringUtils.hasText(second) ? second.trim() : null;
//    }
//
//    public record Query(
//            Long tenantId,
//            String status,
//            String category,
//            String ticketType,
//            LocalDate fromDate,
//            LocalDate toDate,
//            String keyword,
//            Integer page,
//            Integer size
//    ) {
//    }
//
//    private record NormalizedQuery(
//            Long tenantId,
//            String status,
//            String rawCategory,
//            String category,
//            LocalDate fromDate,
//            LocalDate toDate,
//            String keyword,
//            int page,
//            int size,
//            boolean hasFilters
//    ) {
//    }
//
//    private record TenantAccess(Long propertyId, String actorRole, boolean ownsRequestedTenant) {
//    }
//
//    private record ActiveRoom(Long contractId, String roomCode) {
//    }
//
//    private record TicketStatusRow(
//            Long id,
//            String ticketCode,
//            String status,
//            Long roomId
//    ) {
//    }
//
//    private record TicketDetailRow(
//            Long id,
//            String ticketCode,
//            String status,
//            Long roomId,
//            String roomCode,
//            Long propertyId,
//            String propertyName,
//            String title,
//            String description,
//            String priority,
//            String ticketScope,
//            LocalDateTime createdAt,
//            Long createdBy,
//            String createdByName,
//            String workerName,
//            String repairItems,
//            LocalDateTime completedAt,
//            String completionNote
//    ) {
//    }
//
//    private record SqlParts(String sql, List<Object> args) {
//    }
//}
