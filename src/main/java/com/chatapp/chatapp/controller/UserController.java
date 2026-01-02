package com.chatapp.chatapp.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chatapp.chatapp.dto.AvatarRequest;
import com.chatapp.chatapp.dto.AvatarResponse;
import com.chatapp.chatapp.dto.UserResponse;
import com.chatapp.chatapp.service.UserService;

import lombok.RequiredArgsConstructor;

/**
 * REST controller for user-related operations.
 * 
 * <p>
 * This controller provides endpoints for managing user profiles and
 * information,
 * including avatar management and user search functionality.
 * </p>
 * 
 * <p>
 * Base path: {@code /api/v1/user}
 * </p>
 * 
 * <p>
 * Key features:
 * </p>
 * <ul>
 * <li>Retrieve and update user avatar</li>
 * <li>Search for users by username or email</li>
 * </ul>
 * 
 * <p>
 * Security: All endpoints require authentication. Operations are performed
 * in the context of the authenticated user.
 * </p>
 * 
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/user")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    /**
     * Retrieves the avatar URL of the authenticated user.
     * 
     * <p>
     * HTTP Method: GET
     * </p>
     * <p>
     * Path: {@code /api/v1/user/avatar}
     * </p>
     * 
     * <p>
     * Returns the current avatar link/URL for the authenticated user's profile.
     * The avatar is used for visual identification in the chat application.
     * </p>
     * 
     * <p>
     * Response format (example):
     * </p>
     * 
     * <pre>
     * {
     *   "avatarLink": "https://example.com/avatars/user123.jpg"
     * }
     * </pre>
     * 
     * @return ResponseEntity containing AvatarResponse with the user's avatar URL
     *         (200 OK)
     */
    @GetMapping("/avatar")
    public ResponseEntity<AvatarResponse> avatar() {
        AvatarResponse avatarLink = userService.getUserAvatar();
        log.info("retrieved users avatar");

        return ResponseEntity.ok(avatarLink);
    }

    /**
     * Updates the avatar URL of the authenticated user.
     * 
     * <p>
     * HTTP Method: PATCH
     * </p>
     * <p>
     * Path: {@code /api/v1/user/avatar}
     * </p>
     * 
     * <p>
     * Updates the user's profile avatar with a new image URL. The URL should point
     * to a valid, accessible image resource.
     * </p>
     * 
     * <p>
     * Request body format (example):
     * </p>
     * 
     * <pre>
     * {
     *   "avatarLink": "https://example.com/avatars/new-avatar.jpg"
     * }
     * </pre>
     * 
     * <p>
     * Success response (200 OK): "avatar updated"
     * </p>
     * 
     * @param avatarRequest The avatar update request containing the new avatar URL
     * @return ResponseEntity with success message (200 OK) if update succeeds,
     *         or error message (400 Bad Request) if the avatar link is null or
     *         invalid
     * @throws IllegalArgumentException if the avatar link is null or empty
     */
    @PatchMapping("/avatar")
    public ResponseEntity<?> avatar(@RequestBody AvatarRequest avatarRequest) {
        try {
            userService.updateUserAvatar(avatarRequest.getAvatarLink());
            log.info("updated users avatar");
            return ResponseEntity.ok("avatar updated");
        } catch (IllegalArgumentException e) {
            log.info("error updating users avatar: {}", e);
            return ResponseEntity.status(400).body("string is null");
        }
    }

    /**
     * Searches for a user by username or email.
     * 
     * <p>
     * HTTP Method: GET
     * </p>
     * <p>
     * Path: {@code /api/v1/user/search/{query}}
     * </p>
     * 
     * <p>
     * Searches the user database for a user matching the provided query string.
     * The search can match against username or email address. This is typically
     * used
     * to find users when creating new chat views or adding participants.
     * </p>
     * 
     * <p>
     * Response format on success (200 OK):
     * </p>
     * 
     * <pre>
     * {
     *   "uid": "user123",
     *   "username": "johndoe",
     *   "email": "john@example.com",
     *   "avatarLink": "https://example.com/avatars/john.jpg"
     * }
     * </pre>
     * 
     * @param query The search query string (username or email to search for)
     * @return ResponseEntity containing UserResponse with the matching user's
     *         details (200 OK)
     *         on successful search, or error message (400 Bad Request) if the query
     *         is invalid
     *         or no user is found
     * @throws IllegalArgumentException  if the query parameter is invalid
     * @throws UsernameNotFoundException if no user matches the search query
     */
    @GetMapping("/search/{query}")
    public ResponseEntity<?> searchUser(@PathVariable String query) {
        try {
            UserResponse searchResult = userService.searchUser(query);
            log.info("returned successful search result: {}; search query: {}", searchResult.getUid(), query);
            return ResponseEntity.ok().body(searchResult);
        } catch (IllegalArgumentException e) {
            log.info("unsuccessful user search: {}", e.getMessage());
            return ResponseEntity.status(400).body(e.getMessage());
        } catch (UsernameNotFoundException e) {
            log.info("unsuccessful user search: {}", e.getMessage());
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }
}
