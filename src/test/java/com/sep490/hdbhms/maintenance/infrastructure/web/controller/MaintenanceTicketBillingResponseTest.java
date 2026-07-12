package com.sep490.hdbhms.maintenance.infrastructure.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep490.hdbhms.maintenance.infrastructure.web.dto.response.MaintenanceTicketResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MaintenanceTicketBillingResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializesTicketAndBillingStatusesSeparately() throws Exception {
        MaintenanceTicketResponse response = MaintenanceTicketResponse.builder()
                .ticketStatus("COMPLETED")
                .ticketStatusLabel("Hoàn tất xử lý")
                .billingStatus("PENDING_PAYMENT")
                .billingStatusLabel("Chờ thanh toán")
                .invoiceId(41L)
                .invoiceCode("INV-MNT-4-0618094846")
                .invoiceStatus("ISSUED")
                .paymentStatus("PENDING_PAYMENT")
                .chargeAmount(2_000L)
                .chargeToTenant(true)
                .payer("TENANT")
                .lineType("MAINTENANCE_COMPENSATION")
                .build();

        String json = objectMapper.writeValueAsString(response);

        assertTrue(json.contains("\"ticketStatus\":\"COMPLETED\""));
        assertTrue(json.contains("\"ticketStatusLabel\":\"Hoàn tất xử lý\""));
        assertTrue(json.contains("\"billingStatus\":\"PENDING_PAYMENT\""));
        assertTrue(json.contains("\"billingStatusLabel\":\"Chờ thanh toán\""));
        assertTrue(json.contains("\"paymentStatus\":\"PENDING_PAYMENT\""));
        assertTrue(json.contains("\"chargeToTenant\":true"));
        assertTrue(json.contains("\"payer\":\"TENANT\""));
        assertTrue(json.contains("\"lineType\":\"MAINTENANCE_COMPENSATION\""));
    }
}
