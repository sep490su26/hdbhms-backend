package com.sep490.hdbhms.portal.infrastructure.web.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ContractHomeResponse {
    Long id;
    String contractCode;
    String status;
    LocalDateTime startDate;
    LocalDateTime endDate;
}
