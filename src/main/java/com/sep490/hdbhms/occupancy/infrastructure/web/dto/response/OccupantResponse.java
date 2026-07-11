package com.sep490.hdbhms.occupancy.infrastructure.web.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OccupantResponse {
    Long tenantProfileId;
    String fullName;
    String phoneNumber;
    String identityNumber;
    String status;
}
