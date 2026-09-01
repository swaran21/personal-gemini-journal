package com.pm.personalgeminijournalbackend.common;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import java.util.NoSuchElementException;
import com.pm.personalgeminijournalbackend.security.LocationPinRateLimitExceededException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler({MethodArgumentNotValidException.class, IllegalArgumentException.class})
    ProblemDetail badRequest(Exception e) { ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed"); p.setTitle("Invalid request"); return p; }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail malformedBody(HttpMessageNotReadableException e) { ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request body must be valid JSON"); p.setTitle("Invalid request"); return p; }
    @ExceptionHandler(IllegalStateException.class)
    ProblemDetail unavailable(IllegalStateException e) { ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, "A required downstream service is unavailable"); p.setTitle("Service unavailable"); return p; }
    @ExceptionHandler(NoSuchElementException.class)
    ProblemDetail notFound(NoSuchElementException e) { ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "The requested resource was not found"); p.setTitle("Not found"); return p; }
    @ExceptionHandler(LocationPinRateLimitExceededException.class)
    ResponseEntity<ProblemDetail> locationLimit(LocationPinRateLimitExceededException e) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, "Location pin limit reached. You can continue journaling without a location.");
        p.setTitle("Too many location pins");
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).header(HttpHeaders.RETRY_AFTER, Long.toString(e.retryAfterSeconds())).body(p);
    }
    @ExceptionHandler(Exception.class)
    ProblemDetail internalServerError(Exception e) { ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred"); p.setTitle("Internal server error"); return p; }
}
