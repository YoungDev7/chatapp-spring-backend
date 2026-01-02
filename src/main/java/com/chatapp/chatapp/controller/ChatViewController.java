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

/**
 * REST controller for managing chat views and messages.
 * 
 * <p>
 * This controller provides endpoints for creating and managing chat views
 * (conversations),
 * sending and receiving messages, and managing chat view membership. It
 * integrates with
 * WebSocket messaging for real-time communication and RabbitMQ for message
 * queuing.
 * </p>
 * 
 * <p>
 * Base path: {@code /api/v1/chatviews}
 * </p>
 * 
 * <p>
 * Key features:
 * </p>
 * <ul>
 * <li>Create and retrieve chat views</li>
 * <li>Add and remove users from chat views</li>
 * <li>Send messages via WebSocket</li>
 * <li>Retrieve messages from database and message queues</li>
 * <li>Manage message delivery through RabbitMQ queues</li>
 * </ul>
 * 
 * <p>
 * Security: All endpoints require authentication. Access to chat views is
 * restricted
 * to members only, enforced through permission checks.
 * </p>
 * 
 */
@RestController
@RequestMapping("/api/v1/chatviews")
@RequiredArgsConstructor
public class ChatViewController {

    private static final Logger log = LoggerFactory.getLogger(ChatViewController.class);

    private final ChatViewService chatViewService;
    private final MessageService messageService;
    private final RabbitMQService rabbitMQService;

    /**
     * Creates a new chat view (conversation).
     * 
     * <p>
     * HTTP Method: POST
     * </p>
     * <p>
     * Path: {@code /api/v1/chatviews}
     * </p>
     * 
     * <p>
     * Creates a new chat view with the specified participants and properties.
     * The chat view serves as a conversation container for multiple users to
     * exchange messages.
     * </p>
     * 
     * <p>
     * Request body format (example):
     * </p>
     * 
     * <pre>
     * {
     *   "name": "Team Discussion",
     *   "userUids": ["user1-uid", "user2-uid", "user3-uid"]
     * }
     * </pre>
     * 
     * @param request The chat view creation request containing chat view details
     *                and initial participants
     * @return ResponseEntity with 201 Created status on successful creation
     */
    @PostMapping
    public ResponseEntity<?> createChatView(@RequestBody ChatViewRequest request) {

        chatViewService.createChatView(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Retrieves all chat views for the authenticated user.
     * 
     * <p>
     * HTTP Method: GET
     * </p>
     * <p>
     * Path: {@code /api/v1/chatviews}
     * </p>
     * 
     * <p>
     * Returns a list of all chat views where the authenticated user is a member.
     * Each chat view includes basic information such as ID, name, and participant
     * list.
     * </p>
     * 
     * <p>
     * Response format (example):
     * </p>
     * 
     * <pre>
     * [
     *   {
     *     "chatViewId": "cv123",
     *     "name": "Team Discussion",
     *     "userUids": ["user1-uid", "user2-uid"],
     *     "createdAt": "2025-12-31T10:00:00Z"
     *   },
     *   ...
     * ]
     * </pre>
     * 
     * @param authentication The authentication object containing the current user's
     *                       details
     * @return ResponseEntity containing a list of ChatViewResponse objects (200 OK)
     */
    @GetMapping
    public ResponseEntity<List<ChatViewResponse>> getUserChatViews(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        List<ChatViewResponse> chatViews = chatViewService.getChatViewsForUser(user.getUid());

        log.info("Retrieved {} chatviews for user {}", chatViews.size(), user.getUid());
        return ResponseEntity.ok(chatViews);
    }

    /**
     * Retrieves a specific chat view by its unique identifier.
     * 
     * <p>
     * HTTP Method: GET
     * </p>
     * <p>
     * Path: {@code /api/v1/chatviews/{chatViewId}}
     * </p>
     * 
     * <p>
     * Returns detailed information about a specific chat view. Access is restricted
     * to users who are members of the chat view.
     * </p>
     * 
     * <p>
     * Response format (example):
     * </p>
     * 
     * <pre>
     * {
     *   "chatViewId": "cv123",
     *   "name": "Team Discussion",
     *   "userUids": ["user1-uid", "user2-uid"],
     *   "createdAt": "2025-12-31T10:00:00Z"
     * }
     * </pre>
     * 
     * @param chatViewId     The unique identifier of the chat view to retrieve
     * @param authentication The authentication object containing the current user's
     *                       details
     * @return ResponseEntity containing the ChatViewResponse (200 OK) if user is a
     *         member,
     *         or 403 Forbidden if user is not authorized to access this chat view
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
     * Retrieves all messages in a chat view from the database and purges the user's
     * RabbitMQ queue.
     * 
     * <p>
     * HTTP Method: GET
     * </p>
     * <p>
     * Path: {@code /api/v1/chatviews/{chatViewId}/messages}
     * </p>
     * 
     * <p>
     * This endpoint performs two operations:
     * </p>
     * <ol>
     * <li>Fetches all persisted messages for the chat view from the database</li>
     * <li>Purges (empties) the user's RabbitMQ queue for this chat view without
     * returning queued messages</li>
     * </ol>
     * 
     * <p>
     * The returned messages are from the database only. Messages in the queue are
     * discarded
     * to synchronize the user's message state. This is typically used when a user
     * opens a chat
     * view to get the complete message history.
     * </p>
     * 
     * <p>
     * Response format (example):
     * </p>
     * 
     * <pre>
     * [
     *   {
     *     "messageId": "msg123",
     *     "text": "Hello everyone!",
     *     "senderUid": "user1-uid",
     *     "chatViewId": "cv123",
     *     "createdAt": "2025-12-31T10:30:00Z"
     *   },
     *   ...
     * ]
     * </pre>
     * 
     * @param chatViewId     The unique identifier of the chat view
     * @param authentication The authentication object containing the current user's
     *                       details
     * @return ResponseEntity containing a list of MessageResponse objects from the
     *         database (200 OK)
     *         if user is a member, or 403 Forbidden if user is not authorized to
     *         access this chat view
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

        // Purge user's queue for this chatview (queue messages are ignored, we return
        // DB messages)
        rabbitMQService.purgeMessagesFromQueue(chatViewId, user.getUid());

        log.info("Retrieved {} DB messages and purged queue for user {} in chatview {}",
                dbMessages.size(), user.getUid(), chatViewId);
        return ResponseEntity.ok(dbMessages);
    }

    /**
     * Retrieves and removes all messages from the user's RabbitMQ queue for a
     * specific chat view.
     * 
     * <p>
     * HTTP Method: GET
     * </p>
     * <p>
     * Path: {@code /api/v1/chatviews/{chatViewId}/messages/queue}
     * </p>
     * 
     * <p>
     * This endpoint retrieves messages that were queued for the user while they
     * were offline
     * or had not yet fetched them. The queue is completely emptied after retrieval,
     * ensuring
     * messages are not delivered multiple times.
     * </p>
     * 
     * <p>
     * This is typically used for incremental message updates when a user is
     * actively
     * viewing a chat, fetching only new messages that arrived since the last check.
     * </p>
     * 
     * <p>
     * Response format (example):
     * </p>
     * 
     * <pre>
     * [
     *   {
     *     "messageId": "msg456",
     *     "text": "New message!",
     *     "senderUid": "user2-uid",
     *     "chatViewId": "cv123",
     *     "createdAt": "2025-12-31T11:00:00Z"
     *   },
     *   ...
     * ]
     * </pre>
     * 
     * @param chatViewId     The unique identifier of the chat view
     * @param authentication The authentication object containing the current user's
     *                       details
     * @return ResponseEntity containing a list of MessageResponse objects from the
     *         queue (200 OK)
     *         if user is a member, or 403 Forbidden if user is not authorized to
     *         access this chat view
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
     * Adds a user to an existing chat view.
     * 
     * <p>
     * HTTP Method: POST
     * </p>
     * <p>
     * Path: {@code /api/v1/chatviews/{chatViewId}/users/{userUid}}
     * </p>
     * 
     * <p>
     * Adds a new participant to the chat view, allowing them to view messages
     * and participate in the conversation. The user will have access to all future
     * messages and can retrieve the chat view's message history.
     * </p>
     * 
     * @param chatViewId The unique identifier of the chat view
     * @param userUid    The unique identifier of the user to add
     * @return ResponseEntity with 200 OK status on successful addition,
     *         or 403 Forbidden if the operation fails
     * @throws Exception if there's an error adding the user to the chat view
     */
    @PostMapping("/{chatViewId}/users/{userUid}")
    public ResponseEntity<Void> addUserToChatView(
            @PathVariable String chatViewId,
            @PathVariable String userUid) {

        try {
            chatViewService.addUserToChatView(chatViewId, userUid);
            log.info("Added user {} to chatview {}", userUid, chatViewId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

    }

    /**
     * Removes a user from a chat view.
     * 
     * <p>
     * HTTP Method: DELETE
     * </p>
     * <p>
     * Path: {@code /api/v1/chatviews/{chatViewId}/users/{userUid}}
     * </p>
     * 
     * <p>
     * Removes a participant from the chat view. The user will no longer have access
     * to new messages in this conversation. Users can remove themselves from a chat
     * view,
     * or existing members can remove other participants.
     * </p>
     * 
     * <p>
     * Authorization: The requesting user must either be:
     * </p>
     * <ul>
     * <li>A member of the chat view (to remove others)</li>
     * <li>The user being removed (to remove themselves)</li>
     * </ul>
     * 
     * @param chatViewId     The unique identifier of the chat view
     * @param userUid        The unique identifier of the user to remove
     * @param authentication The authentication object containing the current user's
     *                       details
     * @return ResponseEntity with 200 OK status on successful removal,
     *         or 403 Forbidden if the requesting user is not authorized
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
     * Handles incoming WebSocket messages for a specific chat view.
     * 
     * <p>
     * WebSocket Destination: {@code /app/chatview/{chatViewId}}
     * </p>
     * 
     * <p>
     * This method processes messages sent via WebSocket connections. When a message
     * is received, it performs the following operations:
     * </p>
     * <ol>
     * <li>Persists the message to the database for history</li>
     * <li>Distributes the message to all chat view members via:
     * <ul>
     * <li>Direct WebSocket delivery to online users</li>
     * <li>RabbitMQ queue storage for offline users</li>
     * </ul>
     * </li>
     * </ol>
     * 
     * <p>
     * Message payload format (example):
     * </p>
     * 
     * <pre>
     * {
     *   "text": "Hello everyone!",
     *   "createdAt": "2025-12-31T10:30:00Z"
     * }
     * </pre>
     * 
     * <p>
     * The sender is automatically determined from the authenticated principal.
     * </p>
     * 
     * @param chatViewId     The unique identifier of the chat view (from URL path
     *                       variable)
     * @param messageRequest The message payload containing text and timestamp
     * @param principal      The authenticated user's principal (provides sender
     *                       information)
     */
    @MessageMapping("/chatview/{chatViewId}")
    public void handleChatViewMessage(
            @DestinationVariable String chatViewId,
            @Payload MessageRequest messageRequest,
            Principal principal) {
        // Post message - this will save to DB and send via WS or stored in RabbitMQ
        // queue
        messageService.postMessageToChatView(
                messageRequest.getText(),
                chatViewId,
                messageRequest.getCreatedAt(),
                principal);
    }
}
