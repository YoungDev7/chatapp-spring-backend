package com.chatapp.chatapp.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import com.chatapp.chatapp.config.RabbitMQConfig;
import com.chatapp.chatapp.dto.MessageResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RabbitMQService {
    
    private static final Logger log = LoggerFactory.getLogger(RabbitMQService.class);
    
    private final RabbitTemplate rabbitTemplate;
    private final AmqpAdmin amqpAdmin;
    private final TopicExchange chatExchange;
    private final RabbitMQConfig rabbitMQConfig;
    private final ObjectMapper objectMapper;
    
    /**
     * Creates a queue for a specific user in a chatview
     */
    public void createUserQueueForChatView(String chatViewId, String userUid) {
        try {
            Queue queue = rabbitMQConfig.createUserChatViewQueue(chatViewId, userUid);
            amqpAdmin.declareQueue(queue);
            
            Binding binding = rabbitMQConfig.createUserChatViewBinding(queue, chatExchange, chatViewId);
            amqpAdmin.declareBinding(binding);
            
            log.info("Created queue and binding for user {} in chatview {}", userUid, chatViewId);
        } catch (Exception e) {
            log.error("Error creating queue for user {} in chatview {}: {}", userUid, chatViewId, e.getMessage());
            throw new RuntimeException("Failed to create queue for user", e);
        }
    }
    
    /**
     * Deletes a user's queue for a specific chatview
     */
    public void deleteUserQueueForChatView(String chatViewId, String userUid) {
        try {
            String queueName = RabbitMQConfig.getQueueName(chatViewId, userUid);
            amqpAdmin.deleteQueue(queueName);
            log.info("Deleted queue for user {} in chatview {}", userUid, chatViewId);
        } catch (Exception e) {
            log.error("Error deleting queue for user {} in chatview {}: {}", userUid, chatViewId, e.getMessage());
        }
    }
    
    
    /**
     * Sends a message to a specific user's queue in a chatview
     * Used for offline users to queue messages for later delivery
     */
    public void sendMessageToUserQueue(String chatViewId, String userUid, MessageResponse messageResponse) {
        try {
            String queueName = RabbitMQConfig.getQueueName(chatViewId, userUid);
            rabbitTemplate.convertAndSend(queueName, messageResponse);
            log.info("Sent message directly to queue {} for offline user {}", 
                queueName, userUid);
        } catch (Exception e) {
            log.error("Error sending message to user queue for user {} in chatview {}: {}", 
                userUid, chatViewId, e.getMessage());
            throw new RuntimeException("Failed to send message to user queue", e);
        }
    }
    
    /**
     * Purges all messages from a user's queue in a chatview
     */
    public void purgeMessagesFromQueue(String chatViewId, String userUid) {
        try {
            String queueName = RabbitMQConfig.getQueueName(chatViewId, userUid);
            amqpAdmin.purgeQueue(queueName);
            log.info("Purged queue for user {} in chatview {}", userUid, chatViewId);
        } catch (Exception e) {
            log.error("Error purging queue for user {} in chatview {}: {}", userUid, chatViewId, e.getMessage());
        }
    }

    /**
     * Returns all messages from a user's queue in a specific chatview and purges
     * the queue
     */
    public List<MessageResponse> getAndPurgeMessagesFromQueue(String chatViewId, String userUid) {
        String queueName = RabbitMQConfig.getQueueName(chatViewId, userUid);
        List<MessageResponse> messages = new ArrayList<>();
        
        try {
            Message message;
            while ((message = rabbitTemplate.receive(queueName, 100)) != null) {
                try {
                    MessageResponse messageResponse = objectMapper.readValue(
                        message.getBody(), 
                        MessageResponse.class
                    );
                    messages.add(messageResponse);
                } catch (Exception e) {
                    log.error("Error processing message from queue {}: {}", queueName, e.getMessage());
                }
            }
            
            if (!messages.isEmpty()) {
                log.info("Retrieved and purged {} messages from queue for user {} in chatview {}", 
                    messages.size(), userUid, chatViewId);
            }
            
        } catch (Exception e) {
            log.error("Error consuming messages from queue {}: {}", queueName, e.getMessage());
        }
        
        return messages;
    }

    /**
     * Checks if a queue exists for a user in a chatview
     */
    public boolean queueExists(String chatViewId, String userUid) {
        try {
            String queueName = RabbitMQConfig.getQueueName(chatViewId, userUid);
            var properties = amqpAdmin.getQueueProperties(queueName);
            return properties != null;
        } catch (Exception e) {
            return false;
        }
    }
}
