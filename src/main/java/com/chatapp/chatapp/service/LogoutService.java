package com.chatapp.chatapp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Service;

import com.chatapp.chatapp.Dto.JwtValidationResult;
import com.chatapp.chatapp.entity.User;
import com.chatapp.chatapp.repository.TokenRepository;
import com.chatapp.chatapp.repository.UserRepository;
import com.chatapp.chatapp.util.LoggerUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LogoutService implements LogoutHandler{
    
    private static final Logger log = LoggerFactory.getLogger(LogoutService.class);
    
    private final TokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final LoggerUtil loggerUtil;

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
        
        loggerUtil.setupRequestContext(request);
        
        try{
            if (authHeader == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Authorization header missing and no refresh token in cookies");
                log.warn("[{}] Authorization header missing and no refresh token in cookies", HttpServletResponse.SC_UNAUTHORIZED);
                return; 
            }
    
            if (!authHeader.startsWith("Bearer ")) {
              response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
              response.getWriter().write("Invalid authorization format");
              log.warn("[{}] Invalid authorization format (missing Bearer)", HttpServletResponse.SC_UNAUTHORIZED);
              return; 
            }
    
            final String jwt = authHeader.substring(7);
            User user;
            JwtValidationResult validationResult = jwtService.validateToken(jwt);
    
            if(!validationResult.isValid()){
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Invalid token " + validationResult.getStatus());
                log.warn("[{}] Invalid access token {}; username: {}", 
                    HttpServletResponse.SC_UNAUTHORIZED, 
                    validationResult.getStatus().toString(),
                    validationResult.getUsername());
                return; 
            }
    
            loggerUtil.setupUserContext(validationResult.getUsername());
    
            try{
                user = userRepository.findUserByEmail(validationResult.getUsername()).orElseThrow(() -> new UsernameNotFoundException("User not found"));
                var validUserTokens = tokenRepository.findAllValidTokenByUser(user.getUid());
            
                if (validUserTokens.isEmpty()){
                    log.info("[{}] logout successful", 200);
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

                log.info("[{}] logout successful", 200);
                response.getWriter().write("logout successful");
                response.setStatus(HttpServletResponse.SC_OK);

            } catch (Exception e){
                log.warn("[{}] Logout failed: {}", HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
                response.getWriter().write("logout failed");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }catch (Exception e){
            log.error("Logout error: {}", e.getMessage(), e);
        }finally{
            loggerUtil.clearContext();
        }

    }
}
