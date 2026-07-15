package com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AccountPropertyAssignmentRequest {
    @NotNull
    Long propertyId;
}