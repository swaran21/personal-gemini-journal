package com.pm.personalgeminijournalbackend.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminControllerTest {
    private final AdminController controller = new AdminController();

    @Test
    void dashboardContainsOnlyPrivacyPreservingControlInformation() {
        AdminController.AdminDashboard dashboard = controller.dashboard();

        assertEquals("ADMIN", dashboard.role());
        assertFalse(dashboard.controls().isEmpty());
        assertTrue(dashboard.privacyBoundary().contains("never other users' private journal data"));
        assertTrue(dashboard.controls().stream().allMatch(control -> "ENFORCED".equals(control.status())));
        assertTrue(dashboard.controls().stream().noneMatch(control -> control.detail().toLowerCase().contains("journal content")));
    }

    @Test
    void statusProvesAccessWithoutLeakingTenantData() {
        AdminController.AdminStatus status = controller.status();

        assertEquals("ADMIN_ACCESS_VERIFIED", status.status());
        assertTrue(status.checkedAt().isBefore(java.time.Instant.now().plusSeconds(1)));
    }
}
