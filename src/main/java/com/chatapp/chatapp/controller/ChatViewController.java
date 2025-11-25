package com.chatapp.chatapp.controller;

import java.security.Principal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chatapp.chatapp.dto.ChatViewRequest;
import com.chatapp.chatapp.dto.ChatViewResponse;
import com.chatapp.chatapp.dto.MessageRequest;
import com.chatapp.chatapp.dto.MessageResponse;
import com.chatapp.chatapp.entity.User;
import com.chatapp.chatapp.service.ChatViewService;
import com.chatapp.chatapp.service.MessageService;
import com.chatapp.chatapp.service.RabbitMQService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/chatviews")
@RequiredArgsConstructor
public class ChatViewController {
    
    private static final Logger log = LoggerFactory.getLogger(ChatViewController.class);
    
    private final ChatViewService chatViewService;
    private final MessageService messageService;
    private final RabbitMQService rabbitMQService;
    
    /**
     * Creates a new chatview
     */
    @PostMapping
    public ResponseEntity<ChatViewResponse> createChatView(@RequestBody ChatViewRequest request) {
        
        ChatViewResponse response = chatViewService.createChatView(request);
        
        log.info("Created chatview {}", response.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Gets all chatviews for the authenticated user
     */
    @GetMapping
    public ResponseEntity<List<ChatViewResponse>> getUserChatViews(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        List<ChatViewResponse> chatViews = chatViewService.getChatViewsForUser(user.getUid());
        
        log.info("Retrieved {} chatviews for user {}", chatViews.size(), user.getUid());
        return ResponseEntity.ok(chatViews);
    }
    
    /**
     * Gets a specific chatview by ID
     */
    @GetMapping("/{chatViewId}")
    public ResponseEntity<ChatViewResponse> getChatView(
            @PathVariable String chatViewId,
            Authentication authentication) {
        
        User user = (User) authentication.getPrincipal();
        
        // Check if user is a member of the chatview
        if (!chatViewService.isUserInChatView(chatViewId, user.getUid())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        ChatViewResponse response = chatViewService.getChatViewById(chatViewId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Gets all messages in a chatview from database AND purges user's RabbitMQ queue
     * Returns all DB messages + any messages that were in queue (which are now removed)
     */
    @GetMapping("/{chatViewId}/messages")
    public ResponseEntity<List<MessageResponse>> getAllMessagesFromDBAndPurgeQueue(
            @PathVariable String chatViewId,
            Authentication authentication) {
        
        User user = (User) authentication.getPrincipal();
        
        // Check if user is a member of the chatview
        if (!chatViewService.isUserInChatView(chatViewId, user.getUid())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        // Get all messages from database
        List<MessageResponse> dbMessages = messageService.getMessagesByChatView(chatViewId);
        
        // Purge user's queue for this chatview (queue messages are ignored, we return DB messages)
        rabbitMQService.purgeMessagesFromQueue(chatViewId, user.getUid());
        
        log.info("Retrieved {} DB messages and purged queue for user {} in chatview {}", 
            dbMessages.size(), user.getUid(), chatViewId);
        return ResponseEntity.ok(dbMessages);
    }
    
    /**
     * Gets only the messages that are in user's RabbitMQ queue for this chatview
     * and empties the queue (removes all messages from queue)
     */
    @GetMapping("/{chatViewId}/messages/queue")
    public ResponseEntity<List<MessageResponse>> getQueueMessages(
            @PathVariable String chatViewId,
            Authentication authentication) {
        
        User user = (User) authentication.getPrincipal();
        
        // Check if user is a member of the chatview
        if (!chatViewService.isUserInChatView(chatViewId, user.getUid())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        // Get and purge messages from user's queue
        List<MessageResponse> queueMessages = rabbitMQService.getAndPurgeMessagesFromQueue(
            chatViewId, user.getUid());
        
        log.info("Retrieved and purged {} queue messages for user {} in chatview {}", 
            queueMessages.size(), user.getUid(), chatViewId);
        return ResponseEntity.ok(queueMessages);
    }
    
    /**
     * Adds a user to a chatview
     */
    @PostMapping("/{chatViewId}/users/{userUid}")
    public ResponseEntity<Void> addUserToChatView(
            @PathVariable String chatViewId,
            @PathVariable String userUid,
            Authentication authentication) {
        
        User currentUser = (User) authentication.getPrincipal();
        
        // Check if current user is a member of the chatview
        if (!chatViewService.isUserInChatView(chatViewId, currentUser.getUid())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        chatViewService.addUserToChatView(chatViewId, userUid);
        log.info("Added user {} to chatview {} by user {}", userUid, chatViewId, currentUser.getUid());
        return ResponseEntity.ok().build();
    }
    
    /**
     * Removes a user from a chatview
     */
    @DeleteMapping("/{chatViewId}/users/{userUid}")
    public ResponseEntity<Void> removeUserFromChatView(
            @PathVariable String chatViewId,
            @PathVariable String userUid,
            Authentication authentication) {
        
        User currentUser = (User) authentication.getPrincipal();
        
        // Check if current user is a member of the chatview or removing themselves
        if (!chatViewService.isUserInChatView(chatViewId, currentUser.getUid()) 
                && !currentUser.getUid().equals(userUid)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        chatViewService.removeUserFromChatView(chatViewId, userUid);
        log.info("Removed user {} from chatview {}", userUid, chatViewId);
        return ResponseEntity.ok().build();
    }
    
    /**
     * Handles incoming WebSocket messages for a specific chatview
     * Messages are stored in DB and sent to RabbitMQ for distribution
     */
    @MessageMapping("/chatview/{chatViewId}")
    public void handleChatViewMessage(
            @DestinationVariable String chatViewId,
            @Payload MessageRequest messageRequest,
            Principal principal
    ) {
        // Post message - this will save to DB and send via WS or stored in RabbitMQ queue
        messageService.postMessageToChatView(
            messageRequest.getText(),
            chatViewId,
            messageRequest.getCreatedAt(),
            principal
        );
    }
}
