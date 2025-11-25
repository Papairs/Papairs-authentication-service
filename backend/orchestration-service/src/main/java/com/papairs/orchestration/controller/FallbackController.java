package com.papairs.orchestration.controller;

import com.papairs.orchestration.dto.response.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;

@RestController
@RequestMapping(value = "/fallback", method = {RequestMethod.GET, RequestMethod.POST})
public class FallbackController {

    /**
     * Provides a fallback response for requests to the Authentication Service
     * <p>
     * This method is triggered when the circuit breaker for the Auth Service is open
     * </p>
     * @param exchange The current server exchange, used to extract details like the original request path.
     * @return A {@link ResponseEntity} containing a standardized {@link ErrorResponse}
     * with an HTTP 503 Service Unavailable status
     */
    @RequestMapping("/auth-service")
    public ResponseEntity<ErrorResponse> authServiceFallback(ServerWebExchange exchange) {
        return createFallbackResponse("Authentication service", exchange);
    }

    /**
     * Provides a fallback response for requests to the Documents Service
     * <p>
     * This method is triggered when the circuit breaker for the Docs Service is open
     * </p>
     * @param exchange The current server exchange, used to extract details like the original request path.
     * @return A {@link ResponseEntity} containing a standardized {@link ErrorResponse}
     * with an HTTP 503 Service Unavailable status.
     */
    @RequestMapping("/docs-service")
    public ResponseEntity<ErrorResponse> docsServiceFallback(ServerWebExchange exchange) {
        return createFallbackResponse("Documents service", exchange);
    }

    /**
     * Private helper method to construct a standardized fallback response
     * @param serviceName The friendly name of the service that is unavailable
     * @param exchange The current server exchange from which the original request path is obtained
     * @return A {@link ResponseEntity} containing the formatted {@link ErrorResponse} and a 503 status code
     */
    private ResponseEntity<ErrorResponse> createFallbackResponse(
            String serviceName, ServerWebExchange exchange) {
        ErrorResponse error = ErrorResponse.of(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "Service Unavailable",
                serviceName + " is currently unavailable. Please try again later",
                exchange.getRequest().getPath().value()
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }
}