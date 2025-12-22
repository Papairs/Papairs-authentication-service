package com.papairs.docs.interceptor;

import com.papairs.docs.exception.InvalidRequestException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class UserIdValidationInterceptor implements HandlerInterceptor {
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String INTERNAL_SERVICE_HEADER = "X-Internal-Service";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Allow internal service requests to bypass user ID validation
        String internalService = request.getHeader(INTERNAL_SERVICE_HEADER);
        if (internalService != null && !internalService.isBlank()) {
            return true;
        }

        // For regular requests, require user ID
        String userId = request.getHeader(USER_ID_HEADER);
        if (userId == null || userId.isBlank()) {
            throw new InvalidRequestException("User ID is required");
        }

        return true;
    }
}
