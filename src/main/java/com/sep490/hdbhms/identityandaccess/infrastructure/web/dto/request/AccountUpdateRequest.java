package com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AccountUpdateRequest {
    String username;
    String email;
    String password;
    List<String> roles;
}