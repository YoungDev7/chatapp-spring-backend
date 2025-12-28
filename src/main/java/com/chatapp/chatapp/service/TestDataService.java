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
