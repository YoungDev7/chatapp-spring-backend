package com.chatapp.chatapp.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.chatapp.chatapp.entity.User;
import com.chatapp.chatapp.repository.UserRepository;
import com.chatapp.chatapp.util.ApplicationLogger;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    //new user
    public void postNewUser(User user){
        if (user.getUid() != null) {
            throw new IllegalArgumentException("New user should not have an ID set");
        }
        
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);

        ApplicationLogger.infoLog("Saving new user: " + user.getName());
        userRepository.save(user);
    }
}
