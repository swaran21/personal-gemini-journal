package com.pm.personalgeminijournalbackend.common;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler({MethodArgumentNotValidException.class, IllegalArgumentException.class})
    ProblemDetail badRequest(Exception e) { ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed"); p.setTitle("Invalid request"); return p; }
    @ExceptionHandler(IllegalStateException.class)
    ProblemDetail unavailable(IllegalStateException e) { ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, "A required downstream service is unavailable"); p.setTitle("Service unavailable"); return p; }
}
