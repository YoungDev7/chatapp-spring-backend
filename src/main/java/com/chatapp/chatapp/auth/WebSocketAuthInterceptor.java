package com.chatapp.chatapp.auth;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import com.chatapp.chatapp.config.WebSocketConfig;
import com.chatapp.chatapp.dto.JwtValidationResult;
import com.chatapp.chatapp.entity.User;
import com.chatapp.chatapp.service.JwtService;
import com.chatapp.chatapp.util.LoggerUtil;

import lombok.RequiredArgsConstructor;

/**
 * Interceptor that handles authentication for WebSocket STOMP connections.
 * 
 * <p>
 * This interceptor validates JWT tokens during WebSocket CONNECT commands and
 * ensures
 * that the authenticated user is properly set in the Spring Security context
 * for all
 * subsequent WebSocket messages.
 * </p>
 * 
 * <p>
 * Key responsibilities:
 * </p>
 * <ul>
 * <li>Intercepts STOMP CONNECT commands to validate JWT tokens</li>
 * <li>Extracts JWT tokens from multiple sources (Authorization header, query
 * parameters, session attributes)</li>
 * <li>Validates tokens using JwtService and loads user details</li>
 * <li>Sets up Spring Security authentication for the WebSocket session</li>
 * <li>Maintains authentication context for non-CONNECT messages</li>
 * </ul>
 * 
 * @see WebSocketHandshakeInterceptor
 * @see WebSocketConfig
 */
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WebSocketAuthInterceptor.class);

    private final LoggerUtil loggerUtil;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || !(StompCommand.CONNECT.equals(accessor.getCommand()))) {
            // If user exists on accessor but not in SecurityContext, set it
            if (accessor != null && accessor.getUser() != null
                    && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Get the authentication from session attributes instead of casting
                UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) accessor
                        .getSessionAttributes().get("SPRING_SECURITY_CONTEXT");
                if (auth != null) {
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }

            return message;
        }

        String token;

        try {
            token = extractToken(accessor);
        } catch (IllegalArgumentException e) {
            accessor.setHeader("simpConnectMessage", "Authentication failed");
            log.warn("WebSocket connection failed: {}; command: {}; channel: {}",
                    e.getMessage(),
                    accessor.getCommand() != null ? accessor.getCommand().toString() : "unknown",
                    channel.toString());

            return null;
        }

        JwtValidationResult validationResult = jwtService.validateToken(token);

        if (!validationResult.isValid()) {
            accessor.setHeader("simpConnectMessage", "Authentication failed");
            log.warn("WebSocket connection failed: {}; command: {}; channel: {};",
                    validationResult.getStatus(),
                    accessor.getCommand() != null ? accessor.getCommand().toString() : "unknown",
                    channel.toString());

            return null;
        }

        try {
            UserDetails userDetails = userDetailsService.loadUserByUsername(validationResult.getUsername());

            // Cast to your User entity to get the uid
            User user = (User) userDetails;

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities());

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Store authentication in session attributes for later use
            accessor.getSessionAttributes().put("SPRING_SECURITY_CONTEXT", authentication);

            // Set user principal to userUid for /user/{userUid}/... routing
            accessor.setUser(() -> user.getUid());

            log.info("WebSocket connection successful for user: {} (uid: {}); command: {}; channel: {};",
                    user.getEmail(),
                    user.getUid(),
                    accessor.getCommand() != null ? accessor.getCommand().toString() : "unknown",
                    channel.toString());

            return message;

        } catch (Exception e) {
            accessor.setHeader("simpConnectMessage", "Authentication failed");
            log.warn("WebSocket connection failed: {}; command: {}; channel: {};",
                    e.getMessage(),
                    accessor.getCommand() != null ? accessor.getCommand().toString() : "unknown",
                    channel.toString());

            return null;
        }
    }

    private String extractToken(StompHeaderAccessor accessor) {
        // Try to get from Authorization header (STOMP headers)
        List<String> authorization = accessor.getNativeHeader("Authorization");

        if (authorization != null && !authorization.isEmpty()) {
            String auth = authorization.get(0);

            if (auth.startsWith("Bearer ")) {
                return auth.substring(7);
            } else {
                throw new IllegalArgumentException("Invalid Authorization header format");
            }
        }

        // Then try to get from query parameters (SockJS URL)
        String query = accessor.getFirstNativeHeader("query");
        if (query != null && query.contains("token=Bearer ")) {
            return query.substring(query.indexOf("token=Bearer ") + 13);
        }

        // Finally try to get from sessionAttributes (from handshake)
        // handskaheInterceptor handles whether or not the header contains "Bearer" or
        // is empty
        String token = null;
        if (accessor.getSessionAttributes() != null) {
            token = (String) accessor.getSessionAttributes().get("token");
        }

        if (token != null) {
            return token;
        }

        throw new IllegalArgumentException("Authorization header is missing");
    }
}