package com.ebike.shared.exception;

import java.time.OffsetDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(org.springframework.web.bind.MethodArgumentNotValidException exception) {
        return ResponseEntity.badRequest().body(new ApiErrorResponse(400,"Bad Request","Thông tin không hợp lệ. Vui lòng kiểm tra định dạng và độ dài các trường.",OffsetDateTime.now().toString()));
    }

    @ExceptionHandler({org.springframework.dao.DataIntegrityViolationException.class, org.springframework.dao.OptimisticLockingFailureException.class})
    public ResponseEntity<ApiErrorResponse> handleConflict(Exception exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiErrorResponse(409,"Conflict","Dữ liệu bị trùng hoặc đã thay đổi. Vui lòng tải lại và kiểm tra thông tin.",OffsetDateTime.now().toString()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatusException(ResponseStatusException exception) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        String message = exception.getReason() == null || exception.getReason().isBlank()
            ? status.getReasonPhrase()
            : exception.getReason();

        return ResponseEntity.status(status)
            .body(new ApiErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                message,
                OffsetDateTime.now().toString()
            ));
    }

    public record ApiErrorResponse(
        int status,
        String error,
        String message,
        String timestamp
    ) {
    }
}
