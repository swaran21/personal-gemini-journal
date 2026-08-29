package com.pm.personalgeminijournalbackend.journal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record JournalEntryRequest(@NotBlank @Size(max = 10000) String content) { }
