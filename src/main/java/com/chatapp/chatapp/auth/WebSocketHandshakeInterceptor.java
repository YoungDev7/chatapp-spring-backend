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

import com.chatapp.chatapp.util.ApplicationLogger;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationLogger.class);

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

                ApplicationLogger.websocketConnectionLog(servletRequest, "WebSocket Handshake successful", 101, servletRequest.getQueryString());                
                return true;
            } else {
                attributes.put("token", null);
                ApplicationLogger.websocketConnectionLog(servletRequest, "WebSocket Handshake failed: missing token or authentication header", 401, servletRequest.getQueryString());
            }    
        }
        //incase failure accured due to request not being instance of ServletServerHttpRequest
        logger.debug("handshake failed " + request.getClass());
        response.setStatusCode(HttpStatusCode.valueOf(401));
        response.getBody().write("WebSocket Handshake failed: missing token or authentication header".getBytes());
        //otherwise use:
        //response.getHeaders().add("X-Auth-Error", "WebSocket Handshake failed: missing token or authentication header");
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                              WebSocketHandler wsHandler, Exception exception) {
        // Nothing to do here
    }
}