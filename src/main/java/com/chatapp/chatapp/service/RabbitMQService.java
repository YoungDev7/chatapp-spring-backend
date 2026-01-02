package com.chatapp.chatapp.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import com.chatapp.chatapp.config.RabbitMQConfig;
import com.chatapp.chatapp.dto.MessageResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/**
 * Service responsible for managing RabbitMQ message queues and routing.
 * 
 * <p>
 * This service handles the creation, deletion, and management of user-specific
 * RabbitMQ queues for chatview message delivery. It provides the messaging
 * infrastructure
 * for storing messages intended for offline users until they reconnect.
 * 
 * <p>
 * Key responsibilities include:
 * <ul>
 * <li>Creating and binding user-specific queues for each chatview</li>
 * <li>Sending messages to offline users' queues</li>
 * <li>Retrieving and purging queued messages when users come online</li>
 * <li>Managing queue lifecycle (creation, deletion, purging)</li>
 * <li>Checking queue existence and properties</li>
 * </ul>
 * 
 * <p>
 * Each user in a chatview has their own dedicated queue following the naming
 * convention: chatview.{chatViewId}.user.{userUid}
 * 
 */
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
     * Creates a queue for a specific user in a chatview.
     * 
     * <p>
     * This method creates a durable queue and binds it to the chat exchange
     * with a routing key specific to the chatview. The queue will persist messages
     * even if the RabbitMQ server restarts.
     * 
     * @param chatViewId the ID of the chatview
     * @param userUid    the UID of the user for whom to create the queue
     * @throws RuntimeException if queue or binding creation fails
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
     * Deletes a user's queue for a specific chatview.
     * 
     * <p>
     * This method should be called when a user is removed from a chatview
     * to clean up resources and prevent message accumulation.
     * 
     * @param chatViewId the ID of the chatview
     * @param userUid    the UID of the user whose queue should be deleted
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
     * Sends a message to a specific user's queue in a chatview.
     * 
     * <p>
     * This method is used for offline users to queue messages for later delivery.
     * Messages are sent directly to the user's queue and will be retrieved when
     * the user comes online.
     * 
     * @param chatViewId      the ID of the chatview
     * @param userUid         the UID of the offline user
     * @param messageResponse the message to queue
     * @throws RuntimeException if sending the message fails
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
     * Purges all messages from a user's queue in a chatview.
     * 
     * <p>
     * This operation removes all messages from the queue without retrieving them.
     * Use this with caution as purged messages cannot be recovered.
     * 
     * @param chatViewId the ID of the chatview
     * @param userUid    the UID of the user whose queue should be purged
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
     * the queue.
     * 
     * <p>
     * This method retrieves all queued messages in order and empties the queue.
     * It is typically called when a user comes online to deliver all messages that
     * arrived while they were offline.
     * 
     * <p>
     * The method uses a 100ms timeout per message receive operation to quickly
     * determine when the queue is empty.
     * 
     * @param chatViewId the ID of the chatview
     * @param userUid    the UID of the user
     * @return list of MessageResponse objects that were queued (may be empty)
     */
    public List<MessageResponse> getAndPurgeMessagesFromQueue(String chatViewId, String userUid) {
        String queueName = RabbitMQConfig.getQueueName(chatViewId, userUid);
        List<MessageResponse> messages = new ArrayList<>();

        try {
            MessageResponse messageResponse;
            while ((messageResponse = (MessageResponse) rabbitTemplate.receiveAndConvert(queueName, 100)) != null) {
                messages.add(messageResponse);
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
     * Checks if a queue exists for a user in a chatview.
     * 
     * @param chatViewId the ID of the chatview
     * @param userUid    the UID of the user
     * @return true if the queue exists, false otherwise
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
