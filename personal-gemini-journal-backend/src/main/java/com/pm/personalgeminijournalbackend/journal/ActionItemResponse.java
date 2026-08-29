package com.pm.personalgeminijournalbackend.journal;
import java.time.Instant;
public record ActionItemResponse(String id, String goal, String status, Instant createdAt) { }
