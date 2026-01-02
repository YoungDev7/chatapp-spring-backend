package com.chatapp.chatapp.service;

import java.security.Principal;

import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.chatapp.chatapp.entity.User;
import com.chatapp.chatapp.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Utility service for authentication-related operations.
 * 
 * <p>
 * This service provides helper methods for extracting authenticated user
 * information
 * from different contexts including HTTP security context and WebSocket STOMP
 * headers.
 * It handles various authentication principal types and provides consistent
 * user retrieval.
 * 
 * <p>
 * Key responsibilities include:
 * <ul>
 * <li>Extracting authenticated User from Spring Security context</li>
 * <li>Extracting authenticated User from WebSocket STOMP headers</li>
 * <li>Handling different authentication principal types</li>
 * <li>Providing consistent error handling for authentication failures</li>
 * </ul>
 * 
 */
@Service
@RequiredArgsConstructor
public class AuthUtilService {

    private final UserRepository userRepository;

    /**
     * Retrieves the authenticated user from the current security context.
     * 
     * <p>
     * This method extracts the User object from Spring Security's
     * SecurityContextHolder.
     * It validates that an authentication exists and that the principal is of the
     * correct type.
     * 
     * @return the authenticated User object
     * @throws InsufficientAuthenticationException if no authentication is found or
     *                                             the principal is not a User
     *                                             instance
     */
    public User getAuthenticatedUser() throws AuthenticationException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            throw new InsufficientAuthenticationException("No authentication found");
        }

        if (!(authentication.getPrincipal() instanceof User)) {
            throw new InsufficientAuthenticationException("Invalid authentication principal type");
        }

        return (User) authentication.getPrincipal();
    }

    /**
     * Retrieves the authenticated user from a STOMP header accessor.
     * 
     * <p>
     * This method extracts the User object from WebSocket STOMP message headers.
     * It handles UsernamePasswordAuthenticationToken and direct User principal
     * types.
     * 
     * @param headerAccessor the STOMP header accessor containing the principal
     * @return the authenticated User object
     * @throws InsufficientAuthenticationException if the principal is null or
     *                                             invalid
     */
    public User getAuthenticatedUserFromStompHeader(StompHeaderAccessor headerAccessor) {
        Principal principal = headerAccessor.getUser();

        if (principal == null) {
            throw new InsufficientAuthenticationException("principal is null");
        }

        // Check if principal is UsernamePasswordAuthenticationToken
        if (principal instanceof UsernamePasswordAuthenticationToken) {
            UsernamePasswordAuthenticationToken authToken = (UsernamePasswordAuthenticationToken) principal;
            Object principalObj = authToken.getPrincipal();

            if (!(principalObj instanceof User)) {
                throw new InsufficientAuthenticationException("Invalid authentication principal type");
            }

            return (User) principalObj;
        }

        // Direct User instance (shouldn't normally happen, but keep as fallback)
        if (principal instanceof User) {
            return (User) principal;
        }

        throw new InsufficientAuthenticationException("Invalid authentication principal type");
    }

    /**
     * Retrieves the authenticated user from a principal object.
     * 
     * <p>
     * This overloaded method provides additional fallback logic by attempting to
     * look up the user by UID from the principal name if standard extraction fails.
     * This is useful for WebSocket scenarios where the principal may be in
     * different formats.
     * 
     * @param principal the principal object from the WebSocket connection
     * @return the authenticated User object
     * @throws InsufficientAuthenticationException if the principal is null,
     *                                             invalid,
     *                                             or the user cannot be found in
     *                                             the database
     */
    public User getAuthenticatedUserFromStompHeader(Principal principal) {

        if (principal == null) {
            throw new InsufficientAuthenticationException("principal is null");
        }

        // Check if principal is UsernamePasswordAuthenticationToken
        if (principal instanceof UsernamePasswordAuthenticationToken) {
            UsernamePasswordAuthenticationToken authToken = (UsernamePasswordAuthenticationToken) principal;
            Object principalObj = authToken.getPrincipal();

            if (!(principalObj instanceof User)) {
                throw new InsufficientAuthenticationException("Invalid authentication principal type");
            }

            return (User) principalObj;
        }

        // Direct User instance (shouldn't normally happen, but keep as fallback)
        if (principal instanceof User) {
            return (User) principal;
        }

        String principalName = principal.getName();
        if (principalName != null && !principalName.isEmpty()) {
            return userRepository.findUserByUid(principalName)
                    .orElseThrow(
                            () -> new InsufficientAuthenticationException("User not found for uid: " + principalName));
        }

        throw new InsufficientAuthenticationException(
                "Invalid authentication principal type: " + principal.getClass().getName());
    }
}
