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
import org.springframework.util.StringUtils;

import com.chatapp.chatapp.util.ApplicationLogger;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor  {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private static final Logger logger = LoggerFactory.getLogger(ApplicationLogger.class);
            
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        logger.debug("preSend: {}", accessor);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            try {
                String token = extractToken(accessor);
                
                if (StringUtils.hasText(token)) {
                    JwtValidationResult validationResult = jwtService.validateToken(token);
                    
                    if (validationResult.isValid()) {
                        try {
                            UserDetails userDetails = userDetailsService.loadUserByUsername(validationResult.getUsername());
                            
                            UsernamePasswordAuthenticationToken authentication = 
                                new UsernamePasswordAuthenticationToken(
                                    userDetails, 
                                    null, 
                                    userDetails.getAuthorities()
                                );
                            
                            SecurityContextHolder.getContext().setAuthentication(authentication);

                            accessor.setUser(authentication);

                            ApplicationLogger.websocketConnectionLog("connection successful", accessor.getUser().getName(), accessor.getCommand().toString(), channel.toString(), token);
                            return message;

                        } catch (Exception e) {
                            accessor.setHeader("simpConnectMessage", "Authentication failed");
                            ApplicationLogger.websocketConnectionLog("connection failed" + e.getMessage(), accessor.getUser().getName(), accessor.getCommand().toString(), channel.toString(), token);

                            return null;
                        }
                    }
                }
            } catch (IllegalArgumentException e) {
                accessor.setHeader("simpConnectMessage", "Authentication failed");
                ApplicationLogger.websocketConnectionLog("connection failed" + e.getMessage(), accessor.getUser().getName(), accessor.getCommand().toString(), channel.toString(), null);

                return null;
            }
        }
        return null;
    }
    
    private String extractToken(StompHeaderAccessor accessor) {
        // Try to get from Authorization header (STOMP headers)
        List<String> authorization = accessor.getNativeHeader("Authorization");

        if (authorization != null && !authorization.isEmpty()) {
            String auth = authorization.get(0);
            
            if (auth.startsWith("Bearer ")) {
                return auth.substring(7);
            }else {
                throw new IllegalArgumentException("Invalid Authorization header format");
            }
        }else {
            throw new IllegalArgumentException("Authorization header is missing");
        }
        
        // // Then try to get from query parameters (SockJS URL)
        // String query = accessor.getFirstNativeHeader("query");
        // if (query != null && query.contains("token=")) {
        //     return query.substring(query.indexOf("token=") + 6);
        // }
        
        // // Finally try to get from sessionAttributes (from handshake)
        // String token = (String) accessor.getSessionAttributes().get("token");
        // if (token != null) {
        //     return token;
        // }
        // return null;
    }
}