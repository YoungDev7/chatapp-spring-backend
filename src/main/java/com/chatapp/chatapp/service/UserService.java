package com.chatapp.chatapp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.chatapp.chatapp.entity.User;
import com.chatapp.chatapp.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
    
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    //new user
    public void postNewUser(User user){
        if (user.getUid() != null) {
            throw new IllegalArgumentException("New user should not have an ID set");
        }
        
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);

        log.info("Saving new user: {}", user.getName());
        userRepository.save(user);
    }
}
