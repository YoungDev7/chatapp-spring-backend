package com.chatapp.chatapp.auth;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Service;

import com.chatapp.chatapp.entity.User;
import com.chatapp.chatapp.repository.IUserRepository;
import com.chatapp.chatapp.repository.TokenRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LogoutService implements LogoutHandler{
    
    private final TokenRepository tokenRepository;
    private final IUserRepository repository;
    private final AuthService authService;

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        try{
            User user = repository.findUserByEmail(username).orElseThrow(() -> new IllegalStateException("user not found: " + username));
            authService.revokeAllUserTokens(user);
            SecurityContextHolder.clearContext();
        }catch(Exception e){
            System.out.println("[LOG] Logout failed: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return; 
        }
    }
    

}
