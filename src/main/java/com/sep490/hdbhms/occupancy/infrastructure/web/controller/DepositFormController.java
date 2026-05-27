package com.sep490.hdbhms.occupancy.infrastructure.web.controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/deposit/forms")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DepositFormController {

}
