package com.pm.personalgeminijournalbackend.journal;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

final class PageCursor {
    private PageCursor() { }

    static String encode(Instant createdAt, String id) {
        String value = createdAt.toEpochMilli() + ":" + id;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    static Decoded decode(String cursor) {
        if (cursor == null || cursor.isBlank()) return null;
        if (cursor.length() > 512) throw new IllegalArgumentException("Invalid pagination cursor");
        try {
            String value = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            int separator = value.indexOf(':');
            if (separator < 1) throw new IllegalArgumentException();
            return new Decoded(Instant.ofEpochMilli(Long.parseLong(value.substring(0, separator))), value.substring(separator + 1));
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Invalid pagination cursor");
        }
    }

    record Decoded(Instant createdAt, String id) { }
}
