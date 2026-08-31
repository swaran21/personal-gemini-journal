package com.pm.personalgeminijournalbackend.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalSecurityConfigTest {
    @Test
    void acceptsOnlyTheConfiguredAudience() {
        assertFalse(LocalSecurityConfig.audienceValidator("journal-web").validate(jwt("user-1", List.of("journal-web"))).hasErrors());
        assertTrue(LocalSecurityConfig.audienceValidator("journal-web").validate(jwt("user-1", List.of("another-client"))).hasErrors());
    }

    @Test
    void rejectsBlankOversizedAndPathLikeSubjects() {
        assertFalse(LocalSecurityConfig.subjectValidator().validate(jwt("6d9104a2-bbdd-45f0-b8d2-20f2a6c0a912", List.of("journal-web"))).hasErrors());
        for (String invalid : List.of("../other-user", "contains space", "a".repeat(129))) {
            assertTrue(LocalSecurityConfig.subjectValidator().validate(jwt(invalid, List.of("journal-web"))).hasErrors());
        }
    }

    private Jwt jwt(String subject, List<String> audience) {
        Instant now = Instant.now();
        return Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .subject(subject)
                .audience(audience)
                .issuedAt(now.minusSeconds(5))
                .expiresAt(now.plusSeconds(60))
                .build();
    }
}
