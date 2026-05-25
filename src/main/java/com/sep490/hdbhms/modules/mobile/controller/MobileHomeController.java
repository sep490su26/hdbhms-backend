//package com.sep490.hdbhms.modules.mobile.controller;
//
//import com.sep490.hdbhms.modules.mobile.dto.MobileHomeResponse;
//import com.sep490.hdbhms.modules.mobile.service.MobileHomeService;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.security.oauth2.jwt.Jwt;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//@RestController
//@RequestMapping("/api/v1/tenants/{tenantId}/mobile")
//@Tag(name = "Mobile Home", description = "Aggregated data for mobile home screen")
//public class MobileHomeController {
//
//    private final MobileHomeService mobileHomeService;
//
//    public MobileHomeController(MobileHomeService mobileHomeService) {
//        this.mobileHomeService = mobileHomeService;
//    }
//
//    @GetMapping("/home")
//    @Operation(summary = "Get mobile home summary")
//    public ResponseEntity<MobileHomeResponse> getHome(
//            @AuthenticationPrincipal Jwt jwt,
//            @PathVariable Long tenantId
//    ) {
//        return ResponseEntity.ok(mobileHomeService.getHome(Long.parseLong(jwt.getSubject()), tenantId));
//    }
//}
