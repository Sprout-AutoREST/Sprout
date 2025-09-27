package de.flix29.sprout.runtime.error;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final SproutErrorProperties props;

    public GlobalExceptionHandler(SproutErrorProperties props) {
        this.props = props;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> onInvalid(MethodArgumentNotValidException ex, HttpServletRequest req) {
        var errors = ex.getBindingResult().getFieldErrors().stream().map(FieldErrorDetail::from).toList();
        return build(
                req,
                HttpStatus.BAD_REQUEST,
                "validation_failed",
                "Request body validation failed",
                errors,
                ex,
                false
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> onConstraint(ConstraintViolationException ex, HttpServletRequest req) {
        var errors = ex.getConstraintViolations().stream().map(FieldErrorDetail::from).toList();
        return build(
                req,
                HttpStatus.BAD_REQUEST,
                "constraint_violation",
                "Constraint validation failed",
                errors,
                ex,
                false
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> onNotReadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return build(
                req,
                HttpStatus.BAD_REQUEST,
                "malformed_json",
                "Malformed JSON request body",
                null,
                ex,
                false
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> onTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        String msg = "Parameter '%s' requires type '%s'".formatted(ex.getName(), ex.getRequiredType() != null ?
                ex.getRequiredType().getSimpleName() : "unknown");
        return build(
                req,
                HttpStatus.BAD_REQUEST,
                "type_mismatch",
                msg,
                null,
                ex,
                false
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> onMissingParam(MissingServletRequestParameterException ex, HttpServletRequest req) {
        return build(
                req,
                HttpStatus.BAD_REQUEST,
                "missing_parameter",
                "Missing required parameter '%s'".formatted(ex.getParameterName()),
                null,
                ex,
                false
        );
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> onUnsupported(HttpMediaTypeNotSupportedException ex, HttpServletRequest req) {
        return build(
                req,
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "unsupported_media_type",
                "Unsupported Content-Type",
                null,
                ex,
                false
        );
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ApiError> onNotAcceptable(HttpMediaTypeNotAcceptableException ex, HttpServletRequest req) {
        return build(
                req,
                HttpStatus.NOT_ACCEPTABLE,
                "not_acceptable",
                "Requested media type not acceptable",
                null,
                ex,
                false
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> onMethodNotAllowed(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest req
    ) {
        return build(
                req,
                HttpStatus.METHOD_NOT_ALLOWED,
                "method_not_allowed",
                "HTTP method not allowed",
                null,
                ex,
                false
        );
    }

    @ExceptionHandler({NoSuchElementException.class, EntityNotFoundException.class})
    public ResponseEntity<ApiError> onNotFound(RuntimeException ex, HttpServletRequest req) {
        return build(
                req,
                HttpStatus.NOT_FOUND,
                "not_found",
                "Resource not found",
                null,
                ex,
                false
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> onIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        return build(
                req,
                HttpStatus.CONFLICT,
                "data_integrity_violation",
                "Data integrity violation",
                null,
                ex,
                false
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> onUnhandled(Exception ex, HttpServletRequest req) {
        return build(
                req,
                HttpStatus.INTERNAL_SERVER_ERROR,
                props.getInternalErrorCode(),
                "Unexpected server error",
                null,
                ex,
                true
        );
    }

    private ResponseEntity<ApiError> build(
            HttpServletRequest httpServletRequest,
            HttpStatus status,
            String code,
            String message,
            List<FieldErrorDetail> fieldErrors,
            Exception exception,
            boolean logAsError
    ) {
        var body = ApiError.of(
                httpServletRequest.getRequestURI(), status.value(), status.getReasonPhrase(), code, message, fieldErrors
        );
        if (logAsError || status.is5xxServerError()) {
            if (props.isLogStacktraces()) {
                log.error("{} {} -> {} {} (code={})", httpServletRequest.getMethod(),
                        httpServletRequest.getRequestURI(), status.value(),
                        status.getReasonPhrase(), code, exception);
            } else {
                log.error("{} {} -> {} {} (code={}) : {}", httpServletRequest.getMethod(),
                        httpServletRequest.getRequestURI(), status.value(),
                        status.getReasonPhrase(), code, exception.getMessage());
            }
        } else {
            if (props.isLogStacktraces()) {
                log.warn("{} {} -> {} {} (code={})", httpServletRequest.getMethod(),
                        httpServletRequest.getRequestURI(), status.value(),
                        status.getReasonPhrase(), code, exception
                );
            } else {
                log.warn("{} {} -> {} {} (code={}) : {}", httpServletRequest.getMethod(),
                        httpServletRequest.getRequestURI(), status.value(),
                        status.getReasonPhrase(), code, exception.getMessage()
                );
            }
        }
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(body);
    }
}