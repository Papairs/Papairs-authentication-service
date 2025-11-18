package com.papairs.orchestration.client;

import com.papairs.orchestration.config.ServiceProperties;
import com.papairs.orchestration.dto.request.CreateDocsUserRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class DocsServiceClient {

    private final WebClient webClient;
    private final Duration timeout;

    public DocsServiceClient(
            WebClient.Builder webClientBuilder,
            ServiceProperties serviceProperties
    ) {
        ServiceProperties.ServiceConfig config = serviceProperties.docsService();
        this.webClient = webClientBuilder.baseUrl(config.url()).build();
        this.timeout = Duration.ofSeconds(config.timeoutSeconds());
    }

    /**
     * Creates a new user entry in the Docs Service
     * @param request The {@link CreateDocsUserRequest} containing the details of the user to create
     * @return A {@link reactor.core.publisher.Mono} that completes successfully
     * when the user is created, or emits an error otherwise
     */
    public Mono<Void> createUser(CreateDocsUserRequest request) {
        return webClient.post()
                .uri("/api/docs/users")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Void.class)
                .timeout(timeout);
    }
}
