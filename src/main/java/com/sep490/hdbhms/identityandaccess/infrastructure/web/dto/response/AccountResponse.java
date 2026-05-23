package com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

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
    LocalDateTime createdAt;
    LocalDateTime lastUpdatedAt;
}
