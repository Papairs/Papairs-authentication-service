package com.papairs.orchestration.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@Configuration
public class DownstreamHealthConfig {

    private final WebClient webClient;

    @Value("${services.auth-service.url}")
    private String authServiceUrl;

    @Value("${services.docs-service.url}")
    private String docsServiceUrl;

    public DownstreamHealthConfig(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    @Bean("authService")
    public ReactiveHealthIndicator authServiceHealth() {
        return () -> checkService(authServiceUrl + "/actuator/health");
    }

    @Bean("docsService")
    public ReactiveHealthIndicator docsServiceHealth() {
        return () -> checkService(docsServiceUrl + "/actuator/health");
    }

    private Mono<Health> checkService(String url) {
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(5))
                .map(response -> {
                    String status = (String) response.getOrDefault("status", "UNKNOWN");
                    if ("UP".equalsIgnoreCase(status)) {
                        return Health.up().withDetail("upstream-details", response).build();
                    } else {
                        return Health.down().withDetail("upstream-details", response).build();
                    }
                })
                .onErrorResume(ex -> Mono.just(Health.down()
                        .withException(ex)
                        .withDetail("error", "Service unreachable: " + ex.getMessage())
                        .build()));
    }
}
