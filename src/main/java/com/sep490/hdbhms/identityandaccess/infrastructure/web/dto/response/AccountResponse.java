package com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AccountResponse {
    String uuid;
    String username;
    String email;
    Boolean emailVerified;
    String providerId;
    Set<RoleResponse> roles;
    LocalDateTime createdAt;
    LocalDateTime lastUpdatedAt;
}
