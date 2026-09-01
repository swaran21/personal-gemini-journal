package com.pm.personalgeminijournalbackend.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.*;

/** Maps trusted identity-provider claims to application roles. Every verified user receives ROLE_USER. */
final class RoleAuthorities {
    private RoleAuthorities() { }

    static List<SimpleGrantedAuthority> from(Object realmAccess, Object directRoles) {
        Set<String> roles = new HashSet<>();
        collect(directRoles, roles);
        if (realmAccess instanceof Map<?, ?> map) collect(map.get("roles"), roles);
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        if (roles.stream().anyMatch(role -> role.equalsIgnoreCase("journal-admin") || role.equalsIgnoreCase("admin"))) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        return List.copyOf(authorities);
    }

    private static void collect(Object value, Set<String> target) {
        if (value instanceof Collection<?> values) values.stream().filter(String.class::isInstance).map(String.class::cast).forEach(target::add);
        else if (value instanceof String role) target.add(role);
    }
}
