package com.pm.personalgeminijournalbackend.reflection;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record WeeklyReflectionRequest(LocalDate weekStart, @Size(max = 64) String timeZone) { }
