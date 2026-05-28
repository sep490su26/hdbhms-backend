package com.sep490.hdbhms.occupancy.infrastructure.web.controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/tenants")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TenantController {

}
