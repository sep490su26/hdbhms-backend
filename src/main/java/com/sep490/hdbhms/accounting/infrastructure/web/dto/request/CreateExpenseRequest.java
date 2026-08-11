package com.sep490.hdbhms.accounting.infrastructure.web.dto.request;

import com.sep490.hdbhms.accounting.domain.value_objects.ExpenseType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record CreateExpenseRequest(
        @NotNull(message = "Vui lòng chọn cơ sở") Long propertyId,
        Long roomId,
        @NotNull(message = "Vui lòng chọn loại chi phí") ExpenseType expenseType,
        @NotNull(message = "Vui lòng nhập số tiền") @Positive(message = "Số tiền phải lớn hơn 0") Long amount,
        @NotBlank(message = "Vui lòng nhập lý do chi phí")
        @Size(max = 1000, message = "Lý do chi phí không được vượt quá 1000 ký tự") String reason,
        @Size(max = 4000, message = "Mô tả không được vượt quá 4000 ký tự") String description,
        @Size(max = 255, message = "Tên nhà cung cấp không được vượt quá 255 ký tự") String vendorName,
        LocalDate expectedPaymentDate,
        List<@Valid CreateExpenseAttachmentRequest> attachments
) {
}
