package com.chatapp.chatapp.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chatapp.chatapp.entity.User;
import com.chatapp.chatapp.entity.UserSession;
import com.chatapp.chatapp.repository.UserRepository;
import com.chatapp.chatapp.repository.UserSessionRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service responsible for managing user online/offline status and WebSocket
 * sessions.
 * 
 * <p>
 * This service tracks user presence by maintaining session information that
 * includes
 * their online status and active WebSocket session IDs. It provides essential
 * functionality
 * for the real-time messaging system to determine whether to deliver messages
 * via WebSocket
 * or queue them in RabbitMQ.
 * 
 * <p>
 * Key responsibilities include:
 * <ul>
 * <li>Tracking user online/offline status</li>
 * <li>Managing WebSocket session ID associations with users</li>
 * <li>Providing presence information for message delivery routing</li>
 * <li>Creating user sessions when users first connect</li>
 * </ul>
 * 
 */
@Service
@RequiredArgsConstructor
public class UserSessionService {

    private static final Logger log = LoggerFactory.getLogger(UserSessionService.class);

    private final UserSessionRepository userSessionRepository;
    private final UserRepository userRepository;

    /**
     * Marks a user as online and stores their WebSocket session ID
     */
    @Transactional
    public void setUserOnline(String userUid, String websocketSessionId) {
        UserSession session = userSessionRepository.findByUserUid(userUid)
                .orElseGet(() -> {
                    User user = userRepository.findById(userUid)
                            .orElseThrow(() -> new RuntimeException("User not found: " + userUid));
                    return new UserSession(user);
                });

        session.setIsOnline(true);
        session.setWebsocketSessionId(websocketSessionId);
        userSessionRepository.save(session);

        log.info("User {} is now online with session {}", userUid, websocketSessionId);
    }

    /**
     * Marks a user as offline
     */
    @Transactional
    public void setUserOffline(String userUid) {
        Optional<UserSession> sessionOpt = userSessionRepository.findByUserUid(userUid);

        if (sessionOpt.isPresent()) {
            UserSession session = sessionOpt.get();
            session.setIsOnline(false);
            session.setWebsocketSessionId(null);
            userSessionRepository.save(session);
            log.info("User {} is now offline", userUid);
        }
    }

    /**
     * Marks a user as offline by WebSocket session ID
     */
    @Transactional
    public void setUserOfflineBySessionId(String websocketSessionId) {
        Optional<UserSession> sessionOpt = userSessionRepository.findByWebsocketSessionId(websocketSessionId);

        if (sessionOpt.isPresent()) {
            UserSession session = sessionOpt.get();
            session.setIsOnline(false);
            session.setWebsocketSessionId(null);
            userSessionRepository.save(session);
            log.info("User with session {} is now offline", websocketSessionId);
        }
    }

    /**
     * Checks if a user is currently online
     */
    public boolean isUserOnline(String userUid) {
        return userSessionRepository.findByUserUid(userUid)
                .map(UserSession::getIsOnline)
                .orElse(false);
    }

    /**
     * Gets the WebSocket session ID for a user
     */
    public Optional<String> getUserWebSocketSessionId(String userUid) {
        return userSessionRepository.findByUserUid(userUid)
                .map(UserSession::getWebsocketSessionId);
    }

    public Optional<String> getUserUidFromSessionId(String websocketSessionId) {
        return userSessionRepository.findByWebsocketSessionId(websocketSessionId)
                .map(UserSession::getUserUid);
    }
}
