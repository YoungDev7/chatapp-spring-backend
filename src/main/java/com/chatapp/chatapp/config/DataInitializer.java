package com.chatapp.chatapp.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.chatapp.chatapp.service.TestDataService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {
    
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    
    private final TestDataService testDataService;
    private final Config config;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("Initializing application data...");
        testDataService.initializeMinimalTestData();
        testDataService.initializeMaximalTestData();
        config.initializeGlobalChatView();
        log.info("Application data initialization completed");
    }
}
