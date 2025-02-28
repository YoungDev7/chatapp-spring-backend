package com.chatapp.chatapp;


import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
