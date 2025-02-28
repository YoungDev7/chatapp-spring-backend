package com.chatapp.chatapp;


import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClassConfig {

    @Bean
    CommandLineRunner commandLineRunner(IMessageRepository messageRepository, IUserRepository userRepository){
        return args ->{
            ClassMessage testMessage = new ClassMessage("Hello, World!", "John");
            ClassUser testUser = new ClassUser("Mike", "password123", "mikehock@mail.com");
            //repository.save(testMessage);
            //userRepository.save(testUser);
        };
    }
}
