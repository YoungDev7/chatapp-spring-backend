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


    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        //TODO: not working, authentication is null even when its not overriden to securitycontextholder 
        authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        try{
            User user = repository.findUserByEmail(username).orElseThrow(() -> new IllegalStateException("user not found: " + username));
            var validUserTokens = tokenRepository.findAllValidTokenByUser(user.getUid());
        
            if (validUserTokens.isEmpty()){
                return;
            }
    
            validUserTokens.forEach(token -> {
              token.setExpired(true);
              token.setRevoked(true);
            });
    
            tokenRepository.saveAll(validUserTokens);
            SecurityContextHolder.clearContext();
        }catch(Exception e){
            System.out.println("[LOG] Logout failed: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return; 
        }
    }
    

}
