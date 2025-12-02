package com.chatapp.chatapp.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chatapp.chatapp.dto.MessageResponse;
import com.chatapp.chatapp.entity.User;
import com.chatapp.chatapp.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TestDataService {

    private static final Logger log = LoggerFactory.getLogger(TestDataService.class);

    @Value("${spring.profiles.active}")
    private String activeSpringProfile;
    @Value("${app.docker-profile}")
    private String activeDockerProfile;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;
    // Inject RabbitMQService lazily via setter to break circular dependency
    private RabbitMQService rabbitMQService;

    @Autowired
    public void setRabbitMQService(@Lazy RabbitMQService rabbitMQService) {
        this.rabbitMQService = rabbitMQService;
    }


    @Transactional
    public void initializeMinimalTestData() {
        if(activeDockerProfile.equals("docker_dev") || activeSpringProfile.equals("dev")){

            Optional<User> testUserOneOptional = userRepository.findUserByEmail("test1@email.com");
            Optional<User> testUserTwoOptional = userRepository.findUserByEmail("test2@email.com");

            if(!testUserOneOptional.isPresent()){
                User testUser = new User("Test User One", passwordEncoder.encode("testuserone"), "test1@email.com");
                userRepository.save(testUser);
                log.info("adding missing test user 1");
            }

            if(!testUserTwoOptional.isPresent()){
                User testUser2 = new User("Test User Two", passwordEncoder.encode("testusertwo"), "test2@email.com");
                userRepository.save(testUser2);
                log.info("adding missing test user 2");
            }
        }
    }

    @Transactional
    public void initializeMaximalTestData(){
        if(activeDockerProfile.equals("docker_dev") || activeSpringProfile.equals("dev")){
            log.info("Starting maximal test data initialization...");
            
            try {
                // Get test user one UID
                Optional<User> testUserOneOptional = userRepository.findUserByEmail("test1@email.com");
                Optional<User> testUserTwoOptional = userRepository.findUserByEmail("test2@email.com");
                
                if(!testUserOneOptional.isPresent() || !testUserTwoOptional.isPresent()) {
                    log.error("Test users must exist before initializing maximal test data. Please run initializeMinimalTestData first.");
                    return;
                }
                
                String testUserOneUid = testUserOneOptional.get().getUid();
                String testUserTwoUid = testUserTwoOptional.get().getUid();
                
                // Load and execute SQL file
                ClassPathResource resource = new ClassPathResource("db/test-data-maximal.sql");
                try (InputStream inputStream = resource.getInputStream();
                     InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                     BufferedReader reader = new BufferedReader(inputStreamReader)) {
                    
                    String sqlContent = reader.lines().collect(Collectors.joining("\n"));
                    
                    // Replace placeholders with actual UIDs
                    sqlContent = sqlContent.replace("{{TEST_USER_ONE_UID}}", testUserOneUid);
                    sqlContent = sqlContent.replace("{{TEST_USER_TWO_UID}}", testUserTwoUid);
                    
                    // Split by semicolon and execute each statement
                    String[] sqlStatements = sqlContent.split(";");
                    int successCount = 0;
                    int failCount = 0;
                    int skippedCount = 0;
                    for (int i = 0; i < sqlStatements.length; i++) {
                        String statement = sqlStatements[i];
                        // Remove comment lines but keep the SQL
                        String[] lines = statement.split("\n");
                        StringBuilder sqlBuilder = new StringBuilder();
                        for (String line : lines) {
                            String trimmedLine = line.trim();
                            if (!trimmedLine.isEmpty() && !trimmedLine.startsWith("--")) {
                                sqlBuilder.append(line).append("\n");
                            }
                        }
                        String trimmed = sqlBuilder.toString().trim();
                        
                        if (!trimmed.isEmpty()) {
                            try {
                                jdbcTemplate.execute(trimmed);
                                successCount++;
                            } catch (Exception e) {
                                failCount++;
                            }
                        } else {
                            skippedCount++;
                        }
                    }
                    
                    log.info("Successfully executed test data SQL file - Success: {}, Failed: {}, Skipped: {}", successCount, failCount, skippedCount);
                    
                } catch (Exception e) {
                    log.error("Error loading or executing SQL file: {}", e.getMessage(), e);
                    return;
                }
                
                // Add messages to RabbitMQ queue for "messages in queue" chatview
                String chatViewId = "104"; // Corresponds to the "messages in queue" chatview
                
                // Create queue if it doesn't exist
                if (!rabbitMQService.queueExists(chatViewId, testUserOneUid)) {
                    rabbitMQService.createUserQueueForChatView(chatViewId, testUserOneUid);
                }
                
                // Add 5 messages to the queue for test user one
                ZonedDateTime baseTime = ZonedDateTime.now().minusMinutes(15);
                
                MessageResponse queueMsg1 = new MessageResponse(
                    "Queue message 1 for test user one",
                    "Test User Two",
                    testUserTwoUid,
                    chatViewId,
                    baseTime
                );
                
                MessageResponse queueMsg2 = new MessageResponse(
                    "Queue message 2 for test user one",
                    "Test User Five",
                    "test-user-5-uid",
                    chatViewId,
                    baseTime.plusMinutes(2)
                );
                
                MessageResponse queueMsg3 = new MessageResponse(
                    "Queue message 3 for test user one",
                    "Test User Seven",
                    "test-user-7-uid",
                    chatViewId,
                    baseTime.plusMinutes(4)
                );
                
                MessageResponse queueMsg4 = new MessageResponse(
                    "Queue message 4 for test user one",
                    "Test User Two",
                    testUserTwoUid,
                    chatViewId,
                    baseTime.plusMinutes(6)
                );
                
                MessageResponse queueMsg5 = new MessageResponse(
                    "Queue message 5 for test user one",
                    "Test User Five",
                    "test-user-5-uid",
                    chatViewId,
                    baseTime.plusMinutes(8)
                );
                
                // Send messages to queue
                rabbitMQService.sendMessageToUserQueue(chatViewId, testUserOneUid, queueMsg1);
                rabbitMQService.sendMessageToUserQueue(chatViewId, testUserOneUid, queueMsg2);
                rabbitMQService.sendMessageToUserQueue(chatViewId, testUserOneUid, queueMsg3);
                rabbitMQService.sendMessageToUserQueue(chatViewId, testUserOneUid, queueMsg4);
                rabbitMQService.sendMessageToUserQueue(chatViewId, testUserOneUid, queueMsg5);
                
                log.info("Maximal test data initialization completed successfully");
                
            } catch (Exception e) {
                log.error("Error during maximal test data initialization: {}", e.getMessage(), e);
            }
        } else {
            log.info("Maximal test data initialization skipped - not in dev profile");
        }
    }
}
