package com.sep490.hdbhms.identityandaccess.infrastructure.web.controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/accounts")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InternalAccountController {
//    @GetMapping("/{newUsername}")
//    public ApiResponse<AccountResponse> getAccountByUsername(@PathVariable String newUsername) {
//        return ApiResponse.<AccountResponse>builder()
//                .data(accountService.getAccountResponseByUsername(newUsername))
//                .build();
//    }
}
