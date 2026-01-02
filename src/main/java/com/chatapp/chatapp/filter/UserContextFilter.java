package com.chatapp.chatapp.filter;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.chatapp.chatapp.util.LoggerUtil;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Filter that enriches the logging context with authenticated user information.
 * 
 * <p>
 * This filter executes after JWT authentication and extracts the authenticated
 * user's username from the Spring Security context. It then adds this
 * information
 * to the MDC (Mapped Diagnostic Context) for enhanced logging with
 * user-specific details.
 * </p>
 * 
 * <p>
 * Key responsibilities:
 * </p>
 * <ul>
 * <li>Extracts authenticated user information from SecurityContext</li>
 * <li>Adds username to MDC for all subsequent logging in the request</li>
 * <li>Executes after {@link JwtAuthenticationFilter} to ensure authentication
 * is complete</li>
 * <li>Skips MDC setup for anonymous/unauthenticated requests</li>
 * </ul>
 * 
 * <p>
 * Filter Ordering: This filter executes third in the chain with
 * {@code @Order(3)},
 * after {@link LoggingFilter} and {@link JwtAuthenticationFilter}. This ensures
 * that
 * authentication is already established before attempting to extract user
 * information.
 * </p>
 * 
 * <p>
 * MDC Enhancement: Adds the {@code username} key to the existing MDC context
 * established by {@link LoggingFilter}.
 * </p>
 * 
 * @see LoggerUtil
 * @see JwtAuthenticationFilter
 * @see OncePerRequestFilter
 */
@Component
@RequiredArgsConstructor
public class UserContextFilter extends OncePerRequestFilter {

    private final LoggerUtil loggerUtil;

    /**
     * Enriches MDC context with authenticated user information if available.
     * 
     * <p>
     * Checks if the request has an authenticated user in the Spring Security
     * context.
     * If authentication exists and is not anonymous, extracts the username and adds
     * it
     * to the MDC using {@link LoggerUtil#setupUserContext(String)}.
     * </p>
     * 
     * <p>
     * Note: The MDC context set by this method is cleared by {@link LoggingFilter}
     * at the end of request processing.
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

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()
                && !authentication.getPrincipal().equals("anonymousUser")) {
            String username = authentication.getName();
            loggerUtil.setupUserContext(username);
        }

        filterChain.doFilter(request, response);
    }
}