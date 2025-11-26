package com.papairs.orchestration.exception;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.papairs.orchestration.dto.response.ErrorResponse;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles errors that occur when a downstream service, called via {@link org.springframework.web.reactive.function.client.WebClient},
     * returns a 4xx or 5xx HTTP status
     * <p>
     * It attempts to parse the error message from the downstream service's response body
     * to provide a more detailed and meaningful error to the client. The original HTTP status
     * code from the downstream service is preserved in the response
     * </p>
     * @param ex       The exception containing the details of the downstream service's error response
     * @param exchange The current server exchange, used to get the original request path
     * @return A {@link Mono} that emits a {@link ResponseEntity} containing the standardized {@link ErrorResponse}
     */
    @ExceptionHandler(WebClientResponseException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleDownstreamServiceError(
            WebClientResponseException ex, ServerWebExchange exchange) {

        String message = extractAuthResponseMessage(ex);

        ErrorResponse error = ErrorResponse.of(
                ex.getStatusCode().value(),
                ex.getStatusText(),
                message,
                exchange.getRequest().getPath().value()
        );

        return Mono.just(ResponseEntity
                .status(ex.getStatusCode())
                .body(error));
    }

    /**
     * Handles a custom {@link ServiceUnavailableException}, typically thrown when the
     * application logic determines that a required service is not available
     * @param ex The custom exception indicating service unavailability
     * @param exchange The current server exchange
     * @return A {@link Mono} that emits a {@link ResponseEntity} with an HTTP 503 Service Unavailable status
     */
    @ExceptionHandler(ServiceUnavailableException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleServiceUnavailable(
            ServiceUnavailableException ex, ServerWebExchange exchange) {

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "Service Unavailable",
                ex.getMessage(),
                exchange.getRequest().getPath().value()
        );

        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(error));
    }

    /**
     * Handles {@link CallNotPermittedException} from Resilience4j, which is thrown
     * when a circuit breaker is open and preventing calls to a failing service
     * @param ex The exception thrown by the circuit breaker
     * @param exchange The current server exchange
     * @return A {@link Mono} that emits a {@link ResponseEntity} with a generic service unavailable
     * message and an HTTP 503 status.
     */
    @ExceptionHandler(CallNotPermittedException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleCircuitBreakerOpen(
            CallNotPermittedException ex, ServerWebExchange exchange) {

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "Service Unavailable",
                "Service is temporarily unavailable due to high failure rate. Please try again later",
                exchange.getRequest().getPath().value()
        );

        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(error));
    }

    /**
     * Handles {@link WebExchangeBindException}, which occurs when request body validation
     * (e.g., from {@code jakarta.validation} annotations) fails
     * <p>
     * It aggregates all validation failures into a single, comma-separated string to
     * provide clear feedback to the client
     * </p>
     * @param ex The exception containing the details of the validation binding errors
     * @param exchange The current server exchange
     * @return A {@link Mono} that emits a {@link ResponseEntity} with an HTTP 400 Bad Request status
     */
    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleValidationErrors(
            WebExchangeBindException ex, ServerWebExchange exchange) {

        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "Validation Failed",
                errors,
                exchange.getRequest().getPath().value()
        );

        return Mono.just(ResponseEntity.badRequest().body(error));
    }

    /**
     * A private helper method to safely extract a meaningful error message from the
     * body of a {@link WebClientResponseException}.
     * <p>
     * It attempts to parse the response body as JSON and extract the value of the "message" field.
     * If parsing fails or the field is not present, it gracefully falls back to the generic
     * HTTP status text (e.g., "Bad Request")
     * </p>
     * @param ex The exception containing the response body from the downstream service
     * @return The specific error message from the JSON body, or the HTTP status text as a fallback
     */
    private String extractAuthResponseMessage(WebClientResponseException ex) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.readTree(ex.getResponseBodyAsString());
            return json.has("message")
                    ? json.get("message").asText()
                    : ex.getStatusText();
        } catch (Exception e) {
            return ex.getStatusText();
        }
    }
}
