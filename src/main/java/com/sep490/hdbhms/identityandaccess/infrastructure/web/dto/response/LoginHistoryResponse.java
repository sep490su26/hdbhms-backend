package com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.response;

import com.sep490.hdbhms.identityandaccess.domain.valueObjects.LoginMethod;
import com.sep490.hdbhms.identityandaccess.domain.valueObjects.LoginStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LoginHistoryResponse {
    Long accountId;

    LoginStatus status;

    String ipAddress;
    String userAgent;

    LoginMethod method;

    LocalDateTime loggedInAt;
}
