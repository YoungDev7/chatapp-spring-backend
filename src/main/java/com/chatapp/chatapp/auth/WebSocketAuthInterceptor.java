package com.chatapp.chatapp.auth;

import java.util.List;

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
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // Extract JWT token from headers or query parameters
            String token = extractToken(accessor);
            
            if (StringUtils.hasText(token)) {
                JwtValidationResult validationResult = jwtService.validateToken(token);
                
                if (validationResult.isValid()) {
                    try {
                        // Load user details and set authentication
                        UserDetails userDetails = userDetailsService.loadUserByUsername(validationResult.getUsername());
                        UsernamePasswordAuthenticationToken authentication = 
                            new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                        
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        accessor.setUser(authentication);
                    } catch (Exception e) {
                        // Token validation failed
                        System.out.println("[LOG] WebSocket auth failed: " + e.getMessage());
                    }
                }
            }
        }
        return message;
    }
    
    private String extractToken(StompHeaderAccessor accessor) {
        // Try to get from Authorization header first (STOMP headers)
        List<String> authorization = accessor.getNativeHeader("Authorization");
        if (authorization != null && !authorization.isEmpty()) {
            String auth = authorization.get(0);
            if (auth.startsWith("Bearer ")) {
                return auth.substring(7);
            }
        }
        
        // Then try to get from query parameters (SockJS URL)
        String query = accessor.getFirstNativeHeader("query");
        if (query != null && query.contains("token=")) {
            return query.substring(query.indexOf("token=") + 6);
        }
        
        // Finally try to get from sessionAttributes (from handshake)
        String token = (String) accessor.getSessionAttributes().get("token");
        if (token != null) {
            return token;
        }
        
        return null;
    }
}