package com.sep490.hdbhms.maintenance.infrastructure.web.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TenantRepairDecisionRequest {
    @NotNull(message = "Vui lòng chọn đồng ý hoặc không sửa.")
    Boolean approved;

    @Size(max = 1000, message = "Lý do không sửa tối đa 1000 ký tự.")
    String reason;
}
