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

@Component
@RequiredArgsConstructor
public class ChatViewEventListener {

    private static final Logger log = LoggerFactory.getLogger(ChatViewEventListener.class);

    private final NotificationService notificationService;
    private final ChatViewService chatViewService;
    private final UserRepository userRepository;

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
