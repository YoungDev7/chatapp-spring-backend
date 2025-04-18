package com.chatapp.chatapp.auth;

import java.net.URI;
import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                  WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        if (request instanceof ServletServerHttpRequest) {
            URI uri = request.getURI();
            String query = uri.getQuery();
            
            // Extract token parameter from query string
            if (query != null && query.contains("token=")) {
                String token = extractTokenFromQuery(query);
                attributes.put("token", token);
            }
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                              WebSocketHandler wsHandler, Exception exception) {
        // Nothing to do here
    }
    
    private String extractTokenFromQuery(String query) {
        int tokenIndex = query.indexOf("token=");
        if (tokenIndex == -1) return null;
        
        // Get substring after "token="
        String tokenSubstring = query.substring(tokenIndex + 6);
        
        // Find the end of the token (either & or end of string)
        int endIndex = tokenSubstring.indexOf("&");
        if (endIndex == -1) {
            return tokenSubstring;
        } else {
            return tokenSubstring.substring(0, endIndex);
        }
    }
}