package com.chatapp.chatapp.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.chatapp.chatapp.event.UserAddedToChatViewEvent;
import com.chatapp.chatapp.service.NotificationService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ChatViewEventListener {

    private static final Logger log = LoggerFactory.getLogger(ChatViewEventListener.class);

    private final NotificationService notificationService;

    /**
     * Sends notification AFTER transaction commits
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserAddedToChatView(UserAddedToChatViewEvent event) {
        log.info("Sending notification after commit for user {} added to chatview {}",
                event.getUserUid(), event.getChatViewId());

        notificationService.notifyUserAddedToChatView(event.getUserUid(), event.getChatViewId());
    }
}
