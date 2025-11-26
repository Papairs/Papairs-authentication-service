package com.papairs.orchestration.filter;

import com.papairs.orchestration.exception.ServiceUnavailableException;
import com.papairs.orchestration.service.TokenValidationService;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class AuthenticationGatewayFilter implements GatewayFilter {

    private final TokenValidationService tokenValidationService;

    public AuthenticationGatewayFilter(TokenValidationService tokenValidationService) {
        this.tokenValidationService = tokenValidationService;
    }

    /**
     * The core logic of the authentication filter
     * <p>
     * It performs the following steps:
     * <ol>
     *     <li>Extracts the "Authorization" header from the request</li>
     *     <li>Validates that the header exists and starts with "Bearer "</li>
     *     <li>If validation fails, it immediately returns a 401 Unauthorized response</li>
     *     <li>If successful, it calls the {@link TokenValidationService} to validate the token</li>
     *     <li>On successful token validation, it adds the user ID to the request headers as "X-User-Id"
     *         and passes the request down the filter chain</li>
     *     <li>If the token validation service is unavailable, it returns a 503 Service Unavailable response</li>
     * </ol>
     * @param exchange The current server web exchange, containing the request and response
     * @param chain The filter chain, used to pass the request to the next filter
     * @return A {@link Mono<Void>} that indicates the completion of the filtering logic
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorizedResponse(exchange);
        }

        String token = authHeader.substring(7);

        return tokenValidationService.validateTokenAndGetUserId(token)
                .flatMap(userId -> {
                    ServerWebExchange modifiedExchange = exchange.mutate()
                            .request(builder -> builder
                                    .headers(headers -> headers.remove("X-User-Id"))
                                    .header("X-User-Id", userId))
                            .build();
                    return chain.filter(modifiedExchange);
                })
                .onErrorResume(ServiceUnavailableException.class,
                        error -> serviceUnavailableResponse(exchange))
                .onErrorResume(error -> unauthorizedResponse(exchange));
    }

    /**
     * Helper method to terminate the request and send an HTTP 401 Unauthorized response
     * @param exchange The current server web exchange
     * @return A {@link Mono<Void>} that completes the response
     */
    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    /**
     * Helper method to terminate the request and send an HTTP 503 Service Unavailable response
     * This is typically used when the token validation service cannot be reached
     * @param exchange The current server web exchange
     * @return A {@link Mono<Void>} that completes the response
     */
    private Mono<Void> serviceUnavailableResponse(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
        return exchange.getResponse().setComplete();
    }
}