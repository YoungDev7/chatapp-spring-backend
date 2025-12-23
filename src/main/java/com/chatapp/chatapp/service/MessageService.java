package com.chatapp.chatapp.service;

import java.security.Principal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chatapp.chatapp.dto.MessageResponse;
import com.chatapp.chatapp.entity.ChatView;
import com.chatapp.chatapp.entity.Message;
import com.chatapp.chatapp.entity.User;
import com.chatapp.chatapp.repository.ChatViewRepository;
import com.chatapp.chatapp.repository.MessageRepository;
import com.chatapp.chatapp.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MessageService {

    private static final Logger log = LoggerFactory.getLogger(MessageService.class);

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ChatViewRepository chatViewRepository;
    private final RabbitMQService rabbitMQService;
    private final AuthService authService;
    private final ChatViewService chatViewService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserSessionService userSessionService;

    /**
     * Posts a new message to a chatview
     * Saves to database, delivers to online users via WebSocket,
     * and queues for offline users in RabbitMQ
     */
    @Transactional
    public void postMessageToChatView(String text, String chatViewId, ZonedDateTime createdAt, Principal principal) {

        User sender = authService.getAuthenticatedUserFromStompHeader(principal);

        ChatView chatView = chatViewRepository.findByIdWithUsers(chatViewId)
                .orElseThrow(() -> new IllegalStateException("ChatView not found: " + chatViewId));

        // Verify user is member of chatview
        if (!chatViewService.isUserInChatView(chatViewId, sender.getUid())) {
            log.error("User {} attempted to send message to chatview {} without membership",
                    sender.getUid(), chatViewId);
            throw new AccessDeniedException("User not authorized for chatview: " + chatViewId);
        }

        Message message = new Message(text, sender, chatView, createdAt);
        message = messageRepository.save(message);

        MessageResponse response = new MessageResponse(
                message.getText(),
                sender.getName(),
                sender.getUid(),
                chatViewId,
                message.getCreatedAt());

        // Get all users in this chatview
        Set<String> userUidsInChatView = chatView.getUsers().stream()
                .map(User::getUid)
                .collect(Collectors.toSet());

        // Deliver message to all users (online via WebSocket, offline via RabbitMQ
        // queue)
        deliverMessageToUsers(response, userUidsInChatView, chatViewId);

        log.info("Posted message {} to chatview {} by user {}", message.getId(), chatViewId, sender.getUid());
    }

    /**
     * Gets all messages for a specific chatview
     */
    public List<MessageResponse> getMessagesByChatView(String chatViewId) {
        List<Message> messages = messageRepository.findByChatViewId(chatViewId);
        return messages.stream()
                .map(this::mapToMessageResponse)
                .collect(Collectors.toList());
    }

    // delete message
    public void deleteMessage(Long id) {
        if (messageRepository.existsById(id)) {
            messageRepository.deleteById(id);
        } else {
            throw new IllegalStateException("message not found in database, ID:" + id);
        }
    }

    /**
     * Delivers a message to all users in a chatview.
     * Online users receive via WebSocket immediately using user-specific
     * destinations.
     * Offline users receive via RabbitMQ queue (for later delivery).
     */
    private void deliverMessageToUsers(MessageResponse message, Set<String> userUids, String chatViewId) {
        for (String userUid : userUids) {
            try {
                boolean isOnline = userSessionService.isUserOnline(userUid);

                if (isOnline) {
                    // User is online - deliver immediately via WebSocket using user-specific
                    // destination
                    // Spring automatically sends to /user/{userUid}/queue/chatview/{chatViewId}
                    // Only the authenticated user can subscribe to their own /user destinations
                    String destination = "/queue/chatview/" + chatViewId;
                    messagingTemplate.convertAndSendToUser(userUid, destination, message);
                    log.debug("Delivered message to ONLINE user {} in chatview {} via WebSocket",
                            userUid, chatViewId);
                } else {
                    // User is offline - send to RabbitMQ queue for later delivery
                    rabbitMQService.sendMessageToUserQueue(chatViewId, userUid, message);
                    log.debug("Queued message for OFFLINE user {} in chatview {} in RabbitMQ",
                            userUid, chatViewId);
                }

            } catch (Exception e) {
                log.error("Error delivering message to user {} in chatview {}: {}",
                        userUid, chatViewId, e.getMessage());
            }
        }
    }

    private MessageResponse mapToMessageResponse(Message message) {
        return new MessageResponse(
                message.getText(),
                message.getSender().getName(),
                message.getSender().getUid(),
                message.getChatView().getId(),
                message.getCreatedAt());
    }
}
