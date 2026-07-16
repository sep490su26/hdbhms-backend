# Business Rule Decisions Required

## Scope and source status

This document records contradictions that cannot be resolved safely as a Git conflict or technical forward-fix. No behavior below was changed during stabilization solely to choose one source over another.

Sources reviewed:

- `TroManager_PRD_v4 (1).pdf` and its workspace summary, PRD v4.0 dated 2026-05-01.
- `SU26_SEP490_G88_Report 3.0_SRS.docx`.
- `SU26_SEP490_G88_Report 3.1_RTW.xlsx`.
- `Requirements.xlsx`, the requirements workbook available in the workspace.

`Requirement_Draft.xlsx` was not present under `D:\Web_Source`. It may be a different name for `Requirements.xlsx`, but that equivalence has not been assumed and must be confirmed by the Product Owner.

## Decisions

| ID | Chủ đề | PRD | SRS | Workbook | Code hiện tại | Ảnh hưởng | Cần quyết định |
|---|---|---|---|---|---|---|---|
| BRD-01 | Số người tối đa trong phòng | Tối đa 3 người/phòng. | `BV-01 Room Capacity` cho phép 1-10; phần data policy lại ghi tối đa 3 active occupants. | `Requirements.xlsx` giới hạn 3, gồm người đứng hợp đồng. | Deposit DTO giới hạn 1-3; room mặc định `maxOccupants=3`, nhưng room editor cho phép cấu hình lớn hơn. | Deposit, transfer, occupant provisioning, room UI và seed có thể áp dụng giới hạn khác nhau. | Chọn hard limit 3 hay capacity cấu hình 1-10; nếu cấu hình, xác định hard ceiling và dữ liệu cũ cần chuẩn hóa. |
| BRD-02 | Thời điểm bắt buộc CCCD/chân dung | Form đặt cọc online yêu cầu CCCD hai mặt và ảnh chân dung. | Deposit booking yêu cầu complete guest identification information. | `Requirements.xlsx` row 104 chỉ thu thông tin cơ bản trên web, upload ảnh sau lần đăng nhập app đầu; các row 80-82 lại bắt buộc upload sau đổi mật khẩu. | Single và batch checkout hiện bắt buộc ba multipart files ngay khi đặt cọc. | Thay đổi thời điểm sẽ tác động API multipart, frontend guest, mobile onboarding, storage và validation. | Xác nhận ảnh bắt buộc tại checkout hay sau khi cấp tài khoản; nếu tách bước, quy định điều kiện thanh toán/ký hợp đồng khi hồ sơ chưa đủ. |
| BRD-03 | Nhà cung cấp thanh toán chính thức | Nêu VietQR/MoMo/ZaloPay cho cọc và hóa đơn. | Luồng invoice và callback chỉ định PayOS. | `Requirements.xlsx` tiếp tục nêu VietQR/MoMo/ZaloPay; RTW mô tả PayOS ở các luồng hiện hành. | Luồng thật đang dùng PayOS adapter/webhook; không có production flow tương đương cho MoMo/ZaloPay. | Contract UI, callback verification, idempotency, reconciliation và vận hành tài khoản thu phụ thuộc lựa chọn này. | Xác nhận PayOS là provider chính thức hay cần thêm provider; phân biệt VietQR là rail QR ngân hàng hay provider độc lập. |
| BRD-04 | Ai được sửa giá | PRD mô tả chủ trọ quyết định giá và ngoại lệ giá; ma trận quyền không cấp quản lý quyền quản lý phòng. | SRS/RTW có use case Manager edit lease contract price; data policy lại ghi only Owners configure pricing and fee settings. | RTW `UC-040` gán Manager sửa giá hợp đồng. | Giá niêm yết/phòng là Owner-only; `PATCH /lease-contracts/{id}/terms` và renewal cho Owner/Manager thay `monthlyRent`. | Sai quyền có thể thay đổi hóa đơn và nghĩa vụ hợp đồng. | Tách rõ giá niêm yết, giá hợp đồng, giá gia hạn và tariff; quyết định role được sửa từng loại và approval/audit bắt buộc. |
| BRD-05 | Danh sách role chính thức | Vai trò chính: Owner, Manager, Tenant; guest là khách chưa đăng nhập. | Nêu Owner, Manager, Accountant, Tenant, Guest; phần role assignment có nơi bỏ Guest. | RTW dùng bốn authenticated roles và Guest cho public use case. | Enum là `LEAD`, `TENANT`, `MANAGER`, `ACCOUNTANT`, `OWNER`; Guest không phải persisted role. | Navigation, controller annotations, account provisioning và báo cáo quyền phải dùng cùng taxonomy. | Xác nhận Guest là actor vô danh hay account role; xác nhận LEAD có phải role persisted hay domain state; chốt Accountant. |
| BRD-06 | Trạng thái phòng được đặt cọc online | Luồng chính nói chọn phòng Trống; bảng trạng thái cho phép người khác xin vào phòng Sắp trống. | Một AC chỉ cho Available; data policy lại cho Vacant hoặc Soon Vacant được reserve online. | `Requirements.xlsx` row 104 nói chọn phòng Trống. | Catalog public hiển thị `VACANT` và `SOON_VACANT`; checkout cho `SOON_VACANT` nếu ngày ký/vào phù hợp ngày dự kiến trống. | Tác động availability, double booking, lịch ký, hợp đồng sắp hết hạn và trải nghiệm guest. | Xác nhận `SOON_VACANT` có được cọc; nếu có, chốt cửa sổ ngày, điều kiện intention và xử lý khi khách cũ gia hạn. |
| BRD-07 | TTL reset password | Không chốt rõ trong PRD summary. | Functional section ghi reset token 15 phút; data/security policy ghi 30 phút. | RTW yêu cầu time-limited OTP nhưng không chốt TTL trong row use case. | `PasswordResetTokenAdapter` dùng 15 phút; OTP thông thường dùng 5 phút. | Security policy, email copy, test và support flow có thể lệch nhau. | Chọn 15 hay 30 phút cho password reset; xác nhận OTP thường có TTL riêng hay dùng cùng policy. |
| BRD-08 | Chuyển trạng thái phòng thủ công | Trạng thái chủ yếu phát sinh từ cọc, hợp đồng, bảo trì và hết hạn. | Nói hệ thống tự duy trì trạng thái theo business activities; Owner có quyền activate/deactivate room. | Không định nghĩa đầy đủ ma trận manual transition. | Generic Owner `PUT /rooms/{id}` hiện có thể gán bất kỳ enum status nào, kể cả lifecycle-managed status. | Có thể làm lệch room so với hold, deposit, lease hoặc transfer dù các service chính giữ invariant. | Chốt các transition thủ công được phép (đặc biệt MAINTENANCE/DRAFT/VACANT), điều kiện commitment và audit; các trạng thái còn lại nên chỉ do workflow quản lý. |

## Approval rule

Mỗi quyết định cần ghi người duyệt, ngày hiệu lực, lựa chọn cuối cùng và migration/backfill nếu có. Sau đó mới cập nhật code, API contract, test và tài liệu liên quan; không dùng file có timestamp mới hơn làm tiêu chí quyết định.
