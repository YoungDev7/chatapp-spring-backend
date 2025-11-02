package com.chatapp.chatapp.util;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Utility class for setting up MDC context for HTTP requests.
 * Use standard SLF4J logging in your classes instead of static methods.
 */
@Component
public class LoggerUtil {
    /**
     * Sets up MDC context from an HTTP request.
     * Call this at the beginning of request processing.
     */
    public void setupRequestContext(HttpServletRequest request) {
        MDC.put("path", request.getServletPath());
        MDC.put("clientIP", request.getRemoteAddr());
        MDC.put("method", request.getMethod());
    }

    /**
     * Sets up MDC context with user information.
     */
    public void setupUserContext(String username) {
        MDC.put("username", username);
    }

    /**
     * Clears all MDC context.
     */
    public void clearContext() {
        MDC.clear();
    }
}