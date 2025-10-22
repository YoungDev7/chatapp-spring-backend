package com.chatapp.chatapp.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chatapp.chatapp.Dto.AuthRequest;
import com.chatapp.chatapp.Dto.AuthResponse;
import com.chatapp.chatapp.Dto.RegisterRequest;
import com.chatapp.chatapp.Dto.TokenInfo;
import com.chatapp.chatapp.entity.User;
import com.chatapp.chatapp.service.AuthService;
import com.chatapp.chatapp.util.LoggerUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    
    private final AuthService authService;
    private final LoggerUtil loggerUtil;

    /**
     * Authenticates a user with email and password credentials.
     * 
     * @param request The authentication request containing email and password
     * @param httpServletRequest The HTTP servlet request for logging purposes
     * @param httpServletResponse The HTTP servlet response to set refresh token cookie
     * @return ResponseEntity containing the access token on success (200), 
     *         or error message on authentication failure (401)
     */
    @PostMapping("/authenticate")
    public ResponseEntity<?> authenticate(@RequestBody AuthRequest request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        loggerUtil.setupRequestContext(httpServletRequest);
        loggerUtil.setupUserContext(request.getEmail());
        
        try{
            TokenInfo tokens = authService.authenticate(request);
            
            httpServletResponse.addHeader(HttpHeaders.SET_COOKIE, tokens.getRefreshCookie().toString());
            
            AuthResponse response = new AuthResponse(tokens.getAccessToken());
            
            log.info("[{}] user logged in", 200);

            return ResponseEntity.ok(response);

        }catch (BadCredentialsException e){
            log.warn("[{}] user login failed: {}", 401, e.getMessage());
            
            return ResponseEntity.status(401).body("invalid email or password");
        }finally{
            loggerUtil.clearContext();
        }
    }

    /**
     * Registers a new user account.
     * 
     * @param request The registration request containing user details (email, username, password)
     * @param httpServletRequest The HTTP servlet request for logging purposes
     * @param httpServletResponse The HTTP servlet response (unused but kept for consistency)
     * @return ResponseEntity with success message (201) on successful registration,
     *         conflict error (409) if user already exists,
     *         bad request (400) for invalid input format,
     *         or forbidden (403) for other registration failures
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse){
        loggerUtil.setupRequestContext(httpServletRequest);
        loggerUtil.setupUserContext(request.getEmail());
        
        try{
            authService.register(request);
            log.info("[{}] user registered", 201);
            return ResponseEntity.status(201).body("registration successful");
        }catch (IllegalArgumentException e){
            String message = e.getMessage();

            if(message.equals("user with this email exists") || message.equals("user with this username exists")){
                log.warn("[{}] registration failed: {}", 409, message);
                return ResponseEntity.status(409).body(message);    
            }

            if(message.contains("Invalid email format: ") || message.equals("password or username is blank")){
                log.warn("[{}] registration failed: {}", 400, message);
                return ResponseEntity.status(400).body(message);
            }
            
            log.warn("[{}] registration failed: {}", 403, message);
            return ResponseEntity.status(403).body("registration fail");
        }finally{
            loggerUtil.clearContext();
        }
    }

    /**
     * Refreshes an expired access token using a valid refresh token.
     * Performs token rotation by generating new access and refresh tokens,
     * revoking all existing valid tokens for the user.
     * The refresh token is expected to be provided via HTTP-only cookie.
     * 
     * @param httpServletRequest The HTTP servlet request for logging purposes
     * @param httpServletResponse The HTTP servlet response to set the new refresh token cookie
     * @return ResponseEntity containing a new access token (200) on success,
     *         or unauthorized error (401) if refresh token is invalid or user not found
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse){
        loggerUtil.setupRequestContext(httpServletRequest);
        
        try{
            TokenInfo tokenInfo = authService.refreshToken();
            httpServletResponse.addHeader(HttpHeaders.SET_COOKIE, tokenInfo.getRefreshCookie().toString());
            AuthResponse response = new AuthResponse(tokenInfo.getAccessToken());            
                        
            User user = authService.getAuthenticatedUser();
            loggerUtil.setupUserContext(user.getName());
            log.info("[{}] token refreshed", 200);

            return ResponseEntity.ok(response);

        }catch (Exception e){
            log.warn("[{}] Refresh error: {}", 401, e.getMessage());
            return ResponseEntity.status(401).body("Refresh error");
        }finally{
            loggerUtil.clearContext();
        }
    }

    /**
     * Validates the current access token.
     * This endpoint can be used to check if a user's token is still valid.
     * 
     * @param httpServletRequest The HTTP servlet request for logging purposes
     * @return ResponseEntity with "valid" message (200) if token is valid.
     *         Invalid tokens will be handled by security filters before reaching this method.
     */
    @GetMapping("/validateToken")
    public ResponseEntity<?> validateToken(HttpServletRequest httpServletRequest){
        loggerUtil.setupRequestContext(httpServletRequest);
        
        User user = authService.getAuthenticatedUser();
        loggerUtil.setupUserContext(user.getName());
        log.info("[{}] token validated", 200);
        loggerUtil.clearContext();

        return ResponseEntity.ok("valid");
    }

    /**
     * Logs out the currently authenticated user.
     * Revokes all valid tokens for the user and clears the security context.
     * 
     * @param httpServletRequest The HTTP servlet request for logging purposes
     * @return ResponseEntity with success message (200) on successful logout,
     *         or unauthorized error (401) if logout fails due to authentication issues
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest httpServletRequest) {
        loggerUtil.setupRequestContext(httpServletRequest);

        try{
            authService.logout();
            User user = authService.getAuthenticatedUser();
            loggerUtil.setupUserContext(user.getName());
            log.info("[{}] logout successful", 200);
            return ResponseEntity.ok("logout successful");
        } catch (Exception e){
            log.warn("[{}] Logout failed: {}", HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
            return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED).body("logout failed");
        } finally {
            loggerUtil.clearContext();
        }  
    }
    
        
}
