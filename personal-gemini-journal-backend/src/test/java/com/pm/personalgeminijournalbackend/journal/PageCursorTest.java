package com.pm.personalgeminijournalbackend.journal;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class PageCursorTest {
    @Test void roundTripsOpaqueCursorWithoutUserIdentity() {
        Instant instant = Instant.parse("2026-09-01T10:00:00Z");
        String cursor = PageCursor.encode(instant, "entry-id");
        PageCursor.Decoded decoded = PageCursor.decode(cursor);
        assertEquals(instant, decoded.createdAt()); assertEquals("entry-id", decoded.id()); assertFalse(cursor.contains("entry-id"));
    }
    @Test void rejectsMalformedOrEmptyCursorSafely() {
        assertNull(PageCursor.decode(null)); assertNull(PageCursor.decode(" "));
        assertThrows(IllegalArgumentException.class, () -> PageCursor.decode("not-a-valid-cursor"));
        assertThrows(IllegalArgumentException.class, () -> PageCursor.decode("a".repeat(513)));
    }

    @Test void validatesLocationCoordinateBoundsAndSanitizesLabel() {
        GeoLocation location = new GeoLocation(12.9, 77.6, "  Library\u0000 ");
        assertEquals("Library", location.label());
        assertThrows(IllegalArgumentException.class, () -> new GeoLocation(91, 0, null));
        assertThrows(IllegalArgumentException.class, () -> new GeoLocation(0, -181, null));
        assertThrows(IllegalArgumentException.class, () -> new GeoLocation(Double.NaN, 0, null));
    }
}
