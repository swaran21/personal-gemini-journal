package com.pm.personalgeminijournalbackend.journal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateActionItemRequest(@NotBlank @Size(max = 1000) String goal) { }
