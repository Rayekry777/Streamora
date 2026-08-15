package com.streamora.identity.api;

import com.streamora.identity.api.ApiErrorEnvelope.ApiError;
import com.streamora.identity.api.ApiErrorEnvelope.FieldError;
import com.streamora.identity.application.IdentityAuthenticationException;
import com.streamora.identity.application.IdentityConflictException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps identity failures to stable, non-sensitive API error envelopes. */
@RestControllerAdvice
public class IdentityApiExceptionHandler {

    @ExceptionHandler(IdentityAuthenticationException.class)
    ResponseEntity<ApiErrorEnvelope> authentication(
            IdentityAuthenticationException exception,
            HttpServletRequest request) {
        return error(HttpStatus.UNAUTHORIZED, exception.code(), exception.getMessage(), List.of(), request);
    }

    @ExceptionHandler(IdentityForbiddenException.class)
    ResponseEntity<ApiErrorEnvelope> forbidden(
            IdentityForbiddenException exception,
            HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, exception.code(), exception.getMessage(), List.of(), request);
    }

    @ExceptionHandler(IdentityConflictException.class)
    ResponseEntity<ApiErrorEnvelope> conflict(
            IdentityConflictException exception,
            HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, exception.code(), exception.getMessage(), List.of(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorEnvelope> validation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        List<FieldError> details = exception.getBindingResult().getFieldErrors().stream()
                .map(field -> new FieldError(field.getField(), field.getDefaultMessage()))
                .toList();
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "请求参数不符合要求", details, request);
    }

    private static ResponseEntity<ApiErrorEnvelope> error(
            HttpStatus status,
            String code,
            String message,
            List<FieldError> details,
            HttpServletRequest request) {
        String requestId = RequestIds.resolve(request.getHeader("X-Request-Id"));
        return ResponseEntity.status(status)
                .body(new ApiErrorEnvelope(new ApiError(code, message, details), requestId));
    }
}
