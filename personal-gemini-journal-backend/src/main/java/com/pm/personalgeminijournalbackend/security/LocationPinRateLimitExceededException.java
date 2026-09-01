package com.pm.personalgeminijournalbackend.security;

/** Raised without location details when an authenticated user exceeds the optional pin quota. */
public class LocationPinRateLimitExceededException extends RuntimeException {
    private final long retryAfterSeconds;
    public LocationPinRateLimitExceededException(long retryAfterSeconds) { this.retryAfterSeconds = retryAfterSeconds; }
    public long retryAfterSeconds() { return retryAfterSeconds; }
}
