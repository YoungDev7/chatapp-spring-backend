package com.chatapp.chatapp.config;


import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.chatapp.chatapp.entity.Message;
import com.chatapp.chatapp.entity.User;
import com.chatapp.chatapp.repository.IMessageRepository;
import com.chatapp.chatapp.repository.IUserRepository;

@Configuration
public class Config {

    @Bean
    CommandLineRunner commandLineRunner(IMessageRepository messageRepository, IUserRepository userRepository){
        return args ->{
            Message testMessage = new Message("Hello, World!", "John");
            User testUser = new User("Mike", "password123", "mikehock@mail.com");
            //repository.save(testMessage);
            //userRepository.save(testUser);
        };
    }
}
