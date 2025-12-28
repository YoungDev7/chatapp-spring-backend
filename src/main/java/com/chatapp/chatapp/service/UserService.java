package com.chatapp.chatapp.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chatapp.chatapp.dto.AvatarResponse;
import com.chatapp.chatapp.dto.UserResponse;
import com.chatapp.chatapp.entity.User;
import com.chatapp.chatapp.event.UserCreatedEvent;
import com.chatapp.chatapp.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthUtilService authUtilService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void postNewUser(User user) {
        if (user.getUid() != null) {
            throw new IllegalArgumentException("New user should not have an ID set");
        }

        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);

        log.info("Saving new user: {}", user.getName());
        userRepository.save(user);

        log.debug("before event call: " + user.getUid());
        eventPublisher.publishEvent(new UserCreatedEvent(user.getEmail()));
    }

    public AvatarResponse getUserAvatar() {
        User user = authUtilService.getAuthenticatedUser();
        AvatarResponse response = new AvatarResponse(user.getAvatarLink());
        return response;
    }

    public void updateUserAvatar(String newAvatarLink) throws IllegalArgumentException {
        if (newAvatarLink == null) {
            throw new IllegalArgumentException("avatar link string is null");
        }

        newAvatarLink = newAvatarLink.trim();

        User user = authUtilService.getAuthenticatedUser();
        user.setAvatarLink(newAvatarLink);
        userRepository.save(user);
    }

    public UserResponse searchUser(String searchQuery) throws IllegalArgumentException, UsernameNotFoundException {
        if (searchQuery == null) {
            throw new IllegalArgumentException("search query string is null");
        }

        searchQuery = searchQuery.trim();

        if (searchQuery.isBlank()) {
            throw new IllegalArgumentException("search query string is empty or blank");
        }

        Optional<User> userOptional = userRepository.findUserByNameIgnoreCase(searchQuery);

        if (userOptional.isPresent()) {
            return new UserResponse(userOptional.get().getUid(), userOptional.get().getName());
        } else {
            throw new UsernameNotFoundException("user " + searchQuery + " not found");
        }
    }
}
