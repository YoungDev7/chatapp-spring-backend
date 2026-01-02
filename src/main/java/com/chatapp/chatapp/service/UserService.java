package com.chatapp.chatapp.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.AuthenticationException;
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

/**
 * Service responsible for managing user accounts and profile operations.
 * 
 * <p>
 * This service handles user account creation, profile updates, and user search
 * functionality. It coordinates with the password encoder for secure password
 * storage
 * and publishes events for user lifecycle operations.
 * 
 * <p>
 * Key responsibilities include:
 * <ul>
 * <li>Creating new user accounts with password encoding</li>
 * <li>Managing user avatar links</li>
 * <li>Searching for users by username</li>
 * <li>Publishing UserCreatedEvent for new user registrations</li>
 * <li>Validating user input data</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthUtilService authUtilService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Creates a new user account in the system.
     * 
     * <p>
     * This method encodes the user's password using BCrypt, persists the user
     * to the database, and publishes a UserCreatedEvent for downstream processing
     * (such as welcome emails or initial setup).
     * 
     * <p>
     * The user must not have a UID set as this is generated upon persistence.
     * 
     * @param user the user entity to create (must not have UID set)
     * @throws IllegalArgumentException if the user already has a UID set
     */
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

    /**
     * Retrieves the avatar link for the currently authenticated user.
     * 
     * @return AvatarResponse containing the user's avatar link (may be null if not
     *         set)
     * @throws AuthenticationException if no authenticated user is found
     */
    public AvatarResponse getUserAvatar() {
        User user = authUtilService.getAuthenticatedUser();
        AvatarResponse response = new AvatarResponse(user.getAvatarLink());
        return response;
    }

    /**
     * Updates the avatar link for the currently authenticated user.
     * 
     * <p>
     * The avatar link is trimmed of whitespace before being saved.
     * 
     * @param newAvatarLink the new avatar URL (must not be null)
     * @throws IllegalArgumentException if the newAvatarLink is null
     * @throws AuthenticationException  if no authenticated user is found
     */
    public void updateUserAvatar(String newAvatarLink) throws IllegalArgumentException {
        if (newAvatarLink == null) {
            throw new IllegalArgumentException("avatar link string is null");
        }

        newAvatarLink = newAvatarLink.trim();

        User user = authUtilService.getAuthenticatedUser();
        user.setAvatarLink(newAvatarLink);
        userRepository.save(user);
    }

    /**
     * Searches for a user by username (case-insensitive).
     * 
     * <p>
     * The search query is trimmed of whitespace and must not be blank.
     * 
     * @param searchQuery the username to search for (case-insensitive)
     * @return UserResponse containing the found user's UID and name
     * @throws IllegalArgumentException  if searchQuery is null, empty, or blank
     * @throws UsernameNotFoundException if no user is found with the specified
     *                                   username
     */
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
