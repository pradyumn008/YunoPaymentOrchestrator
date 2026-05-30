package com.yuno.payment.orchestrator.exception;

import com.yuno.payment.orchestrator.api.model.ErrorResponse;
import com.yuno.payment.orchestrator.idempotency.DuplicateRequestIdException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;


@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ErrorResponse> handleBodyValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
		Map<String, String> violations = new LinkedHashMap<>();
		ex.getBindingResult().getFieldErrors()
				.forEach(error -> violations.put(error.getField(), error.getDefaultMessage()));
		return build(HttpStatus.BAD_REQUEST, "Validation failed", request, violations);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
		Map<String, String> violations = new LinkedHashMap<>();
		ex.getConstraintViolations()
				.forEach(v -> violations.put(v.getPropertyPath().toString(), v.getMessage()));
		return build(HttpStatus.BAD_REQUEST, "Validation failed", request, violations);
	}

	@ExceptionHandler(MissingRequestHeaderException.class)
	ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException ex, HttpServletRequest request) {
		return build(HttpStatus.BAD_REQUEST, "Missing required header: " + ex.getHeaderName(), request, Map.of());
	}

	/**
	 * Maps invalid UUID path variables (e.g. {@code /api/v1/payments/not-a-uuid})
	 * to 404 so that the error message is consistent with a missing payment ID.
	 */
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
		if (UUID.class.equals(ex.getRequiredType())) {
			String rawValue = ex.getValue() instanceof String s ? s : String.valueOf(ex.getValue());
			return build(HttpStatus.NOT_FOUND, "Payment not found: " + rawValue, request, Map.of());
		}
		return build(HttpStatus.BAD_REQUEST, "Invalid value for parameter '" + ex.getName() + "'", request, Map.of());
	}

	/**
	 * Returns 409 Conflict when the same {@code Request-Id} is reused.
	 * The message is always {@code "same requestId"}.
	 */
	@ExceptionHandler(DuplicateRequestIdException.class)
	ResponseEntity<ErrorResponse> handleDuplicateRequestId(DuplicateRequestIdException ex, HttpServletRequest request) {
		return build(HttpStatus.CONFLICT, ex.getMessage(), request, Map.of());
	}

	@ExceptionHandler(PaymentNotFoundException.class)
	ResponseEntity<ErrorResponse> handlePaymentNotFound(PaymentNotFoundException ex, HttpServletRequest request) {
		return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, Map.of());
	}

	private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, HttpServletRequest request,
			Map<String, String> violations) {
		return ResponseEntity.status(status).body(new ErrorResponse(
				OffsetDateTime.now(ZoneOffset.UTC),
				status.value(),
				status.getReasonPhrase(),
				message,
				request.getRequestURI(),
				violations
		));
	}
}
