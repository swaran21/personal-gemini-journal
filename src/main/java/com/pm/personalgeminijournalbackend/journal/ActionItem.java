package com.pm.personalgeminijournalbackend.journal;
import java.time.Instant;
public record ActionItem(String id, String text, boolean completed, Instant createdAt) { }
