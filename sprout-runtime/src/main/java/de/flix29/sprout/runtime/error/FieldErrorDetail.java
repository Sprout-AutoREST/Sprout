package de.flix29.sprout.runtime.error;

import jakarta.validation.ConstraintViolation;
import org.springframework.validation.FieldError;

public record FieldErrorDetail(String field, String code, String message, Object rejectedValue) {
    public static FieldErrorDetail from(FieldError fieldError) {
        return new FieldErrorDetail(
                fieldError.getField(),
                fieldError.getCode(),
                fieldError.getDefaultMessage(),
                clip(fieldError.getRejectedValue())
        );
    }

    public static FieldErrorDetail from(ConstraintViolation<?> constraintViolation) {
        String field = constraintViolation.getPropertyPath() != null ?
                constraintViolation.getPropertyPath().toString() : null;
        String code = (constraintViolation.getConstraintDescriptor() != null &&
                constraintViolation.getConstraintDescriptor().getAnnotation() != null)
                ? constraintViolation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName() : null;
        return new FieldErrorDetail(
                field,
                code,
                constraintViolation.getMessage(),
                clip(constraintViolation.getInvalidValue())
        );
    }

    private static Object clip(Object v) {
        if (v == null) {
            return null;
        }

        String stringValue = String.valueOf(v);
        return stringValue.length() > 120 ? stringValue.substring(0, 117) + "..." : stringValue;
    }
}