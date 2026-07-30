package com.sep490.hdbhms.maintenance.infrastructure.web.controller;

import com.sep490.hdbhms.accounting.domain.value_objects.ExpenseType;
import com.sep490.hdbhms.maintenance.domain.value_objects.CostType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MaintenanceTicketExpenseMappingTest {

    @Test
    void buildsStableMaintenanceExpenseCodes() {
        assertEquals("EXP-MT-42", MaintenanceTicketController.maintenanceExpenseCode(42L, 1));
        assertEquals("EXP-MT-42-2", MaintenanceTicketController.maintenanceExpenseCode(42L, 2));
        assertEquals("DYC-MT-42", MaintenanceTicketController.maintenanceExpenseRequestCode(42L, 1));
    }

    @Test
    void mapsMaintenanceCostsToOperatingExpenseTypes() {
        assertEquals(ExpenseType.REPAIR, MaintenanceTicketController.maintenanceExpenseType(CostType.LABOR));
        assertEquals(ExpenseType.REPAIR, MaintenanceTicketController.maintenanceExpenseType(CostType.MATERIAL));
        assertEquals(ExpenseType.COMMON_UTILITY, MaintenanceTicketController.maintenanceExpenseType(CostType.COMMON_OPERATING));
        assertEquals(ExpenseType.OTHER, MaintenanceTicketController.maintenanceExpenseType(null));
    }
}
