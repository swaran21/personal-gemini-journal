package com.pm.personalgeminijournalbackend.security;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RoleAuthoritiesTest {
    @Test void verifiedUsersAlwaysReceiveUserRole() {
        assertEquals(List.of("ROLE_USER"), RoleAuthorities.from(null, null).stream().map(Object::toString).toList());
    }

    @Test void trustedAdminClaimAddsAdminWithoutRemovingUserRole() {
        assertEquals(List.of("ROLE_USER", "ROLE_ADMIN"), RoleAuthorities.from(Map.of("roles", List.of("journal-admin")), null).stream().map(Object::toString).toList());
    }

    @Test void arbitraryClaimsDoNotBecomeAuthorities() {
        assertEquals(List.of("ROLE_USER"), RoleAuthorities.from(Map.of("roles", List.of("billing-owner")), List.of("unknown")).stream().map(Object::toString).toList());
    }
}
