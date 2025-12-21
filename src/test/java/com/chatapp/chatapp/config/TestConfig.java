package com.chatapp.chatapp.config;

import static org.mockito.Mockito.mock;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import com.chatapp.chatapp.service.RabbitMQService;

@TestConfiguration
@Profile("test")
public class TestConfig {

    @Bean
    @Primary
    public RabbitMQService rabbitMQService() {
        return mock(RabbitMQService.class);
    }
}