package com.chatapp.chatapp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.chatapp.chatapp.auth.WebSocketAuthInterceptor;
import com.chatapp.chatapp.auth.WebSocketHandshakeInterceptor;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins(
                        "http://localhost:5173",
                        "http://127.0.0.1:5173",
                        "http://frontend:5173",
                        "http://0.0.0.0:5173",
                        "http://host.docker.internal:5173",
                        "http://localhost",
                        "http://127.0.0.1",
                        "http://localhost:80",
                        "http://127.0.0.1:80")
                .withSockJS()
                .setInterceptors(new WebSocketHandshakeInterceptor());
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        // Enable simple broker
        // /topic - for broadcast messages
        // /queue - for point-to-point messages (used with /user prefix)
        registry.enableSimpleBroker("/topic", "/queue");
        // Set user destination prefix for user-specific messages
        // Messages sent to /user/queue/... are automatically routed to the
        // authenticated user
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketAuthInterceptor);
    }
}
