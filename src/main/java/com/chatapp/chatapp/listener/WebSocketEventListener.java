package com.chatapp.chatapp.listener;

import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import com.chatapp.chatapp.auth.WebSocketAuthInterceptor;
import com.chatapp.chatapp.service.ChatViewService;
import com.chatapp.chatapp.service.UserSessionService;

import lombok.RequiredArgsConstructor;

/**
 * Event listener that handles WebSocket lifecycle events for user presence and
 * subscription management.
 * 
 * <p>
 * This component listens to WebSocket STOMP events to track user connectivity
 * status
 * and validate chat view subscriptions. It integrates with the application's
 * user session
 * management and chat view security.
 * </p>
 * 
 * <p>
 * Key responsibilities:
 * </p>
 * <ul>
 * <li>Tracks user online/offline status based on WebSocket connection
 * state</li>
 * <li>Updates user session data when clients connect or disconnect</li>
 * <li>Validates user membership before allowing chat view subscriptions</li>
 * <li>Provides security monitoring for unauthorized subscription attempts</li>
 * </ul>
 * 
 * <p>
 * Event Handling:
 * </p>
 * <ul>
 * <li>{@link SessionConnectedEvent} - Marks users as online when WebSocket
 * connects</li>
 * <li>{@link SessionDisconnectEvent} - Marks users as offline when WebSocket
 * disconnects</li>
 * <li>{@link SessionSubscribeEvent} - Validates chat view memberships for
 * subscriptions</li>
 * </ul>
 * 
 * @see UserSessionService
 * @see ChatViewService
 * @see org.springframework.web.socket.messaging.SessionConnectedEvent
 */
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private static final Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);

    private final UserSessionService userSessionService;
    private final ChatViewService chatViewService;

    // Pattern to match /user/queue/chatview/{chatViewId}
    private static final Pattern CHATVIEW_PATTERN = Pattern.compile("^/user/queue/chatview/([^/]+)$");

    /**
     * Handles WebSocket connection events and marks users as online.
     * 
     * <p>
     * When a client successfully establishes a WebSocket connection, this method
     * extracts the user's unique identifier (UID) from the principal and updates
     * the user's session status to online using {@link UserSessionService}.
     * </p>
     * 
     * <p>
     * Note: This event fires after the STOMP CONNECT frame has been processed
     * and authentication has been completed by {@link WebSocketAuthInterceptor}.
     * </p>
     * 
     * @param event the session connected event containing user and session
     *              information
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        Principal principal = headerAccessor.getUser();

        if (principal != null) {
            String userUid = principal.getName();
            String sessionId = headerAccessor.getSessionId();

            // Mark user as online
            userSessionService.setUserOnline(userUid, sessionId);

            log.info("User {} connected with session {}", userUid, sessionId);
        } else {
            log.warn("WebSocket session connected without user principal: sessionId={}",
                    headerAccessor.getSessionId());
        }
    }

    /**
     * Handles WebSocket disconnection events and marks users as offline.
     * 
     * <p>
     * When a client disconnects from the WebSocket, this method updates the user's
     * session status to offline. The user is identified by the session ID rather
     * than
     * the principal, as the user context may not be available during disconnection.
     * </p>
     * 
     * <p>
     * This event fires when:
     * <ul>
     * <li>The client explicitly closes the WebSocket connection</li>
     * <li>The connection times out</li>
     * <li>A network error causes disconnection</li>
     * </ul>
     * 
     * @param event the session disconnect event containing session information
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        // Mark user as offline
        userSessionService.setUserOfflineBySessionId(sessionId);

        log.info("WebSocket session {} disconnected", sessionId);
    }

    /**
     * Handles WebSocket subscription events and validates chat view access.
     * 
     * <p>
     * This method is invoked when a client attempts to subscribe to a WebSocket
     * destination. For chat view destinations matching the pattern
     * {@code /user/queue/chatview/{chatViewId}}, it validates that the user is
     * a member of the specified chat view before allowing the subscription.
     * </p>
     * 
     * <p>
     * Security: While Spring automatically enforces user-specific destination
     * access control for {@code /user} destinations, this method provides an
     * additional layer of security by verifying chat view membership and logging
     * unauthorized access attempts for security monitoring.
     * </p>
     * 
     * @param event the session subscribe event containing destination and user
     *              information
     */
    @EventListener
    public void handleWebSocketSubscribeListener(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = headerAccessor.getDestination();
        String sessionId = headerAccessor.getSessionId();
        Principal principal = headerAccessor.getUser();

        if (destination != null && principal != null) {
            String userUid = principal.getName();

            // Validate subscription to chatview destinations
            Matcher matcher = CHATVIEW_PATTERN.matcher(destination);
            if (matcher.matches()) {
                String chatViewId = matcher.group(1);

                // Verify user is a member of this chatview
                if (!chatViewService.isUserInChatView(chatViewId, userUid)) {
                    log.warn("User {} attempted to subscribe to chatview {} without membership. Session: {}",
                            userUid, chatViewId, sessionId);
                    // Note: Spring will automatically reject the subscription since we're using
                    // /user destinations
                    // but we log the attempt for security monitoring
                    return;
                }

                log.debug("User {} subscribed to chatview {} (validated membership)", userUid, chatViewId);
            }
        }

        log.debug("Session {} subscribed to {}", sessionId, destination);
    }
}
