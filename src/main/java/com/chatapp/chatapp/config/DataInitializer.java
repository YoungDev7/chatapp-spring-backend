package com.chatapp.chatapp.config;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.chatapp.chatapp.entity.ChatView;
import com.chatapp.chatapp.repository.ChatViewRepository;
import com.chatapp.chatapp.service.TestDataService;

import lombok.RequiredArgsConstructor;

/**
 * Application data initialization component.
 * <p>
 * This component implements ApplicationRunner to execute data initialization
 * logic
 * when the application starts. It ensures that required data structures exist
 * in the
 * database before the application begins serving requests.
 * </p>
 *
 * <p>
 * <strong>Initialization Tasks:</strong>
 * </p>
 * <ul>
 * <li>Creates the global chat view with ID "1" if it doesn't exist</li>
 * <li>Initializes minimal test data for development environments</li>
 * <li>Logs initialization progress for monitoring</li>
 * </ul>
 *
 * <p>
 * <strong>Profile Configuration:</strong>
 * </p>
 * <ul>
 * <li>Active in all profiles EXCEPT test profile (marked
 * with @Profile("!test"))</li>
 * <li>Skipped during test execution to avoid interference with test data</li>
 * </ul>
 *
 * <p>
 * <strong>Important Notes:</strong>
 * </p>
 * <ul>
 * <li>Uses custom ID "1" for global chat view (requires special repository
 * method)</li>
 * <li>Initialization is transactional to ensure data consistency</li>
 * <li>TestDataService provides additional development data</li>
 * </ul>
 *
 * @see ApplicationRunner
 * @see TestDataService
 */
@Component
@RequiredArgsConstructor
@Profile("!test")
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final TestDataService testDataService;
    private final ChatViewRepository chatViewRepository;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("Initializing application data...");
        initializeGlobalChatView();
        testDataService.initializeMinimalTestData();
        log.info("Application data initialization completed");
    }

    /**
     * Initializes the global chat view if it doesn't already exist.
     * <p>
     * The global chat view (with ID "1" and name "global") serves as the default
     * public chat room for all users. This method ensures it exists in the database
     * before users attempt to access it.
     * </p>
     */
    @Transactional
    public void initializeGlobalChatView() {
        Optional<ChatView> chatviewOptional = chatViewRepository.findById("1");

        if (!chatviewOptional.isPresent()) {
            chatViewRepository.insertChatViewWithCustomId("1", "global");

            ChatView globalChatView = chatViewRepository.findByIdWithUsers("1").orElseThrow();

            chatViewRepository.save(globalChatView);
            log.info("global chat view is missing, new one was generated");
        }
    }
}
