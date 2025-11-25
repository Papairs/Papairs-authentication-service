package com.papairs.orchestration.service;

import com.papairs.orchestration.client.AuthServiceClient;
import com.papairs.orchestration.config.ServiceProperties;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
public class CompensationService {

    private final AuthServiceClient authServiceClient;
    private final Duration timeout;

    public CompensationService(
            AuthServiceClient authServiceClient,
            ServiceProperties serviceProperties
    ) {
        this.authServiceClient = authServiceClient;
        this.timeout = Duration.ofSeconds(
                serviceProperties.compensation().timeoutSeconds()
        );
    }

    /**
     * Compensates for a failed user creation saga by deleting the user from the Authentication Service
     * <p>
     * This method is invoked when the initial user creation in the Auth Service succeeded, but a
     * subsequent step in the overall process failed
     * </p>
     * <p>
     * It operates on a "best-effort" basis. If the delete request fails (e.g., due to the
     * Auth Service being temporarily unavailable), the error is suppressed using {@code onErrorResume}
     * This prevents a compensation failure from blocking the entire error-handling process and ensures
     * the original failure is reported to the client
     * </p>
     * @param userId The unique identifier of the user account to be deleted
     * @return A {@link Mono<Void>} that completes once the deletion attempt is finished,
     * regardless of whether it was successful or not
     */
    public Mono<Void> compensateUserCreation(String userId) {
        return authServiceClient.deleteUser(userId)
                .onErrorResume(e -> Mono.empty());
    }
}