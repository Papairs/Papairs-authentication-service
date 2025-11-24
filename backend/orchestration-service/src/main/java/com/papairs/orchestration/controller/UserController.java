package com.papairs.orchestration.controller;

import com.papairs.orchestration.dto.request.RegisterAccountRequest;
import com.papairs.orchestration.dto.response.UserResponse;
import com.papairs.orchestration.service.UserOrchestrationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/auth")
public class UserController {

    private final UserOrchestrationService orchestrationService;

    public UserController(UserOrchestrationService orchestrationService) {
        this.orchestrationService = orchestrationService;
    }

    /**
     * Creates a new user on both the Auth Service and Docs Service
     * <p>
     * This endpoint receives registration details, validates them and initiates
     * the account creation process through the orchestration service
     * </p>
     * @param request The request body containing the user's registration information
     * The {@link Valid @Valid} annotation triggers bean validation
     * @return A {@link Mono} that, upon successful completion, emits a {@link UserResponse}
     * containing details of the newly created user
     */
    @PostMapping("/register")
    public Mono<UserResponse> createUser(@Valid @RequestBody RegisterAccountRequest request) {
        return orchestrationService.createUser(request);
    }
}
