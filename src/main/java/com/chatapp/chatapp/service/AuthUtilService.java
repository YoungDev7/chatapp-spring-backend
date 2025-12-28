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

@Service
@RequiredArgsConstructor
public class AuthUtilService {

    private final UserRepository userRepository;

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
