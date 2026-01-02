package com.chatapp.chatapp.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.chatapp.chatapp.entity.User;
import com.chatapp.chatapp.event.UserAddedToChatViewEvent;
import com.chatapp.chatapp.event.UserCreatedEvent;
import com.chatapp.chatapp.repository.UserRepository;
import com.chatapp.chatapp.service.ChatViewService;
import com.chatapp.chatapp.service.NotificationService;

import lombok.RequiredArgsConstructor;

/**
 * Event listener that handles domain events related to users and chat views.
 * 
 * <p>
 * This component listens to application domain events and performs asynchronous
 * processing after database transactions have been committed. It handles user
 * onboarding and notification tasks in a non-blocking manner.
 * </p>
 * 
 * <p>
 * Key responsibilities:
 * </p>
 * <ul>
 * <li>Automatically adds newly registered users to the default chat view</li>
 * <li>Sends real-time notifications when users are added to chat views</li>
 * <li>Processes events asynchronously to avoid blocking main transaction
 * threads</li>
 * </ul>
 * 
 * <p>
 * Event Processing:
 * </p>
 * <ul>
 * <li>Uses {@code @Async} for non-blocking event handling</li>
 * <li>Uses {@code @TransactionalEventListener} with {@code AFTER_COMMIT}
 * phase</li>
 * <li>Executes only after the originating transaction successfully commits</li>
 * </ul>
 * 
 * @see UserCreatedEvent
 * @see UserAddedToChatViewEvent
 * @see NotificationService
 * @see ChatViewService
 */
@Component
@RequiredArgsConstructor
public class ChatViewEventListener {

    private static final Logger log = LoggerFactory.getLogger(ChatViewEventListener.class);

    private final NotificationService notificationService;
    private final ChatViewService chatViewService;
    private final UserRepository userRepository;

    /**
     * Handles user creation events by adding new users to the default chat view.
     * 
     * <p>
     * When a new user registers, this method automatically adds them to the
     * default chat view (ID: "1") to ensure they have immediate access to the
     * application's main communication channel.
     * </p>
     * 
     * <p>
     * Processing Characteristics:
     * </p>
     * <ul>
     * <li>Executes asynchronously in a separate thread pool</li>
     * <li>Only runs after the user registration transaction commits</li>
     * <li>Uses {@link ChatViewService#addUserToChatViewNoValidation} to bypass
     * permission checks</li>
     * <li>Logs errors without propagating them to avoid failing the
     * registration</li>
     * </ul>
     * 
     * @param event the user created event containing the user's email address
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    // @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleUserCreated(UserCreatedEvent event) {
        try {
            User user = userRepository.findUserByEmail(event.getUserEmail())
                    .orElseThrow(() -> new IllegalStateException("User not found: " + event.getUserEmail()));

            log.debug("Adding user to default chatview: " + user.getUid());
            chatViewService.addUserToChatViewNoValidation("1", user.getUid());
            log.info("Successfully added user {} to default chatview", user.getUid());
        } catch (Exception e) {
            log.error("Failed to add user to default chatview: {}", e.getMessage(), e);
        }
    }

    /**
     * Handles user-added-to-chat-view events by sending real-time notifications.
     * 
     * <p>
     * When a user is added to a chat view, this method sends a WebSocket
     * notification to inform the user about their new chat view membership.
     * This enables real-time UI updates without requiring the user to refresh.
     * </p>
     * 
     * <p>
     * Processing Characteristics:
     * </p>
     * <ul>
     * <li>Executes asynchronously in a separate thread pool</li>
     * <li>Only runs after the add-user transaction commits</li>
     * <li>Uses {@link NotificationService} to send WebSocket notifications</li>
     * <li>Logs errors without propagating them to avoid failing the main
     * operation</li>
     * </ul>
     * 
     * @param event the event containing the user UID and chat view ID
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserAddedToChatView(UserAddedToChatViewEvent event) {
        try {
            log.info("Sending notification for user {} added to chatview {}",
                    event.getUserUid(), event.getChatViewId());
            notificationService.notifyUserAddedToChatView(event.getUserUid(), event.getChatViewId());
        } catch (Exception e) {
            log.error("Failed to send notification: {}", e.getMessage(), e);
        }
    }
}
