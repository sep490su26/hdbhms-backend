package com.sep490.hdbhms.notification.application.service;

import com.sep490.hdbhms.notification.domain.model.NotificationTemplate;
import com.sep490.hdbhms.notification.domain.value_objects.NotificationChannel;
import com.sep490.hdbhms.notification.domain.value_objects.TemplateStatus;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class NotificationTemplateDefaults {
    private static final List<NotificationChannel> ALL_CHANNELS = List.of(NotificationChannel.values());

    private final List<Definition> definitions = List.of(
            definition(
                    "ROOM_TRANSFER_HOLDER_NOMINATION_REQUESTED",
                    "De cu nguoi dai dien phong moi",
                    "Gui cho nguoi duoc de cu lam holder moi khi holder hien tai chuyen di.",
                    "ROOM_TRANSFER",
                    variables(
                            "requestId",
                            "requestCode",
                            "nominatorUserId",
                            "nominatedHolderProfileId",
                            "oldRoomId",
                            "targetRoomId",
                            "oldRoomName",
                            "targetRoomName",
                            "requestedTransferDate",
                            "expectedTransferDate"
                    ),
                    sampleData(
                            "requestId", 680965088362752L,
                            "requestCode", "TR-680965088362752",
                            "nominatorUserId", 15L,
                            "nominatedHolderProfileId", 41L,
                            "oldRoomId", 104L,
                            "targetRoomId", 206L,
                            "oldRoomName", "Phong 104",
                            "targetRoomName", "Phong 206",
                            "requestedTransferDate", "2026-07-07",
                            "expectedTransferDate", "2026-07-07"
                    ),
                    "Bạn được đề cử làm người đại diện phòng",
                    "Yêu cầu chuyển phòng [[${requestCode}]] cần bạn xác nhận làm người đại diện mới của [[${oldRoomName}]] sau khi người hiện tại chuyển đi. Vui lòng phản hồi để quản lý tiếp tục xử lý."
            ),
            definition(
                    "ROOM_TRANSFER_TARGET_HOLDER_APPROVAL_REQUESTED",
                    "Xac nhan nguoi chuyen vao phong",
                    "Gui cho holder phong dich khi co nguoi muon chuyen vao phong cua ho.",
                    "ROOM_TRANSFER",
                    variables(
                            "requestId",
                            "requestCode",
                            "requesterUserId",
                            "oldRoomId",
                            "targetRoomId",
                            "oldRoomName",
                            "targetRoomName",
                            "targetContractId",
                            "requestedTransferDate",
                            "expectedTransferDate"
                    ),
                    sampleData(
                            "requestId", 680965088362752L,
                            "requestCode", "TR-680965088362752",
                            "requesterUserId", 12L,
                            "oldRoomId", 104L,
                            "targetRoomId", 206L,
                            "oldRoomName", "Phong 104",
                            "targetRoomName", "Phong 206",
                            "targetContractId", 91L,
                            "requestedTransferDate", "2026-07-07",
                            "expectedTransferDate", "2026-07-07"
                    ),
                    "Có người muốn chuyển vào phòng của bạn",
                    "Yêu cầu [[${requestCode}]]: khách từ [[${oldRoomName}]] muốn chuyển vào [[${targetRoomName}]]. Ngày dự kiến chuyển là [[${expectedTransferDate}]]. Vui lòng xác nhận nếu bạn đồng ý."
            ),
            definition(
                    "ROOM_TRANSFER_MANAGER_ACTION_REQUIRED",
                    "Yeu cau chuyen phong can quan ly xu ly",
                    "Gui cho manager/owner khi request chuyen phong can thao tac tiep theo.",
                    "ROOM_TRANSFER",
                    variables(
                            "requestId",
                            "requestCode",
                            "actionType",
                            "actionLabel",
                            "oldRoomId",
                            "targetRoomId",
                            "oldRoomName",
                            "targetRoomName",
                            "requestedTransferDate",
                            "expectedTransferDate"
                    ),
                    sampleData(
                            "requestId", 680965088362752L,
                            "requestCode", "TR-680965088362752",
                            "actionType", "UPLOAD_SIGNED_CONTRACT",
                            "actionLabel", "Tai ban hop dong da ky truc tiep",
                            "oldRoomId", 104L,
                            "targetRoomId", 206L,
                            "oldRoomName", "Phong 104",
                            "targetRoomName", "Phong 206",
                            "requestedTransferDate", "2026-07-07",
                            "expectedTransferDate", "2026-07-07"
                    ),
                    "Yêu cầu chuyển phòng cần xử lý",
                    "Yêu cầu [[${requestCode}]] đang cần quản lý xử lý: [[${actionLabel}]]. Chuyển từ [[${oldRoomName}]] sang [[${targetRoomName}]], ngày dự kiến chuyển [[${expectedTransferDate}]]."
            ),
            definition(
                    "TENANT_PROFILE_ACCESS_REQUESTED",
                    "Yeu cau xem ho so khach thue",
                    "Gui cho owner khi manager yeu cau quyen xem ho so khach thue.",
                    "CHANGE_REQUEST",
                    variables(
                            "requestId",
                            "profileId",
                            "managerId",
                            "managerName",
                            "tenantName",
                            "roomName",
                            "propertyName",
                            "reason"
                    ),
                    sampleData(
                            "requestId", 680965088362753L,
                            "profileId", 41L,
                            "managerId", 9L,
                            "managerName", "Tran Thi Quan Ly",
                            "tenantName", "Nguyen Van A",
                            "roomName", "Phong 104",
                            "propertyName", "Nha tro A",
                            "reason", "Can kiem tra ho so hop dong"
                    ),
                    "Yêu cầu xem hồ sơ khách thuê",
                    "[[${managerName}]] yêu cầu xem hồ sơ của [[${tenantName}]] tại [[${roomName}]] - [[${propertyName}]]. Lý do: [[${reason}]]."
            ),
            definition(
                    "TENANT_PROFILE_ACCESS_APPROVED",
                    "Da duoc duyet xem ho so",
                    "Gui cho manager khi owner duyet quyen xem ho so khach thue.",
                    "TENANT_PROFILE",
                    variables(
                            "requestId",
                            "profileId",
                            "managerId",
                            "tenantName",
                            "roomName",
                            "propertyName"
                    ),
                    sampleData(
                            "requestId", 680965088362753L,
                            "profileId", 41L,
                            "managerId", 9L,
                            "tenantName", "Nguyen Van A",
                            "roomName", "Phong 104",
                            "propertyName", "Nha tro A"
                    ),
                    "Đã được duyệt xem hồ sơ",
                    "Chủ trọ đã duyệt quyền xem hồ sơ của [[${tenantName}]] tại [[${roomName}]] - [[${propertyName}]]."
            ),
            definition(
                    "TENANT_PROFILE_ACCESS_REJECTED",
                    "Yeu cau xem ho so bi tu choi",
                    "Gui cho manager khi owner tu choi quyen xem ho so khach thue.",
                    "TENANT_PROFILE",
                    variables(
                            "requestId",
                            "profileId",
                            "managerId",
                            "tenantName",
                            "roomName",
                            "propertyName",
                            "resolutionNote"
                    ),
                    sampleData(
                            "requestId", 680965088362753L,
                            "profileId", 41L,
                            "managerId", 9L,
                            "tenantName", "Nguyen Van A",
                            "roomName", "Phong 104",
                            "propertyName", "Nha tro A",
                            "resolutionNote", "Chua du dieu kien truy cap"
                    ),
                    "Yêu cầu xem hồ sơ bị từ chối",
                    "Chủ trọ đã từ chối yêu cầu xem hồ sơ của [[${tenantName}]] tại [[${roomName}]] - [[${propertyName}]]. Ghi chú: [[${resolutionNote}]]."
            ),
            definition(
                    "DEBT_DIRECT_VISIT_REQUIRED",
                    "Can gap truc tiep khach thue no qua han",
                    "Gui cho owner/manager khi phong no qua han can gap truc tiep.",
                    "MANAGER_TASK",
                    variables(
                            "roomName",
                            "propertyName",
                            "totalDebt",
                            "dueDate"
                    ),
                    sampleData(
                            "roomName", "Phong 104",
                            "propertyName", "Nha tro A",
                            "totalDebt", 3500000L,
                            "dueDate", "2026-07-10"
                    ),
                    "Cần gặp trực tiếp khách thuê nợ quá hạn",
                    "[[${roomName}]] tại [[${propertyName}]] có tổng nợ [[${totalDebt}]] VND. Hạn xử lý: [[${dueDate}]]."
            ),
            definition(
                    "PRE_CREATED_ACCOUNT_NOTIFICATION",
                    "Thong bao tai khoan khach thue tao san",
                    "Gui email/SMS thong tin tai khoan tao san cho khach thue.",
                    "TENANT_ACCOUNT_PROVISIONING",
                    variables(
                            "contractCode",
                            "tenantName",
                            "propertyName",
                            "roomName",
                            "loginIdentifier",
                            "supportContact",
                            "contractId",
                            "recipientProfileId",
                            "recipientEmail",
                            "recipientPhone",
                            "tenantProfileIds"
                    ),
                    sampleData(
                            "contractCode", "HD-2026-001",
                            "tenantName", "Nguyen Van A",
                            "propertyName", "Nha tro A",
                            "roomName", "Phong 104",
                            "loginIdentifier", "tenant@example.com",
                            "supportContact", "0900000000",
                            "contractId", 91L,
                            "recipientProfileId", 41L,
                            "recipientEmail", "tenant@example.com",
                            "recipientPhone", "0900000000",
                            "tenantProfileIds", List.of(41L, 42L)
                    ),
                    "Thông tin tài khoản thuê phòng",
                    "Tài khoản thuê phòng của [[${tenantName}]] cho hợp đồng [[${contractCode}]] tại [[${roomName}]] - [[${propertyName}]] đã được tạo. Tên đăng nhập: [[${loginIdentifier}]]. Cần hỗ trợ liên hệ [[${supportContact}]]."
            )
    );

    private final Map<String, Definition> definitionsByEventType = definitions.stream()
            .collect(Collectors.toUnmodifiableMap(Definition::eventType, Function.identity()));

    public List<Definition> findAll() {
        return definitions;
    }

    public Optional<Definition> findByEventType(String eventType) {
        if (eventType == null || eventType.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(definitionsByEventType.get(eventType.trim()));
    }

    public List<NotificationTemplate> defaultTemplates(String eventType) {
        return findByEventType(eventType)
                .map(definition -> definition.defaultTemplatesByChannel().entrySet().stream()
                        .map(entry -> NotificationTemplate.builder()
                                .templateKey(definition.eventType())
                                .channel(entry.getKey())
                                .titleTemplate(entry.getValue().titleTemplate())
                                .bodyTemplate(entry.getValue().bodyTemplate())
                                .status(TemplateStatus.ACTIVE)
                                .build())
                        .toList())
                .orElseGet(List::of);
    }

    private Definition definition(
            String eventType,
            String displayName,
            String description,
            String targetType,
            List<Variable> variables,
            Map<String, Object> sampleData,
            String titleTemplate,
            String bodyTemplate
    ) {
        return new Definition(
                eventType,
                displayName,
                description,
                targetType,
                ALL_CHANNELS,
                variables,
                sampleData,
                allChannelTemplates(titleTemplate, bodyTemplate)
        );
    }

    private List<Variable> variables(String... names) {
        return List.of(names).stream()
                .map(name -> new Variable(name, true))
                .toList();
    }

    private Map<String, Object> sampleData(Object... entries) {
        Map<String, Object> data = new LinkedHashMap<>();
        for (int index = 0; index < entries.length; index += 2) {
            data.put((String) entries[index], entries[index + 1]);
        }
        return Collections.unmodifiableMap(data);
    }

    private Map<NotificationChannel, DefaultTemplate> allChannelTemplates(String titleTemplate, String bodyTemplate) {
        Map<NotificationChannel, DefaultTemplate> templates = new EnumMap<>(NotificationChannel.class);
        for (NotificationChannel channel : ALL_CHANNELS) {
            templates.put(channel, new DefaultTemplate(titleTemplate, bodyTemplate));
        }
        return Collections.unmodifiableMap(templates);
    }

    public record Definition(
            String eventType,
            String displayName,
            String description,
            String targetType,
            List<NotificationChannel> allowedChannels,
            List<Variable> variables,
            Map<String, Object> sampleData,
            Map<NotificationChannel, DefaultTemplate> defaultTemplatesByChannel
    ) {
    }

    public record Variable(String name, boolean required) {
    }

    public record DefaultTemplate(String titleTemplate, String bodyTemplate) {
    }
}
