package com.chatapp.chatapp.config;


import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.transaction.annotation.Transactional;

import com.chatapp.chatapp.entity.ChatView;
import com.chatapp.chatapp.entity.User;
import com.chatapp.chatapp.repository.ChatViewRepository;
import com.chatapp.chatapp.repository.MessageRepository;
import com.chatapp.chatapp.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class Config {

    private static final Logger log = LoggerFactory.getLogger(Config.class);

    private final UserRepository userRepository;
    private final ChatViewRepository chatViewRepository;

    @Bean
    CommandLineRunner commandLineRunner(MessageRepository messageRepository, UserRepository userRepository, ApplicationContext ctx){
        return args ->{
            
        };
    }

    @Transactional
    public void initializeGlobalChatView() {
        Optional<ChatView> chatviewOptional = chatViewRepository.findById("1");

        if(!chatviewOptional.isPresent()){
            chatViewRepository.insertChatViewWithCustomId("1", "global");
            
            ChatView globalChatView = chatViewRepository.findByIdWithUsers("1").orElseThrow();
            List<User> allUsers = userRepository.findAll();
            
            for (User user : allUsers) {
                globalChatView.addUser(user);
            }
            
            chatViewRepository.save(globalChatView);
            log.info("global chat view is missing, new one was generated");
        }
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
    
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
        
}
