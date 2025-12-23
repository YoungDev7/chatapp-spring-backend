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

import com.chatapp.chatapp.service.ChatViewService;
import com.chatapp.chatapp.service.UserSessionService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private static final Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);

    private final UserSessionService userSessionService;
    private final ChatViewService chatViewService;

    // Pattern to match /user/queue/chatview/{chatViewId}
    private static final Pattern CHATVIEW_PATTERN = Pattern.compile("^/user/queue/chatview/([^/]+)$");

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

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        // Mark user as offline
        userSessionService.setUserOfflineBySessionId(sessionId);

        log.info("WebSocket session {} disconnected", sessionId);
    }

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
