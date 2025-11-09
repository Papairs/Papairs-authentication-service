package com.papairs.docs.util;

import com.papairs.docs.exception.InvalidRequestException;
import jakarta.servlet.http.HttpServletRequest;

public class UserId {
    /**
     * Get user ID from request header: X-User-Id
     * @param request HTTP servlet request
     * @return User ID
     */
    public static String extract(HttpServletRequest request) {
        String userId = request.getHeader("X-User-Id");

        if (userId == null || userId.isBlank()) {
            throw new InvalidRequestException("Missing X-User-Id header");
        }

        return userId;
    }
}
