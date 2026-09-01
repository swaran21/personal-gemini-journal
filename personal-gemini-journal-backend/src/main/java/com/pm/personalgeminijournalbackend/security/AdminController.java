package com.pm.personalgeminijournalbackend.security;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * Privacy-preserving operational information for verified administrators.
 *
 * <p>This controller is intentionally not an administration API for journal content:
 * an administrator never gains the ability to enumerate, read, modify, export, or delete
 * another person's private data. Endpoint authorization is enforced by the security filter
 * chain before this controller runs.</p>
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    @GetMapping("/status")
    public AdminStatus status() { return new AdminStatus("ADMIN_ACCESS_VERIFIED", Instant.now()); }

    @GetMapping("/dashboard")
    public AdminDashboard dashboard() {
        return new AdminDashboard(
                "ADMIN",
                "Your elevated role can view security-control status, but never other users' private journal data.",
                List.of(
                        new Control("Verified identity", "ENFORCED", "Roles come only from verified identity-provider claims."),
                        new Control("Tenant isolation", "ENFORCED", "UID-scoped repositories and database RLS remain in effect for every role."),
                        new Control("AI cost controls", "ENFORCED", "Per-user quotas protect journal writes and AI-intensive requests."),
                        new Control("Location privacy", "ENFORCED", "Location is opt-in and current map links use no Maps API key.")),
                Instant.now());
    }

    public record AdminStatus(String status, Instant checkedAt) { }
    public record AdminDashboard(String role, String privacyBoundary, List<Control> controls, Instant checkedAt) { }
    public record Control(String name, String status, String detail) { }
}
