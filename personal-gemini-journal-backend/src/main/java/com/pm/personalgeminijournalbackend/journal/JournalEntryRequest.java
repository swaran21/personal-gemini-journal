package com.pm.personalgeminijournalbackend.journal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record JournalEntryRequest(
        @NotBlank @Size(max = 10000) String content,
        @Valid LocationRequest location) {
    public record LocationRequest(
            @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
            @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
            @Size(max = 200) String label) {
        public GeoLocation toLocation() { return new GeoLocation(latitude, longitude, label); }
    }
}
