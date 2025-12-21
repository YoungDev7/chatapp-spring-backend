package com.chatapp.chatapp.config;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.chatapp.chatapp.entity.ChatView;
import com.chatapp.chatapp.entity.User;
import com.chatapp.chatapp.repository.ChatViewRepository;
import com.chatapp.chatapp.repository.UserRepository;
import com.chatapp.chatapp.service.ChatViewService;
import com.chatapp.chatapp.service.TestDataService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Profile("!test")
public class DataInitializer implements ApplicationRunner {
    
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    
    private final TestDataService testDataService;
    private final ChatViewRepository chatViewRepository;
    private final ChatViewService chatViewService;
    private final UserRepository userRepository;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("Initializing application data...");
        testDataService.initializeMinimalTestData();
        //testDataService.initializeMaximalTestData();
        initializeGlobalChatView();
        log.info("Application data initialization completed");
    }

    @Transactional
    public void initializeGlobalChatView() {
        Optional<ChatView> chatviewOptional = chatViewRepository.findById("1");

        if(!chatviewOptional.isPresent()){
            chatViewRepository.insertChatViewWithCustomId("1", "global");
            
            ChatView globalChatView = chatViewRepository.findByIdWithUsers("1").orElseThrow();
            
            chatViewRepository.save(globalChatView);
            log.info("global chat view is missing, new one was generated");
        }

        List<User> allUsers = userRepository.findAll();

        for (User user : allUsers) {
            if (!chatViewService.isUserInChatView("1", user.getUid())) {
                chatViewService.addUserToChatView("1", user.getUid());
            }
        }
    }
}
