package com.streamora.media.api;

import com.streamora.media.application.MediaUploadException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class MediaApiExceptionHandler {
    @ExceptionHandler(MediaUploadException.class)
    ResponseEntity<Object> upload(MediaUploadException exception, HttpServletRequest request) {
        HttpStatus status = exception.code().endsWith("NOT_FOUND") ? HttpStatus.NOT_FOUND
                : exception.code().contains("MISMATCH") ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST;
        if ("USER_SESSION_REQUIRED".equals(exception.code())) status = HttpStatus.UNAUTHORIZED;
        return ResponseEntity.status(status).body(new ErrorEnvelope(new ErrorBody(exception.code(), exception.getMessage(), List.of()), RequestIds.resolve(request.getHeader("X-Request-Id"))));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Object> validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        var details = exception.getBindingResult().getFieldErrors().stream().map(error -> new FieldError(error.getField(), error.getDefaultMessage())).toList();
        return ResponseEntity.badRequest().body(new ErrorEnvelope(new ErrorBody("VALIDATION_FAILED", "请求参数不符合要求", details), RequestIds.resolve(request.getHeader("X-Request-Id"))));
    }

    record ErrorEnvelope(ErrorBody error, String requestId) { }
    record ErrorBody(String code, String message, List<?> details) { }
    record FieldError(String field, String reason) { }
}
