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

/**
 * Service responsible for sending real-time notifications to users via
 * WebSocket.
 * 
 * <p>
 * This service handles the delivery of system notifications to online users,
 * such as being added to chatviews or other application events. It checks user
 * online status and WebSocket session availability before attempting delivery.
 * 
 * <p>
 * Key responsibilities include:
 * <ul>
 * <li>Sending chatview membership notifications</li>
 * <li>Verifying user online status before sending notifications</li>
 * <li>Validating active WebSocket sessions</li>
 * <li>Routing notifications to user-specific WebSocket destinations</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final UserSessionService userSessionService;
    private final SimpUserRegistry simpUserRegistry;

    /**
     * Notifies a user that they have been added to a chatview.
     * 
     * <p>
     * Sends a notification of type ADDED_TO_CHATVIEW to the user if they are
     * currently online. If the user is offline, the notification is skipped.
     * 
     * @param userUid    the UID of the user to notify
     * @param chatviewId the ID of the chatview the user was added to
     */
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