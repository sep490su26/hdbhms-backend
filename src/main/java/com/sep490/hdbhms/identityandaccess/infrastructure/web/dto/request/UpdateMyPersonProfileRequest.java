package com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import com.fasterxml.jackson.annotation.JsonAlias;

public record UpdateMyPersonProfileRequest(
        @JsonAlias("phone")
        @Pattern(
                regexp = "^$|^(0|\\+84)\\d{9,10}$",
                message = "Số liên hệ phải là số điện thoại Việt Nam hợp lệ"
        )
        String contactPhone,

        @Email(message = "Địa chỉ email sai định dạng")
        String email
) {
}
