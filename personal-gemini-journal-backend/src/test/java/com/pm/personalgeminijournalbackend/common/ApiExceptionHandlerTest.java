package com.pm.personalgeminijournalbackend.common;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import com.pm.personalgeminijournalbackend.security.LocationPinRateLimitExceededException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiExceptionHandlerTest {
    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test void mapsIllegalArgumentToSanitizedBadRequest() {
        assertEquals(HttpStatus.BAD_REQUEST.value(), handler.badRequest(new IllegalArgumentException("secret detail")).getStatus());
    }

    @Test void mapsMalformedBodyToBadRequest() {
        assertEquals(HttpStatus.BAD_REQUEST.value(), handler.malformedBody(new HttpMessageNotReadableException("bad json")).getStatus());
    }

    @Test void mapsDependencyFailureToServiceUnavailable() {
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE.value(), handler.unavailable(new IllegalStateException("downstream detail")).getStatus());
    }

    @Test void mapsUnexpectedFailureToSanitizedServerError() {
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), handler.internalServerError(new RuntimeException("internal detail")).getStatus());
    }

    @Test void mapsLocationQuotaToRetryableTooManyRequests() {
        var response = handler.locationLimit(new LocationPinRateLimitExceededException(60));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS.value(), response.getStatusCode().value());
        assertEquals("60", response.getHeaders().getFirst("Retry-After"));
    }
}
