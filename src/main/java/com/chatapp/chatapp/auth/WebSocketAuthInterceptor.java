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
 * ensures that the authenticated user is properly set in the Spring Security
 * context
 * for all subsequent WebSocket messages. It works in conjunction with
 * {@link WebSocketHandshakeInterceptor} to provide comprehensive WebSocket
 * security.
 * </p>
 * 
 * <p>
 * Key responsibilities:
 * </p>
 * <ul>
 * <li>Intercepts STOMP CONNECT commands to validate JWT tokens</li>
 * <li>Extracts JWT tokens from multiple sources (Authorization header, query
 * parameters, session attributes)</li>
 * <li>Validates tokens using {@link JwtService} and loads user details</li>
 * <li>Sets up Spring Security authentication for the WebSocket session</li>
 * <li>Maintains authentication context for non-CONNECT messages</li>
 * <li>Sets the user principal to the user's UID for proper {@code /user}
 * destination routing</li>
 * </ul>
 * 
 * <p>
 * Authentication Flow:
 * </p>
 * <ol>
 * <li>Client initiates WebSocket handshake with JWT token in query
 * parameters</li>
 * <li>{@link WebSocketHandshakeInterceptor} validates and stores the token</li>
 * <li>Client sends STOMP CONNECT frame</li>
 * <li>This interceptor extracts and validates the JWT token</li>
 * <li>User principal is set to the user's UID for Spring WebSocket routing</li>
 * <li>Authentication is stored in session attributes for subsequent frames</li>
 * </ol>
 * 
 * @see WebSocketHandshakeInterceptor
 * @see WebSocketConfig
 * @see JwtService
 * @see ChannelInterceptor
 */
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WebSocketAuthInterceptor.class);

    private final LoggerUtil loggerUtil;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    /**
     * Intercepts outbound messages to validate authentication for STOMP CONNECT
     * commands.
     * 
     * <p>
     * For CONNECT commands, this method extracts and validates the JWT token, loads
     * user details, and sets up the Spring Security context. For non-CONNECT
     * commands,
     * it ensures the security context is maintained from the session attributes.
     * </p>
     * 
     * <p>
     * Token Extraction Priority:
     * </p>
     * <ol>
     * <li>Authorization header from STOMP native headers</li>
     * <li>Query parameters (for SockJS compatibility)</li>
     * <li>Session attributes (stored during handshake)</li>
     * </ol>
     * 
     * @param message the STOMP message being sent
     * @param channel the message channel
     * @return the message if authentication succeeds, {@code null} to reject the
     *         connection
     */
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

            User user = (User) userDetails;

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities());

            SecurityContextHolder.getContext().setAuthentication(authentication);

            accessor.getSessionAttributes().put("SPRING_SECURITY_CONTEXT", authentication);

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

    /**
     * Extracts the JWT token from various sources in the STOMP message.
     * 
     * <p>
     * This method attempts to extract the token from multiple locations to support
     * different WebSocket clients and connection methods (native WebSocket and
     * SockJS).
     * </p>
     * 
     * <p>
     * Extraction priority:
     * </p>
     * <ol>
     * <li>STOMP Authorization header (native WebSocket)</li>
     * <li>Query parameter in SockJS URL</li>
     * <li>Session attributes (from handshake interceptor)</li>
     * </ol>
     * 
     * @param accessor the STOMP header accessor containing message headers and
     *                 session
     * @return the extracted JWT token (without "Bearer " prefix)
     * @throws IllegalArgumentException if no valid token is found
     */
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