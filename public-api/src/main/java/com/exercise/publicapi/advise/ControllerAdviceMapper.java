package com.exercise.publicapi.advise;

import com.exercise.publicapi.dto.ErrorDto;
import com.exercise.publicapi.exception.DownstreamServiceException;
import com.exercise.publicapi.exception.InvalidRequestException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

/**
 * Turns every error into the standard {"result": false, "message": "..."} body.
 */
@RestControllerAdvice
public class ControllerAdviceMapper {

    private static final Logger log = LoggerFactory.getLogger(ControllerAdviceMapper.class);

    // Invalid JSON body fields, e.g. a missing price on POST /public-api/listings.
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorDto> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining("; "));

        return error(HttpStatus.BAD_REQUEST, message);
    }

    // Invalid query parameters, e.g. pageNum=0.
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorDto> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(violation -> parameterName(violation) + ": " + violation.getMessage())
                .sorted()
                .collect(Collectors.joining("; "));

        return error(HttpStatus.BAD_REQUEST, message);
    }

    // Query parameters of the wrong type, e.g. pageNum=abc.
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorDto> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return error(HttpStatus.BAD_REQUEST, e.getName() + ": invalid value '" + e.getValue() + "'");
    }

    // Missing body, broken JSON, or a value of the wrong type such as "price": "abc".
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorDto> handleUnreadableBody(HttpMessageNotReadableException e) {
        return error(HttpStatus.BAD_REQUEST, "request body is missing or is not valid JSON");
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ErrorDto> handleInvalidRequest(InvalidRequestException e) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(DownstreamServiceException.class)
    public ResponseEntity<ErrorDto> handleDownstreamService(DownstreamServiceException e) {
        log.warn("Downstream call failed: {}", e.getMessage(), e.getCause());
        return error(e.getStatus(), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDto> handleOtherException(Exception e) {
        // Spring's own web errors (unsupported method, missing route, ...) already know their status.
        if (e instanceof ErrorResponse errorResponse) {
            HttpStatusCode status = errorResponse.getStatusCode();
            return error(status, errorResponse.getBody().getDetail());
        }

        log.error("Unexpected error", e);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "internal server error");
    }

    private ResponseEntity<ErrorDto> error(HttpStatusCode status, String message) {
        return new ResponseEntity<>(new ErrorDto(false, message), status);
    }

    // "getListings.pageNum" -> "pageNum"
    private String parameterName(ConstraintViolation<?> violation) {
        String name = null;
        for (Path.Node node : violation.getPropertyPath()) {
            name = node.getName();
        }
        return name;
    }
}