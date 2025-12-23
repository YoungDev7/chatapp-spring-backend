package com.chatapp.chatapp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Service;

import com.chatapp.chatapp.dto.NotificationDTO;
import com.chatapp.chatapp.dto.NotificationDTO.NotificationType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final UserSessionService userSessionService;
    private final SimpUserRegistry simpUserRegistry;

    public void notifyUserAddedToChatView(String userUid, String chatviewId) {
        sendNotificationToUser(
                NotificationDTO.builder()
                        .notificationType(NotificationType.ADDED_TO_CHATVIEW)
                        .chatViewId(chatviewId)
                        .build(),
                userUid);
    }

    private void sendNotificationToUser(NotificationDTO notification, String userUid) {
        boolean isOnline = userSessionService.isUserOnline(userUid);

        if (!isOnline) {
            log.debug("User {} is offline, skipping notification: {}", userUid,
                    notification.getNotificationType().toString());
            return;
        }

        SimpUser simpUser = simpUserRegistry.getUser(userUid);

        if (simpUser != null && simpUser.hasSessions()) {
            log.debug("Sending notification to user {}: {}", userUid,
                    notification.getNotificationType().toString());
            messagingTemplate.convertAndSendToUser(userUid, "/queue/notifications", notification);
        } else {
            log.warn("User {} is marked online but has no active WebSocket sessions", userUid);
        }
    }
}