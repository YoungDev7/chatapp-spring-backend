package com.chatapp.chatapp.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Service;

import com.chatapp.chatapp.DTO.JwtValidationResult;
import com.chatapp.chatapp.entity.User;
import com.chatapp.chatapp.repository.IUserRepository;
import com.chatapp.chatapp.repository.TokenRepository;
import com.chatapp.chatapp.util.ApplicationLogger;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LogoutService implements LogoutHandler{
    
    private final TokenRepository tokenRepository;
    private final IUserRepository userRepository;
    private final JwtService jwtService;

    /**
     * Handles user logout by validating the JWT access token and revoking all user tokens.
     * 
     * The method expects an Authorization header with a Bearer token. If the token is valid
     * and the user exists, all their tokens are invalidated to ensure complete logout.
     * 
     * @param request the HTTP servlet request containing the Authorization header with JWT token
     * @param response the HTTP servlet response used to write status and messages
     * @param authentication the current authentication object (may be null)
     * 
     * Response scenarios:
     * - 200 OK: Logout successful, all tokens revoked
     * - 401 UNAUTHORIZED: Missing/invalid authorization header, invalid token, or user not found
     * 
     * @see LogoutHandler#logout(HttpServletRequest, HttpServletResponse, Authentication)
     */
    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {

        final String authHeader = request.getHeader("Authorization");
        
        try{
            if (authHeader == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Authorization header missing and no refresh token in cookies");
                ApplicationLogger.requestLogFilter(request, "Authorization header missing and no refresh token in cookies", HttpServletResponse.SC_UNAUTHORIZED, null);
                return; 
            }
    
            if (!authHeader.startsWith("Bearer ")) {
              response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
              response.getWriter().write("Invalid authorization format");
              ApplicationLogger.requestLogFilter(request, "Invalid authorization format (missing Bearer)", HttpServletResponse.SC_UNAUTHORIZED, authHeader);
              return; 
            }
    
            final String jwt = authHeader.substring(7);
            User user;
            JwtValidationResult validationResult = jwtService.validateToken(jwt);
    
            if(!validationResult.isValid()){
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Invalid token " + validationResult.getStatus());
                ApplicationLogger.requestLogFilter(request, "Invalid access token", HttpServletResponse.SC_UNAUTHORIZED, authHeader, validationResult.getUsername(), validationResult.getStatus().toString());
                return; 
            }
    
            try{
                user = userRepository.findUserByEmail(validationResult.getUsername()).orElseThrow(() -> new UsernameNotFoundException("User not found"));
                var validUserTokens = tokenRepository.findAllValidTokenByUser(user.getUid());
            
                if (validUserTokens.isEmpty()){
                    ApplicationLogger.requestLog(request, "logout successful", validationResult.getUsername(), 200);
                    response.getWriter().write("logout successful");
                    response.setStatus(HttpServletResponse.SC_OK);
                    return;
                }
        
                validUserTokens.forEach(token -> {
                  token.setExpired(true);
                  token.setRevoked(true);
                });
        
                tokenRepository.saveAll(validUserTokens);
    
                if(SecurityContextHolder.getContext().getAuthentication() != null){
                    SecurityContextHolder.clearContext();
                }

                ApplicationLogger.requestLog(request, "logout successful", validationResult.getUsername(), 200);
                response.getWriter().write("logout successful");
                response.setStatus(HttpServletResponse.SC_OK);

            } catch (Exception e){
                ApplicationLogger.warningLog("[LOG] Logout failed: " + e.getMessage());
                response.getWriter().write("logout failed");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }catch (Exception e){
            ApplicationLogger.errorLog(e.getMessage());
            e.printStackTrace();
        }

    }
}
