package com.chatapp.chatapp.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chatapp.chatapp.dto.ChatViewRequest;
import com.chatapp.chatapp.dto.ChatViewResponse;
import com.chatapp.chatapp.entity.ChatView;
import com.chatapp.chatapp.entity.User;
import com.chatapp.chatapp.event.UserAddedToChatViewEvent;
import com.chatapp.chatapp.repository.ChatViewRepository;
import com.chatapp.chatapp.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service responsible for managing chat views (chat rooms/conversations).
 * 
 * <p>
 * This service handles the complete lifecycle of chat views including creation,
 * user membership management, and retrieval operations. It coordinates with
 * RabbitMQ
 * to set up message queues for users and publishes events for notification
 * purposes.
 * 
 * <p>
 * Key responsibilities include:
 * <ul>
 * <li>Creating new chat views with initial user membership</li>
 * <li>Adding and removing users from chat views</li>
 * <li>Managing RabbitMQ queues for user-specific message delivery</li>
 * <li>Publishing events when users are added to chat views</li>
 * <li>Retrieving chat view information and user membership data</li>
 * <li>Enforcing authorization checks for chat view operations</li>
 * </ul>
 * 
 */
@Service
@RequiredArgsConstructor
public class ChatViewService {

    private static final Logger log = LoggerFactory.getLogger(ChatViewService.class);

    private final ChatViewRepository chatViewRepository;
    private final UserRepository userRepository;
    private final RabbitMQService rabbitMQService;
    private final ApplicationEventPublisher eventPublisher;
    private final AuthUtilService authUtilService;

    /**
     * Creates a new chatview with the specified users.
     * 
     * <p>
     * The authenticated user is automatically added as the creator and first
     * member.
     * Additional users specified in the request are added after creation. For each
     * user,
     * a RabbitMQ queue is created for offline message delivery, and a
     * UserAddedToChatViewEvent
     * is published for notification purposes.
     * 
     * <p>
     * If any additional users fail to be added, the operation continues and logs a
     * warning
     * without rolling back the chatview creation.
     * 
     * @param request the chat view creation request containing the name and list of
     *                user UIDs
     * @throws IllegalStateException if the authenticated user cannot be retrieved
     */
    @Transactional
    public void createChatView(ChatViewRequest request) {
        ChatView chatView = new ChatView(request.getName());
        User creator = authUtilService.getAuthenticatedUser();

        // Add creator first
        chatView.addUser(creator);
        chatView = chatViewRepository.save(chatView);
        rabbitMQService.createUserQueueForChatView(chatView.getId(), creator.getUid());

        // Publish event instead of direct notification
        eventPublisher.publishEvent(new UserAddedToChatViewEvent(creator.getUid(), chatView.getId()));

        // Add other users
        for (String userUid : request.getUserUids()) {
            if (userUid.equals(creator.getUid())) {
                continue; // Skip if already added as creator
            }
            try {
                addUserToChatViewNoValidation(chatView.getId(), userUid);
            } catch (Exception e) {
                log.warn("Failed to add user {} to chatview: {}", userUid, e.getMessage());
            }
        }

        log.info("Created chatview {}", chatView.getId());
    }

    /**
     * Adds a user to a chatview with authorization check.
     * 
     * <p>
     * This method verifies that the authenticated user is already a member of the
     * chatview before allowing them to add another user. This ensures only existing
     * members can invite new users.
     * 
     * @param chatViewId the ID of the chatview to add the user to
     * @param userUid    the UID of the user to add
     * @throws AccessDeniedException if the authenticated user is not a member of
     *                               the chatview
     * @throws IllegalStateException if the chatview or user is not found
     */
    @Transactional
    public void addUserToChatView(String chatViewId, String userUid)
            throws AccessDeniedException, IllegalStateException {
        User authenticatedUser = authUtilService.getAuthenticatedUser();

        // Check if current user is a member of the chatview
        try {
            if (!isUserInChatView(chatViewId, authenticatedUser.getUid())) {
                throw new AccessDeniedException("user is not member of chatview " + chatViewId);
            }
        } catch (IllegalStateException e) {
            log.warn("Could not verify user membership for chatview {}: {}", chatViewId, e.getMessage());
            throw new AccessDeniedException("Cannot verify membership in chatview " + chatViewId);
        }

        addUserToChatViewNoValidation(chatViewId, userUid);
    }

    /**
     * Adds a user to a chatview without authorization check.
     * 
     * <p>
     * This method bypasses membership verification and is used internally during
     * chatview creation or by administrators. It creates a RabbitMQ queue for the
     * user
     * and publishes a UserAddedToChatViewEvent for notification.
     * 
     * <p>
     * Any exceptions during the operation are caught and logged as warnings without
     * propagating up the call stack.
     * 
     * @param chatViewId the ID of the chatview to add the user to
     * @param userUid    the UID of the user to add
     */
    @Transactional
    public void addUserToChatViewNoValidation(String chatViewId, String userUid) {
        try {
            ChatView chatView = chatViewRepository.findByIdWithUsers(chatViewId)
                    .orElseThrow(() -> new IllegalStateException("ChatView not found: " + chatViewId));

            User user = userRepository.findUserByUid(userUid)
                    .orElseThrow(() -> new IllegalStateException("User not found: " + userUid));

            chatView.addUser(user);
            chatViewRepository.save(chatView);

            rabbitMQService.createUserQueueForChatView(chatViewId, userUid);

            log.debug("WE GOT HERE " + chatView.getId() + " " + user.getUid());

            // Publish event instead of direct notification
            eventPublisher.publishEvent(new UserAddedToChatViewEvent(userUid, chatView.getId()));
        } catch (Exception e) {
            log.warn(e.getMessage());
        }
    }

    /**
     * Removes a user from a chatview.
     * 
     * <p>
     * This method removes the user's membership and deletes their associated
     * RabbitMQ queue to prevent accumulation of undeliverable messages.
     * 
     * @param chatViewId the ID of the chatview to remove the user from
     * @param userUid    the UID of the user to remove
     * @throws IllegalStateException if the chatview or user is not found
     */
    @Transactional
    public void removeUserFromChatView(String chatViewId, String userUid) {
        ChatView chatView = chatViewRepository.findByIdWithUsers(chatViewId)
                .orElseThrow(() -> new IllegalStateException("ChatView not found: " + chatViewId));

        User user = userRepository.findUserByUid(userUid)
                .orElseThrow(() -> new IllegalStateException("User not found: " + userUid));

        chatView.removeUser(user);
        chatViewRepository.save(chatView);

        // Delete RabbitMQ queue for user
        rabbitMQService.deleteUserQueueForChatView(chatViewId, userUid);

        log.info("Removed user {} from chatview {}", userUid, chatViewId);
    }

    /**
     * Gets all chatviews for a user.
     * 
     * <p>
     * Retrieves all chatviews where the specified user is a member and converts
     * them to response DTOs containing chatview details, member avatars, and
     * message counts.
     * 
     * @param userUid the UID of the user whose chatviews to retrieve
     * @return list of ChatViewResponse objects representing the user's chatviews
     */
    @Transactional(readOnly = true)
    public List<ChatViewResponse> getChatViewsForUser(String userUid) {
        List<ChatView> chatViews = chatViewRepository.findChatViewsByUserUid(userUid);
        return chatViews.stream()
                .map(this::mapToChatViewResponse)
                .collect(Collectors.toList());
    }

    /**
     * Gets a specific chatview by ID.
     * 
     * <p>
     * Retrieves a chatview with all its user members and converts it to a response
     * DTO.
     * 
     * @param chatViewId the ID of the chatview to retrieve
     * @return ChatViewResponse object containing the chatview details
     * @throws IllegalStateException if the chatview is not found
     */
    @Transactional(readOnly = true)
    public ChatViewResponse getChatViewById(String chatViewId) {
        ChatView chatView = chatViewRepository.findByIdWithUsers(chatViewId)
                .orElseThrow(() -> new IllegalStateException("ChatView not found: " + chatViewId));
        return mapToChatViewResponse(chatView);
    }

    /**
     * Checks if a user is a member of a chatview.
     * 
     * @param chatViewId the ID of the chatview to check
     * @param userUid    the UID of the user to check
     * @return true if the user is a member of the chatview, false otherwise
     * @throws IllegalStateException if the chatview is not found
     */
    public boolean isUserInChatView(String chatViewId, String userUid) {
        ChatView chatView = chatViewRepository.findByIdWithUsers(chatViewId)
                .orElseThrow(() -> new IllegalStateException("ChatView not found: " + chatViewId));
        return chatView.getUsers().stream()
                .anyMatch(user -> user.getUid().equals(userUid));
    }

    /**
     * Gets all user UIDs in a specific chatview.
     * 
     * @param chatViewId the ID of the chatview
     * @return set of user UIDs who are members of the chatview
     * @throws IllegalStateException if the chatview is not found
     */
    public Set<String> getUserUidsInChatView(String chatViewId) {
        ChatView chatView = chatViewRepository.findByIdWithUsers(chatViewId)
                .orElseThrow(() -> new IllegalStateException("ChatView not found: " + chatViewId));
        return chatView.getUsers().stream()
                .map(User::getUid)
                .collect(Collectors.toSet());
    }

    private ChatViewResponse mapToChatViewResponse(ChatView chatView) {
        Map<String, String> userAvatars = chatView.getUsers().stream()
                .collect(Collectors.toMap(
                        User::getUid,
                        user -> user.getAvatarLink() != null ? user.getAvatarLink() : ""));

        return new ChatViewResponse(
                chatView.getId(),
                chatView.getName(),
                userAvatars,
                chatView.getMessages().size());
    }
}
