package com.pm.personalgeminijournalbackend.journal;

/** User-approved coarse location attached to an entry. Coordinates are never inferred server-side. */
public record GeoLocation(double latitude, double longitude, String label) {
    public GeoLocation {
        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("latitude must be between -90 and 90");
        }
        if (!Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("longitude must be between -180 and 180");
        }
        label = label == null ? null : label.replace("\u0000", "").trim();
        if (label != null && label.length() > 200) throw new IllegalArgumentException("location label must not exceed 200 characters");
        if (label != null && label.isBlank()) label = null;
    }
}
