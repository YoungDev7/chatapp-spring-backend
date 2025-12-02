package com.chatapp.chatapp.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.chatapp.chatapp.dto.UserResponse;
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

        newAvatarLink = newAvatarLink.trim();

        User user = authService.getAuthenticatedUser();
        user.setAvatarLink(newAvatarLink);
        userRepository.save(user);
    }

    public UserResponse searchUser(String searchQuery) throws IllegalArgumentException, UsernameNotFoundException {
        if(searchQuery == null){
            throw new IllegalArgumentException("search query string is null");
        }

        searchQuery = searchQuery.trim();

        if(searchQuery.isBlank()){
            throw new IllegalArgumentException("search query string is empty or blank");
        }

        Optional<User> userOptional = userRepository.findUserByNameIgnoreCase(searchQuery);

        if(userOptional.isPresent()){
            return new UserResponse(userOptional.get().getUid(), userOptional.get().getName());
        }else {
            throw new UsernameNotFoundException("user " + searchQuery + " not found");
        }
    }
}
