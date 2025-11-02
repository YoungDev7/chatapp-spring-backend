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
 * <p>This filter is the first in the security filter chain and is responsible for
 * setting up the MDC (Mapped Diagnostic Context) with request information such as
 * the request path, client IP address, and HTTP method. This context is then
 * available for all subsequent logging statements during the request processing.</p>
 * 
 * <p>Key responsibilities:</p>
 * <ul>
 *   <li>Sets up MDC context at the start of each request using {@link LoggerUtil}</li>
 *   <li>Ensures the context is properly cleared after request completion</li>
 *   <li>Executes before JWT authentication for comprehensive logging coverage</li>
 * </ul>
 * 
 * @see LoggerUtil
 * @see SecurityConfig
 */
@Component
@RequiredArgsConstructor
public class LoggingFilter extends OncePerRequestFilter {

    private final LoggerUtil loggerUtil;

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