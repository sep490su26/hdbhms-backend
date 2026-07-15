package com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.AccountStatus;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserResponse {
    Long id;
    String phone;
    String email;
    Role role;
    boolean mustChangePassword;
    AccountStatus status;
    LocalDateTime lastLoginAt;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    LocalDateTime deletedAt;
    List<AssignedPropertyResponse> assignedProperties;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class AssignedPropertyResponse {
        Long id;
        String name;
        String code;
    }
}
