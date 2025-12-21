package com.chatapp.chatapp.config;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
public class RabbitMQConfig {
    
    public static final String CHAT_EXCHANGE = "chat.exchange";
    public static final String CHATVIEW_ROUTING_KEY_PREFIX = "chatview.";
    
    @Bean
    public TopicExchange chatExchange() {
        return new TopicExchange(CHAT_EXCHANGE);
    }
    
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
    
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
    
    @Bean
    public AmqpAdmin amqpAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }
    
    /**
     * Creates a queue for a specific chatview and user
     * Queue naming pattern: chatview.{chatViewId}.user.{userUid}
     */
    public Queue createUserChatViewQueue(String chatViewId, String userUid) {
        String queueName = getQueueName(chatViewId, userUid);
        return new Queue(queueName, true, false, false);
    }
    
    /**
     * Creates a binding between a user's chatview queue and the exchange
     * Routing key pattern: chatview.{chatViewId}
     */
    public Binding createUserChatViewBinding(Queue queue, TopicExchange exchange, String chatViewId) {
        String routingKey = CHATVIEW_ROUTING_KEY_PREFIX + chatViewId;
        return BindingBuilder.bind(queue).to(exchange).with(routingKey);
    }
    
    /**
     * Gets the queue name for a specific chatview and user
     */
    public static String getQueueName(String chatViewId, String userUid) {
        return "chatview." + chatViewId + ".user." + userUid;
    }
    
    /**
     * Gets the routing key for a specific chatview
     */
    public static String getRoutingKey(String chatViewId) {
        return CHATVIEW_ROUTING_KEY_PREFIX + chatViewId;
    }
}
