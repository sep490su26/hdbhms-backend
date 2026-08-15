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
                    "Đề cử người đại diện phòng mới",
                    "Gửi cho người được đề cử làm người đại diện phòng mới khi người hiện tại chuyển đi.",
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
                            "requestCode", "CP_P206_07_07_2026",
                            "nominatorUserId", 15L,
                            "nominatedHolderProfileId", 41L,
                            "oldRoomId", 104L,
                            "targetRoomId", 206L,
                            "oldRoomName", "Phòng 104",
                            "targetRoomName", "Phòng 206",
                            "requestedTransferDate", "2026-07-07",
                            "expectedTransferDate", "2026-07-07"
                    ),
                    "Bạn được đề cử làm người đại diện phòng",
                    "Yêu cầu chuyển phòng [[${requestCode}]] cần bạn xác nhận làm người đại diện mới của [[${oldRoomName}]] sau khi người hiện tại chuyển đi. Vui lòng phản hồi để quản lý tiếp tục xử lý."
            ),
            definition(
                    "ROOM_TRANSFER_TARGET_HOLDER_APPROVAL_REQUESTED",
                    "Xác nhận người chuyển vào phòng",
                    "Gửi cho người đại diện phòng đích khi có người muốn chuyển vào phòng của họ.",
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
                            "requestCode", "CP_P206_07_07_2026",
                            "requesterUserId", 12L,
                            "oldRoomId", 104L,
                            "targetRoomId", 206L,
                            "oldRoomName", "Phòng 104",
                            "targetRoomName", "Phòng 206",
                            "targetContractId", 91L,
                            "requestedTransferDate", "2026-07-07",
                            "expectedTransferDate", "2026-07-07"
                    ),
                    "Có người muốn chuyển vào phòng của bạn",
                    "Yêu cầu [[${requestCode}]]: khách từ [[${oldRoomName}]] muốn chuyển vào [[${targetRoomName}]]. Ngày dự kiến chuyển là [[${expectedTransferDate}]]. Vui lòng xác nhận nếu bạn đồng ý."
            ),
            definition(
                    "ROOM_TRANSFER_MANAGER_ACTION_REQUIRED",
                    "Yêu cầu chuyển phòng cần quản lý xử lý",
                    "Gửi cho quản lý hoặc chủ trọ khi yêu cầu chuyển phòng cần thao tác tiếp theo.",
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
                            "requestCode", "CP_P206_07_07_2026",
                            "actionType", "UPLOAD_SIGNED_CONTRACT",
                            "actionLabel", "Tải bản hợp đồng đã ký trực tiếp",
                            "oldRoomId", 104L,
                            "targetRoomId", 206L,
                            "oldRoomName", "Phòng 104",
                            "targetRoomName", "Phòng 206",
                            "requestedTransferDate", "2026-07-07",
                            "expectedTransferDate", "2026-07-07"
                    ),
                    "Yêu cầu chuyển phòng cần xử lý",
                    "Yêu cầu [[${requestCode}]] đang cần quản lý xử lý: [[${actionLabel}]]. Chuyển từ [[${oldRoomName}]] sang [[${targetRoomName}]], ngày dự kiến chuyển [[${expectedTransferDate}]]."
            ),
            definitionForChannels(
                    "VISIT_REQUEST_CREATED",
                    "Khách đặt lịch xem phòng",
                    "Gửi cho chủ trọ và quản lý khi có khách đặt lịch xem phòng.",
                    "VISIT_REQUEST",
                    List.of(NotificationChannel.WEB, NotificationChannel.PUSH),
                    variables(
                            "visitRequestId",
                            "visitorName",
                            "visitorPhone",
                            "visitorEmail",
                            "propertyId",
                            "propertyName",
                            "roomId",
                            "roomName",
                            "preferredStart",
                            "notes",
                            "targetRoute"
                    ),
                    sampleData(
                            "visitRequestId", 680965088362754L,
                            "visitorName", "Nguyễn Văn A",
                            "visitorPhone", "0912345678",
                            "visitorEmail", "khach@example.com",
                            "propertyId", 2L,
                            "propertyName", "Nhà trọ Hải Đăng",
                            "roomId", 101L,
                            "roomName", "Phòng 101",
                            "preferredStart", "2026-07-16T09:30",
                            "notes", "Muốn xem phòng buổi sáng",
                            "targetRoute", "/dashboard/viewing-customers"
                    ),
                    "Có khách đặt lịch xem phòng",
                    "[[${visitorName}]] ([[${visitorPhone}]]) đặt lịch xem [[${roomName}]] tại [[${propertyName}]] lúc [[${preferredStart}]]."
            ),
            definition(
                    "DEBT_DIRECT_VISIT_REQUIRED",
                    "Cần gặp trực tiếp khách thuê nợ quá hạn",
                    "Gửi cho chủ trọ hoặc quản lý khi phòng nợ quá hạn cần gặp trực tiếp.",
                    "MANAGER_TASK",
                    variables(
                            "roomName",
                            "propertyName",
                            "totalDebt",
                            "dueDate"
                    ),
                    sampleData(
                            "roomName", "Phòng 104",
                            "propertyName", "Nhà trọ A",
                            "totalDebt", 3500000L,
                            "dueDate", "2026-07-10"
                    ),
                    "Cần gặp trực tiếp khách thuê nợ quá hạn",
                    "[[${roomName}]] tại [[${propertyName}]] có tổng nợ [[${totalDebt}]] VND. Hạn xử lý: [[${dueDate}]]."
            ),
            definition(
                    "PRE_CREATED_ACCOUNT_NOTIFICATION",
                    "Thông báo tài khoản khách thuê tạo sẵn",
                    "Gửi Email/SMS thông tin tài khoản tạo sẵn cho khách thuê.",
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
                            "contractCode", "HDT_P104_01_01_2026",
                            "tenantName", "Nguyễn Văn A",
                            "propertyName", "Nhà trọ A",
                            "roomName", "Phòng 104",
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
            ),
            definitionForChannels(
                    "DEPOSIT_INFORMATION_NOTIFICATION",
                    "Thông báo đặt cọc thành công",
                    "Gửi Email/SMS thông tin đặt cọc cho khách thuê sau khi thanh toán thành công.",
                    "DEPOSIT_AGREEMENT",
                    List.of(NotificationChannel.EMAIL, NotificationChannel.SMS),
                    variables(
                            "recipientFullName",
                            "depositReference",
                            "depositDetails",
                            "depositAmount",
                            "propertyName",
                            "expectedMoveInDate",
                            "expectedLeaseSignDate",
                            "confirmedAt",
                            "status",
                            "depositAgreementId",
                            "depositBatchId"
                    ),
                    sampleData(
                            "recipientFullName", "Nguyễn Văn A",
                            "depositReference", "COC_104_2026-08-12",
                            "depositDetails", "Phòng 104",
                            "depositAmount", 3000000L,
                            "propertyName", "Nhà trọ A",
                            "expectedMoveInDate", "2026-09-01",
                            "expectedLeaseSignDate", "2026-08-25",
                            "confirmedAt", "2026-08-12T10:30:00",
                            "status", "PAID",
                            "depositAgreementId", 91L,
                            "depositBatchId", null
                    ),
                    "Đặt cọc thành công - [[${depositReference}]]",
                    "Kính gửi [[${recipientFullName}]],\n\nĐặt cọc [[${depositReference}]] đã được xác nhận thành công.\nChi tiết: [[${depositDetails}]]\nSố tiền: [[${depositAmount}]] VND\nCơ sở: [[${propertyName}]]\nNgày vào ở dự kiến: [[${expectedMoveInDate}]]\nNgày ký hợp đồng dự kiến: [[${expectedLeaseSignDate}]]\n\nTrân trọng."
            ),
            definitionForChannels(
                    "UTILITY_METER_READING_PERIOD_OPENED",
                    "Kỳ nhập điện nước đã mở",
                    "Gửi cho quản lý cơ sở khi hệ thống mở kỳ nhập điện nước hằng tháng.",
                    "UTILITY_BILLING_RUN",
                    List.of(NotificationChannel.WEB, NotificationChannel.PUSH),
                    variables(
                            "runId",
                            "propertyId",
                            "propertyName",
                            "billingPeriod",
                            "period",
                            "totalRooms",
                            "readyCount",
                            "warningCount",
                            "skippedCount",
                            "targetRoute"
                    ),
                    sampleData(
                            "runId", 91L,
                            "propertyId", 2L,
                            "propertyName", "Nhà trọ Hải Đăng 2",
                            "billingPeriod", "2026-07",
                            "period", "2026-07",
                            "totalRooms", 24,
                            "readyCount", 0,
                            "warningCount", 0,
                            "skippedCount", 24,
                            "targetRoute", "/dashboard/meter-readings"
                    ),
                    "Đã mở kỳ nhập điện nước [[${billingPeriod}]]",
                    "Kỳ điện nước [[${billingPeriod}]] tại [[${propertyName}]] đã sẵn sàng. Có [[${totalRooms}]] phòng cần nhập chỉ số trước khi phát hành hóa đơn."
            ),
            definitionForChannels(
                    "INVOICE_ISSUED",
                    "Hóa đơn mới đã phát hành",
                    "Gửi cho khách thuê khi hóa đơn mới được phát hành.",
                    "INVOICE",
                    List.of(NotificationChannel.WEB, NotificationChannel.PUSH),
                    variables(
                            "invoiceId",
                            "invoiceCode",
                            "invoiceType",
                            "roomCode",
                            "propertyName",
                            "billingPeriod",
                            "amount",
                            "totalAmount",
                            "remainingAmount",
                            "dueDate",
                            "targetRoute"
                    ),
                    sampleData(
                            "invoiceId", 91L,
                            "invoiceCode", "INV-UTL-MONTHLY-404-202607",
                            "invoiceType", "UTILITY",
                            "roomCode", "404",
                            "propertyName", "Nhà trọ Hải Đăng 2",
                            "billingPeriod", "2026-07",
                            "amount", 645000L,
                            "totalAmount", 645000L,
                            "remainingAmount", 645000L,
                            "dueDate", "2026-07-31",
                            "targetRoute", "/payment"
                    ),
                    "Có hóa đơn mới [[${invoiceCode}]]",
                    "Hóa đơn [[${invoiceCode}]] của phòng [[${roomCode}]] kỳ [[${billingPeriod}]] đã phát hành. Số tiền cần thanh toán: [[${remainingAmount}]] VND. Hạn thanh toán: [[${dueDate}]]."
            ),
            definitionForChannels(
                    "INVOICE_OVERDUE",
                    "Cảnh báo hóa đơn quá hạn",
                    "Gửi cho khách thuê khi hóa đơn đã hết hạn thanh toán.",
                    "INVOICE",
                    List.of(NotificationChannel.WEB, NotificationChannel.PUSH),
                    variables(
                            "invoiceId",
                            "invoiceCode",
                            "roomCode",
                            "propertyName",
                            "remainingAmount",
                            "dueDate"
                    ),
                    sampleData(
                            "invoiceId", 91L,
                            "invoiceCode", "INV-2026-07-001",
                            "roomCode", "404",
                            "propertyName", "Nhà trọ Hải Đăng 2",
                            "remainingAmount", 1250000L,
                            "dueDate", "2026-07-10"
                    ),
                    "Hóa đơn [[${invoiceCode}]] đã quá hạn",
                    "Hóa đơn [[${invoiceCode}]] của phòng [[${roomCode}]] tại [[${propertyName}]] đã quá hạn từ [[${dueDate}]]. Số tiền còn phải thanh toán: [[${remainingAmount}]] VND."
            ),
            definitionForChannels(
                    "INVOICE_PAID",
                    "Hóa đơn đã thanh toán",
                    "Gửi cho khách thuê khi hóa đơn được ghi nhận đã thanh toán đủ.",
                    "INVOICE",
                    List.of(NotificationChannel.WEB, NotificationChannel.PUSH),
                    variables(
                            "invoiceId",
                            "invoiceCode",
                            "roomCode",
                            "propertyName",
                            "paymentAmount",
                            "paidAmount"
                    ),
                    sampleData(
                            "invoiceId", 91L,
                            "invoiceCode", "INV-2026-07-001",
                            "roomCode", "404",
                            "propertyName", "Nhà trọ Hải Đăng 2",
                            "paymentAmount", 1250000L,
                            "paidAmount", 1250000L
                    ),
                    "Đã ghi nhận thanh toán hóa đơn [[${invoiceCode}]]",
                    "Hóa đơn [[${invoiceCode}]] của phòng [[${roomCode}]] đã được thanh toán đủ. Số tiền ghi nhận: [[${paymentAmount}]] VND."
            ),
            definitionForChannels(
                    "INVOICE_PARTIALLY_PAID",
                    "Hóa đơn thanh toán một phần",
                    "Gửi cho khách thuê khi hóa đơn được ghi nhận thanh toán một phần.",
                    "INVOICE",
                    List.of(NotificationChannel.WEB, NotificationChannel.PUSH),
                    variables(
                            "invoiceId",
                            "invoiceCode",
                            "roomCode",
                            "propertyName",
                            "paymentAmount",
                            "remainingAmount"
                    ),
                    sampleData(
                            "invoiceId", 91L,
                            "invoiceCode", "INV-2026-07-001",
                            "roomCode", "404",
                            "propertyName", "Nhà trọ Hải Đăng 2",
                            "paymentAmount", 500000L,
                            "remainingAmount", 750000L
                    ),
                    "Đã ghi nhận thanh toán một phần",
                    "Hóa đơn [[${invoiceCode}]] của phòng [[${roomCode}]] đã ghi nhận [[${paymentAmount}]] VND. Số tiền còn lại: [[${remainingAmount}]] VND."
            ),
            definitionForChannels(
                    "LIQUIDATION_DEPOSIT_REFUND_RECORDED",
                    "Xác nhận nhận hoàn cọc",
                    "Gửi cho khách thuê khi chủ trọ đã duyệt khoản hoàn cọc và khách thuê cần xác nhận đã nhận tiền.",
                    "CHANGE_REQUEST",
                    List.of(NotificationChannel.WEB, NotificationChannel.PUSH),
                    variables(
                            "requestId",
                            "requestCode",
                            "contractId",
                            "contractCode",
                            "roomCode",
                            "amount",
                            "depositRefundAmount",
                            "targetRoute"
                    ),
                    sampleData(
                            "requestId", 680965088362757L,
                            "requestCode", "TLHD_P101_29_07_2026",
                            "contractId", 91L,
                            "contractCode", "HDT_P101_29_07_2026",
                            "roomCode", "101",
                            "amount", 2000000L,
                            "depositRefundAmount", 2000000L,
                            "targetRoute", "/requests"
                    ),
                    "Khoản hoàn cọc đã được duyệt",
                    "Khoản hoàn cọc [[${depositRefundAmount}]] VND cho hợp đồng [[${contractCode}]] phòng [[${roomCode}]] đã được chủ trọ duyệt. Sau khi nhận tiền, vui lòng xác nhận đã nhận tiền."
            ),
            definitionForChannels(
                    "LIQUIDATION_DEPOSIT_FORFEITURE_CONFIRMATION_REQUIRED",
                    "Xác nhận mất cọc thanh lý hợp đồng",
                    "Gửi cho khách thuê khi khoản tiền cọc bị khấu trừ và cần xác nhận chấp nhận mất cọc.",
                    "CHANGE_REQUEST",
                    List.of(NotificationChannel.WEB, NotificationChannel.PUSH),
                    variables(
                            "requestId",
                            "requestCode",
                            "contractId",
                            "contractCode",
                            "roomCode",
                            "depositForfeitureAmount",
                            "depositForfeitureReason",
                            "targetRoute"
                    ),
                    sampleData(
                            "requestId", 680965088362758L,
                            "requestCode", "TLHD_P403_14_08_2026",
                            "contractId", 403L,
                            "contractCode", "HDT_P403_01_09_2025",
                            "roomCode", "403",
                            "depositForfeitureAmount", 2300000L,
                            "depositForfeitureReason", "Bồi thường hư hỏng tài sản",
                            "targetRoute", "/requests"
                    ),
                    "Cần xác nhận khoản mất cọc",
                    "Khoản mất cọc [[${depositForfeitureAmount}]] VND của hợp đồng [[${contractCode}]] phòng [[${roomCode}]] đang chờ bạn xác nhận chấp nhận. Lý do: [[${depositForfeitureReason}]]."
            ),
            definitionForChannels(
                    "EXPENSE_APPROVAL_REQUESTED",
                    "Yêu cầu chi cần duyệt",
                    "Gửi cho chủ trọ khi quản lý tạo yêu cầu chi.",
                    "EXPENSE_REQUEST",
                    List.of(NotificationChannel.WEB, NotificationChannel.PUSH),
                    variables(
                            "expenseCode",
                            "expenseType",
                            "propertyName",
                            "roomCode",
                            "amount",
                            "description"
                    ),
                    sampleData(
                            "expenseCode", "EXP-202607-001",
                            "expenseType", "MAINTENANCE",
                            "propertyName", "Nhà trọ Hải Đăng 2",
                            "roomCode", "404",
                            "amount", 3200000L,
                            "description", "Sửa điều hòa"
                    ),
                    "Có yêu cầu chi mới cần duyệt",
                    "Yêu cầu chi [[${expenseCode}]] tại [[${propertyName}]] cần duyệt. Số tiền: [[${amount}]] VND. Nội dung: [[${description}]]."
            ),
            definitionForChannels(
                    "EXPENSE_APPROVED",
                    "Yêu cầu chi đã được duyệt",
                    "Gửi cho người tạo khi chủ trọ duyệt yêu cầu chi.",
                    "EXPENSE_REQUEST",
                    List.of(NotificationChannel.WEB, NotificationChannel.PUSH),
                    variables(
                            "expenseCode",
                            "amount",
                            "propertyName",
                            "status"
                    ),
                    sampleData(
                            "expenseCode", "EXP-202607-001",
                            "amount", 3200000L,
                            "propertyName", "Nhà trọ Hải Đăng 2",
                            "status", "READY_FOR_PAYMENT"
                    ),
                    "Yêu cầu chi [[${expenseCode}]] đã được duyệt",
                    "Chủ trọ đã duyệt yêu cầu chi [[${expenseCode}]] với số tiền [[${amount}]] VND. Trạng thái hiện tại: [[${status}]]."
            ),
            definitionForChannels(
                    "EXPENSE_REJECTED",
                    "Yêu cầu chi bị từ chối",
                    "Gửi cho người tạo khi chủ trọ từ chối yêu cầu chi.",
                    "EXPENSE_REQUEST",
                    List.of(NotificationChannel.WEB, NotificationChannel.PUSH),
                    variables(
                            "expenseCode",
                            "amount",
                            "propertyName",
                            "resolutionNote"
                    ),
                    sampleData(
                            "expenseCode", "EXP-202607-001",
                            "amount", 3200000L,
                            "propertyName", "Nhà trọ Hải Đăng 2",
                            "resolutionNote", "Thiếu báo giá"
                    ),
                    "Yêu cầu chi [[${expenseCode}]] bị từ chối",
                    "Chủ trọ đã từ chối yêu cầu chi [[${expenseCode}]]. Lý do: [[${resolutionNote}]]."
            ),
            definitionForChannels(
                    "EXPENSE_PAID",
                    "Khoản chi đã được thanh toán",
                    "Gửi cho người tạo khi chủ trọ ghi nhận đã thanh toán khoản chi.",
                    "EXPENSE_REQUEST",
                    List.of(NotificationChannel.WEB, NotificationChannel.PUSH),
                    variables(
                            "expenseCode",
                            "amount",
                            "propertyName",
                            "status"
                    ),
                    sampleData(
                            "expenseCode", "EXP-202607-001",
                            "amount", 3200000L,
                            "propertyName", "Nhà trọ Hải Đăng 2",
                            "status", "PAID"
                    ),
                    "Đã thanh toán yêu cầu chi [[${expenseCode}]]",
                    "Chủ trọ đã ghi nhận thanh toán yêu cầu chi [[${expenseCode}]] với số tiền [[${amount}]] VND."
            ),
            definitionForChannels(
                    "CHANGE_REQUEST_CREATED",
                    "Có yêu cầu mới cần xử lý",
                    "Gửi cho người phụ trách khi một yêu cầu thay đổi mới được tạo.",
                    "CHANGE_REQUEST",
                    List.of(NotificationChannel.PUSH),
                    variables(
                            "requestId",
                            "requestCode",
                            "requestType",
                            "requestTypeLabel",
                            "title",
                            "description",
                            "requesterId",
                            "requesterRole",
                            "assignedRole",
                            "targetType",
                            "targetId",
                            "targetRoute"
                    ),
                    sampleData(
                            "requestId", 680965088362756L,
                            "requestCode", "GHHD_P101_29_07_2026",
                            "requestType", "CONTRACT_RENEWAL",
                            "requestTypeLabel", "Yêu cầu gia hạn hợp đồng",
                            "title", "Yêu cầu gia hạn hợp đồng HDT_P101_29_07_2026",
                            "description", "Khách muốn gia hạn hợp đồng.",
                            "requesterId", 12L,
                            "requesterRole", "TENANT",
                            "assignedRole", "OWNER",
                            "targetType", "CONTRACT",
                            "targetId", 91L,
                            "targetRoute", "/dashboard/requests?requestId=680965088362756"
                    ),
                    "Có [[${requestTypeLabel}]] mới",
                    "[[${requestCode}]] - [[${title}]] đang chờ xử lý. Nội dung: [[${description}]]."
            ),
            definitionForChannels(
                    "LEASE_EXPIRY_REMINDER_FIRST",
                    "Nhắc lần 1 hợp đồng sắp hết hạn",
                    "Gửi cho khách thuê khi hợp đồng còn khoảng 3 tháng.",
                    "CONTRACT",
                    List.of(NotificationChannel.PUSH),
                    leaseReminderVariables(),
                    leaseReminderSampleData("FIRST"),
                    "Hợp đồng [[${contractCode}]] sắp hết hạn",
                    "Phòng [[${roomName}]] tại [[${propertyName}]] sẽ hết hạn vào [[${endDate}]]. Bạn muốn gia hạn, chuyển phòng hay chuyển đi?"
            ),
            definitionForChannels(
                    "LEASE_EXPIRY_REMINDER_SECOND",
                    "Nhắc lần 2 hợp đồng sắp hết hạn",
                    "Gửi lại cho khách thuê nếu chưa phản hồi sau lần nhắc đầu.",
                    "CONTRACT",
                    List.of(NotificationChannel.PUSH),
                    leaseReminderVariables(),
                    leaseReminderSampleData("SECOND"),
                    "Bạn chưa phản hồi về hợp đồng [[${contractCode}]]",
                    "Vui lòng chọn ý định cho phòng [[${roomName}]] trước ngày hết hạn [[${endDate}]] để quản lý sắp xếp kịp thời."
            ),
            definitionForChannels(
                    "LEASE_EXPIRY_REMINDER_FINAL",
                    "Nhắc lần cuối hợp đồng sắp hết hạn",
                    "Gửi lần cuối cho khách thuê và kích hoạt công việc cho quản lý.",
                    "CONTRACT",
                    List.of(NotificationChannel.PUSH),
                    leaseReminderVariables(),
                    leaseReminderSampleData("FINAL"),
                    "Nhắc lần cuối về hợp đồng [[${contractCode}]]",
                    "Hợp đồng phòng [[${roomName}]] sắp hết hạn vào [[${endDate}]]. Vui lòng phản hồi để tránh chậm xử lý bàn giao hoặc gia hạn."
            ),
            definitionForChannels(
                    "LEASE_EXPIRY_MANAGER_VISIT_REQUIRED",
                    "Cần gặp trực tiếp khách sắp hết hạn hợp đồng",
                    "Gửi cho quản lý khi khách không phản hồi sau 3 lần nhắc.",
                    "MANAGER_TASK",
                    List.of(NotificationChannel.WEB, NotificationChannel.PUSH),
                    leaseManagerVariables(),
                    leaseManagerSampleData("Khách chưa phản hồi sau 3 lần nhắc."),
                    "Cần gặp khách phòng [[${roomName}]]",
                    "Hợp đồng [[${contractCode}]] hết hạn ngày [[${endDate}]], hạn công việc [[${dueDate}]]. Lý do: [[${reason}]]."
            ),
            definitionForChannels(
                    "LEASE_RENEWAL_TERMS_CONFIRMATION_DUE",
                    "Cần chốt điều khoản gia hạn",
                    "Gửi cho quản lý khi khách đã chọn gia hạn.",
                    "MANAGER_TASK",
                    List.of(NotificationChannel.WEB, NotificationChannel.PUSH),
                    leaseManagerVariables(),
                    leaseManagerSampleData("Khách đã chọn gia hạn hợp đồng."),
                    "Cần chốt gia hạn hợp đồng [[${contractCode}]]",
                    "Khách phòng [[${roomName}]] đã chọn gia hạn. Cần chốt giá, thời hạn, tiền cọc và lịch ký trước [[${dueDate}]]."
            ),
            definitionForChannels(
                    "LEASE_HANDOVER_CONFIRMATION_DUE",
                    "Cần chốt lịch bàn giao phòng",
                    "Gửi cho quản lý khi hợp đồng sắp đến hạn bàn giao.",
                    "MANAGER_TASK",
                    List.of(NotificationChannel.WEB, NotificationChannel.PUSH),
                    leaseManagerVariables(),
                    leaseManagerSampleData("Cần chốt ngày giờ bàn giao."),
                    "Cần chốt bàn giao phòng [[${roomName}]]",
                    "Hợp đồng [[${contractCode}]] sắp đến hạn [[${endDate}]]. Hạn công việc [[${dueDate}]]. Lý do: [[${reason}]]."
            ),
            definitionForChannels(
                    "CONTRACT_EXPIRING_SOON_REVIEW",
                    "Hợp đồng sắp hết hạn cần theo dõi",
                    "Gửi cho chủ trọ hoặc quản lý khi hợp đồng sắp hết hạn nhưng khách thuê chưa ghi nhận ý định.",
                    "CONTRACT",
                    List.of(NotificationChannel.WEB, NotificationChannel.PUSH),
                    variables(
                            "contractId",
                            "contractCode",
                            "roomId",
                            "roomName",
                            "roomCode",
                            "propertyName",
                            "endDate",
                            "tenantIntention",
                            "targetRoute"
                    ),
                    sampleData(
                            "contractId", 123L,
                            "contractCode", "HDT_P403_01_09_2025",
                            "roomId", 403L,
                            "roomName", "Phòng 403",
                            "roomCode", "403",
                            "propertyName", "Nhà trọ Hải Đăng 1",
                            "endDate", "2026-09-30",
                            "tenantIntention", null,
                            "targetRoute", "/dashboard/contracts/123"
                    ),
                    "Hợp đồng phòng [[${roomCode}]] sắp hết hạn",
                    "Hợp đồng [[${contractCode}]] của phòng [[${roomCode}]] sẽ hết hạn vào [[${endDate}]]. Khách thuê chưa ghi nhận ý định; vui lòng theo dõi và xử lý khi có phản hồi."
            ),
            definitionForChannels(
                    "MAINTENANCE_REPAIR_PROPOSAL_SENT",
                    "Có phương án sửa chữa cần quyết định",
                    "Gửi cho khách thuê khi quản lý đã nhập người sửa và chi phí dự kiến.",
                    "MAINTENANCE_TICKET",
                    List.of(NotificationChannel.WEB, NotificationChannel.PUSH),
                    variables(
                            "ticketId",
                            "ticketCode",
                            "roomCode",
                            "roomName",
                            "propertyName",
                            "workerName",
                            "repairItems",
                            "costAmount",
                            "targetRoute"
                    ),
                    sampleData(
                            "ticketId", 201L,
                            "ticketCode", "SC_P201_14_08_2026",
                            "roomCode", "201",
                            "roomName", "Phòng 201",
                            "propertyName", "Nhà trọ Hải Đăng",
                            "workerName", "Nguyễn Văn sửa",
                            "repairItems", "Thay khóa cửa phòng",
                            "costAmount", 450000L,
                            "targetRoute", "/dashboard/maintenance/201"
                    ),
                    "Phương án sửa chữa phòng [[${roomName}]]",
                    "Quản lý đã gửi phương án sửa chữa phiếu [[${ticketCode}]]. Người sửa: [[${workerName}]]. Chi phí dự kiến: [[${costAmount}]] VNĐ. Vui lòng mở phiếu để quyết định."
            ),
            definitionForChannels(
                    "MAINTENANCE_REPAIR_APPROVED",
                    "Khách thuê đã đồng ý sửa chữa",
                    "Gửi cho quản lý khi khách thuê đồng ý phương án sửa chữa.",
                    "MAINTENANCE_TICKET",
                    List.of(NotificationChannel.WEB, NotificationChannel.PUSH),
                    variables(
                            "ticketId",
                            "ticketCode",
                            "roomCode",
                            "roomName",
                            "targetRoute"
                    ),
                    sampleData(
                            "ticketId", 201L,
                            "ticketCode", "SC_P201_14_08_2026",
                            "roomCode", "201",
                            "roomName", "Phòng 201",
                            "targetRoute", "/dashboard/maintenance/201"
                    ),
                    "Khách thuê đồng ý sửa chữa",
                    "Khách thuê phòng [[${roomName}]] đã đồng ý phương án của phiếu [[${ticketCode}]]. Bạn có thể bắt đầu cập nhật tiến độ xử lý."
            ),
            definitionForChannels(
                    "MAINTENANCE_REPAIR_REJECTED",
                    "Khách thuê không đồng ý sửa chữa",
                    "Gửi cho quản lý khi khách thuê từ chối phương án sửa chữa.",
                    "MAINTENANCE_TICKET",
                    List.of(NotificationChannel.WEB, NotificationChannel.PUSH),
                    variables(
                            "ticketId",
                            "ticketCode",
                            "roomCode",
                            "roomName",
                            "reason",
                            "targetRoute"
                    ),
                    sampleData(
                            "ticketId", 201L,
                            "ticketCode", "SC_P201_14_08_2026",
                            "roomCode", "201",
                            "roomName", "Phòng 201",
                            "reason", "Khách chưa đồng ý mức chi phí dự kiến.",
                            "targetRoute", "/dashboard/maintenance/201"
                    ),
                    "Khách thuê không đồng ý sửa chữa",
                    "Khách thuê phòng [[${roomName}]] không đồng ý phương án của phiếu [[${ticketCode}]]. Lý do: [[${reason}]]."
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
        return definitionForChannels(
                eventType,
                displayName,
                description,
                targetType,
                ALL_CHANNELS,
                variables,
                sampleData,
                titleTemplate,
                bodyTemplate
        );
    }

    private Definition definitionForChannels(
            String eventType,
            String displayName,
            String description,
            String targetType,
            List<NotificationChannel> channels,
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
                channels,
                variables,
                sampleData,
                channelTemplates(channels, titleTemplate, bodyTemplate)
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

    private List<Variable> leaseReminderVariables() {
        return variables(
                "contractId",
                "contractCode",
                "roomId",
                "roomName",
                "propertyName",
                "endDate",
                "daysRemaining",
                "stage",
                "targetRoute"
        );
    }

    private Map<String, Object> leaseReminderSampleData(String stage) {
        return sampleData(
                "contractId", 123L,
                "contractCode", "HDT_P101_01_01_2026",
                "roomId", 10L,
                "roomName", "Phòng 101",
                "propertyName", "Nhà trọ Hải Đăng",
                "endDate", "2026-10-17",
                "daysRemaining", 90,
                "stage", stage,
                "targetRoute", "/contract"
        );
    }

    private List<Variable> leaseManagerVariables() {
        return variables(
                "taskId",
                "contractId",
                "contractCode",
                "roomId",
                "roomName",
                "propertyName",
                "endDate",
                "dueDate",
                "reason",
                "targetRoute"
        );
    }

    private Map<String, Object> leaseManagerSampleData(String reason) {
        return sampleData(
                "taskId", 456L,
                "contractId", 123L,
                "contractCode", "HDT_P101_01_01_2026",
                "roomId", 10L,
                "roomName", "Phòng 101",
                "propertyName", "Nhà trọ Hải Đăng",
                "endDate", "2026-10-17",
                "dueDate", "2026-10-03",
                "reason", reason,
                "targetRoute", "/dashboard/contracts/123"
        );
    }

    private Map<NotificationChannel, DefaultTemplate> allChannelTemplates(String titleTemplate, String bodyTemplate) {
        return channelTemplates(ALL_CHANNELS, titleTemplate, bodyTemplate);
    }

    private Map<NotificationChannel, DefaultTemplate> channelTemplates(
            List<NotificationChannel> channels,
            String titleTemplate,
            String bodyTemplate
    ) {
        Map<NotificationChannel, DefaultTemplate> templates = new EnumMap<>(NotificationChannel.class);
        for (NotificationChannel channel : channels) {
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
