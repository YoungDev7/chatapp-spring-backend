package com.chatapp.chatapp.auth;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WebSocketHandshakeInterceptor.class);

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                  WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {

        if (request instanceof ServletServerHttpRequest) {
            HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();
            
            // Get specific parameter
            String token = servletRequest.getParameter("token");

            if (token != null && token.startsWith("Bearer ")) {
                String actualToken = token.substring(7);
                attributes.put("token", actualToken);

                log.info("[{}] WebSocket Handshake successful; query: {}",101, servletRequest.getQueryString());                
                return true;
            } else {
                attributes.put("token", null);
                log.warn("[{}] WebSocket Handshake failed: missing token or authentication header; query: {}", 401, servletRequest.getQueryString());
            }    
        }
        //incase failure accured due to request not being instance of ServletServerHttpRequest
        log.debug("[{}] handshake failed {}",401, request.getClass());
        response.setStatusCode(HttpStatusCode.valueOf(401));
        response.getBody().write("WebSocket Handshake failed: missing token or authentication header".getBytes());
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                              WebSocketHandler wsHandler, Exception exception) {
        // Nothing to do here
    }
}