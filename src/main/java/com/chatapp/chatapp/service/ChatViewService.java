package com.chatapp.chatapp.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chatapp.chatapp.dto.ChatViewRequest;
import com.chatapp.chatapp.dto.ChatViewResponse;
import com.chatapp.chatapp.entity.ChatView;
import com.chatapp.chatapp.entity.User;
import com.chatapp.chatapp.repository.ChatViewRepository;
import com.chatapp.chatapp.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatViewService {
    
    private static final Logger log = LoggerFactory.getLogger(ChatViewService.class);
    
    private final ChatViewRepository chatViewRepository;
    private final UserRepository userRepository;
    private final RabbitMQService rabbitMQService;
    private final AuthService authService;

    /**
     * Creates a new chatview
     */
    @Transactional
    public ChatViewResponse createChatView(ChatViewRequest request) {
        ChatView chatView = new ChatView(request.getName());
        User creator = authService.getAuthenticatedUser();

        // Add creator first
        chatView.addUser(creator);

        // Add other users
        for(String userUid : request.getUserUids()){
            if (userUid.equals(creator.getUid())) {
                continue; // Skip if already added as creator
            }
            try{
                User member = userRepository.findUserByUid(userUid)
                    .orElseThrow(() -> new IllegalStateException("User not found: " + userUid));
                chatView.addUser(member);
            } catch(Exception e){
                log.warn("Failed to add user {} to chatview: {}", userUid, e.getMessage());
            }
        }

        // Save to generate ID
        chatView = chatViewRepository.save(chatView);
        
        // Now create queues with valid chatView ID
        for(User user : chatView.getUsers()) {
            try {
                rabbitMQService.createUserQueueForChatView(chatView.getId(), user.getUid());
            } catch(Exception e) {
                log.error("Failed to create queue for user {} in chatview {}: {}", 
                    user.getUid(), chatView.getId(), e.getMessage());
            }
        }
        
        log.info("Created chatview {}", chatView.getId());
        
        return mapToChatViewResponse(chatView);
    }
    
    /**
     * Adds a user to a chatview
     */
    @Transactional
    public void addUserToChatView(String chatViewId, String userUid) {
        ChatView chatView = chatViewRepository.findByIdWithUsers(chatViewId)
            .orElseThrow(() -> new IllegalStateException("ChatView not found: " + chatViewId));
        
        User user = userRepository.findUserByUid(userUid)
            .orElseThrow(() -> new IllegalStateException("User not found: " + userUid));
        
        chatView.addUser(user);
        chatViewRepository.save(chatView);
        
        // Create RabbitMQ queue for new user
        rabbitMQService.createUserQueueForChatView(chatViewId, userUid);
        
        log.info("Added user {} to chatview {}", userUid, chatViewId);
    }
    
    /**
     * Removes a user from a chatview
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
     * Gets all chatviews for a user
     */
    @Transactional(readOnly = true)
    public List<ChatViewResponse> getChatViewsForUser(String userUid) {
        List<ChatView> chatViews = chatViewRepository.findChatViewsByUserUid(userUid);
        return chatViews.stream()
            .map(this::mapToChatViewResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Gets a specific chatview by ID
     */
    @Transactional(readOnly = true)
    public ChatViewResponse getChatViewById(String chatViewId) {
        ChatView chatView = chatViewRepository.findByIdWithUsers(chatViewId)
            .orElseThrow(() -> new IllegalStateException("ChatView not found: " + chatViewId));
        return mapToChatViewResponse(chatView);
    }
    
    /**
     * Checks if a user is a member of a chatview
     */
    public boolean isUserInChatView(String chatViewId, String userUid) {
        ChatView chatView = chatViewRepository.findByIdWithUsers(chatViewId)
            .orElseThrow(() -> new IllegalStateException("ChatView not found: " + chatViewId));
        return chatView.getUsers().stream()
            .anyMatch(user -> user.getUid().equals(userUid));
    }
    
    /**
     * Gets all user UIDs in a specific chatview
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
                user -> user.getAvatarLink() != null ? user.getAvatarLink() : ""
            ));
        
        return new ChatViewResponse(
            chatView.getId(),
            chatView.getName(),
            userAvatars,
            chatView.getMessages().size()
        );
    }
}
