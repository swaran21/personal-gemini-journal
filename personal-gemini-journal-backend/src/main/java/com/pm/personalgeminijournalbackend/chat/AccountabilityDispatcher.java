package com.pm.personalgeminijournalbackend.chat;

import java.time.Instant;

/** Dispatches post-save accountability work without exposing persistence details to the request path. */
public interface AccountabilityDispatcher {
    void dispatch(String uid, String entryId, String entry, Instant createdAt);
}
