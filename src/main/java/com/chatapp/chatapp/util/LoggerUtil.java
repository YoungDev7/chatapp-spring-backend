package com.chatapp.chatapp.util;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import com.chatapp.chatapp.filter.LoggingFilter;
import com.chatapp.chatapp.filter.UserContextFilter;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Utility class for managing Mapped Diagnostic Context (MDC) for structured
 * logging.
 * 
 * <p>
 * This component provides centralized MDC management for the application's
 * logging
 * infrastructure. It populates the MDC with contextual information that is
 * automatically
 * included in all log statements during request processing.
 * </p>
 * 
 * <p>
 * MDC Keys Managed:
 * </p>
 * <ul>
 * <li>{@code path} - The servlet path of the HTTP request</li>
 * <li>{@code clientIP} - The remote IP address of the client</li>
 * <li>{@code method} - The HTTP method (GET, POST, PUT, DELETE, etc.)</li>
 * <li>{@code username} - The authenticated username (if available)</li>
 * </ul>
 * 
 * <p>
 * This utility is designed to be used by filters in the security filter chain,
 * particularly {@link LoggingFilter} and {@link UserContextFilter}.
 * </p>
 * 
 * @see org.slf4j.MDC
 * @see LoggingFilter
 * @see UserContextFilter
 */
@Component
public class LoggerUtil {

    /**
     * Sets up MDC context with HTTP request information.
     * 
     * <p>
     * Populates the MDC with the following keys from the request:
     * <ul>
     * <li>{@code path} - The servlet path</li>
     * <li>{@code clientIP} - The remote address</li>
     * <li>{@code method} - The HTTP method</li>
     * </ul>
     * 
     * <p>
     * This method should be called at the beginning of request processing,
     * typically by {@link LoggingFilter}.
     * </p>
     * 
     * @param request the HTTP servlet request to extract context from
     */
    public void setupRequestContext(HttpServletRequest request) {
        MDC.put("path", request.getServletPath());
        MDC.put("clientIP", request.getRemoteAddr());
        MDC.put("method", request.getMethod());
    }

    /**
     * Enriches the MDC context with authenticated user information.
     * 
     * <p>
     * Adds the {@code username} key to the existing MDC context. This method
     * is typically called by {@link UserContextFilter} after successful
     * authentication.
     * </p>
     * 
     * @param username the authenticated username to add to the MDC
     */
    public void setupUserContext(String username) {
        MDC.put("username", username);
    }

    /**
     * Clears all MDC context to prevent memory leaks.
     * 
     * <p>
     * This method removes all keys from the MDC. It must be called at the end
     * of request processing to prevent MDC values from leaking into other requests
     * when threads are reused from the thread pool.
     * </p>
     * 
     * <p>
     * This method is typically called in a {@code finally} block by
     * {@link LoggingFilter} to ensure cleanup even if exceptions occur.
     * </p>
     */
    public void clearContext() {
        MDC.clear();
    }
}