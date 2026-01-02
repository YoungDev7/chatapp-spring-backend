package com.chatapp.chatapp.filter;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.chatapp.chatapp.util.LoggerUtil;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Filter that sets up logging context for all incoming HTTP requests.
 * 
 * <p>
 * This filter is the first in the security filter chain and is responsible for
 * setting up the MDC (Mapped Diagnostic Context) with request information such
 * as
 * the request path, client IP address, and HTTP method. This context is then
 * available for all subsequent logging statements during the request
 * processing.
 * </p>
 * 
 * <p>
 * Key responsibilities:
 * </p>
 * <ul>
 * <li>Sets up MDC context at the start of each request using
 * {@link LoggerUtil}</li>
 * <li>Ensures the context is properly cleared after request completion</li>
 * <li>Executes before JWT authentication for comprehensive logging
 * coverage</li>
 * </ul>
 * 
 * <p>
 * Filter Ordering: This filter executes first in the chain with
 * {@code @Order(1)}
 * to ensure logging context is available for all subsequent filters and request
 * processing.
 * </p>
 * 
 * <p>
 * MDC Keys: The following keys are set in the MDC:
 * <ul>
 * <li>{@code path} - The servlet path of the request</li>
 * <li>{@code clientIP} - The remote IP address of the client</li>
 * <li>{@code method} - The HTTP method (GET, POST, etc.)</li>
 * </ul>
 * 
 * @see LoggerUtil
 * @see OncePerRequestFilter
 */
@Component
@RequiredArgsConstructor
public class LoggingFilter extends OncePerRequestFilter {

    private final LoggerUtil loggerUtil;

    /**
     * Sets up MDC logging context for the request and ensures cleanup after
     * processing.
     * 
     * <p>
     * This method delegates to
     * {@link LoggerUtil#setupRequestContext(HttpServletRequest)}
     * to populate the MDC with request information before proceeding with the
     * filter chain.
     * The {@code finally} block ensures that MDC context is cleared even if an
     * exception occurs.
     * </p>
     * 
     * @param request     the HTTP servlet request
     * @param response    the HTTP servlet response
     * @param filterChain the filter chain to continue processing
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        try {
            loggerUtil.setupRequestContext(request);
            filterChain.doFilter(request, response);
        } finally {
            loggerUtil.clearContext();
        }
    }
}