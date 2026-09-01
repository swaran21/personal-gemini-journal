package com.pm.personalgeminijournalbackend.security;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/** A deliberately non-sensitive RBAC proof endpoint. It never aggregates or exposes user journal data. */
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    @GetMapping("/status")
    public AdminStatus status() { return new AdminStatus("ADMIN_ACCESS_VERIFIED", Instant.now()); }
    public record AdminStatus(String status, Instant checkedAt) { }
}
