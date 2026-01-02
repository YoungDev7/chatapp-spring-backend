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

import com.chatapp.chatapp.config.WebSocketConfig;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * Interceptor that handles the initial WebSocket handshake before establishing
 * a connection.
 * 
 * <p>
 * This interceptor validates that a JWT token is provided during the WebSocket
 * handshake
 * and extracts it from the query parameters. The token is stored in the session
 * attributes
 * for later use by {@link WebSocketAuthInterceptor}. This is the first security
 * layer in
 * the WebSocket authentication flow.
 * </p>
 * 
 * <p>
 * Key responsibilities:
 * </p>
 * <ul>
 * <li>Validates the presence of a JWT token in the query parameters during
 * handshake</li>
 * <li>Ensures the token is in the correct "Bearer " format</li>
 * <li>Stores the extracted token in session attributes for authentication</li>
 * <li>Rejects handshake attempts without valid authentication</li>
 * </ul>
 * 
 * <p>
 * WebSocket Authentication Flow:
 * </p>
 * <ol>
 * <li>Client initiates handshake with token in query parameter (e.g.,
 * {@code ?token=Bearer xyz})</li>
 * <li>This interceptor validates the token format and stores it</li>
 * <li>WebSocket connection is upgraded if handshake succeeds</li>
 * <li>{@link WebSocketAuthInterceptor} validates the token when STOMP CONNECT
 * is received</li>
 * </ol>
 * 
 * <p>
 * Note: The token is passed as a query parameter rather than an HTTP header
 * because
 * JavaScript WebSocket API doesn't support custom headers for the initial
 * handshake.
 * </p>
 * 
 * @see WebSocketAuthInterceptor
 * @see WebSocketConfig
 * @see HandshakeInterceptor
 */
@Component
@RequiredArgsConstructor
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WebSocketHandshakeInterceptor.class);

    /**
     * Validates and extracts the JWT token before completing the WebSocket
     * handshake.
     * 
     * <p>
     * This method checks for a valid JWT token in the query parameters. If found,
     * the token (without "Bearer " prefix) is stored in the attributes map for
     * later
     * use by {@link WebSocketAuthInterceptor}.
     * </p>
     * 
     * <p>
     * Expected query parameter format: {@code token=Bearer <jwt-token>}
     * </p>
     * 
     * @param request    the HTTP request for the WebSocket handshake
     * @param response   the HTTP response for the WebSocket handshake
     * @param wsHandler  the WebSocket handler
     * @param attributes the attributes map to store session-level data
     * @return {@code true} to allow the handshake to proceed, {@code false} to
     *         reject it
     * @throws Exception if an error occurs during handshake processing
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {

        if (request instanceof ServletServerHttpRequest) {
            HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();

            String token = servletRequest.getParameter("token");

            if (token != null && token.startsWith("Bearer ")) {
                String actualToken = token.substring(7);
                attributes.put("token", actualToken);

                log.info("[{}] WebSocket Handshake successful; query: {}", 101, servletRequest.getQueryString());
                return true;
            } else {
                attributes.put("token", null);
                log.warn("[{}] WebSocket Handshake failed: missing token or authentication header; query: {}", 401,
                        servletRequest.getQueryString());
            }
        }
        // incase failure accured due to request not being instance of
        // ServletServerHttpRequest
        log.debug("[{}] handshake failed {}", 401, request.getClass());
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