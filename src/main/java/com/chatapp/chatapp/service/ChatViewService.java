package com.chatapp.chatapp.service;

import java.util.List;
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
    
    /**
     * Creates a new chatview
     */
    @Transactional
    public ChatViewResponse createChatView(ChatViewRequest request, String creatorUid) {
        ChatView chatView = new ChatView(request.getName());
        
        User creator = userRepository.findUserByUid(creatorUid)
            .orElseThrow(() -> new IllegalStateException("User not found: " + creatorUid));
        
        chatView.addUser(creator);
        chatView = chatViewRepository.save(chatView);
        
        // Create RabbitMQ queue for creator
        rabbitMQService.createUserQueueForChatView(chatView.getId(), creatorUid);
        
        log.info("Created chatview {} by user {}", chatView.getId(), creatorUid);
        
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
        Set<String> userUids = chatView.getUsers().stream()
            .map(User::getUid)
            .collect(Collectors.toSet());
        
        return new ChatViewResponse(
            chatView.getId(),
            chatView.getName(),
            userUids,
            chatView.getMessages().size()
        );
    }
}
