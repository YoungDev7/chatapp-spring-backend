package com.chatapp.chatapp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.chatapp.chatapp.entity.User;
import com.chatapp.chatapp.repository.UserRepository;

@Service

public class UserService {
    
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    //needed for @Lazy annotation to avoid circular dependency    
    public UserService(UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Lazy AuthService authService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
    }

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

    public String getUserAvatar(){
        User user = authService.getAuthenticatedUser();
        return user.getAvatarLink();
    }

    public void updateUserAvatar(String newAvatarLink) throws IllegalArgumentException{
        if(newAvatarLink == null){
            throw new IllegalArgumentException("avatar link string is null");
        }

        User user = authService.getAuthenticatedUser();
        user.setAvatarLink(newAvatarLink);
        userRepository.save(user);
    }
}
