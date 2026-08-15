package com.streamora.admin.api;

import com.streamora.admin.api.ApiErrorEnvelope.ApiError;
import com.streamora.admin.api.ApiErrorEnvelope.FieldError;
import com.streamora.admin.application.AdminAuthenticationException;
import com.streamora.admin.application.AdminForbiddenException;
import com.streamora.admin.identity.IdentityUnavailableException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps administrator API failures to stable, non-sensitive envelopes. */
@RestControllerAdvice
public class AdminApiExceptionHandler {

    @ExceptionHandler(AdminAuthenticationException.class)
    ResponseEntity<ApiErrorEnvelope> authentication(
            AdminAuthenticationException exception, HttpServletRequest request) {
        return error(HttpStatus.UNAUTHORIZED, exception.code(), exception.getMessage(), List.of(), request);
    }

    @ExceptionHandler(AdminForbiddenException.class)
    ResponseEntity<ApiErrorEnvelope> forbidden(
            AdminForbiddenException exception, HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, exception.code(), exception.getMessage(), List.of(), request);
    }

    @ExceptionHandler(IdentityUnavailableException.class)
    ResponseEntity<ApiErrorEnvelope> identityUnavailable(
            IdentityUnavailableException exception, HttpServletRequest request) {
        return error(
                HttpStatus.SERVICE_UNAVAILABLE,
                "IDENTITY_SERVICE_UNAVAILABLE",
                "身份服务暂时不可用",
                List.of(),
                request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorEnvelope> validation(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
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
        return ResponseEntity.status(status)
                .body(new ApiErrorEnvelope(
                        new ApiError(code, message, details),
                        RequestIds.resolve(request.getHeader("X-Request-Id"))));
    }
}
