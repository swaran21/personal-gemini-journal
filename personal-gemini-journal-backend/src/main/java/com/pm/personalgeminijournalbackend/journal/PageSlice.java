package com.pm.personalgeminijournalbackend.journal;

import java.util.List;

/** Cursor page. Cursors are opaque to callers and contain no user identity. */
public record PageSlice<T>(List<T> items, String nextCursor, boolean hasMore) {
    public PageSlice {
        items = List.copyOf(items);
        if (!hasMore) nextCursor = null;
    }
}
