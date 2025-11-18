package com.papairs.docs.util;

import com.papairs.docs.exception.InvalidRequestException;
import jakarta.servlet.http.HttpServletRequest;

public class UserId {
    /**
     * Get user ID from request header: UserId
     * @param request HTTP servlet request
     * @return User ID
     */
    public static String extract(HttpServletRequest request) {
        String userId = request.getHeader("UserId");

        if (userId == null || userId.isBlank()) {
            throw new InvalidRequestException("Missing UserId header");
        }

        return userId;
    }
}
