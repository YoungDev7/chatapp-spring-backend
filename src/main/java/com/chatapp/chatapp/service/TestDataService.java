package com.chatapp.chatapp.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chatapp.chatapp.entity.User;
import com.chatapp.chatapp.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service responsible for initializing test data in development environments.
 * 
 * <p>
 * This service creates predefined test users when the application starts in
 * development or Docker development mode. It ensures consistent test data is
 * available
 * for manual testing and development purposes.
 * 
 * <p>
 * Key responsibilities include:
 * <ul>
 * <li>Creating test users if they don't already exist</li>
 * <li>Only operating in development profiles (dev or docker_dev)</li>
 * <li>Preventing duplicate test user creation</li>
 * <li>Logging test data initialization activities</li>
 * </ul>
 * 
 * <p>
 * <b>Note:</b> This service only runs in development environments and should
 * never be active in production.
 * 
 */
@Service
@RequiredArgsConstructor
public class TestDataService {

    private static final Logger log = LoggerFactory.getLogger(TestDataService.class);

    @Value("${spring.profiles.active}")
    private String activeSpringProfile;
    @Value("${app.docker-profile}")
    private String activeDockerProfile;
    private final UserRepository userRepository;
    private final UserService userService;

    /**
     * Initializes minimal test data by creating predefined test users.
     * 
     * <p>
     * This method checks if the application is running in development or Docker
     * development mode, and creates two test users if they don't already exist:
     * <ul>
     * <li>Test User One (test1@email.com, password: testuserone)</li>
     * <li>Test User Two (test2@email.com, password: testusertwo)</li>
     * </ul>
     * 
     * <p>
     * The method is idempotent - it checks for existing users by email before
     * creating them, so it can be safely called multiple times.
     * 
     * <p>
     * <b>Note:</b> This method only operates when the active profile is 'dev' or
     * 'docker_dev'. It has no effect in production environments.
     */
    @Transactional
    public void initializeMinimalTestData() {
        if (activeDockerProfile.equals("docker_dev") || activeSpringProfile.equals("dev")) {

            Optional<User> testUserOneOptional = userRepository.findUserByEmail("test1@email.com");
            Optional<User> testUserTwoOptional = userRepository.findUserByEmail("test2@email.com");

            if (!testUserOneOptional.isPresent()) {
                User testUser = new User("Test User One", "testuserone", "test1@email.com");
                userService.postNewUser(testUser);
                log.info("adding missing test user 1");
            }

            if (!testUserTwoOptional.isPresent()) {
                User testUser2 = new User("Test User Two", "testusertwo", "test2@email.com");
                userService.postNewUser(testUser2);
                log.info("adding missing test user 2");
            }
        }
    }

}
