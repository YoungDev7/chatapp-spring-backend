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
 * <p>This filter executes after JWT authentication and extracts the authenticated
 * user's username from the Spring Security context. It then adds this information
 * to the MDC (Mapped Diagnostic Context) for enhanced logging with user-specific details.</p>
 * 
 * <p>Key responsibilities:</p>
 * <ul>
 *   <li>Extracts authenticated user information from SecurityContext</li>
 *   <li>Adds username to MDC for all subsequent logging in the request</li>
 *   <li>Executes after {@link JwtAuthenticationFilter} to ensure authentication is complete</li>
 *   <li>Skips MDC setup for anonymous/unauthenticated requests</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class UserContextFilter extends OncePerRequestFilter {

    private final LoggerUtil loggerUtil;

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