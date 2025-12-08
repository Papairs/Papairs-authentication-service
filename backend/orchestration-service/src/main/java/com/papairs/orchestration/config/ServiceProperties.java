package com.papairs.orchestration.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "services")
@Validated
public record ServiceProperties(
        ServiceConfig authService,
        ServiceConfig docsService,
        CompensationConfig compensation,
        ServiceConfig aiService
) {
    public record ServiceConfig(
            @NotBlank String url,
            @Min(1) int timeoutSeconds
    ) {}

    public record CompensationConfig(
            @Min(1) int timeoutSeconds
    ) {}
}