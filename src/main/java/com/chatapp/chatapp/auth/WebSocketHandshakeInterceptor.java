package com.chatapp.chatapp.auth;

import java.net.URI;
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

        //logger.debug("beforeHandshake: {}", request);

        if (request instanceof ServletServerHttpRequest) {
            URI uri = request.getURI();
            String query = uri.getQuery();
            HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();

            // Extract token parameter from query string
            if (query != null && query.contains("token=Bearer ")) {
                String token = extractTokenFromQuery(query);
                attributes.put("token", token);

                ApplicationLogger.websocketConnectionLog(servletRequest, "WebSocket Handshake successful", 101, query);                
                return true;
            }else {
                attributes.put("token", null);
                ApplicationLogger.websocketConnectionLog(servletRequest, "WebSocket Handshake failed: missing token or authentication header", 401, query);
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
    
    private String extractTokenFromQuery(String query) {
        int tokenIndex = query.indexOf("token=Bearer ");
        if (tokenIndex == -1) return null;
        
        // Get substring after "token=Bearer "
        String tokenSubstring = query.substring(tokenIndex + 13);
        
        // Find the end of the token (either & or end of string)
        int endIndex = tokenSubstring.indexOf("&");
        if (endIndex == -1) {
            return tokenSubstring;
        } else {
            return tokenSubstring.substring(0, endIndex);
        }
    }
}