package com.sep490.hdbhms.maintenance.infrastructure.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.sep490.hdbhms.maintenance.infrastructure.web.dto.response.MaintenanceTicketResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MaintenanceTicketBillingResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

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

        assertTrue(json.contains("\"ticket_status\":\"COMPLETED\""));
        assertTrue(json.contains("\"ticket_status_label\":\"Hoàn tất xử lý\""));
        assertTrue(json.contains("\"billing_status\":\"PENDING_PAYMENT\""));
        assertTrue(json.contains("\"billing_status_label\":\"Chờ thanh toán\""));
        assertTrue(json.contains("\"payment_status\":\"PENDING_PAYMENT\""));
        assertTrue(json.contains("\"charge_to_tenant\":true"));
        assertTrue(json.contains("\"payer\":\"TENANT\""));
        assertTrue(json.contains("\"line_type\":\"MAINTENANCE_COMPENSATION\""));
    }
}
