package com.chatapp.chatapp.config;


import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.chatapp.chatapp.repository.MessageRepository;
import com.chatapp.chatapp.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class Config {

    private final UserRepository userRepository;
    private final MessageRepository messageRepository;

    @Bean
    CommandLineRunner commandLineRunner(MessageRepository messageRepository, UserRepository userRepository, ApplicationContext ctx){
        return args ->{
            // User testUser = new User("Test User One", this.passwordEncoder().encode("testuserone"), "test1@email.com");
            // User testUser2 = new User("Test User Two", this.passwordEncoder().encode("testusertwo"), "test2@email.com");
            // userRepository.save(testUser);
            // userRepository.save(testUser2);
            
            
            // User testUser = userRepository.findUserByEmail("gabeitch@example.com").orElseThrow(() -> new IllegalStateException("user not found:"));
            // Message testMessage = new Message("test", testUser);
            // messageRepository.save(testMessage);
            
        };
    }

    @Bean
    public UserDetailsService userDetailsService(){
        return username -> userRepository.findUserByEmail(username).orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
        
}
