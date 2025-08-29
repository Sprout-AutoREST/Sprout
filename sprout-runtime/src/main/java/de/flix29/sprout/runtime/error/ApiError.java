package de.flix29.sprout.runtime.error;

import java.time.OffsetDateTime;
import java.util.List;

public record ApiError(
        OffsetDateTime timestamp,
        String path,
        int status,
        String error,
        String code,
        String message,
        List<FieldErrorDetail> errors
) {
    public static ApiError of(
            String path, int status, String error, String code, String message, List<FieldErrorDetail> errors
    ) {
        return new ApiError(OffsetDateTime.now(), path, status, error, code, message, errors);
    }
}